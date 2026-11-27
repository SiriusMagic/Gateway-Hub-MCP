package com.zirakusa.gatewayhub.assistant.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.zirakusa.gatewayhub.assistant.VoiceModelFile
import com.zirakusa.gatewayhub.assistant.WhisperModel
import com.zirakusa.gatewayhub.assistant.compatibleWhisper
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
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

    val template get() = if (this == QWEN3) Template.QWEN3 else Template.CHATML
    val url get() = URI("https://huggingface.co/Qwen/$source").toURL()
    val fileName get() = source.substringAfterLast('/')
}

class ModelStore(private val context: Context) {
    private val directory = File(context.filesDir, "lucia-models")
    private fun target(model: LocalModel) = File(directory, model.fileName)

    fun installed(model: LocalModel): File? = target(model).takeIf { it.isFile && it.length() == model.size }

    suspend fun download(model: LocalModel, onProgress: suspend (Float) -> Unit) {
        transfer(model.url, target(model), model.size, model.sha256, onProgress)
    }

    internal suspend fun download(model: RepositoryModel, onProgress: suspend (Float) -> Unit): File {
        val file = File(File(directory, "hugging-face"), "${model.sha256}.gguf")
        transfer(model.url, file, model.size, model.sha256, onProgress)
        return file
    }

    private fun whisperTarget(model: WhisperModel) = File(File(directory, "voice"), model.fileName)

    internal fun whisperFile(model: WhisperModel): File? = whisperTarget(model).takeIf { it.isFile && it.length() == model.bytes }

    internal suspend fun downloadWhisper(model: WhisperModel, onProgress: suspend (Float) -> Unit) = withContext(Dispatchers.IO) {
        transfer(model.url, whisperTarget(model), model.bytes, model.digest, onProgress)
    }

    internal fun deleteWhisper(model: WhisperModel): Boolean = whisperTarget(model).let { !it.exists() || it.delete() }

    private val importedVoice get() = File(File(directory, "voice"), "imported")

    internal fun voiceModels(): List<File> = importedVoice.listFiles()
        .orEmpty().filter { it.isFile && it.name.endsWith(".bin") }.sortedBy { it.name.substringAfter('_').lowercase() }

    internal suspend fun downloadVoice(model: RepositoryModel, onProgress: suspend (Float) -> Unit): File {
        if (!acceptsVoiceModel(model.name, model.size)) throw ModelDownloadError.Format()
        val target = voiceTarget(model.sha256, model.name)
        transfer(model.url, target, model.size, model.sha256, onProgress, ::compatibleWhisper)
        return target
    }

    internal suspend fun importVoice(uri: Uri, onProgress: suspend (Float) -> Unit): File = withContext(Dispatchers.IO) {
        downloads.withLock {
            require(uri.scheme == "content")
            val resolver = context.contentResolver
            val (name, size) = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameColumn < 0) return@use null
                    val bytes = if (sizeColumn < 0 || cursor.isNull(sizeColumn)) null else cursor.getLong(sizeColumn).takeIf { it > 0 }
                    (cursor.getString(nameColumn) ?: return@use null) to bytes
                } ?: throw ModelDownloadError.Format()
            if (!acceptsVoiceModel(name, size ?: VoiceModelFile.MIN_BYTES)) throw ModelDownloadError.Format()
            val target = voiceTarget(UUID.randomUUID().toString(), name)
            prepare(target, size ?: VoiceModelFile.MIN_BYTES)
            resolver.openInputStream(uri)?.use {
                installVerified(it, target, size, null, ::compatibleWhisper, VoiceModelFile.MAX_BYTES, onProgress)
            } ?: throw IOException()
            target
        }
    }

    private fun voiceTarget(id: String, name: String): File {
        val label = name.substringBeforeLast('.').replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").take(60)
        return File(importedVoice, "${id}_${label}.bin")
    }

    private fun prepare(file: File, required: Long) {
        val parent = requireNotNull(file.parentFile)
        if (!parent.isDirectory && !parent.mkdirs()) throw ModelDownloadError.Storage()
        parent.listFiles { _, name -> name.startsWith("model-") && name.endsWith(".part") }?.forEach { it.delete() }
        val storage = context.getSystemService(android.os.storage.StorageManager::class.java)
        val free = runCatching { storage.getAllocatableBytes(storage.getUuidForPath(parent)) }.getOrDefault(0L)
        if (free < required) throw ModelDownloadError.Storage()
    }

    internal fun repositoryFile(uri: android.net.Uri): File? {
        if (uri.scheme != "file") return null
        val path = uri.path ?: return null
        val file = File(path)
        return file.takeIf { it.parentFile == File(directory, "hugging-face") && it.name.matches(Regex("[0-9a-f]{64}\\.gguf")) }
    }

    private suspend fun transfer(
        url: java.net.URL, file: File, size: Long, sha: String, onProgress: suspend (Float) -> Unit,
        validate: (File) -> Boolean = { true }
    ) =
        withContext(Dispatchers.IO) {
            downloads.withLock {
                if (file.isFile && file.length() == size) return@withLock
                prepare(file, size)
                val connection = (url.openConnection() as HttpsURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    setRequestProperty("Accept-Encoding", "identity")
                }
                try {
                    if (connection.responseCode != 200) throw IOException("Model download unavailable")
                    connection.inputStream.use { installVerified(it, file, size, sha, validate, onProgress = onProgress) }
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
    input: InputStream, target: File, size: Long?, sha256: String?,
    validate: (File) -> Boolean = { true }, maxBytes: Long = size ?: Long.MAX_VALUE, onProgress: suspend (Float) -> Unit
) = withContext(Dispatchers.IO) {
    val partial = File.createTempFile("model-", ".part", target.parentFile)
    try {
        val digest = sha256?.let { MessageDigest.getInstance("SHA-256") }
        var total = 0L
        var checkpoint = 0L
        partial.outputStream().buffered().use { output ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                ensureActive()
                val read = input.read(buffer)
                if (read == -1) break
                total += read
                if (total > maxBytes) throw ModelDownloadError.Integrity()
                output.write(buffer, 0, read)
                digest?.update(buffer, 0, read)
                if (size != null && total - checkpoint >= 4 * 1024 * 1024) {
                    onProgress(total.toFloat() / size)
                    checkpoint = total
                }
            }
        }
        ensureActive()
        val hash = digest?.digest()?.joinToString("") { "%02x".format(it) }
        if ((size != null && total != size) || hash != sha256) throw ModelDownloadError.Integrity()
        if (!validate(partial)) throw ModelDownloadError.Format()
        ensureActive()
        Files.move(partial.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        Unit
    } finally {
        partial.delete()
    }
}

internal fun acceptsVoiceModel(name: String, bytes: Long) =
    name.endsWith(".bin", ignoreCase = true) && bytes in VoiceModelFile.MIN_BYTES..VoiceModelFile.MAX_BYTES

sealed class ModelDownloadError : IOException() {
    class Format : ModelDownloadError()
    class Storage : ModelDownloadError()
    class Integrity : ModelDownloadError()
}
