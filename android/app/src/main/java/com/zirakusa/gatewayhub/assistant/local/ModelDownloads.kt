package com.zirakusa.gatewayhub.assistant.local

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.gatewayhub.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ModelDownloads(private val scope: CoroutineScope, private val supported: Boolean) {
    private var job: Job? = null
    var builtIn by mutableStateOf<LocalModel?>(null)
        private set
    var repository by mutableStateOf<RepositoryModel?>(null)
        private set
    val active get() = builtIn != null || repository != null
    var progress by mutableFloatStateOf(0f)
        private set
    var message by mutableStateOf<Int?>(null)
        private set

    fun start(
        builtIn: LocalModel? = null,
        remote: RepositoryModel? = null,
        download: suspend (suspend (Float) -> Unit) -> Unit,
    ) {
        if (!supported || active || job?.isActive == true) return
        this.builtIn = builtIn
        repository = remote
        progress = 0f
        message = null
        job = scope.launch {
            try {
                download { value -> withContext(Dispatchers.Main.immediate) { progress = value } }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                message = when (error) {
                    is ModelDownloadError.Storage -> R.string.local_storage_error
                    is ModelDownloadError.Integrity -> R.string.local_integrity_error
                    else -> R.string.local_download_error
                }
            } finally {
                this@ModelDownloads.builtIn = null
                repository = null
            }
        }
    }

    fun deleteFailed() { message = R.string.local_delete_error }
    fun cancel() { job?.cancel() }
}
