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
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A catalog entry for speech recognition, stored separately from conversation weights. */
internal enum class WhisperModel(
    val bytes: Long, val digest: String, val fileName: String, private val repository: String, private val revision: String
) {
    BASE(59_707_625, "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
        "ggml-base-q5_1.bin", "ggerganov/whisper.cpp", "5359861c739e955e79d9a303bcbc70fb988958b1"),
    SMALL(113_846_889, "bbca38a3114b8420c0383783d56ef840e8dd3a69ef7a0de1698ab5aa45a2da1f",
        "ggml-small-q3_k.bin", "Pomni/whisper-small-ggml-allquants", "a154baf8f2b411c546da4fabd06f6231805a2158");

    val url get() = URI("https://huggingface.co/$repository/resolve/$revision/$fileName").toURL()

    companion object {
        val supported get() = android.os.Process.is64Bit() && "arm64-v8a" in android.os.Build.SUPPORTED_64_BIT_ABIS
    }
}

internal enum class DictationMode { VOSK, WHISPER, COMPARE, WHISPER_SMALL }

internal data class DictationEngine(val model: WhisperModel, val transcriber: UtteranceTranscriber)

internal class SpeechModels(context: Context, private val scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences("assistant", Context.MODE_PRIVATE)
    private val store = ModelStore(context)
    private var job: Job? = null

    val supported = WhisperModel.supported
    var installed by mutableStateOf(WhisperModel.entries.filter { store.whisperFile(it) != null }.toSet())
        private set
    var selected by mutableStateOf(savedMode().takeIf(::available) ?: defaultWhisperOrVosk())
        private set
    var downloading by mutableStateOf<WhisperModel?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf<Int?>(null)
        private set

    private fun savedMode() = DictationMode.entries.firstOrNull { it.name == prefs.getString("dictation_mode", null) }
        ?: if (prefs.getBoolean("whisper_selected", false)) DictationMode.WHISPER else DictationMode.VOSK

    fun available(mode: DictationMode) = when (mode) {
        DictationMode.VOSK -> true
        DictationMode.WHISPER -> supported && WhisperModel.BASE in installed
        DictationMode.COMPARE -> supported && installed.containsAll(WhisperModel.entries)
        DictationMode.WHISPER_SMALL -> supported && WhisperModel.SMALL in installed
    }

    private fun defaultWhisperOrVosk() = if (available(DictationMode.WHISPER)) DictationMode.WHISPER else DictationMode.VOSK

    fun select(mode: DictationMode) {
        if (!available(mode)) return
        selected = mode
        prefs.edit { putString("dictation_mode", mode.name) }
    }

    fun engines(): List<DictationEngine> {
        if (!supported) return emptyList()
        val models = when (selected) {
            DictationMode.VOSK -> emptyList()
            DictationMode.WHISPER -> listOf(WhisperModel.BASE)
            DictationMode.WHISPER_SMALL -> listOf(WhisperModel.SMALL)
            DictationMode.COMPARE -> listOf(WhisperModel.BASE, WhisperModel.SMALL)
        }
        return models.mapNotNull { model ->
            store.whisperFile(model)?.let { DictationEngine(model, WhisperDictation(it)) }
        }
    }

    fun download(model: WhisperModel) {
        if (!supported || downloading != null || job?.isActive == true) return
        downloading = model
        progress = 0f
        error = null
        job = scope.launch {
            try {
                store.downloadWhisper(model) { value -> withContext(Dispatchers.Main.immediate) { progress = value } }
                if (store.whisperFile(model) != null) installed = installed + model
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = when (failure) {
                    is ModelDownloadError.Storage -> R.string.local_storage_error
                    is ModelDownloadError.Integrity -> R.string.local_integrity_error
                    else -> R.string.local_download_error
                }
            } finally {
                downloading = null
            }
        }
    }

    fun cancel() { job?.cancel() }

    fun delete(model: WhisperModel) {
        if (downloading != null) return
        if (!store.deleteWhisper(model)) {
            error = R.string.local_delete_error
            return
        }
        installed = installed - model
        if (!available(selected)) select(defaultWhisperOrVosk())
        error = null
    }
}
