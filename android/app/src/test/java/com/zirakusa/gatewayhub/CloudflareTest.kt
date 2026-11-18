package com.zirakusa.gatewayhub

import android.os.Looper
import com.zirakusa.gatewayhub.data.CloudflareApi
import com.zirakusa.gatewayhub.data.CloudflareShare
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.data.OwnerToken
import com.zirakusa.gatewayhub.data.SavedCloudflare
import com.zirakusa.gatewayhub.ui.CloudflareInfo
import com.zirakusa.gatewayhub.ui.HubViewModel
import com.zirakusa.gatewayhub.ui.UiText
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** The person's Cloudflare token: checked, shared with Stone, and used to make the hub_ owner token. */
@RunWith(RobolectricTestRunner::class)
class CloudflareTest {
    private val ok = { result: String -> 200 to """{"success":true,"errors":[],"result":$result}""" }

    /** A Cloudflare account "acc2" with the hub Worker "stone-hub" on subdomain "ana". */
    private fun cloud(hubSecret: (String) -> Unit = {}, verify: () -> Pair<Int, String> = { ok("""{"id":"t","status":"active","expires_on":"2030-01-08T00:00:00Z"}""") }) =
        FakeTransport { r ->
            val path = r.url.removePrefix(CloudflareApi.BASE)
            when {
                path == "/user/tokens/verify" -> verify()
                path == "/accounts" -> ok("""[{"id":"acc1"},{"id":"acc2"}]""")
                path.endsWith("/tokens/verify") -> ok("""{"id":"t","status":"active"}""")
                path == "/accounts/acc1/workers/scripts" -> ok("""[{"id":"other"}]""")
                path == "/accounts/acc2/workers/scripts" -> ok("""[{"id":"stone-relay"},{"id":"stone-hub"}]""")
                path == "/accounts/acc2/workers/subdomain" -> ok("""{"subdomain":"ana"}""")
                path == "/accounts/acc2/workers/scripts/stone-hub/secrets" && r.method == "PUT" -> { hubSecret(JSONObject(r.body!!).getString("text")); ok("{}") }
                else -> 404 to """{"success":false,"errors":[{"message":"no"}]}"""
            }
        }

    @Test fun verifyReadsTheExpiryAndAccountTokensToo() = runBlocking {
        assertEquals(java.time.Instant.parse("2030-01-08T00:00:00Z").toEpochMilli(), CloudflareApi("cf", cloud()).verify())
        // Account-owned tokens fail /user/tokens/verify and are checked per account (no expiry = 0).
        assertEquals(0L, CloudflareApi("cf", cloud(verify = { 401 to "{}" })).verify())
        val inactive = cloud(verify = { ok("""{"status":"disabled"}""") })
        assertEquals("CF_INACTIVE", runCatching { CloudflareApi("cf", inactive).verify() }.exceptionOrNull().let { (it as HubException).code })
        val sent = cloud().also { runCatching { CloudflareApi("cf_SECRET", it).verify() } }.sent
        assertTrue(sent.all { it.url.startsWith("https://api.cloudflare.com/") && it.token == "cf_SECRET" && !it.url.contains("cf_SECRET") })
    }

    @Test fun theOwnerTokenIsMadeHereAndStoredOnlyAsTheWorkersSecret() = runBlocking {
        var secret = ""
        val access = OwnerToken.issue(CloudflareApi("cf", cloud(hubSecret = { secret = it })))
        assertEquals("https://stone-hub.ana.workers.dev", access.url)
        assertTrue(access.token.startsWith("hub_") && access.token.length >= 40)
        assertEquals(access.token, secret)
        assertNotEquals(OwnerToken.new(), OwnerToken.new())
        val none = FakeTransport { r -> if (r.url.endsWith("/accounts")) ok("""[{"id":"x"}]""") else ok("[]") }
        assertEquals("NO_HUB", (runCatching { OwnerToken.issue(CloudflareApi("cf", none)) }.exceptionOrNull() as HubException).code)
    }

    @Test fun createAccessLogsInWithTheNewTokenAndSharesTheCloudflareTokenWithStone() {
        var secret = "old"
        val hub = FakeTransport { r -> if (r.token == secret) 200 to STATE_JSON else 401 to "{}" }
        val sessions = MemorySessions()
        val pushed = mutableListOf<SavedCloudflare>()
        val stone = LocalApp("com.zirakusa.stone.kotlin.preview", "Stone", "1.7.15", "music_", true, "com.zirakusa.stone.kotlin.preview.hublink")
        val share = object : CloudflareShare {
            override fun pull(app: LocalApp) = null
            override fun push(app: LocalApp, value: SavedCloudflare) = pushed.add(value)
        }
        val cf = cloud(hubSecret = { secret = it })
        val vm = HubViewModel(sessions, "https://hub.example", { listOf(stone) }, newApi = { url, token -> HubApi(url, token, hub) },
            share = share, newCloudflare = { CloudflareApi(it, cf) })
        vm.createAccess("  cf_TOKEN  ")
        repeat(300) { shadowOf(Looper.getMainLooper()).idle(); if (!vm.state.value.loading && vm.state.value.connected) return@repeat; Thread.sleep(10) }
        assertTrue(vm.state.value.connected)
        assertEquals("https://stone-hub.ana.workers.dev" to secret, sessions.saved)
        assertEquals("cf_TOKEN", sessions.cf!!.token)
        assertEquals(listOf("cf_TOKEN"), pushed.map { it.token })
        assertEquals(UiText.Res(R.string.cf_owner_done), vm.state.value.notice)
        assertFalse(vm.state.value.toString().contains("cf_TOKEN"))   // the screens never get the token
    }

    @Test fun aNewerTokenFromStoneIsTaken() {
        val sessions = MemorySessions().apply { cf = SavedCloudflare("mine", 0, 100) }
        val stone = LocalApp("s", "Stone", "1", "music_", true, "s.hublink")
        val share = object : CloudflareShare {
            override fun pull(app: LocalApp) = SavedCloudflare("theirs", 5, 200)
            override fun push(app: LocalApp, value: SavedCloudflare) = false
        }
        val vm = HubViewModel(sessions, "https://hub.example", { listOf(stone) }, share = share)
        repeat(50) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
        assertEquals("theirs", sessions.cf!!.token)
        assertEquals(CloudflareInfo(5, 200), vm.state.value.cloudflare)
        assertFalse(SavedCloudflare("secret", 0, 0).toString().contains("secret"))
    }

    @Test fun daysLeft() {
        assertNull(CloudflareInfo(0, 0).daysLeft())
        assertEquals(6L, CloudflareInfo(7 * 86_400_000L - 1, 0).daysLeft(now = 0))
        assertEquals(-1L, CloudflareInfo(1, 0).daysLeft(now = 2))
    }
}
