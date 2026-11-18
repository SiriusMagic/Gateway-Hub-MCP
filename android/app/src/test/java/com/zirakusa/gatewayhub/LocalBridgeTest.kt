package com.zirakusa.gatewayhub

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.zirakusa.gatewayhub.data.BridgeConnector
import com.zirakusa.gatewayhub.data.BridgeProbe
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.data.LocalBridge
import com.zirakusa.gatewayhub.data.McpPipe
import com.zirakusa.gatewayhub.ui.HubViewModel
import com.zirakusa.gatewayhub.ui.UiText
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** Phase 1: the local MCP bridge with Stone over a Messenger (read-only, bounded, failures named). */
@RunWith(RobolectricTestRunner::class)
class LocalBridgeTest {
    private val stone = LocalApp("com.zirakusa.stone.kotlin.preview", "Stone", "1.7.16", "music_", true, "x.hublink", bridge = true)
    private val app = RuntimeEnvironment.getApplication()

    private fun tool(name: String, readOnly: Boolean, probe: Boolean) = JSONObject().put("name", name)
        .put("annotations", JSONObject().put("readOnlyHint", readOnly))
        .apply { if (probe) put("_meta", JSONObject().put(LocalBridge.PROBE_META, true)) }

    /** A fake app pipe that records methods and tool calls. */
    private class Pipe(val tools: JSONArray) : McpPipe {
        val calls = mutableListOf<String>()
        override suspend fun request(method: String, params: JSONObject): JSONObject {
            calls += if (method == "tools/call") "call:${params.getString("name")}" else method
            return when (method) {
                "initialize" -> JSONObject().put("serverInfo", JSONObject().put("name", "stone").put("version", "1.7.16"))
                "tools/list" -> JSONObject().put("tools", tools)
                else -> JSONObject().put("isError", false)
            }
        }
    }

    @Test fun onlyTheMarkedReadOnlyProbeIsEverRun() = runBlocking {
        val marked = Pipe(JSONArray().put(tool("a_read", true, false)).put(tool("help", true, true)).put(tool("delete_all", false, false)))
        val r = BridgeProbe.run(marked)
        assertEquals(listOf("initialize", "tools/list", "call:help"), marked.calls)
        assertEquals("help", r.probe); assertEquals(true, r.probeOk); assertEquals(3, r.tools); assertEquals("stone 1.7.16", r.server)
        // Marked but not read-only, or not marked at all: nothing is run.
        val unsafe = Pipe(JSONArray().put(tool("wipe", false, true)).put(tool("a_read", true, false)))
        assertNull(BridgeProbe.run(unsafe).probe)
        assertEquals(listOf("initialize", "tools/list"), unsafe.calls)
    }

    @Test fun answersAreParsedAndErrorsNamed() {
        assertEquals(1, LocalBridge.parse("""{"jsonrpc":"2.0","id":1,"result":{"x":1}}""").getInt("x"))
        fun code(reply: String) = (runCatching { LocalBridge.parse(reply) }.exceptionOrNull() as HubException).code
        assertEquals("REDIRECT_OFF", code("""{"id":null,"error":{"code":-32001,"message":"REDIRECT_OFF"}}"""))
        assertEquals("RESPONSE_TOO_LARGE", code("""{"id":1,"error":{"code":-32000,"message":"RESPONSE_TOO_LARGE: no"}}"""))
        assertEquals("BRIDGE_ERROR", code("""{"id":1,"error":{"code":-32601,"message":"Method not found"}}"""))
        assertEquals("BRIDGE_BAD_ANSWER", code("not json"))
        assertEquals("BRIDGE_TOO_LARGE", code("x".repeat(LocalBridge.MAX_CHARS + 1)))
    }

    /** Binds [LocalBridge] to a fake app service whose handler runs [answer]; runs [block] off the main thread. */
    private fun <T> withFakeService(answer: (Message) -> Unit, timeoutMs: Long = 2_000, block: suspend (LocalBridge) -> T): Result<T> {
        val service = Messenger(Handler(Looper.getMainLooper()) { answer(it); true })
        shadowOf(app).setComponentNameAndServiceForBindServiceForIntent(Intent(LocalBridge.ACTION).setPackage(stone.packageName),
            ComponentName(stone.packageName, "Bridge"), service.binder)
        var result: Result<T>? = null
        val worker = Thread { result = runCatching { runBlocking { block(LocalBridge(app, timeoutMs, 2_000)) } } }.apply { start() }
        while (worker.isAlive) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(5) }
        return result!!
    }

    private fun echo(msg: Message) {
        val request = JSONObject(msg.data.getString(LocalBridge.KEY_MESSAGE)!!)
        val reply = JSONObject().put("jsonrpc", "2.0").put("id", request.get("id")).put("result", JSONObject().put("method", request.getString("method")))
        msg.replyTo.send(Message.obtain(null, LocalBridge.MSG_RPC, msg.arg1, 0).apply { data = Bundle().apply { putString(LocalBridge.KEY_MESSAGE, reply.toString()) } })
    }

    @Test fun requestsAndAnswersTravelOverTheMessenger() {
        val methods = withFakeService(::echo) { bridge ->
            bridge.withApp(stone) { pipe -> listOf(pipe.request("initialize", JSONObject()), pipe.request("tools/list", JSONObject())).map { it.getString("method") } }
        }
        assertEquals(listOf("initialize", "tools/list"), methods.getOrThrow())
    }

    @Test fun silenceAndUnsupportedAppsAreNamed() {
        val silent = withFakeService({ }, timeoutMs = 200) { bridge -> bridge.withApp(stone) { it.request("tools/list", JSONObject()) } }
        assertEquals("BRIDGE_TIMEOUT", (silent.exceptionOrNull() as HubException).code)
        val old = withFakeService(::echo) { bridge -> bridge.withApp(stone.copy(bridge = false)) { it.request("ping", JSONObject()) } }
        assertEquals("BRIDGE_UNSUPPORTED", (old.exceptionOrNull() as HubException).code)
        val stranger = withFakeService(::echo) { bridge -> bridge.withApp(stone.copy(sameDeveloper = false)) { it.request("ping", JSONObject()) } }
        assertEquals("BRIDGE_UNSUPPORTED", (stranger.exceptionOrNull() as HubException).code)
    }

    @Test fun theScannerButtonReportsWhatWasChecked() {
        val connector = object : BridgeConnector {
            override suspend fun <T> withApp(app: LocalApp, block: suspend (McpPipe) -> T): T =
                block(Pipe(JSONArray().put(tool("stone_help", true, true)).put(tool("player_state", true, false))))
        }
        val vm = HubViewModel(MemorySessions(), "https://hub.example", { listOf(stone) }, bridge = connector)
        vm.testBridge(stone.packageName)
        repeat(100) { shadowOf(Looper.getMainLooper()).idle(); if (!vm.state.value.loading) return@repeat; Thread.sleep(10) }
        assertEquals(UiText.Res(R.string.bridge_ok, listOf("Stone", "stone 1.7.16", 2, "stone_help")), vm.state.value.notice)
        val none = HubViewModel(MemorySessions(), "https://hub.example", { listOf(stone) })
        none.testBridge(stone.packageName)
        repeat(100) { shadowOf(Looper.getMainLooper()).idle(); if (!none.state.value.loading) return@repeat; Thread.sleep(10) }
        assertEquals(UiText.Res(R.string.bridge_unsupported), none.state.value.error)
    }
}
