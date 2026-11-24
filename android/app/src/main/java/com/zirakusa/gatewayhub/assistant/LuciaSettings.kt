package com.zirakusa.gatewayhub.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubColors

/**
 * Lucía's settings: her map (the brain), «Oye Lucía» (and which name), only while charging, being the phone's assistant (corner swipe and
 * power button) and appearing on top (so she can open by voice when she isn't the assistant).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun LuciaSettings(state: AssistantState, actions: AssistantActions) =
    Column(Modifier.fillMaxSize().background(HubColors.Background).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(HubColors.Card).clickable { state.brainOpen = true }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.brain_title), color = HubColors.Text, fontSize = 17.sp)
                Text(stringResource(R.string.brain_hint), color = HubColors.Muted, fontSize = 14.sp)
            }
            ResultMark(state.last)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = HubColors.Muted)
        }
        Card {
            Line(stringResource(R.string.wake_title, state.name.shown), stringResource(R.string.wake_hint), state.wake, actions.setWake)
            Text(stringResource(R.string.wake_name), color = HubColors.Muted, fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WakeName.entries.forEach { n ->
                    FilterChip(state.name == n, { actions.setName(n) }, { Text(n.shown) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = HubColors.Gold, selectedLabelColor = HubColors.Background))
                }
            }
            Line(stringResource(R.string.only_charging), stringResource(R.string.only_charging_hint), state.onlyCharging, actions.setOnlyCharging)
        }
        Card {
            Text(stringResource(R.string.default_title), color = HubColors.Text, fontSize = 17.sp)
            Text(stringResource(if (state.isDefault) R.string.default_yes else R.string.default_hint), color = HubColors.Muted, fontSize = 14.sp)
            if (!state.isDefault) Button(stringResource(R.string.default_go), actions.makeDefault)
        }
        if (!state.isDefault) Card {
            Text(stringResource(R.string.appear_title), color = HubColors.Text, fontSize = 17.sp)
            Text(stringResource(if (state.canAppear) R.string.appear_yes else R.string.appear_hint), color = HubColors.Muted, fontSize = 14.sp)
            if (!state.canAppear) Button(stringResource(R.string.appear_go), actions.allowAppear)
        }
        state.local?.takeIf { it.models.supported }?.let { local ->
            Card {
                Text(stringResource(R.string.mode_title), color = HubColors.Text, fontSize = 17.sp)
                Text(stringResource(when (state.mode) { AnswerMode.RULES -> R.string.mode_rules_hint; AnswerMode.MODEL -> R.string.mode_model_hint
                    AnswerMode.BOTH -> R.string.mode_both_hint }), color = HubColors.Muted, fontSize = 14.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(AnswerMode.RULES to R.string.mode_rules, AnswerMode.MODEL to R.string.mode_model, AnswerMode.BOTH to R.string.mode_both).forEach { (m, label) ->
                        FilterChip(state.mode == m, { actions.setMode(m) }, { Text(stringResource(label)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = HubColors.Gold, selectedLabelColor = HubColors.Background))
                    }
                }
                if (state.mode != AnswerMode.RULES) {
                    Text(stringResource(R.string.interpreter_title), color = HubColors.Text, fontSize = 16.sp)
                    Text(stringResource(when (state.interpreter) { Interpreter.FEELINGS -> R.string.interpreter_feelings_hint
                        Interpreter.MCP -> R.string.interpreter_mcp_hint; Interpreter.AUTO -> R.string.interpreter_auto_hint }), color = HubColors.Muted, fontSize = 14.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Interpreter.FEELINGS to R.string.interpreter_feelings, Interpreter.MCP to R.string.interpreter_mcp,
                            Interpreter.AUTO to R.string.interpreter_auto).forEach { (i, label) ->
                            FilterChip(state.interpreter == i, { actions.setInterpreter(i) }, { Text(stringResource(label)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = HubColors.Gold, selectedLabelColor = HubColors.Background))
                        }
                    }
                }
                if (state.mode != AnswerMode.RULES && local.models.selected == null)
                    Text(stringResource(R.string.mode_needs_model), color = HubColors.Bad, fontSize = 14.sp)
                Line(stringResource(R.string.mode_locked), stringResource(R.string.mode_locked_hint), state.rulesWhenLocked, actions.setRulesWhenLocked)
                Button(stringResource(R.string.assistant_advanced)) { state.page = AssistantPage.ADVANCED }
            }
        }
        Card {
            Text(stringResource(R.string.voice_title), color = HubColors.Text, fontSize = 17.sp)
            Text(stringResource(if (state.modelReady) R.string.voice_ready else R.string.voice_loading), color = HubColors.Muted, fontSize = 14.sp)
            if (!state.offlineVoiceReady) {
                Text(stringResource(R.string.local_voice_missing), color = HubColors.Muted, fontSize = 14.sp)
                Button(stringResource(R.string.local_voice_install), actions.installVoice)
            }
        }
    }

@Composable private fun Card(content: @Composable () -> Unit) = Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(HubColors.Card).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }

@Composable private fun Line(title: String, hint: String, on: Boolean, change: (Boolean) -> Unit) =
    Row(Modifier.fillMaxWidth().clickable { change(!on) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = HubColors.Text, fontSize = 17.sp)
            Text(hint, color = HubColors.Muted, fontSize = 14.sp)
        }
        Switch(on, change, colors = SwitchDefaults.colors(checkedTrackColor = HubColors.Gold))
    }

@Composable private fun Button(text: String, onClick: () -> Unit) =
    Text(text, Modifier.clip(RoundedCornerShape(50)).background(HubColors.Gold).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp),
        color = HubColors.Background, fontSize = 15.sp)
