package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.AssistantState
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.Hands
import com.zirakusa.gatewayhub.assistant.Hearing
import com.zirakusa.gatewayhub.assistant.People
import com.zirakusa.gatewayhub.assistant.Person
import com.zirakusa.gatewayhub.assistant.Say
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesAndDictationTest {
    @Test fun theMapTracksTheSequenceAndItsConfirmation() {
        val hands = object : FakeHands() {
            override fun person(name: String) = People.best(listOf(Person("Adonis Sarango", "0991112222")), name)
        }
        val state = AssistantState(Brain(hands))
        val phrase = "Me puedes calcular ocho por ochenta y ocho, baja el volumen al 5 % y de paso llama a Adonis Sarango"
        state.send(phrase, byVoice = true)

        assertEquals(phrase, state.ruleInput)
        assertEquals(listOf(Action.CALCULATE, Action.VOLUME, Action.CALL), state.ruleTrace.map { it.command.action })
        assertEquals(listOf("704"), state.ruleTrace.first().reply.args)
        assertEquals(listOf("volume 5"), hands.done)
        assertEquals(Say.CONFIRM_CALL, state.ruleTrace.last().reply.say)
        state.send("sí", byVoice = true)
        assertEquals(Say.CALL_STARTED, state.ruleTrace.last().reply.say)
        assertEquals(listOf("volume 5", "call 0991112222"), hands.done)
    }

    @Test fun equalContactNamesDoNotSelectAnArbitraryNumber() {
        val a = Person("Adonis Sarango", "0991112222", "a")
        val b = Person("Adonis Sarango", "0993334444", "b")
        assertEquals(People.Match.Ambiguous, People.resolve(listOf(a, b), a.name))
        assertEquals(People.Match.Unique(a), People.resolve(listOf(a, b.copy(contactId = "a")), a.name))
        val hands = object : FakeHands() {
            override fun person(name: String): Person = throw Hands.AmbiguousContact()
        }
        val state = AssistantState(Brain(hands))
        state.send("llama a Adonis Sarango", byVoice = true)
        assertEquals(Say.AMBIGUOUS_CONTACT, state.last?.reply?.say)
        state.send("sí", byVoice = true)
        assertTrue(hands.done.isEmpty())
    }

    @Test fun dictationPreservesFastCommandsAndAsksWhenContactsDisagree() {
        val names = listOf("Adonis Sarango", "Kusa")
        assertEquals("llama a Adonis Sarango", Hearing.dictated("llama QR", "llama a Adonis Sarango", names))
        assertEquals("llama", Hearing.dictated("llama a Kusa", "llama a Adonis Sarango", names))
        assertEquals("sube el volumen", Hearing.dictated("sube el volumen", "no se oye nada", names))
        assertEquals("sube el volumen", Hearing.dictated("sube el volumen", "cuelga", names))
    }
}
