package com.zirakusa.gatewayhub

import android.os.Looper
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.LinkAnswer
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.ui.HubViewModel
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Scan + sync: Stone (same developer, Redireccionar a Hub on) gets linked with no copy and paste. */
@RunWith(RobolectricTestRunner::class)
class AutoLinkTest {
    private val stone = LocalApp("com.zirakusa.stone.kotlin.preview", "Stone", "1.7.13", "music_", true, "com.zirakusa.stone.kotlin.preview.hublink")
    private val stoneLink = "https://relay.example/d/abcdefghijkl12/mcp?token=stn_ABCDEFGHIJKLMNOPQRSTUVWXYZ012345"
    private fun scanJson(linked: Boolean) = SCAN_JSON.replace(""""linked":true,"online":false""", """"linked":$linked,"online":$linked""")

    private class Hub(var linked: Boolean = false) { val sent = mutableListOf<String>() }

    private fun vm(hub: Hub, sessions: MemorySessions, local: List<LocalApp>, answer: (LocalApp) -> LinkAnswer, asked: MutableList<String>): HubViewModel {
        val transport = FakeTransport { r ->
            hub.sent += "${r.method} ${r.url.substringAfter("/api/admin")}"
            when {
                r.url.endsWith("/scan") -> 200 to scanJson(hub.linked)
                r.url.endsWith("/stone") -> { assertEquals(stoneLink, JSONObject(r.body!!).getString("link")); hub.linked = true; 200 to """{"message":"Stone vinculado y en línea."}""" }
                else -> 200 to STATE_JSON
            }
        }
        return HubViewModel(sessions, "https://hub.example", { local }, { app -> asked += app.packageName; answer(app) },
            newApi = { url, token -> HubApi(url, token, transport) })
    }
    private fun HubViewModel.settle() { repeat(200) { shadowOf(Looper.getMainLooper()).idle(); if (!state.value.loading) return; Thread.sleep(10) } }

    @Test fun scanLinksStoneByItself() {
        val hub = Hub(); val asked = mutableListOf<String>()
        val vm = vm(hub, MemorySessions("https://hub.example" to "owner"), listOf(stone), { LinkAnswer(stoneLink, null, null) }, asked); vm.settle()
        vm.scan(); vm.settle()
        assertEquals(listOf(stone.packageName), asked)
        assertTrue(hub.sent.contains("POST /stone"))
        assertTrue(vm.state.value.scan!!.first { it.prefix == "music_" }.remote!!.linked)
        assertEquals("Stone vinculado y en línea.", vm.state.value.notice)
    }

    @Test fun nothingIsAskedFromStrangersOrWithSyncOff() {
        val stranger = stone.copy(packageName = "com.example.fake", sameDeveloper = false, linkAuthority = "com.example.fake.hublink")
        val hub = Hub(); val asked = mutableListOf<String>()
        val vm = vm(hub, MemorySessions("https://hub.example" to "owner"), listOf(stranger), { LinkAnswer(stoneLink, null, null) }, asked); vm.settle()
        vm.scan(); vm.settle()
        assertTrue(asked.isEmpty()); assertTrue("POST /stone" !in hub.sent)
        val sessions = MemorySessions("https://hub.example" to "owner")
        val off = vm(Hub(), sessions, listOf(stone), { LinkAnswer(stoneLink, null, null) }, asked); off.settle()
        off.setAutoSync(false); off.scan(); off.settle()
        assertTrue(asked.isEmpty())
        assertEquals(false, sessions.flags["autoSync"])
    }

    @Test fun whenStoneSaysNoTheReasonIsShownAndNothingIsSent() {
        val hub = Hub(); val asked = mutableListOf<String>()
        val vm = vm(hub, MemorySessions("https://hub.example" to "owner"), listOf(stone),
            { LinkAnswer(null, "REDIRECT_OFF", "Activa «Redireccionar a Hub» en Stone.") }, asked); vm.settle()
        vm.scan(); vm.settle()
        assertEquals(1, asked.size)
        assertTrue("POST /stone" !in hub.sent)
        assertTrue(vm.state.value.notice!!.contains("Redireccionar a Hub"))
    }
}
