package com.zirakusa.gatewayhub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    suspend fun subdomain(account: String): String = call("GET", "/accounts/$account/workers/subdomain").getJSONObject("result").getString("subdomain")
    suspend fun putSecret(account: String, script: String, name: String, value: String) {
        call("PUT", "/accounts/$account/workers/scripts/$script/secrets", JSONObject().put("name", name).put("text", value).put("type", "secret_text"))
    }

    private suspend fun call(method: String, path: String, body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        val (status, text) = try { transport.send(method, "$BASE$path", token, body?.toString()) }
            catch (e: Exception) { throw HubException("CF_NETWORK", "No se pudo conectar con Cloudflare.") }
        val json = runCatching { JSONObject(text) }.getOrNull()
        if (status == 401 || status == 403) throw HubException("CF_FORBIDDEN", "El token de Cloudflare no tiene permiso para esto (Workers Scripts: Edit).")
        if (status !in 200..299 || json?.optBoolean("success") != true) {
            val message = json?.optJSONArray("errors")?.optJSONObject(0)?.optString("message")
            throw HubException("CF_ERROR", message?.takeIf { it.isNotBlank() } ?: "Cloudflare respondió $status.")
        }
        json
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

/**
 * Gives the owner a new hub_ token without wrangler: the app makes it, stores it as the Worker's
 * HUB_TOKEN secret in the person's own account and returns the access (address + token). The previous
 * owner token stops working; agent links keep working.
 */
object OwnerToken {
    fun new(): String {
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        return "hub_" + android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
    }

    suspend fun issue(cf: CloudflareApi, token: String = new()): Access {
        for (account in cf.accounts()) {
            val script = cf.scripts(account).firstOrNull { it in CloudflareApi.HUB_SCRIPTS } ?: continue
            cf.putSecret(account, script, "HUB_TOKEN", token)
            return Access("https://$script.${cf.subdomain(account)}.workers.dev", token)
        }
        throw HubException("NO_HUB", "No encuentro tu hub en Cloudflare (${CloudflareApi.HUB_SCRIPTS.joinToString()}). Publícalo primero.")
    }
}
