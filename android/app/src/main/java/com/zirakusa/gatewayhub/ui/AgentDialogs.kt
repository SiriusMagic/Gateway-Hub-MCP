package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.CreatedAgent
import com.zirakusa.gatewayhub.data.HubModule

private val DURATIONS = listOf(7 to R.string.days_7, 30 to R.string.days_30, 90 to R.string.days_90,
    365 to R.string.days_365, 0 to R.string.days_never)

/** New link: name, apps, read-only and duration. */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun NewAgentDialog(modules: List<HubModule>, dismiss: () -> Unit, create: (String, List<String>, Boolean, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf(modules.map { it.prefix }.toSet()) }
    var readOnly by remember { mutableStateOf(false) }
    var days by remember { mutableIntStateOf(90) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(R.string.agent_new)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(name, { if (it.length <= 40) name = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.new_name)) }, placeholder = { Text(stringResource(R.string.new_name_hint)) })
            SectionTitle(stringResource(R.string.new_apps))
            modules.forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(m.prefix in apps, { on -> apps = if (on) apps + m.prefix else apps - m.prefix })
                    Text(m.title)
                }
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.agent_read_only))
                    Text(stringResource(R.string.new_read_only_hint), color = HubColors.Muted, fontSize = 12.sp)
                }
                Switch(readOnly, { readOnly = it })
            }
            SectionTitle(stringResource(R.string.new_duration))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DURATIONS.forEach { (d, label) -> FilterChip(days == d, { days = d }, { Text(stringResource(label)) }) }
            }
        } },
        confirmButton = { Button({ create(name.trim(), apps.toList(), readOnly, days) }, enabled = name.isNotBlank() && apps.isNotEmpty()) {
            Text(stringResource(R.string.create)) } },
        dismissButton = { TextButton(dismiss) { Text(stringResource(R.string.cancel)) } })
}

/** The new link, shown once. Copying marks it as sensitive (hidden from the clipboard preview). */
@Composable fun CreatedDialog(created: CreatedAgent, dismiss: () -> Unit, showToken: Boolean = true) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = {}, title = { Text(stringResource(R.string.created_title, created.agent.name)) },
        text = { Column {
            Text(stringResource(R.string.created_hint), color = HubColors.Muted, fontSize = 14.sp)
            Surface(Modifier.fillMaxWidth().padding(top = 10.dp), color = HubColors.Background, shape = MaterialTheme.shapes.medium) {
                Text(created.url, Modifier.padding(10.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            if (showToken) TextButton({ copySecret(context, "Gateway Hub", created.token); copied = true }) {
                Text(stringResource(R.string.copy_token), fontSize = 13.sp)
            }
            if (copied) Text(stringResource(R.string.copied), color = HubColors.Good, fontSize = 13.sp)
        } },
        confirmButton = { Button({ copySecret(context, "Gateway Hub", created.url); copied = true }) { Text(stringResource(R.string.copy_link)) } },
        dismissButton = { TextButton(dismiss) { Text(stringResource(R.string.done)) } })
}
