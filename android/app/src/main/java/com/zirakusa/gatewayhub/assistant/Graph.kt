package com.zirakusa.gatewayhub.assistant

import kotlin.math.max

/** A node of the brain: an input, an action, an app or a result. [x] and [y] are 0–1 on the canvas. */
data class Node(val id: String, val label: String, val layer: Int, val x: Float, val y: Float)
data class Edge(val from: String, val to: String)

/**
 * The brain drawn as a network: four columns (input → action → app → result) whose nodes light up along the path of each
 * order, so it can be seen what the assistant understood and which app did it. Only the edges that exist are drawn
 * (each action to its app). Pure layout; the screen animates it.
 */
object Graph {
    const val VOICE = "in:voice"; const val TEXT = "in:text"; const val OK = "out:ok"; const val FAIL = "out:fail"
    fun action(a: Action) = "do:${a.name}"
    fun app(a: FamilyApp) = "app:${a.id}"

    /** The actions shown (UNKNOWN goes straight to «falló»). */
    val actions = Action.entries.filter { it != Action.UNKNOWN }
    /** Apps that some action uses, plus the ones opened by name. */
    val apps = Catalog.apps.filter { a -> actions.any { Catalog.appFor(it) == a } } + listOf("calculadora", "notas", "luma", "filem").map(Catalog::byId)

    fun nodes(labels: (Action) -> String): List<Node> {
        fun column(layer: Int, ids: List<Pair<String, String>>) = ids.mapIndexed { i, (id, label) ->
            Node(id, label, layer, 0.08f + layer * 0.28f, (i + 1f) / (ids.size + 1f))
        }
        return column(0, listOf(VOICE to "Voz", TEXT to "Texto")) +
            column(1, actions.map { action(it) to labels(it) }) +
            column(2, apps.distinct().map { app(it) to it.name }) +
            column(3, listOf(OK to "Hecho", FAIL to "Falló"))
    }

    fun edges(): List<Edge> {
        val ins = listOf(VOICE, TEXT).flatMap { i -> actions.map { Edge(i, action(it)) } }
        val toApps = actions.mapNotNull { a -> Catalog.appFor(a)?.let { Edge(action(a), app(it)) } } +
            apps.distinct().filter { a -> actions.none { Catalog.appFor(it) == a } }.map { Edge(action(Action.OPEN), app(it)) } +
            listOf(Edge(action(Action.OPEN), app(Catalog.byId("stone"))))
        val outs = apps.distinct().flatMap { listOf(Edge(app(it), OK), Edge(app(it), FAIL)) } +
            actions.filter { Catalog.appFor(it) == null && it != Action.OPEN }.flatMap { listOf(Edge(action(it), OK), Edge(action(it), FAIL)) } +
            listOf(Edge(TEXT, FAIL), Edge(VOICE, FAIL))
        return (ins + toApps + outs).distinct()
    }

    /** How many orders went through each node: the bright ones are the most used. */
    fun heat(turns: List<Turn>): Map<String, Int> = turns.flatMap { it.path }.groupingBy { it }.eachCount()

    /** Health of the last orders: understood, done, and the average time. */
    data class Health(val total: Int, val understood: Int, val done: Int, val averageMs: Long)
    fun health(turns: List<Turn>) = Health(turns.size, turns.count { it.command.action != Action.UNKNOWN }, turns.count { it.ok },
        if (turns.isEmpty()) 0 else turns.sumOf { it.millis } / max(1, turns.size))
}
