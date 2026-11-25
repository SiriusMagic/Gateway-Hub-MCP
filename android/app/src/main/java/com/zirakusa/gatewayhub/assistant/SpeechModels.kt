package com.zirakusa.gatewayhub.assistant

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.assistant.local.ModelDownloadError
import com.zirakusa.gatewayhub.assistant.local.ModelStore
import java.io.File
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A catalog entry for speech recognition, stored separately from conversation weights. */
internal object WhisperBase {
    const val bytes = 59_707_625L
    const val digest = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898"
    const val fileName = "ggml-base-q5_1.bin"
    private const val revision = "5359861c739e955e79d9a303bcbc70fb988958b1"
    val url = URI("https://huggingface.co/ggerganov/whisper.cpp/resolve/$revision/$fileName").toURL()
    val supported get() = android.os.Process.is64Bit() && "arm64-v8a" in android.os.Build.SUPPORTED_64_BIT_ABIS
}

internal class SpeechModels(context: Context, private val scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences("assistant", Context.MODE_PRIVATE)
    private val store = ModelStore(context)
    private var job: Job? = null

    val supported = WhisperBase.supported
    var installed by mutableStateOf(store.whisperFile() != null)
        private set
    var selected by mutableStateOf(supported && installed && prefs.getBoolean("whisper_selected", false))
        private set
    var downloading by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf<Int?>(null)
        private set

    fun select(useWhisper: Boolean) {
        if (useWhisper && (!supported || !installed)) return
        selected = useWhisper
        prefs.edit { putBoolean("whisper_selected", useWhisper) }
    }

    fun current(): File? = if (supported && selected) store.whisperFile() else null

    fun download() {
        if (!supported || downloading || job?.isActive == true) return
        downloading = true
        progress = 0f
        error = null
        job = scope.launch {
            try {
                store.downloadWhisper { value -> withContext(Dispatchers.Main.immediate) { progress = value } }
                installed = store.whisperFile() != null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = when (failure) {
                    is ModelDownloadError.Storage -> R.string.local_storage_error
                    is ModelDownloadError.Integrity -> R.string.local_integrity_error
                    else -> R.string.local_download_error
                }
            } finally {
                downloading = false
            }
        }
    }

    fun cancel() { job?.cancel() }

    fun delete() {
        if (downloading) return
        if (!store.deleteWhisper()) {
            error = R.string.local_delete_error
            return
        }
        select(false)
        installed = false
        error = null
    }
}
