package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Hearing
import com.zirakusa.gatewayhub.assistant.Parser
import com.zirakusa.gatewayhub.assistant.local.McpSelection
import com.zirakusa.gatewayhub.assistant.local.McpTool
import com.zirakusa.gatewayhub.assistant.local.StoneRequests
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhoneMcpTest {
    @Test fun routesStoneQuestionsButKeepsDirectCommandsAndConversation() {
        val library = "¿Qué canciones de Daft Punk hay en Stone?"
        assertTrue(StoneRequests.matches(library, Parser.parse(library)))
        assertTrue(StoneRequests.matches("¿qué suena?", Parser.parse("¿qué suena?")))
        listOf("abre Stone", "pausa la música", "reanuda la música", "cuéntame una historia").forEach {
            assertFalse(StoneRequests.matches(it, Parser.parse(it)))
        }
        assertEquals(library, Hearing.pick("que canciones en stone", library, conversational = true))
        assertEquals("qué suena", StoneRequests.explicit("MCP local: qué suena"))
    }

    @Test fun selectionIsLimitedToTheDiscoveredSchema() {
        val tool = McpTool.parse(JSONObject(
            """{"name":"library_search","annotations":{"readOnlyHint":true},"inputSchema":{"type":"object","properties":{"query":{"type":"string"}},"additionalProperties":false}}"""
        ))!!
        fun choose(response: String) = McpSelection.parse(response, listOf(tool))
        assertNotNull(choose("""{"tool":"library_search","arguments":{"query":"Daft Punk"}}"""))
        assertNull(choose("""{"tool":"player_pause","arguments":{}}"""))
        assertNull(choose("""{"tool":"library_search","arguments":{"query":5}}"""))
    }
}
