package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.Parser
import com.zirakusa.gatewayhub.assistant.Say
import com.zirakusa.gatewayhub.assistant.Tally
import com.zirakusa.gatewayhub.assistant.TallyOrder
import com.zirakusa.gatewayhub.assistant.TallyWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** «Modo conteo»: what starts it, how phrases count, corrections, and the note it leaves in Notas. */
class CountTest {
    private fun counts(t: Tally) = t.lines.associate { it.label to it.n }

    @Test fun startsACount() {
        assertEquals(Action.COUNT, Parser.parse("Vamos a contar").action)
        assertEquals(Action.COUNT, Parser.parse("a contar").action)
        assertEquals(Action.COUNT, Parser.parse("modo conteo").action)
        assertEquals("casa pérez".let { "casa perez" }, Parser.parse("nuevo conteo de la casa Pérez")["title"])
        assertEquals("local 3".let { it }, Parser.parse("conteo local 3")["title"])
        // «cuenta regresiva» is still a timer.
        assertEquals(Action.TIMER, Parser.parse("cuenta regresiva de 5 minutos").action)
        val hands = FakeHands(installed = emptySet())
        val turn = Brain(hands).handle("vamos a contar")
        assertEquals(Say.COUNTING, turn.reply.say); assertTrue(turn.ok)   // counting works even before Notas is installed
    }

    @Test fun theElectriciansExample() {
        val t = Tally()
        listOf("tomacorriente", "toma corriente", "tomacorrientes", "Tomacorriente").forEach { assertEquals(Tally.Kind.ADDED, t.hear(it).kind) }
        t.hear("interruptor simple")
        assertEquals(mapOf("Tomacorriente" to 4, "Interruptor simple" to 1), counts(t))
        t.hear("cinco focos")
        t.hear("dos interruptores simples y un tomacorriente")
        assertEquals(mapOf("Tomacorriente" to 5, "Interruptor simple" to 3, "Foco" to 5), counts(t))
        assertEquals(13, t.total)
        // First said first.
        assertEquals(listOf("Tomacorriente", "Interruptor simple", "Foco"), t.lines.map { it.label })
    }

    @Test fun saidWithoutPauses() {
        val t = Tally()
        t.hear("toma corriente toma corriente toma corriente")
        assertEquals(mapOf("Tomacorriente" to 3), counts(t))
        t.hear("foco interruptor simple foco")
        assertEquals(mapOf("Tomacorriente" to 3, "Foco" to 2, "Interruptor simple" to 1), counts(t))
        // Something new said twice in a row is two of it.
        t.hear("cajetín cajetín")
        assertEquals(2, counts(t)["Cajetín"])
        // A thing with several words stays whole.
        t.hear("interruptor de tres vías")
        assertEquals(1, counts(t)["Interruptor de tres vías"])
    }

    @Test fun otherAndMore() {
        val t = Tally()
        t.hear("foco"); t.hear("otro"); t.hear("tres más"); t.hear("otro foco")
        assertEquals(mapOf("Foco" to 6), counts(t))
        // «otro» with nothing counted yet does nothing.
        assertEquals(Tally.Kind.NOTHING, Tally().hear("otro").kind)
    }

    @Test fun corrections() {
        val t = Tally()
        t.hear("cuatro tomacorrientes"); t.hear("interruptor doble")
        assertEquals(Tally.Kind.REMOVED, t.hear("quita uno").kind)            // from the last thing counted
        assertEquals(mapOf("Tomacorriente" to 4), counts(t))
        t.hear("quita dos tomacorrientes")
        assertEquals(2, counts(t)["Tomacorriente"])
        assertEquals(Tally.Kind.UNDONE, t.hear("borra el último").kind)       // undoes the «quita dos»
        assertEquals(4, counts(t)["Tomacorriente"])
        assertEquals(Tally.Kind.UNDONE, t.hear("me equivoqué").kind)          // undoes «quita uno»: the switch is back
        assertEquals(1, counts(t)["Interruptor doble"])
        assertEquals(Tally.Kind.NOT_FOUND, t.hear("quita un foco").kind)
        t.bump(t.lines.first().key, 1); t.bump(t.lines.first().key, -9)        // the − button never goes below zero
        assertEquals(mapOf("Interruptor doble" to 1), counts(t))
    }

    @Test fun ordersWhileCounting() {
        val t = Tally()
        assertEquals(TallyOrder.Finish, TallyWords.order("termina"))
        assertEquals(TallyOrder.Finish, TallyWords.order("Lucía, guardar"))
        assertEquals(TallyOrder.Cancel, TallyWords.order("cancela el conteo"))
        assertEquals(TallyOrder.Read(null), TallyWords.order("¿cuántos van?"))
        t.hear("dos focos")
        assertEquals(listOf(2), t.hear("cuántos focos").lines.map { it.n })
        // Noise and lone words don't count.
        listOf("", "[unk]", "el", "eh", "a ver", "y").forEach { assertEquals(it, Tally.Kind.NOTHING, t.hear(it).kind) }
        assertEquals(2, t.total)
    }

    @Test fun pluralsAndPicking() {
        assertEquals("interruptor", TallyWords.singular("interruptores"))
        assertEquals("luz", TallyWords.singular("luces"))
        assertEquals("simple", TallyWords.singular("simples"))
        assertEquals("vía", TallyWords.singular("vías"))
        assertEquals("tres", TallyWords.singular("tres"))
        assertEquals("breaker", TallyWords.singular("breakers"))
        assertEquals(TallyWords.key("Lámparas"), TallyWords.key("lampara"))
        // The limited recognizer wins unless it heard something outside its words.
        assertEquals("toma corriente", TallyWords.pick("toma corriente", "toma oriente"))
        assertEquals("cajetín", TallyWords.pick("[unk]", "cajetín"))
        val words = TallyWords.grammar().removeSurrounding("[", "]").split(',').map { it.removeSurrounding("\"") }
        assertTrue("toma" in words && "corrientes" in words && "focos" in words && "termina" in words && words.last() == "[unk]")
    }

    @Test fun theNote() {
        val t = Tally("casa pérez")
        t.hear("tomacorriente"); t.hear("tomacorriente"); t.hear("interruptor simple")
        assertEquals("| Punto | Cantidad |\n| Tomacorriente | 2 |\n| Interruptor simple | 1 |\n| Total | 3 |", t.table("Punto", "Cantidad", "Total"))
        val hands = FakeHands()
        assertEquals(1L, hands.saveNote("Conteo: Casa pérez", t.table("Punto", "Cantidad", "Total"), "Conteos"))
        assertEquals("Conteos", hands.notes.single().third)
    }
}
