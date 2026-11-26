package com.zirakusa.gatewayhub.assistant

/** Pull modifiers out of an explicit music order without normalizing the title sent to Stone. */
internal object MusicOrder {
    private val prefix = Regex(
        """^(?:(?:reproduce|reproducir|pon|ponme|coloca|colócame|colocame|toca|escuchar|quiero escuchar|play)(?: algo de| un poco de)?(?: la)? (?:m[uú]sica|canciones|una canci[oó]n|algo)(?: de(?=\s))?|m[uú]sica)(?=\s|$)""",
        RegexOption.IGNORE_CASE
    )
    private val background = Regex(
        """(?<!\S)(?:en segundo plano|de fondo|en el fondo|sin abrir(?: la app| la aplicaci[oó]n| stone)?|sin salir de aqu[ií])(?=\s|$)""",
        RegexOption.IGNORE_CASE
    )
    private val equalizer = Regex(
        """(?<!\S)(?:(?:y|con)\s+)?(?:el\s+)?ecualizador\s+(activo|activado|encendido|inactivo|desactivado|apagado)(?=\s|$)""",
        RegexOption.IGNORE_CASE
    )
    private const val NUMBER = """(?:\d{1,3}|[a-záéíóú]+(?: y [a-záéíóú]+)?)"""
    private const val PERCENT = """(?:%|por\s*ciento)"""
    private val volume = Regex(
        """(?<!\S)(?:(?:y |con )?(?:(?:sube|baja|ajusta|pon) )?(?:el )?volumen(?: (?:a|al|en|de|hasta))? (?:la |el )?$NUMBER(?: ?$PERCENT)?|(?:a|al|con|y) $NUMBER ?$PERCENT(?: (?:del?|el|de el) volumen)?|\d{1,3} ?$PERCENT(?: (?:del?|el|de el) volumen)?|a todo volumen)(?=\s|$)""",
        RegexOption.IGNORE_CASE
    )
    private val recommended = setOf("recomendada", "recomendado", "recomendaciones", "mix del dia", "mezcla del dia")

    fun parse(raw: String): Command? {
        val clean = raw.trim().trim('¿', '¡', '?', '!', '.')
        val text = IntentRules.musicPrefix?.replaceFirst(clean, "pon ") ?: clean
        val opening = prefix.find(text) ?: return null
        var rest = text.substring(opening.range.last + 1).trim()
        val slots = mutableMapOf<String, String>()
        background.find(rest)?.let {
            slots["background"] = "1"
            rest = rest.removeRange(it.range).trim()
        }
        equalizer.find(rest)?.let {
            slots["equalizer"] = if (it.groupValues[1].lowercase() in setOf("activo", "activado", "encendido")) "1" else "0"
            rest = rest.removeRange(it.range).trim()
        }
        volume.findAll(rest).firstOrNull { candidate ->
            val (change, remainder) = Volume.take(Words.normalize(candidate.value))
            if (change is Volume.Change.To && remainder.isEmpty()) {
                slots["volume"] = change.percent.toString()
                true
            } else false
        }?.let { rest = rest.removeRange(it.range).trim() }
        if (opening.value.equals("música", true) || opening.value.equals("musica", true)) {
            if ("background" !in slots) return null
        }
        rest = rest.trim(' ', ',')
        if (Words.normalize(rest) in recommended) return null
        if (rest.isNotBlank()) slots["query"] = rest
        return Command(Action.PLAY_MUSIC, slots)
    }
}
