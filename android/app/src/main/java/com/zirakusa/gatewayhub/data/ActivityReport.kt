package com.zirakusa.gatewayhub.data

import org.json.JSONArray
import org.json.JSONObject

/** The file "Export activity report" saves (through the system file picker): no token, no link. */
object ActivityReport {
    fun json(hubUrl: String, hub: HubState, now: Long = System.currentTimeMillis()): String {
        fun metrics(m: Metrics) = JSONObject().put("calls", m.calls).put("errors", m.errors)
            .put("byApp", JSONArray(m.byApp.map { JSONObject().put("name", it.name).put("calls", it.calls).put("errors", it.errors) }))
            .put("byTool", JSONArray(m.byTool.map { JSONObject().put("name", it.name).put("calls", it.calls).put("errors", it.errors).put("avgMs", it.avgMs.toInt()) }))
        return JSONObject().put("exportedAt", now).put("hub", hubUrl).put("version", hub.version).put("environment", hub.environment)
            .put("apps", JSONArray(hub.modules.map { it.prefix }))
            .put("agents", JSONArray(hub.agents.map {
                JSONObject().put("name", it.name).put("apps", JSONArray(it.modules)).put("readOnly", it.readOnly)
                    .put("status", it.status).put("expiresAt", it.expiresAt).put("lastUsed", it.lastUsed)
            }))
            .put("last24h", metrics(hub.day)).put("last7d", metrics(hub.week)).toString(2)
    }
}
