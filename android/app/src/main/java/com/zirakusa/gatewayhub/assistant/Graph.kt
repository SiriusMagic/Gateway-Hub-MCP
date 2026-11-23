package com.zirakusa.gatewayhub.assistant

import kotlin.math.max

/** A node of the brain: an input, an app or an action. [x] and [y] are 0–1 on the canvas. */
data class Node(val id: String, val label: String, val layer: Int, val x: Float, val y: Float)
data class Edge(val from: String, val to: String)

/**
 * The brain drawn as a tree: input → app → what that app does (Notas → Conteo, Stone → Música…). Each action hangs from
 * the app it belongs to, so it is clear whose it is; what Lucía answers herself (hora, fecha, mi día, volumen) hangs from
 * «Lucía». Whether the order worked is not a node: the screen marks it (Hecho / Falló). Pure layout; the screen draws it.
 */
object Graph {
    const val VOICE = "in:voice"; const val TEXT = "in:text"; const val LUCIA = "app:lucia"
    fun action(a: Action) = "do:${a.name}"
    fun app(a: FamilyApp) = "app:${a.id}"

    /** The actions shown: opening an app is the app itself, and what was not understood stays at the input. */
    val actions = Action.entries.filter { it != Action.UNKNOWN && it != Action.OPEN }
    /** The parent of an action: its app, or Lucía. */
    fun parent(a: Action) = Catalog.appFor(a)?.let(::app) ?: LUCIA

    /** Every app in the family (each one can be opened), Lucía first; each with its actions under it. */
    fun tree(): List<Pair<Pair<String, String>, List<Action>>> =
        listOf((LUCIA to "Lucía") to actions.filter { Catalog.appFor(it) == null }) +
            Catalog.apps.map { a -> (app(a) to a.name) to actions.filter { Catalog.appFor(it) == a } }

    /** One row per action (an app without actions takes one row), so the drawing grows downwards. */
    fun rows() = tree().sumOf { max(1, it.second.size) }

    fun nodes(labels: (Action) -> String): List<Node> {
        val total = rows().toFloat()
        val out = mutableListOf(Node(VOICE, "Voz", 0, 0.06f, 0.5f - 1.5f / total), Node(TEXT, "Texto", 0, 0.06f, 0.5f + 1.5f / total))
        var row = 0
        tree().forEach { (appNode, children) ->
            val size = max(1, children.size)
            out += Node(appNode.first, appNode.second, 1, 0.32f, (row + size / 2f) / total)
            children.forEachIndexed { i, a -> out += Node(action(a), labels(a), 2, 0.64f, (row + i + 0.5f) / total) }
            row += size
        }
        return out
    }

    fun edges(): List<Edge> {
        val tree = tree()
        val ins = listOf(VOICE, TEXT).flatMap { i -> tree.map { Edge(i, it.first.first) } }
        val branches = tree.flatMap { (appNode, children) -> children.map { Edge(appNode.first, action(it)) } }
        return ins + branches
    }

    /** The lit path of an order: input → app → action (the volume, when said with another order, lights up too). */
    fun path(byVoice: Boolean, command: Command, app: FamilyApp?, volumeToo: Boolean): List<String> {
        val input = if (byVoice) VOICE else TEXT
        val a = command.action
        return when (a) {
            Action.UNKNOWN -> listOf(input)
            Action.OPEN -> listOfNotNull(input, app?.let(::app))
            else -> listOf(input, parent(a), action(a))
        } + listOfNotNull(action(Action.VOLUME).takeIf { volumeToo && a != Action.VOLUME })
    }

    /** How many orders went through each node: the bright ones are the most used. */
    fun heat(turns: List<Turn>): Map<String, Int> = turns.flatMap { it.path }.groupingBy { it }.eachCount()

    /** Health of the last orders: understood, done, and the average time. */
    data class Health(val total: Int, val understood: Int, val done: Int, val averageMs: Long)
    fun health(turns: List<Turn>) = Health(turns.size, turns.count { it.command.action != Action.UNKNOWN }, turns.count { it.ok },
        if (turns.isEmpty()) 0 else turns.sumOf { it.millis } / max(1, turns.size))
}
