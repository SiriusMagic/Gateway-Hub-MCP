package com.zirakusa.gatewayhub.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.AccessParser
import com.zirakusa.gatewayhub.data.AppLinker
import com.zirakusa.gatewayhub.data.AppSettings
import com.zirakusa.gatewayhub.data.ENV_ACTIVE
import com.zirakusa.gatewayhub.data.ENV_DISCONNECTED
import com.zirakusa.gatewayhub.data.ENV_PAUSED
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.LinkAnswer
import com.zirakusa.gatewayhub.data.LocalAppSource
import com.zirakusa.gatewayhub.data.MemorySettingsStore
import com.zirakusa.gatewayhub.data.ModeSender
import com.zirakusa.gatewayhub.data.ScanMerge
import com.zirakusa.gatewayhub.data.SessionStore
import com.zirakusa.gatewayhub.data.SettingsStore
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
) : ViewModel(), HubActions {
    private val mutable = MutableStateFlow(UiState(hubUrl = defaultUrl, settings = settingsStore.load()))
    val state: StateFlow<UiState> = mutable
    private var api: HubApi? = null
    private val sync = AppSync(localApps, linker) { requireApi() }

    init {
        mutable.update { it.copy(autoSync = sessions.flag("autoSync", true)) }
        sendMode()
        sessions.load()?.let { (url, token) ->
            api = newApi(url, token)
            mutable.update { it.copy(hubUrl = url, connected = true) }
            refresh()
        }
    }

    /** One paste (address + token, or only the token); checked against the hub before it is saved. */
    fun connect(pasted: String) = run {
        val access = AccessParser.parse(pasted, defaultUrl)
        val candidate = newApi(access.url, access.token)
        candidate.state().also {
            api = candidate
            sessions.save(candidate.base, access.token)
            mutable.update { s -> s.copy(hubUrl = candidate.base, connected = true, hub = it) }
        }
        null
    }

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

    /**
     * Connect, pause or disconnect the whole MCP environment (the hub enforces it). Connecting after a
     * disconnection links again, by themselves, the apps that allow it (with automatic sync on).
     */
    override fun setEnvironment(target: String) = run {
        val wasDisconnected = mutable.value.hub?.environment == ENV_DISCONNECTED
        requireApi().setEnvironment(target)
        val relinked = if (target == ENV_ACTIVE && wasDisconnected && mutable.value.autoSync) sync.scanAndLink { scan -> mutable.update { it.copy(scan = scan) } } else emptyList()
        reload()
        val note = UiText.Res(when (target) { ENV_PAUSED -> R.string.env_note_paused; ENV_DISCONNECTED -> R.string.env_note_disconnected; else -> R.string.env_note_active })
        if (relinked.isEmpty()) note else UiText.Lines(listOf(note) + relinked)
    }

    /**
     * Lists compatible apps and asks the hub for their state (read-only). With [UiState.autoSync], an app
     * of the same developer that is installed but not linked (or whose token was revoked) is asked for its
     * link and linked; the app answers only if the user turned "Redirección hub" on in it.
     */
    override fun scan() = run {
        val notes = if (mutable.value.autoSync) sync.scanAndLink { scan -> mutable.update { it.copy(scan = scan) } } else { mutable.update { it.copy(scan = ScanMerge.merge(localApps.installed(), requireApi().scan())) }; emptyList() }
        notes.takeIf { it.isNotEmpty() }?.let { UiText.Lines(it) }
    }

    /** Links one app now (button "Vincular automáticamente"). */
    override fun autoLink(packageName: String) = run {
        val row = mutable.value.scan?.firstOrNull { it.local?.packageName == packageName }
        val note = sync.link(row?.local, row?.remote, force = true)
        mutable.update { it.copy(scan = ScanMerge.merge(localApps.installed(), requireApi().scan())) }
        note
    }

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
        mutable.update { UiState(hubUrl = it.hubUrl, autoSync = it.autoSync, settings = it.settings) }
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
