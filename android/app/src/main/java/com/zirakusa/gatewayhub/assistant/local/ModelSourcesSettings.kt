package com.zirakusa.gatewayhub.assistant.local

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable internal fun ModelSourceSettings(models: LocalModels, canChange: Boolean) {
    val directory = models.directory
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(directory::choose)
    }
    LaunchedEffect(models) { directory.refresh() }
    Text(stringResource(R.string.local_folder_hint), color = HubColors.Muted)
    TextButton({ pickFolder.launch(directory.uri) }, enabled = canChange && !directory.busy) {
        Text(stringResource(R.string.local_folder_choose))
    }
    if (directory.uri != null) {
        directory.folder?.let { folder ->
            Text(stringResource(R.string.local_folder_name, folder.name), color = HubColors.Text)
            if (folder.models.isEmpty()) Text(stringResource(R.string.local_folder_empty), color = HubColors.Muted)
            for (model in folder.models) {
                Text(stringResource(R.string.local_model_size, model.name, model.sizeLabel), color = HubColors.Text)
                FilterChip(models.selectedLinked == model, { models.selectLinked(model) }, enabled = canChange,
                    label = { Text(stringResource(if (models.selectedLinked == model) R.string.local_active else R.string.local_use)) })
            }
        }
        TextButton(directory::refresh, enabled = canChange && !directory.busy) {
            Text(stringResource(R.string.local_folder_refresh))
        }
    }
    if (directory.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    directory.message?.let { Text(stringResource(it), color = HubColors.Bad) }
    RepositoryModelPicker(
        canDownload = canChange && !models.downloads.active,
        accepts = { name, size -> standaloneGguf(name) && LinkedModel.accepts(name, size) },
        emptyMessage = R.string.local_hf_empty,
        download = { models.download(it) },
    )
    models.downloads.repository?.let { model ->
        Text(model.name, color = HubColors.Text)
        LinearProgressIndicator(progress = { models.downloads.progress }, modifier = Modifier.fillMaxWidth())
        TextButton(models.downloads::cancel) { Text(stringResource(R.string.local_cancel)) }
    }
}

@Composable internal fun RepositoryModelPicker(
    canDownload: Boolean,
    accepts: (String, Long) -> Boolean,
    emptyMessage: Int,
    download: (RepositoryModel) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<RepositoryModel>()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    TextButton({ expanded = !expanded }) { Text(stringResource(R.string.local_hf_add)) }
    if (expanded) {
        OutlinedTextField(query, { query = it.take(256); results = emptyList(); error = null },
            modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !searching,
            label = { Text(stringResource(R.string.local_hf_repo)) })
        TextButton({
            val repository = HuggingFaceModels.repository(query)
            if (repository == null) {
                error = R.string.local_hf_invalid
            } else {
                searching = true
                error = null
                results = emptyList()
                scope.launch {
                    try {
                        results = HuggingFaceModels.files(repository, accepts)
                        if (results.isEmpty()) error = emptyMessage
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        error = R.string.local_hf_error
                    } finally {
                        searching = false
                    }
                }
            }
        }, enabled = query.isNotBlank() && !searching) {
            Text(stringResource(R.string.local_hf_find))
        }
        if (searching) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        error?.let { Text(stringResource(it), color = HubColors.Bad) }
        for (model in results) {
            val size = "%.2f GB".format(java.util.Locale.forLanguageTag("es"), model.size / 1_000_000_000.0)
            Text(stringResource(R.string.local_model_size, model.name, size), color = HubColors.Text)
            TextButton({ download(model) }, enabled = canDownload) {
                Text(stringResource(R.string.local_download))
            }
        }
    }
}
