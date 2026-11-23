package com.zirakusa.gatewayhub.assistant

import java.time.LocalDateTime

/** A person found in the phone's contacts. */
data class Person(val name: String, val number: String)

/** A family app's «Hoy» summary (see Inicio's FAMILIA.md §7). */
data class Summary(val title: String, val lines: List<String>, val empty: String)

/**
 * What the assistant does on the phone; the app gives Android, tests a fake. Each returns false when it couldn't. Calls
 * and messages only open the screen with the number or text ready: the user presses «Llamar» or «Enviar».
 */
interface Hands {
    fun installed(app: FamilyApp): Boolean
    fun open(app: FamilyApp): Boolean
    fun playMusic(query: String?): Boolean
    fun playRecommended(): Boolean
    fun media(key: MediaKey): Boolean
    fun alarm(hour: Int, minute: Int): Boolean
    fun timer(seconds: Int): Boolean
    fun dial(number: String): Boolean
    fun message(number: String, text: String?): Boolean
    /** Null: not found; throws [NoContacts] without the permission. */
    fun person(name: String): Person?
    fun summary(app: FamilyApp): Summary?
    /** Leaves a new note in Notas (a finished count); its id, or null when Notas is missing or too old. */
    fun saveNote(title: String, body: String, folder: String): Long?
    /** Answers or ends the call ringing now; throws [NoPhone] without the permission. False: no call. */
    fun answerCall(): Boolean
    fun endCall(): Boolean
    /** The phone's torch. */
    fun torch(on: Boolean): Boolean
    /** The phone's media volume, 0–100. */
    fun volume(): Int
    fun setVolume(percent: Int): Boolean
    fun now(): LocalDateTime = LocalDateTime.now()
    class NoContacts : Exception()
    class NoPhone : Exception()
}

/** What the assistant answers, written and spoken by the screen in the user's language. */
enum class Say {
    OPENING, NOT_INSTALLED, PLAYING, PLAYING_QUERY, RECOMMENDED, MEDIA_DONE, ALARM_SET, TIMER_SET, CALLING, MESSAGING,
    NO_CONTACT, NEED_CONTACTS, TIME, DATE, SUMMARY, NOTHING_TODAY, NOT_UNDERSTOOD, UNKNOWN_APP, FAILED, COUNTING, VOLUME_SET,
    ANSWERED, HUNG_UP, NO_CALL, NEED_PHONE, NOTED, TORCH_ON, TORCH_OFF
}

/** [volume]: the volume set along with the order («pon música al 30 %»), said after the answer. */
data class Reply(val say: Say, val args: List<String> = emptyList(), val lines: List<String> = emptyList(), val volume: Int? = null)

/** One exchange: what was heard, how it was understood, the app that did it, the answer, if it worked and how long it took. */
data class Turn(val heard: String, val byVoice: Boolean, val command: Command, val app: FamilyApp?, val reply: Reply, val ok: Boolean, val millis: Long) {
    /** The lit path of the graph: input → app → action. Whether it worked ([ok]) is marked apart, not as a node. */
    val path get() = Graph.path(byVoice, command, app, reply.volume != null)
}

/** Understands and acts. Pure apart from its [Hands]. */
class Brain(private val hands: Hands, private val clock: () -> Long = System::currentTimeMillis) {
    fun handle(heard: String, byVoice: Boolean = false): Turn {
        val start = clock()
        val c = Parser.parse(heard)
        val app = when (c.action) { Action.OPEN -> c["app"]?.let(Catalog::byId); else -> Catalog.appFor(c.action) }
        // The volume first, so music never starts too loud; then the order.
        val volume = if (c.action != Action.VOLUME) change(c)?.let { v -> v.takeIf { hands.setVolume(it) } } else null
        val (reply, ok) = act(c, app)
        return Turn(heard, byVoice, c, app, if (volume != null) reply.copy(volume = volume) else reply, ok, clock() - start)
    }

    private fun change(c: Command): Int? {
        val v = c["volume"]?.toIntOrNull()?.let { Volume.Change.To(it) } ?: c["volume_step"]?.let { Volume.Change.Step(it == "up") } ?: return null
        return Volume.apply(v, hands.volume())
    }

    private fun act(c: Command, app: FamilyApp?): Pair<Reply, Boolean> {
        fun missing() = Reply(Say.NOT_INSTALLED, listOf(app!!.name)) to false
        fun done(ok: Boolean, r: Reply) = if (ok) r to true else Reply(Say.FAILED, listOf(app?.name.orEmpty())) to false
        if (app != null && c.action !in setOf(Action.WEATHER, Action.AGENDA, Action.COUNT, Action.ANSWER, Action.HANG_UP) && !hands.installed(app)) return missing()
        return when (c.action) {
            Action.OPEN -> done(hands.open(app!!), Reply(Say.OPENING, listOf(app.name)))
            Action.PHOTO, Action.SCAN, Action.SPEED -> done(hands.open(app!!), Reply(Say.OPENING, listOf(app.name)))
            Action.PLAY_MUSIC -> c["query"].let { q -> done(hands.playMusic(q), if (q == null) Reply(Say.PLAYING) else Reply(Say.PLAYING_QUERY, listOf(q))) }
            Action.PLAY_RECOMMENDED -> done(hands.playRecommended(), Reply(Say.RECOMMENDED))
            Action.MEDIA -> done(hands.media(MediaKey.valueOf(c["key"]!!)), Reply(Say.MEDIA_DONE, listOf(c["key"]!!)))
            Action.ALARM -> done(hands.alarm(c.int("hour")!!, c.int("minute")!!), Reply(Say.ALARM_SET, listOf(c["hour"]!!, c["minute"]!!)))
            Action.TIMER -> done(hands.timer(c.int("seconds")!!), Reply(Say.TIMER_SET, listOf(c["seconds"]!!)))
            Action.CALL, Action.MESSAGE -> reach(c, app!!)
            Action.WEATHER, Action.AGENDA -> {
                val s = if (hands.installed(app!!)) hands.summary(app) else null
                if (s == null) missing()
                else if (s.lines.isEmpty()) Reply(Say.NOTHING_TODAY, listOf(s.empty)) to true
                else Reply(Say.SUMMARY, listOf(s.title), s.lines) to true
            }
            Action.TODAY -> {
                val parts = listOf("clima", "calendario", "reloj", "notas").map(Catalog::byId).filter(hands::installed).mapNotNull(hands::summary)
                val lines = parts.flatMap { s -> s.lines.take(2) }
                if (lines.isEmpty()) Reply(Say.NOTHING_TODAY, listOf("")) to true else Reply(Say.SUMMARY, listOf(""), lines) to true
            }
            // The screen turns into the count; Notas only matters when it is saved.
            Action.COUNT -> Reply(Say.COUNTING, listOfNotNull(c["title"])) to true
            Action.VOLUME -> change(c)?.let { v -> if (hands.setVolume(v)) Reply(Say.VOLUME_SET, listOf("$v")) to true else null }
                ?: (Reply(Say.FAILED, listOf("Volumen")) to false)
            // Android's own call handling: works whatever the phone app is.
            Action.ANSWER, Action.HANG_UP -> try {
                val ok = if (c.action == Action.ANSWER) hands.answerCall() else hands.endCall()
                (if (ok) Reply(if (c.action == Action.ANSWER) Say.ANSWERED else Say.HUNG_UP) else Reply(Say.NO_CALL)) to ok
            } catch (_: Hands.NoPhone) { Reply(Say.NEED_PHONE) to false }
            Action.NOTE -> c["text"].orEmpty().let { text ->
                val title = text.replaceFirstChar { it.uppercase() }.take(60)
                if (hands.saveNote(title, text, "Lucía") != null) Reply(Say.NOTED, listOf(text)) to true else Reply(Say.FAILED, listOf(app!!.name)) to false
            }
            Action.TORCH -> (c["on"] == "1").let { on -> done(hands.torch(on), Reply(if (on) Say.TORCH_ON else Say.TORCH_OFF)) }
            Action.TIME -> hands.now().let { Reply(Say.TIME, listOf("${it.hour}", "${it.minute}")) to true }
            Action.DATE -> Reply(Say.DATE, listOf(hands.now().toLocalDate().toString())) to true
            Action.UNKNOWN -> (c["app_name"]?.let { Reply(Say.UNKNOWN_APP, listOf(it)) } ?: Reply(Say.NOT_UNDERSTOOD)) to false
        }
    }

    /** «Llama a Ana» / «escríbele a Ana»: a number as said, or the contact's; only the screen opens, the user confirms. */
    private fun reach(c: Command, app: FamilyApp): Pair<Reply, Boolean> {
        val who = c["who"].orEmpty()
        val digits = who.filter { it.isDigit() || it == '+' }
        val person = if (digits.count(Char::isDigit) >= 3) Person(who, digits)
            else try { hands.person(who) } catch (_: Hands.NoContacts) { return Reply(Say.NEED_CONTACTS) to false } ?: return Reply(Say.NO_CONTACT, listOf(who)) to false
        val ok = if (c.action == Action.CALL) hands.dial(person.number) else hands.message(person.number, c["text"])
        return if (ok) Reply(if (c.action == Action.CALL) Say.CALLING else Say.MESSAGING, listOf(person.name)) to true else Reply(Say.FAILED, listOf(app.name)) to false
    }
}
