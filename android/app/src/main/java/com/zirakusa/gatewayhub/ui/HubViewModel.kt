package com.zirakusa.gatewayhub.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zirakusa.gatewayhub.data.AccessParser
import com.zirakusa.gatewayhub.data.AppLinker
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.data.RemoteStatus
import com.zirakusa.gatewayhub.data.CreatedAgent
import com.zirakusa.gatewayhub.data.LinkAnswer
import com.zirakusa.gatewayhub.data.TokenApps
import com.zirakusa.gatewayhub.data.LocalAppSource
import com.zirakusa.gatewayhub.data.ScanMerge
import com.zirakusa.gatewayhub.data.ScannedApp
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.HubState
import com.zirakusa.gatewayhub.data.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val hubUrl: String = "",
    val connected: Boolean = false,
    val loading: Boolean = false,
    val hub: HubState? = null,
    val error: String? = null,
    val notice: String? = null,
    /** Shown once in a dialog right after creation; never stored. */
    val created: CreatedAgent? = null,
    /** Last scan of compatible apps (phone + hub); null until the first scan. */
    val scan: List<ScannedApp>? = null,
    /** Scan also links compatible apps by themselves (they must have "Redireccionar a Hub" on). */
    val autoSync: Boolean = true,
)

/** The single owner of the app's state; screens only read [state] and call these actions. */
class HubViewModel(
    private val sessions: SessionStore,
    defaultUrl: String,
    private val localApps: LocalAppSource = LocalAppSource { emptyList() },
    private val linker: AppLinker = AppLinker { LinkAnswer(null, "NOT_SUPPORTED", null) },
    private val newApi: (String, String) -> HubApi = { url, token -> HubApi(url, token) },
) : ViewModel(), HubActions {
    private val defaultUrl = defaultUrl
    private val mutable = MutableStateFlow(UiState(hubUrl = defaultUrl))
    val state: StateFlow<UiState> = mutable
    private var api: HubApi? = null

    init {
        mutable.update { it.copy(autoSync = sessions.flag("autoSync", true)) }
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

    override fun refresh() = run { mutable.update { s -> s.copy(hub = requireApi().state()) }; null }

    override fun createAgent(name: String, apps: List<String>, readOnly: Boolean, days: Int) = run {
        val created = requireApi().createAgent(name, apps, readOnly, days)
        mutable.update { it.copy(created = created, hub = requireApi().state()) }
        null
    }

    override fun setPaused(id: String, paused: Boolean) = run {
        requireApi().setPaused(id, paused); reload(); if (paused) "Enlace pausado." else "Enlace reanudado."
    }
    override fun revoke(id: String) = run { requireApi().revoke(id); reload(); "Enlace revocado: deja de funcionar ya." }
    /** «Vincular con token»: the token says which app it is; the answer is one simple line («Stone: token vinculado»). */
    override fun linkToken(link: String) = run {
        val app = TokenApps.identify(link) ?: throw HubException("UNKNOWN_TOKEN", "No reconozco ese token.")
        if (!app.supported) throw HubException("NOT_SUPPORTED", "${app.name} todavía no se puede vincular al hub.")
        if (app.prefix == "music_") requireApi().linkStone(link) else requireApi().linkApp(app.prefix, link)
        reload(); "${app.name}: token vinculado"
    }

    /**
     * Lists compatible apps and asks the hub for their state (read-only). With [UiState.autoSync], an app
     * of the same developer that is installed but not linked (or whose token was revoked) is asked for its
     * link and linked; the app answers only if the user turned "Redireccionar a Hub" on in it.
     */
    override fun scan() = run {
        val local = localApps.installed()
        var remote = requireApi().scan()
        val notes = if (mutable.value.autoSync) ScanMerge.merge(local, remote).mapNotNull { link(it.local, it.remote) } else emptyList()
        if (notes.isNotEmpty()) remote = requireApi().scan()
        mutable.update { it.copy(scan = ScanMerge.merge(local, remote)) }
        notes.joinToString("\n").ifBlank { null }
    }

    /**
     * «Conectar todas»: every app of the same developer installed here turns «Redireccionar a Hub» on by itself (the user's
     * tap is the consent) and is linked to the hub. One line per app that could not, so it is clear which one and why.
     */
    override fun connectAll() = run {
        val local = localApps.installed()
        val rows = ScanMerge.merge(local, requireApi().scan())
        val problems = mutableListOf<String>(); var done = 0
        for (row in rows) {
            val app = row.local ?: continue; val remote = row.remote ?: continue
            if (!app.sameDeveloper || app.linkAuthority == null || !remote.remote) continue
            val answer = linker.connect(app)
            val link = answer.link
            if (link == null) { problems += "${app.label}: ${answer.message ?: answer.error}"; continue }
            runCatching { if (remote.prefix == "music_") requireApi().linkStone(link) else requireApi().linkApp(remote.prefix, link) }
                .onSuccess { done++ }.onFailure { problems += "${app.label}: ${it.message}" }
        }
        mutable.update { it.copy(scan = ScanMerge.merge(localApps.installed(), requireApi().scan())) }
        (listOf("$done apps conectadas al hub.") + problems).joinToString("\n")
    }

    /** Links one app now (button "Vincular automáticamente"). */
    override fun autoLink(packageName: String) = run {
        val row = mutable.value.scan?.firstOrNull { it.local?.packageName == packageName }
        val note = link(row?.local, row?.remote, force = true)
        mutable.update { it.copy(scan = ScanMerge.merge(localApps.installed(), requireApi().scan())) }
        note
    }

    override fun setAutoSync(on: Boolean) { sessions.setFlag("autoSync", on); mutable.update { it.copy(autoSync = on) } }

    private suspend fun link(app: LocalApp?, remote: RemoteStatus?, force: Boolean = false): String? {
        if (app == null || remote == null || !remote.remote || !app.sameDeveloper || app.linkAuthority == null) return null
        val revoked = remote.linked && !remote.online && remote.detail.contains("rejected")
        if (!force && remote.linked && !revoked) return null
        val answer = linker.requestLink(app)
        val link = answer.link ?: return "${app.label}: ${answer.message ?: answer.error}"
        // Stone has its own route; every other app with Modo MCP (Luma…) goes through /apps/<prefix>.
        return if (remote.prefix == "music_") requireApi().linkStone(link) else requireApi().linkApp(remote.prefix, link)
    }

    override fun dismissCreated() = mutable.update { it.copy(created = null) }
    override fun dismissMessage() = mutable.update { it.copy(error = null, notice = null) }

    override fun logout() {
        sessions.clear(); api = null
        mutable.update { UiState(hubUrl = it.hubUrl) }
    }

    private suspend fun reload() = mutable.update { it.copy(hub = requireApi().state()) }
    private fun requireApi() = api ?: throw HubException("UNAUTHORIZED", "Conéctate de nuevo.")

    /** Runs [block] with a loading flag; its text (if any) becomes a notice, a failure becomes an error. */
    private fun run(block: suspend () -> String?) {
        mutable.update { it.copy(loading = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                val note = block()
                mutable.update { it.copy(loading = false, notice = note) }
            } catch (e: HubException) {
                // A token the hub no longer accepts means the session is over.
                if (e.code == "UNAUTHORIZED" && mutable.value.connected) logout()
                mutable.update { it.copy(loading = false, error = e.message) }
            } catch (e: Exception) {
                mutable.update { it.copy(loading = false, error = "El hub respondió algo inesperado.") }
            }
        }
    }
}
