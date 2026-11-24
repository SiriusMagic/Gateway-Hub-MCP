package com.zirakusa.gatewayhub.assistant

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * What Lucía says after a family app's tool answered: short Spanish, the first few items, never codes or ids. A few
 * tools have their own words (the next alarm, the time in a city, the free space…); lists are read the same way for
 * every app («Tienes 3 tareas: comprar pan, …»). Pure.
 */
object Speech {
    private const val READ = 3

    /** The noun of each list the apps return. */
    private val nouns = mapOf("alarms" to "alarmas", "tasks" to "tareas", "notes" to "notas", "events" to "eventos", "holidays" to "feriados",
        "messages" to "mensajes", "calls" to "llamadas", "birthdays" to "cumpleaños", "contacts" to "contactos", "files" to "archivos",
        "items" to "códigos", "history" to "códigos", "codes" to "códigos", "days" to "días", "results" to "resultados",
        "playlists" to "listas", "threads" to "conversaciones", "photos" to "fotos", "media" to "fotos")
    private val singular = mapOf("alarmas" to "alarma", "tareas" to "tarea", "notas" to "nota", "eventos" to "evento", "feriados" to "feriado",
        "mensajes" to "mensaje", "llamadas" to "llamada", "contactos" to "contacto", "archivos" to "archivo", "códigos" to "código",
        "días" to "día", "resultados" to "resultado", "listas" to "lista", "conversaciones" to "conversación", "fotos" to "foto")
    private val names = listOf("title", "text", "name", "label", "holiday", "who", "from", "display_name", "city", "number", "path", "preview")
    private val months = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")

    private val presets = mapOf("bass" to "más graves", "treble" to "más agudos", "voice" to "la voz resaltada", "normal" to "sonido normal")

    /** [args]: what was asked (the answer of some tools is only «ok»). */
    fun say(tool: String, r: JSONObject, today: LocalDate, args: JSONObject = JSONObject()): String = when (tool) {
        "player_set_shuffle", "player_set_shuffle_off" -> if (args.optBoolean("enabled")) "Modo aleatorio activado." else "Aleatorio desactivado: en orden."
        "player_set_repeat" -> when (args.optString("mode")) { "one" -> "Repetiré esta canción."; "all" -> "Repetiré toda la lista."; else -> "Sin repetir." }
        "player_state" -> if (!r.optBoolean("playing") && r.optString("title").isBlank()) "No está sonando nada." else
            (if (r.optBoolean("playing")) "Suena " else "En pausa: ") + "«${r.optString("title")}»" + r.optString("artist").takeIf { it.isNotBlank() && it != "null" }?.let { " de $it" }.orEmpty() + "."
        "player_set_favorite" -> "Añadida a tus favoritos."
        "library_lyrics" -> r.optString("lyrics").trim().takeIf { it.isNotBlank() }?.let { "«${r.optString("title")}»:\n" + it.lines().take(8).joinToString("\n") }
            ?: "Esta canción no tiene letra guardada. Di «busca la letra»."
        "notes_get" -> (r.optJSONObject("note") ?: r).let { n -> listOf(n.optString("title"), n.optString("body").ifBlank { n.optString("text") }.take(400)).filter { it.isNotBlank() }.joinToString(":\n") }
        "weather_now" -> listOf("temperature", "temp", "temperature_c").firstNotNullOfOrNull { k -> r.opt(k)?.takeIf { it != JSONObject.NULL } }
            ?.let { t -> "Ahora hay $t grados" + r.optString("sky").ifBlank { r.optString("condition") }.takeIf { it.isNotBlank() }?.let { ", $it" }.orEmpty() + "." } ?: list(r, today)
        "calc_convert_units", "calc_convert_currency" -> listOf("result", "value", "text", "display").firstNotNullOfOrNull { k -> r.opt(k)?.takeIf { it != JSONObject.NULL }?.toString() }
            ?.let { "Son $it." } ?: list(r, today)
        "player_sleep_timer" -> args.optInt("minutes").let { if (it == 0) "Temporizador de música cancelado." else "La música se detendrá en $it minutos." }
        "equalizer_set_preset" -> "Ecualizador: ${presets[args.optString("preset")] ?: args.optString("preset")}."
        "clock_alarm_enable" -> "Alarma de las ${args.optString("time")} ${if (args.optBoolean("enabled")) "encendida" else "apagada"}."
        "camera_settings_set" -> (if (args.has("watermark")) "Marca de agua" else "Cuadrícula") + if (args.optBoolean(if (args.has("watermark")) "watermark" else "grid")) " activada." else " desactivada."
        "clock_next_alarm" -> r.optJSONObject("alarm")?.let { a ->
            "Tu próxima alarma es a las ${a.optString("time")}" + label(a) + r.optString("in_text").takeIf { it.isNotBlank() }?.let { ", en ${it.replace(" d", " días")}" }.orEmpty() + "."
        } ?: "No tienes ninguna alarma activa."
        "clock_alarms" -> r.optJSONArray("alarms").objects().let { list ->
            if (list.isEmpty()) "No tienes alarmas." else "Tienes ${list.size} ${plural(list.size, "alarma", "alarmas")}: " +
                list.take(READ + 2).joinToString(", ") { it.optString("time") + if (it.optBoolean("enabled", true)) "" else " (apagada)" } + "."
        }
        "clock_world_time" -> "En ${r.optString("city")} son las ${r.optString("time")}."
        "calc_evaluate" -> listOf("result", "value", "text", "display").firstNotNullOfOrNull { k -> r.opt(k)?.takeIf { it != JSONObject.NULL }?.toString() }
            ?.let { "Son $it." } ?: list(r, today)
        "files_storage" -> if (r.has("freeBytes")) "Te quedan ${gb(r.optLong("freeBytes"))} libres de ${gb(r.optLong("totalBytes"))}." else list(r, today)
        "gallery_stats" -> "Tienes ${r.optInt("photos")} ${plural(r.optInt("photos"), "foto", "fotos")} y ${r.optInt("videos")} ${plural(r.optInt("videos"), "video", "videos")}."
        "tasks_list" -> r.optJSONArray("tasks").objects().filter { !it.optBoolean("done") }.let { open ->
            if (open.isEmpty()) "No tienes tareas pendientes." else "Tienes ${open.size} ${plural(open.size, "tarea pendiente", "tareas pendientes")}: " +
                open.take(READ).joinToString(", ") { it.optString("text") } + if (open.size > READ) " y más." else "."
        }
        "tasks_create" -> "Tarea añadida: ${r.optString("text")}."
        "tasks_complete" -> "Hecho: ${r.optString("text")}."
        "calendar_event_create" -> r.optJSONObject("event")?.let { "Evento creado: ${it.optString("title")}, ${when_(it, today)}." } ?: "Evento creado."
        else -> list(r, today)
    }

    /** Any answer: the first list in it, read as «Tienes N …: a, b, c», or else its main fields. */
    fun list(r: JSONObject, today: LocalDate): String {
        val key = r.keys().asSequence().firstOrNull { r.opt(it) is JSONArray } ?: return single(r, today)
        val items = r.optJSONArray(key).objects()
        val total = if (r.has("total")) r.optInt("total") else items.size
        val noun = nouns[key] ?: "resultados"
        if (total == 0) return "No hay $noun."
        val read = items.take(READ).map { item(it, today) }.filter { it.isNotBlank() }
        return "Hay $total ${if (total == 1) singular[noun] ?: noun else noun}" + (if (read.isEmpty()) "." else ": " + read.joinToString("; ") + if (total > READ) "; y más." else ".")
    }

    private fun single(r: JSONObject, today: LocalDate): String {
        r.keys().asSequence().firstOrNull { r.opt(it) is JSONObject }?.let { return item(r.getJSONObject(it), today).ifBlank { "Listo." } }
        return item(r, today).ifBlank { "Listo." }
    }

    /** «Comprar pan», «Ana Pérez, en 2 días», «Reunión, el 25 de noviembre a las 15:00». */
    fun item(o: JSONObject, today: LocalDate): String {
        val name = names.firstNotNullOfOrNull { k -> o.optString(k).takeIf { it.isNotBlank() && it != "null" } }.orEmpty()
        val extra = when {
            o.has("days") && o.opt("days") is Int -> o.getInt("days").let { d -> if (d == 0) "hoy" else if (d == 1) "mañana" else "en $d días" } +
                (o.optInt("turns", 0).takeIf { it > 0 }?.let { ", cumple $it" }.orEmpty())
            else -> when_(o, today)
        }
        return listOf(name, extra).filter { it.isNotBlank() }.joinToString(", ")
    }

    /** The when of an item: «hoy a las 07:30», «el 25 de noviembre», or nothing. */
    private fun when_(o: JSONObject, today: LocalDate): String {
        val raw = listOf("start", "next_ring", "at", "date", "time").firstNotNullOfOrNull { k -> o.optString(k).takeIf { it.isNotBlank() && it != "null" } } ?: return ""
        runCatching { OffsetDateTime.parse(raw) }.getOrNull()?.let { return day(it.toLocalDate(), today) + " a las %02d:%02d".format(it.hour, it.minute) }
        runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()?.let { d ->
            val time = Regex("T(\\d{2}:\\d{2})").find(raw)?.groupValues?.get(1)
            return day(d, today) + (time?.let { " a las $it" }.orEmpty())
        }
        return if (Regex("^\\d{1,2}:\\d{2}$").matches(raw)) "a las $raw" else ""
    }

    private fun day(d: LocalDate, today: LocalDate) = when (d) {
        today -> "hoy"; today.plusDays(1) -> "mañana"; else -> "el ${d.dayOfMonth} de ${months[d.monthValue - 1]}"
    }

    private fun label(a: JSONObject) = a.optString("label").takeIf { it.isNotBlank() && it != "null" }?.let { " ($it)" }.orEmpty()
    private fun plural(n: Int, one: String, many: String) = if (n == 1) one else many
    private fun gb(bytes: Long) = "%.1f GB".format(java.util.Locale("es"), bytes / 1_000_000_000.0)
    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
