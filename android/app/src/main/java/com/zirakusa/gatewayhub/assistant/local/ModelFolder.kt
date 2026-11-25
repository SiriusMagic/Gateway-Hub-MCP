package com.zirakusa.gatewayhub.assistant.local

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

internal data class ModelFolder(val name: String, val models: List<LinkedModel>)

/** Reads the chosen directory through its Android tree grant, without moving its files. */
internal fun scanModelFolder(resolver: ContentResolver, tree: Uri): ModelFolder {
    val root = DocumentsContract.getTreeDocumentId(tree)
    val document = DocumentsContract.buildDocumentUriUsingTree(tree, root)
    val name = resolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: root.substringAfterLast(':').substringAfterLast('/')
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, root)
    val columns = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
    )
    val models = requireNotNull(resolver.query(children, columns, null, null, null)).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                if (cursor.getString(3) == DocumentsContract.Document.MIME_TYPE_DIR) continue
                val file = cursor.getString(1).orEmpty()
                val size = cursor.getLong(2)
                if (!LinkedModel.accepts(file, size) || !standaloneGguf(file)) continue
                val uri = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                add(LinkedModel(uri.toString(), file, size, Template.guess(file)))
            }
        }
    }
    return ModelFolder(name, models.sortedBy { it.name.lowercase() })
}
