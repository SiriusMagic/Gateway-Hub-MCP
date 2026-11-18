package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.Agent
import com.zirakusa.gatewayhub.data.HubModule

/** One card per AI link: what it may do, until when, and pause / revoke. */
@Composable fun AgentsList(agents: List<Agent>, modules: List<HubModule>, setPaused: (String, Boolean) -> Unit, revoke: (String) -> Unit) {
    if (agents.isEmpty()) Text(stringResource(R.string.agents_empty), Modifier.padding(vertical = 24.dp), color = HubColors.Muted)
    var confirm by remember { mutableStateOf<Agent?>(null) }
    agents.forEach { agent -> AgentCard(agent, modules, setPaused) { confirm = agent } }
    confirm?.let { agent ->
        AlertDialog(onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.revoke)) },
            text = { Text(stringResource(R.string.revoke_confirm, agent.name)) },
            confirmButton = { TextButton({ revoke(agent.id); confirm = null }) { Text(stringResource(R.string.revoke), color = HubColors.Bad) } },
            dismissButton = { TextButton({ confirm = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable private fun AgentCard(agent: Agent, modules: List<HubModule>, setPaused: (String, Boolean) -> Unit, askRevoke: () -> Unit) {
    val never = stringResource(R.string.agent_never)
    HubCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(agent.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            when (agent.status) {
                "paused" -> Pill(stringResource(R.string.status_paused), HubColors.Gold)
                "expired" -> Pill(stringResource(R.string.status_expired), HubColors.Bad)
                else -> Pill(stringResource(R.string.status_active), HubColors.Good)
            }
        }
        val apps = if (agent.allApps) stringResource(R.string.agent_all_apps)
            else agent.modules.joinToString(" · ") { p -> modules.firstOrNull { it.prefix == p }?.title ?: p }
        Text(apps, Modifier.padding(top = 6.dp), color = HubColors.Text, fontSize = 14.sp)
        Text(stringResource(if (agent.readOnly) R.string.agent_read_only else R.string.agent_read_write), color = HubColors.Muted, fontSize = 13.sp)
        Text(stringResource(R.string.agent_expires, formatDate(agent.expiresAt, never)), color = HubColors.Muted, fontSize = 13.sp)
        Text(stringResource(R.string.agent_last_used, formatDate(agent.lastUsed, never)), color = HubColors.Muted, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(askRevoke) { Text(stringResource(R.string.revoke), color = HubColors.Bad) }
            if (agent.status != "expired") OutlinedButton({ setPaused(agent.id, !agent.paused) }, Modifier.padding(start = 8.dp)) {
                Text(stringResource(if (agent.paused) R.string.resume else R.string.pause))
            }
        }
    }
}
