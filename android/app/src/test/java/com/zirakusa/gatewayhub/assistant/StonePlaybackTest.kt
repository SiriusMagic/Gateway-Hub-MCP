package com.zirakusa.gatewayhub.assistant

import com.zirakusa.gatewayhub.FakeHands
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StonePlaybackTest {
    @Test fun playbackControlsUseOnlyTheirLocalToolsAndKeepRestartSeparate() {
        val app = object : FakeHands() {
            override fun tool(app: FamilyApp, name: String, args: JSONObject): JSONObject {
                assertEquals("stone", app.id)
                done += "$name $args"
                return JSONObject().put("ok", true)
            }
        }
        val state = AssistantState(Brain(app))
        listOf("Pausa", "Reanuda", "Siguiente canción", "Canción anterior", "Vuelve al inicio de esta canción")
            .forEach { state.send(it, true) }
        assertEquals(listOf("player_pause {}", "player_play {}", "player_next {}", "player_previous {}",
            "player_seek {\"positionSeconds\":0}"), app.done)
        assertTrue(state.turns.all { it.ok && it.observed["channel"] == "mcp_local" })
    }

    @Test fun calculatorAndStoneKeepTheOriginalPlan() {
        val stone = FakeStone()
        val state = AssistantState(Brain(stone))
        val text = "Calcula dos más 2 y pon música en segundo plano New Americana con el volumen a 50 y el ecualizador activo"
        state.send(text, byVoice = true)
        val (calc, music) = state.turns
        assertEquals("4", calc.reply.args.single())
        assertEquals("2+2", calc.command["expression"])
        assertEquals("New Americana", music.command["query"])
        assertEquals(text, music.evidence?.plan?.original)
        assertNotEquals(calc.evidence?.id, music.evidence?.id)
        assertEquals(listOf("search", "volume", "equalizer", "play", "return"), music.evidence?.plan?.steps?.last()?.dependencies)
        assertEquals(listOf("title New Americana", "return"), stone.done)
        assertTrue(music.ok)
    }

    @Test fun bundledCatalogMatchesTheOneCallContract() {
        val data = javaClass.getResourceAsStream("/intent-actions/stone.v1.json")!!.bufferedReader().use { it.readText() }
        val catalog = JSONObject(data)
        assertEquals(1, catalog.getInt("schemaVersion"))
        val entries = catalog.getJSONArray("actions")
        val actions = (0 until entries.length()).map(entries::getJSONObject)
        assertEquals(actions.size, actions.map { it.getString("id") }.toSet().size)
        val pilot = actions.single { it.getString("id") == "stone.player.play_by_title" }
        assertEquals(StoneTitleAction.TOOL, pilot.getString("herramienta"))
        assertEquals(setOf("titulo", "volumen", "ecualizador", "segundoPlano", "idPista"),
            pilot.getJSONObject("entrada").getJSONObject("properties").keys().asSequence().toSet())
    }

    @Test fun prefersOneSignedCallForTheWholeMusicAction() {
        val stone = FakeStone()
        val state = AssistantState(Brain(stone))
        state.send("Pon música en segundo plano New Americana con el volumen a 50 y el ecualizador activo", false)
        assertEquals(listOf("title New Americana", "return"), stone.done)
        val args = stone.nativeArgs!!
        assertEquals(50, args.getInt("volumen"))
        assertTrue(args.getBoolean("ecualizador"))
        assertTrue(args.getBoolean("segundoPlano"))
        assertEquals(53, state.last!!.reply.volume)
        assertTrue(state.last!!.ok)
    }

    @Test fun stopsWhenTheSignedToolIsUnavailableOrTheResultIsUncertain() {
        val stone = FakeStone().apply { nativeError = "UNKNOWN_TOOL" }
        val state = AssistantState(Brain(stone))
        state.send("Pon música New Americana", false)
        assertEquals(listOf("title New Americana"), stone.done)
        assertFalse(state.last!!.ok)
        stone.done.clear()
        stone.nativeError = "MCP_RPC_ERROR"
        state.send("Pon música New Americana", false)
        assertEquals(listOf("title New Americana"), stone.done)
        assertFalse(state.last!!.ok)
    }

    @Test fun missingAndAmbiguousTracksNeverStartOtherMusic() {
        val stone = FakeStone().apply { songs = listOf() }
        val state = AssistantState(Brain(stone))
        state.send("Pon música New Americana", false)
        assertEquals(listOf("title New Americana"), stone.done)
        assertTrue(state.last!!.reply.args.single().contains("No encontré"))
        stone.done.clear()
        stone.songs = listOf(song("1", "New Americana", "Artista A"), song("2", "New Americana", "Artista B"))
        state.send("Pon música New Americana con el volumen a 50", false)
        assertEquals(listOf("title New Americana"), stone.done)
        val prompt = state.last!!
        state.chooseMusic(prompt, 1)
        state.chooseMusic(prompt, 1)
        assertEquals(listOf("title New Americana", "title New Americana"), stone.done)
        assertTrue(state.last!!.ok)
    }

    @Test fun coldStoneIsOpenedOnceAndAnOldEqualizerIsReported() {
        val stone = FakeStone().apply { closed = true; eqMissing = true }
        val state = AssistantState(Brain(stone).apply { stoneIo = Dispatchers.Unconfined })
        state.send("Pon música en segundo plano New Americana con el ecualizador activo", false)
        assertEquals(listOf("title New Americana", "open stone", "title New Americana", "return"), stone.done)
        assertFalse(state.last!!.ok)
        assertTrue(state.last!!.reply.args.single().contains("UNKNOWN_TOOL"))
    }

    @Test fun failedVolumeWaitsForAnExplicitDecision() {
        val stone = FakeStone().apply { volumeAllowed = false }
        val state = AssistantState(Brain(stone))
        state.send("Pon música New Americana con el volumen a 50 y el ecualizador activo", false)
        assertEquals(listOf("title New Americana"), stone.done)
        state.send("sí", false)
        assertEquals(listOf("title New Americana", "title New Americana"), stone.done)
        assertTrue(state.last!!.reply.args.single().contains("volumen actual"))
    }

    @Test fun ambiguousContactNeedsMoreInformationAndChosenContactStillNeedsConfirmation() {
        val stone = object : FakeHands() {
            override fun person(name: String): Person {
                if (name.equals("kuri", true)) throw Hands.AmbiguousContact()
                return Person("Kuri Pérez", "5551234")
            }
        }
        val state = AssistantState(Brain(stone))
        state.send("Llama a Kuri", true)
        assertEquals(Say.ASK_AMBIGUOUS_CONTACT, state.last!!.reply.say)
        state.send("Kuri Pérez", true)
        assertEquals(Say.CONFIRM_CALL, state.last!!.reply.say)
        assertTrue(stone.done.isEmpty())
        state.send("sí", true)
        assertEquals("call 5551234", stone.done.single())
    }

    private class FakeStone : FakeHands() {
        var nativeError: String? = null
        var nativeArgs: JSONObject? = null
        var closed = false
        var eqMissing = false
        var volumeAllowed = true
        var songs = listOf(song("1", "New Americana"))

        override fun open(app: FamilyApp): Boolean { closed = false; return super.open(app) }
        override fun comeBack() { done += "return" }

        override fun tool(app: FamilyApp, name: String, args: JSONObject): JSONObject? {
            if (name != "player_play_by_title") return super.tool(app, name, args)
            done += "title ${args.getString("titulo")}"
            if (closed) throw Hands.ToolError("STONE_CLOSED", "")
            nativeError?.let { throw Hands.ToolError(it, "") }
            nativeArgs = args
            val steps = JSONObject()
            val response = JSONObject().put("steps", steps).put("segundoPlano", args.optBoolean("segundoPlano"))
            val track = if (args.has("idPista")) songs.find { it.getString("id") == args.getString("idPista") }
                else songs.singleOrNull()
            if (track == null) {
                val code = if (songs.isEmpty()) "TRACK_NOT_FOUND" else "AMBIGUOUS_TITLE"
                steps.put("search", JSONObject().put("code", code))
                return response.put("status", if (songs.isEmpty()) "failed" else "ambiguous").put("matches", JSONArray(songs))
            }
            if (args.has("volumen")) {
                if (!volumeAllowed) {
                    steps.put("volume", JSONObject().put("code", "VOLUME_REJECTED"))
                    return response.put("status", "failed").put("selected", track)
                }
                steps.put("volume", JSONObject().put("code", "OK").put("actual", 53))
            }
            if (args.has("ecualizador")) steps.put("equalizer", JSONObject().put("code", if (eqMissing) "UNKNOWN_TOOL" else "OK"))
            steps.put("play", JSONObject().put("code", "OK"))
            return response.put("status", if (eqMissing) "partial" else "playing").put("track", track)
        }
    }

    companion object {
        private fun song(id: String, title: String, artist: String = "") =
            JSONObject().put("id", id).put("title", title).put("artist", artist)
    }
}
