package com.zirakusa.gatewayhub.assistant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubColors

/** What the count screen asks of the phone. */
class CountActions(val pause: () -> Unit = {}, val resume: () -> Unit = {}, val save: () -> Unit = {}, val discard: () -> Unit = {},
    val openNote: () -> Unit = {}, val share: () -> Unit = {}, val again: () -> Unit = {})

/** «Modo conteo»: the total, each thing with − and +, what was just heard, and save to Notas. Big, to read from afar. */
@Composable fun CountScreen(state: AssistantState, actions: CountActions) {
    var sure by remember { mutableStateOf(false) }
    // Back would lose the count: ask first.
    BackHandler { if (state.tallyLines.isEmpty() || state.tallySavedId != null) actions.discard() else sure = true }
    Column(Modifier.fillMaxSize().background(HubColors.Background).statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.count_title), color = HubColors.Muted, fontSize = 14.sp)
                state.tally?.title?.takeIf { it.isNotBlank() }?.let { Text(it.replaceFirstChar(Char::uppercase), color = HubColors.Text, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Text("${state.tallyLines.sumOf { it.n }}", color = HubColors.Gold, fontSize = 56.sp, fontWeight = FontWeight.Bold)
        }
        val heard = state.partial.ifBlank { state.tallyHeard }
        Text(if (heard.isBlank()) " " else "🎙 $heard", Modifier.padding(vertical = 6.dp), color = HubColors.Gold, fontSize = 16.sp, maxLines = 2)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.tallyLines.isEmpty()) item {
                Text(stringResource(R.string.count_empty), color = HubColors.Muted, fontSize = 16.sp, modifier = Modifier.padding(vertical = 12.dp))
            }
            items(state.tallyLines, key = { it.key }) { line -> Line(line, state, state.tallySavedId == null) }
        }
        when {
            state.tallySavedId != null -> Saved(actions)
            else -> {
                if (state.tallyFailed) Text(stringResource(R.string.count_no_notas), color = HubColors.Bad, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                Text(stringResource(R.string.count_help), color = HubColors.Muted, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val on = state.tallyListening
                    Box(Modifier.size(64.dp).scale(1f + 0.25f * state.level).clip(CircleShape).background(if (on) HubColors.Gold else HubColors.Card)
                        .border(2.dp, HubColors.Gold, CircleShape).clickable(onClickLabel = stringResource(if (on) R.string.count_pause else R.string.count_resume)) {
                            if (on) actions.pause() else actions.resume()
                        }, contentAlignment = Alignment.Center) {
                        Icon(if (on) Icons.Default.Pause else Icons.Default.Mic, stringResource(if (on) R.string.count_pause else R.string.count_resume),
                            tint = if (on) HubColors.Background else HubColors.Gold)
                    }
                    Text(stringResource(if (on) R.string.count_listening else R.string.count_paused), Modifier.weight(1f).padding(horizontal = 12.dp),
                        color = HubColors.Muted, fontSize = 13.sp)
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill(stringResource(R.string.count_discard), Modifier.weight(1f), HubColors.Card, HubColors.Text) {
                        if (state.tallyLines.isEmpty()) actions.discard() else sure = true
                    }
                    Pill(stringResource(if (state.tallyFailed) R.string.count_share else R.string.count_save), Modifier.weight(1.4f), HubColors.Gold, HubColors.Background,
                        enabled = state.tallyLines.isNotEmpty()) { if (state.tallyFailed) actions.share() else actions.save() }
                }
            }
        }
    }
    if (sure) AlertDialog(onDismissRequest = { sure = false }, text = { Text(stringResource(R.string.count_discard_sure)) },
        confirmButton = { TextButton({ sure = false; actions.discard() }) { Text(stringResource(R.string.count_discard)) } },
        dismissButton = { TextButton({ sure = false }) { Text(stringResource(R.string.back)) } })
}

@Composable private fun Line(line: Tallied, state: AssistantState, editable: Boolean) =
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(HubColors.Card).padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(line.label, Modifier.weight(1f), color = HubColors.Text, fontSize = 18.sp)
        if (editable) IconButton({ state.bumpTally(line.key, -1) }) { Icon(Icons.Default.Remove, stringResource(R.string.count_less, line.label), tint = HubColors.Muted) }
        Text("${line.n}", Modifier.width(52.dp), color = HubColors.Gold, fontSize = 26.sp, fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (editable) IconButton({ state.bumpTally(line.key, 1) }) { Icon(Icons.Default.Add, stringResource(R.string.count_more, line.label), tint = HubColors.Gold) }
    }

@Composable private fun Saved(actions: CountActions) = Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(stringResource(R.string.count_saved), color = HubColors.Good, fontSize = 16.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Pill(stringResource(R.string.count_new), Modifier.weight(1f), HubColors.Card, HubColors.Text, onClick = actions.again)
        Pill(stringResource(R.string.count_open_note), Modifier.weight(1f), HubColors.Gold, HubColors.Background, onClick = actions.openNote)
    }
    Pill(stringResource(R.string.count_close), Modifier.fillMaxWidth(), HubColors.Card, HubColors.Text, onClick = actions.discard)
}

@Composable private fun Pill(text: String, modifier: Modifier, background: androidx.compose.ui.graphics.Color, color: androidx.compose.ui.graphics.Color,
    enabled: Boolean = true, onClick: () -> Unit) =
    Box(modifier.clip(CircleShape).background(if (enabled) background else HubColors.Card).clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) color else HubColors.Muted, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
