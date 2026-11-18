package com.zirakusa.gatewayhub

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.gatewayhub.data.CreatedAgent
import com.zirakusa.gatewayhub.data.HubJson
import com.zirakusa.gatewayhub.ui.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Every screen with realistic data, for a visual check (build/screenshots/). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private val hub = HubJson.state(STATE_JSON)
    private val connected = UiState(hubUrl = "https://stone-hub.example.workers.dev", connected = true, hub = hub)
    private object NoActions : HubActions {
        override fun refresh() {}; override fun logout() {}; override fun dismissMessage() {}; override fun dismissCreated() {}
        override fun setPaused(id: String, paused: Boolean) {}; override fun revoke(id: String) {}
        override fun createAgent(name: String, apps: List<String>, readOnly: Boolean, days: Int) {}
        override fun linkStone(link: String) {}; override fun unlinkStone() {}
    }

    private fun shot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { HubTheme { content() } }
        compose.onRoot().captureRoboImage("build/screenshots/hub-$name.png")
    }

    @Test fun login() = shot("login") { LoginScreen(UiState(hubUrl = "https://stone-hub.example.workers.dev")) { _, _ -> } }
    @Test fun agents() = shot("agents") { HomeScreen(connected, NoActions, HubTab.AGENTS) }
    @Test fun apps() = shot("apps") { HomeScreen(connected, NoActions, HubTab.APPS) }
    @Test fun metrics() = shot("metrics") { HomeScreen(connected, NoActions, HubTab.METRICS) }
    @Test fun createdLink() = shot("created") {
        HomeScreen(connected.copy(created = CreatedAgent(hub.agents[0], "https://stone-hub.example.workers.dev/mcp?token=gw_EXAMPLEexampleEXAMPLE", "gw_EXAMPLE")), NoActions)
    }
}
