package com.zirakusa.gatewayhub

import android.os.Looper
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.ui.HubViewModel
import com.zirakusa.gatewayhub.ui.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class HubViewModelTest {
    private fun vm(sessions: MemorySessions, transport: FakeTransport) =
        HubViewModel(sessions, "https://hub.example", newApi = { url, token -> HubApi(url, token, transport) })

    /** Lets the IO work finish and the main thread apply it. */
    private fun HubViewModel.settle() {
        repeat(200) { shadowOf(Looper.getMainLooper()).idle(); if (!state.value.loading) return; Thread.sleep(10) }
    }

    @Test fun aWrongTokenIsNeverSaved() {
        val sessions = MemorySessions()
        val vm = vm(sessions, FakeTransport { 401 to "{}" })
        vm.connect("https://hub.example#hub_wrongwrongwrongwrongwrong"); vm.settle()
        assertFalse(vm.state.value.connected)
        assertNull(sessions.saved)
        assertEquals(UiText.Res(R.string.err_unauthorized), vm.state.value.error)
    }

    @Test fun theRightTokenIsSavedAndTheHubLoaded() {
        val sessions = MemorySessions()
        val vm = vm(sessions, FakeTransport())
        vm.connect("  https://hub.example/admin#hub_OWNERownerOWNERowner12  "); vm.settle()
        assertTrue(vm.state.value.connected)
        assertEquals("https://hub.example" to "hub_OWNERownerOWNERowner12", sessions.saved)
        assertEquals(2, vm.state.value.hub!!.agents.size)
    }

    @Test fun aRevokedOwnerTokenEndsTheSession() {
        val sessions = MemorySessions("https://hub.example" to "old")
        val transport = FakeTransport()
        val vm = vm(sessions, transport); vm.settle()
        assertTrue(vm.state.value.connected)
        transport.reply = { 401 to "{}" }
        vm.refresh(); vm.settle()
        assertFalse(vm.state.value.connected)
        assertNull(sessions.saved)
    }

    @Test fun aNewLinkIsShownOnceAndForgotten() {
        val transport = FakeTransport { if (it.method == "POST") 200 to """{"agent":{"id":"a1b2c3d4e5f6","name":"X","modules":["*"],"readOnly":false,
            "paused":false,"status":"active","expiresAt":0,"created":1,"lastUsed":0},"url":"https://hub.example/mcp?token=gw_once","token":"gw_once"}"""
            else 200 to STATE_JSON }
        val vm = vm(MemorySessions("https://hub.example" to "owner"), transport); vm.settle()
        vm.createAgent("X", listOf("music_"), false, 7); vm.settle()
        assertNotNull(vm.state.value.created)
        vm.dismissCreated()
        assertNull(vm.state.value.created)
        assertFalse(vm.state.value.toString().contains("gw_once"))
    }

    @Test fun aStoneTokenIsExplainedAndNeverSent() {
        val transport = FakeTransport()
        val vm = vm(MemorySessions(), transport)
        vm.connect("https://stone-relay.example.workers.dev/d/3e6076f88b2ae131ea6e/mcp?token=stn_abcdefghijklmnopqrstuv"); vm.settle()
        assertEquals(UiText.Res(R.string.err_stone_link), vm.state.value.error)
        assertTrue(transport.sent.isEmpty())
    }

    /** The shared hub answers: a new space, its state, and sign-in codes. */
    private fun sharedHub(transport: FakeTransport = FakeTransport()) = transport.apply { reply = { r -> when {
        r.url.endsWith("/register") -> 200 to """{"hub":"https://hub.example","owner":"hub_s0123456789abcdef_OWNERownerOWNERowner12",
            "device":{"id":"0a0b0c0d0e0f","name":"Pixel","created":1},"secret":"dev_s0123456789abcdef_DEVICEdeviceDEVICE"}"""
        r.url.endsWith("/connect-code") -> 200 to """{"code":"12345678","expiresAt":${System.currentTimeMillis() + 600_000}}"""
        else -> 200 to STATE_JSON.replace("\"environment\":\"active\"", "\"environment\":\"active\",\"shared\":true,\"publicUrl\":\"https://hub.example/mcp\"")
    } } }

    private fun HubViewModel.settleStart() {
        repeat(300) { shadowOf(Looper.getMainLooper()).idle(); val s = state.value
            if (!s.starting && !s.loading && (s.connect != null || s.startFailed)) return; Thread.sleep(10) }
    }

    @Test fun theFirstLaunchJoinsTheSharedHubWithNothingToType() {
        val sessions = MemorySessions(); val transport = sharedHub()
        val vm = HubViewModel(sessions, "https://hub.example", newApi = { url, token -> HubApi(url, token, transport) },
            hubTransport = transport, autoStart = true, phoneName = "Pixel")
        vm.settleStart()
        val s = vm.state.value
        assertTrue(s.connected); assertTrue(s.hub!!.shared); assertEquals("https://hub.example/mcp", s.hub!!.publicUrl)
        assertEquals("12345678", s.connect!!.first)
        assertEquals("https://hub.example" to "hub_s0123456789abcdef_OWNERownerOWNERowner12", sessions.saved)
        assertEquals("0a0b0c0d0e0f" to "dev_s0123456789abcdef_DEVICEdeviceDEVICE", sessions.device("https://hub.example"))
        assertTrue(transport.sent.first().body!!.contains("Pixel"))
        // The owner secret travels only in the header, never in a URL.
        assertTrue(transport.sent.none { it.url.contains("hub_s") })
    }

    @Test fun noInternetOnTheFirstLaunchOffersToTryAgain() {
        val transport = FakeTransport { throw java.io.IOException("offline") }
        val vm = HubViewModel(MemorySessions(), "https://hub.example", newApi = { url, token -> HubApi(url, token, transport) },
            hubTransport = transport, autoStart = true)
        vm.settleStart()
        assertTrue(vm.state.value.startFailed); assertFalse(vm.state.value.connected)
        sharedHub(transport); vm.start(); vm.settleStart()
        assertTrue(vm.state.value.connected)
    }

    @Test fun withoutAutoStartNothingIsSent() {
        val transport = FakeTransport()
        HubViewModel(MemorySessions(), "https://hub.example", hubTransport = transport).let { shadowOf(Looper.getMainLooper()).idle() }
        assertTrue(transport.sent.isEmpty())
    }
}
