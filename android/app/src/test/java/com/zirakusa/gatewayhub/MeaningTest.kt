package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.Examples
import com.zirakusa.gatewayhub.assistant.Meaning
import com.zirakusa.gatewayhub.assistant.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Layer 2: phrases the rules don't know, said again in Lucía's words; and her examples ranked by how alike they are. */
@RunWith(RobolectricTestRunner::class)
class MeaningTest {
    private val brain = Brain(FakeHands())
    private fun c(text: String) = brain.commandFor(text)

    @Test fun otherWaysOfSayingIt() {
        mapOf(
            "quiero escuchar alone" to Action.PLAY_MUSIC, "me gustaría oír música de soda stereo" to Action.PLAY_MUSIC,
            "me puedes poner la canción que se llama alone" to Action.PLAY_MUSIC, "hazme sonar despacito" to Action.PLAY_MUSIC,
            "despiértame a las 6" to Action.ALARM, "avísame en 10 minutos" to Action.TIMER, "telefonea a Luis" to Action.CALL,
            "lánzame la cámara" to Action.OPEN, "¿me puedes decir qué hora es?" to Action.TIME,
        ).forEach { (said, want) -> assertEquals(said, want, c(said).action) }
        assertEquals("alone", c("quiero escuchar alone")["query"]); assertEquals("soda stereo", c("me gustaría oír música de soda stereo")["query"])
        assertEquals("alone", c("me puedes poner la canción que se llama alone")["query"])
        assertEquals(6, c("despiértame a las 6").int("hour")); assertEquals(600, c("avísame en 10 minutos").int("seconds"))
        // What the rules already knew is untouched, and nonsense stays unknown (then the model, if any).
        assertEquals(Parser.parse("pon una alarma a las 7").action, c("pon una alarma a las 7").action)
        assertEquals(Action.UNKNOWN, c("me gusta mucho el color azul de las flores").action)
        assertTrue(Meaning.candidates("qué hora es").isEmpty())
    }

    @Test fun examplesByLikeness() {
        assertEquals("llama a Luis", Examples.closest("telefonea a mi hermano Luis", 3).first())
        assertEquals("pon un temporizador de 5 minutos", Examples.closest("avísame en 5 minutos", 3).first())
        assertTrue(Examples.closest("cuánto espacio libre tengo", 3).contains("cuánto espacio me queda"))
    }
}
