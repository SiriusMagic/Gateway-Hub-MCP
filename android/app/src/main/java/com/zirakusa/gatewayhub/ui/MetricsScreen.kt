package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.CallStat
import com.zirakusa.gatewayhub.data.HubState
import com.zirakusa.gatewayhub.data.Metrics
import kotlin.math.roundToInt

/** Calls, errors and response time per agent, app and tool, measured by the hub. */
@Composable fun MetricsList(hub: HubState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Total(stringResource(R.string.metrics_day), hub.day, Modifier.weight(1f))
        Total(stringResource(R.string.metrics_week), hub.week, Modifier.weight(1f))
    }
    if (hub.week.calls == 0) { Text(stringResource(R.string.metrics_empty), Modifier.padding(vertical = 16.dp), color = HubColors.Muted); return }
    val owner = stringResource(R.string.metrics_owner)
    val revoked = stringResource(R.string.metrics_revoked)
    val names = hub.agents.associate { it.id to it.name }
    Breakdown(stringResource(R.string.metrics_by_agent), hub.week.byAgent.map {
        it.copy(name = if (it.name == "owner") owner else names[it.name] ?: "$revoked ${it.name}") })
    Breakdown(stringResource(R.string.metrics_by_app), hub.week.byApp.map { s ->
        s.copy(name = hub.modules.firstOrNull { it.prefix == s.name }?.title ?: s.name) })
    Breakdown(stringResource(R.string.metrics_by_tool), hub.week.byTool.take(10))
}

@Composable private fun Total(title: String, m: Metrics, modifier: Modifier) {
    HubCard(modifier) {
        Text(title, color = HubColors.Muted, fontSize = 13.sp)
        Text("${m.calls}", fontWeight = FontWeight.Bold, fontSize = 28.sp)
        Text("${stringResource(R.string.metrics_calls)} · ${m.errors} ${stringResource(R.string.metrics_errors)}",
            color = if (m.errors > 0) HubColors.Bad else HubColors.Muted, fontSize = 13.sp)
    }
}

@Composable private fun Breakdown(title: String, rows: List<CallStat>) {
    if (rows.isEmpty()) return
    HubCard {
        Text(title, fontWeight = FontWeight.Bold)
        val top = rows.maxOf { it.calls }.coerceAtLeast(1)
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(r.name, Modifier.weight(1f), fontSize = 14.sp, maxLines = 1)
                Text(stringResource(R.string.metrics_row, r.calls, r.errors, r.avgMs.roundToInt()), color = HubColors.Muted, fontSize = 12.sp)
            }
            LinearProgressIndicator({ r.calls / top.toFloat() }, Modifier.fillMaxWidth().padding(top = 4.dp),
                color = if (r.errors * 10 > r.calls) HubColors.Bad else HubColors.Gold, trackColor = HubColors.Line)
        }
    }
}
