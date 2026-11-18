package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.channel.ChannelClient
import com.zirakusa.gatewayhub.channel.ChannelRunner
import com.zirakusa.gatewayhub.data.DeviceInfo

/** Settings › Phones (owner's phone): who is on the channel, a one-use code for another phone, revoke. */
@Composable fun PhonesSection(devices: List<DeviceInfo>, pairing: Pair<String, Long>?, hubUrl: String, loading: Boolean, vm: HubActions) {
    SectionTitle(stringResource(R.string.phones_title))
    HubCard {
        if (devices.isEmpty()) Text(stringResource(R.string.phones_none), color = HubColors.Muted, fontSize = 13.sp)
        devices.forEach { d ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(d.name, fontWeight = FontWeight.SemiBold)
                    if (d.providers.isNotEmpty()) Text(d.providers.joinToString(" ") { "$it*" }, color = HubColors.Muted, fontSize = 12.sp)
                }
                Pill(stringResource(if (d.online) R.string.phones_online else R.string.phones_offline), if (d.online) HubColors.Good else HubColors.Muted)
                TextButton({ vm.revokeDevice(d.id) }, enabled = !loading) { Text(stringResource(R.string.revoke), color = HubColors.Bad) }
            }
        }
        OutlinedButton(vm::createPairing, Modifier.padding(top = 6.dp), enabled = !loading) { Text(stringResource(R.string.phones_add)) }
        pairing?.takeIf { it.second > System.currentTimeMillis() }?.let { (code, until) ->
            Text(code.chunked(5).joinToString("-"), Modifier.padding(top = 8.dp), fontFamily = FontFamily.Monospace, fontSize = 26.sp,
                fontWeight = FontWeight.Bold, color = HubColors.Gold)
            Text(stringResource(R.string.phones_code_hint, hubUrl.removePrefix("https://"), formatDate(until, "")), color = HubColors.Muted, fontSize = 12.sp)
        }
    }
}

/** Login: join a hub as a satellite phone, with the address and a one-use code from the owner's phone. */
@Composable fun JoinHubCard(loading: Boolean, join: (String, String) -> Unit) {
    var url by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    HubCard(Modifier.padding(top = 16.dp)) {
        Text(stringResource(R.string.join_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(stringResource(R.string.join_hint), Modifier.padding(vertical = 6.dp), color = HubColors.Muted, fontSize = 13.sp)
        OutlinedTextField(url, { url = it.trim() }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.join_url)) })
        OutlinedTextField(code, { code = it.take(12) }, Modifier.fillMaxWidth().padding(top = 6.dp), singleLine = true,
            label = { Text(stringResource(R.string.join_code)) }, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
        Button({ join(url, code); code = "" }, Modifier.fillMaxWidth().padding(top = 10.dp), enabled = !loading && url.isNotBlank() && code.length >= 10) {
            Text(stringResource(R.string.join_button), fontWeight = FontWeight.Bold)
        }
    }
}

/** A satellite phone: no owner token; it serves its own apps through the hub's channel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SatelliteScreen(state: UiState, vm: HubActions) {
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var leaving by remember { mutableStateOf(false) }
    LaunchedEffect(state.notice, state.error) { (state.error ?: state.notice)?.let { snackbar.showSnackbar(it.resolve(context)); vm.dismissMessage() } }
    val channel by ChannelRunner.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Column {
        Text(stringResource(R.string.satellite_title)); Text(state.hubUrl.removePrefix("https://"), color = HubColors.Muted, fontSize = 12.sp, maxLines = 1)
    } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = HubColors.Background)) },
        snackbarHost = { SnackbarHost(snackbar) }, containerColor = HubColors.Background) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = HubColors.Gold)
            HubCard {
                Text(stringResource(if (channel == ChannelClient.State.ONLINE) R.string.channel_online else R.string.channel_offline), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.satellite_hint), Modifier.padding(top = 4.dp), color = HubColors.Muted, fontSize = 13.sp)
            }
            if (state.providerApps.isEmpty()) Text(stringResource(R.string.satellite_apps_none), Modifier.padding(16.dp), color = HubColors.Muted)
            state.providerApps.forEach { app ->
                HubCard {
                    Text(app.label, fontWeight = FontWeight.Bold); Text("${app.packageName} · ${app.prefix ?: ""}", color = HubColors.Muted, fontSize = 12.sp)
                    ProviderButtons(app, state.loading, ProvidersUi(state.packages, emptyList(), vm))
                }
            }
            TextButton({ leaving = true }, Modifier.padding(vertical = 12.dp)) { Text(stringResource(R.string.satellite_leave), color = HubColors.Bad) }
        }
    }
    if (leaving) AlertDialog(onDismissRequest = { leaving = false }, title = { Text(stringResource(R.string.satellite_leave)) },
        text = { Text(stringResource(R.string.satellite_leave_confirm)) },
        confirmButton = { TextButton({ leaving = false; ChannelRunner.stop(); vm.leaveHub() }) { Text(stringResource(R.string.satellite_leave), color = HubColors.Bad) } },
        dismissButton = { TextButton({ leaving = false }) { Text(stringResource(R.string.cancel)) } })
}
