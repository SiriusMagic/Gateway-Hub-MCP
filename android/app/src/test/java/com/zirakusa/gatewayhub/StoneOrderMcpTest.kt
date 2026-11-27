package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.AssistantState
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.FamilyApp
import com.zirakusa.gatewayhub.assistant.Hands
import com.zirakusa.gatewayhub.assistant.StoneOrder
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StoneOrderMcpTest {
    private class Stone : FakeHands() {
        val calls = mutableListOf<Triple<String, String, String>>()
        var reply: JSONObject? = JSONObject("""{"status":"playing","track":{"title":"New Americana"},
            "steps":{"search":{"code":"OK"},"volume":{"code":"OK","requested":50,"actual":50},"play":{"code":"OK"}}}""")
        var error: Hands.ToolError? = null
        override fun tracedTool(app: FamilyApp, name: String, args: JSONObject, id: String): JSONObject? {
            assertEquals("stone", app.id)
            calls += Triple(id, name, args.toString())
            error?.let { throw it }
            return reply
        }
    }

    @Test fun voiceAndTextSendTheSameTitleAndVolumeOnce() {
        val stone = Stone()
        val luna = AssistantState(Brain(stone))
        luna.send("pon New Americana al 50 %", byVoice = false)
        luna.heard("Pon New Americana al cincuenta por ciento.", final = true)
        assertEquals(2, stone.calls.size)
        assertEquals(stone.calls[0].second to stone.calls[0].third, stone.calls[1].second to stone.calls[1].third)
        assertEquals(StoneOrder.PLAY, stone.calls[0].second)
        val args = JSONObject(stone.calls[0].third)
        assertEquals("New Americana", args.getString("titulo"))
        assertEquals(50, args.getInt("volumen"))
        assertNotEquals(UUID.fromString(stone.calls[0].first), UUID.fromString(stone.calls[1].first))
        assertEquals(stone.calls[1].first, luna.last?.mcp?.id)
        assertEquals("Pon New Americana al cincuenta por ciento.", luna.lastTranscript)
        assertTrue(luna.turns.all { it.ok })
        assertTrue(stone.done.isEmpty())
    }

    @Test fun missingReplyAndKnownFailureNeverRepeatOrChangeVolumeDirectly() {
        val stone = Stone().apply { reply = null }
        val luna = AssistantState(Brain(stone))
        luna.send("pon New Americana al 50 %", false)
        assertEquals("RESULT_UNCONFIRMED", luna.last?.mcp?.code)
        assertTrue(luna.last?.reply?.args?.first()?.startsWith("Resultado sin confirmar") == true)
        assertEquals(1, stone.calls.size)
        stone.error = Hands.ToolError("LOCAL_DISABLED", "")
        luna.send("pon New Americana al 50 %", false)
        assertEquals("permiso_local", luna.last?.mcp?.stage)
        assertEquals(2, stone.calls.size)
        assertTrue(stone.done.isEmpty())
    }

    @Test fun emptyMatchesReportMissingTitle() {
        val stone = Stone().apply {
            reply = JSONObject("""{"status":"failed","matches":[],"steps":{"search":{"code":"TRACK_NOT_FOUND"}}}""")
        }
        val turn = Brain(stone).handle("Pon New Americana al 50 %")
        assertEquals("TRACK_NOT_FOUND", turn.mcp?.code)
        assertEquals("No encontré «New Americana» en Stone.", turn.reply.args.single())
        assertEquals(1, stone.calls.size)
    }
    @Test fun calculationKeepsTheMusicModifiersAndOneMcpCall() {
        val stone = Stone()
        val luna = AssistantState(Brain(stone))
        luna.send("Calcula dos más 2 y pon música en segundo plano New Americana con el volumen a 50 y el ecualizador activo", false)
        assertEquals(listOf("4"), luna.turns.first().reply.args)
        val args = JSONObject(stone.calls.single().third)
        assertEquals("New Americana", args.getString("titulo"))
        assertEquals(50, args.getInt("volumen"))
        assertTrue(args.getBoolean("ecualizador"))
        assertTrue(args.getBoolean("segundoPlano"))
    }

    @Test fun ambiguousTitleWaitsForTheUserToPickAnId() {
        val stone = Stone().apply {
            reply = JSONObject("""{"status":"ambiguous","matches":[{"id":"a","title":"New Americana","artist":"Uno"},
                {"id":"b","title":"New Americana","artist":"Dos"}],"steps":{"search":{"code":"AMBIGUOUS_TITLE"}}}""")
        }
        val luna = AssistantState(Brain(stone))
        luna.send("pon New Americana al 50 %", false)
        assertEquals("AMBIGUOUS_TITLE", luna.last?.mcp?.code)
        assertEquals(1, stone.calls.size)
        luna.send("opción dos", false)
        val args = JSONObject(stone.calls.last().third)
        assertEquals("b", args.getString("idPista"))
        assertEquals(50, args.getInt("volumen"))
        assertEquals(2, stone.calls.size)
    }
}
