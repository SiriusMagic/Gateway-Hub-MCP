package com.zirakusa.gatewayhub.assistant.local

import java.io.IOException
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val splitFile = Regex("-[0-9]{5}-of-[0-9]{5}\\.gguf$", RegexOption.IGNORE_CASE)
internal fun standaloneGguf(name: String): Boolean =
    name.endsWith(".gguf", ignoreCase = true) &&
        !name.startsWith("mmproj", ignoreCase = true) && !splitFile.containsMatchIn(name)

internal data class RepositoryModel(
    val repository: String,
    val revision: String,
    val path: String,
    val size: Long,
    val sha256: String,
) {
    val name = path.substringAfterLast('/')
    val url: URL get() = HuggingFaceModels.url("/$repository/resolve/$revision/$path")
}

internal object HuggingFaceModels {
    private const val HOST = "huggingface.co"
    private val repoPath = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,95}/[A-Za-z0-9][A-Za-z0-9._-]{0,95}")
    private val commit = Regex("[0-9a-f]{40}")
    private val digest = Regex("[0-9a-f]{64}")

    fun repository(text: String): String? {
        val raw = text.trim().trimEnd('/')
        if (repoPath.matches(raw)) return raw
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        if (uri.scheme != "https" || uri.host != HOST || uri.port != -1 || uri.userInfo != null) return null
        return uri.path.removePrefix("/").takeIf(repoPath::matches)
    }

    internal fun url(path: String, query: String? = null): URL = URI("https", HOST, path, query, null).toURL()

    suspend fun files(repository: String): List<RepositoryModel> = withContext(Dispatchers.IO) {
        require(repoPath.matches(repository))
        val metadata = JSONObject(read(url("/api/models/$repository")))
        if (metadata.optBoolean("private") || metadata.opt("gated") in listOf("auto", "manual", true)) throw IOException()
        val revision = metadata.optString("sha")
        require(commit.matches(revision))
        parseFiles(repository, revision, JSONArray(read(url("/api/models/$repository/tree/$revision", "recursive=true"))))
    }

    internal fun parseFiles(repo: String, revision: String, tree: JSONArray): List<RepositoryModel> = buildList {
        for (index in 0 until tree.length()) {
            val entry = tree.optJSONObject(index) ?: continue
            if (entry.optString("type") != "file") continue
            val path = entry.optString("path")
            if (path.split('/').any { it.isBlank() || it == ".." || it == "." } || '\\' in path) continue
            val name = path.substringAfterLast('/')
            val lfs = entry.optJSONObject("lfs") ?: continue
            val sha = lfs.optString("oid")
            val size = lfs.optLong("size")
            if (standaloneGguf(name) && LinkedModel.accepts(name, size) && digest.matches(sha)) {
                add(RepositoryModel(repo, revision, path, size, sha))
            }
        }
    }.sortedBy { it.name.lowercase() }

    private fun read(url: URL): String {
        val connection = (url.openConnection() as HttpsURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
        }
        try {
            if (connection.responseCode != 200) throw IOException()
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val response = StringBuilder()
                val buffer = CharArray(4096)
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    response.appendRange(buffer, 0, count)
                    if (response.length > 2_000_000) throw IOException()
                }
                response.toString()
            }
        } finally {
            connection.disconnect()
        }
    }
}
