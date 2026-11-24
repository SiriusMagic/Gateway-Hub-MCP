package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.FamilyApp
import com.zirakusa.gatewayhub.assistant.Graph
import com.zirakusa.gatewayhub.assistant.Parser
import com.zirakusa.gatewayhub.assistant.Say
import com.zirakusa.gatewayhub.assistant.Skills
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

/** Every family app's tools as things to say, the lookups (which list, which note…) and the «sí / no» before changes. */
@RunWith(RobolectricTestRunner::class)
class AllToolsTest {
    private val today = LocalDate.of(2026, 11, 24)                     // a Tuesday
    private fun p(text: String) = Parser.parse(text, today)
    private fun tool(text: String) = p(text).let { assertEquals(text, Action.TOOL, it.action); it["tool"] }
    private fun args(text: String) = JSONObject().apply { p(text).slots.forEach { (k, v) ->
        when { k.startsWith("arg:") -> put(k.removePrefix("arg:"), v); k.startsWith("num:") -> put(k.removePrefix("num:"), v.toInt())
            k.startsWith("bool:") -> put(k.removePrefix("bool:"), v.toBoolean()); k.startsWith("dbl:") -> put(k.removePrefix("dbl:"), v.toDouble()) } } }

    @Test fun stonePhrases() {
        mapOf(
            "pon la canción despacito" to "player_play_song", "pon bohemian rhapsody a continuación" to "player_queue_next", "qué sigue" to "player_queue",
            "vacía la cola" to "player_queue_clear", "ve al minuto 2" to "player_seek", "más rápido" to "player_set_speed", "dame 15 minutos más" to "player_sleep_timer_extend",
            "cancela el temporizador de música" to "player_sleep_timer_off", "quita esta canción de favoritos" to "player_unfavorite", "mis canciones favoritas" to "library_favorites",
            "mis artistas" to "library_browse", "busca música nueva" to "library_rescan", "ordena mis canciones por artista" to "library_sort",
            "muéstrame la letra" to "library_lyrics", "busca la letra de esta canción" to "metadata_fetch_lyrics", "crea una lista llamada gym" to "playlist_create",
            "agrega esta canción a la lista gym" to "playlist_add_songs", "quita esta canción de la lista gym" to "playlist_remove_songs",
            "qué canciones tiene la lista gym" to "playlist_songs", "renombra la lista gym a correr" to "playlist_rename", "borra la lista gym" to "playlist_delete",
            "cambia el artista de esta canción a Soda Stereo" to "metadata_update", "deshaz el último cambio" to "metadata_undo", "activa el ecualizador" to "equalizer_set_enabled",
            "modo cine" to "settings_set_audio_mode", "activa el fundido entre canciones" to "settings_set_transitions", "pon la pantalla horizontal" to "video_set_orientation",
            "pantalla completa" to "video_set_scale", "brillo al 40 por ciento" to "video_set_brightness", "cambia el tema a negro" to "appearance_set_theme",
            "cambia el color a rosa" to "appearance_set_accent",
        ).forEach { (said, want) -> assertEquals(said, want, tool(said)) }
        assertEquals(120, args("ve al minuto 2").getInt("positionSeconds")); assertEquals(1.25, args("más rápido").getDouble("speed"), 0.0)
        assertEquals("artist", args("ordena mis canciones por artista").getString("by")); assertEquals("hifi", args("modo cine").getString("mode"))
        assertEquals("Soda Stereo".lowercase(), args("cambia el artista de esta canción a Soda Stereo").getString("artist").lowercase())
        assertEquals("amoled", args("cambia el tema a negro").getString("theme")); assertEquals("landscape", args("pon la pantalla horizontal").getString("orientation"))
    }

    @Test fun familyPhrases() {
        mapOf(
            "borra la alarma de las 7" to "clock_alarm_delete", "lee la nota receta" to "notes_get", "agrega a la nota compras leche" to "notes_update",
            "fija la nota compras" to "notes_pin", "borra la nota compras" to "notes_delete", "mis carpetas de notas" to "notes_folders",
            "crea una carpeta trabajo" to "folders_create", "borra la tarea pagar luz" to "tasks_delete",
            "pospón la reunión con Ana para el viernes a las 4" to "calendar_event_move", "mueve el dentista al 30" to "calendar_event_move",
            "cancela la cita del dentista" to "calendar_event_delete", "qué temperatura hace ahora" to "weather_now", "mis lugares del clima" to "weather_places",
            "quita Quito del clima" to "weather_place_remove", "lee los mensajes de Ana" to "sms_messages", "mis últimas llamadas" to "phone_calls",
            "convierte 5 kilómetros a millas" to "calc_convert_units", "cuántos euros son 20 dólares".replace("cuántos euros son 20 dólares", "convierte 20 dolares a euros") to "calc_convert_currency",
            "historial de la calculadora" to "calc_history", "borra el historial de la calculadora" to "calc_history_clear", "mis álbumes de fotos" to "gallery_albums",
            "mis últimas fotos" to "gallery_recent", "fotos de ayer" to "gallery_by_date", "borra el archivo factura" to "files_trash",
            "renombra el archivo factura a recibo" to "files_rename", "mide la velocidad de internet" to "network_speed_test", "mis pruebas de velocidad" to "network_speed_history",
            "qué redes wifi hay cerca" to "network_wifi_nearby", "dónde estoy" to "compass_location", "mis mediciones" to "level_measurements",
            "qué apps tengo" to "launcher_apps", "qué hay en mi dock" to "launcher_dock", "pon el flash de la cámara en automático" to "camera_flash",
            "enciende la linterna de la cámara" to "camera_flash", "temporizador de la cámara a 10 segundos" to "camera_timer", "toma una foto" to "camera_capture",
            "tómame una selfie" to "camera_capture",
        ).forEach { (said, want) -> assertEquals(said, want, tool(said)) }
        val move = args("pospón la reunión con Ana para el viernes a las 4")
        assertEquals("reunion con ana", move.getString("event")); assertEquals("2026-11-27", move.getString("date")); assertEquals("04:00", move.getString("time"))
        assertEquals("km", args("convierte 5 kilómetros a millas").getString("from")); assertEquals("length", args("convierte 5 kilómetros a millas").getString("category"))
        assertEquals("2026-11-23", args("fotos de ayer").getString("from"))
        assertEquals("torch", args("enciende la linterna de la cámara").getString("flash")); assertEquals("auto", args("pon el flash de la cámara en automático").getString("flash"))
        assertTrue(args("tómame una selfie").getBoolean("selfie"))
        // Lucía's own orders stay: the phone's torch, a photo app opened by name.
        assertEquals(Action.TORCH, p("enciende la linterna").action); assertEquals(Action.OPEN, p("abre la cámara").action)
    }

    /** Apps answering on the phone, as their link channels do; [asked] keeps every call. */
    private class Apps : FakeHands() {
        val asked = mutableListOf<String>(); var captured = mutableListOf<Boolean>()
        override fun now(): LocalDateTime = LocalDateTime.of(2026, 11, 24, 9, 0)
        override fun capture(selfie: Boolean) = captured.add(selfie)
        override fun tool(app: FamilyApp, name: String, args: JSONObject): JSONObject? {
            asked += "${app.id} $name $args"
            return when (name) {
                "playlist_list" -> JSONObject().put("playlists", JSONArray().put(JSONObject().put("id", "p1").put("name", "Gym")))
                "player_state" -> JSONObject().put("playing", true).put("title", "De música ligera").put("songId", "s7")
                "library_search" -> JSONObject().put("results", JSONArray().put(JSONObject().put("id", "s9").put("title", "Despacito")))
                "calendar_events" -> JSONObject().put("events", JSONArray().put(JSONObject().put("id", 3).put("title", "Reunión con Ana")))
                "notes_search" -> JSONObject().put("notes", JSONArray().put(JSONObject().put("id", 5).put("title", "Compras")))
                else -> JSONObject().put("ok", true)
            }
        }
    }

    @Test fun lookupsConfirmationsAndThePhoto() {
        val apps = Apps(); val brain = Brain(apps)
        // A song by its name: Stone's own search, its best result.
        assertEquals("Poniendo «Despacito».", brain.handle("pon la canción despacito").reply.args[0])
        assertEquals("stone player_play_song {\"songId\":\"s9\"}", apps.asked.last())
        // Two lookups: the song playing and the list.
        assertEquals("«De música ligera» añadida a la lista Gym.", brain.handle("agrega esta canción a la lista gym").reply.args[0])
        assertEquals("stone playlist_add_songs {\"songIds\":\"s7\",\"playlistId\":\"p1\"}", apps.asked.last())
        // Deleting asks first; «no» does nothing, «sí» does it.
        val ask = brain.handle("borra la lista gym")
        assertEquals(Say.CONFIRM, ask.reply.say); assertEquals("¿Borro la lista Gym? Las canciones no se borran.", ask.reply.args[0])
        val calls = apps.asked.size
        assertEquals(Say.CANCELLED, brain.handle("no").reply.say); assertEquals(calls, apps.asked.size)
        brain.handle("borra la lista gym"); val yes = brain.handle("sí")
        assertEquals("Lista Gym borrada.", yes.reply.args[0]); assertEquals("stone playlist_delete {\"playlistId\":\"p1\"}", apps.asked.last())
        assertEquals(listOf(Graph.TEXT, "app:stone", "tool:playlist_delete"), yes.path)
        // Postponing: the event by its words, then the question, then calendar_event_move without Lucía's own words.
        assertEquals("¿Muevo «Reunión con Ana» al viernes a las 4:00?", brain.handle("pospón la reunión con Ana para el viernes a las 4").reply.args[0])
        brain.handle("sí, hazlo".replace(", hazlo", ""))
        assertEquals("calendario calendar_event_move {\"date\":\"2026-11-27\",\"time\":\"04:00\",\"id\":3}", apps.asked.last())
        // Editing tags: asked, with the field and the value said.
        assertEquals("¿cambio el artista de «de música ligera» a «soda stereo»?", brain.handle("cambia el artista de esta canción a soda stereo").reply.args[0].lowercase())
        brain.handle("sí")
        assertTrue(apps.asked.last().startsWith("stone metadata_update {\"artist\":\"soda stereo\",\"songId\":\"s7\""))
        // The photo: asked, then Cámara opens with its countdown (never an MCP tool).
        assertEquals(Say.CONFIRM, brain.handle("toma una foto").reply.say); assertTrue(apps.captured.isEmpty())
        assertEquals("Abriendo la cámara: foto en 3 segundos.", brain.handle("sí").reply.args[0]); assertEquals(listOf(false), apps.captured)
        // Another order instead of an answer: the question is dropped and the new order runs.
        brain.handle("borra la lista gym"); assertEquals(Action.TIME, brain.handle("qué hora es").command.action)
        assertEquals(Action.UNKNOWN, brain.handle("sí").command.action)
    }

    @Test fun everySkillHasOneNodeUnderItsApp() {
        val ids = Graph.nodes { it.name }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        Skills.every.forEach { s -> assertTrue(s.tool, Graph.edges().any { it.from == "app:${s.app}" && it.to == "tool:${s.tool}" }) }
        assertTrue(Skills.every.size >= 90)
        assertFalse(Skills.every.any { it.call.contains("delete") && it.confirm == null })        // every delete asks first
    }
}
