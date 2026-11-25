package com.zirakusa.gatewayhub.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubCard
import com.zirakusa.gatewayhub.ui.HubColors

/** One group of the RS page: Lucía's own rules (no app) or one family app's. */
internal data class RsGroup(val app: FamilyApp?, val rules: List<Examples.Rule>)

/** RS's groups, filtered by [query] (phrase or what it does, accents and case ignored); empty groups are left out. Pure. */
internal fun rsGroups(query: String): List<RsGroup> {
    val q = Words.normalize(query).trim()
    fun matches(r: Examples.Rule) = q.isEmpty() || Words.normalize(r.phrase + " " + (r.label ?: r.tool.orEmpty())).contains(q)
    return (listOf(RsGroup(null, Examples.rulesFor(null))) + Catalog.apps.map { RsGroup(it, Examples.rulesFor(it.id)) })
        .map { it.copy(rules = it.rules.filter(::matches)) }.filter { it.rules.isNotEmpty() }
}

/**
 * «RS» (Regla de Sentimiento), its own page in the side menu, apart from MCP: every phrase Lucía's rules understand,
 * what each does and whether she asks first, Lucía's own ones and then by app, with a search. It also says which
 * interpreter is on («Sentimientos» is the one that turns free speech into these rules).
 */
@Composable internal fun RsScreen(interpreter: Interpreter, openApp: ((FamilyApp) -> Boolean)? = null) {
    val context = LocalContext.current
    val open = openApp ?: remember(context) { AndroidHands(context)::open }
    var query by rememberSaveable { mutableStateOf("") }
    val groups = rsGroups(query)
    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            HubCard {
                Text(stringResource(R.string.rs_title), color = HubColors.Text, fontSize = 18.sp)
                Text(stringResource(R.string.rs_intro), color = HubColors.Muted, fontSize = 14.sp)
                Text(pluralStringResource(R.plurals.phone_mcp_rules_total, Examples.rules.size, Examples.rules.size, Examples.rulesFor(null).size), color = HubColors.Muted, fontSize = 13.sp)
                Text(stringResource(R.string.rs_interpreter, stringResource(when (interpreter) {
                    Interpreter.FEELINGS -> R.string.interpreter_feelings; Interpreter.MCP -> R.string.interpreter_mcp; Interpreter.AUTO -> R.string.interpreter_auto
                })), color = if (interpreter == Interpreter.MCP) HubColors.Muted else HubColors.Good, fontSize = 13.sp)
            }
        }
        item {
            OutlinedTextField(query, { query = it.take(60) }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text(stringResource(R.string.rs_search)) })
        }
        if (groups.isEmpty()) item { Text(stringResource(R.string.rs_none), color = HubColors.Muted) }
        items(groups, key = { it.app?.id ?: "lucia" }) { group -> RsGroupCard(group, query.isNotBlank(), open) }
    }
}

@Composable private fun RsGroupCard(group: RsGroup, open: Boolean, openApp: (FamilyApp) -> Boolean) {
    var expanded by rememberSaveable(group.app?.id) { mutableStateOf(false) }
    var openFailed by remember(group.app?.id) { mutableStateOf(false) }
    HubCard {
        Text(group.app?.name ?: stringResource(R.string.lucia), color = HubColors.Text, fontSize = 17.sp)
        Text(pluralStringResource(if (group.app == null) R.plurals.rs_own else R.plurals.rs_count, group.rules.size, group.rules.size), color = HubColors.Muted, fontSize = 13.sp)
        // While searching, every group with a match is shown open.
        // «Abrir» opens the app itself (as in MCP); Lucía's own rules have no app to open.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            group.app?.let { app -> OutlinedButton({ openFailed = !openApp(app) }) { Text(stringResource(R.string.scan_open)) } }
            if (!open) TextButton({ expanded = !expanded }) { Text(stringResource(if (expanded) R.string.phone_mcp_rules_hide else R.string.rs_show)) }
        }
        if (openFailed) Text(stringResource(R.string.phone_mcp_open_failed), color = HubColors.Bad)
        if (expanded || open) Column(Modifier.padding(bottom = 4.dp)) { RulesList(group.rules) }
    }
}

/** Each rule: the phrase Lucía understands, then what it does and whether she asks first. */
@Composable internal fun RulesList(rules: List<Examples.Rule>) {
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
