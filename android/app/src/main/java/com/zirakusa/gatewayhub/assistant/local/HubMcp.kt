package com.zirakusa.gatewayhub.assistant.local

import java.io.IOException
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class AgentLink private constructor(val endpoint: URI, val token: String) {
    companion object {
        private val tokenPattern = Regex("gw_[A-Za-z0-9_-]{43}")

        fun parse(raw: String): AgentLink {
            val uri = URI(raw.trim())
            val token = uri.rawQuery?.removePrefix("token=").orEmpty()
            require(uri.scheme.equals("https", true) && uri.host != null && uri.path == "/mcp" &&
                uri.fragment == null && uri.userInfo == null && uri.rawQuery == "token=$token" && tokenPattern.matches(token))
            return AgentLink(URI("https", null, uri.host, uri.port, "/mcp", null, null), token)
        }

        fun restore(endpoint: String, token: String) = parse("$endpoint?token=$token")
    }
}

internal class HubMcp(private val link: AgentLink) {
    private var nextId = 0
    private var session: String? = null
    private var version: String? = null

    suspend fun discover(): List<McpTool> {
        val initialized = request("initialize", JSONObject()
            .put("protocolVersion", "2025-11-25").put("capabilities", JSONObject())
            .put("clientInfo", JSONObject().put("name", "lucia").put("version", "1.0")))
        version = initialized.getString("protocolVersion")
        request("notifications/initialized", notification = true)
        val tools = request("tools/list").getJSONArray("tools")
        return (0 until tools.length()).mapNotNull { McpTool.parse(tools.getJSONObject(it)) }
    }

    suspend fun call(tool: McpTool, args: JSONObject): String {
        require(McpArguments.valid(tool.schema, args))
        val result = request("tools/call", JSONObject().put("name", tool.name).put("arguments", args))
        val content = result.optJSONArray("content") ?: JSONArray()
        val text = (0 until content.length()).mapNotNull { index ->
            val part = content.optJSONObject(index)
            part?.optString("text")?.takeIf { part.optString("type") == "text" && it.isNotBlank() }
        }.joinToString("\n").take(4_000)
        if (result.optBoolean("isError")) throw McpFailure.Tool()
        return text.ifBlank { result.optJSONObject("structuredContent")?.toString()?.take(4_000).orEmpty() }
    }

    private suspend fun request(method: String, params: JSONObject? = null, notification: Boolean = false): JSONObject =
        withContext(Dispatchers.IO) {
            val id = ++nextId
            val request = JSONObject().put("jsonrpc", "2.0").put("method", method)
            if (!notification) request.put("id", id)
            if (params != null) request.put("params", params)
            val connection = (link.endpoint.toURL().openConnection() as HttpsURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 12_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer ${link.token}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json, text/event-stream")
                session?.let { setRequestProperty("Mcp-Session-Id", it) }
                version?.let { setRequestProperty("Mcp-Protocol-Version", it) }
            }
            try {
                connection.outputStream.use { it.write(request.toString().toByteArray()) }
                when (val status = connection.responseCode) {
                    401, 403 -> throw McpFailure.Authorization()
                    429 -> throw McpFailure.RateLimit()
                    !in 200..299 -> throw IOException("Hub HTTP $status")
                }
                if (method == "initialize") session = connection.getHeaderField("Mcp-Session-Id")
                if (notification) return@withContext JSONObject()
                val body = connection.inputStream.use { input ->
                    val bytes = ByteArray(1_048_577)
                    var offset = 0
                    while (offset < bytes.size) {
                        val read = input.read(bytes, offset, bytes.size - offset)
                        if (read == -1) break
                        offset += read
                    }
                    if (offset == bytes.size) throw IOException("Hub response too large")
                    String(bytes, 0, offset, Charsets.UTF_8)
                }
                val response = JSONObject(body)
                if (response.optInt("id", -1) != id || response.has("error")) throw IOException("Hub RPC failure")
                response.getJSONObject("result")
            } finally {
                connection.disconnect()
            }
        }
}

internal sealed class McpFailure : IOException() {
    class Authorization : McpFailure()
    class RateLimit : McpFailure()
    class Unavailable : McpFailure()
    class Tool : McpFailure()
}
