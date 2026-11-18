package com.zirakusa.gatewayhub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.time.OffsetDateTime

/**
 * The person's own Cloudflare account, through its API token (Workers Scripts: Edit). Only
 * api.cloudflare.com is ever called, over HTTPS; the token travels only in the Authorization header.
 */
class CloudflareApi(private val token: String, private val transport: HubTransport = HttpTransport) {
    /** Whether the token works and when it expires (0 = no end date). Account-owned tokens are checked per account. */
    suspend fun verify(): Long {
        val user = runCatching { call("GET", "/user/tokens/verify") }.getOrNull()
            ?: accounts().firstNotNullOfOrNull { runCatching { call("GET", "/accounts/$it/tokens/verify") }.getOrNull() }
            ?: throw HubException("CF_INVALID", "Cloudflare no acepta ese token.")
        val result = user.getJSONObject("result")
        if (result.optString("status") != "active") throw HubException("CF_INACTIVE", "Ese token de Cloudflare no está activo.")
        return result.optString("expires_on").takeIf { it.isNotBlank() && it != "null" }
            ?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() } ?: 0
    }

    suspend fun accounts(): List<String> = call("GET", "/accounts").getJSONArray("result").let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("id") } }
    suspend fun scripts(account: String): List<String> =
        call("GET", "/accounts/$account/workers/scripts").getJSONArray("result").let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("id") } }
    suspend fun subdomain(account: String): String = runCatching {
        call("GET", "/accounts/$account/workers/subdomain").getJSONObject("result").getString("subdomain")
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: throw HubException("NO_SUBDOMAIN", "")

    /** Uploads the Worker (multipart: metadata + module). [metadata] says bindings, migrations or what to keep. */
    suspend fun uploadScript(account: String, script: String, metadata: JSONObject, module: ByteArray) = withContext(Dispatchers.IO) {
        val boundary = "gatewayhub" + System.nanoTime()
        val body = java.io.ByteArrayOutputStream().apply {
            fun part(headers: String, bytes: ByteArray) { write("--$boundary\r\n$headers\r\n\r\n".toByteArray()); write(bytes); write("\r\n".toByteArray()) }
            part("Content-Disposition: form-data; name=\"metadata\"\r\nContent-Type: application/json", metadata.toString().toByteArray())
            part("Content-Disposition: form-data; name=\"index.js\"; filename=\"index.js\"\r\nContent-Type: application/javascript+module", module)
            write("--$boundary--\r\n".toByteArray())
        }.toByteArray()
        val (status, text) = try { transport.upload("PUT", "$BASE/accounts/$account/workers/scripts/$script", token, "multipart/form-data; boundary=$boundary", body) }
            catch (e: Exception) { throw HubException("CF_NETWORK", "") }
        check(status, text)
    }

    /** Serves the Worker at <script>.<subdomain>.workers.dev. */
    suspend fun enableWorkersDev(account: String, script: String) {
        call("POST", "/accounts/$account/workers/scripts/$script/subdomain", JSONObject().put("enabled", true))
    }
    suspend fun putSecret(account: String, script: String, name: String, value: String) {
        call("PUT", "/accounts/$account/workers/scripts/$script/secrets", JSONObject().put("name", name).put("text", value).put("type", "secret_text"))
    }

    private suspend fun call(method: String, path: String, body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        val (status, text) = try { transport.send(method, "$BASE$path", token, body?.toString()) }
            catch (e: Exception) { throw HubException("CF_NETWORK", "No se pudo conectar con Cloudflare.") }
        check(status, text)
    }

    private fun check(status: Int, text: String): JSONObject {
        val json = runCatching { JSONObject(text) }.getOrNull()
        if (status == 401 || status == 403) throw HubException("CF_FORBIDDEN", "El token de Cloudflare no tiene permiso para esto (Workers Scripts: Edit).")
        if (status !in 200..299 || json?.optBoolean("success") != true) {
            val message = json?.optJSONArray("errors")?.optJSONObject(0)?.optString("message")
            throw HubException("CF_ERROR", message?.takeIf { it.isNotBlank() } ?: "Cloudflare respondió $status.")
        }
        return json
    }

    companion object {
        const val BASE = "https://api.cloudflare.com/client/v4"
        /** Opens Cloudflare's "Create API token" with the one permission the apps need already chosen. */
        const val CREATE_URL = "https://dash.cloudflare.com/profile/api-tokens?permissionGroupKeys=" +
            "%5B%7B%22key%22%3A%22workers_scripts%22%2C%22type%22%3A%22edit%22%7D%5D&name=Gateway%20Hub%20y%20Stone"
        /** Worker names the hub may have in the person's account (the first one found is used). */
        val HUB_SCRIPTS = listOf("stone-hub", "gateway-hub-mcp", "gateway-hub")
    }
}

/** The Worker the APK carries (assets/hub-worker.js + hub-worker.json). */
class WorkerBundle(val module: ByteArray, val version: String, val compatibilityDate: String)

/**
 * The person's own hub, with a Cloudflare token used for this one task and then forgotten (never stored):
 * deploy it if the account has none, update it to the APK's version, and/or give the owner a new hub_ token
 * (made on the phone and stored only as the Worker's HUB_TOKEN secret).
 */
object HubDeployer {
    const val NEW_SCRIPT = "gateway-hub"

    class Result(val access: Access?, val url: String, val created: Boolean)

    suspend fun run(cf: CloudflareApi, bundle: WorkerBundle?, newOwner: Boolean, token: String = OwnerToken.new()): Result {
        val accounts = cf.accounts()
        val found = accounts.firstNotNullOfOrNull { a -> cf.scripts(a).firstOrNull { it in CloudflareApi.HUB_SCRIPTS }?.let { a to it } }
        if (found == null && bundle == null) throw HubException("NO_HUB", "")
        val (account, script) = found ?: ((accounts.firstOrNull() ?: throw HubException("NO_HUB", "")) to NEW_SCRIPT)
        val created = found == null
        val url = "https://$script.${cf.subdomain(account)}.workers.dev"
        if (bundle != null) {
            val hub = JSONObject().put("type", "durable_object_namespace").put("name", "HUB").put("class_name", "HubStore")
            val metadata = JSONObject().put("main_module", "index.js").put("compatibility_date", bundle.compatibilityDate)
            if (created) metadata.put("bindings", JSONArray().put(hub).put(JSONObject().put("type", "plain_text").put("name", "STONE_RELAY_HOST").put("text", "")))
                .put("migrations", JSONObject().put("new_tag", "v1").put("new_sqlite_classes", JSONArray().put("HubStore")))
            // An update keeps the existing variables and secrets (HUB_TOKEN, STONE_RELAY_HOST) and the stored data.
            else metadata.put("bindings", JSONArray().put(hub)).put("keep_bindings", JSONArray().put("plain_text").put("secret_text"))
            cf.uploadScript(account, script, metadata, bundle.module)
            if (created) cf.enableWorkersDev(account, script)
        }
        val access = if (newOwner || created) { cf.putSecret(account, script, "HUB_TOKEN", token); Access(url, token) } else null
        return Result(access, url, created)
    }
}

/** A new owner token, made on the phone. */
object OwnerToken {
    fun new(): String {
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        return "hub_" + android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
    }
}
