package com.zirakusa.gatewayhub.assistant.local

import android.content.Context
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class LocalModel(
    val title: String,
    val sizeLabel: String,
    val size: Long,
    val sha256: String,
    private val source: String
) {
    QWEN3("Qwen3 4B", "2,50 GB", 2_497_280_256, "7485fe6f11af29433bc51cab58009521f205840f5b4ae3a32fa7f92e8534fdf5",
        "Qwen3-4B-GGUF/resolve/bc640142c66e1fdd12af0bd68f40445458f3869b/Qwen3-4B-Q4_K_M.gguf"),
    QWEN25("Qwen2.5 1.5B", "1,12 GB", 1_117_320_736, "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e",
        "Qwen2.5-1.5B-Instruct-GGUF/resolve/91cad51170dc346986eccefdc2dd33a9da36ead9/qwen2.5-1.5b-instruct-q4_k_m.gguf");

    val url get() = URI("https://huggingface.co/Qwen/$source").toURL()
    val fileName get() = source.substringAfterLast('/')
}

class ModelStore(context: Context) {
    private val directory = File(context.filesDir, "lucia-models")
    private fun target(model: LocalModel) = File(directory, model.fileName)

    fun installed(model: LocalModel): File? = target(model).takeIf { it.isFile && it.length() == model.size }

    suspend fun download(model: LocalModel, onProgress: suspend (Float) -> Unit) = withContext(Dispatchers.IO) {
        downloads.withLock {
            if (installed(model) != null) return@withLock
            if (!directory.isDirectory && !directory.mkdirs()) throw ModelDownloadError.Storage()
            // A killed process can leave a partial file. The shared lock protects active downloads.
            directory.listFiles { _, name -> name.startsWith("model-") && name.endsWith(".part") }?.forEach { it.delete() }
            if (directory.usableSpace < model.size) throw ModelDownloadError.Storage()
            val connection = (model.url.openConnection() as HttpsURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept-Encoding", "identity")
            }
            try {
                if (connection.responseCode != 200) throw IOException("Model download unavailable")
                connection.inputStream.use { installVerified(it, target(model), model.size, model.sha256, onProgress) }
            } finally {
                connection.disconnect()
            }
        }
    }

    fun delete(model: LocalModel): Boolean = target(model).let { !it.exists() || it.delete() }

    private companion object { val downloads = Mutex() }
}

/** Publish a model only after receiving all bytes and verifying the pinned digest. */
internal suspend fun installVerified(
    input: InputStream, target: File, size: Long, sha256: String, onProgress: suspend (Float) -> Unit
) = withContext(Dispatchers.IO) {
    val partial = File.createTempFile("model-", ".part", target.parentFile)
    try {
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        var checkpoint = 0L
        partial.outputStream().buffered().use { output ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                ensureActive()
                val read = input.read(buffer)
                if (read == -1) break
                total += read
                if (total > size) throw ModelDownloadError.Integrity()
                output.write(buffer, 0, read)
                digest.update(buffer, 0, read)
                if (total - checkpoint >= 4 * 1024 * 1024) {
                    onProgress(total.toFloat() / size)
                    checkpoint = total
                }
            }
        }
        ensureActive()
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        if (total != size || hash != sha256) throw ModelDownloadError.Integrity()
        Files.move(partial.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        Unit
    } finally {
        partial.delete()
    }
}

sealed class ModelDownloadError : IOException() {
    class Storage : ModelDownloadError()
    class Integrity : ModelDownloadError()
}
