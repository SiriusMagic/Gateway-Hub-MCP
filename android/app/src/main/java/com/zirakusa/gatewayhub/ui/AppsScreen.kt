package com.zirakusa.gatewayhub.ui

import android.content.ClipboardManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.HubState

/** The hub's apps and, for Stone, linking with the link copied from Stone › Gateway Hub › Vincular. */
@Composable fun AppsList(hub: HubState) {
    hub.modules.forEach { m ->
        HubCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${m.prefix}*", color = HubColors.Muted, fontSize = 12.sp)
                }
                when {
                    m.prefix != "music_" -> Pill(stringResource(R.string.app_ready), HubColors.Good)
                    hub.stoneLinked -> Pill(stringResource(R.string.app_linked), HubColors.Good)
                    else -> Pill(stringResource(R.string.app_unlinked), HubColors.Bad)
                }
            }
            Text(m.summary, Modifier.padding(top = 6.dp), color = HubColors.Muted, fontSize = 13.sp)
            if (m.prefix == "music_" && !hub.stoneLinked) Text(stringResource(R.string.stone_hint), Modifier.padding(top = 6.dp), color = HubColors.Muted, fontSize = 13.sp)
        }
    }
}
