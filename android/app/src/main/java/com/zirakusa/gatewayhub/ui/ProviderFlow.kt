package com.zirakusa.gatewayhub.ui

import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.BridgeConnector
import com.zirakusa.gatewayhub.data.BridgeProbe
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.IssuedPackage
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.data.LocalAppSource
import com.zirakusa.gatewayhub.data.McpPipe
import com.zirakusa.gatewayhub.data.PackageIssuer
import com.zirakusa.gatewayhub.data.PackageLedger
import com.zirakusa.gatewayhub.data.PackageSigner
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gateway's side of the provider contract (phase 2): link an app of this phone (environment public key in,
 * instance id back), give it a reading package at once, offer, revoke and sync execution packages, and test.
 */
class ProviderFlow(
    private val bridge: BridgeConnector,
    private val signer: () -> PackageSigner,
    private val ledger: PackageLedger,
    private val localApps: LocalAppSource,
) {
    fun packages(): List<IssuedPackage> = ledger.packages()

    /** Links [packageName]; tells it the environment state; makes sure it has an active reading package. */
    suspend fun link(packageName: String, environment: String, mode: String): UiText = withApp(packageName) { app, pipe ->
        val key = signer()
        val linked = pipe.request("gatewayhub/link", JSONObject().put("env", key.envId).put("publicKey", key.publicKey()).put("contract", 1))
        val instance = linked.optString("instance").takeIf { it.startsWith("inst_") } ?: throw HubException("BRIDGE_BAD_ANSWER", "")
        ledger.saveInstance(app.packageName, instance)
        pipe.request("gatewayhub/state", JSONObject().put("environment", environment).put("mode", mode))
        sync(app.packageName, linked.optJSONArray("packages"))
        if (ledger.packages().none { it.app == app.packageName && it.agent == "*" && it.acts == listOf("leer") && it.status == "active" })
            offerWith(pipe, app, instance, "*", "Lectura", listOf("leer"), 30)
        UiText.Res(R.string.provider_linked, listOf(app.label))
    }

    suspend fun offer(packageName: String, agent: String, label: String, acts: List<String>, days: Int): UiText = withApp(packageName) { app, pipe ->
        val instance = ledger.instance(app.packageName) ?: throw HubException("NOT_LINKED", "")
        val status = offerWith(pipe, app, instance, agent, label, acts, days)
        UiText.Res(if (status == "active") R.string.package_active else R.string.package_pending, listOf(app.label))
    }

    suspend fun revoke(packageName: String, id: String): UiText = withApp(packageName) { app, pipe ->
        sync(app.packageName, pipe.request("gatewayhub/packages/revoke", JSONObject().put("id", id)).optJSONArray("packages"))
        UiText.Res(R.string.package_revoked)
    }

    /** Asks the app for its package states (the user approves in the app). */
    suspend fun refresh(packageName: String) = withApp(packageName) { app, pipe ->
        sync(app.packageName, pipe.request("gatewayhub/packages/list", JSONObject()).optJSONArray("packages")); null
    }

    /** Tells every linked app the environment state and mode (safe mode: the app serves only while on screen). */
    suspend fun pushState(environment: String, mode: String) {
        for (app in localApps.installed().filter { ledger.instance(it.packageName) != null })
            runCatching { bridge.withApp(app) { it.request("gatewayhub/state", JSONObject().put("environment", environment).put("mode", mode)) } }
    }

    /** initialize + tools/list + the marked probe, with the reading package when there is one. */
    suspend fun test(packageName: String): UiText = withApp(packageName) { app, pipe ->
        val readPackage = ledger.packages().firstOrNull { it.app == app.packageName && it.agent == "*" && it.status == "active" }
        val grant = readPackage?.let { JSONObject().put(GRANT, JSONObject().put("agent", "*").put("package", it.id)) }
        val r = BridgeProbe.run(pipe, grant)
        UiText.Res(when (r.probeOk) { true -> R.string.bridge_ok; false -> R.string.bridge_probe_failed; null -> R.string.bridge_no_probe },
            listOf(app.label, r.server, r.tools, r.probe.orEmpty()))
    }

    private suspend fun offerWith(pipe: McpPipe, app: LocalApp, instance: String, agent: String, label: String, acts: List<String>, days: Int): String {
        val (envelope, issued) = PackageIssuer.issue(signer(), app.packageName, instance, agent, label, acts, days = days)
        val answer = pipe.request("gatewayhub/packages/offer", JSONObject().put("packages", JSONArray().put(envelope)))
            .optJSONArray("packages")?.optJSONObject(0) ?: throw HubException("BRIDGE_BAD_ANSWER", "")
        answer.optString("error").takeIf { it.isNotBlank() }?.let { throw HubException("PACKAGE_$it", "") }
        val status = answer.optString("status", "pending")
        ledger.savePackages(ledger.packages().filter { it.id != issued.id } + issued.copy(status = status))
        return status
    }

    /** The app's list is the truth: statuses are updated and packages it no longer has are dropped. */
    private fun sync(app: String, statuses: JSONArray?) {
        val known = (0 until (statuses?.length() ?: 0)).associate { statuses!!.getJSONObject(it).let { o -> o.getString("id") to o.getString("status") } }
        ledger.savePackages(ledger.packages().mapNotNull { p -> if (p.app != app) p else known[p.id]?.let { p.copy(status = it) } })
    }

    private suspend fun <T> withApp(packageName: String, block: suspend (LocalApp, McpPipe) -> T): T {
        val app = localApps.installed().firstOrNull { it.packageName == packageName } ?: throw HubException("BRIDGE_UNSUPPORTED", "")
        return bridge.withApp(app) { block(app, it) }
    }

    companion object { const val GRANT = "com.zirakusa.gatewayhub/grant" }
}
