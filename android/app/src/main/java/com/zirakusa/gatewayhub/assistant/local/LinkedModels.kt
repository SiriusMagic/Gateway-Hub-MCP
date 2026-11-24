package com.zirakusa.gatewayhub.assistant.local

import org.json.JSONArray
import org.json.JSONObject

/** How a model family expects a conversation written. Guessed from the file's name; ChatML when unknown. */
enum class Template {
    CHATML, QWEN3, GEMMA, LLAMA3, PHI3, MISTRAL;

    companion object {
        fun guess(fileName: String): Template {
            val n = fileName.lowercase()
            return when {
                "qwen3" in n -> QWEN3
                "qwen" in n || "chatml" in n || "hermes" in n || "smollm" in n -> CHATML
                "gemma" in n -> GEMMA
                Regex("llama[-_ .]?3").containsMatchIn(n) -> LLAMA3
                "phi-3" in n || "phi3" in n || "phi-4" in n || "phi4" in n -> PHI3
                "mistral" in n || "mixtral" in n -> MISTRAL
                else -> CHATML
            }
        }
    }
}

/**
 * A .gguf the person already has (Descargas, a memory card…), used where it is: nothing is copied, so a model takes its
 * space once. [uri] is the document Android lets the Hub read; [name] and [size] as shown.
 */
data class LinkedModel(val uri: String, val name: String, val size: Long, val template: Template) {
    val id get() = "linked:$uri"
    val sizeLabel get() = "%.2f GB".format(java.util.Locale("es"), size / 1_000_000_000.0)

    companion object {
        /** Only GGUF files of a sensible size (llama.cpp reads nothing else). */
        fun accepts(name: String, size: Long) = name.lowercase().endsWith(".gguf") && size in 50_000_000L..40_000_000_000L

        fun save(list: List<LinkedModel>): String = JSONArray(list.map {
            JSONObject().put("uri", it.uri).put("name", it.name).put("size", it.size).put("template", it.template.name)
        }).toString()

        fun load(text: String?): List<LinkedModel> = runCatching {
            val a = JSONArray(text ?: return emptyList())
            (0 until a.length()).mapNotNull { a.optJSONObject(it) }.mapNotNull { o ->
                val name = o.optString("name"); val size = o.optLong("size")
                if (o.optString("uri").isBlank() || !accepts(name, size)) null
                else LinkedModel(o.getString("uri"), name, size, Template.entries.firstOrNull { it.name == o.optString("template") } ?: Template.guess(name))
            }
        }.getOrDefault(emptyList())
    }
}
