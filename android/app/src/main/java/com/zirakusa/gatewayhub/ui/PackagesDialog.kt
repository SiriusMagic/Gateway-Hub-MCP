package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.ACTS
import com.zirakusa.gatewayhub.data.Agent
import com.zirakusa.gatewayhub.data.IssuedPackage
import com.zirakusa.gatewayhub.data.LocalApp

/** What the Scanner needs for provider apps: issued packages, the hub's agents and the actions. */
class ProvidersUi(val packages: List<IssuedPackage> = emptyList(), val agents: List<Agent> = emptyList(), val actions: HubActions? = null)

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ProviderButtons(app: LocalApp, busy: Boolean, p: ProvidersUi) {
    var open by remember { mutableStateOf(false) }
    val vm = p.actions ?: return
    val count = p.packages.count { it.app == app.packageName }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton({ vm.testBridge(app.packageName) }, enabled = !busy) { Text(stringResource(R.string.bridge_test)) }
        TextButton({ vm.linkProvider(app.packageName) }, enabled = !busy) { Text(stringResource(R.string.provider_link)) }
        TextButton({ open = true; vm.refreshPackages(app.packageName) }, enabled = !busy) { Text(stringResource(R.string.packages_open, count)) }
    }
    if (open) PackagesDialog(app, p, busy) { open = false }
}

/** Packages issued to one app; a new one is offered here and, unless it only reads, approved in the app. */
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun PackagesDialog(app: LocalApp, p: ProvidersUi, busy: Boolean, close: () -> Unit) {
    val vm = p.actions ?: return
    var label by remember { mutableStateOf("") }
    var agent by remember { mutableStateOf("*") }
    var acts by remember { mutableStateOf(setOf("leer")) }
    var days by remember { mutableIntStateOf(30) }
    val actNames = ACTS.associateWith { actLabel(it) }
    AlertDialog(onDismissRequest = close, confirmButton = { TextButton(close) { Text(stringResource(R.string.done)) } },
        title = { Text(stringResource(R.string.packages_title, app.label)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            val mine = p.packages.filter { it.app == app.packageName }
            if (mine.isEmpty()) Text(stringResource(R.string.packages_none), color = HubColors.Muted, fontSize = 13.sp)
            mine.forEach { pkg ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(pkg.label.ifBlank { pkg.id }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${agentName(pkg.agent, p.agents)} · ${pkg.acts.joinToString(", ") { actNames[it] ?: it }} · ${formatDate(pkg.exp * 1000, "")}",
                            color = HubColors.Muted, fontSize = 12.sp)
                    }
                    Pill(stringResource(if (pkg.status == "active") R.string.package_status_active else R.string.package_status_pending),
                        if (pkg.status == "active") HubColors.Good else HubColors.Gold)
                    TextButton({ vm.revokePackage(app.packageName, pkg.id) }, enabled = !busy) { Text(stringResource(R.string.revoke), color = HubColors.Bad) }
                }
            }
            SectionTitle(stringResource(R.string.packages_new))
            OutlinedTextField(label, { label = it.take(80) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.packages_label)) })
            Text(stringResource(R.string.packages_agent), Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(agent == "*", { agent = "*" }, { Text(stringResource(R.string.agent_all_agents)) })
                p.agents.filter { it.status == "active" }.forEach { a -> FilterChip(agent == a.id, { agent = a.id }, { Text(a.name) }) }
            }
            Text(stringResource(R.string.packages_acts), Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ACTS.forEach { a -> FilterChip(a in acts, { acts = if (a in acts) acts - a else acts + a }, { Text(actNames.getValue(a)) }) }
            }
            FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(7, 30, 90).forEach { d -> FilterChip(days == d, { days = d }, { Text(pluralStringResource(R.plurals.package_days, d, d)) }) }
            }
            Text(stringResource(R.string.packages_hint), Modifier.padding(top = 6.dp), color = HubColors.Muted, fontSize = 12.sp)
            Button({ vm.offerPackage(app.packageName, agent, label.ifBlank { acts.joinToString(", ") { it } }, ACTS.filter { it in acts }, days) },
                Modifier.fillMaxWidth().padding(top = 8.dp), enabled = !busy && acts.isNotEmpty()) { Text(stringResource(R.string.packages_offer)) }
        } })
}

@Composable private fun actLabel(act: String) = stringResource(when (act) {
    "revisar" -> R.string.act_review; "escribir" -> R.string.act_write; "modificar" -> R.string.act_modify
    "revertir" -> R.string.act_revert; else -> R.string.act_read
})

@Composable private fun agentName(id: String, agents: List<Agent>) =
    if (id == "*") stringResource(R.string.agent_all_agents) else agents.firstOrNull { it.id == id }?.name ?: id
