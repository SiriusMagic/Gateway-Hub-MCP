package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Catalog
import com.zirakusa.gatewayhub.assistant.Examples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** «Reglas de sentimientos» in Lucía › MCP: every rule listed once, each app tool's rule under its family app. */
class SentimentRulesTest {
    @Test fun everyRuleBelongsSomewhere() {
        assertEquals(Examples.actions.size + Examples.tools.size, Examples.rules.size)
        val ids = Catalog.apps.map { it.id }.toSet()
        Examples.rules.filter { it.tool != null }.forEach { assertTrue("${it.tool} → ${it.app}", it.app in ids) }
        assertEquals(Examples.actions, Examples.rulesFor(null).map { it.phrase })
        // Each app's list is exactly its rules, and together they are all of them.
        assertEquals(Examples.rules.size, (listOf<String?>(null) + ids).sumOf { Examples.rulesFor(it).size })
        assertTrue(Examples.rulesFor("stone").any { it.phrase == "qué está sonando" })
        assertTrue(Examples.rulesFor("notas").first { it.tool == "notes_delete" }.asks)       // deleting asks first
        assertFalse(Examples.rulesFor("clima").first { it.tool == "weather_now" }.asks)        // reading doesn't
        assertTrue(Examples.rulesFor("nadie").isEmpty())
    }
}
