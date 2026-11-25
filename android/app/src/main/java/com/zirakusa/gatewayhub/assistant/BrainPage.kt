package com.zirakusa.gatewayhub.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubColors

/** The deterministic rules and the most recent sequence of actions they handled. */
@Composable internal fun BrainPage(state: AssistantState) {
    val resources = LocalResources.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.rules_examples), color = HubColors.Text, fontSize = 17.sp)
        Text(stringResource(R.string.rules_example_math), color = HubColors.Muted)
        Text(stringResource(R.string.rules_example_call), color = HubColors.Muted)
        if (state.ruleInput.isNotBlank()) {
            Text(stringResource(R.string.rules_last), color = HubColors.Text, fontSize = 17.sp)
            Text(state.ruleInput, color = HubColors.Muted)
            for (turn in state.ruleTrace) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(Talk.label(turn.command.action, resources), Modifier.weight(1f), color = HubColors.Text)
                    ResultMark(turn)
                }
                Text(Talk.text(turn.reply, resources), color = HubColors.Muted)
            }
        }
        BrainCanvas(state, Modifier.fillMaxWidth().height((Graph.rows() * 30).dp))
    }
}

/** ● Hecho (green) or ● Falló (red); grey before the first order. */
@Composable fun ResultMark(turn: Turn?) {
    val (color, text) = when {
        turn == null -> HubColors.Muted to R.string.brain_waiting
        turn.reply.asksToCall || turn.reply.asksCallName || turn.reply.say in setOf(Say.CONFIRM, Say.NEED_CALL_PERMISSION) ->
            HubColors.Gold to R.string.rules_waiting
        turn.ok -> HubColors.Good to R.string.brain_done
        else -> HubColors.Bad to R.string.brain_failed
    }
    Row(Modifier.padding(end = 8.dp).clip(RoundedCornerShape(50)).background(HubColors.Card).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(stringResource(text), color = color, fontSize = 13.sp)
    }
}
