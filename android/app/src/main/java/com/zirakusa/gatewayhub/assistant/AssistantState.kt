package com.zirakusa.gatewayhub.assistant

import androidx.compose.runtime.getValue
import com.zirakusa.gatewayhub.assistant.local.LocalAssistant
import com.zirakusa.gatewayhub.assistant.local.LocalTurns
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The only owner of the assistant's screen: the conversation, what is being heard, the last order (it lights the graph). */
class AssistantState(private val brain: Brain, private val speak: (Reply) -> Unit = {},
    /** Several answers said as one (several orders in one sentence); by default one after another. */
    private val speakAll: (List<Reply>) -> Unit = { it.lastOrNull()?.let(speak) }) {
    /** Orders said after one that asks «¿…?»: they run after the «sí». */
    private var waiting = emptyList<String>()
    private var callNameUntil = 0L
    internal var speechModels: SpeechModels? = null
    internal var local: LocalAssistant? = null
    internal var message: (Int) -> String = { "" }
    private val localTurns = LocalTurns({ message(it) }, ::complete)
    var turns by mutableStateOf(emptyList<Turn>()); private set
    internal var ruleTrace by mutableStateOf(emptyList<Turn>()); private set
    internal var ruleInput by mutableStateOf(""); private set
    var typed by mutableStateOf("")
    var listening by mutableStateOf(false)
    var partial by mutableStateOf("")
    var level by mutableFloatStateOf(0f)
    var voiceAnswers by mutableStateOf(true)
    /** Lucía's settings page and what the phone allows (the activity keeps these up to date). */
    internal var page by mutableStateOf(AssistantPage.CHAT)
    var wake by mutableStateOf(false)
    var name by mutableStateOf(WakeName.LUCIA)
    var onlyCharging by mutableStateOf(false)
    var isDefault by mutableStateOf(false)
    var canAppear by mutableStateOf(false)
    var modelReady by mutableStateOf(false)
    var offlineVoiceReady by mutableStateOf(false)
    /** The count open on screen (null: talking), its lines, what was just heard, and the note it was saved as. */
    var tally by mutableStateOf<Tally?>(null); private set
    var tallyLines by mutableStateOf(emptyList<Tallied>()); private set
    var tallyHeard by mutableStateOf("")
    var tallyListening by mutableStateOf(false)
    var tallySavedId by mutableStateOf<Long?>(null)
    var tallyFailed by mutableStateOf(false)
    /** Each new order restarts the light running through the graph. */
    var pulse by mutableIntStateOf(0); private set
    val last get() = turns.lastOrNull()
    internal val askingToCall get() = brain.askingToCall
    internal val askingCallName get() = last?.reply?.asksCallName == true && System.currentTimeMillis() <= callNameUntil
    val health get() = Graph.health(turns)
    val heat get() = Graph.heat(turns)

    fun send(text: String, byVoice: Boolean) {
        val heard = text.trim()
        val assistant = local
        if (heard.isBlank() || assistant?.busy == true) return
        val nameReply = last?.takeIf { askingCallName }
        callNameUntil = 0L
        if (nameReply != null && brain.commandFor(heard).action == Action.UNKNOWN) {
            PhoneSpeech.answerName(heard)?.let { name ->
                val command = nameReply.command.copy(slots = nameReply.command.slots + ("who" to name))
                val turn = brain.handle(heard, byVoice, command)
                if (!turn.reply.asksToCall && !turn.reply.asksCallName && turn.reply.say != Say.NEED_CONTACTS) {
                    waiting = emptyList()
                }
                complete(turn)
                return
            }
        }
        // The orders left after a question: they go on only after «sí».
        val after = waiting.also { waiting = emptyList() }.takeIf {
            if (brain.askingToCall) Confirmation.callAnswer(heard) == true else brain.asking && Intents.yes(heard)
        }.orEmpty()
        if (assistant != null && localTurns.routeChat(assistant, heard, byVoice)) {
            brain.cancelOutgoingCall()
            return
        }
        if (nameReply == null && !(brain.asking && (Intents.yes(heard) || Confirmation.callAnswer(heard) != null))) {
            ruleInput = heard
            ruleTrace = emptyList()
        }
        val plan = Intents.split(heard, brain::commandFor)
        if (plan.size > 1) return runAll(plan, byVoice)
        val command = brain.commandFor(plan[0]).takeIf { it.action != Action.UNKNOWN } ?: brain.commandFor(heard)
        val turn = brain.handle(heard, byVoice, command)
        if (turn.reply.say == Say.NEED_CALL_PERMISSION) {
            waiting = after
            complete(turn)
        } else if (after.isNotEmpty()) {
            runAll(after, byVoice, first = turn)
        } else {
            complete(turn)
        }
    }

    /**
     * Several orders, one after another, answered together. One that asks first («¿Borro la lista?») stops there: the
     * rest waits for the «sí».
     */
    private fun runAll(orders: List<String>, byVoice: Boolean, first: Turn? = null) {
        val replies = mutableListOf<Reply>()
        first?.let { complete(it, speakIt = false); replies += it.reply }
        for ((i, order) in orders.withIndex()) {
            val turn = brain.handle(order, byVoice)
            complete(turn, speakIt = false)
            replies += turn.reply
            if (turn.reply.asksCallName || turn.reply.asksToCall || turn.reply.say in setOf(Say.CONFIRM, Say.NEED_CALL_PERMISSION)) {
                waiting = orders.drop(i + 1)
                break
            }
        }
        if (byVoice && voiceAnswers) speakAll(replies.filter { it.say != Say.COUNTING })
    }

    internal fun retryContactPermission() {
        val request = last?.takeIf { it.reply.say == Say.NEED_CONTACTS } ?: return
        complete(brain.handle(request.heard, request.byVoice, request.command))
    }

    internal fun finishCallPermission(granted: Boolean) {
        val turn = last?.takeIf { it.reply.say == Say.NEED_CALL_PERMISSION } ?: return
        val rest = waiting.also { waiting = emptyList() }
        val (reply, ok) = brain.finishCallPermission(granted) ?: return
        replaceLastRule(turn.copy(reply = reply, ok = ok))
        if (turn.byVoice && voiceAnswers) speak(reply)
        if (reply.say == Say.CALL_STARTED && rest.isNotEmpty()) runAll(rest, turn.byVoice)
    }

    private fun complete(turn: Turn) = complete(turn, speakIt = true)

    private fun complete(turn: Turn, speakIt: Boolean) {
        turns = (turns + turn).takeLast(MAX)
        if (turn.command.action != Action.CHAT || turn.reply.say != Say.GENERATED) ruleTrace = (ruleTrace + turn).takeLast(MAX)
        if (turn.reply.asksCallName) callNameUntil = System.currentTimeMillis() + Brain.CONFIRM_MS
        pulse++
        if (speakIt && turn.byVoice && voiceAnswers) speak(turn.reply)
        if (turn.command.action == Action.COUNT) startTally(turn.command["title"].orEmpty())
        onTurn(turn)
    }

    /** The activity follows each order (a count opens its screen). */
    var onTurn: (Turn) -> Unit = {}

    fun startTally(title: String) { tally = Tally(title); tallyLines = emptyList(); tallyHeard = ""; tallySavedId = null; tallyFailed = false }
    fun endTally() { tally = null; tallyLines = emptyList(); tallyListening = false; tallyHeard = "" }

    /** A phrase heard while counting; [Tally.Outcome] tells the screen how to answer (beep, read, save). */
    fun hearTally(text: String): Tally.Outcome? {
        val t = tally ?: return null
        val o = t.hear(text)
        if (o.kind != Tally.Kind.NOTHING) tallyHeard = text
        tallyLines = t.lines
        return o
    }

    fun bumpTally(key: String, delta: Int) { tally?.let { it.bump(key, delta); tallyLines = it.lines } }

    /**
     * The last order turned out not to work after all (the music never started): its answer is corrected, it is marked
     * failed on the map, and the correction is said.
     */
    fun late(reply: Reply) {
        val t = turns.lastOrNull() ?: return
        if (t.command.action !in setOf(Action.PLAY_MUSIC, Action.PLAY_RECOMMENDED, Action.MEDIA)) return
        replaceLastRule(t.copy(reply = reply.copy(volume = t.reply.volume), ok = false))
        pulse++
        if (t.byVoice && voiceAnswers) speak(reply)
    }

    private fun replaceLastRule(turn: Turn) {
        if (ruleTrace.lastOrNull() === last) ruleTrace = ruleTrace.dropLast(1) + turn
        turns = turns.dropLast(1) + turn
    }

    fun sendTyped() { send(typed, byVoice = false); typed = "" }
    fun heard(text: String, final: Boolean) { partial = text; if (final) { listening = false; partial = ""; send(text, byVoice = true) } }

    companion object { const val MAX = 40 }
}
