package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.AssistantState
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.Catalog
import com.zirakusa.gatewayhub.assistant.FamilyApp
import com.zirakusa.gatewayhub.assistant.Graph
import com.zirakusa.gatewayhub.assistant.Hands
import com.zirakusa.gatewayhub.assistant.MediaKey
import com.zirakusa.gatewayhub.assistant.Parser
import com.zirakusa.gatewayhub.assistant.People
import com.zirakusa.gatewayhub.assistant.Person
import com.zirakusa.gatewayhub.assistant.Say
import com.zirakusa.gatewayhub.assistant.Summary
import com.zirakusa.gatewayhub.assistant.Words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** Hands that only write down what they were asked, for the assistant's tests. */
class FakeHands(var installed: Set<String> = Catalog.apps.map { it.id }.toSet(), var contactsAllowed: Boolean = true) : Hands {
    val done = mutableListOf<String>()
    override fun installed(app: FamilyApp) = app.id in installed
    override fun open(app: FamilyApp) = done.add("open ${app.id}")
    override fun playMusic(query: String?) = done.add("music ${query ?: "*"}")
    override fun playRecommended() = done.add("recommended")
    override fun media(key: MediaKey) = done.add("media $key")
    override fun alarm(hour: Int, minute: Int) = done.add("alarm $hour:$minute")
    override fun timer(seconds: Int) = done.add("timer $seconds")
    override fun dial(number: String) = done.add("dial $number")
    override fun message(number: String, text: String?) = done.add("sms $number ${text ?: ""}".trim())
    override fun person(name: String): Person? {
        if (!contactsAllowed) throw Hands.NoContacts()
        return People.best(listOf(Person("Ana Pérez", "0991234567"), Person("Anabel Ruiz", "0987654321")), name)
    }
    override fun summary(app: FamilyApp) = when (app.id) {
        "clima" -> Summary("El clima", listOf("Quito · 14° Parcialmente nublado", "Máx 19° · Mín 9°"), "")
        "calendario" -> Summary("Hoy en tu calendario", emptyList(), "No hay eventos")
        else -> null
    }
    override fun now(): LocalDateTime = LocalDateTime.of(2026, 11, 21, 9, 5)
}

/** The assistant: Spanish words, times and durations, every phrase it knows, the brain with fake hands, and the graph. */
class AssistantTest {
    private fun p(text: String) = Parser.parse(text)

    @Test fun wordsTimesAndDurations() {
        assertEquals("pon 1 alarma a las 7 y media", Words.normalize("¡Oye, por favor pon una alarma a las siete y media!").replace("una", "1"))
        assertEquals("despiertame a las 6", Words.normalize("Despiértame a las seis"))
        assertEquals("llamale a ñusta", Words.normalize("Llámale a Ñusta"))
        assertEquals(7 to 0, Words.clock("7")); assertEquals(7 to 30, Words.clock("7 y media")); assertEquals(7 to 15, Words.clock("7 y cuarto"))
        assertEquals(7 to 45, Words.clock("8 menos cuarto")); assertEquals(18 to 0, Words.clock("6 de la tarde")); assertEquals(22 to 10, Words.clock("10:10 pm"))
        assertEquals(0 to 0, Words.clock("12 de la noche")); assertEquals(6 to 5, Words.clock("6 y 5 de la manana")); assertNull(Words.clock("nunca"))
        assertEquals(600, Words.duration("10 minutos")); assertEquals(5400, Words.duration("1 hora y media")); assertEquals(1800, Words.duration("media hora"))
        assertEquals(90, Words.duration("90 segundos")); assertEquals(3900, Words.duration("1 hora 5 minutos")); assertNull(Words.duration("un rato"))
        assertEquals(300, Words.duration(Words.normalize("cinco minutos"))); assertEquals(60, Words.duration(Words.normalize("un minuto")))
    }

    @Test fun phrasesItKnows() {
        assertEquals(Action.PLAY_MUSIC, p("Reproduce música").action); assertNull(p("reproduce música")["query"])
        assertEquals("bad bunny", p("pon música de Bad Bunny")["query"]); assertEquals("soda stereo", p("Pon Soda Stereo")["query"])
        assertEquals(Action.PLAY_RECOMMENDED, p("Pon mis recomendaciones del día").action); assertEquals(Action.PLAY_RECOMMENDED, p("ponme el mix del día").action)
        assertEquals("PAUSE", p("pausa")["key"]); assertEquals("NEXT", p("Siguiente canción")["key"]); assertEquals("RESUME", p("continúa")["key"])
        p("Pon una alarma a las siete y media").let { assertEquals(Action.ALARM, it.action); assertEquals(7, it.int("hour")); assertEquals(30, it.int("minute")) }
        p("despiértame a las 6 de la mañana").let { assertEquals(6, it.int("hour")); assertEquals(0, it.int("minute")) }
        assertEquals(Action.OPEN, p("pon una alarma").action)                                    // no time: open Reloj
        p("Pon un temporizador de cinco minutos").let { assertEquals(Action.TIMER, it.action); assertEquals(300, it.int("seconds")) }
        assertEquals(1800, p("avísame en media hora").int("seconds"))
        p("Llama a Ana").let { assertEquals(Action.CALL, it.action); assertEquals("ana", it["who"]) }
        assertEquals("099 123 4567", p("llama al 099 123 4567")["who"])
        p("Mándale un mensaje a Ana que llego tarde").let { assertEquals(Action.MESSAGE, it.action); assertEquals("ana", it["who"]); assertEquals("llego tarde", it["text"]) }
        assertEquals(Action.WEATHER, p("¿Qué tiempo hace?").action); assertEquals(Action.WEATHER, p("va a llover hoy").action)
        assertEquals(Action.AGENDA, p("qué tengo hoy").action); assertEquals(Action.TODAY, p("Buenos días").action)
        assertEquals(Action.TIME, p("¿Qué hora es?").action); assertEquals(Action.DATE, p("qué día es hoy").action)
        assertEquals(Action.PHOTO, p("toma una foto").action); assertEquals(Action.SCAN, p("escanea un QR").action); assertEquals(Action.SPEED, p("haz una prueba de velocidad").action)
        assertEquals("camara", p("Abre la cámara")["app"]); assertEquals("video", p("abre stone video")["app"]); assertEquals("filem", p("muéstrame mis archivos")["app"])
        assertEquals("calculadora", p("calculadora")["app"])
        assertEquals(Action.UNKNOWN, p("abre spotify").action); assertEquals("spotify", p("abre spotify")["app_name"])
        assertEquals(Action.UNKNOWN, p("cuéntame un chiste").action); assertEquals(Action.UNKNOWN, p("   ").action)
    }

    @Test fun theBrainActs() {
        val hands = FakeHands()
        var t = 1000L
        val brain = Brain(hands) { t.also { t += 7 } }
        assertEquals(Say.PLAYING, brain.handle("reproduce música").reply.say)
        assertEquals(Say.RECOMMENDED, brain.handle("recomendaciones del día", byVoice = true).reply.say)
        brain.handle("alarma a las 6 y media de la tarde"); brain.handle("temporizador de 10 minutos"); brain.handle("pausa")
        val call = brain.handle("llama a Ana")
        assertEquals(Say.CALLING, call.reply.say); assertEquals("Ana Pérez", call.reply.args[0])
        brain.handle("mándale un mensaje a Anabel que ya voy")
        brain.handle("llama al 611")
        assertEquals(listOf("music *", "recommended", "alarm 18:30", "timer 600", "media PAUSE", "dial 0991234567", "sms 0987654321 ya voy", "dial 611"), hands.done)
        val weather = brain.handle("qué tiempo hace")
        assertEquals(Say.SUMMARY, weather.reply.say); assertEquals(2, weather.reply.lines.size); assertTrue(weather.ok)
        assertEquals(Say.NOTHING_TODAY, brain.handle("qué tengo hoy").reply.say)
        assertEquals(listOf("9", "5"), brain.handle("qué hora es").reply.args)
        assertEquals(7L, brain.handle("abre notas").millis)
        assertEquals(Say.NO_CONTACT, brain.handle("llama a Pedro").reply.say)                        // never a guess between people
        assertEquals(Person("Ana Pérez", "1"), People.best(listOf(Person("Ana Pérez", "1"), Person("Ana María", "2")), "ana pérez"))
        assertEquals(Person("Mamá ❤", "3"), People.best(listOf(Person("Mamá ❤", "3")), "mi mama"))
        hands.contactsAllowed = false
        assertEquals(Say.NEED_CONTACTS, brain.handle("llama a Ana").reply.say)
        hands.installed = emptySet()
        val missing = brain.handle("reproduce música")
        assertEquals(Say.NOT_INSTALLED, missing.reply.say); assertFalse(missing.ok)
        assertEquals(Say.NOT_UNDERSTOOD, brain.handle("cuéntame un chiste").reply.say)
    }

    @Test fun theGraphLightsThePath() {
        val brain = Brain(FakeHands())
        val turn = brain.handle("reproduce música", byVoice = true)
        assertEquals(listOf(Graph.VOICE, "do:PLAY_MUSIC", "app:stone", Graph.OK), turn.path)
        assertEquals(listOf(Graph.TEXT, Graph.FAIL), brain.handle("cuéntame un chiste").path)
        val nodes = Graph.nodes { it.name }
        val ids = nodes.map { it.id }.toSet()
        assertTrue(Graph.edges().all { it.from in ids && it.to in ids })                         // every edge joins two drawn nodes
        assertTrue(turn.path.all { it in ids })
        for (a in Action.entries.filter { it != Action.UNKNOWN && it != Action.OPEN }) {          // each action reaches its app or a result
            assertTrue(a.name, Graph.edges().any { it.from == Graph.action(a) })
        }
        assertTrue(nodes.all { it.x in 0f..1f && it.y in 0f..1f })
        val state = AssistantState(brain)
        listOf("reproduce música", "pausa", "abre notas", "nada de nada").forEach { state.send(it, byVoice = false) }
        val h = state.health
        assertEquals(4, h.total); assertEquals(3, h.understood); assertEquals(3, h.done)
        assertEquals(2, state.heat["app:stone"]); assertEquals(1, state.heat["app:notas"]); assertEquals(4, state.pulse)
        state.heard("pon música", final = false); assertEquals("pon música", state.partial); assertEquals(4, state.turns.size)
        state.heard("pon música", final = true); assertEquals("", state.partial); assertEquals(5, state.turns.size); assertTrue(state.last!!.byVoice)
    }
}
