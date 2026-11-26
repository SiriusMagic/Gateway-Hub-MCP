package com.zirakusa.gatewayhub

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.gatewayhub.assistant.AssistantActions
import com.zirakusa.gatewayhub.assistant.AssistantPage
import com.zirakusa.gatewayhub.assistant.AssistantScreen
import com.zirakusa.gatewayhub.assistant.AssistantState
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.DictationComparison
import com.zirakusa.gatewayhub.assistant.WakeName
import com.zirakusa.gatewayhub.ui.HubTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The assistant before the first order, with the light running through the brain, and after a few orders. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-w400dp-h860dp-xxhdpi")
class AssistantScreenshotsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun assistant() {
        val hands = FakeHands()
        val state = AssistantState(Brain(hands))
        compose.setContent { HubTheme { AssistantScreen(state) } }
        compose.onNodeWithText("Reproduce música").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/assistant-empty.png")
        compose.onNodeWithText("Reproduce música").performClick()                      // an example sends itself
        assertEquals(listOf("music *"), hands.done)
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(700)
        compose.onRoot().captureRoboImage("build/screenshots/assistant-running.png")
        compose.mainClock.autoAdvance = true
        state.send("qué tiempo hace", byVoice = true)
        state.send("abre spotify", byVoice = false)
        state.send("pon una alarma a las 7 y media", byVoice = true)
        compose.waitForIdle()
        compose.onNodeWithText("Alarma a las 7:30.").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/assistant-turns.png")
        // The whole brain lives in her settings: Voz → Notas → Conteo, with «Hecho» marked apart.
        state.send("vamos a contar", byVoice = true); state.endTally()
        state.page = AssistantPage.SETTINGS
        compose.onNodeWithText("Mapa de Lucía").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Hecho").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/assistant-map.png")
    }

    @Test fun mcpOpensFromTheDrawerWithoutConnectingAndKeepsTheConversation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val state = AssistantState(Brain(FakeHands()))
        var externalRequests = 0
        compose.setContent {
            HubTheme { AssistantScreen(state, AssistantActions(manageHub = { externalRequests++ })) }
        }
        compose.onNodeWithText("Reproduce música").performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.assistant_menu)).performClick()
        compose.onNodeWithText(context.getString(R.string.phone_mcp_title)).performClick()
        compose.onNodeWithText(context.getString(R.string.scanner_none)).assertExists()
        assertEquals(0, externalRequests)

        compose.onNodeWithText(context.getString(R.string.phone_mcp_connect_external)).performClick()
        assertEquals(1, externalRequests)
        compose.onNodeWithContentDescription(context.getString(R.string.assistant_menu)).performClick()
        compose.onNodeWithText(context.getString(R.string.assistant_chat_tab)).performClick()
        compose.onNodeWithText("Reproduce música").assertExists()
        assertEquals(1, state.turns.size)
    }

    @Test fun copiesTheDisplayedReplyToTheClipboard() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val state = AssistantState(Brain(FakeHands()))
        compose.setContent { HubTheme { AssistantScreen(state) } }
        compose.onNodeWithText("Reproduce música").performClick()
        val expected = context.getString(R.string.say_playing)
        compose.onNodeWithText(expected).assertExists()
        compose.onNodeWithContentDescription("Copiar respuesta").performClick()
        compose.runOnIdle {
            val clip = context.getSystemService(ClipboardManager::class.java).primaryClip
            assertEquals(expected, clip?.getItemAt(0)?.text?.toString())
        }
    }

    @Test fun wakePanelShowsDictationAndAcceptsATypedRequest() {
        val state = AssistantState(Brain(FakeHands()))
        state.name = WakeName.LUNA
        state.openWakePanel()
        state.listening = true
        compose.setContent { HubTheme { AssistantScreen(state, AssistantActions(stop = { state.listening = false })) } }
        compose.onNodeWithText("Luna").assertExists()
        compose.onNodeWithText("Escuchando…").assertExists()
        compose.runOnIdle { state.heard("qué tiempo", final = false) }
        compose.onNodeWithText("qué tiempo").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/assistant-wake.png")

        compose.runOnIdle { state.whisperDecoding = true }
        compose.onNodeWithText("Transcribiendo con Whisper…").assertExists()
        compose.runOnIdle {
            state.dictationComparison = DictationComparison("qué tiempo hace", "¿Qué tiempo hace?", 150L)
            state.heard("qué tiempo hace", final = true)
        }
        compose.onNodeWithText("qué tiempo hace").assertExists()
        compose.onNodeWithText("Escuchando…").assertDoesNotExist()
        compose.onNodeWithText("Whisper (+150 ms) · ¿Qué tiempo hace?").assertExists()
        val input = compose.onNodeWithText("Escribe tu petición…")
        input.performClick()
        input.performTextInput("pon una alarma a las 7")
        val res = ApplicationProvider.getApplicationContext<Context>().resources
        compose.onNodeWithContentDescription(res.getString(R.string.send)).performClick()
        compose.onNodeWithText("Alarma a las 7:00.").assertExists()
        compose.onNodeWithText("Abrir chat").performClick()
        compose.onNodeWithContentDescription("Nuevo chat").assertExists()
        compose.onNodeWithText("Alarma a las 7:00.").assertExists()
    }

    @Test fun newChatClearsTheConversationAndCancelsThePendingCall() {
        val hands = FakeHands()
        val state = AssistantState(Brain(hands))
        compose.setContent { HubTheme { AssistantScreen(state) } }
        compose.runOnIdle {
            state.send("llama a Ana", byVoice = false)
            assertEquals(true, state.askingToCall)
            state.typed = "Borrador"
        }
        compose.onNodeWithContentDescription("Nuevo chat").performClick()
        compose.onNodeWithText("Reproduce música").assertExists()
        compose.onNodeWithText("llama a Ana").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("", state.typed)
            assertEquals(0, state.health.total)
            state.send("sí", byVoice = false)
            assertEquals(emptyList<String>(), hands.done)
        }
    }

    /** «Vamos a contar» turns the screen into the count; − and + fix a line; the total is big enough to read from afar. */
    @Test fun counting() {
        val state = AssistantState(Brain(FakeHands()))
        compose.setContent { HubTheme { AssistantScreen(state) } }
        compose.onNodeWithText("Vamos a contar").performClick()
        compose.onNodeWithText("Guardar en Notas").assertExists()
        listOf("toma corriente toma corriente toma corriente", "tomacorriente", "interruptor simple", "cinco focos", "interruptor de tres vías")
            .forEach { state.hearTally(it) }
        state.tallyListening = true
        compose.waitForIdle()
        compose.onNodeWithText("11").assertExists()
        compose.onNodeWithContentDescription("Quitar uno a Foco").performClick()
        compose.onNodeWithText("10").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/assistant-count.png")
    }
}
