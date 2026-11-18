package com.zirakusa.gatewayhub.channel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * What the phone does with each message from the hub (no networking here, so it is easy to test):
 * - `{t:"req", id, app, message}`: forwards the MCP message to that app over the local bridge and answers
 *   `{t:"res", id, reply}`. The message's JSON-RPC id is replaced by `ch-<id>`, unique on this channel, so the
 *   app can tell concurrent calls apart and cancel the right one.
 * - `{t:"cancel", id}`: sends `notifications/cancelled` for it to the same app.
 */
class ChannelSession(
    private val scope: CoroutineScope,
    private val send: (String) -> Unit,
    private val forward: suspend (app: String, message: String) -> String,
    private val notify: suspend (app: String, message: String) -> Unit,
) {
    private val apps = java.util.concurrent.ConcurrentHashMap<String, String>()   // channel id → app

    fun handle(text: String) {
        if (text.length > MAX_CHARS) return
        val m = runCatching { JSONObject(text) }.getOrNull() ?: return
        val id = m.optString("id").takeIf { it.isNotBlank() && it.length <= 20 } ?: return
        when (m.optString("t")) {
            "req" -> {
                val app = m.optString("app").takeIf { APP.matches(it) } ?: return
                val message = m.optJSONObject("message") ?: return
                message.put("id", "ch-$id")
                apps[id] = app
                scope.launch {
                    val reply = try { JSONObject(forward(app, message.toString())) } catch (e: Exception) { errorReply(id, e) }
                    if (apps.remove(id) != null) send(JSONObject().put("t", "res").put("id", id).put("reply", reply).toString())
                }
            }
            "cancel" -> {
                val app = apps.remove(id) ?: return
                scope.launch {
                    runCatching { notify(app, JSONObject().put("jsonrpc", "2.0").put("method", "notifications/cancelled")
                        .put("params", JSONObject().put("requestId", "ch-$id").put("reason", "The hub timed out or the agent cancelled.")).toString()) }
                }
            }
        }
    }

    private fun errorReply(id: String, e: Exception) = JSONObject().put("jsonrpc", "2.0").put("id", "ch-$id")
        .put("error", JSONObject().put("code", -32000).put("message", (e as? com.zirakusa.gatewayhub.data.HubException)?.code ?: "PROVIDER_ERROR"))

    companion object {
        const val MAX_CHARS = 1 shl 20
        private val APP = Regex("^[a-zA-Z][a-zA-Z0-9_.]{2,100}$")
    }
}
