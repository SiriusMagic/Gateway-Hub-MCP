package com.zirakusa.gatewayhub.assistant.local

import org.json.JSONObject

/**
 * Layer 3 of Lucía: the model plans with the family's tools, several steps when one is not enough («pon alone» → look
 * for it, then play it). Each step it sees what the earlier ones answered. What only reads runs at once; the first step
 * that changes something stops the plan and waits for the person's «sí» (the card). At most [MAX_STEPS]. Pure apart from
 * [choose] (the model) and [call] (the app).
 */
internal class Planner(
    /** The model's next step, given what was done so far (each line: tool, arguments, answer); null: done or nothing fits. */
    private val choose: suspend (done: List<String>) -> McpSelection?,
    private val call: suspend (McpSelection) -> JSONObject,
    private val readOnly: (McpSelection) -> Boolean,
) {
    sealed interface Outcome {
        /** Nothing fits at all. */
        data object Nothing : Outcome
        /** Only reads: their answers, the last one for Lucía to say. */
        data class Read(val steps: List<Pair<McpSelection, JSONObject>>) : Outcome
        /** A change waits for the «sí», after the reads that prepared it. */
        data class Confirm(val change: McpSelection, val before: List<Pair<McpSelection, JSONObject>>) : Outcome
    }

    suspend fun plan(): Outcome {
        val steps = mutableListOf<Pair<McpSelection, JSONObject>>()
        repeat(MAX_STEPS) {
            val next = choose(steps.map { (s, r) -> line(s, r) }) ?: return done(steps)
            // The same read twice means the model is going round in circles: stop with what it has.
            if (steps.any { (s, _) -> s.tool.name == next.tool.name && s.arguments.toString() == next.arguments.toString() }) return done(steps)
            if (!readOnly(next)) return Outcome.Confirm(next, steps)
            steps += next to call(next)
        }
        return done(steps)
    }

    private fun done(steps: List<Pair<McpSelection, JSONObject>>) = if (steps.isEmpty()) Outcome.Nothing else Outcome.Read(steps)

    companion object {
        const val MAX_STEPS = 3
        /** One step as the model sees it next time; long answers are cut. */
        fun line(s: McpSelection, r: JSONObject) = "${s.tool.name} ${s.arguments} → ${r.toString().take(1_200)}"
    }
}
