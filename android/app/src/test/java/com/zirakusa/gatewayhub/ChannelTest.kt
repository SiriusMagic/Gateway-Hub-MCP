package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.channel.ChannelRunner
import com.zirakusa.gatewayhub.channel.ChannelSession
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.IssuedPackage
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.data.MemoryLedger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Phase 3: the phone's channel forwards the hub's requests to the linked app over the local bridge. */
@RunWith(RobolectricTestRunner::class)
class ChannelTest {
    private val stone = "com.zirakusa.stone.kotlin.preview"
    private val sent = mutableListOf<JSONObject>()
    private val forwarded = mutableListOf<Pair<String, JSONObject>>()
    private val notified = mutableListOf<Pair<String, JSONObject>>()
    private var answer: (JSONObject) -> String = { m -> JSONObject().put("jsonrpc", "2.0").put("id", m.get("id")).put("result", JSONObject().put("ok", true)).toString() }
    private var hold = false
    private val session = ChannelSession(CoroutineScope(Dispatchers.Unconfined), { sent += JSONObject(it) },
        forward = { app, text -> JSONObject(text).also { forwarded += app to it }.let { m -> if (hold) kotlinx.coroutines.awaitCancellation() else answer(m) } },
        notify = { app, text -> notified += app to JSONObject(text) })

    private fun req(id: String, app: String = stone) = JSONObject().put("t", "req").put("id", id).put("app", app)
        .put("message", JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "tools/call").put("params", JSONObject().put("name", "player_state"))).toString()

    @Test fun requestsGetAChannelIdAndTheirAnswerGoesBack() {
        session.handle(req("5"))
        assertEquals(stone, forwarded.single().first)
        assertEquals("ch-5", forwarded.single().second.getString("id"))         // unique per channel, so the app can cancel the right one
        val res = sent.single()
        assertEquals("res", res.getString("t")); assertEquals("5", res.getString("id"))
        assertTrue(res.getJSONObject("reply").getJSONObject("result").getBoolean("ok"))
    }

    @Test fun failuresAreAnsweredWithTheirCode() {
        answer = { throw HubException("BRIDGE_TIMEOUT", "") }
        session.handle(req("6"))
        assertEquals("BRIDGE_TIMEOUT", sent.single().getJSONObject("reply").getJSONObject("error").getString("message"))
    }

    @Test fun cancelReachesTheSameAppAndTheLateAnswerIsDropped() {
        hold = true
        session.handle(req("7"))
        session.handle(JSONObject().put("t", "cancel").put("id", "7").toString())
        val (app, note) = notified.single()
        assertEquals(stone, app)
        assertEquals("notifications/cancelled", note.getString("method"))
        assertEquals("ch-7", note.getJSONObject("params").getString("requestId"))
        assertTrue(sent.isEmpty())
        session.handle(JSONObject().put("t", "cancel").put("id", "999").toString())   // unknown: nothing sent
        assertEquals(1, notified.size)
    }

    @Test fun badMessagesAreIgnored() {
        for (text in listOf("not json", "{}", req("8", app = "../evil"), JSONObject().put("t", "req").put("id", "x".repeat(30)).toString(), "x".repeat((1 shl 20) + 1)))
            session.handle(text)
        assertTrue(forwarded.isEmpty() && sent.isEmpty())
    }

    @Test fun registrationListsOnlyLinkedProvidersWithTheirPackages() {
        val ledger = MemoryLedger().apply {
            saveInstance(stone, "inst_1")
            savePackages(listOf(IssuedPackage("pkg_abcdefgh01", stone, "*", "Lectura", listOf("leer"), 99, "active")))
        }
        val apps = listOf(LocalApp(stone, "Stone", "1", "music_", true, "x", bridge = true),
            LocalApp("com.not.linked", "Otra", "1", "other_", true, "y", bridge = true),
            LocalApp("com.stranger", "Ajena", "1", "evil_", false, "z", bridge = true))
        val r = JSONObject(ChannelRunner.registration(ledger) { apps })
        assertEquals("register", r.getString("t"))
        val providers = r.getJSONArray("providers")
        assertEquals(1, providers.length())
        val p = providers.getJSONObject(0)
        assertEquals("music_", p.getString("prefix")); assertEquals("pkg_abcdefgh01", p.getJSONArray("packages").getJSONObject(0).getString("id"))
    }

    @Test fun aDeviceCredentialIsRequestedWithTheOwnerToken() = runBlocking {
        val transport = FakeTransport { 200 to """{"device":{"id":"0123456789ab","name":"Pixel","created":1},"secret":"dev_SECRET"}""" }
        assertEquals("0123456789ab" to "dev_SECRET", HubApi("https://hub.example", "owner", transport).createDevice("Pixel"))
        assertEquals("https://hub.example/api/admin/devices", transport.sent.single().url)
        assertEquals("owner", transport.sent.single().token)
    }
}
