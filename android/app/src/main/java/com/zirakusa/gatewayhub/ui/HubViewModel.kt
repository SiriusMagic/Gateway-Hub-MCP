package com.zirakusa.gatewayhub.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The single owner of the app's state; screens only read [state] and call these actions. */
class HubViewModel(
    private val sessions: SessionStore,
    private val defaultUrl: String,
    private val localApps: LocalAppSource = LocalAppSource { emptyList() },
    private val linker: AppLinker = AppLinker { LinkAnswer(null, "NOT_SUPPORTED", null) },
    private val newApi: (String, String) -> HubApi = { url, token -> HubApi(url, token) },
    private val settingsStore: SettingsStore = MemorySettingsStore(),
    private val modeSender: ModeSender = ModeSender { _, _ -> false },
    share: CloudflareShare = CloudflareShare.None,
    newCloudflare: (String) -> CloudflareApi = { CloudflareApi(it) },
    bridge: BridgeConnector = BridgeConnector.None,
    signer: () -> PackageSigner = SoftwareSigner().let { key -> { key } },
    ledger: PackageLedger = MemoryLedger(),
    private val afterPackages: () -> Unit = {},
) : ViewModel(), HubActions {
    private val mutable = MutableStateFlow(UiState(hubUrl = defaultUrl, settings = settingsStore.load()))
    val state: StateFlow<UiState> = mutable
    private var api: HubApi? = null
    private val sync = AppSync(localApps, linker) { requireApi() }
    private val providers = ProviderFlow(bridge, signer, ledger, localApps)
    private val cloudflare = CloudflareFlow(sessions, localApps, share, newCloudflare)

    init {
        mutable.update { it.copy(autoSync = sessions.flag("autoSync", true), packages = ledger.packages()) }
        sendMode()
        viewModelScope.launch(Dispatchers.IO) { val info = cloudflare.pullNewer(); mutable.update { it.copy(cloudflare = info) } }
        sessions.load()?.let { (url, token) ->
            api = newApi(url, token)
            mutable.update { it.copy(hubUrl = url, connected = true) }
            refresh()
        }
    }

    /** One paste (address + token, or only the token); checked against the hub before it is saved. */
    fun connect(pasted: String) = run { open(AccessParser.parse(pasted, defaultUrl)); null }

    /** Checks [access] against the hub and only then saves it. */
    private suspend fun open(access: Access, tries: Int = 1) {
        val candidate = newApi(access.url, access.token)
        val hub = stateWithRetry(candidate, tries)
        api = candidate
        sessions.save(candidate.base, access.token)
        mutable.update { s -> s.copy(hubUrl = candidate.base, connected = true, hub = hub) }
    }

    /** No hub_ token? The app makes one with the person's Cloudflare token ([cfToken], or the saved one). */
    override fun createAccess(cfToken: String) = run {
        if (cfToken.isNotBlank()) mutable.update { it.copy(cloudflare = cloudflare.save(cfToken)) }
        open(cloudflare.issueOwner(), tries = 6); UiText.Res(R.string.cf_owner_done)
    }
    override fun saveCloudflare(token: String) = run { mutable.update { it.copy(cloudflare = cloudflare.save(token)) }; UiText.Res(R.string.cf_saved) }
    override fun forgetCloudflare() { cloudflare.forget(); mutable.update { it.copy(cloudflare = null) } }

    override fun refresh() = run { reload(); null }

    override fun createAgent(name: String, apps: List<String>, readOnly: Boolean, days: Int) = run {
        val created = requireApi().createAgent(name, apps, readOnly, days)
        mutable.update { it.copy(created = created, hub = requireApi().state()) }
        null
    }

    override fun setPaused(id: String, paused: Boolean) = run {
        requireApi().setPaused(id, paused); reload(); UiText.Res(if (paused) R.string.note_paused else R.string.note_resumed)
    }
    override fun revoke(id: String) = run { requireApi().revoke(id); reload(); UiText.Res(R.string.note_revoked) }
    override fun linkStone(link: String) = run { val note = requireApi().linkStone(link); reload(); UiText.Raw(note) }
    override fun unlinkStone() = run { requireApi().unlinkStone(); reload(); UiText.Res(R.string.note_unlinked) }

    /** Connect, pause or disconnect the MCP environment (the hub enforces it); reconnecting relinks Stone. */
    override fun setEnvironment(target: String) = run {
        val wasDisconnected = mutable.value.hub?.environment == ENV_DISCONNECTED
        requireApi().setEnvironment(target)
        val relinked = if (target == ENV_ACTIVE && wasDisconnected && mutable.value.autoSync) sync.scanAndLink { scan -> mutable.update { it.copy(scan = scan) } } else emptyList()
        reload()
        val note = UiText.Res(when (target) { ENV_PAUSED -> R.string.env_note_paused; ENV_DISCONNECTED -> R.string.env_note_disconnected; else -> R.string.env_note_active })
        if (relinked.isEmpty()) note else UiText.Lines(listOf(note) + relinked)
    }

    /** Read-only scan; with [UiState.autoSync] it also links the apps that allow it (see [AppSync]). */
    override fun scan() = run {
        val notes = if (mutable.value.autoSync) sync.scanAndLink { scan -> mutable.update { it.copy(scan = scan) } } else { mutable.update { it.copy(scan = ScanMerge.merge(localApps.installed(), requireApi().scan())) }; emptyList() }
        notes.takeIf { it.isNotEmpty() }?.let { UiText.Lines(it) }
    }

    /** Links one app now (button "Vincular automáticamente"). */
    override fun autoLink(packageName: String) = run {
        val row = mutable.value.scan?.firstOrNull { it.local?.packageName == packageName }
        sync.link(row?.local, row?.remote, force = true).also { mutable.update { s -> s.copy(scan = ScanMerge.merge(localApps.installed(), requireApi().scan())) } }
    }

    override fun testBridge(packageName: String) = providers(packageName) { providers.test(it) }
    override fun linkProvider(packageName: String) = providers(packageName) { providers.link(it, mutable.value.hub?.environment ?: ENV_ACTIVE, mutable.value.settings.mode.name.lowercase()) }
    override fun offerPackage(packageName: String, agent: String, label: String, acts: List<String>, days: Int) = providers(packageName) { providers.offer(it, agent, label, acts, days) }
    override fun revokePackage(packageName: String, id: String) = providers(packageName) { providers.revoke(it, id) }
    override fun refreshPackages(packageName: String) = providers(packageName) { providers.refresh(it) }
    private fun providers(name: String, block: suspend (String) -> UiText?) = run { block(name).also { mutable.update { s -> s.copy(packages = providers.packages()) }; afterPackages() } }

    override fun setAutoSync(on: Boolean) { sessions.setFlag("autoSync", on); mutable.update { it.copy(autoSync = on) } }

    override fun updateSettings(change: (AppSettings) -> AppSettings) {
        val next = change(mutable.value.settings)
        val modeChanged = next.mode != mutable.value.settings.mode
        settingsStore.save(next); mutable.update { it.copy(settings = next) }; if (modeChanged) sendMode()
    }

    /** Tells Stone (same developer) the chosen mode, at start and on every change. */
    private fun sendMode() = mutable.value.settings.mode.let { mode -> viewModelScope.launch(Dispatchers.IO) { sync.sendMode(modeSender, mode) } }

    override fun dismissCreated() = mutable.update { it.copy(created = null) }
    override fun dismissMessage() = mutable.update { it.copy(error = null, notice = null) }

    override fun logout() {
        sessions.clear(); api = null
        mutable.update { UiState(hubUrl = it.hubUrl, autoSync = it.autoSync, settings = it.settings, cloudflare = it.cloudflare) }
    }

    private suspend fun reload() = mutable.update { it.copy(hub = requireApi().state()) }
    private fun requireApi() = api ?: throw HubException("UNAUTHORIZED", "")

    /** Runs [block] with a loading flag; its text (if any) becomes a notice, a failure becomes an error. */
    private fun run(block: suspend () -> UiText?) {
        mutable.update { it.copy(loading = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                val note = block()
                mutable.update { it.copy(loading = false, notice = note) }
            } catch (e: HubException) {
                // A token the hub no longer accepts means the session is over.
                if (e.code == "UNAUTHORIZED" && mutable.value.connected) logout()
                mutable.update { it.copy(loading = false, error = e.toUiText()) }
            } catch (e: Exception) {
                mutable.update { it.copy(loading = false, error = UiText.Res(R.string.err_unexpected)) }
            }
        }
    }
}
