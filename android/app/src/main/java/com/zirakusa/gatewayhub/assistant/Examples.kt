package com.zirakusa.gatewayhub.assistant

/**
 * Lucía's «sentimientos»: one example of everything she understands, each a phrase the rules already know (tested in
 * IntentsTest). A local model reads them to turn free speech into her own orders; it never calls a tool itself. Pure.
 */
object Examples {
    /** What Lucía does herself (no app tool). */
    val actions = listOf(
        "abre la cámara", "pon New Americana al 50 %", "pon la canción despacito", "pon mis recomendaciones", "pausa la música", "siguiente canción",
        "sube el volumen", "volumen al 30 por ciento", "pon una alarma a las 7 y media", "pon un temporizador de 5 minutos",
        "llama a Luis", "manda un mensaje a Ana que llego en 10 minutos", "qué hora es", "qué día es", "qué tiempo hace",
        "qué tengo hoy", "buenos días", "anota comprar pan", "vamos a contar", "enciende la linterna", "apaga la linterna",
        "contesta la llamada", "cuelga", "toma una foto", "lee un código qr", "mide la velocidad de internet",
        "suma dos más dos", "resta 3 a 10",
    )

    /** One phrase per family tool. */
    val tools: Map<String, String> = mapOf(
        "appearance_set_accent" to "cambia el color a rosa",
        "appearance_set_theme" to "cambia el tema a negro",
        "calc_convert_currency" to "convierte 20 dolares a euros",
        "calc_convert_units" to "convierte 5 kilómetros a millas",
        "calc_evaluate" to "cuánto es 25 por 4",
        "calc_history" to "historial de la calculadora",
        "calc_history_clear" to "borra el historial de la calculadora",
        "calc_percent" to "el 15 por ciento de 200",
        "calc_power" to "2 elevado a 10",
        "calc_sqrt" to "raíz cuadrada de 144",
        "calendar_day" to "qué tengo mañana",
        "calendar_event_create" to "crea un evento dentista mañana a las 3 de la tarde",
        "calendar_event_delete" to "cancela la cita del dentista",
        "calendar_event_move" to "pospón la reunión con Ana para el viernes a las 4",
        "calendar_events" to "qué tengo esta semana",
        "calendar_holidays" to "próximos feriados",
        "camera_flash" to "pon el flash de la cámara en automático",
        "camera_recent" to "mis últimas fotos de la cámara",
        "camera_settings" to "cómo está configurada la cámara",
        "camera_settings_set" to "activa la marca de agua",
        "camera_timer" to "temporizador de la cámara a 10 segundos",
        "clock_alarm_delete" to "borra la alarma de las 7",
        "clock_alarm_enable" to "apaga la alarma de las 7",
        "clock_alarms" to "qué alarmas tengo",
        "clock_next_alarm" to "¿Cuál es mi próxima alarma?",
        "clock_world_time" to "qué hora es en Madrid",
        "compass_heading" to "hacia dónde estoy mirando",
        "compass_location" to "dónde estoy",
        "contacts_birthdays" to "quién cumple años esta semana",
        "contacts_create" to "crea un contacto Ana López con el número 0991234567",
        "contacts_favorite" to "marca a Ana como favorita",
        "contacts_favorites" to "mis contactos favoritos",
        "contacts_search" to "cuál es el número de Ana",
        "equalizer_set_enabled" to "activa el ecualizador",
        "equalizer_set_preset" to "más graves",
        "files_create_folder" to "crea una carpeta facturas en documentos",
        "files_list" to "qué hay en descargas",
        "files_recent" to "archivos recientes",
        "files_rename" to "renombra el archivo factura a recibo",
        "files_search" to "busca el archivo factura",
        "files_storage" to "cuánto espacio me queda",
        "files_trash" to "borra el archivo factura",
        "folders_create" to "crea una carpeta trabajo",
        "gallery_albums" to "mis álbumes de fotos",
        "gallery_by_date" to "fotos de ayer",
        "gallery_recent" to "mis últimas fotos",
        "gallery_search" to "busca videos de gatos",
        "gallery_stats" to "cuántas fotos tengo",
        "launcher_apps" to "qué apps tengo",
        "launcher_dock_add" to "pon Clima en el dock",
        "launcher_dock_remove" to "quita Clima del dock",
        "launcher_dock" to "qué hay en mi dock",
        "level_measurements" to "mis mediciones",
        "level_reading" to "está nivelado",
        "level_calibration" to "cómo está calibrado el nivel",
        "library_browse" to "mis artistas",
        "library_favorites" to "mis canciones favoritas",
        "library_lyrics" to "muéstrame la letra",
        "library_rescan" to "busca música nueva",
        "library_sort" to "ordena mis canciones por artista",
        "library_stats" to "cuántas canciones tengo",
        "metadata_fetch_lyrics" to "busca la letra de esta canción",
        "metadata_undo" to "deshaz el último cambio",
        "metadata_update" to "cambia el artista de esta canción a Soda Stereo",
        "network_latency" to "cuál es mi ping",
        "network_latency_test" to "mide la latencia",
        "network_signal" to "qué tal mi señal",
        "network_speed_history" to "mis pruebas de velocidad",
        "network_speed_test" to "mide la velocidad de internet",
        "network_status" to "cómo está mi conexión",
        "network_wifi_nearby" to "qué redes wifi hay cerca",
        "notes_delete" to "borra la nota compras",
        "notes_folders" to "mis carpetas de notas",
        "notes_get" to "lee la nota receta",
        "notes_list" to "qué notas tengo",
        "notes_move" to "mueve la nota compras a la carpeta casa",
        "notes_pin" to "fija la nota compras",
        "notes_search" to "busca en mis notas receta",
        "notes_update" to "agrega a la nota compras leche",
        "phone_calls" to "mis últimas llamadas",
        "phone_missed" to "tengo llamadas perdidas",
        "player_queue" to "qué sigue",
        "player_queue_clear" to "vacía la cola",
        "player_queue_next" to "pon bohemian rhapsody a continuación",
        "player_seek" to "ve al minuto 2",
        "player_set_favorite" to "me gusta esta canción",
        "player_set_repeat" to "repite esta canción siempre",
        "player_set_shuffle" to "pon aleatorio",
        "player_set_shuffle_off" to "quita el aleatorio",
        "player_set_speed" to "más rápido",
        "player_sleep_timer" to "apaga la música en 30 minutos",
        "player_sleep_timer_extend" to "dame 15 minutos más",
        "player_sleep_timer_off" to "cancela el temporizador de música",
        "player_state" to "qué está sonando",
        "player_unfavorite" to "quita esta canción de favoritos",
        "playlist_add_songs" to "agrega esta canción a la lista gym",
        "playlist_create" to "crea una lista llamada gym",
        "playlist_delete" to "borra la lista gym",
        "playlist_list" to "qué listas tengo",
        "playlist_play" to "pon la lista rock de los 80",
        "playlist_remove_songs" to "quita esta canción de la lista gym",
        "playlist_rename" to "renombra la lista gym a correr",
        "playlist_songs" to "qué canciones tiene la lista gym",
        "qr_history" to "últimos qr",
        "rec_list" to "mis grabaciones",
        "rec_get" to "detalles de la grabación reunión",
        "rec_rename" to "renombra la grabación reunión a junta",
        "settings_set_audio_mode" to "modo cine",
        "settings_set_transitions" to "activa el fundido entre canciones",
        "sms_messages" to "lee los mensajes de Ana",
        "sms_search" to "busca mensajes de banco",
        "sms_threads" to "mis conversaciones",
        "sms_unread" to "tengo mensajes sin leer",
        "tasks_complete" to "marca la tarea comprar pan como hecha",
        "tasks_create" to "agrega tarea comprar piñas",
        "tasks_delete" to "borra la tarea pagar luz",
        "tasks_list" to "qué tareas tengo",
        "video_library_stats" to "cuántos videos tengo en stone video",
        "video_play" to "pon el video de bad bunny",
        "video_search" to "busca el video de gatos",
        "video_set_brightness" to "brillo al 40 por ciento",
        "video_set_orientation" to "pon la pantalla horizontal",
        "video_set_scale" to "pantalla completa",
        "video_shuffle" to "pon los videos en aleatorio",
        "video_state" to "qué video es este",
        "weather_forecast" to "va a llover mañana",
        "weather_now" to "qué temperatura hace ahora",
        "weather_place_remove" to "quita Quito del clima",
        "weather_places" to "mis lugares del clima",
        "weather_search" to "busca Cuenca en el clima"
    )

    /**
     * One «regla de sentimiento»: a phrase Lucía understands and what it does. [app] is the family app whose tool runs
     * (null: Lucía does it herself), [label] what the tool does, [asks] whether she asks «¿…?» before doing it.
     */
    data class Rule(val phrase: String, val tool: String?, val app: String?, val label: String?, val asks: Boolean)

    /** Every rule, for the «Reglas de sentimientos» lists: Lucía's own first, then one per app tool. */
    val rules: List<Rule> by lazy {
        val skills = Skills.every.associateBy { it.tool }
        actions.map { Rule(it, null, null, null, asks = false) } + tools.map { (tool, phrase) ->
            val skill = skills[tool]
            Rule(phrase, tool, skill?.app ?: appOf(tool), skill?.label, asks = skill?.confirm != null)
        }
    }

    /** The rules of one family app (by its [Catalog] id), or Lucía's own ones when [app] is null. */
    fun rulesFor(app: String?): List<Rule> = rules.filter { it.app == app }

    /** A tool without its own skill entry: its app from the tool's prefix (as each app names its tools). */
    private fun appOf(tool: String): String? = when (tool.substringBefore('_')) {
        "library", "player", "playlist", "metadata", "equalizer", "settings", "appearance" -> "stone"
        "calc" -> "calculadora"; "calendar" -> "calendario"; "camera" -> "camara"; "clock" -> "reloj"; "compass" -> "brujula"
        "contacts" -> "contactos"; "files", "folders" -> "filem"; "gallery" -> "luma"; "launcher" -> "inicio"; "level" -> "nivel"
        "network" -> "redes"; "notes", "tasks" -> "notas"; "phone" -> "telefono"; "qr" -> "qr"; "rec" -> "grabadora"
        "sms" -> "sms"; "video" -> "video"; "weather" -> "clima"
        else -> null
    }

    /** The examples most alike [query] ([Meaning.rank]): the model reads only these, so it answers sooner. */
    fun closest(query: String, max: Int = 40): List<String> = Meaning.rank(query, actions + tools.values, max)
}
