package com.zirakusa.gatewayhub.assistant

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The family apps' MCP tools as things to say: each [Skill] is one tool of one app, the phrases that ask for it and the
 * arguments taken from them. Lucía runs it on the phone (no internet) through the app's own Modo MCP, so the app keeps
 * all its rules (the AI never sends an SMS, never deletes a contact…). Pure; every phrase is in the tests.
 */
/** A tool's arguments, plain (no Android): text and whole numbers. */
class Args { val values = LinkedHashMap<String, Any>(); fun put(key: String, value: Any): Args = apply { values[key] = value } }

/** [tool] names the skill (and its node on the map); [call] is the app's tool it runs (the same unless two skills share one). */
data class Skill(val tool: String, val app: String, val label: String, val call: String = tool, private val read: (Said, LocalDate) -> Args?) {
    fun args(said: Said, today: LocalDate) = read(said, today)
}

/**
 * What was said, as the parser normalised it ([original], with ñ) and with ñ as n ([t]) so the phrases match either way
 * («mañana» / «manana»). Both have the same length: pieces are cut from [original], so a task keeps its ñ.
 */
class Said(val original: String) {
    val t = original.replace('ñ', 'n')
    fun group(m: MatchResult, i: Int) = m.groups[i]?.range?.let { original.substring(it.first, it.last + 1) }.orEmpty()
}

object Skills {
    private fun r(pattern: String) = Regex("^(?:$pattern)$")
    /** A fixed set of arguments when one of [patterns] matches. */
    private fun fixed(args: () -> Args, vararg patterns: String): (Said, LocalDate) -> Args? = { s, _ -> if (patterns.any { r(it).matches(s.t) }) args() else null }
    private val num = Regex("(\\d+)")
    private fun whole(vararg patterns: String): (Said, LocalDate) -> Args? = { s, _ -> if (patterns.any { r(it).matches(s.t) }) Args() else null }
    private fun one(pattern: String, key: String): (Said, LocalDate) -> Args? = { s, _ ->
        r(pattern).find(s.t)?.let { m -> (1 until m.groups.size).map { s.group(m, it) }.firstOrNull { it.isNotBlank() }?.trim() }?.let { Args().put(key, it) }
    }

    private const val WEEK = "lunes|martes|miercoles|jueves|viernes|sabado|domingo"
    private val days = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")

    /** «hoy», «mañana», «pasado mañana», «el viernes», «el 25» → the date (the next one that comes). */
    fun day(word: String, today: LocalDate): LocalDate? {
        val w = word.trim().removePrefix("el ").removePrefix("este ").trim()
        return when {
            w == "hoy" -> today
            w == "manana" -> today.plusDays(1)
            w == "pasado manana" -> today.plusDays(2)
            w in days -> { val target = DayOfWeek.of(days.indexOf(w) + 1); var d = today.plusDays(1); while (d.dayOfWeek != target) d = d.plusDays(1); d }
            w.toIntOrNull() in 1..31 -> { val n = w.toInt(); val m = runCatching { today.withDayOfMonth(n) }.getOrNull()
                if (m != null && !m.isBefore(today)) m else runCatching { today.plusMonths(1).withDayOfMonth(n) }.getOrNull() }
            else -> null
        }
    }
    private const val DAY = "hoy|manana|pasado manana|(?:el |este )?(?:$WEEK)|el \\d{1,2}"

    /** «25 por 4», «100 mas 10 por ciento», «raiz de 81» → what Calculadora understands. */
    fun expression(said: String): String {
        var e = " $said "
        listOf(" por ciento" to "%", " porciento" to "%", " multiplicado por " to "*", " por " to "*", " x " to "*", " dividido para " to "/",
            " dividido entre " to "/", " dividido por " to "/", " entre " to "/", " mas " to "+", " menos " to "-", " elevado a la " to "^",
            " elevado a " to "^", " al cuadrado" to "^2", " al cubo" to "^3", "raiz cuadrada de " to "√", "raiz de " to "√", " coma " to ".")
            .forEach { (a, b) -> e = e.replace(a, b) }
        return e.replace(" ", "")
    }

    val all = listOf(
        // Stone (Música): what the player itself does — its own tools, so it really happens, not just opens.
        Skill("player_set_shuffle", "stone", "Aleatorio", read = fixed({ Args().put("enabled", true) }, "(?:pon|activa|modo|en) (?:el )?(?:modo )?aleatorio",
            "mezcla (?:las canciones|la musica|todo)", "aleatorio", "reproduce (?:en )?aleatorio")),
        Skill("player_set_shuffle_off", "stone", "Sin aleatorio", "player_set_shuffle", fixed({ Args().put("enabled", false) }, "(?:quita|desactiva|apaga|sin) (?:el )?(?:modo )?aleatorio",
            "en orden", "reproduce en orden")),
        Skill("player_set_repeat", "stone", "Repetir") { s, _ ->
            when {
                r("(?:repite|repetir|modo repetir|pon en bucle|en bucle)(?: (?:siempre )?(?:esta |la )?cancion| una| solo esta)? (?:siempre|en bucle|una y otra vez)|repetir (?:una|esta cancion|la cancion)|modo repetir una").matches(s.t) -> Args().put("mode", "one")
                r("(?:repite|repetir|pon en bucle) (?:toda la lista|todo|la lista|la cola)|modo repetir(?: todo)?").matches(s.t) -> Args().put("mode", "all")
                r("(?:quita|desactiva|sin) (?:el )?(?:modo )?repetir|no repitas|deja de repetir").matches(s.t) -> Args().put("mode", "off")
                else -> null
            }
        },
        Skill("playlist_play", "stone", "Poner lista", read = one("(?:pon|ponme|reproduce|toca|escuchar) (?:la |mi )?(?:lista|playlist)(?: de reproduccion)?(?: de| llamada)? (.+)", "name")),
        Skill("playlist_list", "stone", "Mis listas", read = whole("(?:que|cuales) (?:listas|playlists) tengo", "mis (?:listas|playlists)")),
        Skill("player_state", "stone", "¿Qué suena?", read = whole("que (?:cancion )?(?:esta sonando|suena|es esta)", "como se llama esta cancion", "quien canta(?: esta cancion| esto)?",
            "que estoy escuchando")),
        Skill("player_set_favorite", "stone", "Me gusta", read = fixed({ Args().put("favorite", true) }, "me gusta esta cancion", "(?:agrega|anade|pon|guarda)(?:la)? (?:esta cancion )?(?:a|en) (?:mis )?favoritos",
            "marca(?:la)? como favorita")),
        Skill("player_sleep_timer", "stone", "Apagar en…") { s, _ ->
            r("(?:apaga|para|deten|pausa) (?:la musica|stone|la cancion) en (\\d+) minutos?|temporizador de (?:sueno|musica) de (\\d+) minutos?|deja de sonar en (\\d+) minutos?")
                .find(s.t)?.let { m -> (1..3).map { m.groupValues[it] }.firstOrNull { it.isNotEmpty() }?.toIntOrNull() }?.let { Args().put("minutes", it) }
        },
        Skill("equalizer_set_preset", "stone", "Ecualizador") { s, _ ->
            when {
                r("(?:mas|sube(?: los)?) (?:graves|bajos)|(?:pon|activa) (?:los )?graves").matches(s.t) -> Args().put("preset", "bass")
                r("(?:mas|sube(?: los)?) agudos").matches(s.t) -> Args().put("preset", "treble")
                r("(?:mas|sube la) voz|(?:resalta|realza) la voz").matches(s.t) -> Args().put("preset", "voice")
                r("ecualizador normal|quita el ecualizador|sonido normal").matches(s.t) -> Args().put("preset", "normal")
                else -> null
            }
        },
        Skill("library_stats", "stone", "Cuántas canciones", read = whole("cuantas canciones tengo", "cuanta musica tengo")),
        // Reloj
        Skill("clock_next_alarm", "reloj", "Próxima alarma", read = whole("(?:cual es |cuando suena |a que hora suena )?(?:mi |la )?proxima alarma", "a que hora suena (?:mi |la )?alarma",
            "cuando suena (?:mi |la )?alarma")),
        Skill("clock_alarms", "reloj", "Mis alarmas", read = whole("(?:que|cuales|cuantas) alarmas tengo(?: puestas| activas)?", "(?:dime |lee |muestrame )?mis alarmas", "lista de alarmas")),
        Skill("clock_world_time", "reloj", "Hora en…", read = one("(?:que hora es|dime la hora|hora) en (.+)", "city")),
        // Notas
        Skill("tasks_list", "notas", "Mis tareas", read = whole("(?:que|cuales|cuantas) tareas (?:tengo|hay)(?: pendientes)?", "(?:dime |lee |muestrame )?mis tareas(?: pendientes)?",
            "tareas pendientes", "que tengo pendiente")),
        Skill("tasks_create", "notas", "Nueva tarea", read = one("(?:agrega|anade|crea|pon|apunta) (?:una |la )?(?:nueva )?tarea(?: de| para| que diga)? (.+)|nueva tarea (.+)", "text")),
        Skill("tasks_complete", "notas", "Tarea hecha", read = one("(?:marca|pon) (?:la )?tarea (.+?) como (?:hecha|completada|terminada)|(?:ya )?(?:termine|hice|complete) (?:la )?tarea (.+)", "text")),
        Skill("notes_search", "notas", "Buscar notas", read = one("busca (?:en (?:mis )?notas|la nota|una nota|notas)(?: de| sobre| que diga)? (.+)", "query")),
        Skill("notes_list", "notas", "Mis notas", read = whole("(?:que|cuales|cuantas) notas tengo", "(?:dime |lee |muestrame )?mis notas", "mis ultimas notas")),
        // Calendario
        // Today stays «Mi día» / «Agenda» (the summaries); other days ask Calendario.
        Skill("calendar_day", "calendario", "Agenda del día") { s, today ->
            r("(?:que tengo|que hay|mi agenda|mis eventos|eventos|citas|agenda)(?: para| de| del)? ($DAY)").find(s.t)?.let { day(it.groupValues[1], today) }
                ?.takeIf { it != today }?.let { Args().put("date", it.toString()) }
        },
        Skill("calendar_event_create", "calendario", "Nuevo evento") { s, today ->
            r("(?:crea|agenda|pon|anade|agrega|apunta) (?:un |una )?(?:evento|cita|reunion)(?: de| para| llamado| que diga)? (.+?) (?:para )?($DAY)(?: a la| a las| a)? ?(.*)")
                .find(s.t)?.let { m ->
                    val date = day(m.groupValues[2], today) ?: return@let null
                    Args().put("title", s.group(m, 1).replaceFirstChar { it.uppercase() }).put("date", date.toString()).apply {
                        Words.clock(m.groupValues[3])?.let { (h, min) -> put("time", "%02d:%02d".format(h, min)) }
                    }
                }
        },
        Skill("calendar_holidays", "calendario", "Feriados") { s, today ->
            r("(?:cuales son los |dime los |los )?(?:proximos )?(?:feriados|dias festivos)(?: de este ano| del ano| del (\\d{4}))?").find(s.t)
                ?.let { Args().put("year", it.groupValues[1].toIntOrNull() ?: today.year) }
        },
        // Clima
        Skill("weather_forecast", "clima", "Pronóstico", read = whole("(?:el )?(?:clima|tiempo|pronostico)(?: de| para)? manana", "va a llover manana", "(?:el )?pronostico(?: del tiempo| de la semana)?",
            "como estara el (?:clima|tiempo)(?: manana)?")) ,
        // Teléfono y Contactos (Contactos lives inside Teléfono)
        Skill("phone_missed", "telefono", "Perdidas", read = whole("(?:tengo |hay )?llamadas perdidas", "(?:quien|alguien) me (?:llamo|ha llamado)(?: hoy)?", "me llamaron")),
        Skill("contacts_search", "telefono", "Número de…", read = one("(?:cual es |dime |dame )?(?:el )?(?:numero|telefono|celular) de (.+)|busca (?:el )?contacto (.+)", "query")),
        Skill("contacts_birthdays", "telefono", "Cumpleaños", read = whole("(?:quien|quienes) cumple(?:n)? anos(?: pronto| esta semana| este mes)?", "(?:proximos )?cumpleanos(?: proximos)?")),
        // SMS
        Skill("sms_unread", "sms", "Sin leer", read = whole("(?:tengo |hay )?(?:mensajes|sms)(?: nuevos| sin leer)", "(?:tengo|hay) (?:mensajes|sms)", "lee mis mensajes(?: nuevos)?")),
        // Calculadora
        Skill("calc_evaluate", "calculadora", "Calcular") { s, _ ->
            r("(?:cuanto es|cuanto son|calcula|calcular|resuelve) (.+)").find(s.t)?.let { Args().put("expression", expression(it.groupValues[1])) }
        },
        // Luma, Filem, QR
        Skill("gallery_stats", "luma", "Cuántas fotos", read = whole("cuantas fotos tengo", "cuantos videos tengo", "cuantas fotos y videos tengo")),
        Skill("files_storage", "filem", "Espacio libre", read = whole("cuanto espacio (?:libre )?(?:tengo|me queda|queda|hay)(?: en el telefono| en el celular)?", "espacio libre", "(?:como esta )?(?:el )?almacenamiento")),
        Skill("files_recent", "filem", "Archivos recientes", read = whole("(?:mis )?archivos recientes", "(?:mis )?ultimos archivos", "que descargue")),
        Skill("qr_history", "qr", "Últimos QR", read = whole("(?:mis )?(?:ultimos|historial de) (?:codigos )?qr", "que qr (?:lei|escanee)")),
        // Redes, Brújula, Nivel
        Skill("network_status", "redes", "Mi conexión", read = whole("(?:como esta|estado de) (?:mi |la )?(?:conexion|internet|wifi|red)", "a que (?:wifi|red) estoy conectado")),
        Skill("compass_heading", "brujula", "Dirección", read = whole("(?:hacia )?(?:donde|a donde) (?:estoy mirando|apunto|apunta el telefono)", "que direccion es", "donde esta el norte")),
        Skill("level_reading", "nivel", "¿Está nivelado?", read = whole("(?:esta|estoy) (?:nivelado|derecho|recto)", "lectura del nivel", "que inclinacion (?:tiene|hay)")),
        // More of Reloj, Calendario, SMS, Contactos, Luma, Filem and Cámara.
        Skill("clock_alarm_enable", "reloj", "Encender/apagar alarma") { s, _ ->
            r("(?:(apaga|desactiva|quita|cancela)|(enciende|activa|pon)) (?:la )?alarma (?:de las |de la |de )?(.+)").find(s.t)?.let { m ->
                Words.clock(m.groupValues[3])?.let { (h, min) -> Args().put("time", "%02d:%02d".format(h, min)).put("enabled", m.groupValues[2].isNotEmpty()) }
            }
        },
        Skill("calendar_events", "calendario", "Esta semana") { s, today ->
            when {
                r("(?:que tengo|mis eventos|mi agenda|eventos|que hay)(?: para| de)? esta semana").matches(s.t) ->
                    Args().put("from", today.toString()).put("to", today.plusDays((7 - today.dayOfWeek.value).toLong()).toString())
                r("(?:que tengo|mis eventos|mi agenda|eventos|que hay)(?: para| de)? (?:la )?(?:proxima semana|semana que viene)").matches(s.t) ->
                    today.plusDays((8 - today.dayOfWeek.value).toLong()).let { Args().put("from", it.toString()).put("to", it.plusDays(6).toString()) }
                r("(?:que tengo|mis eventos|eventos|que hay)(?: para| de)? este mes").matches(s.t) ->
                    Args().put("from", today.toString()).put("to", today.withDayOfMonth(today.lengthOfMonth()).toString())
                else -> r("(?:cuando es|cuando tengo|busca en el calendario|busca el evento) (?:la |el |mi )?(.+)").find(s.t)?.let { m ->
                    Args().put("from", today.toString()).put("to", today.plusDays(90).toString()).put("query", s.group(m, 1))
                }
            }
        },
        Skill("sms_search", "sms", "Buscar mensajes", read = one("busca (?:en )?(?:los |mis )?(?:mensajes|sms)(?: de| que digan| con| sobre)? (.+)|(?:mensajes|sms) (?:de|que digan) (.+)", "query")),
        Skill("sms_threads", "sms", "Conversaciones", read = whole("(?:mis )?(?:conversaciones|chats de sms)", "(?:quien|quienes) me (?:escribio|escribieron)", "ultimos mensajes")),
        Skill("contacts_favorites", "telefono", "Favoritos", read = whole("(?:mis )?contactos favoritos", "(?:quienes son )?mis favoritos")),
        Skill("gallery_search", "luma", "Buscar fotos", read = one("(?:busca|encuentra|muestrame) (?:las |mis )?(?:fotos|imagenes|videos)(?: de| con| donde (?:sale|salga|hay))? (.+)", "query")),
        Skill("files_search", "filem", "Buscar archivo", read = one("(?:busca|encuentra) (?:el |un |mi )?(?:archivo|documento|pdf)(?: llamado| que se llama| de)? (.+)", "query")),
        Skill("camera_settings_set", "camara", "Ajustes de cámara") { s, _ ->
            r("(activa|pon|enciende|quita|desactiva|apaga) (?:la )?(marca de agua|cuadricula)(?: de la camara| en la camara)?").find(s.t)?.let { m ->
                val on = m.groupValues[1] in setOf("activa", "pon", "enciende")
                Args().put(if (m.groupValues[2] == "marca de agua") "watermark" else "grid", on)
            }
        },
    )

    /** The skill a phrase asks for, with its arguments. */
    fun find(text: String, today: LocalDate): Pair<Skill, Args>? = Said(text).let { said -> all.firstNotNullOfOrNull { s -> s.args(said, today)?.let { s to it } } }
    fun byTool(tool: String) = all.firstOrNull { it.tool == tool }
}
