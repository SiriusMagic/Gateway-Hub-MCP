package com.zirakusa.gatewayhub

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zirakusa.gatewayhub.channel.ChannelRunner
import com.zirakusa.gatewayhub.data.ENV_ACTIVE
import com.zirakusa.gatewayhub.data.KeystoreSessionStore
import com.zirakusa.gatewayhub.data.KeystoreSigner
import com.zirakusa.gatewayhub.data.LocalBridge
import com.zirakusa.gatewayhub.data.WorkerBundle
import com.zirakusa.gatewayhub.data.PrefsLedger
import com.zirakusa.gatewayhub.data.McpMode
import com.zirakusa.gatewayhub.data.PackageScanner
import com.zirakusa.gatewayhub.data.PrefsSettingsStore
import com.zirakusa.gatewayhub.data.ProviderLinker
import com.zirakusa.gatewayhub.ui.HomeScreen
import com.zirakusa.gatewayhub.ui.HubTheme
import com.zirakusa.gatewayhub.ui.HubViewModel
import com.zirakusa.gatewayhub.ui.LoginScreen
import com.zirakusa.gatewayhub.ui.PALETTES
import com.zirakusa.gatewayhub.ui.WelcomeScreen
import com.zirakusa.gatewayhub.watch.HubWatchService

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        vm.state.value.let { ChannelRunner.sync(applicationContext, it.connected && it.hub?.environment == ENV_ACTIVE) }
    }

    /** Safe mode: nothing keeps serving once Gateway leaves the screen. */
    override fun onStop() {
        if (vm.state.value.settings.mode == McpMode.SAFE) ChannelRunner.stop()
        super.onStop()
    }

    private val vm: HubViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val linker = ProviderLinker(applicationContext)
                return HubViewModel(KeystoreSessionStore(applicationContext), BuildConfig.DEFAULT_HUB_URL, PackageScanner(applicationContext),
                    linker, settingsStore = PrefsSettingsStore(applicationContext), modeSender = linker, workerBundle = ::workerBundle, bridge = LocalBridge(applicationContext),
                    signer = { KeystoreSigner(applicationContext) }, ledger = PrefsLedger(applicationContext),
                    afterPackages = { ChannelRunner.refresh(applicationContext) }) as T
            }
        }
    }
    /** The hub Worker this APK carries (npm run bundle:android). */
    private fun workerBundle(): WorkerBundle? = runCatching {
        val meta = org.json.JSONObject(assets.open("hub-worker.json").bufferedReader().use { it.readText() })
        WorkerBundle(assets.open("hub-worker.js").use { it.readBytes() }, meta.getString("version"), meta.getString("compatibilityDate"))
    }.getOrNull()

    /** The language this Activity was created with; choosing another one recreates it. */
    private var language: String? = null

    override fun attachBaseContext(base: Context) {
        language = PrefsSettingsStore(base).load().language
        super.attachBaseContext(LocaleContext.wrap(base, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            val settings = state.settings
            LaunchedEffect(settings.language) { if (settings.language != language) recreate() }
            // Screenshots are allowed unless the person blocks them in Settings › Privacy.
            LaunchedEffect(settings.blockScreenshots) {
                if (settings.blockScreenshots) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            LaunchedEffect(settings.theme) {
                val p = PALETTES.getValue(settings.theme)
                val bar = p.background.toArgb()
                enableEdgeToEdge(if (p.dark) SystemBarStyle.dark(bar) else SystemBarStyle.light(bar, bar),
                    if (p.dark) SystemBarStyle.dark(bar) else SystemBarStyle.light(bar, bar))
            }
            // The background service runs only in background mode, logged in, with the environment connected.
            val environment = state.hub?.environment
            // The phone's channel with the hub: while connected and the environment is active (the app open,
            // or the background service in background mode).
            LaunchedEffect(state.connected, environment) { ChannelRunner.sync(applicationContext, state.connected && environment == ENV_ACTIVE) }
            LaunchedEffect(settings.mode, state.connected, environment) {
                if (!state.connected || environment != null)
                    HubWatchService.sync(applicationContext, state.connected && settings.mode == McpMode.BACKGROUND && environment == ENV_ACTIVE)
            }
            HubTheme(settings.theme) {
                when {
                    settings.language == null -> WelcomeScreen { tag -> vm.updateSettings { it.copy(language = tag) } }
                    state.connected -> HomeScreen(state, vm)
                    else -> LoginScreen(state, vm::connect, vm::createAccess)
                }
            }
        }
    }
}
