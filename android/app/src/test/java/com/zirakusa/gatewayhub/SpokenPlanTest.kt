package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.AssistantState
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.Hearing
import com.zirakusa.gatewayhub.assistant.Intents
import com.zirakusa.gatewayhub.assistant.Parser
import com.zirakusa.gatewayhub.assistant.People
import com.zirakusa.gatewayhub.assistant.Person
import com.zirakusa.gatewayhub.assistant.PhoneSpeech
import com.zirakusa.gatewayhub.assistant.Say
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpokenPlanTest {
    private val people = listOf(Person("Kusa", "0991234567"), Person("Luis Mora", "0991112233"))
    private val hands = object : FakeHands() {
        override fun person(name: String): Person? {
            if (!contactsAllowed) throw com.zirakusa.gatewayhub.assistant.Hands.NoContacts()
            return People.best(people, name)
        }
    }
    private val state = AssistantState(Brain(hands))
    private val names = people.map(Person::name)

    @Test fun spokenNumbersStayTogetherAndDistinctOrdersRunInOrder() {
        val phrase = "necesito que calcules ocho por ochenta y ocho sube el volumen y baja el volumen al cinco por ciento y llama a Kusa"
        assertEquals(phrase, Hearing.pick("baja el volumen al cinco por ciento", phrase))
        state.send(phrase, byVoice = true)
        assertEquals(listOf(Action.CALCULATE, Action.VOLUME, Action.VOLUME, Action.CALL), state.turns.map { it.command.action })
        assertEquals(listOf("704"), state.turns.first().reply.args)
        assertEquals(listOf("volume 85", "volume 5"), hands.done)
        assertEquals(Say.CONFIRM_CALL, state.last?.reply?.say)
        state.send("sí", byVoice = true)
        assertEquals("call 0991234567", hands.done.last())
    }

    @Test fun textInsideAMessageAndUnknownVolumePhrasesStaySeparateFromActions() {
        val text = "escríbele a Luis diciendo sube el volumen y llama a Kusa"
        assertEquals(listOf(text), Intents.split(text, Parser::parse))
        assertEquals(Action.UNKNOWN, Parser.parse("no sé qué pasó al 5%").action)
    }

    @Test fun realContactsHelpChooseTheTranscriptWithoutGuessingBetweenTwoPeople() {
        assertEquals("llama a Kusa", Hearing.pick("llama qr", "llama a Kusa", contacts = names))
        assertEquals("llama a Kusa", Hearing.pick("llama a Kusa", "llama hacia", contacts = names))
        assertEquals("llama", Hearing.pick("llama a Luis", "llama a Kusa", contacts = names))
        assertEquals("Kusa", PhoneSpeech.contactName("cuelga", "Kusa", names))
        assertEquals("", PhoneSpeech.contactName("Luis", "Kusa", names))
        assertNull(People.best(people, "Kuri"))
    }

    @Test fun permissionRestoresTheClarifiedContact() {
        hands.contactsAllowed = false
        state.send("llama", byVoice = true)
        state.send("Kusa", byVoice = true)
        assertEquals(Say.NEED_CONTACTS, state.last?.reply?.say)
        hands.contactsAllowed = true
        state.retryContactPermission()
        assertEquals(listOf("Kusa"), state.last?.reply?.args)
        assertEquals(Say.CONFIRM_CALL, state.last?.reply?.say)
        assertTrue(hands.done.isEmpty())
    }

    @Test fun aBadTranscriptAsksForANameAndStillRequiresConsent() {
        listOf("llama", "llama qr", "llama hacia", "llama cuelga", "llama y cuelga").forEach {
            state.send(it, byVoice = true)
            assertEquals(it, Say.ASK_CALL_CONTACT, state.last?.reply?.say)
        }
        assertTrue(hands.done.isEmpty())
        state.send("Kusa", byVoice = true)
        assertEquals(Say.CONFIRM_CALL, state.last?.reply?.say)
        assertEquals(listOf("Kusa"), state.last?.reply?.args)
        assertTrue(hands.done.isEmpty())
        state.send("sí", byVoice = true)
        assertEquals(listOf("call 0991234567"), hands.done)
    }
}
