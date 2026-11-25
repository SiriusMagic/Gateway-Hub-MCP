package com.zirakusa.gatewayhub.assistant

import org.json.JSONArray
import org.json.JSONObject

/**
 * «Reproduce música alone»: from Stone's library_search results, what to play. A song whose title is what was said goes
 * first (the one asked for), then the rest that matched (same artist, album or words), so the music keeps going. Pure.
 */
object MusicPick {
    /** [ids] in playback order (at most [MAX]); [title] / [artist] of the first, for Lucía's answer. */
    data class Pick(val ids: List<String>, val title: String, val artist: String, val exact: Boolean)

    const val MAX = 50

    fun pick(query: String, results: JSONArray?): Pick? {
        val songs = (0 until (results?.length() ?: 0)).mapNotNull { results!!.optJSONObject(it) }
            .filter { it.optString("id").isNotBlank() && !it.optBoolean("isVideo") }
        if (songs.isEmpty()) return null
        val wanted = Words.normalize(query)
        fun title(o: JSONObject) = Words.normalize(o.optString("title"))
        val best = songs.firstOrNull { title(it) == wanted } ?: songs.firstOrNull { wanted.isNotEmpty() && title(it).contains(wanted) }
        val order = listOfNotNull(best) + songs.filter { it !== best }
        val first = order.first()
        return Pick(order.take(MAX).map { it.optString("id") }, first.optString("title"),
            first.optString("artist").takeIf { it.isNotBlank() && it != "null" && !it.startsWith("<") }.orEmpty(), best != null)
    }

    /** «Suena «Alone», de Marshmello.» or «Pongo 12 canciones de «soda stereo».» */
    fun say(p: Pick, query: String): String =
        if (p.exact || p.ids.size == 1) "Suena «${p.title}»" + (if (p.artist.isNotEmpty()) ", de ${p.artist}." else ".")
        else "Pongo ${p.ids.size} canciones de «$query», empezando por «${p.title}»."
}
