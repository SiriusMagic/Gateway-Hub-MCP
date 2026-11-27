package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.AssistantState
import com.zirakusa.gatewayhub.assistant.Brain
import com.zirakusa.gatewayhub.assistant.Catalog
import com.zirakusa.gatewayhub.assistant.FamilyApp
import com.zirakusa.gatewayhub.assistant.Edge
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
open class FakeHands(var installed: Set<String> = Catalog.apps.map { it.id }.toSet(), var contactsAllowed: Boolean = true) : Hands {
    val done = mutableListOf<String>()
    override fun installed(app: FamilyApp) = app.id in installed
    override fun open(app: FamilyApp) = done.add("open ${app.id}")
    var phoneAllowed = true; var ringing = true
    override fun torch(on: Boolean) = done.add("torch $on")
    override fun answerCall(): Boolean { if (!phoneAllowed) throw Hands.NoPhone(); done.add("answer"); return ringing }
    override fun endCall(): Boolean { if (!phoneAllowed) throw Hands.NoPhone(); done.add("hang up"); return ringing }
    override fun playMusic(query: String?) = done.add("music ${query ?: "*"}")
    override fun playRecommended() = done.add("recommended")
    override fun media(key: MediaKey) = done.add("media $key")
    override fun alarm(hour: Int, minute: Int) = done.add("alarm $hour:$minute")
    override fun timer(seconds: Int) = done.add("timer $seconds")
    var outgoingAllowed = true
    override fun placeCall(number: String, speaker: Boolean): Boolean {
        if (!outgoingAllowed) throw Hands.NoCallPermission()
        return done.add("call $number" + if (speaker) " speaker" else "")
    }
    override fun searchWeb(query: String) = done.add("web $query")
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
    override open fun now(): LocalDateTime = LocalDateTime.of(2026, 11, 21, 9, 5)
    var level = 70
    override fun volume() = level
    override fun setVolume(percent: Int): Boolean { level = percent; return done.add("volume $percent") }
    val notes = mutableListOf<Triple<String, String, String>>()
    override fun saveNote(title: String, body: String, folder: String): Long? =
        if ("notas" in installed) { notes += Triple(title, body, folder); notes.size.toLong() } else null
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
        assertEquals("Bad Bunny", p("pon música de Bad Bunny")["query"]); assertEquals("soda stereo", p("Pon Soda Stereo")["query"])
        assertEquals(Action.PLAY_RECOMMENDED, p("Pon mis recomendaciones del día").action); assertEquals(Action.PLAY_RECOMMENDED, p("ponme el mix del día").action)
        listOf("pon pausa", "pon pausa a la música", "ponle pausa", "pausa la canción", "para la música", "detén la canción", "quita la música", "apaga la música")
            .forEach { assertEquals(it, "PAUSE", p(it)["key"]) }
        listOf("reinicia la música", "reinicia la canción", "repite la canción", "vuelve a empezar", "otra vez", "desde el principio")
            .forEach { assertEquals(it, "RESTART", p(it)["key"]) }
        listOf("otra canción", "cambia la canción", "pasa la canción", "siguiente tema").forEach { assertEquals(it, "NEXT", p(it)["key"]) }
        listOf("quita la pausa", "dale play", "vuelve a poner la música").forEach { assertEquals(it, "RESUME", p(it)["key"]) }
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
        // A photo and a speed test now go to the apps' own tools (asked first); scanning still opens QR.
        assertEquals("camera_capture", p("toma una foto")["tool"]); assertEquals(Action.SCAN, p("escanea un QR").action)
        assertEquals("network_speed_test", p("haz una prueba de velocidad")["tool"])
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
        assertEquals(Say.CONFIRM_CALL, call.reply.say); assertEquals("Ana Pérez", call.reply.args[0])
        assertEquals(Say.CALL_STARTED, brain.handle("sí").reply.say)
        brain.handle("mándale un mensaje a Anabel que ya voy")
        brain.handle("marca 611")
        assertEquals(listOf("music *", "recommended", "alarm 18:30", "timer 600", "media PAUSE", "call 0991234567", "sms 0987654321 ya voy", "dial 611"), hands.done)
        val weather = brain.handle("qué tiempo hace")
        assertEquals(Say.SUMMARY, weather.reply.say); assertEquals(2, weather.reply.lines.size); assertTrue(weather.ok)
        assertEquals(Say.NOTHING_TODAY, brain.handle("qué tengo hoy").reply.say)
        assertEquals(listOf("9", "5"), brain.handle("qué hora es").reply.args)
        assertEquals(7L, brain.handle("abre notas").millis)
        assertEquals(Say.NO_CONTACT, brain.handle("llama a Pedro").reply.say)                        // never a guess between people
        assertEquals(Person("Ana Pérez", "1"), People.best(listOf(Person("Ana Pérez", "1"), Person("Ana María", "2")), "ana pérez"))
        assertEquals(Person("Mamá ❤", "3"), People.best(listOf(Person("Mamá ❤", "3")), "mi mama"))
        // Names as heard or typed: any order, sounds alike, and one person with two numbers is still one person.
        val book = listOf(Person("Disled Jefe", "4"), Person("Disled Jefe", "5"), Person("Box Ramírez", "6"), Person("Yesica Mora", "7"), Person("Pedro Ruiz", "8"))
        assertEquals("4", People.best(book, "disled jefe")?.number); assertEquals("4", People.best(book, "jefe disled")?.number)
        assertEquals("6", People.best(book, "vox")?.number); assertEquals("7", People.best(book, "jessica")?.number)
        assertEquals("4", People.best(book, "dizled")?.number); assertEquals(null, People.best(book, "carlos"))
        assertEquals(null, People.best(listOf(Person("Ana", "1"), Person("Ane", "2")), "ani"))     // two as close: no guess
        assertTrue("bo" in People.guesses("vox")); assertTrue("je" in People.guesses("gessica"))
        hands.contactsAllowed = false
        assertEquals(Say.NEED_CONTACTS, brain.handle("llama a Ana").reply.say)
        hands.installed = emptySet()
        val missing = brain.handle("reproduce música")
        assertEquals(Say.NOT_INSTALLED, missing.reply.say); assertFalse(missing.ok)
        assertEquals(Say.NOT_UNDERSTOOD, brain.handle("cuéntame un chiste").reply.say)
    }

    @Test fun answersAndEndsCallsByVoice() {
        listOf("contesta", "contesta la llamada", "atiende", "responde la llamada", "acepta la llamada").forEach { assertEquals(it, Action.ANSWER, p(it).action) }
        listOf("cuelga", "rechaza la llamada", "corta la llamada", "termina la llamada", "no contestes").forEach { assertEquals(it, Action.HANG_UP, p(it).action) }
        val hands = FakeHands(); val brain = Brain(hands)
        assertEquals(Say.ANSWERED, brain.handle("contesta").reply.say)
        assertEquals(listOf(Graph.TEXT, "app:telefono", "do:ANSWER"), brain.handle("contesta").path)
        hands.ringing = false; assertEquals(Say.NO_CALL, brain.handle("cuelga").reply.say)
        hands.phoneAllowed = false; val t = brain.handle("cuelga"); assertEquals(Say.NEED_PHONE, t.reply.say); assertFalse(t.ok)
        hands.installed = emptySet(); hands.phoneAllowed = true; hands.ringing = true
        assertEquals(Say.ANSWERED, brain.handle("contesta").reply.say)                             // any phone app: Android answers
    }

    @Test fun contactsLiveInsideTelefono() {
        assertEquals("telefono", Catalog.byPackage("com.zirakusa.telefono")!!.id)
        val c = Catalog.byId("contactos")
        assertEquals("com.zirakusa.contactos.MainActivity", c.activity); assertTrue("com.zirakusa.telefono" in c.packages)
        assertEquals("contactos", Parser.parse("abre contactos")["app"])
    }

    @Test fun notesAndTorch() {
        assertEquals("mañana compro pan", p("Anota que mañana compro pan")["text"]); assertEquals(Action.NOTE, p("crea una nota comprar leche").action)
        assertEquals("1", p("enciende la linterna")["on"]); assertEquals("0", p("apaga la linterna")["on"]); assertEquals(Action.TORCH, p("prende la luz").action)
        val hands = FakeHands(); val brain = Brain(hands)
        val note = brain.handle("apunta llamar a Ana")
        assertEquals(Say.NOTED, note.reply.say); assertEquals(listOf(Graph.TEXT, "app:notas", "do:NOTE"), note.path)
        assertEquals(Say.TORCH_ON, brain.handle("enciende la linterna").reply.say); assertTrue("torch true" in hands.done)
    }

    /** Music that never started: the answer is corrected and the order is marked failed. */
    @Test fun silentMusicIsCorrected() {
        val said = mutableListOf<Say>()
        val state = AssistantState(Brain(FakeHands()), { said += it.say })
        state.send("reproduce música", byVoice = true); assertTrue(state.last!!.ok)
        state.late(com.zirakusa.gatewayhub.assistant.Reply(Say.NOT_PLAYING, listOf("Stone")))
        assertFalse(state.last!!.ok); assertEquals(Say.NOT_PLAYING, state.last!!.reply.say)
        assertEquals(listOf(Say.PLAYING, Say.NOT_PLAYING), said)
    }

    @Test fun theGraphLightsThePath() {
        val brain = Brain(FakeHands())
        val turn = brain.handle("reproduce música", byVoice = true)
        // A tree: input → app → what the app does. Done or failed is a mark, not a node.
        assertEquals(listOf(Graph.VOICE, "app:stone", "do:PLAY_MUSIC"), turn.path); assertTrue(turn.ok)
        assertEquals(listOf(Graph.TEXT), brain.handle("cuéntame un chiste").path)
        assertEquals(listOf(Graph.TEXT, "app:notas", "do:COUNT"), brain.handle("vamos a contar").path)       // Conteo hangs from Notas
        assertEquals(listOf(Graph.TEXT, "app:reloj", "do:TIME"), brain.handle("qué hora es").path)          // the hour hangs from Reloj, Lucía says it
        assertEquals(listOf(Graph.TEXT, "app:calendario", "do:DATE"), brain.handle("qué día es hoy").path)
        assertEquals(com.zirakusa.gatewayhub.assistant.Say.TIME, FakeHands(installed = emptySet()).let { Brain(it).handle("qué hora es").reply.say })   // without Reloj too
        assertEquals(listOf(Graph.TEXT, "app:luma"), brain.handle("abre la galería").path)
        assertTrue(Graph.edges().contains(Edge("app:notas", "do:COUNT")))
        assertFalse(Graph.edges().any { it.from == Graph.VOICE && it.to.startsWith("do:") })  // inputs only reach apps
        val nodes = Graph.nodes { it.name }
        val ids = nodes.map { it.id }.toSet()
        assertTrue(Graph.edges().all { it.from in ids && it.to in ids })                         // every edge joins two drawn nodes
        assertTrue(turn.path.all { it in ids })
        for (a in Graph.actions) assertEquals(a.name, 1, Graph.edges().count { it.to == Graph.action(a) })   // each action has one parent
        Catalog.apps.forEach { assertTrue(it.id, Graph.app(it) in ids) }                       // every app of the family is drawn
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
