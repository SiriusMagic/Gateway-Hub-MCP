package com.zirakusa.gatewayhub.assistant

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubColors

@Composable internal fun VoiceModelStatus(models: SpeechModels, listening: Boolean) {
    Text(stringResource(R.string.voice_models_title), color = HubColors.Text)
    FilterChip(!models.selected, { models.select(false) }, enabled = !listening,
        label = { Text(stringResource(R.string.voice_vosk)) })
    if (models.installed && models.supported) {
        FilterChip(models.selected, { models.select(true) }, enabled = !listening,
            label = { Text(stringResource(R.string.voice_whisper)) })
    }
    if (models.downloading) {
        LinearProgressIndicator(progress = { models.progress }, modifier = Modifier.fillMaxWidth())
        TextButton(models::cancel) { Text(stringResource(R.string.local_cancel)) }
    }
    models.error?.let { Text(stringResource(it), color = HubColors.Bad) }
}

@Composable internal fun WhisperSettings(models: SpeechModels, listening: Boolean) {
    Text(stringResource(R.string.voice_whisper_spec))
    when {
        !models.supported -> Text(stringResource(R.string.voice_whisper_arm64))
        models.installed -> TextButton(models::delete, enabled = !listening && !models.downloading) {
            Text(stringResource(R.string.local_delete))
        }
        !models.downloading -> TextButton(models::download) { Text(stringResource(R.string.local_download)) }
    }
    VoiceModelStatus(models, listening)
}
