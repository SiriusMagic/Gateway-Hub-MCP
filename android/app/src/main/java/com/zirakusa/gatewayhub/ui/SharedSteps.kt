package com.zirakusa.gatewayhub.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.Agent
import com.zirakusa.gatewayhub.data.HubState
import kotlinx.coroutines.delay

/**
 * Shared hub, steps 2 and 3: the public link (the same for everybody, nothing secret in it) and the code the
 * person types when the AI asks. The code changes every 10 minutes and works once.
 */
@Composable fun SharedSteps(state: UiState, hub: HubState, vm: HubActions) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    Step(2, R.string.shared_step_link, copied || hub.agents.isNotEmpty())
    HubCard {
        Surface(Modifier.fillMaxWidth(), color = HubColors.Background, shape = MaterialTheme.shapes.medium) {
            Text(hub.publicUrl, Modifier.padding(12.dp), fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        }
        BigButton(stringResource(if (copied) R.string.copied else R.string.copy_link), true) {
            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Gateway Hub", hub.publicUrl))
            copied = true
        }
        Text(stringResource(R.string.shared_link_hint), Modifier.padding(top = 10.dp), color = HubColors.Muted, fontSize = 14.sp)
    }

    Step(3, R.string.shared_step_code, hub.agents.isNotEmpty())
    HubCard {
        val code = state.connect
        // A new code as soon as this one expires, while the screen is open.
        LaunchedEffect(code?.second) {
            if (code == null) return@LaunchedEffect           // the first one comes with the setup
            delay((code.second - System.currentTimeMillis()).coerceAtLeast(0))
            vm.newConnectCode()
        }
        Text(code?.first?.let { it.take(4) + " " + it.drop(4) } ?: "···· ····", Modifier.fillMaxWidth(),
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 40.sp, textAlign = TextAlign.Center,
            color = HubColors.Gold)
        Text(stringResource(R.string.shared_code_hint), Modifier.fillMaxWidth().padding(top = 6.dp), color = HubColors.Muted,
            fontSize = 14.sp, textAlign = TextAlign.Center)
        TextButton(vm::newConnectCode, Modifier.align(Alignment.CenterHorizontally), enabled = !state.loading) {
            Text(stringResource(R.string.shared_new_code)) }
    }
}

/** The AIs that can use the apps now, each with "Remove". */
@Composable fun ConnectedAis(ais: List<Agent>, vm: HubActions) {
    if (ais.isEmpty()) return
    var removing by remember { mutableStateOf<String?>(null) }
    SectionTitle(stringResource(R.string.simple_connected))
    HubCard {
        ais.forEach { agent ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp), tint = if (agent.paused) HubColors.Muted else HubColors.Good)
                Text(agent.name + if (agent.paused) " · " + stringResource(R.string.status_paused) else "", Modifier.weight(1f).padding(start = 8.dp))
                TextButton({ removing = agent.id }) { Text(stringResource(R.string.simple_ai_remove), color = HubColors.Bad) }
            }
        }
    }
    removing?.let { id -> AlertDialog(onDismissRequest = { removing = null },
        title = { Text(stringResource(R.string.simple_ai_remove)) },
        text = { Text(stringResource(R.string.revoke_confirm, ais.firstOrNull { it.id == id }?.name.orEmpty())) },
        confirmButton = { TextButton({ removing = null; vm.revoke(id) }) { Text(stringResource(R.string.simple_ai_remove), color = HubColors.Bad) } },
        dismissButton = { TextButton({ removing = null }) { Text(stringResource(R.string.cancel)) } }) }
}
