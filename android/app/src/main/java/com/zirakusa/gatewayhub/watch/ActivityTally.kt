package com.zirakusa.gatewayhub.watch

import com.zirakusa.gatewayhub.data.Agent
import com.zirakusa.gatewayhub.data.Metrics

/** What ran between two summaries: added up from each poll of the hub. */
data class ActivityTally(val calls: Int = 0, val errors: Int = 0, val agentIds: Set<String> = emptySet()) {
    operator fun plus(m: Metrics) = ActivityTally(calls + m.calls, errors + m.errors, agentIds + m.byAgent.filter { it.calls > 0 }.map { it.name })

    /** Agent names for the summary; the owner and revoked links are named by [owner] / [revoked]. */
    fun agentNames(agents: List<Agent>, owner: String, revoked: String): List<String> {
        val byId = agents.associate { it.id to it.name }
        return agentIds.map { if (it == "owner") owner else byId[it] ?: revoked }.distinct().sorted()
    }
}
