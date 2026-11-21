package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.data.TokenApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TokenAppsTest {
    private val tail = "a".repeat(43)

    @Test fun theTokenSaysWhichAppItIs() {
        assertEquals("Stone", TokenApps.identify("https://relay.example/d/abcdefabcdef/mcp?token=stn_$tail")?.name)
        assertEquals("gallery_", TokenApps.identify("lum_$tail")?.prefix)
        assertEquals("Calculadora", TokenApps.identify("  cal_$tail ")?.name)
        assertEquals("files_", TokenApps.identify("https://r/d/x/mcp?token=fil_$tail")?.prefix)
        assertFalse(TokenApps.identify("not_$tail")!!.supported)
    }

    @Test fun hubAgentAndUnknownTokensAreNotApps() {
        assertNull(TokenApps.identify("hub_$tail"))
        assertNull(TokenApps.identify("gw_$tail"))
        assertNull(TokenApps.identify("xyz_$tail"))
        assertNull(TokenApps.identify("stn_corto"))
        assertNull(TokenApps.identify(""))
    }
}
