package com.zirakusa.gatewayhub.assistant.local

import android.content.Context
import android.os.Bundle
import androidx.core.net.toUri
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.assistant.Catalog
import com.zirakusa.gatewayhub.assistant.Prefs
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.data.PackageScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal enum class PhoneMcpStatus(val label: Int) {
    /** Stone answered with the two tools the local model may ask. */
    READY(R.string.phone_mcp_ready),
    /** «Permitir MCP local» is off in Arka. */
    OFF(R.string.phone_mcp_off),
    SIGNATURE(R.string.phone_mcp_signature),
    DECLARED(R.string.phone_mcp_declared),
    UNAVAILABLE(R.string.phone_mcp_unavailable),
    MISSING(R.string.phone_mcp_stone_missing),
    /** A family app answered with its tools (through its hub link). */
    LISTED(R.string.phone_mcp_ready),
    /** An app from before Arka ran its Modo MCP: it only answers with «Redireccionar a Hub» on. */
    LINK_OFF(R.string.phone_mcp_link_off)
}

/** [remote]: its public link (the Modo MCP through the hub) is on; null when the app is too old to say. */
internal data class PhoneMcpApp(val app: LocalApp, val status: PhoneMcpStatus, val tools: List<McpTool> = emptyList(), val remote: Boolean? = null)
/** One tool of a family app on this phone; [key] names the app («video», «reloj»…). */
internal data class PhoneTool(val app: LocalApp, val key: String, val tool: McpTool)

internal class PhoneMcpError(val status: PhoneMcpStatus) : Exception()

/**
 * Arka runs the family's Modo MCP over Android IPC, through each app's hub link (signature-protected): it lists and calls
 * tools for Lucía (only with «Permitir MCP local» on) and turns each app's public link on or off («Activar MCP»).
 * This client has no URL, relay or hub credential.
 */
internal class PhoneMcp(private val context: Context) {
    private val scanner = PackageScanner(context)
    private val prefs = Prefs(context)
    private fun family() = scanner.installed().filter { it.sameDeveloper && it.linkAuthority != null }

    suspend fun inventory(): List<PhoneMcpApp> = withContext(Dispatchers.IO) {
        scanner.installed().sortedBy { it.label.lowercase() }.map(::inspect)
    }

    suspend fun stone(): PhoneMcpApp = withContext(Dispatchers.IO) {
        if (!prefs.localMcp) throw PhoneMcpError(PhoneMcpStatus.OFF)
        val apps = scanner.installed().filter { isStone(it) }.map(::inspect)
        apps.firstOrNull { it.status == PhoneMcpStatus.READY } ?: apps.firstOrNull() ?: throw PhoneMcpError(PhoneMcpStatus.MISSING)
    }

    /** Every tool of the family apps that answer through their hub link (same developer). */
    suspend fun allTools(): List<PhoneTool> = withContext(Dispatchers.IO) {
        if (!prefs.localMcp) return@withContext emptyList()
        family().flatMap { app ->
            val key = Catalog.byPackage(app.packageName)?.id ?: app.packageName.substringAfterLast('.')
            list(app).tools.map { PhoneTool(app, key, it) }
        }
    }

    /** Runs one family tool through its app's hub link; its structuredContent. Arguments are checked against its schema. */
    suspend fun callFamily(target: PhoneTool, arguments: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        require(McpArguments.valid(target.tool.schema, arguments))
        run(target.app, target.tool.name, arguments)
    }

    /** The local model asks Stone: only its two read-only tools. */
    suspend fun call(app: PhoneMcpApp, selection: McpSelection): JSONObject = withContext(Dispatchers.IO) {
        require(isStone(app.app) && app.status == PhoneMcpStatus.READY)
        val tool = app.tools.single { it.name == selection.tool.name }
        require(tool.readOnly && McpArguments.valid(tool.schema, selection.arguments))
        run(app.app, tool.name, selection.arguments)
    }

    /** «Activar MCP» for one app: its public link on or off. The new state, or null when the app is too old to do it. */
    suspend fun setRemote(app: LocalApp, on: Boolean): Boolean? = withContext(Dispatchers.IO) {
        val answer = ask(app, if (on) "connect" else "disconnect") ?: return@withContext null
        when {
            answer.containsKey("remote") -> answer.getBoolean("remote")
            answer.getString("link") != null -> true
            else -> null
        }
    }

    /** «Activar MCP» for the whole family: how many apps did it (the rest are too old or not ours). */
    suspend fun setRemoteAll(on: Boolean): Int = withContext(Dispatchers.IO) { family().count { setRemote(it, on) == on } }

    private fun isStone(app: LocalApp) = app.prefix == "music_" && app.packageName in Catalog.stoneMcpPackages

    private fun inspect(app: LocalApp): PhoneMcpApp {
        if (!app.sameDeveloper) return PhoneMcpApp(app, PhoneMcpStatus.SIGNATURE)
        val listed = list(app)
        val remote = ask(app, "status")?.takeIf { it.containsKey("remote") }?.getBoolean("remote")
        if (!isStone(app) || listed.status != PhoneMcpStatus.LISTED) return listed.copy(remote = remote)
        val tools = listed.tools.filter { it.readOnly && it.name in STONE_TOOLS }
        return PhoneMcpApp(app, if (tools.isEmpty()) PhoneMcpStatus.UNAVAILABLE else PhoneMcpStatus.READY, tools, remote)
    }

    /** A family app's tools as it lists them through its hub link, the same channel Lucía uses on the phone. */
    private fun list(app: LocalApp): PhoneMcpApp = try {
        val result = rpc(app, JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "tools/list"))
        val list = result.optJSONArray("tools") ?: JSONArray()
        val tools = (0 until list.length()).mapNotNull { list.optJSONObject(it)?.let(McpTool::parse) }
        PhoneMcpApp(app, if (tools.isEmpty()) PhoneMcpStatus.UNAVAILABLE else PhoneMcpStatus.LISTED, tools)
    } catch (error: PhoneMcpError) {
        PhoneMcpApp(app, error.status)
    }

    private fun run(app: LocalApp, name: String, arguments: JSONObject): JSONObject {
        if (!prefs.localMcp) throw PhoneMcpError(PhoneMcpStatus.OFF)
        val result = rpc(app, JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "tools/call")
            .put("params", JSONObject().put("name", name).put("arguments", arguments)))
        if (result.optBoolean("isError")) throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
        return result.optJSONObject("structuredContent") ?: JSONObject()
    }

    private fun rpc(app: LocalApp, request: JSONObject): JSONObject {
        val bundle = ask(app, "tool", Bundle().apply { putString("request", request.toString()) }) ?: throw PhoneMcpError(PhoneMcpStatus.DECLARED)
        when (bundle.getString("error")) {
            null -> Unit
            "LOCAL_DISABLED" -> throw PhoneMcpError(PhoneMcpStatus.OFF)
            "REDIRECT_OFF" -> throw PhoneMcpError(PhoneMcpStatus.LINK_OFF)
            "NOT_SAME_DEVELOPER" -> throw PhoneMcpError(PhoneMcpStatus.SIGNATURE)
            else -> throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
        }
        val response = runCatching { JSONObject(bundle.getString("response").orEmpty()) }.getOrNull() ?: throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
        if (response.has("error")) throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
        return response.optJSONObject("result") ?: throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
    }

    /** One call to the app's hub link; null when it has none or answered nothing (too old). */
    private fun ask(app: LocalApp, method: String, extras: Bundle? = null): Bundle? {
        val authority = app.linkAuthority ?: return null
        if (!app.sameDeveloper) throw PhoneMcpError(PhoneMcpStatus.SIGNATURE)
        return try {
            context.contentResolver.call("content://$authority".toUri(), method, null, extras)
        } catch (_: SecurityException) {
            throw PhoneMcpError(PhoneMcpStatus.SIGNATURE)
        } catch (_: Exception) {
            null
        }
    }

    companion object { val STONE_TOOLS = setOf("library_search", "player_state") }
}
