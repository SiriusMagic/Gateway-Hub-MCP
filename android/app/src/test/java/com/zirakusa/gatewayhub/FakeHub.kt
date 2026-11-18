package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.data.HubTransport
import com.zirakusa.gatewayhub.data.SessionStore

/** The JSON the real hub answers (shape copied from gateway-hub-mcp/src/admin/service.ts). */
const val STATE_JSON = """{"version":"0.0.1","modules":[
 {"prefix":"music_","title":"Música (Stone)","summary":"Stone on the phone"},
 {"prefix":"notes_","title":"Notas","summary":"A private notepad"},
 {"prefix":"calc_","title":"Calculadora","summary":"Exact arithmetic"},
 {"prefix":"utils_","title":"Utilidades","summary":"Time and random numbers"}],
 "stoneLinked":true,
 "agents":[
  {"id":"a1b2c3d4e5f6","name":"Claude","modules":["*"],"readOnly":false,"paused":false,"status":"active","expiresAt":0,"created":1763400000000,"lastUsed":1763420000000},
  {"id":"0f0f0f0f0f0f","name":"ChatGPT","modules":["music_","calc_"],"readOnly":true,"paused":true,"status":"paused","expiresAt":1771200000000,"created":1763400000000,"lastUsed":0}],
 "metrics":{
  "day":{"since":0,"calls":42,"errors":2,"byAgent":[],"byApp":[],"byTool":[]},
  "week":{"since":0,"calls":180,"errors":5,
   "byAgent":[{"name":"a1b2c3d4e5f6","calls":120,"errors":1,"avgMs":210},{"name":"owner","calls":40,"errors":0,"avgMs":95},{"name":"deadbeef0000","calls":20,"errors":4,"avgMs":880}],
   "byApp":[{"name":"music_","calls":130,"errors":4,"avgMs":400},{"name":"calc_","calls":50,"errors":1,"avgMs":3}],
   "byTool":[{"name":"music_player_state","calls":90,"errors":0,"avgMs":300},{"name":"calc_evaluate","calls":50,"errors":1,"avgMs":3},{"name":"music_library_search","calls":40,"errors":4,"avgMs":620}]}}}"""

const val SCAN_JSON = """{"scannedAt":1,"apps":[
 {"prefix":"music_","title":"Música (Stone)","androidPackage":"com.zirakusa.stone.kotlin.preview","remote":true,"linked":true,"online":false,"tools":0,"detail":"Stone is not open on the phone, or API mode is off."},
 {"prefix":"notes_","title":"Notas","androidPackage":null,"remote":false,"linked":true,"online":true,"tools":5,"detail":"Integrada en el hub."},
 {"prefix":"calc_","title":"Calculadora","androidPackage":null,"remote":false,"linked":true,"online":true,"tools":2,"detail":"Integrada en el hub."}]}"""

class Recorded(val method: String, val url: String, val token: String, val body: String?)

/** Answers like the hub and records what the app sent. */
class FakeTransport(var reply: (Recorded) -> Pair<Int, String> = { 200 to STATE_JSON }) : HubTransport {
    val sent = mutableListOf<Recorded>()
    override fun send(method: String, url: String, token: String, body: String?): Pair<Int, String> =
        Recorded(method, url, token, body).also { sent += it }.let(reply)
}

class MemorySessions(var saved: Pair<String, String>? = null) : SessionStore {
    val flags = mutableMapOf<String, Boolean>()
    override fun flag(name: String, default: Boolean) = flags[name] ?: default
    override fun setFlag(name: String, value: Boolean) { flags[name] = value }
    override fun load() = saved
    override fun save(hubUrl: String, token: String) { saved = hubUrl to token }
    override fun clear() { saved = null }
}
