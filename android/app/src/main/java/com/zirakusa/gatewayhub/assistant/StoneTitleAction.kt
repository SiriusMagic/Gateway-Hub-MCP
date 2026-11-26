package com.zirakusa.gatewayhub.assistant

import org.json.JSONObject

/** Compatibility with the signed single action implemented by Stone. A returned error must never cause replay. */
internal object StoneTitleAction {
    const val TOOL = "player_play_by_title"

    suspend fun run(
        command: Command, selected: StonePlayback.Song?, skipVolume: Boolean,
        call: suspend (String, JSONObject) -> JSONObject, returnToAssistant: () -> Unit,
        observed: MutableMap<String, String>
    ): StonePlayback.Result {
        val query = command["query"]
        val tool = if (query == null) "player_play_collection" else TOOL
        val args = JSONObject().put("segundoPlano", command["background"] == "1")
        if (query != null) args.put("titulo", query)
        else args.put("coleccion", if (command.action == Action.PLAY_RECOMMENDED) "diaria" else "biblioteca")
        if (!skipVolume) args.put("volumen", command.int("volume"))
        command["equalizer"]?.let { args.put("ecualizador", it == "1") }
        if (query != null) selected?.let { args.put("idPista", it.id) }
        val response = try {
            call(tool, args)
        } catch (error: Hands.ToolError) {
            observed["prepare"] = error.code
            return StonePlayback.Result(failure(error.code), false)
        }
        val steps = response.optJSONObject("steps") ?: return result("Stone devolvió una respuesta que no pude verificar.")
        steps.keys().forEach { key -> steps.optJSONObject(key)?.optString("code")?.let { observed[key] = it } }
        val volume = steps.optJSONObject("volume")?.takeIf { it.has("actual") }?.optInt("actual")
        if (response.optString("status") == "ambiguous") {
            val matches = response.optJSONArray("matches")
            val options = (0 until (matches?.length() ?: 0)).mapNotNull { song(matches?.optJSONObject(it)) }
            return StonePlayback.Result(Reply(Say.TOOL_RESULT, listOf("Encontré varias coincidencias para «$query». Elige una canción.")),
                false, options)
        }
        if (observed["volume"] == "VOLUME_REJECTED") {
            val target = song(response.optJSONObject("selected"))
            return StonePlayback.Result(Reply(Say.TOOL_RESULT, listOf("Stone no pudo ajustar el volumen. ¿Reproduzco con el volumen actual?")),
                false, listOfNotNull(target), volumeBlocked = target != null)
        }
        val track = song(response.optJSONObject("track"))
        val confirmed = track != null && observed["play"] == "OK"
        val text = mutableListOf<String>()
        if (confirmed) {
            text += "Suena «${track.label}»."
            if (response.optBoolean("segundoPlano")) returnToAssistant()
        } else {
            text += when (val reason = observed["search"] ?: observed["play"]) {
                "TRACK_NOT_FOUND" -> "No encontré «$query» en tu biblioteca de Stone."
                "EMPTY_LIBRARY" -> "La biblioteca de audio de Stone está vacía."
                "INVALID_SELECTION" -> "La selección ya no coincide. Vuelve a pedir el título."
                else -> "Stone no pudo confirmar la reproducción (${observed["play"] ?: reason ?: "UNKNOWN_RESULT"})."
            }
        }
        observed["equalizer"]?.let { status ->
            text += when {
                status != "OK" -> "No pude cambiar el ecualizador ($status)."
                command["equalizer"] == "1" -> "Ecualizador activado."
                else -> "Ecualizador desactivado."
            }
        }
        if (skipVolume) text += "Se mantuvo el volumen actual."
        val ok = confirmed && response.optString("status") == "playing"
        if (ok && query == null && command["equalizer"] == null && !skipVolume) {
            val say = if (command.action == Action.PLAY_RECOMMENDED) Say.RECOMMENDED else Say.PLAYING
            return StonePlayback.Result(Reply(say, volume = volume), true)
        }
        return result(text.joinToString(" "), ok, volume)
    }

    fun failure(code: String): Reply = when (code) {
        "MCP_LOCAL_DESACTIVADO" -> Reply(Say.TOOL_OFF)
        "UNKNOWN_TOOL", "TOOL_UNAVAILABLE" -> Reply(Say.TOOL_RESULT, listOf("La versión de Stone instalada todavía no ofrece esta herramienta MCP."))
        "NO_PERMISSION", "NOT_SAME_DEVELOPER" -> Reply(Say.TOOL_RESULT, listOf("Stone requiere que Luna tenga la misma firma para usar MCP local."))
        "STONE_CLOSED", "PLAYER_NOT_READY" -> Reply(Say.TOOL_RESULT, listOf("Abre Stone y vuelve a pedir la orden."))
        else -> Reply(Say.TOOL_RESULT, listOf("Stone no pudo confirmar la orden ($code)."))
    }

    private fun song(json: JSONObject?): StonePlayback.Song? {
        val id = json?.optString("id")?.takeIf { it.isNotBlank() } ?: return null
        return StonePlayback.Song(id, json.optString("title"), json.optString("artist"), json.optString("album"))
    }

    private fun result(text: String, ok: Boolean = false, volume: Int? = null) =
        StonePlayback.Result(Reply(Say.TOOL_RESULT, listOf(text), volume = volume), ok)
}
