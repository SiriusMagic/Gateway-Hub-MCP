package com.zirakusa.gatewayhub.assistant.local

import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class ChatMessage(val role: String, val text: String)

enum class ConversationTone(val instruction: String) {
    NATURAL("Habla de manera natural y clara."),
    WARM("Habla con calidez y empatía; reconoce los sentimientos que la persona expresa."),
    DIRECT("Habla de manera directa y breve.")
}

internal object NativeLlama {
    init { System.loadLibrary("lucia_llama") }

    external fun load(path: String): Long
    external fun unload(model: Long)
    external fun begin(model: Long, prompt: ByteArray, threads: Int, limit: Int, deterministic: Boolean): Long
    external fun prefill(session: Long): Boolean
    external fun next(session: Long): ByteArray?
    external fun end(session: Long)
}

/** Serializes native work so closing, changing models, or cancelling cannot free an in-use handle. */
class LocalLanguageModel {
    private val mutex = Mutex()
    private var loadedFile: File? = null
    private var weights = 0L

    suspend fun generate(
        model: LocalModel,
        file: File,
        messages: List<ChatMessage>,
        deterministic: Boolean = false,
        onText: suspend (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureActive()
            if (loadedFile != file || weights == 0L) {
                release()
                weights = NativeLlama.load(file.absolutePath)
                check(weights != 0L)
                loadedFile = file
            }
            ensureActive()
            val session = NativeLlama.begin(weights, prompt(model, messages).toByteArray(),
                Runtime.getRuntime().availableProcessors().coerceIn(1, 4), if (deterministic) 512 else 320, deterministic)
            check(session != 0L)
            try {
                do { ensureActive() } while (NativeLlama.prefill(session))
                val output = ByteArrayOutputStream()
                var last = ""
                while (true) {
                    ensureActive()
                    val piece = NativeLlama.next(session) ?: break
                    output.write(piece)
                    val text = visible(output.toString(Charsets.UTF_8.name()))
                    if (text != last) {
                        last = text
                        onText(text)
                    }
                }
                last
            } finally {
                NativeLlama.end(session)
            }
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) { mutex.withLock { release() } }

    private fun release() {
        if (weights != 0L) NativeLlama.unload(weights)
        loadedFile = null
        weights = 0L
    }

    private fun visible(text: String): String {
        val response = text.trimStart().trimEnd('\uFFFD')
        if (response.startsWith("<think>")) return response.substringAfter("</think>", "").trimStart()
        return response.takeUnless { it.isNotEmpty() && "<think>".startsWith(it) }.orEmpty()
    }

    companion object {
        val supported: Boolean get() = Build.SUPPORTED_64_BIT_ABIS.contains("arm64-v8a") && android.os.Process.is64Bit()

        internal fun prompt(model: LocalModel, messages: List<ChatMessage>): String = buildString {
            // Both curated models use ChatML. Qwen3's empty thinking block selects its non-thinking mode.
            for ((role, text) in messages) {
                val safe = text.replace("<|im_start|>", "").replace("<|im_end|>", "").replace("<|endoftext|>", "")
                append("<|im_start|>").append(role).append('\n').append(safe).append("<|im_end|>\n")
            }
            append("<|im_start|>assistant\n")
            if (model == LocalModel.QWEN3) append("<think>\n\n</think>\n\n")
        }
    }
}
