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
 * What a non-technical owner sees: where things stand and the steps. On the shared hub: Stone links by itself,
 * then one link to paste in the AI and the code to type when it asks. On an own hub: a link per AI, with
 * sensible defaults. Everything else lives under "Advanced options".
 */
@Composable fun SimpleHome(state: UiState, hub: HubState, vm: HubActions, openAdvanced: () -> Unit) {
    val stone = state.providerApps.firstOrNull { it.prefix == "music_" }
    val stoneReady = hub.stoneLinked || (stone != null && state.packages.any { it.app == stone.packageName })
    val ais = hub.agents.filter { it.status != "expired" }
    val on = hub.environment == ENV_ACTIVE

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
        if (!on) BigButton(stringResource(R.string.simple_turn_on), !state.loading) { vm.setEnvironment(ENV_ACTIVE) }
    }

    Step(1, R.string.simple_step_stone, stoneReady)
    HubCard {
        when {
            stoneReady -> Text(stringResource(R.string.simple_stone_done), color = HubColors.Good)
            stone == null -> Text(stringResource(R.string.simple_stone_install), color = HubColors.Muted)
            else -> BigButton(stringResource(R.string.simple_stone_connect), !state.loading) { vm.linkProvider(stone.packageName) }
        }
    }

    if (hub.shared) SharedSteps(state, hub, vm) else {
        Step(2, R.string.simple_step_ai, ais.isNotEmpty())
        HubCard {
            Text(stringResource(R.string.simple_ai_hint), color = HubColors.Muted, fontSize = 14.sp)
            val apps = hub.modules.map { it.prefix }
            listOf("Claude", "ChatGPT").forEach { name ->
                BigButton(stringResource(R.string.simple_ai_add, name), !state.loading && apps.isNotEmpty()) { vm.createAgent(name, apps, false, 0) }
            }
            val other = stringResource(R.string.simple_ai_other_name)
            TextButton({ vm.createAgent(other, apps, false, 0) }, enabled = !state.loading && apps.isNotEmpty()) { Text(stringResource(R.string.simple_ai_other)) }
        }
    }
    ConnectedAis(ais, vm)

    TextButton(openAdvanced, Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.simple_advanced), color = HubColors.Muted) }
}

@Composable fun Step(number: Int, title: Int, done: Boolean) {
    Row(Modifier.padding(top = 16.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, Modifier.size(22.dp),
            tint = if (done) HubColors.Good else HubColors.Muted)
        Text("$number. " + stringResource(title), Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable fun BigButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().padding(top = 10.dp).height(52.dp), enabled = enabled) { Text(text, fontWeight = FontWeight.Bold) }
}
