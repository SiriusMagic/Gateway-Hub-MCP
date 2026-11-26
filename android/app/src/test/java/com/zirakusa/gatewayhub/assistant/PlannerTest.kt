package com.zirakusa.gatewayhub.assistant

import com.zirakusa.gatewayhub.assistant.local.McpSelection
import com.zirakusa.gatewayhub.assistant.local.McpTool
import com.zirakusa.gatewayhub.assistant.local.Planner
import com.zirakusa.gatewayhub.assistant.local.ToolGrammar
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Layer 3: several steps with what each answered, a change stops for the «sí», and the grammar only allows real tools. */
@RunWith(RobolectricTestRunner::class)
class PlannerTest {
    private fun tool(name: String, read: Boolean) = McpTool(name, name, "", JSONObject().put("type", "object"), read)
    private val search = tool("stone.library_search", true)
    private val play = tool("stone.player_play_songs", false)
    private fun sel(t: McpTool, args: JSONObject = JSONObject()) = McpSelection(t, args)

    @Test fun searchThenPlayWaitsForTheYes() = runBlocking {
        val seen = mutableListOf<List<String>>()
        val steps = listOf(sel(search, JSONObject().put("query", "alone")), sel(play, JSONObject().put("songIds", "a2")))
        val outcome = Planner({ done -> seen += done; steps.getOrNull(done.size) },
            { JSONObject().put("results", JSONArray().put(JSONObject().put("id", "a2").put("title", "Alone"))) },
            { it.tool.readOnly }).plan()
        assertTrue(outcome is Planner.Outcome.Confirm)
        outcome as Planner.Outcome.Confirm
        assertEquals("stone.player_play_songs", outcome.change.tool.name); assertEquals(1, outcome.before.size)
        assertTrue("the model saw the search's answer", seen[1].single().contains("\"id\":\"a2\""))
    }

    @Test fun readsRunAtOnceAndLoopsStop() = runBlocking {
        var calls = 0
        val again = sel(search, JSONObject().put("query", "x"))
        val outcome = Planner({ again }, { calls++; JSONObject().put("total", 0) }, { true }).plan()
        assertTrue(outcome is Planner.Outcome.Read); assertEquals(1, calls)           // the same read twice: it stops
        assertEquals(Planner.Outcome.Nothing, Planner({ null }, { JSONObject() }, { true }).plan())
        var n = 0
        val many = Planner({ sel(search, JSONObject().put("query", "q${n++}")) }, { JSONObject() }, { true }).plan() as Planner.Outcome.Read
        assertEquals(Planner.MAX_STEPS, many.steps.size)
    }

    @Test fun validatesNamesAndResolvedIdsBeforeExecutingModelTools() {
        val query = "Pon música Canción del Faro"
        fun toolWithText(tool: McpTool, field: String) = tool.copy(schema = JSONObject()
            .put("type", "object").put("properties", JSONObject().put(field, JSONObject().put("type", "string"))))
        val searchTool = toolWithText(search, "query")
        val playTool = toolWithText(play, "songIds")
        assertTrue(IntentPlan.supportsTool(query, sel(searchTool, JSONObject().put("query", "Canción del Faro")), emptyList()))
        assertFalse(IntentPlan.supportsTool(query, sel(searchTool, JSONObject().put("query", "Otro título")), emptyList()))
        val selected = sel(playTool, JSONObject().put("songIds", "a2"))
        assertFalse(IntentPlan.supportsTool(query, selected, emptyList()))
        assertTrue(IntentPlan.supportsTool(query, selected, listOf("""{"id":"a2","title":"Canción del Faro"}""")))
        val plan = IntentPlan.forTools(query, Action.PHONE_MCP, ModelSource("model", "prompt-v1"), listOf("stone" to selected))
        assertEquals(query, plan.original)
        assertEquals("stone", plan.steps.single().app?.id)
        assertEquals("prompt-v1", plan.source?.template)
    }

    @Test fun theGrammarOnlyNamesRealTools() {
        val g = ToolGrammar.forTools(listOf("stone.library_search", "bad name\"", "notas.notes_list"))
        assertTrue(g.startsWith("root ::= \"{}\" | ")); assertTrue(g.contains("\"\\\"stone.library_search\\\"\""))
        assertTrue(g.contains("\"\\\"notas.notes_list\\\"\"")); assertFalse(g.contains("bad name"))
        assertEquals("root ::= \"{}\"\n", ToolGrammar.forTools(emptyList()))
        assertTrue(ToolGrammar.orders.contains("ordenes"))
    }
}
