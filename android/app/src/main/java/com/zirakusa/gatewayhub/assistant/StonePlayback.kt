package com.zirakusa.gatewayhub.assistant

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Executes the music action once. Only reads may be retried. */
internal class StonePlayback(private val hands: Hands, private val io: CoroutineDispatcher?) {
    data class Song(val id: String, val title: String, val artist: String, val album: String) {
        val label get() = listOf(title, artist, album).filter(String::isNotBlank).joinToString(" · ")
    }
    data class Result(val reply: Reply, val ok: Boolean, val options: List<Song> = emptyList(), val volumeBlocked: Boolean = false)
    private val app = Catalog.byId("stone")
    val stages = linkedMapOf<String, String>()

    suspend fun run(command: Command, selected: Song? = null, skipVolume: Boolean = false): Result {
        if (!hands.installed(app)) return Result(Reply(Say.NOT_INSTALLED, listOf(app.name)), false)
        hands.cancelMusicRetry()
        val query = command["query"]
        var label = selected?.label
        var id = selected?.id
        try {
            if (query != null && id == null) {
                val matches = ready("library_search", JSONObject(mapOf("query" to query, "kind" to "audio", "limit" to MusicPick.MAX)))
                val list = matches.optJSONArray("results") ?: return fail("Stone devolvió una búsqueda sin resultados verificables.")
                val songs = (0 until list.length()).mapNotNull { list.optJSONObject(it) }.filterNot { it.optBoolean("isVideo") }
                    .map { Song(it.optString("id"), it.label("title"), it.label("artist"), it.label("album")) }
                    .filter { it.id.isNotBlank() && ',' !in it.id && it.title.isNotBlank() }.distinctBy { it.id }
                if (songs.isEmpty()) {
                    stages["search"] = "not_found"
                    return fail("No encontré «$query» en tu biblioteca de Stone.")
                }
                val exact = songs.filter { Words.normalize(it.title) == Words.normalize(query) }
                val complete = matches.optInt("total", list.length()) <= list.length()
                val options = if (complete) exact.ifEmpty { songs } else songs
                val match = options.singleOrNull().takeIf { complete } ?: return Result(
                    Reply(Say.TOOL_RESULT, listOf("Encontré varias coincidencias para «$query». Elige una canción.")), false, options
                ).also { stages["search"] = "ambiguous" }
                label = match.label
                id = match.id
                stages["search"] = "matched"
            } else if (command["equalizer"] != null || selected != null) {
                ready("player_state")
            }
        } catch (error: Hands.ToolError) {
            stages["prepare"] = error.code
            return fail("Stone no pudo preparar la reproducción (${error.code}).")
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return fail("Stone no pudo preparar la reproducción (STONE_UNAVAILABLE).")
        }
        val volume = command.int("volume")
        if (volume != null && !skipVolume && !hands.setVolume(volume)) {
            stages["volume"] = "denied"
            return Result(Reply(Say.TOOL_RESULT, listOf("No pude ajustar el volumen a $volume %. ¿Reproduzco con el volumen actual?")),
                false, options = listOfNotNull(selected ?: id?.let { Song(it, label.orEmpty(), "", "") }), volumeBlocked = true)
        }

        val appliedVolume = volume?.let { hands.volume() }.takeUnless { skipVolume }
        if (volume != null) stages["volume"] = appliedVolume?.toString() ?: "kept"
        val settings = mutableListOf<String>()
        if (skipVolume) settings += "Se mantuvo el volumen actual."
        val eq = command["equalizer"]
        if (eq != null) {
            val error = toolError {
                val result = call("equalizer_set_enabled", JSONObject().put("enabled", eq == "1"))
                if (!result.optBoolean("ok")) throw Hands.ToolError("EQ_NOT_CONFIRMED", "")
            }
            stages["equalizer"] = error ?: if (eq == "1") "enabled" else "disabled"
            settings += if (error != null) "No pude cambiar el ecualizador ($error)."
                else if (eq == "1") "Ecualizador activado." else "Ecualizador desactivado."
        }
        if (id == null && eq == null) {
            if (!hands.playMusic(null)) return fail("Stone no pudo iniciar la reproducción.", appliedVolume)
            stages["play"] = "requested"
            returnToArka(command)
            return Result(Reply(Say.PLAYING, volume = appliedVolume), true)
        }
        val error = toolError {
            val result = if (id == null) call("player_play") else call("player_play_songs", JSONObject().put("songIds", id))
            if (!result.optBoolean("ok")) throw Hands.ToolError("PLAY_NOT_CONFIRMED", "")
        }
        if (error != null) {
            stages["play"] = error
            return fail((settings + "Stone no pudo reproducir ($error).").joinToString(" "), appliedVolume)
        }
        val verified = (0..4).any { attempt ->
            if (attempt > 0 && io != null) delay(150)
            var matches = false
            toolError {
                val state = call("player_state")
                matches = state.optBoolean("playing") && (id == null || state.optString("songId") == id)
            }
            matches
        }
        stages["play"] = if (verified) "verified" else "unverified"
        returnToArka(command)
        val playback = if (verified) "Suena ${label?.let { "«$it»" } ?: "la música"}."
            else "Stone aceptó la reproducción${label?.let { " de «$it»" }.orEmpty()}, pero no pude verificarla."
        val complete = verified && settings.none { it.startsWith("No pude") }
        return Result(Reply(Say.TOOL_RESULT, listOf((listOf(playback) + settings).joinToString(" ")), volume = appliedVolume), complete)
    }

    private fun returnToArka(command: Command) {
        if (command["background"] == "1") {
            hands.comeBack()
            stages["return"] = "requested"
        }
    }

    private fun JSONObject.label(key: String) = optString(key).takeUnless { it == "null" }.orEmpty()

    private fun fail(message: String, volume: Int? = null) = Result(Reply(Say.TOOL_RESULT, listOf(message), volume = volume), false)

    private suspend fun ready(tool: String, args: JSONObject = JSONObject()): JSONObject {
        try { return call(tool, args) }
        catch (error: Hands.ToolError) {
            if (error.code != "STONE_CLOSED" || io == null || !hands.open(app)) throw error
        }
        repeat(12) {
            delay(250)
            try { return call(tool, args) }
            catch (error: Hands.ToolError) { if (error.code != "STONE_CLOSED") throw error }
        }
        throw Hands.ToolError("STONE_CLOSED", "")
    }

    private suspend fun call(name: String, args: JSONObject = JSONObject()): JSONObject {
        fun invoke() = try {
            val result = hands.tool(app, name, args) ?: throw Hands.ToolError("TOOL_UNAVAILABLE", "")
            result.optJSONObject("error")?.let { throw Hands.ToolError(it.optString("code", "TOOL_ERROR"), "") }
            result
        } catch (_: Hands.ToolOff) {
            throw Hands.ToolError("MCP_LOCAL_DESACTIVADO", "")
        } catch (error: Hands.ToolError) {
            throw error
        } catch (_: Exception) {
            throw Hands.ToolError("STONE_UNAVAILABLE", "")
        }
        return if (io == null) invoke() else withContext(io) { invoke() }
    }

    private suspend fun toolError(block: suspend () -> Unit): String? =
        try { block(); null } catch (error: Hands.ToolError) { error.code }
}
