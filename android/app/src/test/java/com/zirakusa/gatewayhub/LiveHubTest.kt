package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.data.HubApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Optional end-to-end check against a real hub with the real HTTPS client. Runs only with
 * GATEWAY_HUB_URL and GATEWAY_HUB_TOKEN set; it creates one test agent and revokes it at the end.
 */
@RunWith(RobolectricTestRunner::class)
class LiveHubTest {
    @Test fun createPauseResumeRevokeOnTheRealHub() = runBlocking {
        val url = System.getenv("GATEWAY_HUB_URL").orEmpty(); val token = System.getenv("GATEWAY_HUB_TOKEN").orEmpty()
        assumeTrue("GATEWAY_HUB_URL / GATEWAY_HUB_TOKEN not set", url.isNotBlank() && token.isNotBlank())
        val api = HubApi(url, token)
        assertTrue(api.state().version.isNotBlank())
        // The scan only reads: every app answers with its state, nothing is turned on.
        assertTrue(api.scan().any { it.prefix == "music_" })
        assertTrue(api.state().environment.isNotBlank())
        val created = api.createAgent("Prueba app Android", listOf("music_"), readOnly = true, days = 1)
        try {
            assertTrue(created.url.startsWith("https://") && created.token.startsWith("gw_"))
            api.setPaused(created.agent.id, true)
            assertTrue(api.state().agents.first { it.id == created.agent.id }.paused)
            api.setPaused(created.agent.id, false)
        } finally { api.revoke(created.agent.id) }
        assertFalse(api.state().agents.any { it.id == created.agent.id })
    }
}
