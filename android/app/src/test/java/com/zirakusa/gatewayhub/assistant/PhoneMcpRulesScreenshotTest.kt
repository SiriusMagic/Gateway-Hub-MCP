package com.zirakusa.gatewayhub.assistant

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.gatewayhub.ui.HubTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Lucía › MCP: the «Reglas de sentimientos» card, with the total and Lucía's own rules unfolded. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-w400dp-h860dp-xxhdpi")
class PhoneMcpRulesScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ownRules() {
        compose.setContent { HubTheme { PhoneMcpScreen(null) {} } }
        compose.onNodeWithText("${Examples.rules.size} reglas en total · ${Examples.actions.size} las hace Lucía sin ninguna app").assertExists()
        compose.onNodeWithText("Ver las reglas de Lucía").performClick()
        compose.onNodeWithText("«pon música de Soda Stereo»").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/mcp-rules.png")
    }
}
