package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.AssistantState
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.Examples
import com.zirakusa.gatewayhub.assistant.Intents
import com.zirakusa.gatewayhub.assistant.Parser
import com.zirakusa.gatewayhub.assistant.People
import com.zirakusa.gatewayhub.assistant.Person
import com.zirakusa.gatewayhub.assistant.Reply
import com.zirakusa.gatewayhub.assistant.Say
import com.zirakusa.gatewayhub.assistant.local.OrderReply
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Lucía's «sentimientos»: several orders in one sentence, the examples a model reads, and what it may answer. */
@RunWith(RobolectricTestRunner::class)
class IntentsTest {
    private fun knows(t: String) = Parser.parse(t).action != Action.UNKNOWN
    private fun split(t: String) = Intents.split(t, ::knows)

    @Test fun oneSentenceManyOrders() {
        assertEquals(listOf("llama a Luis", "qué hora es"), split("Hey Luna llama a Luis por favor y ayúdame con la hora"))
        assertEquals(listOf("pon música", "sube el volumen"), split("oye lucía, pon música y sube el volumen"))
        assertEquals(listOf("enciende la linterna", "pon una alarma a las 6", "qué tiempo hace"),
            split("enciende la linterna, pon una alarma a las 6 y después dime el clima, gracias"))
        // Whole when a piece is not an order: the «y» belongs to the order.
        assertEquals(listOf("pon una alarma a las 7 y media"), split("pon una alarma a las 7 y media"))
        assertEquals(listOf("manda un mensaje a Ana que compre pan y leche"), split("manda un mensaje a Ana que compre pan y leche"))
        // Courtesy and the wake words go; a bare subject is its question.
        assertEquals(listOf("llama a Luis"), split("Hey Luna, llama a Luis por favor"))
        assertEquals("qué hora es", Intents.clean("ayúdame con la hora"))
        assertEquals("qué día es", Intents.clean("dime la fecha por favor"))
        assertEquals("luna llama a luis y dime la hora", com.zirakusa.gatewayhub.assistant.Hearing.pick("llama a luis la hora", "luna llama a luis y dime la hora"))
        assertTrue(Intents.yes("sí, hazlo")); assertTrue(Intents.yes("Claro")); assertTrue(!Intents.yes("no"))
    }

    @Test fun everyExampleIsAnOrderLuciaKnows() {
        Examples.actions.forEach { assertTrue(it, knows(it)) }
        Examples.tools.forEach { (tool, said) ->
            val c = Parser.parse(said)
            assertEquals(said, Action.TOOL, c.action); assertEquals(said, tool, c["tool"])
        }
        assertTrue(Examples.tools.size >= 110)
        assertEquals("llama a Luis", Examples.closest("llama a mi hermano Luis", 5).first())
    }

    @Test fun theModelOnlyRewrites() {
        assertEquals(listOf("llama a Luis", "qué hora es"),
            OrderReply.parse("Claro: {\"ordenes\": [\"Llama a Luis\", \"¿qué hora es?\"]}", ::knows).map { it.replaceFirstChar(Char::lowercase).trim('¿', '?') })
        assertEquals(emptyList<String>(), OrderReply.parse("{\"ordenes\": []}", ::knows))
        assertEquals(emptyList<String>(), OrderReply.parse("no sé", ::knows))
        assertEquals(emptyList<String>(), OrderReply.parse("{\"ordenes\": [\"haz magia\", 7]}", ::knows))     // only what the rules know
        assertEquals(5, OrderReply.parse("{\"ordenes\": [" + (1..9).joinToString(",") { "\"pon una alarma a las $it\"" } + "]}", ::knows).size)
    }

    @Test fun luciaDoesBothAndAnswersOnce() {
        val hands = object : FakeHands() {
            override fun person(name: String) = People.best(listOf(Person("Luis Mora", "0991112233")), name)
        }
        val spoken = mutableListOf<List<Reply>>()
        val state = AssistantState(Brain(hands), {}, { spoken += it })
        state.send("Hey Luna llama a Luis por favor y ayúdame con la hora", byVoice = true)
        assertEquals(listOf(Action.CALL, Action.TIME), state.turns.map { it.command.action })
        assertTrue(hands.done.any { it.contains("0991112233") })
        assertEquals(1, spoken.size); assertEquals(listOf(Say.CALLING, Say.TIME), spoken[0].map { it.say })
        // An order that asks first stops there; the rest waits for «sí».
        state.send("vacía la cola y dime la hora", byVoice = false)
        assertEquals(Say.CONFIRM, state.last!!.reply.say)
        state.send("sí", byVoice = false)
        assertEquals(Action.TIME, state.last!!.command.action)
    }
}
