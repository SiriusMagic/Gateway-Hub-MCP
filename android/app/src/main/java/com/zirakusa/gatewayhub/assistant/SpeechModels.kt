package com.zirakusa.gatewayhub.assistant

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.assistant.local.ModelDownloadError
import com.zirakusa.gatewayhub.assistant.local.ModelStore
import com.zirakusa.gatewayhub.assistant.local.RepositoryModel
import java.io.File
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class DictationMode { VOSK, WHISPER, COMPARE, WHISPER_SMALL, WHISPER_SMALL_Q6, IMPORTED }

/** Presets are independent of the conversation model catalog. */
internal enum class WhisperModel(
    val mode: DictationMode, val label: Int, val bytes: Long, val digest: String, val fileName: String,
    private val repository: String, private val revision: String
) {
    BASE(DictationMode.WHISPER, R.string.dictation_whisper, 59_707_625,
        "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
        "ggml-base-q5_1.bin", "ggerganov/whisper.cpp", "5359861c739e955e79d9a303bcbc70fb988958b1"),
    SMALL(DictationMode.WHISPER_SMALL, R.string.dictation_small, 113_846_889,
        "bbca38a3114b8420c0383783d56ef840e8dd3a69ef7a0de1698ab5aa45a2da1f",
        "ggml-small-q3_k.bin", "Pomni/whisper-small-ggml-allquants", "a154baf8f2b411c546da4fabd06f6231805a2158"),
    SMALL_Q6(DictationMode.WHISPER_SMALL_Q6, R.string.dictation_small_q6, 206_820_789,
        "80e639ef6e400f85f16f2b83707f96f7ec235a2f3b9514027e59e55224e557f1",
        "ggml-small-q6_k.bin", "Pomni/whisper-small-ggml-allquants", "a154baf8f2b411c546da4fabd06f6231805a2158");

    val url get() = URI("https://huggingface.co/$repository/resolve/$revision/$fileName").toURL()

    companion object {
        val supported get() = android.os.Process.is64Bit() && "arm64-v8a" in android.os.Build.SUPPORTED_64_BIT_ABIS
    }
}

internal data class DictationEngine(val label: String, val transcriber: UtteranceTranscriber)

internal fun voiceModelLabel(file: File): String = file.nameWithoutExtension.substringAfter('_')

internal class SpeechModels(private val context: Context, private val scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences("assistant", Context.MODE_PRIVATE)
    private val store = ModelStore(context)
    private var job: Job? = null
    private val compare = listOf(WhisperModel.BASE, WhisperModel.SMALL)

    val supported = WhisperModel.supported
    var installed by mutableStateOf(WhisperModel.entries.filter { store.whisperFile(it) != null }.toSet())
        private set
    var imported by mutableStateOf(store.voiceModels())
        private set
    var selectedFile by mutableStateOf(imported.firstOrNull { it.name == prefs.getString("voice_model", null) })
        private set
    var selected by mutableStateOf(savedMode().takeIf(::available) ?: defaultWhisperOrVosk())
        private set
    var downloading by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf<Int?>(null)
        private set

    private fun savedMode() = DictationMode.entries.firstOrNull { it.name == prefs.getString("dictation_mode", null) }
        ?: if (prefs.getBoolean("whisper_selected", false)) DictationMode.WHISPER else DictationMode.VOSK

    fun available(mode: DictationMode) = when (mode) {
        DictationMode.VOSK -> true
        DictationMode.COMPARE -> supported && installed.containsAll(compare)
        DictationMode.IMPORTED -> supported && selectedFile?.isFile == true
        else -> supported && installed.any { it.mode == mode }
    }

    private fun defaultWhisperOrVosk() = if (available(DictationMode.WHISPER)) DictationMode.WHISPER else DictationMode.VOSK

    fun select(mode: DictationMode) {
        if (!available(mode)) return
        selected = mode
        prefs.edit { putString("dictation_mode", mode.name) }
    }

    fun select(file: File) {
        if (!supported || file !in imported) return
        selectedFile = file
        selected = DictationMode.IMPORTED
        prefs.edit {
            putString("voice_model", file.name)
            putString("dictation_mode", selected.name)
        }
    }

    fun engines(): List<DictationEngine> {
        if (!supported) return emptyList()
        if (selected == DictationMode.IMPORTED) {
            val file = selectedFile?.takeIf(File::isFile) ?: return emptyList()
            return listOf(DictationEngine(voiceModelLabel(file), WhisperDictation(file)))
        }
        val presets = if (selected == DictationMode.COMPARE) compare else WhisperModel.entries.filter { it.mode == selected }
        return presets.mapNotNull { preset ->
            store.whisperFile(preset)?.let { DictationEngine(context.getString(preset.label), WhisperDictation(it)) }
        }
    }

    fun download(model: WhisperModel) = install { progress ->
        store.downloadWhisper(model, progress)
        if (store.whisperFile(model) != null) installed = installed + model
    }

    fun download(model: RepositoryModel) = install { progress ->
        store.downloadVoice(model, progress)
        imported = store.voiceModels()
    }

    fun importFile(uri: Uri) = install { progress ->
        store.importVoice(uri, progress)
        imported = store.voiceModels()
    }

    private fun install(work: suspend (suspend (Float) -> Unit) -> Unit) {
        if (!supported || downloading || job?.isActive == true) return
        downloading = true
        progress = 0f
        error = null
        job = scope.launch {
            try {
                work { value -> withContext(Dispatchers.Main.immediate) { progress = value } }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = when (failure) {
                    is ModelDownloadError.Storage -> R.string.local_storage_error
                    is ModelDownloadError.Integrity -> R.string.local_integrity_error
                    is ModelDownloadError.Format -> R.string.dictation_invalid_file
                    else -> R.string.local_download_error
                }
            } finally {
                downloading = false
            }
        }
    }

    fun cancel() { job?.cancel() }

    fun delete(model: WhisperModel) {
        if (downloading) return
        if (!store.deleteWhisper(model)) {
            error = R.string.local_delete_error
            return
        }
        installed = installed - model
        if (!available(selected)) select(defaultWhisperOrVosk())
        error = null
    }

    fun delete(file: File) {
        if (downloading || file !in imported) return
        if (!file.delete()) {
            error = R.string.local_delete_error
            return
        }
        if (selectedFile == file) {
            selectedFile = null
            prefs.edit { remove("voice_model") }
            if (selected == DictationMode.IMPORTED) select(defaultWhisperOrVosk())
        }
        imported = store.voiceModels()
        error = null
    }
}
