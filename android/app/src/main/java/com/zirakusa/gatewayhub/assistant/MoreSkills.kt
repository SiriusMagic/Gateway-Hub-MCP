package com.zirakusa.gatewayhub.assistant

import com.zirakusa.gatewayhub.assistant.Skills.r
import com.zirakusa.gatewayhub.assistant.Skills.whole

/**
 * More of the family's MCP tools as feelings rules (RS), so Lucía reaches most of what the apps can do: the calculator's
 * percent, power and square root; moving notes; folders and listings in Filem; the level's calibration; the dock; places
 * in Clima; the latency test; the camera's settings; contacts (create, favourite); recordings (details, rename).
 * What changes something asks first. Pure; every phrase is in the tests.
 */
object MoreSkills {
    private val NOTE = Lookup("notes_search", "notes", "title", "note", "id", "id", with = { mapOf("query" to (it["note"] ?: ""), "limit" to 10) }, first = true)
    private val FOLDER = Lookup("notes_folders", "folders", "name", "folder", "id", "folder_id")
    private val APP = Lookup("launcher_apps", "apps", "name", "app", "package", "package", with = { mapOf("query" to (it["app"] ?: "")) }, first = true)
    private val CONTACT = Lookup("contacts_search", "contacts", "name", "who", "id", "id", with = { mapOf("query" to (it["who"] ?: ""), "limit" to 10) }, first = true)
    private val RECORDING = Lookup("rec_list", "recordings", "name", "recording", "id", "id", with = { mapOf("query" to (it["recording"] ?: "")) }, first = true)
    private const val NUM = "(\\d+(?:[.,]\\d+)?)"
    /** Filem's folders by their everyday name. */
    private val FOLDERS = mapOf("descargas" to "Download", "documentos" to "Documents", "musica" to "Music", "fotos" to "DCIM", "camara" to "DCIM/Camera",
        "imagenes" to "Pictures", "videos" to "Movies", "almacenamiento" to "", "telefono" to "")
    private fun num(text: String) = text.replace(',', '.').toDouble()

    val all = listOf(
        // Calculadora: through its engine, exact.
        Skill("calc_percent", "calculadora", "Porcentaje") { s, _ ->
            r("(?:saca |calcula )?(?:el )?$NUM (?:por ciento|porciento|%) de $NUM").find(s.t)?.let { m -> Args().put("percent", num(m.groupValues[1])).put("of", num(m.groupValues[2])) }
        },
        Skill("calc_power", "calculadora", "Potencia") { s, _ ->
            r("(?:calcula )?$NUM elevado a(?: la)? $NUM").find(s.t)?.let { m -> Args().put("base", num(m.groupValues[1])).put("exponent", num(m.groupValues[2])) }
        },
        Skill("calc_sqrt", "calculadora", "Raíz cuadrada") { s, _ ->
            r("(?:calcula |saca )?(?:la )?raiz(?: cuadrada)? de $NUM").find(s.t)?.let { m -> Args().put("x", num(m.groupValues[1])) }
        },
        // Notas: a note into a folder.
        Skill("notes_move", "notas", "Mover nota", confirm = "¿Muevo la nota {found} a la carpeta {found2}?", done = "Nota {found} movida a {found2}.",
            lookup = NOTE, also = FOLDER) { s, _ ->
            r("(?:mueve|pasa|guarda) (?:la |mi )?nota (.+?) a (?:la carpeta |mi carpeta )(.+)").find(s.t)?.let { m -> Args().put("note", s.group(m, 1)).put("folder", s.group(m, 2)) }
        },
        // Filem: what is in a folder, and a new folder.
        Skill("files_list", "filem", "Qué hay en…") { s, _ ->
            r("que (?:hay|archivos hay|tengo) en (?:la carpeta |mis )?(${FOLDERS.keys.joinToString("|")})").find(s.t)?.let { m ->
                Args().put("path", FOLDERS.getValue(m.groupValues[1])).put("sort", "date").put("descending", true).put("limit", 10)
            }
        },
        Skill("files_create_folder", "filem", "Nueva carpeta (archivos)", done = "Carpeta {name} creada en Filem.") { s, _ ->
            r("crea (?:una )?carpeta (?:llamada )?(.+?) en (?:filem|mis archivos|archivos|(${FOLDERS.keys.joinToString("|")}))").find(s.t)?.let { m ->
                Args().put("path", FOLDERS[m.groupValues[2]] ?: "").put("name", s.group(m, 1))
            }
        },
        // Nivel
        Skill("level_calibration", "nivel", "Calibración", read = whole("(?:como esta |esta )?calibrado el nivel", "calibracion del nivel")),
        // Inicio: the dock.
        Skill("launcher_dock_add", "inicio", "Añadir al dock", done = "{found} está en el dock.", lookup = APP) { s, _ ->
            r("(?:pon|anade|agrega) (.+?) (?:en el|al) dock").find(s.t)?.let { m -> Args().put("app", s.group(m, 1)) }
        },
        Skill("launcher_dock_remove", "inicio", "Quitar del dock", confirm = "¿Quito {found} del dock? La app sigue instalada.", done = "{found} ya no está en el dock.",
            lookup = APP) { s, _ ->
            r("(?:quita|saca) (.+?) del dock").find(s.t)?.let { m -> Args().put("app", s.group(m, 1)) }
        },
        // Clima: looking for a place (adding one needs its coordinates, so it is done in Clima).
        Skill("weather_search", "clima", "Buscar lugar") { s, _ ->
            r("busca (?:la ciudad |el lugar )?(.+?) en (?:el )?clima").find(s.t)?.let { m -> Args().put("query", s.group(m, 1)) }
        },
        // Redes: a few KB of data, so it asks first.
        Skill("network_latency_test", "redes", "Medir latencia", confirm = "¿Mido la latencia ahora? Usa unos pocos KB de datos.") { s, _ ->
            if (r("(?:mide|prueba|haz una prueba de) (?:la latencia|el ping)(?: ahora)?").matches(s.t)) Args().put("count", 10) else null
        },
        // Cámara
        Skill("camera_settings", "camara", "Ajustes de la cámara", read = whole("(?:como esta configurada|ajustes de|configuracion de) la camara")),
        // Teléfono › Contactos: create and favourite; never delete.
        Skill("contacts_create", "telefono", "Nuevo contacto", confirm = "¿Creo el contacto {name}{_with}?", done = "Contacto {name} creado.") { s, _ ->
            r("(?:crea|guarda|anade|agrega) (?:un |el )?(?:nuevo )?contacto (.+?)(?: con el (?:numero|telefono) ([\\d +]{6,}))?").find(s.t)?.let { m ->
                val phone = m.groupValues[2].replace(" ", "")
                Args().put("name", s.group(m, 1)).apply { if (phone.isNotEmpty()) put("phone", phone) }.put("_with", if (phone.isEmpty()) "" else " con el número $phone")
            }
        },
        Skill("contacts_favorite", "telefono", "Contacto favorito", confirm = "¿{_how} a {found}?", done = "Listo: {found}.", lookup = CONTACT) { s, _ ->
            r("(?:(marca|pon) a (.+?) (?:como|en) favorit[oa]s?|(quita) a (.+?) de (?:mis )?favoritos)").find(s.t)?.let { m ->
                val on = m.groupValues[3].isEmpty()
                Args().put("who", s.group(m, if (on) 2 else 4)).put("favorite", on).put("_how", if (on) "Marco como favorito" else "Quito de favoritos")
            }
        },
        // Grabadora: details and a new name (recording and deleting stay on the phone).
        Skill("rec_get", "grabadora", "Detalles de grabación", lookup = RECORDING) { s, _ ->
            r("(?:detalles|cuanto dura|informacion) de la grabacion (.+)").find(s.t)?.let { m -> Args().put("recording", s.group(m, 1)) }
        },
        Skill("rec_rename", "grabadora", "Renombrar grabación", confirm = "¿Cambio el nombre de «{found}» a «{name}»?", done = "Ahora se llama «{name}».",
            lookup = RECORDING) { s, _ ->
            r("(?:cambia el nombre de la|renombra la) grabacion (.+?) (?:a|por|como) (.+)").find(s.t)?.let { m -> Args().put("recording", s.group(m, 1)).put("name", s.group(m, 2)) }
        },
    )
}
