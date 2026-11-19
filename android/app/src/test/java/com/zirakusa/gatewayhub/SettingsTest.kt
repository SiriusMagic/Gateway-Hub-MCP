package com.zirakusa.gatewayhub

import android.os.Looper
import com.zirakusa.gatewayhub.data.ActivityReport
import com.zirakusa.gatewayhub.data.AppSettings
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubJson
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.data.McpMode
import com.zirakusa.gatewayhub.data.MemorySettingsStore
import com.zirakusa.gatewayhub.data.PrefsSettingsStore
import com.zirakusa.gatewayhub.data.ThemeChoice
import com.zirakusa.gatewayhub.ui.HubViewModel
import com.zirakusa.gatewayhub.ui.UiText
import com.zirakusa.gatewayhub.ui.linkFailure
import com.zirakusa.gatewayhub.watch.ActivityTally
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SettingsTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun defaultsAllowScreenshotsServeInTheBackgroundAndAskTheLanguage() {
        val d = PrefsSettingsStore(context).load()
        assertNull(d.language)                       // the first launch asks
        assertFalse(d.blockScreenshots)              // screenshots on by default; only the person blocks them
        assertEquals(McpMode.BACKGROUND, d.mode)          // the AI reaches Stone without Gateway on screen
        assertTrue(d.activityNotifications)
    }

    @Test fun settingsAreSavedAndSurviveLogout() {
        val store = PrefsSettingsStore(context)
        val vm = HubViewModel(MemorySessions("https://hub.example" to "owner"), "https://hub.example",
            newApi = { url, token -> HubApi(url, token, FakeTransport()) }, settingsStore = store)
        vm.updateSettings { it.copy(language = "en", theme = ThemeChoice.LIGHT, blockScreenshots = true, mode = McpMode.BACKGROUND, summaryMinutes = 60) }
        vm.logout()
        assertEquals(AppSettings("en", ThemeChoice.LIGHT, true, McpMode.BACKGROUND, true, 60), vm.state.value.settings)
        assertEquals(vm.state.value.settings, PrefsSettingsStore(context).load())
        context.getSharedPreferences("hub_settings", 0).edit().putString("theme", "EVIL").putInt("summaryMinutes", 1).apply()
        assertEquals(ThemeChoice.NIGHT, store.load().theme)    // unknown values fall back to defaults
        assertEquals(30, store.load().summaryMinutes)
    }

    @Test fun environmentAndActivityTalkToTheHub() = runBlocking {
        val transport = FakeTransport { if (it.url.contains("/activity")) 200 to ACTIVITY_JSON else 200 to "{}" }
        val api = HubApi("https://hub.example", "owner", transport)
        api.setEnvironment("paused")
        assertEquals("POST", transport.sent[0].method)
        assertEquals("https://hub.example/api/admin/environment", transport.sent[0].url)
        assertEquals("paused", JSONObject(transport.sent[0].body!!).getString("state"))
        val activity = api.activity(1234)
        assertTrue(transport.sent[1].url.endsWith("/api/admin/activity?since=1234"))
        assertEquals(3, activity.metrics.calls)
        assertEquals("active", HubJson.state(STATE_JSON).environment)
        assertEquals("active", HubJson.state(STATE_JSON.replace(""","environment":"active"""", "")).environment) // older hubs
    }

    @Test fun theSummaryNamesAgentsAndAddsUpPolls() {
        val agents = HubJson.state(STATE_JSON).agents
        val one = HubJson.activity(ACTIVITY_JSON).metrics
        val tally = ActivityTally() + one + one
        assertEquals(6, tally.calls); assertEquals(2, tally.errors)
        assertEquals(listOf("Claude", "Propietario"), tally.agentNames(agents, "Propietario", "(revocado)"))
    }

    @Test fun theExportedReportCarriesNoSecret() {
        val report = ActivityReport.json("https://hub.example", HubJson.state(STATE_JSON), now = 5)
        val o = JSONObject(report)
        assertEquals(2, o.getJSONArray("agents").length())
        assertEquals(180, o.getJSONObject("last7d").getInt("calls"))
        assertFalse(report.contains("gw_") || report.contains("hub_") || report.contains("token"))
    }

    @Test @Config(qualifiers = "en") fun englishTexts() {
        assertEquals("Wrong owner token.", UiText.Res(R.string.err_unauthorized).resolve(context))
        assertEquals("Stone: The app did not answer.", linkFailure("Stone", "NO_ANSWER", null).resolve(context))
        assertEquals("MCP active — Gateway Hub", context.getString(R.string.notif_active_title))
    }

    @Test @Config(qualifiers = "es") fun spanishTextsAndTheAppsOwnWords() {
        assertEquals("Token del propietario incorrecto.", UiText.Res(R.string.err_unauthorized).resolve(context))
        assertEquals("Stone: Activa X", linkFailure("Stone", "REDIRECT_OFF", "Activa X").resolve(context))
        assertEquals("a\nb", UiText.Lines(listOf(UiText.Raw("a"), UiText.Raw("b"))).resolve(context))
    }

    @Test fun theViewModelStartsFromTheSavedSettings() {
        val saved = AppSettings(language = "es", theme = ThemeChoice.OCEAN)
        val vm = HubViewModel(MemorySessions(), "https://hub.example", settingsStore = MemorySettingsStore(saved))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(saved, vm.state.value.settings)
    }
}
