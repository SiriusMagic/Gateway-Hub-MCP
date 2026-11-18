package com.zirakusa.gatewayhub.data

import org.json.JSONArray
import org.json.JSONObject

/** One app inside the hub ("music_" = Stone…). */
data class HubModule(val prefix: String, val title: String, val summary: String)

/** An AI client with its own link. The hub never sends its token back, only this description. */
data class Agent(
    val id: String, val name: String, val modules: List<String>, val readOnly: Boolean,
    val paused: Boolean, val status: String, val expiresAt: Long, val created: Long, val lastUsed: Long,
) {
    val allApps: Boolean get() = "*" in modules
}

data class CallStat(val name: String, val calls: Int, val errors: Int, val avgMs: Double)
data class Metrics(val calls: Int, val errors: Int, val byAgent: List<CallStat>, val byApp: List<CallStat>, val byTool: List<CallStat>)

data class HubState(
    val version: String, val modules: List<HubModule>, val stoneLinked: Boolean,
    val agents: List<Agent>, val day: Metrics, val week: Metrics,
    /** The whole MCP environment: "active", "paused" or "disconnected" (set by the owner). */
    val environment: String = ENV_ACTIVE,
)

const val ENV_ACTIVE = "active"
const val ENV_PAUSED = "paused"
const val ENV_DISCONNECTED = "disconnected"

/** What ran since a moment (for the activity notifications). */
data class Activity(val environment: String, val metrics: Metrics)

/** A new agent: [url] and [token] are shown once and never stored by this app. */
data class CreatedAgent(val agent: Agent, val url: String, val token: String)

internal object HubJson {
    private fun <T> JSONArray.map(transform: (JSONObject) -> T) = (0 until length()).map { transform(getJSONObject(it)) }
    private fun JSONArray.strings() = (0 until length()).map { getString(it) }

    fun agent(o: JSONObject) = Agent(o.getString("id"), o.getString("name"), o.getJSONArray("modules").strings(),
        o.getBoolean("readOnly"), o.getBoolean("paused"), o.getString("status"), o.getLong("expiresAt"),
        o.getLong("created"), o.getLong("lastUsed"))

    private fun stats(a: JSONArray) = a.map { CallStat(it.getString("name"), it.getInt("calls"), it.getInt("errors"), it.optDouble("avgMs", 0.0)) }
    private fun metrics(o: JSONObject) = Metrics(o.getInt("calls"), o.getInt("errors"),
        stats(o.getJSONArray("byAgent")), stats(o.getJSONArray("byApp")), stats(o.getJSONArray("byTool")))

    fun state(text: String): HubState {
        val o = JSONObject(text)
        val m = o.getJSONObject("metrics")
        return HubState(o.getString("version"), o.getJSONArray("modules").map { HubModule(it.getString("prefix"), it.getString("title"), it.getString("summary")) },
            o.getBoolean("stoneLinked"), o.getJSONArray("agents").map(::agent), metrics(m.getJSONObject("day")), metrics(m.getJSONObject("week")),
            o.optString("environment", ENV_ACTIVE).ifBlank { ENV_ACTIVE })
    }

    fun scan(text: String) = JSONObject(text).getJSONArray("apps").map {
        RemoteStatus(it.getString("prefix"), it.getString("title"), it.optString("androidPackage").takeIf { p -> p.isNotBlank() && p != "null" },
            it.getBoolean("remote"), it.getBoolean("linked"), it.getBoolean("online"), it.getInt("tools"), it.getString("detail"))
    }

    fun activity(text: String) = JSONObject(text).let { Activity(it.optString("environment", ENV_ACTIVE), metrics(it)) }

    fun created(text: String) = JSONObject(text).let { CreatedAgent(agent(it.getJSONObject("agent")), it.getString("url"), it.getString("token")) }
}
