package com.zirakusa.gatewayhub.assistant.local

import android.content.res.Resources
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.Command
import com.zirakusa.gatewayhub.assistant.Words
import org.json.JSONArray
import org.json.JSONObject

internal object StoneRequests {
    private val prefix = Regex("^mcp local[:,]?\\s+", RegexOption.IGNORE_CASE)
    private val library = Regex("\\b(canciones|temas|album(?:es)?|artistas?|biblioteca|musica|videos?)\\b")
    private val query = Regex("^(?:que|cual(?:es)?|cuant[oa]s?|hay|busca(?:me)?|encuentra|muestra(?:me)?|consulta|lista)\\b")

    fun explicit(text: String) = prefix.find(text.trim())?.let { text.trim().substring(it.range.last + 1).trim() }
        ?.takeIf(String::isNotEmpty)

    fun matches(text: String, command: Command): Boolean {
        if (command.action == Action.TOOL) return command["app"] == "stone" && command["tool"] in PhoneMcp.STONE_TOOLS
        if (command.action != Action.UNKNOWN) return false
        val normalized = Words.normalize(text)
        return query.containsMatchIn(normalized) && library.containsMatchIn(normalized) &&
            (Regex("\\bstone\\b").containsMatchIn(normalized) || Regex("\\b(?:mi|la) biblioteca\\b").containsMatchIn(normalized))
    }

    fun answer(tool: String, result: JSONObject, resources: Resources): String {
        if (tool == "player_state") {
            val title = result.optString("title").takeUnless { it.isBlank() || it == "null" }
                ?: return resources.getString(R.string.phone_mcp_no_track)
            val artist = result.optString("artist").takeUnless { it.isBlank() || it == "null" }
            val track = track(title, artist, resources)
            return resources.getString(if (result.optBoolean("playing")) R.string.phone_mcp_playing else R.string.phone_mcp_paused, track)
        }
        val matches = result.optJSONArray("results") ?: JSONArray()
        if (matches.length() == 0) return resources.getString(R.string.phone_mcp_no_songs)
        val songs = (0 until minOf(matches.length(), 8)).mapNotNull { i ->
            matches.optJSONObject(i)?.let { track(it.optString("title"), it.optString("artist").takeIf(String::isNotBlank), resources) }
        }
        val total = result.optInt("total", songs.size)
        return (listOf(resources.getQuantityString(R.plurals.phone_mcp_songs, total, total)) + songs).joinToString("\n")
    }

    private fun track(title: String, artist: String?, resources: Resources) =
        artist?.let { resources.getString(R.string.phone_mcp_track, title, it) } ?: title
}
