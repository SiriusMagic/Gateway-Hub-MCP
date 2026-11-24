package com.zirakusa.gatewayhub.assistant.local

import android.content.Context
import android.os.Bundle
import androidx.core.net.toUri
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.assistant.Catalog
import com.zirakusa.gatewayhub.data.LocalApp
import com.zirakusa.gatewayhub.data.PackageScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal enum class PhoneMcpStatus(val label: Int) {
    READY(R.string.phone_mcp_ready),
    OFF(R.string.phone_mcp_off),
    SIGNATURE(R.string.phone_mcp_signature),
    DECLARED(R.string.phone_mcp_declared),
    UNAVAILABLE(R.string.phone_mcp_unavailable),
    MISSING(R.string.phone_mcp_stone_missing),
    /** A family app answered with its tools (through its hub link). */
    LISTED(R.string.phone_mcp_ready),
    /** Its «Redireccionar a Hub» is off. */
    LINK_OFF(R.string.phone_mcp_link_off)
}

internal data class PhoneMcpApp(val app: LocalApp, val status: PhoneMcpStatus, val tools: List<McpTool> = emptyList())
/** One tool of a family app on this phone; [key] names the app («video», «reloj»…). */
internal data class PhoneTool(val app: LocalApp, val key: String, val tool: McpTool)

internal class PhoneMcpError(val status: PhoneMcpStatus) : Exception()

/** Discovers Stone over Android IPC. This client has no URL, relay or Hub credential. */
internal class PhoneMcp(private val context: Context) {
    private val scanner = PackageScanner(context)

    suspend fun inventory(): List<PhoneMcpApp> = withContext(Dispatchers.IO) {
        scanner.installed().sortedBy { it.label.lowercase() }.map(::inspect)
    }

    suspend fun stone(): PhoneMcpApp = withContext(Dispatchers.IO) {
        val apps = scanner.installed().filter { isStone(it) }.map(::inspect)
        apps.firstOrNull { it.status == PhoneMcpStatus.READY }
            ?: apps.firstOrNull { it.app.localMcpAuthority != null }
            ?: apps.firstOrNull()
            ?: throw PhoneMcpError(PhoneMcpStatus.MISSING)
    }

    /** Every tool of the family apps that answer through their hub link (same developer, «Redireccionar a Hub» on). */
    suspend fun allTools(): List<PhoneTool> = withContext(Dispatchers.IO) {
        scanner.installed().filter { it.sameDeveloper && it.linkAuthority != null }.flatMap { app ->
            val key = Catalog.byPackage(app.packageName)?.id ?: app.packageName.substringAfterLast('.')
            family(app).tools.map { PhoneTool(app, key, it) }
        }
    }

    /** Runs one family tool through its app's hub link; its structuredContent. Arguments are checked against its schema. */
    suspend fun callFamily(target: PhoneTool, arguments: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        require(McpArguments.valid(target.tool.schema, arguments))
        val authority = target.app.linkAuthority ?: throw PhoneMcpError(PhoneMcpStatus.DECLARED)
        val request = JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "tools/call")
            .put("params", JSONObject().put("name", target.tool.name).put("arguments", arguments)).toString()
        val bundle = try {
            context.contentResolver.call("content://$authority".toUri(), "tool", null, Bundle().apply { putString("request", request) })
        } catch (_: SecurityException) { throw PhoneMcpError(PhoneMcpStatus.SIGNATURE) } ?: throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
        when (bundle.getString("error")) {
            null -> Unit
            "REDIRECT_OFF" -> throw PhoneMcpError(PhoneMcpStatus.LINK_OFF)
            else -> throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
        }
        val result = JSONObject(bundle.getString("response") ?: throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)).optJSONObject("result")
            ?: throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
        if (result.optBoolean("isError")) throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
        result.optJSONObject("structuredContent") ?: JSONObject()
    }

    suspend fun call(app: PhoneMcpApp, selection: McpSelection): JSONObject = withContext(Dispatchers.IO) {
        require(isStone(app.app) && app.status == PhoneMcpStatus.READY)
        val tool = app.tools.single { it.name == selection.tool.name }
        require(tool.readOnly && McpArguments.valid(tool.schema, selection.arguments))
        val params = JSONObject().put("name", tool.name).put("arguments", selection.arguments)
        val result = rpc(app.app, "tools/call", params)
        if (result.optBoolean("isError")) throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
        result.optJSONObject("structuredContent") ?: throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
    }

    private fun isStone(app: LocalApp) = app.prefix == "music_" && app.packageName in Catalog.stoneMcpPackages

    private fun inspect(app: LocalApp): PhoneMcpApp {
        if (!app.sameDeveloper) return PhoneMcpApp(app, PhoneMcpStatus.SIGNATURE)
        if (!isStone(app) || app.localMcpAuthority == null) return family(app)
        return try {
            val result = rpc(app, "tools/list")
            val list = result.optJSONArray("tools") ?: JSONArray()
            val tools = (0 until list.length()).mapNotNull { i ->
                list.optJSONObject(i)?.let(McpTool::parse)?.takeIf { it.readOnly && it.name in STONE_TOOLS }
            }
            PhoneMcpApp(app, if (tools.isEmpty()) PhoneMcpStatus.UNAVAILABLE else PhoneMcpStatus.READY, tools)
        } catch (error: PhoneMcpError) {
            PhoneMcpApp(app, error.status)
        }
    }

    /**
     * Any other family app (Stone Video, Reloj, Notas…): its tools as it lists them through its hub link, the same channel
     * Lucía uses on the phone. Only listed here; the local model still calls Stone alone.
     */
    private fun family(app: LocalApp): PhoneMcpApp {
        val authority = app.linkAuthority ?: return PhoneMcpApp(app, PhoneMcpStatus.DECLARED)
        val request = JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "tools/list").toString()
        return try {
            val bundle = context.contentResolver.call("content://$authority".toUri(), "tool", null, Bundle().apply { putString("request", request) })
                ?: return PhoneMcpApp(app, PhoneMcpStatus.DECLARED)
            when (bundle.getString("error")) {
                null -> Unit
                "REDIRECT_OFF" -> return PhoneMcpApp(app, PhoneMcpStatus.LINK_OFF)
                else -> return PhoneMcpApp(app, PhoneMcpStatus.UNAVAILABLE)
            }
            val list = JSONObject(bundle.getString("response") ?: return PhoneMcpApp(app, PhoneMcpStatus.DECLARED))
                .optJSONObject("result")?.optJSONArray("tools") ?: JSONArray()
            val tools = (0 until list.length()).mapNotNull { list.optJSONObject(it)?.let(McpTool::parse) }
            PhoneMcpApp(app, if (tools.isEmpty()) PhoneMcpStatus.UNAVAILABLE else PhoneMcpStatus.LISTED, tools)
        } catch (_: SecurityException) {
            PhoneMcpApp(app, PhoneMcpStatus.SIGNATURE)
        } catch (_: Exception) {
            PhoneMcpApp(app, PhoneMcpStatus.UNAVAILABLE)
        }
    }

    private fun rpc(app: LocalApp, method: String, params: JSONObject? = null): JSONObject {
        val authority = app.localMcpAuthority ?: throw PhoneMcpError(PhoneMcpStatus.DECLARED)
        if (!app.sameDeveloper) throw PhoneMcpError(PhoneMcpStatus.SIGNATURE)
        val request = JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method)
        if (params != null) request.put("params", params)
        return try {
            val bundle = context.contentResolver.call("content://$authority".toUri(), "rpc", null,
                Bundle().apply { putString("request", request.toString()) })
            when (bundle?.getString("error")) {
                "LOCAL_MCP_OFF" -> throw PhoneMcpError(PhoneMcpStatus.OFF)
                "NOT_SAME_DEVELOPER" -> throw PhoneMcpError(PhoneMcpStatus.SIGNATURE)
                null -> Unit
                else -> throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
            }
            val body = bundle?.getString("response") ?: throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
            val response = JSONObject(body)
            if (response.optInt("id") != 1 || response.has("error")) throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
            response.getJSONObject("result")
        } catch (error: PhoneMcpError) {
            throw error
        } catch (_: SecurityException) {
            throw PhoneMcpError(PhoneMcpStatus.SIGNATURE)
        } catch (_: Exception) {
            throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
        }
    }

    companion object { val STONE_TOOLS = setOf("library_search", "player_state") }
}
