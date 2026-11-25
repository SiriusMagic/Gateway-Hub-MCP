package com.zirakusa.gatewayhub.assistant

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.assistant.local.LocalAssistant
import com.zirakusa.gatewayhub.assistant.local.PhoneMcpApp
import com.zirakusa.gatewayhub.assistant.local.PhoneMcpStatus
import com.zirakusa.gatewayhub.data.PackageScanner
import com.zirakusa.gatewayhub.ui.HubCard
import com.zirakusa.gatewayhub.ui.HubColors

@Composable internal fun PhoneMcpScreen(local: LocalAssistant?, connectExternal: () -> Unit) {
    var apps by remember(local) { mutableStateOf<List<PhoneMcpApp>?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    LaunchedEffect(local, refresh) { apps = local?.phoneMcp?.inventory().orEmpty() }
    val inventory = apps
    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.phone_mcp_apps), Modifier.weight(1f), color = HubColors.Text, fontSize = 18.sp)
                TextButton({ refresh++ }) { Text(stringResource(R.string.refresh)) }
            }
        }
        // Lucía's own «reglas de sentimientos» (no app needed) and how many she has in all.
        item { OwnRulesCard() }
        when {
            inventory == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            inventory.isEmpty() -> item { Text(stringResource(R.string.scanner_none), color = HubColors.Muted) }
            else -> items(inventory, key = { it.app.packageName }) { PhoneMcpCard(it) }
        }
        item {
            Button(connectExternal, Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text(stringResource(R.string.phone_mcp_connect_external))
            }
        }
    }
}

@Composable private fun PhoneMcpCard(row: PhoneMcpApp) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var rulesShown by rememberSaveable { mutableStateOf(false) }
    var openFailed by remember { mutableStateOf(false) }
    val family = Catalog.byPackage(row.app.packageName)
    val known = Skills.every.filter { it.app == family?.id }.map { it.label }.distinct()
    HubCard {
        Text(row.app.label, color = HubColors.Text, fontSize = 18.sp)
        row.app.version.takeIf { it.isNotBlank() }?.let { Text(it, color = HubColors.Muted) }
        Text(if (row.status == PhoneMcpStatus.LISTED) pluralStringResource(R.plurals.phone_mcp_listed, row.tools.size, row.tools.size) else stringResource(row.status.label),
            color = if (row.status == PhoneMcpStatus.READY || row.status == PhoneMcpStatus.LISTED) HubColors.Good else HubColors.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({
                openFailed = try {
                    context.startActivity(Intent(PackageScanner.ACTION).addCategory(Intent.CATEGORY_DEFAULT)
                        .setPackage(row.app.packageName))
                    false
                } catch (_: ActivityNotFoundException) { true } catch (_: SecurityException) { true }
            }) { Text(stringResource(R.string.scan_open)) }
            if (known.isNotEmpty() || row.tools.isNotEmpty()) {
                TextButton({ expanded = !expanded; if (expanded) rulesShown = false }) { Text(stringResource(R.string.phone_mcp_tools)) }
            }
            val rules = Examples.rulesFor(family?.id ?: "-")
            if (rules.isNotEmpty()) TextButton({ rulesShown = !rulesShown; if (rulesShown) expanded = false }) { Text(stringResource(R.string.phone_mcp_rules)) }
        }
        if (rulesShown) RulesList(Examples.rulesFor(family?.id ?: "-"))
        if (openFailed) Text(stringResource(R.string.phone_mcp_open_failed), color = HubColors.Bad)
        if (expanded) {
            if (row.tools.isNotEmpty()) {
                row.tools.forEach { tool ->
                    when (tool.name) {
                        "library_search" -> if (row.status == PhoneMcpStatus.READY) Text(stringResource(R.string.phone_mcp_search), color = HubColors.Text) else ToolRow(tool)
                        "player_state" -> if (row.status == PhoneMcpStatus.READY) Text(stringResource(R.string.phone_mcp_player), color = HubColors.Text) else ToolRow(tool)
                        else -> ToolRow(tool)
                    }
                }
            } else {
                Text(stringResource(R.string.phone_mcp_known), color = HubColors.Muted)
                known.forEach { Text(it, color = HubColors.Text) }
            }
        }
    }
}

/** One tool as the app lists it: its title (or name) and the first line of what it does. */
@Composable private fun ToolRow(tool: com.zirakusa.gatewayhub.assistant.local.McpTool) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Text(tool.title.ifBlank { tool.name }, color = HubColors.Text)
        tool.description.lineSequence().firstOrNull()?.take(120)?.takeIf { it.isNotBlank() }?.let { Text(it, color = HubColors.Muted, fontSize = 13.sp) }
    }
}

/** Lucía's own rules (she does them herself, no app tool), folded, with the total of rules she has. */
@Composable private fun OwnRulesCard() {
    var open by rememberSaveable { mutableStateOf(false) }
    val own = Examples.rulesFor(null)
    HubCard {
        Text(stringResource(R.string.phone_mcp_rules), color = HubColors.Text, fontSize = 18.sp)
        Text(stringResource(R.string.phone_mcp_rules_total, Examples.rules.size, own.size), color = HubColors.Muted, fontSize = 13.sp)
        TextButton({ open = !open }) { Text(stringResource(if (open) R.string.phone_mcp_rules_hide else R.string.phone_mcp_rules_own)) }
        if (open) RulesList(own)
    }
}

/** Each rule: the phrase Lucía understands, then what it does and whether she asks first. */
@Composable private fun RulesList(rules: List<Examples.Rule>) {
    Column(Modifier.padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rules.forEach { r ->
            Column {
                Text("«${r.phrase}»", color = HubColors.Text)
                val what = r.label ?: r.tool?.replace('_', ' ')
                val asks = if (r.asks) stringResource(R.string.phone_mcp_rule_asks) else null
                listOfNotNull(what, asks).joinToString(" · ").takeIf { it.isNotBlank() }?.let { Text(it, color = HubColors.Muted, fontSize = 13.sp) }
            }
        }
    }
}
