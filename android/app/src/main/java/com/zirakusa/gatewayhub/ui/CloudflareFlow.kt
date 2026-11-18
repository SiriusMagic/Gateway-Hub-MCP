package com.zirakusa.gatewayhub.ui

import com.zirakusa.gatewayhub.data.Access
import com.zirakusa.gatewayhub.data.CloudflareApi
import com.zirakusa.gatewayhub.data.CloudflareShare
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.HubState
import kotlinx.coroutines.delay
import com.zirakusa.gatewayhub.data.LocalAppSource
import com.zirakusa.gatewayhub.data.OwnerToken
import com.zirakusa.gatewayhub.data.SavedCloudflare
import com.zirakusa.gatewayhub.data.SessionStore

/** What the screens may know of the Cloudflare token: never the token itself. */
data class CloudflareInfo(val expiresAt: Long, val savedAt: Long) {
    /** Whole days left (negative once expired); null when it has no end date. */
    fun daysLeft(now: Long = System.currentTimeMillis()): Long? = if (expiresAt <= 0) null else Math.floorDiv(expiresAt - now, 86_400_000L)
}

/**
 * The person's Cloudflare token: checked and saved (encrypted), shared with Stone so it is updated in
 * one place, and used to give the owner a new hub_ token.
 */
class CloudflareFlow(
    private val sessions: SessionStore,
    private val localApps: LocalAppSource,
    private val share: CloudflareShare,
    private val newApi: (String) -> CloudflareApi,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun info(): CloudflareInfo? = sessions.cloudflare()?.let { CloudflareInfo(it.expiresAt, it.savedAt) }

    /** Checks [token] with Cloudflare, saves it and passes it to Stone. */
    suspend fun save(token: String): CloudflareInfo {
        val clean = token.trim()
        if (clean.isEmpty()) throw HubException("CF_EMPTY", "")
        val saved = SavedCloudflare(clean, newApi(clean).verify(), now())
        sessions.saveCloudflare(saved)
        apps().forEach { runCatching { share.push(it, saved) } }
        return CloudflareInfo(saved.expiresAt, saved.savedAt)
    }

    /** Takes Stone's token when it is newer than ours (it was updated there). */
    fun pullNewer(): CloudflareInfo? {
        val mine = sessions.cloudflare()
        val theirs = runCatching { apps().mapNotNull { share.pull(it) }.maxByOrNull { it.savedAt } }.getOrNull()
        if (theirs != null && theirs.savedAt > (mine?.savedAt ?: 0)) sessions.saveCloudflare(theirs)
        return info()
    }

    fun forget() = sessions.saveCloudflare(null)

    /** A new owner token stored in the person's hub; the old one stops working. */
    suspend fun issueOwner(): Access {
        val saved = sessions.cloudflare() ?: throw HubException("CF_MISSING", "")
        return OwnerToken.issue(newApi(saved.token))
    }

    private fun apps() = localApps.installed().filter { it.sameDeveloper && it.linkAuthority != null }
}

/** The hub's state; right after a new owner token is stored the hub may still refuse it for a few seconds. */
suspend fun stateWithRetry(api: HubApi, tries: Int, pause: Long = 2_000): HubState {
    repeat(tries - 1) {
        try { return api.state() } catch (e: HubException) { if (e.code != "UNAUTHORIZED") throw e }
        delay(pause)
    }
    return api.state()
}
