package com.zirakusa.gatewayhub.assistant

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Only the signed on-device MCP channel executes Stone playback orders. */
internal class StonePlayback(private val hands: Hands, private val io: CoroutineDispatcher?) {
    data class Song(val id: String, val title: String, val artist: String, val album: String) {
        val label get() = listOf(title, artist, album).filter(String::isNotBlank).joinToString(" · ")
    }
    data class Result(val reply: Reply, val ok: Boolean, val options: List<Song> = emptyList(), val volumeBlocked: Boolean = false)
    private val app = Catalog.byId("stone")
    val stages = linkedMapOf<String, String>()

    suspend fun run(command: Command, selected: Song? = null, skipVolume: Boolean = false): Result {
        if (!hands.installed(app)) return Result(Reply(Say.NOT_INSTALLED, listOf(app.name)), false)
        stages["channel"] = "mcp_local"
        return if (command.action == Action.MEDIA) transport(command) else StoneTitleAction.run(
            command, selected, skipVolume, ::ready, { returnToArka(command) }, stages
        )
    }

    private suspend fun transport(command: Command): Result {
        val key = MediaKey.valueOf(command["key"]!!)
        val tool = when (key) {
            MediaKey.PAUSE -> "player_pause"
            MediaKey.RESUME -> "player_play"
            MediaKey.NEXT -> "player_next"
            MediaKey.PREVIOUS -> "player_previous"
            MediaKey.RESTART -> "player_seek"
        }
        val args = JSONObject()
        if (key == MediaKey.RESTART) args.put("positionSeconds", 0)
        var volume: Int? = null
        return try {
            val target = command.int("volume") ?: command["volume_step"]?.let { step ->
                val current = ready("player_state", JSONObject()).optInt("volumePercent", -1)
                if (current !in 0..100) throw Hands.ToolError("VOLUME_NOT_CONFIRMED", "")
                Volume.apply(Volume.Change.Step(step == "up"), current)
            }
            if (target != null) {
                if (!ready("player_set_volume", JSONObject().put("percent", target)).optBoolean("ok")) {
                    throw Hands.ToolError("VOLUME_NOT_CONFIRMED", "")
                }
                volume = ready("player_state", JSONObject()).optInt("volumePercent", -1).takeIf { it >= 0 }
                stages["volume"] = volume?.toString() ?: "unverified"
            }
            if (!ready(tool, args).optBoolean("ok")) throw Hands.ToolError("PLAY_NOT_CONFIRMED", "")
            stages["play"] = "OK"
            returnToArka(command)
            Result(Reply(Say.MEDIA_DONE, listOf(key.name), volume = volume), true)
        } catch (error: Hands.ToolError) {
            stages["play"] = error.code
            Result(StoneTitleAction.failure(error.code).copy(volume = volume), false)
        }
    }

    private fun returnToArka(command: Command) {
        if (command["background"] == "1") {
            hands.comeBack()
            stages["return"] = "requested"
        }
    }

    private suspend fun ready(name: String, args: JSONObject): JSONObject {
        suspend fun attempt(): JSONObject? = try {
            call(name, args).takeUnless { it.optJSONObject("steps")?.optJSONObject("play")?.optString("code") == "PLAYER_NOT_READY" }
        } catch (error: Hands.ToolError) {
            if (error.code != "STONE_CLOSED" && error.code != "PLAYER_NOT_READY") throw error
            null
        }
        attempt()?.let { return it }
        if (io != null && hands.open(app)) repeat(12) {
            delay(250)
            attempt()?.let { return it }
        }
        throw Hands.ToolError("STONE_CLOSED", "")
    }

    private suspend fun call(name: String, args: JSONObject): JSONObject {
        fun invoke() = try {
            hands.tool(app, name, args) ?: throw Hands.ToolError("TOOL_UNAVAILABLE", "")
        } catch (_: Hands.ToolOff) {
            throw Hands.ToolError("MCP_LOCAL_DESACTIVADO", "")
        } catch (error: Hands.ToolError) {
            throw error
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            throw Hands.ToolError("STONE_UNAVAILABLE", "")
        }
        return if (io == null) invoke() else withContext(io) { invoke() }
    }
}
