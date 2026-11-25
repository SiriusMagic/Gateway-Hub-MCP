package com.zirakusa.gatewayhub.assistant

/**
 * The volume said inside any order: «pon música al 30 por ciento», «con el volumen a 50», «a todo volumen», «volumen a la
 * mitad»; or by itself: «sube el volumen», «más bajo». Lucía sets it herself (the phone's media volume), before the order
 * runs so the music never starts too loud. Pure; works on [Words.normalize]d text.
 */
object Volume {
    /** A percentage (0–100), or a step up or down. */
    sealed interface Change { data class To(val percent: Int) : Change; data class Step(val up: Boolean) : Change }

    private val named = mapOf("maximo" to 100, "tope" to 100, "full" to 100, "todo" to 100, "mitad" to 50, "minimo" to 5)
    private const val TO = "(?:a|al|en|de|hasta)"
    private val patterns = listOf(
        // «volumen 30», «el volumen al 30 %», «con volumen a 30 por ciento», «sube el volumen a 30»
        Regex("(?:(?:con|a|al|en|y) )?(?:(?:sube|subele|baja|bajale|pon|ponle|cambia|ajusta) )?(?:el )?volumen(?: $TO)? (\\d{1,3})(?: ?(?:%|por ciento|porciento))?"),
        // «al 30 por ciento (de volumen)», «a 30 %»
        Regex("(?:(?:con|a|al|en|y) )?(\\d{1,3}) ?(?:%|por ciento|porciento)(?: (?:de|del|el|de el) volumen)?"),
        // «volumen al máximo», «a todo volumen», «volumen a la mitad»
        Regex("(?:(?:con|y) )?(?:(?:sube|subele|baja|bajale|pon|ponle) )?(?:el )?volumen (?:$TO )?(?:la |el )?(maximo|tope|full|mitad|minimo)"),
        Regex("(?:a|con) (todo) (?:el )?volumen"),
    )
    private val step = Regex("^(?:(sube|subele|subir|aumenta|mas alto|mas fuerte)|(baja|bajale|bajar|disminuye|mas bajo|mas despacio|mas suave))(?: (?:el|un poco|un poco el|mas el))?(?: volumen)?(?: un poco)?(?: mas)?$")
    /** What can be left around a volume once it is taken out («pon el», «y sube»). */
    private val leftover = Regex("(?:^| )(?:y|con|pon|ponle|sube|subele|baja|bajale|ajusta|el|la)$")

    /** The volume said in [t] and [t] without it. */
    fun take(t: String): Pair<Change?, String> {
        step.find(t)?.let { m -> return Change.Step(up = m.groupValues[1].isNotEmpty()) to "" }
        for (p in patterns) {
            val m = p.find(t) ?: continue
            val v = m.groupValues[1]
            val percent = (v.toIntOrNull() ?: named[v] ?: continue).coerceIn(0, 100)
            var rest = (t.substring(0, m.range.first) + " " + t.substring(m.range.last + 1)).replace(Regex("\\s+"), " ").trim()
            while (leftover.containsMatchIn(rest)) rest = rest.replace(leftover, "").trim()
            return Change.To(percent) to rest
        }
        return null to t
    }

    /** The new percentage after [change], from [now]: steps of 15. */
    fun apply(change: Change, now: Int) = when (change) {
        is Change.To -> change.percent
        is Change.Step -> (now + if (change.up) 15 else -15).coerceIn(0, 100)
    }
}
