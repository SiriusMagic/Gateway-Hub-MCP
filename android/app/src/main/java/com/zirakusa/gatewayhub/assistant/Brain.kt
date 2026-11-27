package com.zirakusa.gatewayhub.assistant

import java.time.LocalDateTime

/** A person found in the phone's contacts. */
data class Person(val name: String, val number: String)

/** A family app's «Hoy» summary (see Inicio's FAMILIA.md §7). */
data class Summary(val title: String, val lines: List<String>, val empty: String)

/**
 * What the assistant does on the phone; the app gives Android, tests a fake. Each returns false when it couldn't. Calls
 * can open the dialer or start a confirmed call. Messages open the compose screen.
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
    fun placeCall(number: String, speaker: Boolean): Boolean = false
    fun inCall(): Boolean = false
    fun searchWeb(query: String): Boolean = false
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
    /**
     * Runs one MCP tool of a family app on the phone (no internet), through its own Modo MCP: its answer
     * (structuredContent), or null when the app is too old to be asked. Throws [ToolOff] when Arka's «Permitir MCP local»
     * is off (or the app is too old), [ToolError] when the tool refused.
     */
    fun tool(app: FamilyApp, name: String, args: org.json.JSONObject): org.json.JSONObject? = null
    /** Cámara opens with a 3 s countdown (after the user's «sí»); false when Cámara is missing or too old. */
    fun tracedTool(app: FamilyApp, name: String, args: org.json.JSONObject, id: String) = tool(app, name, args)

    fun mcpPing(app: FamilyApp, id: String): Boolean = false

    fun capture(selfie: Boolean): Boolean = false
    /** «… en segundo plano»: once the other app has started its work, Lucía comes back to the front. */
    fun comeBack() {}
    /** The phone's media volume, 0–100. */
    fun volume(): Int
    fun setVolume(percent: Int): Boolean
    fun now(): LocalDateTime = LocalDateTime.now()
    class NoCallPermission : Exception()
    class NoContacts : Exception()
    class NoPhone : Exception()
    class ToolOff : Exception()
    class ToolError(val code: String, message: String) : Exception(message)
}

/** What the assistant answers, written and spoken by the screen in the user's language. */
enum class Say {
    OPENING, NOT_INSTALLED, PLAYING, PLAYING_QUERY, RECOMMENDED, MEDIA_DONE, ALARM_SET, TIMER_SET, CALLING, MESSAGING,
    NO_CONTACT, ASK_CALL_CONTACT, NEED_CONTACTS, TIME, DATE, SUMMARY, NOTHING_TODAY, NOT_UNDERSTOOD, UNKNOWN_APP, FAILED, COUNTING, VOLUME_SET,
    ANSWERED, HUNG_UP, NO_CALL, NEED_PHONE, NOTED, TORCH_ON, TORCH_OFF,
    TOOL_RESULT, TOOL_OFF, TOOL_OLD, TOOL_ERROR, NOT_PLAYING, CONFIRM, CANCELLED, GENERATED, QUICK, CALCULATED, MATH_ERROR,
    WEB_OPENED, WEB_QUERY, WEB_UNAVAILABLE, CONFIRM_CALL, CONFIRM_SPEAKER_CALL, CALL_SPEAKER_MANUAL, CALL_STARTED, NEED_CALL_PERMISSION
}

/** [volume]: the volume set along with the order («pon música al 30 %»), said after the answer. */
data class Reply(val say: Say, val args: List<String> = emptyList(), val lines: List<String> = emptyList(), val volume: Int? = null) {
    val asksCallName get() = say == Say.ASK_CALL_CONTACT
    val asksToCall get() = say == Say.CONFIRM_CALL || say == Say.CONFIRM_SPEAKER_CALL
}

/** One exchange: what was heard, how it was understood, the app that did it, the answer, if it worked and how long it took. */
data class Turn(val heard: String, val byVoice: Boolean, val command: Command, val app: FamilyApp?, val reply: Reply, val ok: Boolean, val millis: Long, internal val mcp: McpOrder? = null) {
    /** The lit path of the graph: input → app → action. Whether it worked ([ok]) is marked apart, not as a node. */
    val path get() = Graph.path(byVoice, command, app, reply.volume != null)
}

/** Understands and acts. Pure apart from its [Hands]. */
class Brain(private val hands: Hands, private val clock: () -> Long = System::currentTimeMillis) {
    companion object { const val CONFIRM_MS = 60_000L }
    /** The rules first; what they don't know, said again in Lucía's own words ([Meaning], layer 2), before any model. */
    internal fun commandFor(heard: String): Command {
        val today = hands.now().toLocalDate()
        val c = Parser.parse(heard, today)
        return if (c.action != Action.UNKNOWN) c else Meaning.understand(heard) { Parser.parse(it, today) } ?: c
    }

    fun handle(heard: String, byVoice: Boolean = false, c: Command = commandFor(heard)): Turn {
        val start = clock()
        outgoingCalls.answer(heard)?.let { (reply, ok) ->
            return Turn(heard, byVoice, Command(Action.CALL), Catalog.byId("telefono"), reply, ok, clock() - start)
        }
        // The answer to a question Lucía just asked («¿Borro la lista Rock?» → «sí»).
        pending?.let { p -> answer(Words.normalize(heard).replace('ñ', 'n'))?.let { (reply, ok) ->
            return Turn(heard, byVoice, Command(Action.TOOL, mapOf("app" to p.app.id, "tool" to p.skill.tool)), p.app, reply, ok, clock() - start)
        } }
        if (c.action == Action.CALL && (c["who"].isNullOrBlank() || byVoice && PhoneSpeech.missingName(c["who"].orEmpty()))) {
            return Turn(heard, byVoice, c, Catalog.byId("telefono"), Reply(Say.ASK_CALL_CONTACT), false, clock() - start)
        }
        val app = when (c.action) { Action.OPEN, Action.TOOL -> c["app"]?.let(Catalog::byId); else -> Catalog.appFor(c.action) }
        if (StoneOrder.matches(c)) return StoneOrder.execute(hands, heard, byVoice, c, clock)
        // The volume first, so music never starts too loud; then the order.
        val volume = if (c.action != Action.VOLUME) change(c)?.let { v -> v.takeIf { hands.setVolume(it) } } else null
        val (reply, ok) = act(c, app)
        if (ok && c["background"] == "1") hands.comeBack()
        return Turn(heard, byVoice, c, app, if (volume != null) reply.copy(volume = volume) else reply, ok, clock() - start)
    }

    private fun change(c: Command): Int? {
        val v = c["volume"]?.toIntOrNull()?.let { Volume.Change.To(it) } ?: c["volume_step"]?.let { Volume.Change.Step(it == "up") } ?: return null
        return Volume.apply(v, hands.volume())
    }

    private fun playByArtistSearch(app: FamilyApp, query: String): Pair<Reply, Boolean>? {
        val found = try { hands.tool(app, "library_search", org.json.JSONObject().put("query", query).put("kind", "audio").put("limit", MusicPick.MAX)) }
            catch (_: Exception) { return null } ?: return null
        val pick = MusicPick.pick(query, found.optJSONArray("results"))
            ?: return Reply(Say.TOOL_RESULT, listOf("No encontré «$query» en tu biblioteca de Stone.")) to false
        val played = try { hands.tool(app, "player_play_songs", org.json.JSONObject().put("songIds", pick.ids.joinToString(","))) }
            catch (_: Exception) { return null } ?: return null
        return if (played.optJSONObject("error") != null) null else Reply(Say.TOOL_RESULT, listOf(MusicPick.say(pick, query))) to true
    }

    private fun act(c: Command, app: FamilyApp?): Pair<Reply, Boolean> {
        fun missing() = Reply(Say.NOT_INSTALLED, listOf(app!!.name)) to false
        fun done(ok: Boolean, r: Reply) = if (ok) r to true else Reply(Say.FAILED, listOf(app?.name.orEmpty())) to false
        if (app != null && c.action !in setOf(Action.WEATHER, Action.AGENDA, Action.COUNT, Action.ANSWER, Action.HANG_UP, Action.MEDIA, Action.CALL, Action.CALL_SPEAKER) && !hands.installed(app)) return missing()
        return when (c.action) {
            Action.OPEN -> done(hands.open(app!!), Reply(Say.OPENING, listOf(app.name)))
            Action.PHOTO, Action.SCAN, Action.SPEED -> done(hands.open(app!!), Reply(Say.OPENING, listOf(app.name)))
            Action.PLAY_MUSIC -> c["query"]?.let { artist ->
                playByArtistSearch(app!!, artist) ?: Reply(Say.TOOL_OLD, listOf(app.name)) to false
            } ?: done(hands.playMusic(null), Reply(Say.PLAYING))
            Action.PLAY_RECOMMENDED -> done(hands.playRecommended(), Reply(Say.RECOMMENDED))
            Action.MEDIA -> done(hands.media(MediaKey.valueOf(c["key"]!!)), Reply(Say.MEDIA_DONE, listOf(c["key"]!!)))
            Action.ALARM -> done(hands.alarm(c.int("hour")!!, c.int("minute")!!), Reply(Say.ALARM_SET, listOf(c["hour"]!!, c["minute"]!!)))
            Action.TIMER -> done(hands.timer(c.int("seconds")!!), Reply(Say.TIMER_SET, listOf(c["seconds"]!!)))
            Action.CALL_SPEAKER -> (if (hands.inCall()) Reply(Say.CALL_SPEAKER_MANUAL) else Reply(Say.NO_CALL)) to false
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
            Action.TOOL -> tool(c, app!!)
            Action.TIME -> hands.now().let { Reply(Say.TIME, listOf("${it.hour}", "${it.minute}")) to true }
            Action.DATE -> Reply(Say.DATE, listOf(hands.now().toLocalDate().toString())) to true
            Action.CALCULATE -> c["calculation"]?.let { Reply(Say.CALCULATED, listOf(it)) to true }
                ?: (Reply(Say.MATH_ERROR) to false)
            Action.WEB_SEARCH -> c["query"].orEmpty().let { query ->
                when {
                    query.isBlank() -> Reply(Say.WEB_QUERY) to true
                    hands.searchWeb(query) -> Reply(Say.WEB_OPENED, listOf(query)) to true
                    else -> Reply(Say.WEB_UNAVAILABLE) to false
                }
            }
            Action.CHAT -> c["quick"]?.let { Reply(Say.QUICK, listOf(it)) to (it != "image_unavailable") }
                ?: (Reply(Say.NOT_UNDERSTOOD) to false)
            Action.UNKNOWN, Action.HUB, Action.PHONE_MCP -> (c["app_name"]?.let { Reply(Say.UNKNOWN_APP, listOf(it)) } ?: Reply(Say.NOT_UNDERSTOOD)) to false
        }
    }

    /** A tool waiting for the user's «sí» (it deletes, edits or takes a photo), and until when it waits. */
    private class Pending(val app: FamilyApp, val skill: Skill, val sent: org.json.JSONObject, val said: Map<String, Any?>, val until: Long)
    private var pending: Pending? = null
    /** Lucía asked «¿…?» and waits for «sí» or «no». */
    val askingToCall get() = outgoingCalls.asking
    val asking get() = pending != null || askingToCall

    /**
     * A family app's tool: the item first when the skill needs one («la lista Rock», «la alarma de las 7»), then the question
     * when it deletes or edits, then the tool itself with only the arguments it knows.
     */
    private fun tool(c: Command, app: FamilyApp): Pair<Reply, Boolean> {
        val name = c["tool"]!!
        val skill = Skills.byTool(name)
        return try {
            val args = LinkedHashMap<String, Any?>()
            c.slots.forEach { (k, v) -> when { k.startsWith("arg:") -> args[k.removePrefix("arg:")] = v; k.startsWith("num:") -> args[k.removePrefix("num:")] = v.toInt()
                k.startsWith("bool:") -> args[k.removePrefix("bool:")] = v.toBoolean(); k.startsWith("dbl:") -> args[k.removePrefix("dbl:")] = v.toDouble() } }
            val sent = org.json.JSONObject(args.filterValues { it != null })
            // The items first: «la lista Rock», «la alarma de las 7», «esta canción» (two when needed: a song and a list).
            listOfNotNull(skill?.lookup, skill?.also).forEachIndexed { i, l ->
                val answer = hands.tool(app, l.tool, org.json.JSONObject(l.with(args.filterValues { it != null }.mapValues { it.value!! })))
                    ?: return Reply(Say.TOOL_OLD, listOf(app.name)) to false
                val wanted = Words.normalize(args[l.said]?.toString().orEmpty())
                val items = if (l.list.isEmpty()) listOf(answer) else answer.optJSONArray(l.list)?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }.orEmpty()
                val found = if (l.list.isEmpty()) items.firstOrNull() else
                    items.firstOrNull { Words.normalize(it.optString(l.field)) == wanted } ?: items.firstOrNull { wanted.isNotEmpty() && Words.normalize(it.optString(l.field)).contains(wanted) }
                        ?: items.firstOrNull().takeIf { l.first }
                if (found == null || !found.has(l.id) || found.isNull(l.id) || found.optString(l.id).isBlank())
                    return Reply(Say.TOOL_ERROR, listOf(app.name, if (l.list.isEmpty()) "No está sonando nada." else "No encuentro «${args[l.said] ?: "eso"}».")) to false
                sent.remove(l.said); sent.put(l.arg, found.get(l.id).toString().let { v -> if (found.get(l.id) is Number) found.get(l.id) else v })
                args[if (i == 0) "found" else "found2"] = found.optString(l.field).ifBlank { args[l.said]?.toString().orEmpty() }
            }
            sent.keys().asSequence().toList().filter { it.startsWith("_") }.forEach(sent::remove)
            if (skill?.confirm != null) {
                pending = Pending(app, skill, sent, args, clock() + CONFIRM_MS)
                return Reply(Say.CONFIRM, listOf(fill(skill.confirm, args))) to true
            }
            run(app, name, skill, sent, args)
        } catch (_: Hands.ToolOff) { Reply(Say.TOOL_OFF, listOf(app.name)) to false
        } catch (e: Hands.ToolError) { Reply(Say.TOOL_ERROR, listOf(app.name, e.message.orEmpty())) to false }
    }

    private fun run(app: FamilyApp, name: String, skill: Skill?, sent: org.json.JSONObject, said: Map<String, Any?>): Pair<Reply, Boolean> {
        // The photo by voice is an order to Cámara, not a tool: it opens with a countdown the user sees and can stop.
        if ((skill?.call ?: name) == FamilySkills.CAPTURE)
            return if (hands.capture(sent.optBoolean("selfie"))) Reply(Say.TOOL_RESULT, listOf(fill(skill?.done.orEmpty(), said))) to true
                else Reply(Say.TOOL_OLD, listOf(app.name)) to false
        val r = hands.tool(app, skill?.call ?: name, sent) ?: return Reply(Say.TOOL_OLD, listOf(app.name)) to false
        val text = skill?.done?.let { fill(it, said) } ?: Speech.say(skill?.call ?: name, r, hands.now().toLocalDate(), org.json.JSONObject(said.filterValues { it != null }))
        return Reply(Say.TOOL_RESULT, listOf(text)) to true
    }

    /** «Sí» / «no» to the question asked; null when the phrase is not an answer (then it is a new order). */
    private fun answer(t: String): Pair<Reply, Boolean>? {
        val p = pending ?: return null
        if (clock() > p.until) { pending = null; return null }
        return when {
            Confirmation.yes.matches(t) -> {
                pending = null
                try { run(p.app, p.skill.tool, p.skill, p.sent, p.said) }
                catch (_: Hands.ToolOff) { Reply(Say.TOOL_OFF, listOf(p.app.name)) to false }
                catch (e: Hands.ToolError) { Reply(Say.TOOL_ERROR, listOf(p.app.name, e.message.orEmpty())) to false }
            }
            Confirmation.no.matches(t) -> { pending = null; Reply(Say.CANCELLED) to true }
            else -> { pending = null; null }
        }
    }

    private val outgoingCalls = OutgoingCalls(hands, clock)
    internal fun finishCallPermission(granted: Boolean) = outgoingCalls.finishPermission(granted)
    internal fun cancelOutgoingCall() = outgoingCalls.cancel()

    /** «Llama a Ana» / «escríbele a Ana»: a number as said, or the contact's. */
    private fun reach(c: Command, app: FamilyApp): Pair<Reply, Boolean> {
        val who = c["who"].orEmpty()
        val digits = who.filter { it.isDigit() || it == '+' }
        val person = if (who.matches(Regex("[+0-9 .-]+")) && digits.count(Char::isDigit) >= 3) Person(who, digits)
            else try { hands.person(who) } catch (_: Hands.NoContacts) { return Reply(Say.NEED_CONTACTS) to false } ?: return Reply(Say.NO_CONTACT, listOf(who)) to false
        if (c.action == Action.CALL) {
            return if (c["dial_only"] == "1") outgoingCalls.dial(person) else outgoingCalls.request(person, speaker = c["speaker"] == "1")
        }
        return if (hands.message(person.number, c["text"])) Reply(Say.MESSAGING, listOf(person.name)) to true
            else Reply(Say.FAILED, listOf(app.name)) to false
    }
}
