package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.EnergyGate
import com.zirakusa.gatewayhub.assistant.Hearing
import com.zirakusa.gatewayhub.assistant.Parser
import com.zirakusa.gatewayhub.assistant.Vocabulary
import com.zirakusa.gatewayhub.assistant.WakeName
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Lucía's ears: the wake phrase, the energy gate, the vocabulary of orders and how the two recognizers are joined. */
@RunWith(RobolectricTestRunner::class)
class LuciaTest {
    private fun list(json: String) = JSONArray(json).let { a -> (0 until a.length()).map { a.getString(it) } }

    @Test fun wakePhrase() {
        val l = WakeName.LUCIA
        assertTrue(l.heard("oye lucía")); assertTrue(l.heard("hola lucía")); assertTrue(l.heard("[unk] oye lucía [unk]"))
        assertFalse(l.heard("lucía")); assertFalse(l.heard("oye luisa")); assertFalse(l.heard("hoy lucía luz"))       // the name alone never wakes
        assertFalse(WakeName.NOVA.heard("oye lucía")); assertTrue(WakeName.NOVA.heard("Oye Nova"))
        val g = list(l.grammar())
        assertTrue("oye lucía" in g && "[unk]" in g && "lucas" in g); assertEquals(g.size, g.toSet().size)
        assertEquals(WakeName.LUCIA, WakeName.from(null)); assertEquals(WakeName.KIRA, WakeName.from("KIRA"))
        WakeName.entries.forEach { n -> assertTrue(n.decoys.none { it == n.word }) }
    }

    @Test fun energyGateSkipsSilence() {
        val g = EnergyGate()
        val quiet = ShortArray(1600) { ((it % 7) - 3).toShort() }
        val voice = ShortArray(1600) { (if (it % 2 == 0) 4000 else -4000).toShort() }
        assertTrue(g.rms(quiet) < 5); assertEquals(4000.0, g.rms(voice), 0.1)
        repeat(20) { assertFalse(g.open(g.rms(quiet))) }                                     // a quiet room: nothing decoded
        assertTrue(g.open(g.rms(voice)))
        repeat(6) { assertTrue(g.open(g.rms(quiet))) }                                       // the tail keeps the last syllables
        assertFalse(g.open(g.rms(quiet)))
        val noisy = EnergyGate()
        repeat(200) { noisy.open(900.0) }                                                     // a fan: the floor learns it
        assertTrue(noisy.floor > 500); assertFalse(noisy.open(1000.0)); assertTrue(noisy.open(4000.0))
    }

    @Test fun vocabularyAndHearing() {
        val words = list(Vocabulary.grammar(listOf("Ana Pérez", "José-Luis")))
        assertTrue(listOf("reproduce", "música", "alarma", "recomendaciones", "siete", "ana", "pérez", "josé", "luis", "stone", "cámara", "[unk]").all { it in words })
        assertEquals(words.size, words.toSet().size)
        assertEquals("pon mis recomendaciones día", Hearing.pick("pon mis recomendaciones día", "pon visa home naciones el liga"))
        assertEquals("abre la cámara", Hearing.pick("[unk] [unk]", "abre la cámara"))            // the list missed it, the free one has it
        assertEquals("manda un mensaje a ana que llego en diez minutos", Hearing.pick("manda un mensaje a ana", "manda un mensaje a ana que llego en diez minutos"))
        assertEquals("reproduce música", Hearing.pick("reproduce música [unk]", ""))
    }

    @Test fun whatTheSmallModelDropsStillWorks() {
        assertEquals(Action.WEATHER, Parser.parse("tiempo").action); assertEquals(Action.WEATHER, Parser.parse("tiempo hace").action)
        assertEquals("soda stereo", Parser.parse("pon música soda stereo")["query"])
        assertEquals(Action.AGENDA, Parser.parse("tengo hoy").action)
        assertEquals(300, Parser.parse("temporizador cinco minutos").int("seconds"))
        assertEquals(Action.PLAY_RECOMMENDED, Parser.parse("pon mis recomendaciones día").action)
        assertEquals("camara", Parser.parse("abre va cámara")["app"])
    }
}
