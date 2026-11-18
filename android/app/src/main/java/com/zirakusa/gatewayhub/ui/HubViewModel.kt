package com.zirakusa.gatewayhub.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zirakusa.gatewayhub.data.CreatedAgent
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
)

/** The single owner of the app's state; screens only read [state] and call these actions. */
class HubViewModel(
    private val sessions: SessionStore,
    defaultUrl: String,
    private val newApi: (String, String) -> HubApi = { url, token -> HubApi(url, token) },
) : ViewModel(), HubActions {
    private val mutable = MutableStateFlow(UiState(hubUrl = defaultUrl))
    val state: StateFlow<UiState> = mutable
    private var api: HubApi? = null

    init {
        sessions.load()?.let { (url, token) ->
            api = newApi(url, token)
            mutable.update { it.copy(hubUrl = url, connected = true) }
            refresh()
        }
    }

    /** Checks the token against the hub before saving it. */
    fun connect(url: String, token: String) = run {
        val candidate = newApi(url, token.trim())
        candidate.state().also {
            api = candidate
            sessions.save(candidate.base, token.trim())
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
    override fun linkStone(link: String) = run { val note = requireApi().linkStone(link); reload(); note }
    override fun unlinkStone() = run { requireApi().unlinkStone(); reload(); "Stone desvinculado." }

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
