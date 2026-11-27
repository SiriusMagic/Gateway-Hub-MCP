package com.zirakusa.gatewayhub.assistant

import java.util.UUID
import org.json.JSONObject

/** Private to the phone. Reports take only operational metadata from this record. */
data class McpOrder(
    val id: String,
    val tool: String,
    val arguments: Map<String, Any>,
    val stage: String,
    val code: String,
    val millis: Long,
    val response: String? = null,
    val app: String = "music_",
)

internal object StoneOrder {
    const val PLAY = "player_play_by_title"
    const val PROBE = "player_state"

    fun matches(command: Command) = command.action == Action.PLAY_MUSIC &&
        !command["query"].isNullOrBlank() && command["artist_search"] != "1"

    private val choice = Regex("""^(?:opci[oó]n|n[uú]mero|elige|pon la opci[oó]n) (.+)$""", RegexOption.IGNORE_CASE)

    fun choose(previous: Turn?, said: String): Command? {
        val report = previous?.mcp?.takeIf { it.code == "AMBIGUOUS_TITLE" } ?: return null
        val ordinal = choice.matchEntire(said.trim().trimEnd('.'))?.groupValues?.get(1) ?: return null
        val index = Words.normalize(ordinal).toIntOrNull()?.takeIf { it in 1..10 } ?: return null
        val candidates = report.response?.let(::JSONObject)?.optJSONArray("matches") ?: return null
        val id = candidates.optJSONObject(index - 1)?.optString("id")?.takeIf(String::isNotBlank) ?: return null
        return previous.command.copy(slots = previous.command.slots + ("track_id" to id))
    }

    fun execute(hands: Hands, heard: String, byVoice: Boolean, command: Command, clock: () -> Long): Turn {
        val app = Catalog.byId("stone")
        val id = UUID.randomUUID().toString()
        val title = checkNotNull(command["query"])
        val args = buildMap<String, Any> {
            put("titulo", title)
            command["track_id"]?.let { put("idPista", it) }
            command.int("volume")?.let { put("volumen", it) }
            command["equalizer"]?.let { put("ecualizador", it == "1") }
            command["background"]?.let { put("segundoPlano", it == "1") }
        }
        val start = clock()
        fun finish(code: String, message: String = advice(code), response: JSONObject? = null, ok: Boolean = false): Turn {
            val millis = (clock() - start).coerceAtLeast(0)
            return Turn(heard, byVoice, command, app, Reply(Say.TOOL_RESULT, listOf(message)), ok, millis,
                McpOrder(id, PLAY, args, stage(code), code, millis, response?.toString()))
        }
        if (!hands.installed(app)) return finish("APP_MISSING")
        val result = try {
            hands.tracedTool(app, PLAY, JSONObject(args), id)
        } catch (_: Hands.ToolOff) {
            return finish("HUB_LOCAL_DISABLED")
        } catch (error: Hands.ToolError) {
            return finish(error.code, advice(error.code, error.message.orEmpty()))
        } catch (_: Exception) {
            null
        } ?: return finish("RESULT_UNCONFIRMED")
        val steps = result.optJSONObject("steps") ?: return finish("RESULT_UNCONFIRMED", response = result)
        val track = result.optJSONObject("track")?.optString("title").orEmpty()
        val status = result.optString("status")
        val code = listOf("play", "search", "volume", "equalizer").firstNotNullOfOrNull { step ->
            steps.optJSONObject(step)?.optString("code")?.takeUnless { it.isBlank() || it == "OK" }
        } ?: "OK"
        if (status in setOf("playing", "partial") && track.isNotBlank() && steps.optJSONObject("play")?.optString("code") == "OK") {
            val volume = steps.optJSONObject("volume")
            val message = buildString {
                append("Reproduciendo «$track».")
                if (volume?.optString("code") == "OK") append(" Volumen al " + volume.optInt("actual") + " %.")
                if (status == "partial") append(" Stone no pudo aplicar el ecualizador.")
            }
            return finish(code, message, result, ok = status == "playing")
        }
        val message = when (code) {
            "AMBIGUOUS_TITLE" -> {
                val matches = result.optJSONArray("matches")
                val choices = (0 until (matches?.length() ?: 0)).mapNotNull { matches?.optJSONObject(it) }
                    .mapIndexed { index, it -> (index + 1).toString() + ". " + it.optString("title") + " (" + it.optString("artist") + ")" }
                    .joinToString("\n")
                "Encontré varias pistas. Di «opción» y el número de la pista.\n$choices"
            }
            "TRACK_NOT_FOUND" -> "No encontré «$title» en Stone."
            "VOLUME_REJECTED" -> "Stone no pudo ajustar el volumen."
            "PLAYBACK_FAILED" -> "Stone no pudo reproducir «$title»."
            else -> advice(if (code == "OK") "RESULT_UNCONFIRMED" else code)
        }
        return finish(if (code == "OK") "RESULT_UNCONFIRMED" else code, message, result)
    }

    fun stage(code: String) = when (code) {
        "HUB_LOCAL_DISABLED", "LOCAL_DISABLED", "NO_PERMISSION", "NOT_SAME_DEVELOPER" -> "permiso_local"
        "STONE_TOKEN", "UNAUTHORIZED" -> "token"
        "APP_MISSING", "PROVIDER_UNAVAILABLE", "NOT_READY", "STONE_OFFLINE", "STONE_UNREACHABLE", "STONE_NOT_LINKED", "APP_NOT_LINKED", "NO_HUB", "NETWORK" -> "conexion"
        "RESULT_UNCONFIRMED", "PLAYBACK_UNCONFIRMED" -> "respuesta"
        else -> "ejecucion"
    }

    fun advice(code: String, fallback: String = "") = when (code) {
        "RESULT_UNCONFIRMED", "PLAYBACK_UNCONFIRMED" -> "Resultado sin confirmar. Revisa el reproductor de Stone."
        "HUB_LOCAL_DISABLED" -> "Activa «Permitir MCP local» en Luna."
        "LOCAL_DISABLED" -> "Activa «Permitir MCP local» en Stone."
        "NO_PERMISSION", "NOT_SAME_DEVELOPER" -> "Usa versiones de Luna y Stone firmadas por el mismo desarrollador."
        "TOOL_UNAVAILABLE" -> "Actualiza Stone a una versión que admita reproducir por título."
        "APP_MISSING" -> "Instala Stone para reproducir por título."
        "STONE_OFFLINE", "NOT_READY", "STONE_UNREACHABLE", "PROVIDER_UNAVAILABLE", "PLAYER_NOT_READY" -> "Abre Stone y prueba la conexión."
        "STONE_TOKEN", "UNAUTHORIZED" -> "Renueva el token y vuelve a vincular Stone al Hub."
        "NO_HUB", "STONE_NOT_LINKED", "NETWORK" -> "Conecta el Hub y vincula Stone en el escáner."
        "OK" -> "Stone respondió correctamente."
        else -> fallback.ifBlank { "Revisa el estado de MCP en Stone." }
    }
}
