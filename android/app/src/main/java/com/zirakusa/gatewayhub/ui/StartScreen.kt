package com.zirakusa.gatewayhub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R

/** First launch: the app joins the shared hub by itself. Only "try again" if there is no internet. */
@Composable fun StartScreen(state: UiState, vm: HubActions) {
    Surface(Modifier.fillMaxSize(), color = HubColors.Background, contentColor = HubColors.Text) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Hub, null, Modifier.size(64.dp), tint = HubColors.Gold)
            Text("Gateway Hub", Modifier.padding(top = 12.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.login_subtitle), Modifier.padding(top = 4.dp, bottom = 28.dp), color = HubColors.Muted, textAlign = TextAlign.Center)
            when {
                state.starting -> {
                    CircularProgressIndicator(color = HubColors.Gold)
                    Text(stringResource(R.string.start_working), Modifier.padding(top = 12.dp), color = HubColors.Muted)
                }
                else -> {
                    if (state.startFailed) Text(stringResource(R.string.start_failed), Modifier.padding(bottom = 8.dp), color = HubColors.Bad, textAlign = TextAlign.Center)
                    BigButton(stringResource(if (state.startFailed) R.string.start_retry else R.string.start_go), true, vm::start)
                }
            }
            Spacer(Modifier.height(32.dp))
            TextButton(vm::useOwnHub, enabled = !state.starting) { Text(stringResource(R.string.start_own_hub), color = HubColors.Muted) }
        }
    }
}
