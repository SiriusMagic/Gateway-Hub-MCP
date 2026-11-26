package com.zirakusa.gatewayhub.assistant

import com.zirakusa.gatewayhub.assistant.local.McpArguments
import com.zirakusa.gatewayhub.assistant.local.McpSelection
import java.util.UUID

internal data class ModelSource(val model: String, val template: String)

internal data class IntentStep(val phrase: String, val command: Command) {
    val app get() = when (command.action) {
        Action.OPEN, Action.TOOL, Action.PHONE_MCP -> command["app"]?.let { id -> Catalog.apps.firstOrNull { it.id == id } }
        else -> Catalog.appFor(command.action)
    }

    val dependencies: List<String> get() = if (command.action == Action.PLAY_MUSIC) buildList {
        if (command["query"] != null) add("search")
        if (command["volume"] != null) add("volume")
        if (command["equalizer"] != null) add("equalizer")
        add("play")
        if (command["background"] == "1") add("return")
    } else emptyList()
}

internal data class IntentPlan(
    val original: String,
    val steps: List<IntentStep>,
    val source: ModelSource? = null,
    val id: String = UUID.randomUUID().toString()
) {
    companion object {
        const val VERSION = "2"
        // Limit fallback to verbatim arguments. Other model-assisted actions still have their own tool review.
        private val originalSlots = setOf("who", "query", "text", "title", "name", "newname")
        private fun preserves(source: String, value: String): Boolean {
            val target = Words.normalize(value)
            return target.isEmpty() || " $target " in " ${Words.normalize(source)} "
        }

        /** Tool discovery supplies the supported action and schema. A previous read can supply an identifier. */
        fun supportsTool(original: String, selection: McpSelection, previousReads: List<String>): Boolean {
            if (!McpArguments.valid(selection.tool.schema, selection.arguments)) return false
            return selection.arguments.keys().asSequence().all { key ->
                val value = selection.arguments.opt(key) ?: return@all false
                when {
                    key.lowercase() in originalSlots && value is String -> preserves(original, value)
                    key.endsWith("Id") || key.endsWith("Ids") -> when (value) {
                        is String -> value.split(',').all { id ->
                            id.isNotBlank() && (preserves(original, id.trim()) || previousReads.any { preserves(it, id.trim()) })
                        }
                        is Number -> preserves(original, value.toString()) || previousReads.any { preserves(it, value.toString()) }
                        else -> false
                    }
                    else -> true
                }
            }
        }

        fun forTools(original: String, action: Action, source: ModelSource, steps: List<Pair<String?, McpSelection>>): IntentPlan =
            IntentPlan(original, steps.map { (app, selection) ->
                val parameters = mapOf("tool" to selection.tool.name, "arguments" to selection.arguments.toString())
                IntentStep(original, Command(action, if (app == null) parameters else parameters + ("app" to app)))
            }, source)

        fun validated(original: String, orders: List<String>, parse: (String) -> Command, source: ModelSource? = null): IntentPlan? {
            if (orders.isEmpty()) return null
            val steps = orders.map { IntentStep(it, parse(it)) }
            if (steps.any { (phrase, command) ->
                command.action == Action.UNKNOWN ||
                    command["volume"]?.let { it.toIntOrNull() !in 0..100 } == true ||
                    command["background"] !in setOf(null, "1") ||
                    command["equalizer"] !in setOf(null, "0", "1") ||
                    command.action == Action.PLAY_MUSIC && MusicOrder.parse(phrase)?.let { it != command } == true ||
                    source != null && (command.action == Action.TOOL || command.action in setOf(Action.ANSWER, Action.HANG_UP) ||
                        command.slots.any { (key, value) -> key in originalSlots && !preserves(original, value) })
            }) return null
            return IntentPlan(original, steps, source)
        }
    }
}

internal data class IntentEvidence(val plan: IntentPlan, val step: Int) {
    val id get() = "${plan.id}:$step"
}
