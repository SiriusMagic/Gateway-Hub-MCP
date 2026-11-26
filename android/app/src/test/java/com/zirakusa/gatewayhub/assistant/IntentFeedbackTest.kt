package com.zirakusa.gatewayhub.assistant

import android.content.Context
import com.zirakusa.gatewayhub.FakeHands
import com.zirakusa.gatewayhub.MemorySessions
import com.zirakusa.gatewayhub.data.HubTransport
import com.zirakusa.gatewayhub.data.RecordCipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class IntentFeedbackTest {
    @Test fun independentVotesWaitForConsentAndRetryTheSameEvent() {
        val context = RuntimeEnvironment.getApplication() as Context
        val ciphertext = object : RecordCipher {
            override fun seal(text: String) = text.reversed()
            override fun open(value: String) = value.reversed()
        }
        val attempts = mutableListOf<JSONObject>()
        val tokens = mutableListOf<String>()
        var failNext = true
        val transport = HubTransport { _, url, token, raw ->
            tokens += token
            if (url.endsWith("/credentials")) 201 to """{"token":"scoped"}"""
            else {
                attempts += JSONObject(raw!!)
                if (failNext) { failNext = false; 503 to "" } else 202 to ""
            }
        }
        val scope = CoroutineScope(Job().apply { cancel() })
        val sessions = MemorySessions()
        val feedback = IntentFeedback(context, scope, ciphertext, sessions, transport)
        val state = AssistantState(Brain(FakeHands()))
        state.feedback = feedback
        val phrase = "Calcula dos más 2 y pon música"
        state.send(phrase, false)
        assertEquals(2, state.turns.size)
        state.vote(state.turns[0], IntentVote.LIKE)
        state.vote(state.turns[1], IntentVote.CORRECTED, "Arrancó otra canción", "Reproduce la música que elegí")
        feedback.flush()
        assertTrue(tokens.isEmpty())
        feedback.share(true)
        feedback.flush()
        assertTrue(tokens.isEmpty())
        sessions.save("https://hub.example", "owner")
        feedback.flush()
        feedback.flush()
        feedback.flush()
        assertEquals(listOf("owner", "scoped", "scoped", "scoped"), tokens)
        assertEquals(3, attempts.size)
        assertEquals(attempts[0].getString("id"), attempts[1].getString("id"))
        val sent = attempts.distinctBy { it.getString("id") }.associateBy { it.getInt("step") }
        assertEquals("funcionó", sent.getValue(0).getString("status"))
        assertEquals("corregido", sent.getValue(1).getString("status"))
        assertEquals(phrase, sent.getValue(1).getString("original"))
        assertEquals("Reproduce la música que elegí", sent.getValue(1).getString("expected"))
        assertNotEquals(sent.getValue(0).getString("actionId"), sent.getValue(1).getString("actionId"))
        assertFalse(sent.getValue(1).has("model"))
    }

    @Test fun locallyChosenAliasesCanBeReplacedOrRemoved() {
        val feedback = IntentFeedback(RuntimeEnvironment.getApplication(), CoroutineScope(Job().apply { cancel() }),
            object : RecordCipher {
                override fun seal(text: String) = text
                override fun open(value: String) = value
            })
        feedback.putAlias("Kuri", "content://contacts/first")
        assertEquals("content://contacts/first", feedback.alias("kuri"))
        feedback.putAlias("Kuri", "content://contacts/second")
        assertEquals(listOf("Kuri"), feedback.aliases())
        assertEquals("content://contacts/second", feedback.alias("Kuri"))
        feedback.putAlias("Kuri", null)
        assertNull(feedback.alias("Kuri"))
    }
}
