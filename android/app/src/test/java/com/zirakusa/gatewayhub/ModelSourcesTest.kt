package com.zirakusa.gatewayhub

import android.content.ContentProvider
import android.content.ContentValues
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import com.zirakusa.gatewayhub.assistant.local.HuggingFaceModels
import com.zirakusa.gatewayhub.assistant.local.acceptsVoiceModel
import com.zirakusa.gatewayhub.assistant.local.scanModelFolder
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowContentResolver
import com.zirakusa.gatewayhub.assistant.local.standaloneGguf
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModelSourcesTest {
    @Test fun repositorySourcesStayOnHuggingFace() {
        assertEquals("Qwen/Qwen2.5-1.5B-Instruct-GGUF", HuggingFaceModels.repository("Qwen/Qwen2.5-1.5B-Instruct-GGUF"))
        assertEquals("owner/model", HuggingFaceModels.repository("https://huggingface.co/owner/model/"))
        listOf("http://huggingface.co/owner/model", "https://huggingface.co.example/owner/model",
            "https://other@huggingface.co/owner/model", "../model", "https://huggingface.co/datasets/owner/model").forEach {
            assertNull(it, HuggingFaceModels.repository(it))
        }
    }

    @Test fun aChosenFolderListsModelFilesThroughItsTreeGrant() {
        val authority = "lucia.models.test"
        ShadowContentResolver.registerProviderInternal(authority, object : ContentProvider() {
            override fun onCreate() = true
            override fun getType(uri: Uri): String? = null
            override fun insert(uri: Uri, values: ContentValues?): Uri? = null
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
            override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
            override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                selectionArgs: Array<out String>?, sortOrder: String?): MatrixCursor {
                return MatrixCursor(requireNotNull(projection)).apply {
                    if (uri.lastPathSegment != "children") {
                        addRow(arrayOf<Any?>("model"))
                    } else {
                        addRow(arrayOf<Any?>("model/own.gguf", "Own.GGUF", 500_000_000L, "application/octet-stream"))
                        addRow(arrayOf<Any?>("model/folder.gguf", "folder.gguf", 500_000_000L, DocumentsContract.Document.MIME_TYPE_DIR))
                        addRow(arrayOf<Any?>("model/notes.pdf", "notes.pdf", 67_000_000L, "application/pdf"))
                    }
                }
            }
        })
        val tree = DocumentsContract.buildTreeDocumentUri(authority, "model")
        val folder = scanModelFolder(RuntimeEnvironment.getApplication().contentResolver, tree)
        assertEquals("model", folder.name)
        val model = folder.models.single()
        assertEquals("Own.GGUF", model.name)
        assertEquals(DocumentsContract.buildDocumentUriUsingTree(tree, "model/own.gguf").toString(), model.uri)
    }

    @Test fun onlyStandaloneFilesWithVerifiedMetadataAreOffered() {
        val sha = "a".repeat(64)
        val size = 454_000_000L
        fun file(path: String, hash: String = sha) = JSONObject().put("type", "file").put("path", path)
            .put("lfs", JSONObject().put("oid", hash).put("size", size))
        val revision = "b".repeat(40)
        val files = JSONArray(listOf(file("quant/My model.Q4.gguf"), file("mmproj-F16.gguf"),
            file("model-00001-of-00002.gguf"), file("../unexpected.gguf"), file("bad-hash.gguf", "weak"), file("model.safetensors")))
        val found = HuggingFaceModels.parseFiles("org/model", revision, files)
        val item = found.single()
        assertEquals("My model.Q4.gguf", item.name)
        assertEquals(size, item.size)
        assertEquals(sha, item.sha256)
        assertEquals("https://huggingface.co/org/model/resolve/$revision/quant/My%20model.Q4.gguf", item.url.toString())
        assertTrue(standaloneGguf("model.GGUF"))
        assertFalse(standaloneGguf("mmproj.gguf"))
    }
    @Test fun speechRepositoryUsesBinWithoutChangingTheConversationCatalog() {
        val sha = "b".repeat(64)
        val revision = "a".repeat(40)
        fun file(path: String, size: Long) = JSONObject().put("type", "file").put("path", path)
            .put("lfs", JSONObject().put("oid", sha).put("size", size))
        val tree = JSONArray(listOf(file("quant/Spanish.BIN", 206_820_789), file("talk.gguf", 500_000_000),
            file("../bad.bin", 200_000_000), file("tiny.bin", 31), file("oversize.bin", 2_000_000_000)))
        val voice = HuggingFaceModels.parseFiles("org/model", revision, tree,
            ::acceptsVoiceModel)
        assertEquals("Spanish.BIN", voice.single().name)
        assertEquals(sha, voice.single().sha256)
        assertEquals("talk.gguf", HuggingFaceModels.parseFiles("org/model", revision, tree).single().name)
    }
}
