package com.zirakusa.gatewayhub.assistant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubCard
import com.zirakusa.gatewayhub.ui.HubColors

/**
 * Arka › MCP, on top: the family's Modo MCP, which no app shows any more. «Activar MCP» turns every app's public link on
 * or off, «Permitir MCP local» lets Lucía (and the feelings rules) use their tools on the phone, «Crear token» goes to the
 * hub's agents. [switched] is how many apps the last «Activar MCP» changed.
 */
@Composable internal fun PhoneMcpControls(remote: Boolean, onRemote: (Boolean) -> Unit, local: Boolean, onLocal: (Boolean) -> Unit,
    createToken: () -> Unit, switched: Int?) {
    HubCard {
        Text(stringResource(R.string.phone_mcp_control), color = HubColors.Text, fontSize = 18.sp)
        Text(stringResource(R.string.phone_mcp_control_hint), color = HubColors.Muted, fontSize = 13.sp)
        Toggle(R.string.phone_mcp_enable, R.string.phone_mcp_enable_hint, remote, onRemote)
        switched?.let { Text(pluralStringResource(R.plurals.phone_mcp_switched, it, it), color = HubColors.Good, fontSize = 13.sp) }
        Toggle(R.string.phone_mcp_local, R.string.phone_mcp_local_hint, local, onLocal)
        Text(stringResource(R.string.phone_mcp_token_hint), Modifier.padding(top = 12.dp), color = HubColors.Muted, fontSize = 13.sp)
        Button(createToken, Modifier.fillMaxWidth().padding(top = 4.dp)) { Text(stringResource(R.string.phone_mcp_connect_external)) }
    }
}

@Composable private fun Toggle(title: Int, hint: Int, on: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), color = HubColors.Text, fontSize = 16.sp)
            Text(stringResource(hint), color = HubColors.Muted, fontSize = 13.sp)
        }
        Switch(on, change, Modifier.padding(start = 8.dp))
    }
}
