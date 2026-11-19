package com.zirakusa.gatewayhub

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.gatewayhub.data.AppSettings
import com.zirakusa.gatewayhub.data.CreatedAgent
import com.zirakusa.gatewayhub.data.DeviceInfo
import com.zirakusa.gatewayhub.data.McpMode
import com.zirakusa.gatewayhub.data.ThemeChoice
import com.zirakusa.gatewayhub.data.HubJson
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.data.ScanMerge
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
@Config(qualifiers = "es-w400dp-h860dp-xxhdpi")
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private val hub = HubJson.state(STATE_JSON)
    private val connected = UiState(hubUrl = "https://stone-hub.example.workers.dev", connected = true, hub = hub)
    private object NoActions : HubActions {
        override fun refresh() {}; override fun logout() {}; override fun dismissMessage() {}; override fun dismissCreated() {}
        override fun setPaused(id: String, paused: Boolean) {}; override fun revoke(id: String) {}
        override fun createAgent(name: String, apps: List<String>, readOnly: Boolean, days: Int) {}
        override fun scan() {}
        override fun setEnvironment(target: String) {}; override fun updateSettings(change: (AppSettings) -> AppSettings) {}
        override fun createAccess(cfToken: String) {}; override fun updateHub(cfToken: String) {}; override fun rotateOwner(cfToken: String) {}
        override fun testBridge(packageName: String) {}
        override fun linkProvider(packageName: String) {}; override fun refreshPackages(packageName: String) {}
        override fun offerPackage(packageName: String, agent: String, label: String, acts: List<String>, days: Int) {}
        override fun revokePackage(packageName: String, id: String) {}
        override fun createPairing() {}; override fun revokeDevice(id: String) {}; override fun leaveHub() {}
    }

    private fun shot(name: String, theme: ThemeChoice = ThemeChoice.NIGHT, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { HubTheme(theme) { content() } }
        compose.onRoot().captureRoboImage("build/screenshots/hub-$name.png")
    }

    @Test fun login() = shot("login") { LoginScreen(UiState(hubUrl = "https://stone-hub.example.workers.dev"), { }) }
    @Test fun agents() = shot("agents") { HomeScreen(connected, NoActions, HubTab.AGENTS) }
    @Test fun scanner() = shot("scanner") { HomeScreen(connected.copy(scan = ScanMerge.merge(
        listOf(LocalApp("com.zirakusa.stone.kotlin.preview", "Stone Kotlin · Prueba", "1.7.13", "music_", true, bridge = true),
            LocalApp("com.example.lookalike", "Stone Pro", "9.9", "music_", false)),
        HubJson.scan(SCAN_JSON))), NoActions, HubTab.SCANNER) }
    @Test fun apps() = shot("apps") { HomeScreen(connected, NoActions, HubTab.APPS) }
    @Test fun metrics() = shot("metrics") { HomeScreen(connected, NoActions, HubTab.METRICS) }
    @Test fun createdLink() = shot("created") {
        HomeScreen(connected.copy(created = CreatedAgent(hub.agents[0], "https://stone-hub.example.workers.dev/mcp?token=gw_EXAMPLEexampleEXAMPLE", "gw_EXAMPLE")), NoActions)
    }
    @Test fun welcome() = shot("welcome") { WelcomeScreen { } }
    @Test fun settings() = shot("settings") { HomeScreen(connected.copy(settings = AppSettings(language = "es")), NoActions, HubTab.SETTINGS) }
    @Test fun settingsBackgroundPaused() = shot("settings-paused", ThemeChoice.OCEAN) {
        HomeScreen(connected.copy(hub = hub.copy(environment = "paused"), settings = AppSettings("es", ThemeChoice.OCEAN, mode = McpMode.BACKGROUND)), NoActions, HubTab.SETTINGS)
    }
    @Test fun lightTheme() = shot("agents-light", ThemeChoice.LIGHT) { HomeScreen(connected, NoActions, HubTab.AGENTS) }
    @Test @Config(qualifiers = "en-w400dp-h860dp-xxhdpi") fun settingsEnglish() = shot("settings-en", ThemeChoice.FOREST) {
        HomeScreen(connected.copy(settings = AppSettings("en", ThemeChoice.FOREST)), NoActions, HubTab.SETTINGS)
    }
    @Test fun loginWithCloudflare() = shot("login-cloudflare") {
        LoginScreen(UiState(hubUrl = "https://stone-hub.example.workers.dev"), { })
    }

    @Test fun satellite() = shot("satellite") {
        SatelliteScreen(UiState(hubUrl = "https://stone-hub.example.workers.dev", satellite = true, providerApps = listOf(
            LocalApp("com.zirakusa.stone.kotlin.preview", "Stone", "1.7.18", "music_", true, bridge = true))), NoActions)
    }
    @Test fun phones() = shot("phones") {
        HomeScreen(connected.copy(settings = AppSettings("es"), pairing = "ABCDE23456" to System.currentTimeMillis() + 300_000,
            hub = hub.copy(devices = listOf(DeviceInfo("0123456789ab", "Pixel 8", true, listOf("music_")), DeviceInfo("ba9876543210", "Tablet", false, listOf("music2_"))))),
            NoActions, HubTab.SETTINGS)
    }
}
