package com.zirakusa.gatewayhub.assistant

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubColors

/** What the screen asks of the phone. */
class AssistantActions(val listen: () -> Unit = {}, val stop: () -> Unit = {}, val makeDefault: () -> Unit = {},
    val setWake: (Boolean) -> Unit = {}, val setName: (WakeName) -> Unit = {}, val setOnlyCharging: (Boolean) -> Unit = {},
    val allowAppear: () -> Unit = {}, val setVoice: (Boolean) -> Unit = {}, val installVoice: () -> Unit = {},
    val manageHub: () -> Unit = {})

/** «Asistente»: the conversation and a box to write or the microphone to speak; the brain's map is in her settings. */
@Composable fun AssistantScreen(state: AssistantState, actions: AssistantActions = AssistantActions(), counting: CountActions = CountActions()) {
    if (state.tally != null) return CountScreen(state, counting)
    if (state.brainOpen) {
        androidx.activity.compose.BackHandler { state.brainOpen = false }
        return BrainPage(state)
    }
    AssistantWorkspace(state, actions) { showMenu -> Talking(state, actions, showMenu) }
    state.local?.pending?.let { pending ->
        AlertDialog(
            onDismissRequest = { state.confirmHub(false) },
            title = { Text(stringResource(R.string.local_hub_review)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(pending.title)
                    Text(pending.arguments)
                }
            },
            confirmButton = { TextButton({ state.confirmHub(true) }) { Text(stringResource(R.string.local_hub_confirm)) } },
            dismissButton = { TextButton({ state.confirmHub(false) }) { Text(stringResource(R.string.local_cancel)) } }
        )
    }
}

@Composable private fun Talking(state: AssistantState, actions: AssistantActions, showMenu: () -> Unit) =
    Column(Modifier.fillMaxSize().background(HubColors.Background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(showMenu) { Icon(Icons.Default.Menu, stringResource(R.string.assistant_menu), tint = HubColors.Text) }
            Column(Modifier.weight(1f)) {
                Text(state.name.shown, color = HubColors.Text, fontSize = 24.sp)
                val h = state.health
                Text(if (h.total == 0) stringResource(R.string.assistant_hint) else pluralStringResource(R.plurals.assistant_health, h.total, h.total, h.understood, h.done, h.averageMs),
                    color = HubColors.Muted, fontSize = 12.sp)
            }
            IconButton({ actions.setVoice(!state.voiceAnswers) }) {
                Icon(if (state.voiceAnswers) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    stringResource(if (state.voiceAnswers) R.string.voice_on else R.string.voice_off), tint = HubColors.Muted)
            }
        }
        val local = state.local
        val busy = local?.busy == true
        val showModelSetup = local?.models?.supported == true && local.models.selected == null
        val showIntro = state.turns.isEmpty() || showModelSetup
        val introCount = if (showIntro) 1 else 0
        val list = rememberLazyListState()
        LaunchedEffect(state.last, busy) {
            if (busy) list.scrollToItem(state.turns.size + introCount)
            else if (state.turns.isNotEmpty()) list.animateScrollToItem(state.turns.lastIndex + introCount)
        }
        LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showIntro) item {
                if (showModelSetup) {
                    Button({ state.page = AssistantPage.MODELS }) { Text(stringResource(R.string.assistant_choose_model)) }
                }
                if (state.turns.isEmpty()) Examples(state, actions.makeDefault)
            }
            items(state.turns) { t -> TurnBubbles(t) }
            if (busy) item {
                Text(local.draft.orEmpty().ifBlank { stringResource(R.string.local_working) }, color = HubColors.Text,
                    modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(HubColors.Card).padding(14.dp))
                if (local.canStop) TextButton(local::stop) { Text(stringResource(R.string.local_stop)) }
            }
        }
        if (state.partial.isNotBlank()) Text(state.partial, Modifier.padding(horizontal = 20.dp), color = HubColors.Gold, fontSize = 15.sp)
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(state.typed, { state.typed = it.take(200) }, Modifier.weight(1f), shape = RoundedCornerShape(24.dp), singleLine = true, enabled = !busy && !state.listening,
                placeholder = { Text(stringResource(R.string.assistant_write)) },
                trailingIcon = { if (state.typed.isNotBlank()) IconButton(state::sendTyped, enabled = !busy && !state.listening) { Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.send), tint = HubColors.Gold) } })
            val listening = state.listening
            Box(Modifier.padding(start = 10.dp).size(60.dp).scale(1f + 0.25f * state.level).clip(CircleShape)
                .background(if (listening) HubColors.Gold else HubColors.Card).border(2.dp, HubColors.Gold, CircleShape)
                .clickable(enabled = !busy, onClickLabel = stringResource(if (listening) R.string.stop_listening else R.string.speak)) { if (listening) actions.stop() else actions.listen() },
                contentAlignment = Alignment.Center) {
                Icon(if (listening) Icons.Default.Stop else Icons.Default.Mic, stringResource(if (listening) R.string.stop_listening else R.string.speak),
                    tint = if (listening) HubColors.Background else HubColors.Gold)
            }
        }
    }

@Composable private fun TurnBubbles(t: Turn) = Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text((if (t.byVoice) "🎙 " else "") + t.heard, Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(18.dp)).background(HubColors.Gold)
            .padding(horizontal = 14.dp, vertical = 8.dp), color = HubColors.Background, fontSize = 15.sp)
    }
    val res = LocalResources.current
    Column(Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(18.dp)).background(HubColors.Card).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(Talk.text(t.reply, res), color = if (t.ok) HubColors.Text else HubColors.Bad, fontSize = 15.sp)
        Text(listOfNotNull(Talk.label(t.command.action, res), t.app?.name, "${t.millis} ms").distinct().joinToString(" · "), color = HubColors.Muted, fontSize = 11.sp)
    }
}

/** Before the first order: a few things to try (tap one to send it). */
@Composable private fun Examples(state: AssistantState, makeDefault: () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(stringResource(R.string.assistant_try), color = HubColors.Muted, fontSize = 13.sp)
    listOf(R.string.ex_music, R.string.ex_volume, R.string.ex_recommended, R.string.ex_alarm, R.string.ex_timer, R.string.ex_weather, R.string.ex_count, R.string.ex_call, R.string.ex_open).forEach { id ->
        val phrase = stringResource(id)
        Text(phrase, Modifier.clip(CircleShape).background(HubColors.Card).clickable { state.send(phrase, byVoice = false) }.padding(horizontal = 14.dp, vertical = 8.dp),
            color = HubColors.Text, fontSize = 14.sp)
    }
    Text(stringResource(R.string.assistant_default), Modifier.padding(top = 6.dp).clickable(onClick = makeDefault).padding(vertical = 6.dp),
        color = HubColors.Gold, fontSize = 14.sp)
}
