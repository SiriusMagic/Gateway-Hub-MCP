package com.zirakusa.gatewayhub.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** MCP requests to one app; [request] returns the JSON-RPC "result" or throws [HubException]. */
fun interface McpPipe { suspend fun request(method: String, params: JSONObject): JSONObject }

/** Opens a pipe to an app on this phone for the length of [block] (replaceable in tests). */
interface BridgeConnector {
    suspend fun <T> withApp(app: LocalApp, block: suspend (McpPipe) -> T): T
    /** No bridge (tests, or an app that offers none). */
    object None : BridgeConnector { override suspend fun <T> withApp(app: LocalApp, block: suspend (McpPipe) -> T): T = throw HubException("BRIDGE_UNSUPPORTED", "") }
}

/**
 * Gateway side of the local bridge (phase 1): MCP JSON-RPC over a Messenger bound to the app's
 * service with action [ACTION]. Android requires our signature permission to bind, and the app checks
 * our uid and signature again. Bounded messages, one id per request, a deadline, and every channel
 * failure turned into a clear code.
 */
class LocalBridge(private val context: Context, private val timeoutMs: Long = 75_000, private val connectMs: Long = 10_000) : BridgeConnector {
    override suspend fun <T> withApp(app: LocalApp, block: suspend (McpPipe) -> T): T {
        if (!app.bridge || !app.sameDeveloper) throw HubException("BRIDGE_UNSUPPORTED", "")
        val thread = HandlerThread("hub-bridge").apply { start() }
        val pending = ConcurrentHashMap<Int, CompletableDeferred<String>>()
        val replies = Messenger(Handler(thread.looper) { msg ->
            val text = runCatching { msg.data?.getString(KEY_MESSAGE) }.getOrNull()
            pending.remove(msg.arg1)?.let { if (text == null) it.completeExceptionally(HubException("BRIDGE_BAD_ANSWER", "")) else it.complete(text) }
            true
        })
        val connected = CompletableDeferred<Messenger>()
        val lost = { code: String -> val e = HubException(code, ""); connected.completeExceptionally(e); pending.values.forEach { it.completeExceptionally(e) } }
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) { service?.let { connected.complete(Messenger(it)) } ?: lost("BRIDGE_DENIED") }
            override fun onServiceDisconnected(name: ComponentName?) = lost("BRIDGE_LOST")
            override fun onBindingDied(name: ComponentName?) = lost("BRIDGE_LOST")
            override fun onNullBinding(name: ComponentName?) = lost("BRIDGE_DENIED")
        }
        val bound = try { context.bindService(Intent(ACTION).setPackage(app.packageName), connection, Context.BIND_AUTO_CREATE) }
            catch (e: SecurityException) { thread.quitSafely(); throw HubException("BRIDGE_DENIED", "") }
        try {
            if (!bound) throw HubException("BRIDGE_UNSUPPORTED", "")
            val target = try { withTimeout(connectMs) { connected.await() } } catch (e: kotlinx.coroutines.TimeoutCancellationException) { throw HubException("BRIDGE_TIMEOUT", "") }
            val ids = AtomicInteger(0)
            return block(McpPipe { method, params ->
                val id = ids.incrementAndGet()
                val text = JSONObject().put("jsonrpc", "2.0").put("id", id).put("method", method).put("params", params).toString()
                if (text.length > MAX_CHARS) throw HubException("BRIDGE_TOO_LARGE", "")
                val answer = CompletableDeferred<String>().also { pending[id] = it }
                val msg = Message.obtain(null, MSG_RPC, id, 0).apply { data = Bundle().apply { putString(KEY_MESSAGE, text) }; replyTo = replies }
                try { target.send(msg) } catch (e: RemoteException) { pending.remove(id); throw HubException("BRIDGE_LOST", "") }
                val reply = try { withTimeout(timeoutMs) { answer.await() } } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    pending.remove(id); throw HubException("BRIDGE_TIMEOUT", "")
                }
                parse(reply)
            })
        } finally {
            runCatching { context.unbindService(connection) }
            thread.quitSafely()
        }
    }

    companion object {
        const val ACTION = "com.zirakusa.gatewayhub.action.MCP"
        const val MSG_RPC = 1
        const val KEY_MESSAGE = "message"
        /** Same bound as the app side: ~400 KB as UTF-16, under Binder's 1 MB shared buffer. */
        const val MAX_CHARS = 200_000
        const val PROBE_META = "com.zirakusa.gatewayhub/probe"

        /** The "result" of a JSON-RPC answer; an "error" becomes a [HubException] with the app's code. */
        fun parse(reply: String): JSONObject {
            if (reply.length > MAX_CHARS) throw HubException("BRIDGE_TOO_LARGE", "")
            val json = runCatching { JSONObject(reply) }.getOrNull() ?: throw HubException("BRIDGE_BAD_ANSWER", "")
            json.optJSONObject("error")?.let { error ->
                val message = error.optString("message")
                val code = Regex("^[A-Z][A-Z_]{2,40}").find(message)?.value ?: "BRIDGE_ERROR"
                throw HubException(code, message)
            }
            return json.optJSONObject("result") ?: throw HubException("BRIDGE_BAD_ANSWER", "")
        }
    }
}

/** What "Probar puente local" found: the server, its tools, and the probe tool's outcome (null = none marked). */
data class ProbeResult(val server: String, val tools: Int, val probe: String?, val probeOk: Boolean?)

/**
 * Checks the bridge: initialize, tools/list and, only if the app marked one tool as its probe AND as read-only,
 * that tool with no arguments. No other tool is ever run.
 */
object BridgeProbe {
    suspend fun run(pipe: McpPipe): ProbeResult {
        val init = pipe.request("initialize", JSONObject().put("protocolVersion", "2025-06-18")
            .put("capabilities", JSONObject()).put("clientInfo", JSONObject().put("name", "gateway-hub").put("version", "local-bridge")))
        val server = init.optJSONObject("serverInfo")?.let { "${it.optString("name")} ${it.optString("version")}".trim() }.orEmpty()
        val tools = pipe.request("tools/list", JSONObject()).optJSONArray("tools") ?: throw HubException("BRIDGE_BAD_ANSWER", "")
        val probe = (0 until tools.length()).map { tools.getJSONObject(it) }.firstOrNull {
            it.optJSONObject("_meta")?.optBoolean(LocalBridge.PROBE_META) == true && it.optJSONObject("annotations")?.optBoolean("readOnlyHint") == true
        }?.optString("name")
        val ok = probe?.let { !pipe.request("tools/call", JSONObject().put("name", it).put("arguments", JSONObject())).optBoolean("isError", false) }
        return ProbeResult(server, tools.length(), probe, ok)
    }
}
