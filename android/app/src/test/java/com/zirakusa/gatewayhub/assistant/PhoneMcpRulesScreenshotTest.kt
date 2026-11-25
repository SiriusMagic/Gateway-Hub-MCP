package com.zirakusa.gatewayhub.assistant

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.gatewayhub.ui.HubTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Lucía › MCP no longer shows «Reglas de sentimientos» (they live only in RS). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-w400dp-h860dp-xxhdpi")
class PhoneMcpRulesScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun noRulesInMcp() {
        compose.setContent { HubTheme { PhoneMcpScreen(null, {}) } }
        compose.onNodeWithText("Apps de este teléfono").assertExists()
        compose.onNodeWithText("Reglas de sentimientos").assertDoesNotExist()
        compose.onNodeWithText("Ver las reglas de Lucía").assertDoesNotExist()
        compose.onNodeWithText("${Examples.rules.size} reglas en total", substring = true).assertDoesNotExist()
        compose.onRoot().captureRoboImage("build/screenshots/mcp-rules.png")
    }
}
