package com.zirakusa.gatewayhub

import android.os.Looper
import com.zirakusa.gatewayhub.data.CloudflareApi
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubDeployer
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.WorkerBundle
import com.zirakusa.gatewayhub.ui.HubViewModel
import com.zirakusa.gatewayhub.ui.UiText
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** Phase 4: the person's own hub, deployed or updated with a Cloudflare token used once and never stored. */
@RunWith(RobolectricTestRunner::class)
class CloudflareTest {
    private val ok = { result: String -> 200 to """{"success":true,"errors":[],"result":$result}""" }
    private val bundle = WorkerBundle("export default {}".toByteArray(), "0.0.4", "2025-09-01")

    /** One Cloudflare account; [scripts] are the Workers it already has. Records every write. */
    private class Cloud(var scripts: List<String>, val subdomain: String? = "ana") {
        val writes = mutableListOf<Pair<String, String>>()   // "METHOD path" → body
        var secret = ""
    }
    private fun transport(c: Cloud, verify: () -> Pair<Int, String> = { ok("""{"status":"active"}""") }) = FakeTransport { r ->
        val path = r.url.removePrefix(CloudflareApi.BASE)
        if (r.method != "GET") c.writes += "${r.method} $path" to (r.body ?: "")
        when {
            path == "/user/tokens/verify" -> verify()
            path == "/accounts" -> ok("""[{"id":"acc1"}]""")
            path == "/accounts/acc1/workers/scripts" -> ok(c.scripts.joinToString(",", "[", "]") { """{"id":"$it"}""" })
            path == "/accounts/acc1/workers/subdomain" -> c.subdomain?.let { ok("""{"subdomain":"$it"}""") } ?: (404 to """{"success":false}""")
            path.endsWith("/secrets") -> { c.secret = JSONObject(r.body!!).getString("text"); ok("{}") }
            else -> ok("{}")
        }
    }

    @Test fun aNewAccountGetsTheHubPublishedWithItsOwnerToken() = runBlocking {
        val c = Cloud(emptyList())
        val r = HubDeployer.run(CloudflareApi("cf", transport(c)), bundle, newOwner = true)
        assertTrue(r.created); assertEquals("https://gateway-hub.ana.workers.dev", r.url)
        val upload = c.writes.first { it.first == "PUT /accounts/acc1/workers/scripts/gateway-hub" }.second
        assertTrue(upload.contains("\"new_sqlite_classes\":[\"HubStore\"]") && upload.contains("export default {}"))
        assertTrue(c.writes.any { it.first == "POST /accounts/acc1/workers/scripts/gateway-hub/subdomain" })
        assertEquals(r.access!!.token, c.secret); assertTrue(c.secret.startsWith("hub_"))
    }

    @Test fun anExistingHubIsUpdatedInPlaceKeepingItsSecretsAndData() = runBlocking {
        val c = Cloud(listOf("stone-relay", "stone-hub"))
        val r = HubDeployer.run(CloudflareApi("cf", transport(c)), bundle, newOwner = false)
        assertFalse(r.created); assertNull(r.access); assertEquals("https://stone-hub.ana.workers.dev", r.url)
        val upload = c.writes.single().second
        assertTrue(upload.contains("\"keep_bindings\":[\"plain_text\",\"secret_text\"]"))
        assertFalse(upload.contains("migrations"))                          // the stored data stays where it is
        assertEquals("", c.secret)                                          // the owner token is not touched
    }

    @Test fun rotatingTheOwnerUploadsNothing() = runBlocking {
        val c = Cloud(listOf("stone-hub"))
        val r = HubDeployer.run(CloudflareApi("cf", transport(c)), null, newOwner = true)
        assertEquals(listOf("PUT /accounts/acc1/workers/scripts/stone-hub/secrets"), c.writes.map { it.first })
        assertEquals(r.access!!.token, c.secret)
    }

    @Test fun noSubdomainOrNoHubIsExplained() = runBlocking {
        fun code(block: suspend () -> Unit) = (runCatching { runBlocking { block() } }.exceptionOrNull() as HubException).code
        assertEquals("NO_SUBDOMAIN", code { HubDeployer.run(CloudflareApi("cf", transport(Cloud(emptyList(), subdomain = null))), bundle, true) })
        assertEquals("NO_HUB", code { HubDeployer.run(CloudflareApi("cf", transport(Cloud(emptyList()))), null, true) })
        assertEquals("CF_INACTIVE", code { CloudflareApi("cf", transport(Cloud(emptyList())) { ok("""{"status":"disabled"}""") }).verify() })
    }

    @Test fun createAccessLogsInAndTheCloudflareTokenIsNeverKept() {
        val c = Cloud(listOf("stone-hub"))
        val cf = transport(c)
        val hub = FakeTransport { r -> if (r.token == c.secret) 200 to STATE_JSON else 401 to "{}" }
        val sessions = MemorySessions()
        val vm = HubViewModel(sessions, "https://hub.example", newApi = { url, token -> HubApi(url, token, hub) },
            newCloudflare = { CloudflareApi(it, cf) }, workerBundle = { bundle })
        vm.createAccess("  cf_TOKEN_123  ")
        repeat(400) { shadowOf(Looper.getMainLooper()).idle(); if (!vm.state.value.loading && vm.state.value.connected) return@repeat; Thread.sleep(10) }
        assertTrue(vm.state.value.connected)
        assertEquals("https://stone-hub.ana.workers.dev" to c.secret, sessions.saved)
        assertEquals(UiText.Res(R.string.cf_owner_done), vm.state.value.notice)
        assertTrue(cf.sent.all { it.token == "cf_TOKEN_123" })
        // Nowhere on the phone: not in the session, not in any preference file.
        val prefsDir = java.io.File(RuntimeEnvironment.getApplication().applicationInfo.dataDir, "shared_prefs")
        assertFalse(prefsDir.walk().filter { it.isFile }.any { it.readText().contains("cf_TOKEN_123") })
        assertFalse(vm.state.value.toString().contains("cf_TOKEN_123"))
    }
}
