package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.ENV_ACTIVE
import com.zirakusa.gatewayhub.data.HubState

/**
 * What a non-technical owner sees: where things stand and the two steps (connect Stone, connect an AI), with
 * sensible defaults (all apps, read and write, no expiry). Everything else lives under "Advanced options".
 */
@Composable fun SimpleHome(state: UiState, hub: HubState, vm: HubActions, openAdvanced: () -> Unit) {
    val stone = state.providerApps.firstOrNull { it.prefix == "music_" }
    val stoneReady = hub.stoneLinked || (stone != null && state.packages.any { it.app == stone.packageName })
    val ais = hub.agents.filter { it.status != "expired" }
    val on = hub.environment == ENV_ACTIVE
    var removing by remember { mutableStateOf<String?>(null) }

    HubCard {
        val (icon, title, color) = when {
            !on -> Triple(Icons.Default.PauseCircle, R.string.simple_off, HubColors.Gold)
            stoneReady && ais.isNotEmpty() -> Triple(Icons.Default.CheckCircle, R.string.simple_ready, HubColors.Good)
            else -> Triple(Icons.Default.RadioButtonUnchecked, R.string.simple_missing, HubColors.Gold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(40.dp), tint = color)
            Text(stringResource(title), Modifier.padding(start = 12.dp), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        if (!on) Button({ vm.setEnvironment(ENV_ACTIVE) }, Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp), enabled = !state.loading) {
            Text(stringResource(R.string.simple_turn_on), fontWeight = FontWeight.Bold) }
    }

    Step(1, R.string.simple_step_stone, stoneReady)
    HubCard {
        when {
            stoneReady -> Text(stringResource(R.string.simple_stone_done), color = HubColors.Good)
            stone == null -> Text(stringResource(R.string.simple_stone_install), color = HubColors.Muted)
            else -> BigButton(stringResource(R.string.simple_stone_connect), !state.loading) { vm.linkProvider(stone.packageName) }
        }
    }

    Step(2, R.string.simple_step_ai, ais.isNotEmpty())
    HubCard {
        Text(stringResource(R.string.simple_ai_hint), color = HubColors.Muted, fontSize = 14.sp)
        val apps = hub.modules.map { it.prefix }
        listOf("Claude", "ChatGPT").forEach { name ->
            BigButton(stringResource(R.string.simple_ai_add, name), !state.loading && apps.isNotEmpty()) { vm.createAgent(name, apps, false, 0) }
        }
        val other = stringResource(R.string.simple_ai_other_name)
        TextButton({ vm.createAgent(other, apps, false, 0) }, enabled = !state.loading && apps.isNotEmpty()) { Text(stringResource(R.string.simple_ai_other)) }
        ais.forEach { agent ->
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp), tint = if (agent.paused) HubColors.Muted else HubColors.Good)
                Text(agent.name + if (agent.paused) " · " + stringResource(R.string.status_paused) else "", Modifier.weight(1f).padding(start = 8.dp))
                TextButton({ removing = agent.id }) { Text(stringResource(R.string.simple_ai_remove), color = HubColors.Bad) }
            }
        }
    }

    TextButton(openAdvanced, Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.simple_advanced), color = HubColors.Muted) }

    removing?.let { id -> AlertDialog(onDismissRequest = { removing = null },
        title = { Text(stringResource(R.string.simple_ai_remove)) },
        text = { Text(stringResource(R.string.revoke_confirm, ais.firstOrNull { it.id == id }?.name.orEmpty())) },
        confirmButton = { TextButton({ removing = null; vm.revoke(id) }) { Text(stringResource(R.string.simple_ai_remove), color = HubColors.Bad) } },
        dismissButton = { TextButton({ removing = null }) { Text(stringResource(R.string.cancel)) } }) }
}

@Composable private fun Step(number: Int, title: Int, done: Boolean) {
    Row(Modifier.padding(top = 16.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, Modifier.size(22.dp),
            tint = if (done) HubColors.Good else HubColors.Muted)
        Text("$number. " + stringResource(title), Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable private fun BigButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().padding(top = 10.dp).height(52.dp), enabled = enabled) { Text(text, fontWeight = FontWeight.Bold) }
}
