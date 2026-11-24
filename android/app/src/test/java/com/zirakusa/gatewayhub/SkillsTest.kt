package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.Graph
import com.zirakusa.gatewayhub.assistant.Hands
import com.zirakusa.gatewayhub.assistant.Parser
import com.zirakusa.gatewayhub.assistant.Say
import com.zirakusa.gatewayhub.assistant.Skills
import com.zirakusa.gatewayhub.assistant.Speech
import com.zirakusa.gatewayhub.assistant.FamilyApp
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

/** The family apps' MCP tools as things to say: which phrase asks which tool, what Lucía says back, and the whole way. */
@RunWith(RobolectricTestRunner::class)
class SkillsTest {
    private val today = LocalDate.of(2026, 11, 24)                     // a Tuesday
    private fun p(text: String) = Parser.parse(text, today)
    private fun tool(text: String) = p(text).let { assertEquals(text, Action.TOOL, it.action); it["tool"] }
    private fun args(text: String) = JSONObject().apply { p(text).slots.forEach { (k, v) ->
        when { k.startsWith("arg:") -> put(k.removePrefix("arg:"), v); k.startsWith("num:") -> put(k.removePrefix("num:"), v.toInt()) } } }

    @Test fun phrasesAskTheRightTool() {
        mapOf(
            "¿Cuál es mi próxima alarma?" to "clock_next_alarm", "a qué hora suena la alarma" to "clock_next_alarm", "qué alarmas tengo" to "clock_alarms",
            "qué hora es en Madrid" to "clock_world_time", "qué tareas tengo" to "tasks_list", "mis tareas pendientes" to "tasks_list",
            "agrega tarea comprar piñas" to "tasks_create", "marca la tarea comprar pan como hecha" to "tasks_complete",
            "busca en mis notas receta" to "notes_search", "qué notas tengo" to "notes_list", "qué tengo mañana" to "calendar_day",
            "qué tengo el viernes" to "calendar_day", "crea un evento dentista mañana a las 3 de la tarde" to "calendar_event_create",
            "próximos feriados" to "calendar_holidays", "va a llover mañana" to "weather_forecast", "tengo llamadas perdidas" to "phone_missed",
            "quién me llamó" to "phone_missed", "cuál es el número de Ana" to "contacts_search", "quién cumple años esta semana" to "contacts_birthdays",
            "tengo mensajes sin leer" to "sms_unread", "cuánto es 25 por 4" to "calc_evaluate", "cuántas fotos tengo" to "gallery_stats",
            "cuánto espacio me queda" to "files_storage", "archivos recientes" to "files_recent", "últimos qr" to "qr_history",
            "cómo está mi conexión" to "network_status", "hacia dónde estoy mirando" to "compass_heading", "está nivelado" to "level_reading",
        ).forEach { (said, want) -> assertEquals(said, want, tool(said)) }
    }

    @Test fun argumentsComeFromWhatWasSaid() {
        assertEquals("madrid", args("qué hora es en Madrid").getString("city"))
        assertEquals("comprar piñas", args("agrega tarea comprar piñas").getString("text"))            // the ñ stays
        assertEquals("2026-11-25", args("qué tengo mañana").getString("date"))
        assertEquals("2026-11-27", args("qué tengo el viernes").getString("date"))
        assertEquals("2026-12-03", args("qué tengo el 3").getString("date"))                          // the 3rd already passed this month
        val event = args("crea un evento dentista mañana a las 3 de la tarde")
        assertEquals("Dentista", event.getString("title")); assertEquals("2026-11-25", event.getString("date")); assertEquals("15:00", event.getString("time"))
        assertEquals(2026, args("próximos feriados").getInt("year"))
        assertEquals("25*4", args("cuánto es 25 por 4").getString("expression"))
        assertEquals("100+10%", Skills.expression("100 mas 10 por ciento")); assertEquals("√81", Skills.expression("raiz de 81"))
        assertEquals("ana", args("cuál es el número de Ana").getString("query"))
        // Today stays the summaries Lucía already had; the old orders are untouched.
        assertEquals(Action.AGENDA, p("qué tengo hoy").action); assertEquals(Action.TIME, p("qué hora es").action)
        assertEquals(Action.MEDIA, p("pausa").action)
    }

    @Test fun luciaSaysItShort() {
        val alarm = JSONObject().put("alarm", JSONObject().put("time", "06:30").put("label", "Trabajo")).put("in_text", "7 h 5 min")
        assertEquals("Tu próxima alarma es a las 06:30 (Trabajo), en 7 h 5 min.", Speech.say("clock_next_alarm", alarm, today))
        assertEquals("No tienes ninguna alarma activa.", Speech.say("clock_next_alarm", JSONObject().put("alarm", JSONObject.NULL), today))
        val tasks = JSONObject().put("tasks", JSONArray().put(JSONObject().put("text", "Comprar pan").put("done", false))
            .put(JSONObject().put("text", "Pagar luz").put("done", true)))
        assertEquals("Tienes 1 tarea pendiente: Comprar pan.", Speech.say("tasks_list", tasks, today))
        assertEquals("En Madrid son las 14:05.", Speech.say("clock_world_time", JSONObject().put("city", "Madrid").put("time", "14:05"), today))
        val missed = JSONObject().put("total", 2).put("calls", JSONArray().put(JSONObject().put("name", "Ana Pérez").put("at", "2026-11-24T08:15:00-05:00"))
            .put(JSONObject().put("number", "0991234567")))
        assertEquals("Hay 2 llamadas: Ana Pérez, hoy a las 08:15; 0991234567.", Speech.say("phone_missed", missed, today))
        val birthdays = JSONObject().put("birthdays", JSONArray().put(JSONObject().put("name", "Ana").put("days", 1).put("turns", 35)))
        assertEquals("Hay 1 cumpleaños: Ana, mañana, cumple 35.", Speech.say("contacts_birthdays", birthdays, today))
        assertEquals("No hay mensajes.", Speech.say("sms_unread", JSONObject().put("total", 0).put("messages", JSONArray()), today))
        assertEquals("Te quedan 12,5 GB libres de 64,0 GB.", Speech.say("files_storage", JSONObject().put("freeBytes", 12_500_000_000).put("totalBytes", 64_000_000_000), today))
        assertEquals("Son 100.", Speech.say("calc_evaluate", JSONObject().put("result", "100"), today))
        assertEquals("Hay 1 evento: Dentista, mañana a las 15:00.",
            Speech.say("calendar_day", JSONObject().put("events", JSONArray().put(JSONObject().put("title", "Dentista").put("start", "2026-11-25T15:00"))), today))
    }

    /** A family app answering on the phone, as its link channel does. */
    private class Apps : FakeHands() {
        val asked = mutableListOf<String>(); var redirect = true
        override fun now(): LocalDateTime = LocalDateTime.of(2026, 11, 24, 9, 0)
        override fun tool(app: FamilyApp, name: String, args: JSONObject): JSONObject? {
            if (!redirect) throw Hands.ToolOff()
            asked += "${app.id} $name $args"
            return when (name) {
                "tasks_list" -> JSONObject().put("tasks", JSONArray().put(JSONObject().put("id", 7).put("text", "Comprar pan").put("done", false)))
                "tasks_complete" -> JSONObject().put("id", 7).put("text", "Comprar pan").put("done", true)
                "contacts_search" -> throw Hands.ToolError("NO_PERMISSION", "Permite los contactos en Teléfono.")
                else -> JSONObject().put("alarm", JSONObject().put("time", "06:30")).put("in_text", "21 h 30 min")
            }
        }
    }

    @Test fun theWholeWayThroughTheApp() {
        val apps = Apps(); val brain = Brain(apps)
        val next = brain.handle("¿cuál es mi próxima alarma?", byVoice = true)
        assertEquals("Tu próxima alarma es a las 06:30, en 21 h 30 min.", next.reply.args[0]); assertTrue(next.ok)
        assertEquals(listOf(Graph.VOICE, "app:reloj", "tool:clock_next_alarm"), next.path)
        // «marca la tarea … como hecha» finds the task by its words first.
        val done = brain.handle("marca la tarea comprar pan como hecha")
        assertEquals("Hecho: Comprar pan.", done.reply.args[0]); assertEquals("notas tasks_complete {\"id\":7}", apps.asked.last())
        assertEquals(Say.TOOL_ERROR, brain.handle("marca la tarea lavar el auto como hecha").reply.say)
        assertEquals(listOf("Teléfono", "Permite los contactos en Teléfono."), brain.handle("el número de Ana").reply.args)
        apps.redirect = false
        val off = brain.handle("qué alarmas tengo"); assertEquals(Say.TOOL_OFF, off.reply.say); assertFalse(off.ok)
        apps.installed = emptySet()
        assertEquals(Say.NOT_INSTALLED, brain.handle("qué alarmas tengo").reply.say)
    }

    /** The offline recognizer knows the words, and the free one keeps what to write (a task, a city). */
    @Test fun voiceHearsTheNewWords() {
        listOf("contesta", "cuelga", "linterna", "tarea", "próxima", "cumpleaños", "feriados", "calcula", "espacio").forEach {
            assertTrue(it, it in com.zirakusa.gatewayhub.assistant.Vocabulary.base)
        }
        assertEquals("agrega tarea comprar piñas", com.zirakusa.gatewayhub.assistant.Hearing.pick("agrega tarea comprar [unk]", "agrega tarea comprar piñas"))
    }

    @Test fun everyToolHangsFromItsAppInTheMap() {
        val ids = Graph.nodes { it.name }.map { it.id }.toSet()
        Skills.all.forEach { s ->
            assertTrue(s.tool, "tool:${s.tool}" in ids)
            assertTrue(s.tool, Graph.edges().any { it.from == "app:${s.app}" && it.to == "tool:${s.tool}" })
        }
        assertEquals(Skills.all.size, Skills.all.map { it.tool }.toSet().size)                    // one node per tool
    }
}
