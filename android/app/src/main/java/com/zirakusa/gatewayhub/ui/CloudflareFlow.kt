package com.zirakusa.gatewayhub.ui

import com.zirakusa.gatewayhub.data.Access
import com.zirakusa.gatewayhub.data.CloudflareApi
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubDeployer
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.HubState
import com.zirakusa.gatewayhub.data.WorkerBundle
import kotlinx.coroutines.delay

/**
 * Everything that needs the person's Cloudflare token. The token is checked, used for this one task and
 * forgotten: it is never stored, in this app or in Stone.
 */
class CloudflareFlow(private val newApi: (String) -> CloudflareApi, private val bundle: () -> WorkerBundle?) {
    private suspend fun api(token: String): CloudflareApi {
        val clean = token.trim()
        if (clean.isEmpty()) throw HubException("CF_EMPTY", "")
        return newApi(clean).also { it.verify() }
    }

    /** No hub_ token: deploys the hub if the account has none (or updates it) and makes a new owner token. */
    suspend fun createAccess(token: String): Access = HubDeployer.run(api(token), bundle(), newOwner = true).access!!

    /** Updates the hub to the version this app carries; keeps its token, agents and data. Returns its address. */
    suspend fun updateHub(token: String): String = HubDeployer.run(api(token), bundle() ?: throw HubException("NO_BUNDLE", ""), newOwner = false).url

    /** A new owner token stored in the hub; the old one stops working. */
    suspend fun rotateOwner(token: String): Access = HubDeployer.run(api(token), null, newOwner = true).access!!
}

/** The hub's state; right after a new owner token is stored the hub may still refuse it for a few seconds. */
suspend fun stateWithRetry(api: HubApi, tries: Int, pause: Long = 2_000): HubState {
    repeat(tries - 1) {
        try { return api.state() } catch (e: HubException) { if (e.code != "UNAUTHORIZED" && e.code != "NETWORK") throw e }
        delay(pause)
    }
    return api.state()
}
