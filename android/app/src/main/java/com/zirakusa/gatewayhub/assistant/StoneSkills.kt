package com.zirakusa.gatewayhub.assistant

import com.zirakusa.gatewayhub.assistant.Skills.fixed
import com.zirakusa.gatewayhub.assistant.Skills.one
import com.zirakusa.gatewayhub.assistant.Skills.r
import com.zirakusa.gatewayhub.assistant.Skills.whole

/**
 * Stone's MCP tools as things to say (Música): songs, lists, the queue, speed, equalizer, lyrics, tags, look and sound.
 * What deletes or edits asks first. «Esta canción» is the one playing (looked up with player_state). Pure.
 */
object StoneSkills {
    private const val SONG = "(?:la )?cancion"
    /** The song playing, for «esta canción». */
    private val CURRENT = Lookup("player_state", "", "title", "_now", "songId", "songId")
    /** A song by what was said (title, artist…): Stone's own search, its best result. */
    private val SEARCH = Lookup("library_search", "results", "title", "song", "id", "songId", with = { mapOf("query" to (it["song"] ?: ""), "limit" to 10) }, first = true)
    private val LIST = Lookup("playlist_list", "playlists", "name", "list", "id", "playlistId")
    private val fields = mapOf("titulo" to "title", "nombre" to "title", "artista" to "artist", "cantante" to "artist", "album" to "album", "genero" to "genre")
    private val themes = mapOf("oscuro" to "dark", "gris" to "gray", "medianoche" to "midnight", "negro" to "amoled", "cafe" to "coffee", "dorado" to "golden",
        "oceano" to "ocean", "azul" to "ocean", "bosque" to "forest", "verde" to "forest", "vino" to "wine", "violeta" to "violet", "pizarra" to "slate")
    private val accents = mapOf("dorado" to "gold", "amarillo" to "gold", "naranja" to "orange", "azul" to "blue", "turquesa" to "teal", "blanco" to "white",
        "cafe" to "coffee", "verde" to "green", "rojo" to "red", "morado" to "purple", "violeta" to "purple", "rosa" to "pink", "rosado" to "pink")
    private val modes = mapOf("ahorro" to "battery", "bateria" to "battery", "equilibrado" to "balanced", "normal" to "balanced", "cine" to "hifi",
        "alta fidelidad" to "hifi", "dormilon" to "sleepy", "dormir" to "sleepy")
    private val sorts = mapOf("titulo" to "title", "nombre" to "title", "artista" to "artist", "album" to "album", "duracion" to "duration",
        "tamano" to "size", "mas escuchadas" to "mostplayed", "mas escuchada" to "mostplayed", "letra" to "lyrics")
    private val browse = mapOf("artistas" to "artists", "albumes" to "albums", "carpetas" to "folders", "generos" to "genres")
    private fun keyOf(map: Map<String, String>) = map.keys.joinToString("|")

    val all = listOf(
        // Songs and the queue.
        Skill("player_play_song", "stone", "Poner canción", done = "Poniendo «{found}».", lookup = SEARCH,
            read = one("(?:pon|ponme|reproduce|toca|quiero escuchar) $SONG (.+)", "song")),
        Skill("player_queue_next", "stone", "A continuación", done = "«{found}» sonará a continuación.", lookup = SEARCH,
            read = one("(?:pon|agrega|anade|mete) (?:la cancion )?(.+?) (?:despues|a continuacion|luego|en la cola|a la cola|como siguiente)", "song")),
        Skill("player_queue", "stone", "Qué sigue", read = whole("que (?:sigue|viene despues|canciones siguen)", "que hay en la cola", "(?:mi )?cola de reproduccion")),
        Skill("player_queue_clear", "stone", "Vaciar cola", confirm = "¿Vacío la cola? La música se detiene.", done = "Cola vacía.",
            read = whole("(?:vacia|borra|limpia) la cola")),
        Skill("player_seek", "stone", "Ir a un minuto", done = "Listo.") { s, _ ->
            r("(?:ve|ir|salta|pasa|adelanta) (?:al|hasta el) minuto (\\d+)(?: y (\\d+) segundos?)?|(?:ve|salta) a los (\\d+) segundos").find(s.t)?.let { m ->
                val sec = (m.groupValues[1].toIntOrNull() ?: 0) * 60 + (m.groupValues[2].toIntOrNull() ?: 0) + (m.groupValues[3].toIntOrNull() ?: 0)
                Args().put("positionSeconds", sec)
            }
        },
        Skill("player_set_speed", "stone", "Velocidad", done = "Velocidad: {_how}.") { s, _ ->
            when {
                r("(?:velocidad|reproduce a velocidad|ponla a velocidad) normal|quita la velocidad").matches(s.t) -> Args().put("speed", 1.0).put("_how", "normal")
                r("(?:mas rapido|acelera|aumenta la velocidad|velocidad rapida)").matches(s.t) -> Args().put("speed", 1.25).put("_how", "más rápida")
                r("(?:mas lento|mas despacio|baja la velocidad|velocidad lenta)").matches(s.t) -> Args().put("speed", 0.75).put("_how", "más lenta")
                r("(?:al doble|a doble velocidad|velocidad doble)").matches(s.t) -> Args().put("speed", 2.0).put("_how", "doble")
                r("(?:a la mitad|a media velocidad)").matches(s.t) -> Args().put("speed", 0.5).put("_how", "la mitad")
                else -> null
            }
        },
        Skill("player_sleep_timer_extend", "stone", "Más minutos", done = "{minutes} minutos más de música.") { s, _ ->
            r("(?:dame|agrega|anade|suma|pon) (\\d+) minutos? mas(?: de musica| al temporizador)?").find(s.t)?.let { Args().put("minutes", it.groupValues[1].toInt()) }
        },
        Skill("player_sleep_timer_off", "stone", "Sin temporizador", "player_sleep_timer", done = "Temporizador de música cancelado.",
            read = fixed({ Args().put("minutes", 0) }, "(?:cancela|quita|apaga) el temporizador de (?:musica|sueno|stone)")),
        Skill("player_unfavorite", "stone", "Ya no me gusta", "player_set_favorite", done = "Quitada de tus favoritos.",
            read = fixed({ Args().put("favorite", false) }, "(?:quita|saca) esta cancion de (?:mis )?favoritos", "ya no me gusta esta cancion")),
        // The library.
        Skill("library_favorites", "stone", "Mis favoritas", read = whole("(?:mis|cuales son mis) (?:canciones )?favoritas")),
        Skill("library_browse", "stone", "Artistas, álbumes…") { s, _ ->
            r("(?:mis|que|cuales son mis) (${keyOf(browse)})(?: tengo)?").find(s.t)?.let { Args().put("kind", browse.getValue(it.groupValues[1])) }
        },
        Skill("library_rescan", "stone", "Buscar música nueva", done = "Buscando música nueva en el teléfono.",
            read = whole("busca (?:musica|canciones) nuevas?", "actualiza (?:la biblioteca|mi musica|la musica)")),
        Skill("library_sort", "stone", "Ordenar", done = "Canciones ordenadas por {_by}.") { s, _ ->
            r("ordena (?:mis canciones|la musica|las canciones|la lista) por (${keyOf(sorts)})").find(s.t)?.let { Args().put("by", sorts.getValue(it.groupValues[1])).put("_by", it.groupValues[1]) }
        },
        Skill("library_lyrics", "stone", "Letra", lookup = CURRENT, read = whole("(?:muestrame|dime|lee|cual es) la letra(?: de esta cancion)?", "letra de esta cancion")),
        Skill("metadata_fetch_lyrics", "stone", "Buscar letra", "metadata_fetch_lyrics", done = "Buscando la letra de «{found}».", lookup = CURRENT,
            read = whole("busca la letra(?: de esta cancion)?", "descarga la letra(?: de esta cancion)?")),
        // Lists.
        Skill("playlist_create", "stone", "Crear lista", done = "Lista «{name}» creada.",
            read = one("crea (?:una )?(?:nueva )?(?:lista|playlist)(?: llamada| que se llame| de nombre)? (.+)", "name")),
        Skill("playlist_add_songs", "stone", "Añadir a lista", "playlist_add_songs", done = "«{found}» añadida a la lista {found2}.",
            lookup = CURRENT.copy(arg = "songIds"), also = LIST,
            read = one("(?:agrega|anade|pon|guarda|mete) esta cancion (?:a|en) (?:la |mi )?(?:lista|playlist) (.+)", "list")),
        Skill("playlist_remove_songs", "stone", "Quitar de lista", confirm = "¿Quito «{found}» de la lista {found2}? La canción no se borra.",
            done = "Quitada de la lista {found2}.", lookup = CURRENT.copy(arg = "songIds"), also = LIST,
            read = one("(?:quita|saca|elimina) esta cancion de (?:la |mi )?(?:lista|playlist) (.+)", "list")),
        Skill("playlist_songs", "stone", "Canciones de la lista", lookup = LIST,
            read = one("que canciones (?:tiene|hay en) (?:la |mi )?(?:lista|playlist) (.+)", "list")),
        Skill("playlist_rename", "stone", "Renombrar lista", confirm = "¿Cambio el nombre de la lista {found} a «{name}»?", done = "Ahora se llama «{name}».",
            lookup = LIST) { s, _ ->
            r("(?:cambia el nombre de|renombra) (?:la |mi )?(?:lista|playlist) (.+?) (?:a|por|como) (.+)").find(s.t)?.let { m -> Args().put("list", s.group(m, 1)).put("name", s.group(m, 2)) }
        },
        Skill("playlist_delete", "stone", "Borrar lista", confirm = "¿Borro la lista {found}? Las canciones no se borran.", done = "Lista {found} borrada.",
            lookup = LIST, read = one("(?:borra|elimina) (?:la |mi )?(?:lista|playlist) (.+)", "list")),
        // Tags: always asked first; Stone keeps a backup and «deshaz el último cambio» reverts it.
        Skill("metadata_update", "stone", "Editar etiquetas", confirm = "¿Cambio el {_field} de «{found}» a «{_value}»?", done = "Listo: {_field} cambiado.",
            lookup = CURRENT) { s, _ ->
            r("(?:cambia|pon|corrige) el (${keyOf(fields)}) de esta cancion (?:a|por|como) (.+)").find(s.t)?.let { m ->
                Args().put(fields.getValue(m.groupValues[1]), s.group(m, 2)).put("_field", m.groupValues[1]).put("_value", s.group(m, 2))
            }
        },
        Skill("metadata_undo", "stone", "Deshacer edición", confirm = "¿Deshago el último cambio de etiquetas?", done = "Cambio deshecho.",
            read = whole("deshaz (?:el ultimo cambio|la ultima edicion|lo que cambiaste)(?: de (?:la cancion|las etiquetas))?")),
        // Sound, picture and look.
        Skill("equalizer_set_enabled", "stone", "Ecualizador sí/no", done = "Ecualizador {_how}.") { s, _ ->
            when {
                r("(?:activa|enciende|pon) el ecualizador").matches(s.t) -> Args().put("enabled", true).put("_how", "activado")
                r("(?:apaga|desactiva|quita) el ecualizador").matches(s.t) -> Args().put("enabled", false).put("_how", "apagado")
                else -> null
            }
        },
        Skill("settings_set_audio_mode", "stone", "Modo de audio", done = "Modo {_how}.") { s, _ ->
            r("(?:pon el |activa el |cambia al )?modo (${keyOf(modes)})").find(s.t)?.let { Args().put("mode", modes.getValue(it.groupValues[1])).put("_how", it.groupValues[1]) }
        },
        Skill("settings_set_transitions", "stone", "Fundido", done = "Fundido entre canciones {_how}.") { s, _ ->
            when {
                r("(?:activa|pon) (?:el )?(?:fundido|crossfade|transicion)(?: entre canciones)?").matches(s.t) -> Args().put("crossfade", true).put("_how", "activado")
                r("(?:quita|desactiva) (?:el )?(?:fundido|crossfade|transicion)(?: entre canciones)?").matches(s.t) -> Args().put("crossfade", false).put("_how", "desactivado")
                else -> null
            }
        },
        Skill("video_set_orientation", "video", "Girar pantalla", done = "Pantalla {_how}.") { s, _ ->
            r("(?:pon|gira) (?:la pantalla|el video)(?: en)? (horizontal|vertical|automatica|automatico)").find(s.t)?.let { m ->
                val o = when (m.groupValues[1]) { "horizontal" -> "landscape"; "vertical" -> "portrait"; else -> "auto" }
                Args().put("orientation", o).put("_how", m.groupValues[1])
            }
        },
        Skill("video_set_scale", "video", "Tamaño del video", done = "Video: {_how}.") { s, _ ->
            when {
                r("(?:pon el video en )?pantalla completa|llena la pantalla").matches(s.t) -> Args().put("scale", "fill").put("_how", "pantalla completa")
                r("(?:haz )?zoom al video|acerca el video").matches(s.t) -> Args().put("scale", "zoom").put("_how", "con zoom")
                r("(?:ajusta el video|video completo|que se vea todo el video)").matches(s.t) -> Args().put("scale", "fit").put("_how", "completo")
                else -> null
            }
        },
        Skill("video_set_brightness", "video", "Brillo", done = "Brillo {_how}.") { s, _ ->
            r("brillo (?:al |a )?(\\d{1,3})(?: por ciento)?").find(s.t)?.let { Args().put("percent", it.groupValues[1].toInt().coerceIn(2, 100)).put("_how", "al ${it.groupValues[1]} %") }
                ?: if (r("brillo automatico").matches(s.t)) Args().put("auto", true).put("_how", "automático") else null
        },
        // Stone Video: the same tools as Stone (it is a copy dedicated to video), asked to Stone Video. The keys differ
        // (video_…) so each has its own node on the map; [Skill.call] is the real tool.
        Skill("video_play", "video", "Poner video", call = "player_play_song", done = "Poniendo «{found}».",
            lookup = SEARCH.copy(said = "video", with = { mapOf("query" to (it["video"] ?: ""), "limit" to 10) }),
            read = one("(?:pon|ponme|reproduce|quiero ver|muestrame) (?:el |un )?(?:video|clip|mv) (?:de )?(.+)", "video")),
        Skill("video_search", "video", "Buscar videos", call = "library_search",
            read = { s, d -> one("busca (?:el |un )?(?:video|clip|mv) (?:de |del |sobre )?(.+)", "query")(s, d)
                ?: one("busca (.+) en stone video", "query")(s, d) }),
        Skill("video_state", "video", "¿Qué video es?", call = "player_state",
            read = whole("que video (?:es este|estoy viendo|esta sonando|suena|es)", "como se llama este video")),
        Skill("video_library_stats", "video", "Mis videos", call = "library_stats",
            read = whole("cuantos videos (?:tengo|hay) en stone video", "(?:mis videos|estadisticas) de stone video")),
        Skill("video_shuffle", "video", "Videos al azar", call = "player_set_shuffle", done = "Videos en modo aleatorio.",
            read = fixed({ Args().put("enabled", true) }, "(?:pon|activa) (?:los )?videos (?:en )?(?:aleatorio|al azar)", "mezcla los videos")),
        Skill("appearance_set_theme", "stone", "Tema de Stone", done = "Tema {_how} en Stone.") { s, _ ->
            r("(?:pon|cambia) (?:el )?(?:tema|fondo) (?:de stone )?(?:a |en )?(${keyOf(themes)})").find(s.t)?.let { Args().put("theme", themes.getValue(it.groupValues[1])).put("_how", it.groupValues[1]) }
        },
        Skill("appearance_set_accent", "stone", "Color de Stone", done = "Color {_how} en Stone.") { s, _ ->
            r("(?:pon|cambia) (?:el )?color(?: de acento| de stone)? (?:a |en )?(${keyOf(accents)})").find(s.t)?.let { Args().put("accent", accents.getValue(it.groupValues[1])).put("_how", it.groupValues[1]) }
        },
    )
}
