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
        assertEquals(listOf("search New Americana", "volume 50", "eq true", "play 1", "state", "return"), stone.done)
        assertTrue(music.ok)
    }

    @Test fun missingAndAmbiguousTracksNeverStartOtherMusic() {
        val stone = FakeStone().apply { songs = listOf() }
        val state = AssistantState(Brain(stone))
        state.send("Pon música New Americana", false)
        assertEquals(listOf("search New Americana"), stone.done)
        assertTrue(state.last!!.reply.args.single().contains("No encontré"))
        stone.done.clear()
        stone.songs = listOf(song("1", "New Americana", "Artista A"), song("2", "New Americana", "Artista B"))
        state.send("Pon música New Americana con el volumen a 50", false)
        assertEquals(listOf("search New Americana"), stone.done)
        val prompt = state.last!!
        state.chooseMusic(prompt, 1)
        state.chooseMusic(prompt, 1)
        assertEquals(listOf("search New Americana", "state", "volume 50", "play 2", "state"), stone.done)
        assertTrue(state.last!!.ok)
    }

    @Test fun coldStoneIsOpenedOnceAndAnOldEqualizerIsReported() {
        val stone = FakeStone().apply { closed = true; eqMissing = true }
        val state = AssistantState(Brain(stone).apply { stoneIo = Dispatchers.Unconfined })
        state.send("Pon música en segundo plano New Americana con el ecualizador activo", false)
        assertEquals(listOf("search New Americana", "open stone", "search New Americana", "eq true", "play 1", "state", "return"), stone.done)
        assertFalse(state.last!!.ok)
        assertTrue(state.last!!.reply.args.single().contains("UNKNOWN_TOOL"))
    }

    @Test fun failedVolumeWaitsForAnExplicitDecision() {
        val stone = FakeStone().apply { volumeAllowed = false }
        val state = AssistantState(Brain(stone))
        state.send("Pon música New Americana con el volumen a 50 y el ecualizador activo", false)
        assertEquals(listOf("search New Americana", "volume denied"), stone.done)
        state.send("sí", false)
        assertEquals(listOf("search New Americana", "volume denied", "state", "eq true", "play 1", "state"), stone.done)
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
        var closed = false
        var eqMissing = false
        var volumeAllowed = true
        var songs = listOf(song("1", "New Americana"))
        var current: String? = null

        override fun open(app: FamilyApp): Boolean { closed = false; return super.open(app) }
        override fun comeBack() { done += "return" }
        override fun setVolume(percent: Int): Boolean {
            if (volumeAllowed) return super.setVolume(percent)
            done += "volume denied"
            return false
        }

        override fun tool(app: FamilyApp, name: String, args: JSONObject): JSONObject? = when (name) {
            "library_search" -> {
                done += "search ${args.getString("query")}"
                if (closed) throw Hands.ToolError("STONE_CLOSED", "")
                JSONObject().put("results", JSONArray(songs)).put("total", songs.size)
            }
            "equalizer_set_enabled" -> {
                done += "eq ${args.getBoolean("enabled")}"
                if (eqMissing) throw Hands.ToolError("UNKNOWN_TOOL", "")
                JSONObject().put("ok", true)
            }
            "player_play_songs" -> {
                current = args.getString("songIds")
                done += "play $current"
                JSONObject().put("ok", true)
            }
            "player_state" -> {
                done += "state"
                JSONObject().put("playing", current != null).put("songId", current)
            }
            else -> null
        }
    }

    companion object {
        private fun song(id: String, title: String, artist: String = "") =
            JSONObject().put("id", id).put("title", title).put("artist", artist)
    }
}
