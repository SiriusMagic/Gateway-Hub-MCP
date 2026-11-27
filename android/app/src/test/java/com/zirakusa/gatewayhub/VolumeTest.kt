package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.Graph
import com.zirakusa.gatewayhub.assistant.Parser
import com.zirakusa.gatewayhub.assistant.Say
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** An order with its volume («pon música al 30 por ciento»), and the volume by itself. */
class VolumeTest {
    private fun p(text: String) = Parser.parse(text)

    @Test fun volumeWithAnOrder() {
        listOf("Pon música a 30 por ciento de volumen", "pon música al treinta por ciento", "reproduce música con el volumen a 30",
            "pon música con volumen 30", "pon música al 30%", "pon música al treinta porciento el volumen", "pon música y sube el volumen a 30").forEach {
            val c = p(it)
            assertEquals(it, Action.PLAY_MUSIC, c.action); assertEquals(it, "30", c["volume"]); assertNull(it, c["query"])
        }
        p("pon música de Bad Bunny al 40 por ciento").let { assertEquals("Bad Bunny", it["query"]); assertEquals("40", it["volume"]) }
        p("pon mis recomendaciones del día a todo volumen").let { assertEquals(Action.PLAY_RECOMMENDED, it.action); assertEquals("100", it["volume"]) }
        p("sigue la música con volumen a la mitad").let { assertEquals(Action.MEDIA, it.action); assertEquals("50", it["volume"]) }
        // Without a volume nothing changes.
        assertNull(p("pon una alarma a las 7 y 30")["volume"])
        assertEquals(Action.ALARM, p("pon una alarma a las 7 y 30").action)
    }

    @Test fun volumeAlone() {
        listOf("volumen al 30", "pon el volumen a 30 por ciento", "baja el volumen a 30", "volumen 30").forEach {
            assertEquals(it, Action.VOLUME, p(it).action); assertEquals(it, "30", p(it)["volume"])
        }
        assertEquals("100", p("volumen al máximo")["volume"])
        assertEquals("up", p("sube el volumen")["volume_step"])
        assertEquals("down", p("bájale un poco")["volume_step"])
        assertEquals("down", p("más bajo")["volume_step"])
        assertEquals("100", p("volumen al 250 por ciento")["volume"])
    }

    @Test fun theBrainSetsItFirst() {
        val hands = FakeHands()
        val turn = Brain(hands).handle("pon música al 30 por ciento", byVoice = true)
        assertEquals(listOf("volume 30", "music *"), hands.done)             // volume before the music starts
        assertEquals(30, turn.reply.volume); assertEquals(Say.PLAYING, turn.reply.say)
        assertEquals(listOf(Graph.VOICE, "app:stone", "do:PLAY_MUSIC", "do:VOLUME"), turn.path)
        val up = Brain(hands).handle("sube el volumen")
        assertEquals(Say.VOLUME_SET, up.reply.say); assertEquals(listOf("45"), up.reply.args)
        assertEquals(Say.VOLUME_SET, Brain(hands).handle("volumen al 0").reply.say)
        assertEquals(0, hands.level)
    }
}
