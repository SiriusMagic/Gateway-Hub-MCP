package com.zirakusa.gatewayhub.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.HubException
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.ui.HubCard
import com.zirakusa.gatewayhub.ui.HubColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable internal fun McpDiagnosis(state: AssistantState, connectHub: () -> Unit) {
    val diagnostics = state.diagnostics ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var local by remember { mutableStateOf<McpOrder?>(null) }
    var hub by remember { mutableStateOf<McpOrder?>(null) }
    var testing by remember { mutableStateOf(false) }
    var chooser by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf(emptyList<LocalApp>()) }
    var selected by remember { mutableStateOf<LocalApp?>(null) }
    LaunchedEffect(state.local) {
        apps = state.local?.phoneMcp?.inventory().orEmpty().map { it.app }
            .filter { it.prefix != null && it.prefix != "music_" && Catalog.byPackage(it.packageName) != null }
    }
    val app = selected?.let { Catalog.byPackage(it.packageName)?.copy(packages = listOf(it.packageName)) } ?: Catalog.byId("stone")
    val prefix = selected?.prefix ?: "music_"
    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            HubCard {
                Text(stringResource(R.string.mcp_probe_title))
                TextButton({ chooser = true }, enabled = !testing) { Text(app.name) }
                DropdownMenu(chooser, onDismissRequest = { chooser = false }) {
                    DropdownMenuItem(text = { Text("Stone") }, onClick = {
                        selected = null; chooser = false; local = null; hub = null
                    })
                    apps.forEach { target ->
                        DropdownMenuItem(text = { Text(target.label) }, onClick = {
                            selected = target; chooser = false; local = null; hub = null
                        })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({
                        testing = true
                        scope.launch { try { local = diagnostics.probe(false, app, prefix) } finally { testing = false } }
                    }, enabled = !testing) { Text(stringResource(R.string.mcp_probe_local)) }
                    OutlinedButton({
                        testing = true
                        scope.launch { try { hub = diagnostics.probe(true, app, prefix) } finally { testing = false } }
                    }, enabled = !testing) { Text(stringResource(R.string.mcp_probe_hub)) }
                }
                local?.let { TraceLine(it, stringResource(R.string.mcp_probe_local)) }
                hub?.let { TraceLine(it, stringResource(R.string.mcp_probe_hub)) }
            }
        }
        state.lastTranscript?.let { transcript ->
            item {
                HubCard {
                    Text(stringResource(R.string.mcp_transcript))
                    Text(transcript.ifBlank { stringResource(R.string.dictation_empty) }, color = HubColors.Muted)
                    state.dictationComparison?.let { DictationResult(it) }
                }
            }
        }
        state.lastMcpTurn?.let { turn ->
            item {
                HubCard {
                    Text(stringResource(R.string.mcp_intent))
                    Text(turn.heard)
                    turn.mcp?.let {
                        Text(it.tool, color = HubColors.Muted)
                        Text(JSONObject(it.arguments).toString(2), color = HubColors.Muted)
                        TraceLine(it, stringResource(R.string.mcp_result))
                        it.response?.let { response -> Text(response, color = HubColors.Muted) }
                    }
                }
            }
        }
        diagnostics.lastFailure?.let { failure ->
            item {
                HubCard {
                    TraceLine(failure, stringResource(R.string.mcp_last_failure))
                    when (failure.code) {
                        "NO_HUB", "STONE_NOT_LINKED", "APP_NOT_LINKED", "STONE_TOKEN", "UNAUTHORIZED", "NETWORK" ->
                            TextButton(connectHub) { Text(stringResource(R.string.mcp_connect_hub)) }
                        else -> {
                            val target = if (failure.app == "music_") Catalog.byId("stone")
                                else apps.firstOrNull { it.prefix == failure.app }?.let { Catalog.byPackage(it.packageName) }
                            val launch = target?.packages?.firstNotNullOfOrNull(context.packageManager::getLaunchIntentForPackage)
                            if (launch != null && target != null) TextButton({ context.startActivity(launch) }) {
                                Text(stringResource(R.string.mcp_open_app, target.name))
                            }
                        }
                    }
                    var comment by remember(failure.id) { mutableStateOf(diagnostics.comment(failure.id)) }
                    OutlinedTextField(comment, { comment = it.take(300) }, modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.mcp_comment)) }, maxLines = 3)
                    TextButton({ diagnostics.saveComment(failure.id, comment) }) { Text(stringResource(R.string.mcp_comment_save)) }
                }
            }
        }
        item {
            HubCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.mcp_auto_reports))
                        Text(stringResource(R.string.mcp_auto_details), color = HubColors.Muted)
                    }
                    Switch(diagnostics.sending, diagnostics::allow)
                }
                if (diagnostics.pending > 0) Text(stringResource(R.string.mcp_pending_reports, diagnostics.pending))
            }
        }
        item {
            if (diagnostics.recent.isNotEmpty()) HubCard {
                Text(stringResource(R.string.mcp_recent))
                diagnostics.recent.take(10).forEach { TraceLine(it, it.tool) }
            }
        }
    }
}

@Composable private fun TraceLine(order: McpOrder, label: String) {
    Text(label)
    Text(stringResource(R.string.mcp_trace_line, order.app, order.stage, order.code, order.millis, order.id), color = HubColors.Muted)
    if (order.code != "OK") Text(
        if (order.app == "music_") StoneOrder.advice(order.code) else stringResource(when (order.code) {
            "APP_MISSING", "NOT_READY", "PROVIDER_UNAVAILABLE", "STONE_OFFLINE", "STONE_UNREACHABLE" -> R.string.mcp_open_target_retry
            "LOCAL_DISABLED" -> R.string.mcp_allow_in_target
            "APP_NOT_LINKED", "NO_HUB", "NETWORK", "UNAUTHORIZED", "STONE_TOKEN" -> R.string.mcp_connect_hub
            "NO_PERMISSION", "NOT_SAME_DEVELOPER" -> R.string.phone_mcp_signature
            else -> R.string.mcp_check_target
        }), color = HubColors.Muted)
}

@Composable internal fun McpAnalytics(diagnostics: McpDiagnostics?) {
    if (diagnostics == null) return
    var rows by remember { mutableStateOf<List<JSONObject>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.padding(horizontal = 16.dp)) {
        item {
            HubCard {
                Text(stringResource(R.string.mcp_report_groups))
                TextButton({
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            val data = diagnostics.grouped()
                            rows = (0 until data.length()).mapNotNull(data::optJSONObject)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: HubException) {
                            error = StoneOrder.advice(failure.code, failure.message.orEmpty())
                        } catch (_: Exception) {
                            error = StoneOrder.advice("RESULT_UNCONFIRMED")
                        } finally { loading = false }
                    }
                }, enabled = !loading) { Text(stringResource(R.string.refresh)) }
                error?.let { Text(it, color = HubColors.Bad) }
                rows?.takeIf { it.isEmpty() }?.let { Text(stringResource(R.string.mcp_no_failures)) }
                rows.orEmpty().forEach {
                    Text(stringResource(R.string.mcp_group_line, it.optString("app"), it.optString("tool"), it.optString("stage"),
                        it.optString("code"), it.optInt("count")))
                }
            }
        }
    }
}
