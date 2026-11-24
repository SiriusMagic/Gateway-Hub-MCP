package com.zirakusa.gatewayhub.assistant

/**
 * Turns what was said into a [Command], with rules in Spanish (no internet, no AI model, instant). The order matters:
 * the most specific rules first. Pure; every phrase it knows is in the tests.
 */
object Parser {
    private fun has(t: String, vararg words: String) = words.any { Regex("(^| )$it( |$)").containsMatchIn(t) }

    /**
     * An order and what comes with it: «pon música al 30 por ciento» is music plus a volume. The volume is taken out first
     * ([Volume]); what is left is the order. Only the volume alone is the VOLUME action.
     */
    fun parse(raw: String, today: java.time.LocalDate = java.time.LocalDate.now()): Command {
        this.today = today
        val t = Words.normalize(raw)
        if (t.isEmpty()) return Command(Action.UNKNOWN)
        // «Brillo al 40 por ciento» is the screen's, not the volume.
        if (Regex("(^| )brillo( |$)").containsMatchIn(t)) return one(t)
        val (volume, rest) = Volume.take(t)
        if (volume == null) return one(t)
        val slot = when (volume) { is Volume.Change.To -> "volume" to "${volume.percent}"; is Volume.Change.Step -> "volume_step" to if (volume.up) "up" else "down" }
        val c = if (rest.isEmpty()) Command(Action.UNKNOWN) else one(rest)
        return if (c.action == Action.UNKNOWN || c.action == Action.VOLUME) Command(Action.VOLUME, mapOf(slot)) else c.copy(slots = c.slots + slot)
    }

    private fun one(t: String): Command {
        // Media buttons: short orders, for whatever is playing.
        // «pon pausa a la música», «reinicia la canción», «quita la música», «otra canción»: what is said, with or without
        // «la música / la canción / el tema / esto» after it.
        val what = "(?: (?:a |en )?(?:la |el |esta |este )?(?:musica|cancion|tema|reproduccion|audio|esto|eso))?"
        when {
            Regex("^(?:pausa|pausar|pausala|pon(?:le)? pausa|ponle pausa|haz pausa|para|parala|pararla|deten|detenla|detener|detente|stop|silencio|quita|quitala|apaga|apagala|calla|callate)$what$").matches(t) -> return media(MediaKey.PAUSE)
            Regex("^(?:reinicia|reiniciala|reiniciar|repite|repitela|vuelve a empezar|empieza de nuevo|desde el principio|otra vez|de nuevo|pon de nuevo|vuelve a poner desde el principio)$what(?: desde el principio| de nuevo| otra vez)?$").matches(t) -> return media(MediaKey.RESTART)
            Regex("^(?:siguiente|la siguiente|el siguiente|salta|saltala|pasa|pasala|cambia|cambiala|otra|otro|adelanta)(?: (?:a la |al |la |el |de )?(?:siguiente )?(?:cancion|tema|musica))?(?: siguiente)?$").matches(t) -> return media(MediaKey.NEXT)
            Regex("^(?:anterior|la anterior|el anterior|cancion anterior|tema anterior|vuelve atras|regresa|atras|pon la anterior)$").matches(t) -> return media(MediaKey.PREVIOUS)
            Regex("^(?:continua|continuar|sigue|seguir|reanuda|reanudar|play|dale play|quita (?:la )?pausa|vuelve a poner|despausa|resume)$what$").matches(t) -> return media(MediaKey.RESUME)
        }

        // A family app's own tool («¿cuál es mi próxima alarma?», «agrega tarea comprar pan», «¿cuánto es 25 por 4?»): before the
        // other rules, which would only open the app. Arguments go as «arg:…» slots (numbers as «num:…»).
        Skills.find(t, today)?.let { (skill, args) ->
            return Command(Action.TOOL, mapOf("app" to skill.app, "tool" to skill.tool) + args.values.map { e -> (when (e.value) { is Int -> "num:"; is Boolean -> "bool:"; is Double -> "dbl:"; else -> "arg:" } + e.key) to e.value.toString() })
        }

        // The call ringing now: «contesta», «atiende la llamada», «cuelga», «rechaza la llamada». The user's own voice only;
        // remote AIs never get this (it is not an MCP tool).
        val call = "(?: (?:la |esta |el )?(?:llamada|telefono|celular))?"
        if (Regex("^(?:contesta|contestar|contestale|atiende|atender|responde|responder|acepta|aceptar|coge|toma)$call$").matches(t)) return Command(Action.ANSWER)
        if (Regex("^(?:cuelga|colgar|cuelgale|rechaza|rechazar|rechazala|corta|cortar|termina|terminar|no contestes)$call$").matches(t)) return Command(Action.HANG_UP)

        // A quick note in Notas: «anota que mañana compro pan», «crea una nota comprar leche», «apunta llamar a Ana».
        Regex("^(?:anota|apunta|toma nota|guarda una nota|crea una nota|nueva nota|haz una nota|escribe una nota|recuerda)(?: (?:de )?que| de)? (.+)$").find(t)?.let { m ->
            return Command(Action.NOTE, mapOf("text" to m.groupValues[1].trim()))
        }

        // The torch: «enciende la linterna», «apaga la linterna», «prende la luz».
        if (has(t, "linterna", "flash", "luz")) {
            if (Regex("^(?:enciende|encender|prende|prender|activa|activar|pon|abre)").containsMatchIn(t)) return Command(Action.TORCH, mapOf("on" to "1"))
            if (Regex("^(?:apaga|apagar|desactiva|desactivar|quita|cierra)").containsMatchIn(t)) return Command(Action.TORCH, mapOf("on" to "0"))
        }

        // Timer before alarm: «pon un temporizador de 10 minutos», «avísame en 5 minutos».
        if (has(t, "temporizador", "cuenta regresiva") || Regex("^(avisame|recuerdame|despiertame) en ").containsMatchIn(t)) {
            Words.duration(t)?.let { return Command(Action.TIMER, mapOf("seconds" to "$it")) }
            if (has(t, "temporizador")) return Command(Action.OPEN, mapOf("app" to "reloj"))
        }

        // Counting aloud: «a contar», «vamos a contar», «nuevo conteo casa Pérez», «modo conteo».
        Regex("^(?:a contar|contar|vamos a contar|empieza a contar|empezar a contar|quiero contar|ayudame a contar|modo conteo|modo contar|(?:nuevo |inicia (?:un )?|empieza (?:un )?|haz (?:un )?|un )?conteo)(?: (?:de la|en la|del|de|para|en) (.+)| (.+))?$").find(t)?.let { m ->
            val title = m.groupValues[1].ifEmpty { m.groupValues[2] }.trim()
            return Command(Action.COUNT, if (title.isEmpty()) emptyMap() else mapOf("title" to title))
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
        Regex("^(reproduce|reproducir|pon|ponme|toca|escuchar|quiero escuchar|play)( algo de| un poco de)?( la)? (musica|canciones|una cancion|algo)(?:( de)? (.+))?$").find(t)?.let { m ->
            return Command(Action.PLAY_MUSIC, m.groupValues[6].takeIf { it.isNotBlank() }?.let { mapOf("query" to it) } ?: emptyMap())
        }
        Regex("^(reproduce|pon|ponme|toca) (.+)$").find(t)?.let { m ->
            if (Catalog.find(m.groupValues[2]) == null) return Command(Action.PLAY_MUSIC, mapOf("query" to m.groupValues[2]))
        }

        // Questions the assistant answers itself, with the family's summaries.
        if (has(t, "que hora es", "hora es", "dime la hora")) return Command(Action.TIME)
        if (has(t, "que dia es", "que fecha es", "dia es hoy", "fecha de hoy")) return Command(Action.DATE)
        if (t == "tiempo" || has(t, "clima", "tiempo hace", "que tiempo", "va a llover", "va llover", "llovera", "temperatura", "hace frio", "hace calor")) return Command(Action.WEATHER)
        if (has(t, "que tengo", "tengo hoy", "mi agenda", "mis eventos", "tengo hoy", "eventos de hoy", "citas")) return Command(Action.AGENDA)
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

    private var today: java.time.LocalDate = java.time.LocalDate.now()

    private fun media(k: MediaKey) = Command(Action.MEDIA, mapOf("key" to k.name))
}
