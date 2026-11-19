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
    /** A body that is not JSON (the Worker upload, multipart). Tests may keep the default. */
    fun upload(method: String, url: String, token: String, contentType: String, body: ByteArray): Pair<Int, String> =
        send(method, url, token, String(body))
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
    /** Connects ("active"), pauses or disconnects the whole MCP environment. */
    suspend fun setEnvironment(state: String) { call("POST", "/environment", JSONObject().put("state", state)) }
    /** What ran since [since] (epoch ms). */
    suspend fun activity(since: Long): Activity = HubJson.activity(call("GET", "/activity?since=$since"))
    /** A credential for this phone's channel (shown once; the hub keeps its hash). Returns (device id, secret). */
    suspend fun createDevice(name: String): Pair<String, String> = JSONObject(call("POST", "/devices", JSONObject().put("name", name.take(40))))
        .let { it.getJSONObject("device").getString("id") to it.getString("secret") }
    suspend fun deleteDevice(id: String) { call("DELETE", "/devices/$id") }
    /** A one-use code (5 min) for another phone to join this hub. Returns (code, expiresAt ms). */
    suspend fun createPairing(): Pair<String, Long> = JSONObject(call("POST", "/pairing", JSONObject())).let { it.getString("code") to it.getLong("expiresAt") }
    /** Shared hub: the 8-digit code to type on the public link's sign-in page (10 min, one use). Returns (code, expiresAt ms). */
    suspend fun connectCode(): Pair<String, Long> = JSONObject(call("POST", "/connect-code", JSONObject())).let { it.getString("code") to it.getLong("expiresAt") }
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
        /** Another phone joins [hubUrl] with a pairing code (no owner token). Returns its (device id, secret). */
        suspend fun pair(hubUrl: String, code: String, name: String, transport: HubTransport = HttpTransport): Pair<String, String> = withContext(Dispatchers.IO) {
            val base = normalize(hubUrl)
            val (status, text) = try { transport.send("POST", "$base/pair", "", JSONObject().put("code", code.trim()).put("name", name.take(40)).toString()) }
                catch (e: Exception) { throw HubException("NETWORK", "") }
            if (status == 401) throw HubException("BAD_PAIRING_CODE", "")
            if (status == 429) throw HubException("RATE_LIMITED", "")
            val o = runCatching { JSONObject(text) }.getOrNull() ?: throw HubException("HTTP_$status", "")
            o.getJSONObject("device").getString("id") to o.getString("secret")
        }

        /**
         * A new Gateway on the shared hub gets its own space: the owner secret (kept encrypted here, never shown) and
         * this phone's channel credential. Nothing to type: no Cloudflare, no token.
         */
        suspend fun register(hubUrl: String, phone: String, transport: HubTransport = HttpTransport): Registered = withContext(Dispatchers.IO) {
            val base = normalize(hubUrl)
            val (status, text) = try { transport.send("POST", "$base/register", "", JSONObject().put("name", phone.take(40)).toString()) }
                catch (e: Exception) { throw HubException("NETWORK", "") }
            if (status == 429) throw HubException("RATE_LIMITED", "")
            val o = runCatching { JSONObject(text) }.getOrNull()?.takeIf { status in 200..299 } ?: throw HubException("HTTP_$status", "")
            Registered(base, o.getString("owner"), o.getJSONObject("device").getString("id"), o.getString("secret"))
        }

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
    override fun send(method: String, url: String, token: String, body: String?): Pair<Int, String> =
        upload(method, url, token, "application/json", body?.toByteArray() ?: ByteArray(0), hasBody = body != null)

    override fun upload(method: String, url: String, token: String, contentType: String, body: ByteArray): Pair<Int, String> =
        upload(method, url, token, contentType, body, hasBody = true)

    private fun upload(method: String, url: String, token: String, contentType: String, body: ByteArray, hasBody: Boolean): Pair<Int, String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (hasBody) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", contentType)
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            return status to (stream?.bufferedReader()?.use { it.readText().take(2_000_000) } ?: "")
        } finally { connection.disconnect() }
    }
}
