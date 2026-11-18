package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubException
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric gives the real org.json (the plain JVM only has Android stubs). */
@RunWith(RobolectricTestRunner::class)
class HubApiTest {
    @Test fun onlyHttpsAddressesWithoutPathOrUserInfo() {
        assertEquals("https://hub.example", HubApi.normalize("hub.example/"))
        assertEquals("https://hub.example:8443", HubApi.normalize(" https://hub.example:8443/admin?x=1 "))
        for (bad in listOf("http://hub.example", "ftp://hub.example", "https://user:pw@hub.example", "https://", "javascript:alert(1)")) {
            try { HubApi.normalize(bad); fail(bad) } catch (e: HubException) { assertEquals("INVALID_URL", e.code) }
        }
    }

    @Test fun readsTheStateTheHubSends() = runBlocking {
        val state = HubApi("https://hub.example", "owner", FakeTransport()).state()
        assertEquals("0.0.1", state.version)
        assertEquals(4, state.modules.size)
        assertTrue(state.stoneLinked)
        assertTrue(state.agents[0].allApps)
        assertTrue(state.agents[1].readOnly && state.agents[1].paused)
        assertEquals(180, state.week.calls)
        assertEquals("music_player_state", state.week.byTool[0].name)
    }

    @Test fun theTokenGoesInTheHeaderNeverInTheUrl() = runBlocking {
        val transport = FakeTransport { if (it.method == "POST") 200 to """{"agent":{"id":"a1b2c3d4e5f6","name":"X","modules":["calc_"],"readOnly":true,
            "paused":false,"status":"active","expiresAt":0,"created":1,"lastUsed":0},"url":"https://hub.example/mcp?token=gw_abc","token":"gw_abc"}"""
            else 200 to STATE_JSON }
        val api = HubApi("https://hub.example", "owner-secret", transport)
        val created = api.createAgent("X", listOf("calc_"), readOnly = true, days = 30)
        assertEquals("gw_abc", created.token)
        val sent = transport.sent.single()
        assertEquals("https://hub.example/api/admin/agents", sent.url)
        assertEquals("owner-secret", sent.token)
        assertFalse(sent.url.contains("owner-secret"))
        val body = JSONObject(sent.body!!)
        assertEquals(true, body.getBoolean("readOnly")); assertEquals(30, body.getInt("days"))
        api.setPaused("a1b2c3d4e5f6", true); api.revoke("a1b2c3d4e5f6")
        assertEquals(listOf("POST /agents/a1b2c3d4e5f6/pause", "DELETE /agents/a1b2c3d4e5f6"),
            transport.sent.drop(1).map { "${it.method} ${it.url.substringAfter("/api/admin")}" })
    }

    @Test fun errorsKeepTheHubsCodeAndMessage() = runBlocking {
        suspend fun code(status: Int, body: String) = try {
            HubApi("https://hub.example", "t", FakeTransport { status to body }).state(); "none"
        } catch (e: HubException) { e.code + ":" + e.message }
        assertTrue(code(401, "{}").startsWith("UNAUTHORIZED"))
        assertTrue(code(429, "{}").startsWith("RATE_LIMITED"))
        assertEquals("INVALID_LINK:Solo enlaces del relay", code(400, """{"error":{"code":"INVALID_LINK","message":"Solo enlaces del relay"}}"""))
        val down = try { HubApi("https://hub.example", "t", { _, _, _, _ -> throw java.io.IOException("x") }).state(); "" }
            catch (e: HubException) { e.code }
        assertEquals("NETWORK", down)
    }
}
