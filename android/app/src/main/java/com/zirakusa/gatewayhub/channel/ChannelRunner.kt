package com.zirakusa.gatewayhub.channel

import android.content.Context
import android.os.Build
import com.zirakusa.gatewayhub.data.BridgeConnector
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.KeystoreSessionStore
import com.zirakusa.gatewayhub.data.LocalBridge
import com.zirakusa.gatewayhub.data.PackageScanner
import com.zirakusa.gatewayhub.data.PackageLedger
import com.zirakusa.gatewayhub.data.PrefsLedger
import com.zirakusa.gatewayhub.data.LocalAppSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Keeps this phone's channel with the hub while Gateway should serve (app open, or the background service),
 * and forwards each request to the linked provider app over the local bridge. One per process.
 */
object ChannelRunner {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var client: ChannelClient? = null
    private val mutable = MutableStateFlow(ChannelClient.State.OFFLINE)
    val state: StateFlow<ChannelClient.State> = mutable

    @Synchronized fun sync(context: Context, run: Boolean) { if (run) start(context.applicationContext) else stop() }

    @Synchronized fun stop() { client?.stop(); client = null; mutable.value = ChannelClient.State.OFFLINE }

    /** Sends the apps and packages again (after linking or a package change). */
    fun refresh(context: Context) { client?.send(registration(PrefsLedger(context), PackageScanner(context))) }

    private fun start(context: Context) {
        if (client != null) return
        scope.launch {
            val sessions = KeystoreSessionStore(context)
            val (url, owner) = sessions.load() ?: return@launch
            val device = sessions.device(url) ?: runCatching { HubApi(url, owner).createDevice(Build.MODEL ?: "Android") }.getOrNull()
                ?.also { sessions.saveDevice(url, it) } ?: return@launch
            val bridge: BridgeConnector = LocalBridge(context)
            val apps = PackageScanner(context)
            lateinit var session: ChannelSession
            val c = ChannelClient(url, device.second,
                onOpen = { it.send(registration(PrefsLedger(context), apps)) },
                onText = { session.handle(it) },
                onRevoked = { sessions.saveDevice(url, null); stop() })
            session = ChannelSession(scope, { c.send(it) },
                forward = { app, message -> withApp(apps, bridge, app) { it.raw(message) } },
                notify = { app, message -> withApp(apps, bridge, app) { it.notify(message) } })
            synchronized(this@ChannelRunner) {
                if (client != null) return@launch
                client = c
                scope.launch { c.state.collect { mutable.value = it } }
                c.start()
            }
        }
    }

    private suspend fun <T> withApp(apps: LocalAppSource, bridge: BridgeConnector, app: String, block: suspend (com.zirakusa.gatewayhub.data.McpPipe) -> T): T {
        val local = apps.installed().firstOrNull { it.packageName == app } ?: throw com.zirakusa.gatewayhub.data.HubException("BRIDGE_UNSUPPORTED", "")
        return bridge.withApp(local, block)
    }

    /** The provider apps this Gateway linked (with their prefix) and the packages it issued them. */
    fun registration(ledger: PackageLedger, apps: LocalAppSource): String {
        val providers = apps.installed().filter { it.bridge && it.sameDeveloper && it.prefix != null && ledger.instance(it.packageName) != null }
            .map { app -> JSONObject().put("app", app.packageName).put("prefix", app.prefix).put("title", app.label)
                .put("packages", JSONArray(ledger.packages().filter { it.app == app.packageName }.map { p ->
                    JSONObject().put("id", p.id).put("agent", p.agent).put("acts", JSONArray(p.acts)).put("tools", JSONArray()).put("exp", p.exp).put("status", p.status)
                })) }
        return JSONObject().put("t", "register").put("providers", JSONArray(providers)).toString()
    }
}
