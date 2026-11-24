package com.zirakusa.gatewayhub.assistant.local

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.zirakusa.gatewayhub.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalModels(
    context: Context,
    private val scope: CoroutineScope,
    private val canChange: () -> Boolean,
    private val onUnselected: suspend () -> Unit
) {
    private val prefs = context.getSharedPreferences("assistant", Context.MODE_PRIVATE)
    private val store = ModelStore(context)
    private var job: Job? = null

    val supported = LocalLanguageModel.supported
    var installed by mutableStateOf(LocalModel.entries.filter { store.installed(it) != null }.toSet())
        private set
    var selected by mutableStateOf(LocalModel.entries.firstOrNull { it.name == prefs.getString("local_model", null) && it in installed })
        private set
    var downloading by mutableStateOf<LocalModel?>(null)
        private set
    var progress by mutableStateOf(0f)
        private set
    var downloadMessage by mutableStateOf<Int?>(null)
        private set

    fun file(model: LocalModel) = store.installed(model)

    fun download(model: LocalModel) {
        if (!supported || job?.isActive == true) return
        job = scope.launch {
            downloading = model
            progress = 0f
            downloadMessage = null
            try {
                store.download(model) { value -> withContext(Dispatchers.Main.immediate) { progress = value } }
                installed = installed + model
                if (selected == null) select(model)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                downloadMessage = when (error) {
                    is ModelDownloadError.Storage -> R.string.local_storage_error
                    is ModelDownloadError.Integrity -> R.string.local_integrity_error
                    else -> R.string.local_download_error
                }
            } finally {
                installed = LocalModel.entries.filter { store.installed(it) != null }.toSet()
                downloading = null
            }
        }
    }

    fun cancelDownload() { job?.cancel() }

    fun select(model: LocalModel) {
        if (!canChange() || !supported || model !in installed) return
        selected = model
        prefs.edit { putString("local_model", model.name) }
    }

    fun delete(model: LocalModel) {
        if (!canChange() || downloading == model) return
        if (!store.delete(model)) {
            downloadMessage = R.string.local_delete_error
            return
        }
        installed = installed - model
        if (selected == model) {
            selected = null
            prefs.edit { remove("local_model") }
            scope.launch { onUnselected() }
        }
    }
}
