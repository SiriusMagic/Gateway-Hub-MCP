package com.zirakusa.gatewayhub.assistant

/**
 * Turns what was said into a [Command], with rules in Spanish (no internet, no AI model, instant). The order matters:
 * the most specific rules first. Pure; every phrase it knows is in the tests.
 */
object Parser {
    private fun has(t: String, vararg words: String) = words.any { Regex("(^| )$it( |$)").containsMatchIn(t) }

    fun parse(raw: String): Command {
        val t = Words.normalize(raw)
        if (t.isEmpty()) return Command(Action.UNKNOWN)

        // Media buttons: short orders, for whatever is playing.
        when {
            Regex("^(pausa|pausar|pausa la musica|para la musica|deten la musica|detente|stop|silencio)$").matches(t) -> return media(MediaKey.PAUSE)
            Regex("^(siguiente|siguiente cancion|la siguiente|salta(la)?|pasa la cancion)$").matches(t) -> return media(MediaKey.NEXT)
            Regex("^(anterior|cancion anterior|la anterior|vuelve atras)$").matches(t) -> return media(MediaKey.PREVIOUS)
            Regex("^(continua|sigue|reanuda|reanudar|play|continua la musica)$").matches(t) -> return media(MediaKey.RESUME)
        }

        // Timer before alarm: «pon un temporizador de 10 minutos», «avísame en 5 minutos».
        if (has(t, "temporizador", "cuenta regresiva") || Regex("^(avisame|recuerdame|despiertame) en ").containsMatchIn(t)) {
            Words.duration(t)?.let { return Command(Action.TIMER, mapOf("seconds" to "$it")) }
            if (has(t, "temporizador")) return Command(Action.OPEN, mapOf("app" to "reloj"))
        }

        // Alarm: «pon una alarma a las 7», «despiértame a las 6 y media», «alarma para mañana a las 5».
        if (has(t, "alarma", "despiertame", "levantame", "despertador")) {
            val at = t.substringAfter(" a las ", "").ifEmpty { t.substringAfter(" a la ", "") }.ifEmpty { t.substringAfter(" para las ", "") }
                .ifEmpty { t.substringAfter(" las ", "") }.ifEmpty { t.substringAfter("alarma ", "") }
            Words.clock(at)?.let { (h, m) -> return Command(Action.ALARM, mapOf("hour" to "$h", "minute" to "$m")) }
            return Command(Action.OPEN, mapOf("app" to "reloj"))
        }

        // Messages: «manda un mensaje a Ana que llego tarde», «escríbele a Ana».
        Regex("^(manda|mandale|envia|enviale|escribe|escribele)( un)?( mensaje| sms| texto)?( a)? (.+?)(?: (?:que|diciendo|diciendole|con el texto) (.+))?$").find(t)?.let { m ->
            val who = m.groupValues[5].trim()
            if (who.isNotEmpty() && !has(who, "mensaje", "sms")) return Command(Action.MESSAGE, buildMap { put("who", who); m.groupValues[6].takeIf { it.isNotBlank() }?.let { put("text", it) } })
        }

        // Calls: «llama a Ana», «llama al 099 123 4567», «marca 611».
        Regex("^(llama|llamale|llamar|marca|marcale|telefonea)( a| al)? (.+)$").find(t)?.let { m ->
            return Command(Action.CALL, mapOf("who" to m.groupValues[3].replace(Regex("^(el|la) "), "").trim()))
        }

        // Music: recommendations first, then «reproduce música (de X)», «pon a X».
        if (has(t, "recomendaciones", "recomendado", "recomendada", "recomiendame", "mix del dia", "mezcla del dia", "descubrimiento")) return Command(Action.PLAY_RECOMMENDED)
        Regex("^(reproduce|reproducir|pon|ponme|toca|escuchar|quiero escuchar|play)( algo de| un poco de)?( la)? (musica|canciones|una cancion|algo)( de (.+))?$").find(t)?.let { m ->
            return Command(Action.PLAY_MUSIC, m.groupValues[6].takeIf { it.isNotBlank() }?.let { mapOf("query" to it) } ?: emptyMap())
        }
        Regex("^(reproduce|pon|ponme|toca) (.+)$").find(t)?.let { m ->
            if (Catalog.find(m.groupValues[2]) == null) return Command(Action.PLAY_MUSIC, mapOf("query" to m.groupValues[2]))
        }

        // Questions the assistant answers itself, with the family's summaries.
        if (has(t, "que hora es", "hora es", "dime la hora")) return Command(Action.TIME)
        if (has(t, "que dia es", "que fecha es", "dia es hoy", "fecha de hoy")) return Command(Action.DATE)
        if (has(t, "clima", "tiempo hace", "que tiempo", "va a llover", "llovera", "temperatura", "hace frio", "hace calor")) return Command(Action.WEATHER)
        if (has(t, "que tengo", "mi agenda", "mis eventos", "tengo hoy", "eventos de hoy", "citas")) return Command(Action.AGENDA)
        if (has(t, "buenos dias", "buenas tardes", "buenas noches", "resumen", "que hay hoy", "mi dia", "como va mi dia")) return Command(Action.TODAY)

        // Opening things.
        if (has(t, "foto", "selfie", "fotografia") && has(t, "toma", "tomar", "haz", "hazme", "saca", "sacar")) return Command(Action.PHOTO)
        if (has(t, "escanea", "escanear", "lee") && has(t, "qr", "codigo", "codigo de barras")) return Command(Action.SCAN)
        if (has(t, "prueba de velocidad", "velocidad de internet", "test de velocidad") || t.contains("que tan rapido")) return Command(Action.SPEED)
        Regex("^(abre|abrir|abreme|inicia|lanza|muestrame|ve a|ir a|entra a|entra en)( la app| la aplicacion)?( de| del| el| la| los| las| mis)? (.+)$").find(t)?.let { m ->
            Catalog.find(m.groupValues[4])?.let { return Command(Action.OPEN, mapOf("app" to it.id)) }
            return Command(Action.UNKNOWN, mapOf("app_name" to m.groupValues[4]))
        }
        Catalog.find(t)?.takeIf { t.split(' ').size <= 3 }?.let { return Command(Action.OPEN, mapOf("app" to it.id)) }
        return Command(Action.UNKNOWN)
    }

    private fun media(k: MediaKey) = Command(Action.MEDIA, mapOf("key" to k.name))
}
