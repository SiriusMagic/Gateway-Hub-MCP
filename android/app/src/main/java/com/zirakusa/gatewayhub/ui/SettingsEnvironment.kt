package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.AppSettings
import com.zirakusa.gatewayhub.data.ENV_ACTIVE
import com.zirakusa.gatewayhub.data.ENV_DISCONNECTED
import com.zirakusa.gatewayhub.data.ENV_PAUSED
import com.zirakusa.gatewayhub.data.McpMode

/**
 * The MCP environment: three actions (connect, pause, disconnect) that the hub enforces, and two
 * modes (safe / background) plus the activity notifications.
 */
@Composable fun EnvironmentSection(environment: String, settings: AppSettings, loading: Boolean, vm: HubActions, askNotifications: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    SectionTitle(stringResource(R.string.env_title))
    HubCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.env_state), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            when (environment) {
                ENV_PAUSED -> Pill(stringResource(R.string.env_paused), HubColors.Gold)
                ENV_DISCONNECTED -> Pill(stringResource(R.string.env_disconnected), HubColors.Bad)
                else -> Pill(stringResource(R.string.env_active), HubColors.Good)
            }
        }
        Text(stringResource(when (environment) { ENV_PAUSED -> R.string.env_paused_hint; ENV_DISCONNECTED -> R.string.env_disconnected_hint
            else -> if (settings.mode == McpMode.BACKGROUND) R.string.env_active_background else R.string.env_active_safe }),
            Modifier.padding(top = 6.dp), color = HubColors.Muted, fontSize = 13.sp)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (environment != ENV_ACTIVE) Button({ vm.setEnvironment(ENV_ACTIVE) }, enabled = !loading) { Text(stringResource(R.string.env_connect)) }
            if (environment == ENV_ACTIVE) OutlinedButton({ vm.setEnvironment(ENV_PAUSED) }, enabled = !loading) { Text(stringResource(R.string.env_pause)) }
        }
        if (environment != ENV_DISCONNECTED) TextButton({ confirm = true }, enabled = !loading) {
            Text(stringResource(R.string.env_disconnect), color = HubColors.Bad)
        }
    }
    HubCard {
        Text(stringResource(R.string.mode_title), fontWeight = FontWeight.SemiBold)
        ModeOption(settings.mode == McpMode.SAFE, R.string.mode_safe, R.string.mode_safe_hint) { vm.updateSettings { it.copy(mode = McpMode.SAFE) } }
        ModeOption(settings.mode == McpMode.BACKGROUND, R.string.mode_background, R.string.mode_background_hint) {
            vm.updateSettings { it.copy(mode = McpMode.BACKGROUND) }; askNotifications()
        }
        Text(stringResource(R.string.mode_android_note), Modifier.padding(top = 4.dp), color = HubColors.Muted, fontSize = 12.sp)
    }
    HubCard {
        SwitchRow(stringResource(R.string.notif_setting), stringResource(R.string.notif_setting_hint), settings.activityNotifications) { on ->
            vm.updateSettings { it.copy(activityNotifications = on) }; if (on) askNotifications()
        }
        if (settings.activityNotifications) {
            Text(stringResource(R.string.notif_every), Modifier.padding(top = 8.dp), color = HubColors.Muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppSettings.SUMMARY_MINUTES.forEach { m ->
                    FilterChip(settings.summaryMinutes == m, { vm.updateSettings { it.copy(summaryMinutes = m) } },
                        { Text(if (m < 60) stringResource(R.string.minutes, m) else stringResource(R.string.hours, m / 60)) })
                }
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.env_disconnect)) },
        text = { Text(stringResource(R.string.env_disconnect_confirm)) },
        confirmButton = { TextButton({ confirm = false; vm.setEnvironment(ENV_DISCONNECTED) }) { Text(stringResource(R.string.env_disconnect), color = HubColors.Bad) } },
        dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable private fun ModeOption(selected: Boolean, title: Int, hint: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected, onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        RadioButton(selected, onClick)
        Column(Modifier.padding(start = 4.dp, top = 10.dp)) {
            Text(stringResource(title), fontWeight = FontWeight.SemiBold)
            Text(stringResource(hint), color = HubColors.Muted, fontSize = 13.sp)
        }
    }
}

@Composable fun SwitchRow(title: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            hint?.let { Text(it, color = HubColors.Muted, fontSize = 13.sp) }
        }
        Switch(checked, onChange)
    }
}
