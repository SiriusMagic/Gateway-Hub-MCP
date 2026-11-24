package com.zirakusa.gatewayhub.assistant.local

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.net.toUri
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
    private val app = context.applicationContext
    private val prefs = context.getSharedPreferences("assistant", Context.MODE_PRIVATE)
    /** The linked files opened this run (kept open while the app lives, so the model reads them in place). */
    private val open = HashMap<String, android.os.ParcelFileDescriptor>()
    private val store = ModelStore(context)
    private var job: Job? = null

    val supported = LocalLanguageModel.supported
    var installed by mutableStateOf(LocalModel.entries.filter { store.installed(it) != null }.toSet())
        private set
    var selected by mutableStateOf(LocalModel.entries.firstOrNull { it.name == prefs.getString("local_model", null) && it in installed })
        private set
    /** Models the person already had (a .gguf in Descargas…), used where they are. */
    var linked by mutableStateOf(LinkedModel.load(prefs.getString("linked_models", null)))
        private set
    /** The linked model in use (then [selected] is null). */
    var selectedLinked by mutableStateOf(linked.firstOrNull { it.id == prefs.getString("local_model", null) })
        private set
    /** A model is chosen: one downloaded here or one linked. */
    val hasModel get() = selected != null || selectedLinked != null
    var linkMessage by mutableStateOf<Int?>(null)
        private set
    var downloading by mutableStateOf<LocalModel?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
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
                if (!hasModel) select(model)
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

    /**
     * Uses the .gguf at [uri] (picked by the person) where it is: Android keeps the permission to read it, nothing is
     * copied. The same file twice is one entry. Chosen right away when no model is.
     */
    fun link(uri: android.net.Uri) {
        linkMessage = null
        if (!canChange() || !supported) return
        val resolver = app.contentResolver
        val (name, size) = runCatching {
            resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0).orEmpty() to c.getLong(1) else null
            }
        }.getOrNull() ?: ("" to 0L)
        if (!LinkedModel.accepts(name, size)) { linkMessage = R.string.local_link_not_gguf; return }
        try { resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        catch (_: SecurityException) { linkMessage = R.string.local_link_failed; return }
        val model = LinkedModel(uri.toString(), name, size, Template.guess(name))
        linked = linked.filter { it.uri != model.uri } + model
        prefs.edit { putString("linked_models", LinkedModel.save(linked)) }
        if (!hasModel) selectLinked(model)
    }

    fun selectLinked(model: LinkedModel) {
        if (!canChange() || !supported || model !in linked) return
        selected = null
        selectedLinked = model
        prefs.edit { putString("local_model", model.id) }
    }

    /** Stops using a linked file; the file itself stays where it is. */
    fun unlink(model: LinkedModel) {
        if (!canChange()) return
        linked = linked - model
        prefs.edit { putString("linked_models", LinkedModel.save(linked)) }
        runCatching { app.contentResolver.releasePersistableUriPermission(model.uri.toUri(), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        synchronized(open) { open.remove(model.uri) }?.let { pfd -> scope.launch { onUnselected(); runCatching { pfd.close() } } }
        if (selectedLinked == model) {
            selectedLinked = null
            prefs.edit { remove("local_model") }
            scope.launch { onUnselected() }
        }
    }

    /**
     * What to run now: the template and a path the native side can open. A linked file is read through its open
     * descriptor (/proc/self/fd/N), without copying. Null when nothing is chosen or the file is gone.
     */
    fun current(): Pair<Template, java.io.File>? {
        selected?.let { m -> return file(m)?.let { m.template to it } }
        val m = selectedLinked ?: return null
        val pfd = synchronized(open) {
            open[m.uri] ?: runCatching { app.contentResolver.openFileDescriptor(m.uri.toUri(), "r") }.getOrNull()?.also { open[m.uri] = it }
        } ?: return null
        return m.template to java.io.File("/proc/self/fd/${pfd.fd}")
    }

    fun select(model: LocalModel) {
        if (!canChange() || !supported || model !in installed) return
        selectedLinked = null
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
