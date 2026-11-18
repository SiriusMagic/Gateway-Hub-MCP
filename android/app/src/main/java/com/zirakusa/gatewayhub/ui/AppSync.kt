package com.zirakusa.gatewayhub.ui

import com.zirakusa.gatewayhub.data.AppLinker
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.data.LocalAppSource
import com.zirakusa.gatewayhub.data.McpMode
import com.zirakusa.gatewayhub.data.ModeSender
import com.zirakusa.gatewayhub.data.RemoteStatus
import com.zirakusa.gatewayhub.data.ScanMerge
import com.zirakusa.gatewayhub.data.ScannedApp

/** Scans the phone and the hub, and links by itself each app that allows it ("Redirección hub"). */
class AppSync(private val localApps: LocalAppSource, private val linker: AppLinker, private val api: () -> HubApi) {
    /** Scans, links what can be linked, publishes the final scan and returns one note per app it tried. */
    suspend fun scanAndLink(publish: (List<ScannedApp>) -> Unit): List<UiText> {
        val local = localApps.installed()
        var remote = api().scan()
        val notes = ScanMerge.merge(local, remote).mapNotNull { link(it.local, it.remote) }
        if (notes.isNotEmpty()) remote = api().scan()
        publish(ScanMerge.merge(local, remote))
        return notes
    }

    /** Asks one app for its link and gives it to the hub; null when there is nothing to do. */
    suspend fun link(app: LocalApp?, remote: RemoteStatus?, force: Boolean = false): UiText? {
        if (app == null || remote == null || !remote.remote || !app.sameDeveloper || app.linkAuthority == null) return null
        val revoked = remote.linked && !remote.online && remote.detail.contains("rejected")
        if (!force && remote.linked && !revoked) return null
        if (remote.prefix != "music_") return null // the hub links Stone today; other apps will follow the same path
        val answer = linker.requestLink(app)
        return answer.link?.let { UiText.Raw(api().linkStone(it)) } ?: linkFailure(app.label, answer.error, answer.message)
    }

    /** In safe mode the apps serve MCP only while on screen. Apps without support ignore it. */
    fun sendMode(sender: ModeSender, mode: McpMode) = runCatching {
        val name = if (mode == McpMode.BACKGROUND) "background" else "safe"
        localApps.installed().filter { it.sameDeveloper && it.linkAuthority != null }.forEach { sender.sendMode(it, name) }
    }
}
