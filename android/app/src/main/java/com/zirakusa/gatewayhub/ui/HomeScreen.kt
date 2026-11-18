package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R

/** Everything the owner does once connected; [vm] actions are passed down as callbacks. */
enum class HubTab(val label: Int, val icon: ImageVector) {
    AGENTS(R.string.tab_agents, Icons.Default.Key), SCANNER(R.string.tab_scanner, Icons.Default.Radar),
    APPS(R.string.tab_apps, Icons.Default.Apps), METRICS(R.string.tab_metrics, Icons.Default.Insights)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HomeScreen(state: UiState, vm: HubActions, initialTab: HubTab = HubTab.AGENTS) {
    var tab by remember { mutableStateOf(initialTab) }
    var creating by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.notice, state.error) {
        (state.error ?: state.notice)?.let { snackbar.showSnackbar(it); vm.dismissMessage() }
    }
    Scaffold(
        topBar = { TopAppBar(
            title = { Column {
                Text(stringResource(R.string.app_name))
                Text(state.hubUrl.removePrefix("https://"), color = HubColors.Muted, fontSize = 12.sp, maxLines = 1)
            } },
            actions = {
                IconButton(vm::refresh, enabled = !state.loading) { Icon(Icons.Default.Refresh, stringResource(R.string.refresh)) }
                IconButton({ leaving = true }) { Icon(Icons.AutoMirrored.Filled.Logout, stringResource(R.string.logout)) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = HubColors.Background)) },
        bottomBar = { NavigationBar(containerColor = HubColors.Card) {
            HubTab.entries.forEach { t -> NavigationBarItem(tab == t, { tab = t }, { Icon(t.icon, null) }, label = { Text(stringResource(t.label)) }) }
        } },
        floatingActionButton = { if (tab == HubTab.AGENTS && state.hub != null) ExtendedFloatingActionButton(onClick = { creating = true }, containerColor = HubColors.Gold, contentColor = HubColors.Background,
            icon = { Icon(Icons.Default.Add, null) }, text = { Text(stringResource(R.string.agent_new)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = HubColors.Background,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = HubColors.Gold)
            val hub = state.hub
            if (hub == null) { if (!state.loading) Text(state.error ?: "", Modifier.padding(16.dp), color = HubColors.Bad); return@Column }
            when (tab) {
                HubTab.AGENTS -> AgentsList(hub.agents, hub.modules, vm::setPaused, vm::revoke)
                HubTab.SCANNER -> ScannerList(state.scan, state.loading, vm::scan)
                HubTab.APPS -> AppsList(hub, state.loading, vm::linkStone, vm::unlinkStone)
                HubTab.METRICS -> MetricsList(hub)
            }
            Text(stringResource(R.string.hub_version, hub.version), Modifier.padding(vertical = 16.dp), color = HubColors.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(72.dp))
        }
    }
    if (creating) state.hub?.let { hub -> NewAgentDialog(hub.modules, { creating = false }) { name, apps, ro, days ->
        creating = false; vm.createAgent(name, apps, ro, days) } }
    state.created?.let { CreatedDialog(it, vm::dismissCreated) }
    if (leaving) AlertDialog(onDismissRequest = { leaving = false }, title = { Text(stringResource(R.string.logout)) },
        text = { Text(stringResource(R.string.logout_confirm)) },
        confirmButton = { TextButton({ leaving = false; vm.logout() }) { Text(stringResource(R.string.logout), color = HubColors.Bad) } },
        dismissButton = { TextButton({ leaving = false }) { Text(stringResource(R.string.cancel)) } })
}

/** The actions the screens may trigger (the ViewModel in the app, a fake in tests). */
interface HubActions {
    fun refresh(); fun logout(); fun dismissMessage(); fun dismissCreated()
    fun setPaused(id: String, paused: Boolean); fun revoke(id: String)
    fun createAgent(name: String, apps: List<String>, readOnly: Boolean, days: Int)
    fun linkStone(link: String); fun unlinkStone()
    fun scan()
}
