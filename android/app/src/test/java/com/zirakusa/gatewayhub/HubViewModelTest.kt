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
}
