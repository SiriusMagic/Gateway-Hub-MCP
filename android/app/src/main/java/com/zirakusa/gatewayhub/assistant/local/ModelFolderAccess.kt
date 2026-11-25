package com.zirakusa.gatewayhub.assistant.local

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.net.toUri
import com.zirakusa.gatewayhub.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ModelFolderAccess(
    context: Context,
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope,
    private val canChange: () -> Boolean,
    private val changed: suspend (ModelFolder?, ModelFolder) -> Unit,
) {
    private val resolver = context.contentResolver
    var uri by mutableStateOf(prefs.getString("model_folder", null)?.toUri())
        private set
    var folder by mutableStateOf<ModelFolder?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<Int?>(null)
        private set

    fun choose(choice: Uri) {
        if (!canChange() || busy) return
        val existingGrant = resolver.persistedUriPermissions.any { it.uri == choice && it.isReadPermission }
        try {
            resolver.takePersistableUriPermission(choice, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            message = R.string.local_folder_error
            return
        }
        read(choice, existingGrant)
    }

    fun refresh() {
        val current = uri ?: return
        if (!busy && canChange()) read(current, true)
    }

    private fun read(choice: Uri, existingGrant: Boolean) {
        busy = true
        message = null
        val previous = uri
        scope.launch {
            var accepted = false
            try {
                val scanned = withContext(Dispatchers.IO) { scanModelFolder(resolver, choice) }
                if (!canChange()) return@launch
                val old = folder
                prefs.edit { putString("model_folder", choice.toString()) }
                folder = scanned
                uri = choice
                accepted = true
                changed(old, scanned)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = R.string.local_folder_error
            } finally {
                if (!accepted && !existingGrant) release(choice)
                if (accepted && previous != null && previous != choice) release(previous)
                busy = false
            }
        }
    }

    private fun release(uri: Uri) {
        runCatching { resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
}
