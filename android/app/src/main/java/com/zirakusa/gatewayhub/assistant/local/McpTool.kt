package com.zirakusa.gatewayhub.assistant.local

import java.text.Normalizer
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

internal data class McpTool(val name: String, val title: String, val description: String, val schema: JSONObject, val readOnly: Boolean) {
    fun describe(): String {
        val properties = schema.optJSONObject("properties") ?: JSONObject()
        val required = schema.optJSONArray("required").strings().toSet()
        val fields = properties.keys().asSequence().joinToString(", ") { key ->
            val rule = properties.optJSONObject(key)
            val type = rule?.optString("type").orEmpty()
            val choices = rule?.optJSONArray("enum")?.toString().orEmpty()
            val hint = rule?.optString("description").orEmpty().take(75)
            "$key:${type.ifBlank { "valor" }}${if (key in required) " (obligatorio)" else ""}$choices $hint".trim()
        }
        return "$name — $title. ${description.take(150)} Campos: {$fields}"
    }

    companion object {
        fun parse(data: JSONObject): McpTool? {
            val name = data.optString("name")
            if (!name.matches(Regex("[A-Za-z0-9_./-]{1,128}"))) return null
            val schema = data.optJSONObject("inputSchema") ?: return null
            val annotations = data.optJSONObject("annotations")
            return McpTool(name, annotations?.optString("title").orEmpty().ifBlank { name },
                data.optString("description"), schema, annotations?.opt("readOnlyHint") == true &&
                    annotations.opt("destructiveHint") != true)
        }
    }
}

internal data class McpSelection(val tool: McpTool, val arguments: JSONObject) {
    companion object {
        fun candidates(query: String, tools: List<McpTool>): List<McpTool> {
            val words = words(query) - setOf("para", "quiero", "puedes", "favor", "necesito", "unas", "unos")
            var budget = 4_500
            return tools.sortedByDescending {
                val titles = words(it.name + " " + it.title)
                val details = words(it.description)
                words.sumOf { word -> when {
                    titles.any { it == word } -> 8
                    titles.any { it.startsWith(word) || word.startsWith(it) } -> 3
                    word in details -> 1
                    else -> 0
                } }
            }.filter {
                val length = it.describe().length
                (length <= budget).also { fits -> if (fits) budget -= length }
            }.take(16)
        }

        fun parse(response: String, tools: List<McpTool>): McpSelection? {
            val text = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val tokener = JSONTokener(text)
            val payload = runCatching { tokener.nextValue() as? JSONObject }.getOrNull() ?: return null
            if (tokener.nextClean() != '\u0000' || payload.keys().asSequence().any { it !in setOf("tool", "arguments") }) return null
            val tool = tools.singleOrNull { it.name == payload.opt("tool") } ?: return null
            val arguments = payload.optJSONObject("arguments") ?: return null
            return McpSelection(tool, arguments).takeIf { McpArguments.valid(tool.schema, arguments) }
        }

        private fun words(text: String): Set<String> = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "").split(Regex("[^a-z0-9]+")).filter { it.length > 2 }.toSet()
    }
}

internal object McpArguments {
    fun valid(schema: JSONObject, arguments: JSONObject) = matches(schema, arguments, 0)

    private fun matches(schema: JSONObject, value: Any?, depth: Int): Boolean {
        if (depth > 12 || value == null || value == JSONObject.NULL) return false
        val choices = schema.optJSONArray("enum")
        if (choices != null && (0 until choices.length()).none { choices.opt(it) == value }) return false
        return when (schema.optString("type")) {
            "object", "" -> if (value is JSONObject) {
                val properties = schema.optJSONObject("properties") ?: JSONObject()
                schema.optJSONArray("required").strings().all { value.has(it) && !value.isNull(it) } &&
                    value.keys().asSequence().all { key ->
                        val rule = properties.optJSONObject(key) ?: schema.optJSONObject("additionalProperties")
                        if (rule != null) matches(rule, value.opt(key), depth + 1)
                        else schema.opt("additionalProperties") == true
                    }
            } else schema.optString("type").isEmpty()
            "array" -> value is JSONArray && value.length() in schema.optInt("minItems", 0)..schema.optInt("maxItems", 128) &&
                (0 until value.length()).all { index ->
                    schema.optJSONObject("items")?.let { matches(it, value.opt(index), depth + 1) } ?: true
                }
            "string" -> value is String && value.length in schema.optInt("minLength", 0)..schema.optInt("maxLength", Int.MAX_VALUE)
            "boolean" -> value is Boolean
            "integer", "number" -> value is Number && value.toDouble().let {
                it.isFinite() && (schema.optString("type") != "integer" || it % 1 == 0.0) &&
                    it >= schema.optDouble("minimum", Double.NEGATIVE_INFINITY) && it <= schema.optDouble("maximum", Double.POSITIVE_INFINITY)
            }
            else -> false
        }
    }
}

private fun JSONArray?.strings() = if (this == null) emptyList() else (0 until length()).map { getString(it) }
