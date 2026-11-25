package com.zirakusa.gatewayhub.assistant.local

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.assistant.SpeechModels
import com.zirakusa.gatewayhub.assistant.VoiceModelStatus
import com.zirakusa.gatewayhub.assistant.WhisperSettings
import com.zirakusa.gatewayhub.ui.HubCard
import com.zirakusa.gatewayhub.ui.HubColors

@Composable internal fun ModelPage(local: LocalAssistant?, speech: SpeechModels?, listening: Boolean) {
    val models = local?.models
    val canLink = local?.let { it.models.supported && !it.busy } == true
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) models?.link(uri)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) models?.directory?.choose(uri)
    }
    var menu by rememberSaveable { mutableStateOf(false) }
    var repository by rememberSaveable { mutableStateOf(false) }
    var voice by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (speech != null) HubCard { VoiceModelStatus(speech, listening) }
            if (local != null) HubCard {
                LocalModelSettings(local)
                RepositoryDownloadProgress(local.models)
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            FloatingActionButton({ menu = true }, containerColor = HubColors.Gold, contentColor = HubColors.Background) {
                Icon(Icons.Default.Add, stringResource(R.string.models_add))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem({ Text(stringResource(R.string.local_hf_add_gguf)) }, enabled = canLink,
                    onClick = { menu = false; repository = true })
                DropdownMenuItem({ Text(stringResource(R.string.local_link)) }, enabled = canLink,
                    onClick = { menu = false; filePicker.launch(arrayOf("application/octet-stream", "*/*")) })
                DropdownMenuItem({ Text(stringResource(R.string.local_folder_choose)) }, enabled = canLink && models?.directory?.busy != true,
                    onClick = { menu = false; folderPicker.launch(models?.directory?.uri) })
                DropdownMenuItem({ Text(stringResource(R.string.voice_add_model)) }, enabled = speech != null,
                    onClick = { menu = false; voice = true })
            }
        }
    }
    if (repository && models != null) {
        SourceDialog(R.string.local_hf_add_gguf, { repository = false }) { HuggingFaceFinder(models) }
    }
    if (voice && speech != null) {
        SourceDialog(R.string.voice_add_model, { voice = false }) { WhisperSettings(speech, listening) }
    }
}

@Composable private fun SourceDialog(title: Int, close: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text(stringResource(title)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() } },
        confirmButton = { TextButton(close) { Text(stringResource(R.string.models_close)) } })
}
