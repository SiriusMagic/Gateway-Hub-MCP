package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.data.Access
import com.zirakusa.gatewayhub.data.AccessParser
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.HubJson
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.data.ScanMerge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccessAndScanTest {
    private val owner = "hub_AbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_"
    private val default = "https://default.example"

    @Test fun everyWayPeoplePasteTheAccess() {
        val expected = Access("https://hub.example", owner)
        for (pasted in listOf("https://hub.example#$owner", "https://hub.example/admin#$owner", "https://hub.example/admin?token=$owner",
            "https://hub.example\n$owner", "  $owner   https://hub.example/ ")) {
            assertEquals(pasted, expected, AccessParser.parse(pasted, default))
        }
        assertEquals(Access(default, owner), AccessParser.parse(owner, default))
        assertEquals("https://hub.example#$owner", AccessParser.format(expected))
    }

    @Test fun stoneAndAgentLinksAreExplainedNotUsed() {
        fun code(text: String) = try { AccessParser.parse(text, default); "ok" } catch (e: HubException) { e.code }
        assertEquals("STONE_LINK", code("https://stone-relay.x.workers.dev/d/3e6076f88b2ae131ea6e/mcp?token=stn_WiBFniD3cEWxvpdKMfGU"))
        assertEquals("STONE_LINK", code("stn_WiBFniD3cEWxvpdKMfGUJORq8"))
        assertEquals("AGENT_LINK", code("https://hub.example/mcp?token=gw_abcdefghijklmnopqrstuvwxyz"))
        assertEquals("EMPTY", code("   "))
        assertEquals("NO_TOKEN", code("https://hub.example"))
        try { AccessParser.parse("http://hub.example#$owner", default); fail() } catch (e: HubException) { assertEquals("INVALID_URL", e.code) }
    }

    @Test fun scanJoinsPhoneAppsWithHubModules() {
        val remote = HubJson.scan(SCAN_JSON)
        val stone = LocalApp("com.zirakusa.stone.kotlin.preview", "Stone", "1.7.12", "music_", true)
        val stranger = LocalApp("com.example.other", "Other", "1", "other_", false)
        val rows = ScanMerge.merge(listOf(stone, stranger), remote)
        assertEquals(3, rows.size)
        assertEquals(stone, rows.first { it.prefix == "music_" }.local)
        assertTrue(rows.first { it.prefix == "music_" }.remote!!.linked)
        assertNull(rows.first { it.prefix == "weather_" }.local)
        assertNull(rows.first { it.local == stranger }.remote) // installed, not in the hub yet
    }
}
