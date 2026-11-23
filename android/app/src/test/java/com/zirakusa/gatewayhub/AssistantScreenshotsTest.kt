package com.zirakusa.gatewayhub

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.gatewayhub.assistant.AssistantScreen
import com.zirakusa.gatewayhub.assistant.AssistantState
import com.zirakusa.gatewayhub.assistant.Brain
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
