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
@Composable fun AppsList(hub: HubState, busy: Boolean, linkStone: (String) -> Unit, unlinkStone: () -> Unit) {
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
            if (m.prefix == "music_") StoneLink(hub.stoneLinked, busy, linkStone, unlinkStone)
        }
    }
}

@Composable private fun StoneLink(linked: Boolean, busy: Boolean, linkStone: (String) -> Unit, unlinkStone: () -> Unit) {
    val context = LocalContext.current
    var link by remember { mutableStateOf("") }
    SectionTitle(stringResource(R.string.stone_title))
    Text(stringResource(R.string.stone_hint), color = HubColors.Muted, fontSize = 13.sp)
    OutlinedTextField(link, { link = it }, Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true,
        label = { Text(stringResource(R.string.stone_link)) },
        trailingIcon = { TextButton({
            link = context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        }) { Text(stringResource(R.string.paste)) } })
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
        if (linked) TextButton(unlinkStone, enabled = !busy) { Text(stringResource(R.string.unlink), color = HubColors.Bad) }
        Button({ linkStone(link); link = "" }, Modifier.padding(start = 8.dp), enabled = !busy && link.isNotBlank()) {
            Text(stringResource(R.string.link))
        }
    }
}
