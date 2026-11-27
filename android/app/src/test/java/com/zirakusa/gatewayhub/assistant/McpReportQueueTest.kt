package com.zirakusa.gatewayhub.assistant

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class McpReportQueueTest {
    @Test fun consentPersistsQueueAndRevokingItClearsWork() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = McpReportQueue.preferences(context)
        val report = JSONObject().put("id", "first")
        McpReportQueue.add(prefs, report)
        assertEquals(0, McpReportQueue.size(prefs))

        McpReportQueue.allow(prefs, true)
        McpReportQueue.add(prefs, report)
        McpReportQueue.add(prefs, JSONObject().put("id", "second"))
        assertEquals("first", McpReportQueue.next(McpReportQueue.preferences(context))?.getString("id"))

        McpReportQueue.delivered(prefs, "unknown")
        assertEquals(2, McpReportQueue.size(prefs))
        McpReportQueue.delivered(prefs, "first")
        assertEquals("second", McpReportQueue.next(prefs)?.getString("id"))
        McpReportQueue.allow(prefs, false)
        assertNull(McpReportQueue.next(prefs))
        assertEquals(0, McpReportQueue.size(prefs))
    }
}
