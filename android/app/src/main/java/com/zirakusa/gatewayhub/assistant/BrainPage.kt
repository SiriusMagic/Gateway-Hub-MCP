package com.zirakusa.gatewayhub.assistant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * «Mapa de Lucía» (in her settings): the whole brain, every app with what it does under it, tall enough to read; the
 * result of the last order is a mark at the top (Hecho / Falló), not a node.
 */
@Composable fun BrainPage(state: AssistantState) {
    BackHandler { state.brainOpen = false }
    Column(Modifier.fillMaxSize().background(HubColors.Background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ state.brainOpen = false }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = HubColors.Text) }
            Text(stringResource(R.string.brain_title), Modifier.weight(1f), color = HubColors.Text, fontSize = 22.sp)
            ResultMark(state.last)
        }
        Text(state.last?.heard ?: stringResource(R.string.brain_hint), Modifier.padding(horizontal = 20.dp), color = HubColors.Muted, fontSize = 13.sp)
        BrainCanvas(state, Modifier.fillMaxWidth().padding(top = 8.dp).verticalScroll(rememberScrollState()).height((Graph.rows() * 30).dp))
    }
}

/** ● Hecho (green) or ● Falló (red); grey before the first order. */
@Composable fun ResultMark(turn: Turn?) {
    val (color, text) = when { turn == null -> HubColors.Muted to R.string.brain_waiting; turn.ok -> HubColors.Good to R.string.brain_done; else -> HubColors.Bad to R.string.brain_failed }
    Row(Modifier.padding(end = 8.dp).clip(RoundedCornerShape(50)).background(HubColors.Card).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(stringResource(text), color = color, fontSize = 13.sp)
    }
}
