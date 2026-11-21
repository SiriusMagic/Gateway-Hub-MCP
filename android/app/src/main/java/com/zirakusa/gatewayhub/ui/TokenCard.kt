package com.zirakusa.gatewayhub.ui

import android.content.ClipboardManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.TokenApps

/** «Vincular con token»: paste the link an app copied (Modo MCP › token tipo Hub); the token says which app it is. */
@Composable fun TokenCard(busy: Boolean, linkToken: (String) -> Unit) {
    val context = LocalContext.current
    var link by remember { mutableStateOf("") }
    val app = TokenApps.identify(link)
    HubCard {
        Text(stringResource(R.string.token_title), fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(stringResource(R.string.token_hint), Modifier.padding(top = 4.dp), color = HubColors.Muted, fontSize = 13.sp)
        OutlinedTextField(link, { link = it }, Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true,
            label = { Text(stringResource(R.string.token_field)) },
            trailingIcon = { TextButton({
                link = context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
            }) { Text(stringResource(R.string.paste)) } })
        if (link.isNotBlank()) when {
            app == null -> Text(stringResource(R.string.token_unknown), Modifier.padding(top = 6.dp), color = HubColors.Bad, fontSize = 13.sp)
            !app.supported -> Text(stringResource(R.string.token_not_supported, app.name), Modifier.padding(top = 6.dp), color = HubColors.Bad, fontSize = 13.sp)
            else -> Text(stringResource(R.string.token_detected, app.name), Modifier.padding(top = 6.dp), color = HubColors.Good, fontSize = 13.sp)
        }
        Button({ linkToken(link); link = "" }, Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !busy && app?.supported == true) { Text(stringResource(R.string.link)) }
    }
}
