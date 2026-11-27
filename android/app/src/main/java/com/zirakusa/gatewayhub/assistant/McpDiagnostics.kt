package com.zirakusa.gatewayhub.assistant

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.zirakusa.gatewayhub.BuildConfig
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.KeystoreSessionStore
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class McpDiagnostics(context: Context, scope: CoroutineScope, private val hands: Hands) {
    private val appContext = context.applicationContext
    private val prefs = McpReportQueue.preferences(appContext)
    private val session = KeystoreSessionStore(appContext)
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == McpReportQueue.PENDING) pending = McpReportQueue.size(prefs)
    }
    var sending by mutableStateOf(prefs.getBoolean(McpReportQueue.CONSENT, false))
        private set
    var pending by mutableStateOf(McpReportQueue.size(prefs))
        private set
    var recent by mutableStateOf<List<McpOrder>>(emptyList())
        private set
    var lastFailure by mutableStateOf(loadFailure())
        private set

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
        scope.coroutineContext[Job]?.invokeOnCompletion { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        McpReportQueue.updateSchedule(appContext, prefs)
    }

    private fun array(key: String) = runCatching { JSONArray(prefs.getString(key, "[]")) }.getOrElse { JSONArray() }

    fun allow(value: Boolean) {
        sending = value
        McpReportQueue.allow(prefs, value)
        pending = McpReportQueue.size(prefs)
        McpReportQueue.updateSchedule(appContext, prefs)
    }

    private fun loadFailure(): McpOrder? = runCatching {
        val saved = JSONObject(prefs.getString("last_failure", "{}"))
        McpOrder(saved.getString("id"), saved.getString("tool"), emptyMap(), saved.getString("stage"),
            saved.getString("code"), saved.getLong("ms"), app = saved.getString("app"))
    }.getOrNull()

    fun record(order: McpOrder) {
        recent = (listOf(order) + recent).take(20)
        if (order.code == "OK") return
        lastFailure = order
        val report = JSONObject()
            .put("id", order.id).put("app", order.app).put("tool", order.tool).put("stage", order.stage)
            .put("code", order.code.takeIf { it in reportCodes } ?: "OTHER")
            .put("version", BuildConfig.VERSION_NAME.substringBefore('-'))
            .put("ms", order.millis.coerceIn(0, 120_000))
        prefs.edit { putString("last_failure", report.toString()) }
        if (!sending) return
        McpReportQueue.add(prefs, report)
        pending = McpReportQueue.size(prefs)
        McpReportQueue.updateSchedule(appContext, prefs)
    }

    private suspend fun api() = withContext(Dispatchers.IO) {
        val (url, token) = session.load() ?: throw HubException("NO_HUB", "")
        HubApi(url, token)
    }

    suspend fun probe(viaHub: Boolean, app: FamilyApp = Catalog.byId("stone"), prefix: String = "music_"): McpOrder {
        val tool = if (prefix == "music_") StoneOrder.PROBE else "ping"
        val id = UUID.randomUUID().toString()
        val started = SystemClock.elapsedRealtime()
        val code = try {
            if (viaHub) {
                api().probeMcp(id, prefix)
                "OK"
            } else withContext(Dispatchers.IO) {
                when {
                    !hands.installed(app) -> "APP_MISSING"
                    tool == "ping" -> if (hands.mcpPing(app, id)) "OK" else "RESULT_UNCONFIRMED"
                    hands.tracedTool(app, tool, JSONObject(), id) == null -> "RESULT_UNCONFIRMED"
                    else -> "OK"
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: HubException) {
            failure.code
        } catch (failure: Hands.ToolError) {
            failure.code
        } catch (_: Hands.ToolOff) {
            "HUB_LOCAL_DISABLED"
        } catch (_: Exception) {
            "RESULT_UNCONFIRMED"
        }
        return McpOrder(id, tool, emptyMap(), if (code == "OK") "ejecucion" else StoneOrder.stage(code),
            code, SystemClock.elapsedRealtime() - started, app = prefix).also(::record)
    }

    suspend fun grouped(): JSONArray = api().mcpFailures()

    fun comment(id: String): String {
        val comments = array("comments")
        return (0 until comments.length()).mapNotNull(comments::optJSONObject)
            .firstOrNull { it.optString("id") == id }?.optString("text").orEmpty()
    }

    fun saveComment(id: String, text: String) {
        val previous = array("comments")
        val items = (0 until previous.length()).mapNotNull(previous::optJSONObject)
            .filterNot { it.optString("id") == id }.takeLast(19)
        prefs.edit { putString("comments", JSONArray(items).put(JSONObject().put("id", id).put("text", text.take(300))).toString()) }
    }

    private companion object {
        val reportCodes = setOf("APP_MISSING", "PROVIDER_UNAVAILABLE", "HUB_LOCAL_DISABLED", "LOCAL_DISABLED", "NO_PERMISSION",
            "NOT_SAME_DEVELOPER", "NOT_READY", "STONE_OFFLINE", "STONE_UNREACHABLE", "STONE_TOKEN", "UNAUTHORIZED",
            "STONE_NOT_LINKED", "APP_NOT_LINKED", "NO_HUB", "NETWORK", "RATE_LIMITED", "STONE_ERROR", "TOOL_UNAVAILABLE", "PLAYER_NOT_READY",
            "VOLUME_REJECTED", "PLAYBACK_FAILED", "PLAYBACK_UNCONFIRMED", "RESULT_UNCONFIRMED", "TRACK_NOT_FOUND",
            "AMBIGUOUS_TITLE", "INVALID_SELECTION", "INVALID_PARAM", "EQ_NOT_ACTIVE", "EQ_BATTERY_MODE")
    }
}
