package com.zirakusa.gatewayhub

import android.os.Looper
import com.zirakusa.gatewayhub.data.BridgeConnector
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.data.McpPipe
import com.zirakusa.gatewayhub.data.MemoryLedger
import com.zirakusa.gatewayhub.data.PackageIssuer
import com.zirakusa.gatewayhub.data.SoftwareSigner
import com.zirakusa.gatewayhub.ui.HubViewModel
import com.zirakusa.gatewayhub.ui.ProviderFlow
import com.zirakusa.gatewayhub.ui.UiText
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Phase 2: linking a provider and issuing execution packages it can verify (Stone's rules, played by a fake). */
@RunWith(RobolectricTestRunner::class)
class ProviderFlowTest {
    private val stone = LocalApp("com.zirakusa.stone.kotlin.preview", "Stone", "1.7.17", "music_", true, "x.hublink", bridge = true)

    /** A provider that keeps the link and checks every package like Stone does (signature, env, audience). */
    private class FakeStone : McpPipe {
        var env = ""; var key: java.security.PublicKey? = null; val instance = "inst_abc123"
        val packages = mutableMapOf<String, String>()   // id → status
        val calls = mutableListOf<String>(); var lastMeta: JSONObject? = null
        override suspend fun request(method: String, params: JSONObject): JSONObject {
            calls += method; params.optJSONObject("_meta")?.let { lastMeta = it }
            fun list() = JSONObject().put("packages", JSONArray(packages.map { (id, s) -> JSONObject().put("id", id).put("status", s) }))
            return when (method) {
                "gatewayhub/link" -> {
                    env = params.getString("env")
                    key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(params.getString("publicKey"))))
                    list().put("instance", instance)
                }
                "gatewayhub/packages/offer" -> {
                    val e = params.getJSONArray("packages").getJSONObject(0)
                    val bytes = Base64.getUrlDecoder().decode(e.getString("pkg"))
                    val ok = Signature.getInstance("SHA256withECDSA").run { initVerify(key); update(bytes); verify(Base64.getUrlDecoder().decode(e.getString("sig"))) }
                    val p = JSONObject(String(bytes))
                    val answer = when {
                        !ok -> JSONObject().put("error", "BAD_SIGNATURE")
                        p.getString("env") != env -> JSONObject().put("error", "WRONG_ENVIRONMENT")
                        p.getString("aud") != "com.zirakusa.stone.kotlin.preview#$instance" -> JSONObject().put("error", "WRONG_DEVICE")
                        else -> { val st = if (p.getJSONArray("acts").toString() == """["leer"]""") "active" else "pending"
                            packages[p.getString("id")] = st; JSONObject().put("id", p.getString("id")).put("status", st) }
                    }
                    JSONObject().put("packages", JSONArray().put(answer))
                }
                "gatewayhub/packages/revoke" -> { packages.remove(params.getString("id")); list() }
                "gatewayhub/packages/list" -> list()
                "initialize" -> JSONObject().put("serverInfo", JSONObject().put("name", "stone").put("version", "1.7.17"))
                "tools/list" -> JSONObject().put("tools", JSONArray())
                else -> JSONObject()
            }
        }
    }

    private fun flow(fake: FakeStone, ledger: MemoryLedger = MemoryLedger(), signer: SoftwareSigner = SoftwareSigner()) =
        ProviderFlow(object : BridgeConnector {
            override suspend fun <T> withApp(app: LocalApp, block: suspend (McpPipe) -> T): T = block(fake)
        }, { signer }, ledger, { listOf(stone) })

    @Test fun linkingGivesAReadingPackageThatStoneAccepts() = runBlocking {
        val fake = FakeStone(); val ledger = MemoryLedger(); val f = flow(fake, ledger)
        assertEquals(UiText.Res(R.string.provider_linked, listOf("Stone")), f.link(stone.packageName, "active", "safe"))
        assertEquals("inst_abc123", ledger.instance(stone.packageName))
        val read = f.packages().single()
        assertEquals(listOf("leer"), read.acts); assertEquals("*", read.agent); assertEquals("active", read.status)
        assertTrue(fake.calls.containsAll(listOf("gatewayhub/link", "gatewayhub/state", "gatewayhub/packages/offer")))
        f.link(stone.packageName, "active", "safe")                       // linking again does not pile up reading packages
        assertEquals(1, f.packages().size)
    }

    @Test fun changesWaitForTheAppAndRevokingDropsThem() = runBlocking {
        val fake = FakeStone(); val f = flow(fake)
        assertEquals("NOT_LINKED", (runCatching { f.offer(stone.packageName, "a1", "x", listOf("modificar"), 7) }.exceptionOrNull() as HubException).code)
        f.link(stone.packageName, "active", "safe")
        assertEquals(UiText.Res(R.string.package_pending, listOf("Stone")), f.offer(stone.packageName, "a1", "Etiquetas", listOf("revisar", "modificar"), 7))
        val pending = f.packages().first { it.label == "Etiquetas" }
        assertEquals("pending", pending.status)
        fake.packages[pending.id] = "active"                               // the user approved it in Stone
        f.refresh(stone.packageName)
        assertEquals("active", f.packages().first { it.id == pending.id }.status)
        f.revoke(stone.packageName, pending.id)
        assertTrue(f.packages().none { it.id == pending.id })
    }

    @Test fun aPackageFromAnotherEnvironmentIsRefusedByTheApp() = runBlocking {
        val fake = FakeStone(); val f = flow(fake)
        f.link(stone.packageName, "active", "safe")
        val stranger = flow(fake, MemoryLedger().apply { saveInstance(stone.packageName, "inst_abc123") }, SoftwareSigner())
        val e = runCatching { stranger.offer(stone.packageName, "a1", "x", listOf("leer"), 7) }.exceptionOrNull() as HubException
        assertEquals("PACKAGE_BAD_SIGNATURE", e.code)
    }

    @Test fun theTestUsesTheReadingPackage() = runBlocking {
        val fake = FakeStone(); val f = flow(fake)
        f.link(stone.packageName, "active", "safe")
        f.test(stone.packageName)
        val grant = fake.lastMeta!!.getJSONObject(ProviderFlow.GRANT)
        assertEquals("*", grant.getString("agent")); assertEquals(f.packages().single().id, grant.getString("package"))
    }

    @Test fun issuedPackagesAreBoundedAndSigned() {
        val signer = SoftwareSigner()
        val (envelope, issued) = PackageIssuer.issue(signer, "app", "inst_1", "a1", "x".repeat(200), listOf("leer", "borrar"), days = 9999, now = 1000)
        val bytes = Base64.getUrlDecoder().decode(envelope.getString("pkg"))
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(signer.publicKey())))
        assertTrue(Signature.getInstance("SHA256withECDSA").run { initVerify(key); update(bytes); verify(Base64.getUrlDecoder().decode(envelope.getString("sig"))) })
        val p = JSONObject(String(bytes))
        assertEquals("app#inst_1", p.getString("aud")); assertEquals(signer.envId, p.getString("env"))
        assertEquals(listOf("leer"), issued.acts)                              // unknown acts never leave Gateway
        assertEquals(1000 + 365 * 86_400L, issued.exp); assertEquals(80, issued.label.length)
    }

    @Test fun theViewModelShowsPackages() {
        val fake = FakeStone()
        val vm = HubViewModel(MemorySessions(), "https://hub.example", { listOf(stone) }, bridge = object : BridgeConnector {
            override suspend fun <T> withApp(app: LocalApp, block: suspend (McpPipe) -> T): T = block(fake)
        })
        vm.linkProvider(stone.packageName)
        repeat(100) { shadowOf(Looper.getMainLooper()).idle(); if (!vm.state.value.loading) return@repeat; Thread.sleep(10) }
        assertEquals(1, vm.state.value.packages.size)
        assertEquals(UiText.Res(R.string.provider_linked, listOf("Stone")), vm.state.value.notice)
    }
}
