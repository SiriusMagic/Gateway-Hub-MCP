package com.zirakusa.gatewayhub.assistant

import kotlinx.coroutines.delay
import org.json.JSONObject

/** Compatibility with the signed single action implemented by Stone. A returned error must never cause replay. */
internal object StoneTitleAction {
    const val TOOL = "player_play_by_title"

    suspend fun run(
        query: String, command: Command, selected: StonePlayback.Song?, skipVolume: Boolean,
        call: suspend (String, JSONObject) -> JSONObject, open: () -> Boolean, returnToAssistant: () -> Unit,
        observed: MutableMap<String, String>, remote: Boolean = false
    ): StonePlayback.Result? {
        val args = JSONObject().put("titulo", query).put("segundoPlano", command["background"] == "1")
        if (!skipVolume) args.put("volumen", command.int("volume"))
        command["equalizer"]?.let { args.put("ecualizador", it == "1") }
        selected?.let { args.put("idPista", it.id) }
        val response = try {
            if (remote) call(TOOL, args) else callReady(call, args, open)
        } catch (error: Hands.ToolError) {
            if (error.code == "UNKNOWN_TOOL") return null
            observed["prepare"] = error.code
            return result(if (remote && !error.message.isNullOrBlank()) "Cloudflare Code Mode: ${error.message}"
                else "Stone no pudo confirmar la orden (${error.code}).")
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
        if (observed["volume"] == "VOLUME_REJECTED" || remote && observed["volume"] != null && observed["volume"] != "OK") {
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
            text += when (val reason = observed["search"].takeUnless { it == "OK" } ?: observed["play"]) {
                "TRACK_NOT_FOUND" -> "No encontré «$query» en tu biblioteca de Stone."
                "INVALID_SELECTION" -> "La selección ya no coincide. Vuelve a pedir el título."
                "STONE_CLOSED", "STONE_OFFLINE", "STONE_UNREACHABLE" -> "Stone no respondió. Ábrelo y revisa su enlace con el Hub."
                "STONE_TOKEN" -> "Stone rechazó el token del Hub. Revisa el vínculo con Stone."
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
        val message = text.joinToString(" ")
        return result(if (remote) "Cloudflare Code Mode · $message" else message, confirmed && response.optString("status") == "playing", volume)
    }

    private suspend fun callReady(
        call: suspend (String, JSONObject) -> JSONObject, args: JSONObject, open: () -> Boolean
    ): JSONObject {
        suspend fun ready(): JSONObject? = try {
            call(TOOL, args).takeUnless { it.optJSONObject("steps")?.optJSONObject("play")?.optString("code") == "PLAYER_NOT_READY" }
        } catch (error: Hands.ToolError) {
            if (error.code != "STONE_CLOSED") throw error
            null
        }
        ready()?.let { return it }
        if (open()) repeat(12) {
            delay(250)
            ready()?.let { return it }
        }
        throw Hands.ToolError("STONE_CLOSED", "")
    }

    private fun song(json: JSONObject?): StonePlayback.Song? {
        val id = json?.optString("id")?.takeIf { it.isNotBlank() } ?: return null
        return StonePlayback.Song(id, json.optString("title"), json.optString("artist"), json.optString("album"))
    }

    private fun result(text: String, ok: Boolean = false, volume: Int? = null) =
        StonePlayback.Result(Reply(Say.TOOL_RESULT, listOf(text), volume = volume), ok)
}
