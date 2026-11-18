package com.zirakusa.gatewayhub

import android.os.Looper
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.HubJson
import com.zirakusa.gatewayhub.ui.HubViewModel
import com.zirakusa.gatewayhub.ui.UiText
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Phase 5: another phone joins with a one-use code and keeps only its channel credential. */
@RunWith(RobolectricTestRunner::class)
class PhonesTest {
    private fun HubViewModel.settle() { repeat(200) { shadowOf(Looper.getMainLooper()).idle(); if (!state.value.loading) return; Thread.sleep(10) } }

    @Test fun joiningWithACodeMakesASatelliteWithoutTheOwnerToken() {
        val transport = FakeTransport { r ->
            if (r.url == "https://hub.example/pair" && JSONObject(r.body!!).getString("code") == "ABCDE23456")
                200 to """{"device":{"id":"0123456789ab","name":"Tablet","created":1},"secret":"dev_TABLET"}""" else 401 to "{}"
        }
        val sessions = MemorySessions()
        val vm = HubViewModel(sessions, "https://hub.example", hubTransport = transport)
        vm.join("hub.example", "wrong00000", "Tablet"); vm.settle()
        assertEquals(UiText.Res(R.string.phone_bad_code), vm.state.value.error)
        assertFalse(vm.state.value.satellite)
        vm.join("https://hub.example/admin", " ABCDE23456 ", "Tablet"); vm.settle()
        assertTrue(vm.state.value.satellite)
        assertEquals("https://hub.example", vm.state.value.hubUrl)
        assertEquals("https://hub.example" to ("0123456789ab" to "dev_TABLET"), sessions.device)
        assertNull(sessions.saved)                                                   // no owner token on this phone
        assertTrue(transport.sent.all { it.token.isEmpty() })
        // A restart keeps it a satellite; leaving forgets the credential.
        assertTrue(HubViewModel(sessions, "https://hub.example").state.value.satellite)
        vm.leaveHub()
        assertNull(sessions.device); assertFalse(vm.state.value.satellite)
    }

    @Test fun theOwnerMakesCodesAndRevokesPhones() {
        val transport = FakeTransport { r -> when {
            r.url.endsWith("/pairing") -> 200 to """{"code":"ABCDE23456","expiresAt":99}"""
            r.method == "DELETE" -> 200 to """{"id":"0123456789ab"}"""
            else -> 200 to STATE_JSON.replace("\"environment\":\"active\"", "\"environment\":\"active\",\"devices\":[{\"id\":\"0123456789ab\",\"name\":\"Tablet\",\"online\":true,\"providers\":[{\"prefix\":\"music2_\"}]}]")
        } }
        val vm = HubViewModel(MemorySessions("https://hub.example" to "owner"), "https://hub.example", newApi = { url, token -> HubApi(url, token, transport) })
        vm.settle()
        assertEquals(listOf("music2_"), vm.state.value.hub!!.devices.single().providers)
        vm.createPairing(); vm.settle()
        assertEquals("ABCDE23456" to 99L, vm.state.value.pairing)
        vm.revokeDevice("0123456789ab"); vm.settle()
        assertTrue(transport.sent.any { it.method == "DELETE" && it.url.endsWith("/api/admin/devices/0123456789ab") })
        assertEquals(UiText.Res(R.string.phone_revoked), vm.state.value.notice)
    }

    @Test fun olderHubsWithoutDevicesStillParse() {
        assertTrue(HubJson.state(STATE_JSON).devices.isEmpty())
        assertEquals("BAD_PAIRING_CODE", (runCatching { runBlocking { HubApi.pair("https://hub.example", "X", "n", FakeTransport { 401 to "{}" }) } }.exceptionOrNull() as HubException).code)
    }
}
