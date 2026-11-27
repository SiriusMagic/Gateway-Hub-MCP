package com.zirakusa.gatewayhub.assistant

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollAction
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.gatewayhub.ui.HubTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** «RS» (Reconocimiento de intenciones) in the side menu next to MCP, and «Sentimientos» as one more app in MCP. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-w400dp-h860dp-xxhdpi")
class RsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun groupsAndSearch() {
        val all = rsGroups("")
        assertEquals(null, all.first().app)                                                     // Lucía's own first
        assertEquals(Examples.rules.size, all.sumOf { it.rules.size })                           // every rule once
        val music = rsGroups("cancion")                                                          // accents ignored
        assertTrue(music.all { g -> g.rules.all { "cancion" in Words.normalize(it.phrase + " " + (it.label ?: it.tool.orEmpty())) } })
        assertTrue(music.any { it.app?.id == "stone" })
        assertTrue(rsGroups("zzz nada").isEmpty())
        // The side menu: MCP stays as it was, RS right after it.
        val pages = AssistantPage.entries
        assertEquals(pages.indexOf(AssistantPage.MCP) + 1, pages.indexOf(AssistantPage.RS))
    }

    @Test fun rsPage() {
        compose.setContent { HubTheme { RsScreen(Interpreter.FEELINGS) } }
        compose.onNodeWithText("Reconocimiento de intenciones").assertExists()
        compose.onNodeWithText("Intérprete del modelo ahora: Reconocimiento de intenciones").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/rs.png")
    }

    @Test fun rsOpensEachApp() {
        val opened = mutableListOf<String>()
        compose.setContent { HubTheme { RsScreen(Interpreter.FEELINGS) { opened += it.id; it.id == "stone" } } }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Stone"))
        compose.onAllNodesWithText("Abrir")[0].performClick()                                   // Lucía's own card has none: the first is Stone
        assertEquals(listOf("stone"), opened)
        compose.onNodeWithText("No se pudo abrir esta app.").assertDoesNotExist()
        compose.onRoot().captureRoboImage("build/screenshots/rs-abrir.png")
    }

    @Test fun sentimientosIsOneMoreAppInMcp() {
        var opened = 0
        compose.setContent { HubTheme { PhoneMcpScreen(AssistantState(Brain(com.zirakusa.gatewayhub.FakeHands())), {}) { opened++ } } }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("${Examples.rules.size} reglas de sentimiento"))
        compose.onNodeWithText("Reconocimiento de intenciones").assertExists()
        compose.onNodeWithText("Abrir").performClick(); assertEquals(1, opened)                // «Abrir» → RS
        compose.onNodeWithText("Herramientas").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Integración pendiente", substring = true))
        compose.onNodeWithText("Integración pendiente", substring = true).assertExists()       // no tools made up
        compose.onRoot().captureRoboImage("build/screenshots/mcp-sentimientos.png")
    }
}
