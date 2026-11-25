package com.zirakusa.gatewayhub.assistant.local

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.net.toUri
import com.zirakusa.gatewayhub.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LocalModels(
    context: Context,
    private val scope: CoroutineScope,
    private val canChange: () -> Boolean,
    private val onUnselected: suspend () -> Unit
) {
    private val app = context.applicationContext
    private val prefs = context.getSharedPreferences("assistant", Context.MODE_PRIVATE)
    private val store = ModelStore(context)
    private val documents = LinkedModelFiles(app)

    val supported = LocalLanguageModel.supported
    var installed by mutableStateOf(LocalModel.entries.filter { store.installed(it) != null }.toSet())
        private set
    var selected by mutableStateOf(LocalModel.entries.firstOrNull { it.name == prefs.getString("local_model", null) && it in installed })
        private set
    var linked by mutableStateOf(LinkedModel.load(prefs.getString("linked_models", null)))
        private set
    var selectedLinked by mutableStateOf(linked.firstOrNull { it.id == prefs.getString("local_model", null) })
        private set
    val hasModel get() = selected != null || selectedLinked != null
    internal val directory = ModelFolderAccess(app, prefs, scope, canChange, ::updateFolder)
    internal val downloads = ModelDownloads(scope, supported)
    var linkMessage by mutableStateOf<Int?>(null)
        private set

    init {
        if (supported) directory.refresh()
    }

    fun file(model: LocalModel) = store.installed(model)

    fun download(model: LocalModel) = downloads.start(builtIn = model) { progress ->
        store.download(model, progress)
        installed = LocalModel.entries.filter { store.installed(it) != null }.toSet()
        if (!hasModel) select(model)
    }

    internal fun download(model: RepositoryModel) = downloads.start(remote = model) { progress ->
        val file = store.download(model, progress)
        val installed = LinkedModel(Uri.fromFile(file).toString(), model.name, model.size, Template.guess(model.name))
        rememberLinked(installed)
        if (!hasModel) selectLinked(installed)
    }

    internal fun isRepositoryModel(model: LinkedModel) = store.repositoryFile(model.uri.toUri()) != null

    private suspend fun updateFolder(old: ModelFolder?, new: ModelFolder) {
        val current = prefs.getString("local_model", null)
        val removed = old?.models.orEmpty().filter { prior -> new.models.none { it.uri == prior.uri } }
        if (selected == null) selectedLinked = (linked + new.models).firstOrNull { it.id == current }
        if (removed.any { it.id == current } && selectedLinked == null) {
            prefs.edit { remove("local_model") }
            onUnselected()
        }
        documents.close(removed.map(LinkedModel::uri))
    }

    private fun rememberLinked(model: LinkedModel) {
        linked = linked.filter { it.uri != model.uri } + model
        prefs.edit { putString("linked_models", LinkedModel.save(linked)) }
    }

    fun link(uri: android.net.Uri) {
        linkMessage = null
        if (!canChange() || !supported) return
        val model = documents.inspect(uri)
        if (model == null) { linkMessage = R.string.local_link_not_gguf; return }
        if (!documents.retain(uri)) { linkMessage = R.string.local_link_failed; return }
        rememberLinked(model)
        if (!hasModel) selectLinked(model)
    }

    fun selectLinked(model: LinkedModel) {
        if (!canChange() || !supported || (model !in linked && model !in directory.folder?.models.orEmpty())) return
        selected = null
        selectedLinked = model
        prefs.edit { putString("local_model", model.id) }
    }

    /** Unlinks shared documents and removes app-owned downloads. */
    fun unlink(model: LinkedModel) {
        if (!canChange() || model !in linked) return
        val uri = model.uri.toUri()
        val downloaded = store.repositoryFile(uri)
        if (downloaded != null && downloaded.exists() && !downloaded.delete()) {
            downloads.deleteFailed()
            return
        }
        linked = linked - model
        prefs.edit { putString("linked_models", LinkedModel.save(linked)) }
        if (downloaded == null) documents.release(uri)
        val selected = selectedLinked == model
        if (selected) {
            selectedLinked = null
            prefs.edit { remove("local_model") }
        }
        scope.launch {
            if (selected) onUnselected()
            documents.close(listOf(model.uri))
        }
    }

    fun current(): Pair<Template, java.io.File>? {
        selected?.let { m -> return file(m)?.let { m.template to it } }
        val m = selectedLinked ?: return null
        store.repositoryFile(m.uri.toUri())?.let { file ->
            return file.takeIf { it.isFile && it.length() == m.size }?.let { m.template to it }
        }
        return documents.path(m)?.let { m.template to it }
    }

    internal fun closeDocuments() = documents.closeAll()

    fun select(model: LocalModel) {
        if (!canChange() || !supported || model !in installed) return
        selectedLinked = null
        selected = model
        prefs.edit { putString("local_model", model.name) }
    }

    fun delete(model: LocalModel) {
        if (!canChange() || downloads.builtIn == model) return
        if (!store.delete(model)) {
            downloads.deleteFailed()
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
