package com.zirakusa.gatewayhub.assistant.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubColors

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun LocalModelSettings(local: LocalAssistant) {
    Text(stringResource(R.string.local_models), color = HubColors.Text, fontSize = 17.sp)
    if (!local.models.supported) {
        Text(stringResource(R.string.local_arm64), color = HubColors.Muted)
        return
    }

    val models = local.models
    for (model in LocalModel.entries) {
        Text(stringResource(R.string.local_model_size, model.title, model.sizeLabel), color = HubColors.Text)
        val canChange = !local.busy && local.pending == null
        when {
            models.downloading == model -> {
                LinearProgressIndicator(progress = { models.progress }, modifier = Modifier.fillMaxWidth())
                TextButton(models::cancelDownload) { Text(stringResource(R.string.local_cancel)) }
            }
            model in models.installed -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(models.selected == model, { models.select(model) }, enabled = canChange,
                    label = { Text(stringResource(if (models.selected == model) R.string.local_active else R.string.local_use)) })
                TextButton({ models.delete(model) }, enabled = canChange) { Text(stringResource(R.string.local_delete)) }
            }
            else -> TextButton({ models.download(model) }, enabled = models.downloading == null) {
                Text(stringResource(R.string.local_download))
            }
        }
    }
    models.downloadMessage?.let { Text(stringResource(it), color = HubColors.Bad) }

    // A .gguf already on the phone (downloaded with the browser, or by another app into Descargas): used in place.
    val canChange = !local.busy && local.pending == null
    for (model in models.linked) {
        Text(stringResource(R.string.local_model_size, model.name, model.sizeLabel), color = HubColors.Text)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(models.selectedLinked == model, { models.selectLinked(model) }, enabled = canChange,
                label = { Text(stringResource(if (models.selectedLinked == model) R.string.local_active else R.string.local_use)) })
            TextButton({ models.unlink(model) }, enabled = canChange) { Text(stringResource(R.string.local_unlink)) }
        }
    }
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(models::link)
    }
    Text(stringResource(R.string.local_link_hint), color = HubColors.Muted)
    TextButton({ runCatching { pick.launch(arrayOf("application/octet-stream", "*/*")) } }, enabled = canChange) {
        Text(stringResource(R.string.local_link))
    }
    models.linkMessage?.let { Text(stringResource(it), color = HubColors.Bad) }

    Text(stringResource(R.string.local_tone), color = HubColors.Text)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (tone in ConversationTone.entries) {
            val label = when (tone) {
                ConversationTone.NATURAL -> R.string.local_natural
                ConversationTone.WARM -> R.string.local_warm
                ConversationTone.DIRECT -> R.string.local_direct
            }
            FilterChip(local.tone == tone, { local.updateTone(tone) }, label = { Text(stringResource(label)) })
        }
    }
}

@Composable internal fun HubConnectionSettings(local: LocalAssistant) {
    if (!local.models.supported) return
    Text(stringResource(R.string.local_hub), color = HubColors.Text, fontSize = 17.sp)
    val hub = local.hub
    val host = hub.hubAddress
    if (host != null) {
        Text(stringResource(R.string.local_hub_connected, host), color = HubColors.Text)
        Text(stringResource(R.string.local_hub_example), color = HubColors.Muted)
        TextButton(hub::disconnect, enabled = !local.busy && local.pending == null) {
            Text(stringResource(R.string.local_hub_disconnect))
        }
    } else {
        var link by remember { mutableStateOf("") }
        Text(stringResource(R.string.local_hub_hint), color = HubColors.Muted)
        OutlinedTextField(link, { link = it.take(2_048) }, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.local_hub_agent_link)) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !hub.linkBusy)
        TextButton({ hub.connect(link) { link = "" } }, enabled = link.isNotBlank() && !hub.linkBusy) {
            Text(stringResource(if (hub.linkBusy) R.string.local_working else R.string.local_hub_connect))
        }
    }
    hub.linkMessage?.let { Text(stringResource(it), color = HubColors.Bad) }
}
