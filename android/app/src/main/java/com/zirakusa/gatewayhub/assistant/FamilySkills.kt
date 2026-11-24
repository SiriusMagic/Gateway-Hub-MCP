package com.zirakusa.gatewayhub.assistant

import com.zirakusa.gatewayhub.assistant.Skills.fixed
import com.zirakusa.gatewayhub.assistant.Skills.one
import com.zirakusa.gatewayhub.assistant.Skills.r
import com.zirakusa.gatewayhub.assistant.Skills.whole

/**
 * The rest of the family's MCP tools as things to say: Reloj, Notas, Calendario, Clima, SMS, Teléfono, Calculadora, Luma,
 * Filem, Redes, Brújula, Nivel, Inicio and Cámara. What deletes, moves, renames or takes a photo asks first. Pure.
 */
object FamilySkills {
    /** Cámara's photo by voice: not an MCP tool, an order to Cámara (see [Hands.capture]). */
    const val CAPTURE = "camera_capture"

    private val ALARM = Lookup("clock_alarms", "alarms", "time", "time", "id", "id")
    private val NOTE = Lookup("notes_search", "notes", "title", "note", "id", "id", with = { mapOf("query" to (it["note"] ?: ""), "limit" to 10) }, first = true)
    private val TASK = Lookup("tasks_list", "tasks", "text", "text", "id", "id")
    /** An event of the coming year by its words. */
    private val EVENT = Lookup("calendar_events", "events", "title", "event", "id", "id",
        with = { mapOf("from" to (it["_from"] ?: ""), "to" to (it["_to"] ?: ""), "query" to (it["event"] ?: "")) }, first = true)
    private val PLACE = Lookup("weather_places", "places", "name", "place", "id", "id")
    private val THREAD = Lookup("sms_threads", "threads", "who", "who", "thread_id", "thread_id", with = { mapOf("limit" to 50) })
    private val FILE = Lookup("files_search", "files", "name", "file", "path", "path", with = { mapOf("query" to (it["file"] ?: "")) }, first = true)
    private val UNITS = mapOf("kilometros" to "km", "kilometro" to "km", "metros" to "m", "metro" to "m", "centimetros" to "cm", "millas" to "mi", "milla" to "mi",
        "pies" to "ft", "pulgadas" to "in", "kilos" to "kg", "kilogramos" to "kg", "gramos" to "g", "libras" to "lb", "onzas" to "oz",
        "litros" to "l", "litro" to "l", "galones" to "gal", "mililitros" to "ml", "grados centigrados" to "c", "celsius" to "c", "fahrenheit" to "f")
    private val UNIT_KIND = mapOf("km" to "length", "m" to "length", "cm" to "length", "mi" to "length", "ft" to "length", "in" to "length",
        "kg" to "mass", "g" to "mass", "lb" to "mass", "oz" to "mass", "l" to "volume", "gal" to "volume", "ml" to "volume", "c" to "temperature", "f" to "temperature")
    private val MONEY = mapOf("dolares" to "USD", "dolar" to "USD", "euros" to "EUR", "euro" to "EUR", "pesos colombianos" to "COP", "soles" to "PEN",
        "pesos mexicanos" to "MXN", "libras esterlinas" to "GBP", "yenes" to "JPY", "reales" to "BRL")
    private fun keyOf(map: Map<String, String>) = map.keys.sortedByDescending { it.length }.joinToString("|")

    val all = listOf(
        // Reloj
        Skill("clock_alarm_delete", "reloj", "Borrar alarma", confirm = "¿Borro la alarma de las {found}?", done = "Alarma de las {found} borrada.", lookup = ALARM) { s, _ ->
            r("(?:borra|elimina) (?:la )?alarma (?:de las |de la |de )?(.+)").find(s.t)?.let { m -> Words.clock(m.groupValues[1])?.let { (h, min) -> Args().put("time", "%02d:%02d".format(h, min)) } }
        },
        // Notas
        Skill("notes_get", "notas", "Leer nota", lookup = NOTE, read = one("(?:lee|muestrame|abre|que dice) (?:la |mi )?nota(?: de| sobre| llamada)? (.+)", "note")),
        Skill("notes_update", "notas", "Añadir a nota", done = "Añadido a la nota {found}.", lookup = NOTE) { s, _ ->
            r("(?:agrega|anade|apunta|escribe) (?:en|a) (?:la |mi )?nota (.+?) (?:que |esto |lo siguiente )?:? ?(.+)").find(s.t)?.let { m -> Args().put("note", s.group(m, 1)).put("append", s.group(m, 2)) }
        },
        Skill("notes_pin", "notas", "Fijar nota", done = "Nota {found} {_how}.", lookup = NOTE) { s, _ ->
            r("(fija|ancla|desfija|suelta) (?:la |mi )?nota (.+)").find(s.t)?.let { m ->
                val on = m.groupValues[1] in setOf("fija", "ancla"); Args().put("note", s.group(m, 2)).put("pinned", on).put("_how", if (on) "fijada" else "soltada")
            }
        },
        Skill("notes_delete", "notas", "Borrar nota", confirm = "¿Borro la nota {found}? Va a la papelera de Notas.", done = "Nota {found} borrada.", lookup = NOTE,
            read = one("(?:borra|elimina|tira) (?:la |mi )?nota (.+)", "note")),
        Skill("notes_folders", "notas", "Carpetas", read = whole("(?:mis|que) carpetas (?:de notas|tengo en notas)", "carpetas de notas")),
        Skill("folders_create", "notas", "Nueva carpeta", done = "Carpeta {name} creada en Notas.", read = one("crea (?:una )?carpeta (?:de notas )?(?:llamada )?(.+?)(?: en notas)?", "name")),
        Skill("tasks_delete", "notas", "Borrar tarea", confirm = "¿Borro la tarea «{found}»?", done = "Tarea borrada.", lookup = TASK,
            read = one("(?:borra|elimina|quita) (?:la )?tarea (.+)", "text")),
        // Calendario: «posponer» is calendar_event_move.
        Skill("calendar_event_move", "calendario", "Posponer", confirm = "¿Muevo «{found}» al {_when}?", done = "Listo: «{found}» ahora es el {_when}.", lookup = EVENT) { s, today ->
            r("(?:pospon|posponer|mueve|cambia|pasa|aplaza|reprograma) (?:la |el |mi )?(.+?) (?:para |al |a )(?:el )?(${Skills.DAY_WORDS}|\\d{1,2})(?: a la| a las| a)? ?(.*)").find(s.t)?.let { m ->
                val day = Skills.day(m.groupValues[2], today) ?: return@let null
                Args().put("event", s.group(m, 1)).put("date", day.toString()).put("_from", today.toString()).put("_to", today.plusDays(365).toString())
                    .put("_when", m.groupValues[2] + (Words.clock(m.groupValues[3])?.let { (h, min) -> " a las %d:%02d".format(h, min) } ?: "")).apply {
                        Words.clock(m.groupValues[3])?.let { (h, min) -> put("time", "%02d:%02d".format(h, min)) }
                    }
            }
        },
        Skill("calendar_event_delete", "calendario", "Borrar evento", confirm = "¿Borro «{found}» del calendario?", done = "«{found}» borrado del calendario.", lookup = EVENT) { s, today ->
            r("(?:borra|elimina|cancela|quita) (?:el evento|la cita|la reunion|del calendario)? ?(.+?)(?: del calendario)?").find(s.t)?.takeIf { s.t.contains("calendario") || s.t.contains("evento") || s.t.contains("cita") || s.t.contains("reunion") }
                ?.let { m -> Args().put("event", s.group(m, 1).removePrefix("la ").removePrefix("el ")).put("_from", today.toString()).put("_to", today.plusDays(365).toString()) }
        },
        // Clima
        Skill("weather_now", "clima", "Clima ahora", read = whole("(?:que temperatura hace|cuantos grados hace|como esta el clima|clima)(?: en este momento)?")),
        Skill("weather_places", "clima", "Mis lugares", read = whole("(?:mis|que) lugares (?:del clima|tengo en clima)")),
        Skill("weather_place_remove", "clima", "Quitar lugar", confirm = "¿Quito {found} del clima?", done = "{found} quitado del clima.", lookup = PLACE,
            read = one("(?:quita|borra|elimina) (.+?) (?:del clima|de clima|de mis lugares)", "place")),
        // SMS y Teléfono
        Skill("sms_messages", "sms", "Leer conversación", lookup = THREAD, read = one("(?:lee|muestrame) (?:los )?(?:mensajes|sms|la conversacion) (?:de|con) (.+)", "who")),
        Skill("phone_calls", "telefono", "Últimas llamadas", read = whole("(?:mis )?(?:ultimas llamadas|llamadas recientes)", "historial de llamadas", "a quien llame")),
        // Calculadora
        Skill("calc_convert_units", "calculadora", "Convertir unidades") { s, _ ->
            r("(?:convierte|cuantos|cuantas|cuanto son|pasa) ([\\d.,]+) (${keyOf(UNITS)}) (?:a|en) (${keyOf(UNITS)})").find(s.t)?.let { m ->
                val from = UNITS.getValue(m.groupValues[2]); val to = UNITS.getValue(m.groupValues[3])
                if (UNIT_KIND[from] != UNIT_KIND[to]) null else Args().put("value", m.groupValues[1].replace(',', '.')).put("category", UNIT_KIND.getValue(from)).put("from", from).put("to", to)
            }
        },
        Skill("calc_convert_currency", "calculadora", "Cambio de moneda") { s, _ ->
            r("(?:convierte|cuantos|cuantas|cuanto son) ([\\d.,]+) (${keyOf(MONEY)}) (?:a|en) (${keyOf(MONEY)})").find(s.t)?.let { m ->
                Args().put("amount", m.groupValues[1].replace(',', '.')).put("from", MONEY.getValue(m.groupValues[2])).put("to", MONEY.getValue(m.groupValues[3]))
            }
        },
        Skill("calc_history", "calculadora", "Historial", read = whole("historial de la calculadora", "(?:mis )?ultimas cuentas")),
        Skill("calc_history_clear", "calculadora", "Borrar historial", confirm = "¿Borro el historial de la Calculadora?", done = "Historial de la Calculadora borrado.",
            read = whole("(?:borra|limpia) el historial de la calculadora")),
        // Luma
        Skill("gallery_albums", "luma", "Álbumes", read = whole("(?:mis|que) albumes (?:de fotos|tengo)", "albumes de fotos")),
        Skill("gallery_recent", "luma", "Últimas fotos", read = whole("(?:mis )?(?:ultimas|fotos recientes)(?: fotos)?", "las ultimas fotos que tome")),
        Skill("gallery_by_date", "luma", "Fotos de un día") { s, today ->
            r("(?:fotos|las fotos) (?:de |del )?(${Skills.DAY_WORDS}|ayer)").find(s.t)?.let { m ->
                val d = if (m.groupValues[1] == "ayer") today.minusDays(1) else Skills.dayBack(m.groupValues[1], today)
                d?.let { Args().put("from", it.toString()).put("to", it.toString()) }
            }
        },
        // Filem
        Skill("files_trash", "filem", "Borrar archivo", confirm = "¿Envío «{found}» a la papelera de Filem?", done = "«{found}» está en la papelera.", lookup = FILE,
            read = one("(?:borra|elimina|tira) (?:el )?(?:archivo|documento) (.+)", "file")),
        Skill("files_rename", "filem", "Renombrar archivo", confirm = "¿Cambio el nombre de «{found}» a «{new_name}»?", done = "Ahora se llama «{new_name}».", lookup = FILE) { s, _ ->
            r("(?:cambia el nombre del|renombra el) (?:archivo|documento) (.+?) (?:a|por|como) (.+)").find(s.t)?.let { m -> Args().put("file", s.group(m, 1)).put("new_name", s.group(m, 2)) }
        },
        // Redes, Brújula, Nivel, Inicio
        Skill("network_speed_test", "redes", "Medir velocidad", confirm = "¿Mido la velocidad? Usa unos megas de tus datos.",
            read = fixed({ Args().put("allow_mobile", true) }, "mide la velocidad(?: de internet)?", "haz una prueba de velocidad", "que tan rapido es mi internet")),
        Skill("network_speed_history", "redes", "Pruebas anteriores", read = whole("(?:mis )?pruebas de velocidad(?: anteriores)?", "historial de velocidad")),
        Skill("network_wifi_nearby", "redes", "Wi‑Fi cercanas", read = whole("(?:que )?redes (?:wifi )?(?:hay )?cerca(?:nas)?", "wifis cercanas")),
        Skill("compass_location", "brujula", "Dónde estoy", read = whole("donde estoy", "(?:cual es )?mi ubicacion")),
        Skill("level_measurements", "nivel", "Mediciones", read = whole("(?:mis )?mediciones(?: del nivel)?")),
        Skill("launcher_apps", "inicio", "Mis apps", read = whole("que (?:apps|aplicaciones) tengo", "mis (?:apps|aplicaciones)")),
        Skill("launcher_dock", "inicio", "Mi dock", read = whole("que (?:hay|apps hay) en (?:mi )?(?:dock|barra)", "mi dock")),
        // Cámara: settings, the flash, and the photo by voice (asked first, with a countdown on the phone).
        Skill("camera_flash", "camara", "Flash", "camera_settings_set", done = "Flash de la cámara: {_how}.") { s, _ ->
            r("(?:pon|enciende|activa|apaga|quita) (?:el flash|la linterna)(?: de la camara| en la camara)?(?: en| a)? ?(automatico|activado|apagado|encendido)?").find(s.t)
                ?.takeIf { s.t.contains("camara") || s.t.contains("flash") }?.let { m ->
                    val off = s.t.startsWith("apaga") || s.t.startsWith("quita") || m.groupValues[1] == "apagado"
                    val torch = s.t.contains("linterna")
                    val v = when { off -> "off"; torch -> "torch"; m.groupValues[1] == "automatico" -> "auto"; else -> "on" }
                    Args().put("flash", v).put("_how", mapOf("off" to "apagado", "torch" to "linterna encendida", "auto" to "automático", "on" to "activado").getValue(v))
                }
        },
        Skill("camera_timer", "camara", "Temporizador de cámara", "camera_settings_set", done = "Temporizador de la cámara: {_how}.") { s, _ ->
            r("(?:pon el )?temporizador de la camara (?:a |en |de )?(3|10|tres|diez|apagado)(?: segundos)?|(?:quita|apaga) el temporizador de la camara").find(s.t)?.let { m ->
                val v = when (m.groupValues[1]) { "3", "tres" -> "3"; "10", "diez" -> "10"; else -> "off" }
                Args().put("timer", v).put("_how", if (v == "off") "apagado" else "$v segundos")
            }
        },
        Skill("camera_recent", "camara", "Últimas fotos (Cámara)", read = whole("(?:mis )?ultimas fotos de la camara", "que fotos hice hoy")),
        Skill(CAPTURE, "camara", "Hacer foto", confirm = "¿Hago la {_what}? Verás una cuenta atrás de 3 segundos.", done = "Abriendo la cámara: {_what} en 3 segundos.") { s, _ ->
            when {
                r("(?:toma|tomame|haz|hazme|saca|sacame) (?:una )?(?:selfie|autofoto|foto de mi)").matches(s.t) -> Args().put("selfie", true).put("_what", "selfie")
                r("(?:toma|haz|saca|tomame|hazme|sacame) (?:una )?(?:foto|fotografia)(?: ahora| ya)?").matches(s.t) -> Args().put("selfie", false).put("_what", "foto")
                else -> null
            }
        },
    )
}
