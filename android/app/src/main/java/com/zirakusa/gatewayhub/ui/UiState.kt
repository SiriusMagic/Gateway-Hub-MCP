package com.zirakusa.gatewayhub.ui

import com.zirakusa.gatewayhub.data.AppSettings
import com.zirakusa.gatewayhub.data.CreatedAgent
import com.zirakusa.gatewayhub.data.HubState
import com.zirakusa.gatewayhub.data.IssuedPackage
import com.zirakusa.gatewayhub.data.ScannedApp

data class UiState(
    val hubUrl: String = "",
    val connected: Boolean = false,
    val loading: Boolean = false,
    val hub: HubState? = null,
    val error: UiText? = null,
    val notice: UiText? = null,
    /** Shown once in a dialog right after creation; never stored. */
    val created: CreatedAgent? = null,
    /** Last scan of compatible apps (phone + hub); null until the first scan. */
    val scan: List<ScannedApp>? = null,
    /** Scan also links compatible apps by themselves (they must have "Redirección hub" on). */
    val autoSync: Boolean = true,
    val settings: AppSettings = AppSettings(),
    /** Execution packages this Gateway issued (status as the provider app last reported). */
    val packages: List<IssuedPackage> = emptyList(),
)

/** The actions the screens may trigger (the ViewModel in the app, a fake in tests). */
interface HubActions {
    fun refresh(); fun logout(); fun dismissMessage(); fun dismissCreated()
    fun setPaused(id: String, paused: Boolean); fun revoke(id: String)
    fun createAgent(name: String, apps: List<String>, readOnly: Boolean, days: Int)
    fun linkStone(link: String); fun unlinkStone()
    fun scan(); fun autoLink(packageName: String); fun setAutoSync(on: Boolean)
    /** "active", "paused" or "disconnected". */
    fun setEnvironment(target: String)
    fun updateSettings(change: (AppSettings) -> AppSettings)
    /** With a Cloudflare token used once: deploy or update the hub and log in / update it / new owner token. */
    fun createAccess(cfToken: String); fun updateHub(cfToken: String); fun rotateOwner(cfToken: String)
    /** Phase 1 of the hub migration: checks the local MCP bridge with one app (read-only). */
    fun testBridge(packageName: String)
    /** Phase 2: provider apps and their execution packages (approved in the app). */
    fun linkProvider(packageName: String); fun refreshPackages(packageName: String)
    fun offerPackage(packageName: String, agent: String, label: String, acts: List<String>, days: Int)
    fun revokePackage(packageName: String, id: String)
}
