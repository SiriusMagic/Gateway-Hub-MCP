package com.zirakusa.gatewayhub.assistant

import java.text.Normalizer

/**
 * Spanish as people say it to a phone: no accents, no punctuation, «por favor» and «oye» gone, numbers in words turned
 * into digits («las siete y media» → «las 7 y media»). Pure; the parser works on this.
 */
object Words {
    private val fillers = listOf("por favor", "porfa", "oye", "hola", "asistente", "hub", "puedes", "podrias", "quiero que", "necesito que", "me puedes", "me podrias", "ahora")
    private val numbers = mapOf(
        "cero" to 0, "un" to 1, "una" to 1, "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5, "seis" to 6, "siete" to 7,
        "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13, "catorce" to 14, "quince" to 15,
        "dieciseis" to 16, "diecisiete" to 17, "dieciocho" to 18, "diecinueve" to 19, "veinte" to 20, "veinticinco" to 25,
        "treinta" to 30, "cuarenta" to 40, "cuarenta y cinco" to 45, "cincuenta" to 50, "sesenta" to 60, "setenta" to 70, "ochenta" to 80, "noventa" to 90,
    )

    fun normalize(text: String): String {
        var t = text.lowercase().replace('ñ', '\u0001')
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace('\u0001', 'ñ')
        t = t.replace(Regex("[¿?¡!.,;:\"()]"), " ").replace(Regex("\\s+"), " ").trim()
        fillers.forEach { f -> t = t.replace(Regex("(^| )$f( |$)"), " ").trim() }
        // Longest number words first («cuarenta y cinco» before «cuarenta»); «un»/«una» only before a unit, so «una alarma» stays.
        numbers.keys.sortedByDescending { it.length }.forEach { w ->
            val unitOnly = w == "un" || w == "una" || w == "uno"
            val pattern = if (unitOnly) "(^| )$w (?=(segundo|minuto|hora|cuarto)s?\\b)" else "(^| )$w(?= |$)"
            t = t.replace(Regex(pattern)) { m -> m.groupValues[1] + numbers.getValue(w) + if (unitOnly) " " else "" }
        }
        return t.replace(Regex("\\s+"), " ").trim()
    }

    /** «7», «7:30», «7 y media», «8 menos cuarto», «6 de la tarde», «10 pm» → hour and minute (24 h), or null. */
    fun clock(text: String): Pair<Int, Int>? {
        val m = Regex("""(\d{1,2})(?:[:h](\d{2}))?(?: y (media|cuarto|\d{1,2}))?(?: menos (cuarto|\d{1,2}))?(?: (?:de la |en la )?(manana|tarde|noche|madrugada)| ?(am|pm|a m|p m))?""")
            .find(text) ?: return null
        var h = m.groupValues[1].toInt(); var min = m.groupValues[2].toIntOrNull() ?: 0
        when (val plus = m.groupValues[3]) { "media" -> min = 30; "cuarto" -> min = 15; "" -> Unit; else -> min = plus.toInt() }
        when (val less = m.groupValues[4]) { "" -> Unit; else -> { val l = if (less == "cuarto") 15 else less.toInt(); h -= 1; min = 60 - l } }
        val part = m.groupValues[5].ifEmpty { m.groupValues[6].replace(" ", "") }
        if (part in setOf("tarde", "noche", "pm") && h in 1..11) h += 12
        if (part in setOf("manana", "madrugada", "am") && h == 12) h = 0
        if (part == "noche" && h == 12) h = 0
        if (h < 0) h += 24
        return if (h in 0..23 && min in 0..59) h to min else null
    }

    /** «10 minutos», «1 hora y media», «90 segundos», «media hora», «un cuarto de hora» → seconds, or null. */
    fun duration(text: String): Int? {
        if (Regex("(^| )media hora").containsMatchIn(text)) return 1800 + (duration(text.replace(Regex("(^| )media hora"), " ")) ?: 0)
        if (Regex("1 cuarto de hora").containsMatchIn(text)) return 900
        var total = 0; var found = false
        Regex("""(\d+)\s*(hora|minuto|min|segundo|seg)s?( y media)?""").findAll(text).forEach { m ->
            val n = m.groupValues[1].toInt()
            val unit = when (m.groupValues[2]) { "hora" -> 3600; "minuto", "min" -> 60; else -> 1 }
            total += n * unit + if (m.groupValues[3].isNotEmpty()) unit / 2 else 0; found = true
        }
        return if (found && total in 1..24 * 3600) total else null
    }
}
