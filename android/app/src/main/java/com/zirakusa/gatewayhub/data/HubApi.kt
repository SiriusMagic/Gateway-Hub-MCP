package com.zirakusa.gatewayhub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** A failure the hub explained ([code] from the hub, or NETWORK / UNAUTHORIZED / BAD_ANSWER). */
class HubException(val code: String, message: String) : Exception(message)

/** Sends one request; replaceable in tests. Returns the HTTP status and the body. */
fun interface HubTransport {
    fun send(method: String, url: String, token: String, body: String?): Pair<Int, String>
}

/** Client of the hub's owner API (/api/admin/…). The owner token travels only in the Authorization header. */
class HubApi(baseUrl: String, private val token: String, private val transport: HubTransport = HttpTransport) {
    val base: String = normalize(baseUrl)

    suspend fun state(): HubState = HubJson.state(call("GET", "/state"))
    suspend fun createAgent(name: String, apps: List<String>, readOnly: Boolean, days: Int): CreatedAgent =
        HubJson.created(call("POST", "/agents", JSONObject().put("name", name).put("apps", JSONArray(apps))
            .put("readOnly", readOnly).put("days", days)))
    suspend fun setPaused(id: String, paused: Boolean) { call("POST", "/agents/$id/${if (paused) "pause" else "resume"}", JSONObject()) }
    suspend fun revoke(id: String) { call("DELETE", "/agents/$id") }
    suspend fun linkStone(link: String): String = JSONObject(call("POST", "/stone", JSONObject().put("link", link.trim()))).getString("message")
    /** Other apps with Modo MCP (Luma: gallery_). */
    suspend fun linkApp(prefix: String, link: String): String =
        JSONObject(call("POST", "/apps/$prefix", JSONObject().put("link", link.trim()))).getString("message")
    /** Read-only: the hub asks each remote app whether it is linked and online. */
    suspend fun scan(): List<RemoteStatus> = HubJson.scan(call("GET", "/scan"))

    private suspend fun call(method: String, path: String, body: JSONObject? = null): String = withContext(Dispatchers.IO) {
        val (status, text) = try { transport.send(method, "$base/api/admin$path", token, body?.toString()) }
            catch (e: Exception) { throw HubException("NETWORK", "No se pudo conectar con el hub. Revisa la dirección y tu conexión.") }
        if (status == 401) throw HubException("UNAUTHORIZED", "Token del propietario incorrecto.")
        if (status == 429) throw HubException("RATE_LIMITED", "Demasiados intentos. Espera un minuto.")
        if (status !in 200..299) {
            val error = runCatching { JSONObject(text).getJSONObject("error") }.getOrNull()
            throw HubException(error?.optString("code") ?: "HTTP_$status", error?.optString("message") ?: "El hub respondió $status.")
        }
        text
    }

    companion object {
        /** https://host[:port] only: no other scheme, no path, no user info, no query. */
        fun normalize(raw: String): String {
            val uri = runCatching { URI(raw.trim().let { if ("://" in it) it else "https://$it" }) }.getOrNull()
            if (uri == null || uri.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null)
                throw HubException("INVALID_URL", "La dirección del hub debe empezar por https://")
            return "https://${uri.host}${if (uri.port > 0) ":${uri.port}" else ""}"
        }
    }
}

/** HttpURLConnection, HTTPS only (also enforced by network_security_config.xml). */
object HttpTransport : HubTransport {
    override fun send(method: String, url: String, token: String, body: String?): Pair<Int, String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            return status to (stream?.bufferedReader()?.use { it.readText().take(2_000_000) } ?: "")
        } finally { connection.disconnect() }
    }
}
