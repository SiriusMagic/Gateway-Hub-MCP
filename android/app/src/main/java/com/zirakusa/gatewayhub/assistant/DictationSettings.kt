package com.zirakusa.gatewayhub.assistant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.assistant.local.RepositoryModelPicker
import com.zirakusa.gatewayhub.assistant.local.acceptsVoiceModel
import com.zirakusa.gatewayhub.ui.HubColors
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun DictationSettings(models: SpeechModels, listening: Boolean) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(models::importFile)
    }
    var deleteFile by remember { mutableStateOf<File?>(null) }
    val canChange = !listening && !models.downloading
    Text(stringResource(R.string.dictation_model), color = HubColors.Muted, fontSize = 14.sp)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val modes = listOf(DictationMode.VOSK to R.string.dictation_vosk) +
            WhisperModel.entries.map { it.mode to it.label } + (DictationMode.COMPARE to R.string.dictation_compare)
        for ((mode, label) in modes) {
            FilterChip(models.selected == mode, { models.select(mode) },
                enabled = canChange && models.available(mode), label = { Text(stringResource(label)) })
        }
    }
    if (models.selected == DictationMode.COMPARE) {
        Text(stringResource(R.string.dictation_compare_hint), color = HubColors.Muted, fontSize = 14.sp)
    }
    if (models.downloading) {
        if (models.progress > 0) {
            LinearProgressIndicator(progress = { models.progress }, modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        TextButton(models::cancel) { Text(stringResource(R.string.local_cancel)) }
    }
    if (models.supported) {
        WhisperModel.entries.forEach { model ->
            val label = stringResource(model.label)
            if (model in models.installed) {
                TextButton({ models.delete(model) }, enabled = canChange) {
                    Text(stringResource(R.string.dictation_delete, label))
                }
            } else TextButton({ models.download(model) }, enabled = canChange) {
                Text(stringResource(R.string.dictation_download, label, (model.bytes + 500_000) / 1_000_000))
            }
        }
        TextButton({ picker.launch(arrayOf("*/*")) }, enabled = canChange) {
            Text(stringResource(R.string.dictation_import))
        }
        RepositoryModelPicker(canChange, ::acceptsVoiceModel, R.string.dictation_hf_empty, download = { models.download(it) })
        models.imported.forEach { file ->
            val label = voiceModelLabel(file)
            FilterChip(models.selected == DictationMode.IMPORTED && models.selectedFile == file,
                { models.select(file) }, enabled = canChange, label = { Text(label) })
            TextButton({ deleteFile = file }, enabled = canChange) { Text(stringResource(R.string.dictation_delete, label)) }
        }
    }
    deleteFile?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteFile = null },
            title = { Text(stringResource(R.string.dictation_delete, voiceModelLabel(file))) },
            confirmButton = {
                TextButton({ models.delete(file); deleteFile = null }) { Text(stringResource(R.string.local_delete)) }
            },
            dismissButton = { TextButton({ deleteFile = null }) { Text(stringResource(R.string.local_cancel)) } },
        )
    }
    models.error?.let { Text(stringResource(it), color = HubColors.Bad) }
}

@Composable internal fun DictationResult(result: DictationComparison, modifier: Modifier = Modifier) {
    val empty = stringResource(R.string.dictation_empty)
    Column(modifier) {
        Text(stringResource(R.string.dictation_vosk_result, result.vosk.ifBlank { empty }),
            color = HubColors.Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        result.samples.forEach { sample ->
            val format = if (result.comparison) R.string.dictation_compare_result else R.string.dictation_whisper_result
            Text(stringResource(format, sample.label, sample.text ?: empty, sample.endToTextMs),
                color = HubColors.Muted, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}
