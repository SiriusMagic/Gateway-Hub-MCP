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
        template: Template,
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
            val session = NativeLlama.begin(weights, prompt(template, messages).toByteArray(),
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

        internal fun prompt(model: LocalModel, messages: List<ChatMessage>) = prompt(model.template, messages)

        /** Special markers people could type are removed, so a message can't pretend to be another turn. */
        private val markers = Regex("<\\|[a-z_]+\\|>|<(?:start|end)_of_turn>|</?s>|\\[/?INST]|<bos>|<eos>")

        internal fun prompt(template: Template, messages: List<ChatMessage>): String = buildString {
            val safe = messages.map { it.copy(text = it.text.replace(markers, "")) }
            when (template) {
                // Both curated models use ChatML. Qwen3's empty thinking block selects its non-thinking mode.
                Template.CHATML, Template.QWEN3 -> {
                    for ((role, text) in safe) append("<|im_start|>").append(role).append('\n').append(text).append("<|im_end|>\n")
                    append("<|im_start|>assistant\n")
                    if (template == Template.QWEN3) append("<think>\n\n</think>\n\n")
                }
                Template.LLAMA3 -> {
                    append("<|begin_of_text|>")
                    for ((role, text) in safe) append("<|start_header_id|>").append(role).append("<|end_header_id|>\n\n").append(text).append("<|eot_id|>")
                    append("<|start_header_id|>assistant<|end_header_id|>\n\n")
                }
                Template.PHI3 -> {
                    for ((role, text) in safe) append("<|").append(role).append("|>\n").append(text).append("<|end|>\n")
                    append("<|assistant|>\n")
                }
                // Gemma and Mistral have no system turn: it goes before the first message of the person.
                Template.GEMMA, Template.MISTRAL -> {
                    val system = safe.filter { it.role == "system" }.joinToString("\n\n") { it.text }
                    var first = true
                    if (template == Template.GEMMA) append("<bos>") else append("<s>")
                    for ((role, text) in safe.filter { it.role != "system" }) {
                        val said = if (first && role == "user" && system.isNotBlank()) "$system\n\n$text" else text
                        if (role == "user") first = false
                        if (template == Template.GEMMA) append("<start_of_turn>").append(if (role == "assistant") "model" else "user").append('\n').append(said).append("<end_of_turn>\n")
                        else if (role == "user") append("[INST] ").append(said).append(" [/INST]") else append(said).append("</s>")
                    }
                    if (template == Template.GEMMA) append("<start_of_turn>model\n")
                }
            }
        }
    }
}
