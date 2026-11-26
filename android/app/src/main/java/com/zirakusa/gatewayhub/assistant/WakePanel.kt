package com.zirakusa.gatewayhub.assistant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubColors

/** Compact voice entry over the current screen. Commands use the same conversation as the full chat. */
@Composable internal fun WakePanel(state: AssistantState, actions: AssistantActions, dismiss: () -> Unit, openChat: () -> Unit) {
    BackHandler(onBack = dismiss)
    val busy = state.executing || state.local?.busy == true
    val canEnter = !busy && state.local?.pending == null && state.proposal == null
    val request = state.partial.ifBlank { state.wakeRequest }
    val focus = LocalFocusManager.current
    val shape = RoundedCornerShape(26.dp)
    val submit = {
        if (canEnter && state.typed.isNotBlank()) {
            actions.stop()
            state.partial = ""
            state.sendTyped()
            focus.clearFocus()
        }
    }
    Box(Modifier.fillMaxSize().navigationBarsPadding().imePadding().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.widthIn(max = 460.dp).fillMaxWidth().clip(shape).background(HubColors.Card)
                .border(1.dp, HubColors.Muted.copy(alpha = 0.25f), shape)
                .verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        focus.clearFocus()
                        if (state.listening) actions.stop() else actions.listen()
                    },
                    enabled = canEnter
                ) {
                    val micColor = if (state.listening) HubColors.Gold else HubColors.Background
                    Icon(
                        Icons.Default.Mic,
                        stringResource(if (state.listening) R.string.stop_listening else R.string.speak),
                        Modifier.scale(1f + state.level * 0.15f).size(36.dp).background(micColor, CircleShape).padding(8.dp),
                        tint = if (state.listening) HubColors.Background else HubColors.Gold
                    )
                }
                Column(Modifier.weight(1f).padding(start = 6.dp)) {
                    Text(state.name.shown, color = HubColors.Text, fontSize = 19.sp)
                    if (state.listening || busy) {
                        Text(
                            stringResource(when {
                                state.whisperDecoding -> R.string.dictation_processing
                                state.listening -> R.string.wake_panel_listening
                                else -> R.string.local_working
                            }),
                            Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = HubColors.Gold, fontSize = 13.sp
                        )
                    }
                }
                IconButton(dismiss) { Icon(Icons.Default.Close, stringResource(R.string.wake_panel_close), tint = HubColors.Muted) }
            }
            if (request.isNotBlank()) Text(request, Modifier.padding(horizontal = 8.dp), color = HubColors.Text, maxLines = 4)
            state.dictationComparison?.let { DictationResult(it, Modifier.padding(horizontal = 8.dp)) }
            state.wakeReply?.let {
                Text(Talk.text(it.reply, LocalResources.current), Modifier.padding(horizontal = 8.dp),
                    color = if (it.ok) HubColors.Text else HubColors.Bad, maxLines = 4)
            }
            OutlinedTextField(
                state.typed,
                { state.typed = it.take(400) },
                Modifier.fillMaxWidth().onFocusChanged {
                    if (it.isFocused && state.listening) {
                        if (state.typed.isBlank()) state.typed = state.partial
                        actions.stop()
                        state.partial = ""
                    }
                },
                enabled = canEnter,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                shape = RoundedCornerShape(18.dp),
                placeholder = { Text(stringResource(R.string.wake_panel_write)) },
                trailingIcon = {
                    if (state.typed.isNotBlank()) IconButton(submit, enabled = canEnter) {
                        Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.send), tint = HubColors.Gold)
                    }
                }
            )
            TextButton(openChat, Modifier.align(Alignment.End)) { Text(stringResource(R.string.wake_panel_open_chat)) }
        }
    }
}
