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
    MISSING(R.string.phone_mcp_stone_missing)
}

internal data class PhoneMcpApp(val app: LocalApp, val status: PhoneMcpStatus, val tools: List<McpTool> = emptyList())
internal class PhoneMcpError(val status: PhoneMcpStatus) : Exception()

/** Discovers Stone over Android IPC. This client has no URL, relay or Hub credential. */
internal class PhoneMcp(private val context: Context) {
    private val scanner = PackageScanner(context)
    private val stonePackages = Catalog.byId("stone").packages

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

    suspend fun call(app: PhoneMcpApp, selection: McpSelection): JSONObject = withContext(Dispatchers.IO) {
        require(isStone(app.app) && app.status == PhoneMcpStatus.READY)
        val tool = app.tools.single { it.name == selection.tool.name }
        require(tool.readOnly && McpArguments.valid(tool.schema, selection.arguments))
        val params = JSONObject().put("name", tool.name).put("arguments", selection.arguments)
        val result = rpc(app.app, "tools/call", params)
        if (result.optBoolean("isError")) throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
        result.optJSONObject("structuredContent") ?: throw PhoneMcpError(PhoneMcpStatus.UNAVAILABLE)
    }

    private fun isStone(app: LocalApp) = app.prefix == "music_" && app.packageName in stonePackages

    private fun inspect(app: LocalApp): PhoneMcpApp {
        if (!app.sameDeveloper) return PhoneMcpApp(app, PhoneMcpStatus.SIGNATURE)
        if (!isStone(app) || app.localMcpAuthority == null) return PhoneMcpApp(app, PhoneMcpStatus.DECLARED)
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
