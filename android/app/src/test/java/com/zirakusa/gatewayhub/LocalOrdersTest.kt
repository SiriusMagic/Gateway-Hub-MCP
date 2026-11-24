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
        assertEquals(listOf("volume 5", "music new americana"), hands.done)
        val turn = requireNotNull(state.last)
        assertTrue(turn.ok)
        assertEquals(Reply(Say.CALCULATED, listOf("5602")), state.turns.first().reply)
        assertEquals(5, turn.reply.volume)
        assertEquals(Say.PLAYING_QUERY, turn.reply.say)
        assertEquals(text, Hearing.pick("pon música al cinco por ciento", text))
        state.late(Reply(Say.NOT_PLAYING, listOf("Stone")))
        assertEquals(listOf("5602"), state.turns.first().reply.args)
        assertEquals(5, state.last?.reply?.volume)

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
