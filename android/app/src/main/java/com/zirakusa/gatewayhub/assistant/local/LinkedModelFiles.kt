package com.zirakusa.gatewayhub.assistant.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.net.toUri
import java.io.File

/** Keeps a linked document open while the native model engine uses its descriptor. */
internal class LinkedModelFiles(context: Context) {
    private val resolver = context.contentResolver
    private val open = HashMap<String, ParcelFileDescriptor>()

    fun inspect(uri: Uri): LinkedModel? {
        val (name, size) = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty() to cursor.getLong(1) else null
            }
        }.getOrNull() ?: return null
        if (!LinkedModel.accepts(name, size) || !standaloneGguf(name)) return null
        return LinkedModel(uri.toString(), name, size, Template.guess(name))
    }

    fun retain(uri: Uri): Boolean = runCatching {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }.isSuccess

    fun release(uri: Uri) {
        runCatching { resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    fun path(model: LinkedModel): File? {
        val descriptor = synchronized(open) {
            open[model.uri] ?: runCatching { resolver.openFileDescriptor(model.uri.toUri(), "r") }
                .getOrNull()?.also { open[model.uri] = it }
        } ?: return null
        return File("/proc/self/fd/${descriptor.fd}")
    }

    fun closeAll() {
        val handles = synchronized(open) { open.values.toList().also { open.clear() } }
        handles.forEach { runCatching { it.close() } }
    }

    fun close(uris: List<String>) {
        val handles = synchronized(open) { uris.mapNotNull(open::remove) }
        handles.forEach { runCatching { it.close() } }
    }
}
