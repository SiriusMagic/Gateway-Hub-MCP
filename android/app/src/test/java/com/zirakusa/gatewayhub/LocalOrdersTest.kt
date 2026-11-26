package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.AssistantState
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.Hearing
import com.zirakusa.gatewayhub.assistant.Parser
import com.zirakusa.gatewayhub.assistant.Reply
import com.zirakusa.gatewayhub.assistant.Say
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class LocalOrdersTest {
    private val hands = FakeHands()
    private val brain = Brain(hands)

    @Test fun quickRepliesAreNarrowAndOffline() {
        for (text in listOf("Hola", "¿Hola, cómo estás?", "hola como listas")) {
            assertEquals(Reply(Say.QUICK, listOf("greeting")), brain.handle(text).reply)
        }
        assertEquals(listOf("sad"), brain.handle("Me siento triste").reply.args)
        assertEquals(listOf("happy"), brain.handle("Estoy feliz").reply.args)
        assertEquals(listOf("image_unavailable"), brain.handle("genera una imagen").reply.args)
        assertEquals(Say.NOT_UNDERSTOOD, brain.handle("No me siento triste").reply.say)
        assertEquals(Say.PLAYING, brain.handle("hola, pon música").reply.say)
    }

    @Test fun calculationAndMusicBothRunWithVolumeFirst() {
        val state = AssistantState(brain)
        val text = "entonces calcula 7*9*89 y menos 5 y pon música al 5% new americana"
        state.send(text, byVoice = false)
        assertTrue(hands.done.isEmpty())
        val turn = requireNotNull(state.last)
        assertEquals(false, turn.ok)
        assertEquals(Reply(Say.CALCULATED, listOf("5602")), state.turns.first().reply)
        assertEquals("5", turn.command["volume"])
        assertEquals("new americana", turn.command["query"])
        assertEquals(Say.TOOL_RESULT, turn.reply.say)
        assertEquals(text, Hearing.pick("pon música al cinco por ciento", text))
        assertEquals(listOf("5602"), state.turns.first().reply.args)
        assertEquals(null, state.last?.reply?.volume)

        assertEquals(listOf("0.5"), brain.handle("calcula 1,5 entre 3").reply.args)
        assertEquals(Say.MATH_ERROR, brain.handle("calcula 2/0").reply.say)
        assertEquals(Action.TOOL, Parser.parse("calcula (2+3)*4").action)
    }

    @Test fun explicitWebSearchPreservesQueryAndDoesNotInterceptLocalSearch() {
        val turn = brain.handle("Consulta en internet sobre Quito y el café")
        assertEquals(Say.WEB_OPENED, turn.reply.say)
        assertEquals(listOf("web Quito y el café"), hands.done)
        assertEquals(Say.WEB_QUERY, brain.handle("busca en internet").reply.say)
        assertEquals(Action.TOOL, Parser.parse("busca en mis notas Quito").action)
        assertEquals("busca en internet volcanes de Ecuador",
            Hearing.pick("busca en internet de", "busca en internet volcanes de Ecuador"))
    }

    @Test fun confirmedCallResumesOnceAfterPermission() {
        hands.outgoingAllowed = false
        val state = AssistantState(brain)
        state.send("llama a Ana", byVoice = true)
        assertEquals(Say.CONFIRM_CALL, state.last?.reply?.say)
        assertTrue(hands.done.isEmpty())
        state.send("sí", byVoice = true)
        assertEquals(Say.NEED_CALL_PERMISSION, state.last?.reply?.say)
        assertTrue(hands.done.isEmpty())
        hands.outgoingAllowed = true
        state.finishCallPermission(true)
        state.finishCallPermission(true)
        assertEquals(Say.CALL_STARTED, state.last?.reply?.say)
        assertEquals(listOf("call 0991234567"), hands.done)
    }

    @Test fun bothRecognizersMustAgreeBeforeHandsFreeDialing() {
        assertEquals("sí", Hearing.callConfirmation("sí", "y sí"))
        assertEquals("sí", Hearing.callConfirmation("sí llama", "sí, llama"))
        assertEquals("no", Hearing.callConfirmation("sí", "no llames"))
        assertEquals("", Hearing.callConfirmation("sí", "sí pero espera"))
        assertEquals("", Hearing.callConfirmation("sí", ""))
        assertEquals("", Hearing.callConfirmation("[unk]", "sí"))
    }

    @Test fun theSpeakerRequestSurvivesConfirmationAndAndroidPermission() {
        val state = AssistantState(brain)
        hands.outgoingAllowed = false
        state.send("llama a Ana en altavoz", byVoice = true)
        assertEquals(Say.CONFIRM_SPEAKER_CALL, state.last?.reply?.say)
        assertEquals(listOf("Ana Pérez"), state.last?.reply?.args)
        assertTrue(hands.done.isEmpty())
        state.heard(Hearing.callConfirmation("sí", "y sí"), final = true)
        assertEquals(Say.NEED_CALL_PERMISSION, state.last?.reply?.say)
        hands.outgoingAllowed = true
        state.finishCallPermission(true)
        assertEquals(listOf("call 0991234567 speaker"), hands.done)
        assertEquals(Say.NO_CALL, brain.handle("pon en altavoz").reply.say)
        val inCall = Brain(object : FakeHands() {
            override fun inCall() = true
        })
        assertEquals(Say.CALL_SPEAKER_MANUAL, inCall.handle("pon en altavoz").reply.say)
    }

    @Test fun callsCanBeCancelledAndDeniedPermissionPreparesTheDialer() {
        val state = AssistantState(brain)
        state.send("llama a Ana", byVoice = false)
        state.send("no", byVoice = false)
        assertEquals(Say.CANCELLED, state.last?.reply?.say)
        assertTrue(hands.done.isEmpty())

        hands.outgoingAllowed = false
        state.send("llama a Ana", byVoice = false)
        state.send("sí", byVoice = false)
        state.finishCallPermission(false)
        assertEquals(Say.CALLING, state.last?.reply?.say)
        assertEquals(listOf("dial 0991234567"), hands.done)
    }

    @Test fun aNewOrderOrAnExpiredConfirmationCannotStartACall() {
        var time = 0L
        val timed = Brain(hands) { time }
        timed.handle("llama a Ana")
        timed.handle("hola")
        timed.handle("sí")
        timed.handle("llama a Ana")
        time += Brain.CONFIRM_MS + 1
        timed.handle("sí")
        assertTrue(hands.done.isEmpty())
    }
}
