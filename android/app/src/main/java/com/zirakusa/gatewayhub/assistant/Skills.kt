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

data class Skill(val tool: String, val app: String, val label: String, private val read: (Said, LocalDate) -> Args?) {
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
        // Reloj
        Skill("clock_next_alarm", "reloj", "Próxima alarma", whole("(?:cual es |cuando suena |a que hora suena )?(?:mi |la )?proxima alarma", "a que hora suena (?:mi |la )?alarma",
            "cuando suena (?:mi |la )?alarma")),
        Skill("clock_alarms", "reloj", "Mis alarmas", whole("(?:que|cuales|cuantas) alarmas tengo(?: puestas| activas)?", "(?:dime |lee |muestrame )?mis alarmas", "lista de alarmas")),
        Skill("clock_world_time", "reloj", "Hora en…", one("(?:que hora es|dime la hora|hora) en (.+)", "city")),
        // Notas
        Skill("tasks_list", "notas", "Mis tareas", whole("(?:que|cuales|cuantas) tareas (?:tengo|hay)(?: pendientes)?", "(?:dime |lee |muestrame )?mis tareas(?: pendientes)?",
            "tareas pendientes", "que tengo pendiente")),
        Skill("tasks_create", "notas", "Nueva tarea", one("(?:agrega|anade|crea|pon|apunta) (?:una |la )?(?:nueva )?tarea(?: de| para| que diga)? (.+)|nueva tarea (.+)", "text")),
        Skill("tasks_complete", "notas", "Tarea hecha", one("(?:marca|pon) (?:la )?tarea (.+?) como (?:hecha|completada|terminada)|(?:ya )?(?:termine|hice|complete) (?:la )?tarea (.+)", "text")),
        Skill("notes_search", "notas", "Buscar notas", one("busca (?:en (?:mis )?notas|la nota|una nota|notas)(?: de| sobre| que diga)? (.+)", "query")),
        Skill("notes_list", "notas", "Mis notas", whole("(?:que|cuales|cuantas) notas tengo", "(?:dime |lee |muestrame )?mis notas", "mis ultimas notas")),
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
        Skill("weather_forecast", "clima", "Pronóstico", whole("(?:el )?(?:clima|tiempo|pronostico)(?: de| para)? manana", "va a llover manana", "(?:el )?pronostico(?: del tiempo| de la semana)?",
            "como estara el (?:clima|tiempo)(?: manana)?")) ,
        // Teléfono y Contactos (Contactos lives inside Teléfono)
        Skill("phone_missed", "telefono", "Perdidas", whole("(?:tengo |hay )?llamadas perdidas", "(?:quien|alguien) me (?:llamo|ha llamado)(?: hoy)?", "me llamaron")),
        Skill("contacts_search", "telefono", "Número de…", one("(?:cual es |dime |dame )?(?:el )?(?:numero|telefono|celular) de (.+)|busca (?:el )?contacto (.+)", "query")),
        Skill("contacts_birthdays", "telefono", "Cumpleaños", whole("(?:quien|quienes) cumple(?:n)? anos(?: pronto| esta semana| este mes)?", "(?:proximos )?cumpleanos(?: proximos)?")),
        // SMS
        Skill("sms_unread", "sms", "Sin leer", whole("(?:tengo |hay )?(?:mensajes|sms)(?: nuevos| sin leer)", "(?:tengo|hay) (?:mensajes|sms)", "lee mis mensajes(?: nuevos)?")),
        // Calculadora
        Skill("calc_evaluate", "calculadora", "Calcular") { s, _ ->
            r("(?:cuanto es|cuanto son|calcula|calcular|resuelve) (.+)").find(s.t)?.let { Args().put("expression", expression(it.groupValues[1])) }
        },
        // Luma, Filem, QR
        Skill("gallery_stats", "luma", "Cuántas fotos", whole("cuantas fotos tengo", "cuantos videos tengo", "cuantas fotos y videos tengo")),
        Skill("files_storage", "filem", "Espacio libre", whole("cuanto espacio (?:libre )?(?:tengo|me queda|queda|hay)(?: en el telefono| en el celular)?", "espacio libre", "(?:como esta )?(?:el )?almacenamiento")),
        Skill("files_recent", "filem", "Archivos recientes", whole("(?:mis )?archivos recientes", "(?:mis )?ultimos archivos", "que descargue")),
        Skill("qr_history", "qr", "Últimos QR", whole("(?:mis )?(?:ultimos|historial de) (?:codigos )?qr", "que qr (?:lei|escanee)")),
        // Redes, Brújula, Nivel
        Skill("network_status", "redes", "Mi conexión", whole("(?:como esta|estado de) (?:mi |la )?(?:conexion|internet|wifi|red)", "a que (?:wifi|red) estoy conectado")),
        Skill("compass_heading", "brujula", "Dirección", whole("(?:hacia )?(?:donde|a donde) (?:estoy mirando|apunto|apunta el telefono)", "que direccion es", "donde esta el norte")),
        Skill("level_reading", "nivel", "¿Está nivelado?", whole("(?:esta|estoy) (?:nivelado|derecho|recto)", "lectura del nivel", "que inclinacion (?:tiene|hay)")),
    )

    /** The skill a phrase asks for, with its arguments. */
    fun find(text: String, today: LocalDate): Pair<Skill, Args>? = Said(text).let { said -> all.firstNotNullOfOrNull { s -> s.args(said, today)?.let { s to it } } }
    fun byTool(tool: String) = all.firstOrNull { it.tool == tool }
}
