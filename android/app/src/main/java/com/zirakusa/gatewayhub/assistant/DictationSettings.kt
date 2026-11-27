package com.zirakusa.gatewayhub.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubColors

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun DictationSettings(models: SpeechModels, listening: Boolean) {
    Text(stringResource(R.string.dictation_model), color = HubColors.Muted, fontSize = 14.sp)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((mode, label) in listOf(DictationMode.VOSK to R.string.dictation_vosk,
            DictationMode.WHISPER to R.string.dictation_whisper, DictationMode.WHISPER_SMALL to R.string.dictation_small,
            DictationMode.COMPARE to R.string.dictation_compare)) {
            FilterChip(models.selected == mode, { models.select(mode) },
                enabled = !listening && models.available(mode),
                label = { Text(stringResource(label)) })
        }
    }
    if (models.selected == DictationMode.COMPARE) {
        Text(stringResource(R.string.dictation_compare_hint), color = HubColors.Muted, fontSize = 14.sp)
    }
    when {
        models.downloading != null -> {
            LinearProgressIndicator(progress = { models.progress }, modifier = Modifier.fillMaxWidth())
            TextButton(models::cancel) { Text(stringResource(R.string.local_cancel)) }
        }
        models.supported -> WhisperModel.entries.forEach { model ->
            val label = if (model == WhisperModel.BASE) R.string.dictation_whisper else R.string.dictation_small
            if (model in models.installed)
                TextButton({ models.delete(model) }, enabled = !listening) { Text(stringResource(R.string.dictation_delete, stringResource(label))) }
            else TextButton({ models.download(model) }) {
                Text(stringResource(R.string.dictation_download, stringResource(label), (model.bytes + 500_000) / 1_000_000))
            }
        }
    }
    models.error?.let { Text(stringResource(it), color = HubColors.Bad) }
}

@Composable internal fun DictationResult(result: DictationComparison, modifier: Modifier = Modifier) {
    val empty = stringResource(R.string.dictation_empty)
    Column(modifier) {
        Text(stringResource(R.string.dictation_vosk_result, result.vosk.ifBlank { empty }),
            color = HubColors.Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        result.samples.forEach { sample ->
            val label = if (sample.model == WhisperModel.BASE) R.string.dictation_whisper else R.string.dictation_small
            val format = if (result.comparison) R.string.dictation_compare_result else R.string.dictation_whisper_result
            Text(stringResource(format, stringResource(label), sample.text ?: empty, sample.endToTextMs),
                color = HubColors.Muted, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}
