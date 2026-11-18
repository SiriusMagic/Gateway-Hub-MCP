package com.zirakusa.gatewayhub.watch

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.zirakusa.gatewayhub.LocaleContext
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.channel.ChannelRunner
import com.zirakusa.gatewayhub.data.ENV_ACTIVE
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.KeystoreSessionStore
import com.zirakusa.gatewayhub.data.McpMode
import com.zirakusa.gatewayhub.data.PrefsSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * "MCP en segundo plano": keeps a persistent notification while the hub's environment is connected,
 * asks the hub every few minutes what ran, and posts a summary (only when something ran) and an alert
 * when calls failed or were refused. The notification's buttons pause or disconnect the environment.
 * Android may still stop it (battery rules), so it never promises to run forever.
 */
class HubWatchService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null
    private lateinit var notifier: Notifier

    override fun attachBaseContext(base: Context) = super.attachBaseContext(LocaleContext.wrap(base, PrefsSettingsStore(base).load().language))
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() { super.onCreate(); notifier = Notifier(this) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= 34) startForeground(Notifier.ACTIVE_ID, notifier.active(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(Notifier.ACTIVE_ID, notifier.active())
        if (intent?.action == ACTION_ENVIRONMENT) {
            val target = intent.getStringExtra(EXTRA_STATE) ?: return START_NOT_STICKY
            scope.launch { runCatching { api()?.setEnvironment(target) }.onSuccess { notifier.environment(target) }; finish() }
        } else if (loop?.isActive != true) { loop = scope.launch { watch() }; ChannelRunner.sync(this, true) }
        return START_NOT_STICKY
    }

    private suspend fun watch() {
        var since = System.currentTimeMillis()
        var summarySince = since
        var tally = ActivityTally()
        while (scope.isActive) {
            delay(POLL_MS)
            val settings = PrefsSettingsStore(this).load()
            val api = api()
            if (api == null || settings.mode != McpMode.BACKGROUND) return finish()
            val started = System.currentTimeMillis()
            val activity = try { api.activity(since) } catch (e: HubException) {
                if (e.code == "UNAUTHORIZED") return finish() else continue
            }
            since = started
            if (activity.environment != ENV_ACTIVE) return finish()
            tally += activity.metrics
            if (!settings.activityNotifications) { tally = ActivityTally(); continue }
            if (activity.metrics.errors > 0) notifier.alert(activity.metrics.errors)
            if (started - summarySince >= settings.summaryMinutes * 60_000L) {
                if (tally.calls > 0) {
                    val agents = runCatching { api.state().agents }.getOrDefault(emptyList())
                    notifier.summary(tally.calls, tally.errors, tally.agentNames(agents, getString(R.string.metrics_owner), getString(R.string.metrics_revoked)))
                }
                tally = ActivityTally(); summarySince = started
            }
        }
    }

    private fun api(): HubApi? = KeystoreSessionStore(this).load()?.let { (url, token) -> HubApi(url, token) }

    private fun finish() { ChannelRunner.stop(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        const val ACTION_ENVIRONMENT = "com.zirakusa.gatewayhub.action.ENVIRONMENT"
        const val EXTRA_STATE = "state"
        private const val POLL_MS = 5 * 60_000L

        /** Starts or stops the service so it runs only in background mode, logged in, with the environment connected. */
        fun sync(context: Context, run: Boolean) {
            val intent = Intent(context, HubWatchService::class.java)
            if (run) runCatching { context.startForegroundService(intent) } else context.stopService(intent)
        }
    }
}
