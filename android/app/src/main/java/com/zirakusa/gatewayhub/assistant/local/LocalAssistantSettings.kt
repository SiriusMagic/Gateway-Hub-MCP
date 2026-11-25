package com.zirakusa.gatewayhub.assistant.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubColors

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun LocalModelSettings(local: LocalAssistant) {
    Text(stringResource(R.string.chat_models_title), color = HubColors.Text, fontSize = 17.sp)
    if (!local.models.supported) {
        Text(stringResource(R.string.local_arm64), color = HubColors.Muted)
        return
    }

    val models = local.models
    for (model in LocalModel.entries) {
        Text(stringResource(R.string.local_model_size, model.title, model.sizeLabel), color = HubColors.Text)
        val canChange = !local.busy
        when {
            models.downloads.builtIn == model -> {
                LinearProgressIndicator(progress = { models.downloads.progress }, modifier = Modifier.fillMaxWidth())
                TextButton(models.downloads::cancel) { Text(stringResource(R.string.local_cancel)) }
            }
            model in models.installed -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(models.selected == model, { models.select(model) }, enabled = canChange,
                    label = { Text(stringResource(if (models.selected == model) R.string.local_active else R.string.local_use)) })
                TextButton({ models.delete(model) }, enabled = canChange) { Text(stringResource(R.string.local_delete)) }
            }
            else -> TextButton({ models.download(model) }, enabled = !models.downloads.active) {
                Text(stringResource(R.string.local_download))
            }
        }
    }
    models.downloads.message?.let { Text(stringResource(it), color = HubColors.Bad) }

    // A .gguf already on the phone (downloaded with the browser, or by another app into Descargas): used in place.
    val canChange = !local.busy
    for (model in models.linked) {
        Text(stringResource(R.string.local_model_size, model.name, model.sizeLabel), color = HubColors.Text)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(models.selectedLinked == model, { models.selectLinked(model) }, enabled = canChange,
                label = { Text(stringResource(if (models.selectedLinked == model) R.string.local_active else R.string.local_use)) })
            TextButton({ models.unlink(model) }, enabled = canChange && !models.downloads.active) {
                Text(stringResource(if (models.isRepositoryModel(model)) R.string.local_delete else R.string.local_unlink))
            }
        }
    }
    models.linkMessage?.let { Text(stringResource(it), color = HubColors.Bad) }
    LinkedFolderSettings(models, canChange)

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
