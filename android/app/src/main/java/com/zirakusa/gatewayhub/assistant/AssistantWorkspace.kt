package com.zirakusa.gatewayhub.assistant

import androidx.compose.material.icons.filled.Psychology
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.assistant.local.HubConnectionSettings
import com.zirakusa.gatewayhub.assistant.local.LocalModelSettings
import com.zirakusa.gatewayhub.ui.HubCard
import com.zirakusa.gatewayhub.ui.HubColors
import kotlinx.coroutines.launch

internal enum class AssistantPage(val label: Int, val icon: ImageVector) {
    CHAT(R.string.assistant_chat_tab, Icons.AutoMirrored.Filled.Chat),
    MODELS(R.string.local_models, Icons.Default.Apps),
    MCP(R.string.phone_mcp_title, Icons.Default.Hub),
    /** «RS», Regla de Sentimiento: its own page, apart from MCP. */
    RS(R.string.rs_menu, Icons.Default.Psychology),
    SETTINGS(R.string.lucia_settings, Icons.Default.Settings),
    ADVANCED(R.string.assistant_advanced, Icons.Default.Settings)
}

@Composable internal fun AssistantWorkspace(
    state: AssistantState,
    actions: AssistantActions,
    chat: @Composable (() -> Unit) -> Unit
) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openMenu: () -> Unit = { scope.launch { drawer.open() } }
    BackHandler(drawer.isOpen || state.page != AssistantPage.CHAT) {
        when {
            drawer.isOpen -> scope.launch { drawer.close() }
            state.page == AssistantPage.ADVANCED -> state.page = AssistantPage.SETTINGS
            else -> state.page = AssistantPage.CHAT
        }
    }
    ModalNavigationDrawer(drawerState = drawer, gesturesEnabled = drawer.isOpen, drawerContent = {
        ModalDrawerSheet(drawerContainerColor = HubColors.Card) {
            Text(state.name.shown, Modifier.padding(20.dp), color = HubColors.Gold, fontSize = 24.sp)
            AssistantPage.entries.filter { it != AssistantPage.ADVANCED }.forEach { page ->
                NavigationDrawerItem(
                    label = { Text(stringResource(page.label)) },
                    selected = state.page == page || page == AssistantPage.SETTINGS && state.page == AssistantPage.ADVANCED,
                    icon = { Icon(page.icon, null) },
                    onClick = {
                        state.page = page
                        scope.launch { drawer.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }) {
        if (state.page == AssistantPage.CHAT) chat(openMenu)
        else Column(Modifier.fillMaxSize().background(HubColors.Background).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(openMenu) { Icon(Icons.Default.Menu, stringResource(R.string.assistant_menu), tint = HubColors.Text) }
                Text(stringResource(state.page.label), color = HubColors.Text, fontSize = 22.sp)
            }
            when (state.page) {
                AssistantPage.MODELS -> ModelPage(state)
                AssistantPage.MCP -> PhoneMcpScreen(state.local, actions.manageHub) { state.page = AssistantPage.RS }
                AssistantPage.RS -> RsScreen(state.interpreter)
                AssistantPage.SETTINGS -> LuciaSettings(state, actions)
                AssistantPage.ADVANCED -> Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    state.local?.let { HubCard { HubConnectionSettings(it) } }
                }
                AssistantPage.CHAT -> Unit
            }
        }
    }
}

@Composable private fun ModelPage(state: AssistantState) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        state.local?.let { HubCard { LocalModelSettings(it) } }
    }
}
