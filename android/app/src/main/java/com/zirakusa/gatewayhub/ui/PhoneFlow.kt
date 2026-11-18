package com.zirakusa.gatewayhub.ui

import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.HubTransport
import com.zirakusa.gatewayhub.data.HttpTransport
import com.zirakusa.gatewayhub.data.SessionStore

/**
 * Several phones on one hub (phase 5). The owner's phone makes one-use codes and revokes phones; another
 * phone joins with a code and becomes a satellite: it keeps only its channel credential (no owner token),
 * links its own apps and issues their packages with its own key.
 */
class PhoneFlow(private val sessions: SessionStore, private val transport: HubTransport = HttpTransport) {
    /** The hub this phone serves as a satellite, or null. */
    fun satelliteHub(): String? = if (sessions.load() == null) sessions.deviceHub() else null

    suspend fun join(hubUrl: String, code: String, name: String): String {
        if (code.isBlank()) throw HubException("BAD_PAIRING_CODE", "")
        val url = HubApi.normalize(hubUrl)
        sessions.saveDevice(url, HubApi.pair(url, code, name, transport))
        return url
    }

    fun leave() { sessions.deviceHub()?.let { sessions.saveDevice(it, null) } }
}
