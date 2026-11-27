package com.zirakusa.gatewayhub.assistant

import androidx.compose.runtime.getValue
import com.zirakusa.gatewayhub.assistant.local.LocalAssistant
import com.zirakusa.gatewayhub.assistant.local.LocalTurns
import com.zirakusa.gatewayhub.assistant.local.StoneRequests
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The only owner of the assistant's screen: the conversation, what is being heard, the last order (it lights the graph). */
class AssistantState(private val brain: Brain, private val speak: (Reply) -> Unit = {},
    /** Several answers said as one (several orders in one sentence); by default one after another. */
    private val speakAll: (List<Reply>) -> Unit = { it.lastOrNull()?.let(speak) }) {
    /** Who understands what is said: the rules, the model or both (rules first). */
    var mode by mutableStateOf(AnswerMode.BOTH)
    /** With the screen off or locked, only the rules (fast, light, and nothing personal goes to a long answer). */
    var rulesWhenLocked by mutableStateOf(true)
    /** How the model acts: through Lucía's orders («sentimientos»), straight through MCP, or the orders first. */
    var interpreter by mutableStateOf(Interpreter.FEELINGS)
    /** The phone is off or locked right now (the activity knows). */
    internal var locked: () -> Boolean = { false }

    /** The mode for this order: the model only when one is chosen, and not with the screen off or locked if so set. */
    internal fun modeNow(): AnswerMode {
        val model = local?.models?.let { it.supported && it.hasModel } == true
        return when {
            !model -> AnswerMode.RULES
            rulesWhenLocked && locked() -> AnswerMode.RULES
            else -> mode
        }
    }

    /** Orders said after one that asks «¿…?»: they run after the «sí». */
    private var waiting = emptyList<String>()
    private var callNameUntil = 0L
    internal var local: LocalAssistant? = null
    internal var message: (Int) -> String = { "" }
    private val localTurns = LocalTurns({ message(it) }, ::complete)
    var turns by mutableStateOf(emptyList<Turn>()); private set
    var typed by mutableStateOf("")
    var listening by mutableStateOf(false)
    internal var speechModels: SpeechModels? = null
    internal var dictationComparison by mutableStateOf<DictationComparison?>(null)
    internal var lastTranscript by mutableStateOf<String?>(null)
       private set
    internal var lastMcpTurn by mutableStateOf<Turn?>(null)
       private set
    internal var diagnostics: McpDiagnostics? = null
    internal var executingStone by mutableStateOf(false)
        private set
    internal var runStone: (() -> Turn, (Turn) -> Unit) -> Unit = { work, done -> done(work()) }
    var partial by mutableStateOf("")
    var level by mutableFloatStateOf(0f)
    var voiceAnswers by mutableStateOf(true)
    /** Lucía's settings page and what the phone allows (the activity keeps these up to date). */
    internal var page by mutableStateOf(AssistantPage.CHAT)
    /** «Mapa de Lucía», opened from her settings. */
    var brainOpen by mutableStateOf(false)
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
        if (heard.isBlank() || executingStone || assistant?.busy == true || assistant?.pending != null) return
        StoneOrder.choose(last, heard)?.let { choice ->
            executingStone = true
            runStone({ brain.handle(heard, byVoice, choice) }) { turn ->
                executingStone = false
                complete(turn)
            }
            return
        }
        val nameReply = last?.takeIf { askingCallName }
        callNameUntil = 0L
        if (nameReply != null && brain.commandFor(heard).action == Action.UNKNOWN) {
            PhoneSpeech.answerName(heard)?.let { name ->
                val command = nameReply.command.copy(slots = nameReply.command.slots + ("who" to name))
                complete(brain.handle(heard, byVoice, command))
                return
            }
        }
        // The orders left after a question: they go on only after «sí».
        val after = waiting.also { waiting = emptyList() }.takeIf {
            if (brain.askingToCall) Confirmation.callAnswer(heard) == true else brain.asking && Intents.yes(heard)
        }.orEmpty()
        val plan = Intents.split(heard, brain::commandFor)
        if (plan.any { StoneOrder.matches(brain.commandFor(it)) }) {
            return runAll(plan, byVoice)
        }
        val mode = modeNow()
        if (mode != AnswerMode.RULES && assistant != null && localTurns.route(assistant, heard, byVoice)) {
            brain.cancelOutgoingCall()
            return
        }
        // Only the model: it understands everything; the rules only carry out its orders (and still ask «¿…?»).
        if (mode == AnswerMode.MODEL && assistant != null && !brain.asking) {
            localTurns.interpret(assistant, heard, byVoice, interpreter, ::knows) { orders -> runAll(orders, byVoice) }
            return
        }
        // «Llama a Luis y dime la hora»: each order in turn.
        if (plan.size > 1) return runAll(plan, byVoice)
        val command = brain.commandFor(plan[0]).takeIf { it.action != Action.UNKNOWN } ?: brain.commandFor(heard)
        if (mode == AnswerMode.BOTH && assistant != null && StoneRequests.matches(heard, command)) {
            brain.cancelOutgoingCall()
            localTurns.stone(assistant, heard, heard, byVoice)
            return
        }
        val turn = brain.handle(heard, byVoice, command)
        if (turn.reply.say == Say.NEED_CALL_PERMISSION) {
            waiting = after
            complete(turn)
        } else if (turn.reply.say == Say.NOT_UNDERSTOOD && mode == AnswerMode.BOTH && assistant != null) {
            // The model says it in Lucía's own orders; only when there are none, it talks.
            localTurns.interpret(assistant, heard, byVoice, interpreter, ::knows) { orders -> runAll(orders, byVoice) }
        } else if (after.isNotEmpty()) {
            runAll(after, byVoice, first = turn)
        } else {
            complete(turn)
        }
    }

    /** Whether the rules know [text] as an order. */
    private fun knows(text: String) = brain.commandFor(text).action != Action.UNKNOWN

    /**
     * Several orders, one after another, answered together. One that asks first («¿Borro la lista?») stops there: the
     * rest waits for the «sí».
     */
    private fun runAll(orders: List<String>, byVoice: Boolean, first: Turn? = null) {
        val replies = mutableListOf<Reply>()
        first?.let { complete(it, speakIt = false); replies += it.reply }
        fun speakResults() {
            if (byVoice && voiceAnswers) speakAll(replies.filter { it.say != Say.COUNTING })
        }
        fun next(index: Int) {
            val order = orders.getOrNull(index) ?: return speakResults()
            val command = brain.commandFor(order)
            val done: (Turn) -> Unit = { turn ->
                executingStone = false
                complete(turn, speakIt = false)
                replies += turn.reply
                when {
                    turn.reply.asksCallName -> speakResults()
                    turn.reply.asksToCall || turn.reply.say in setOf(Say.CONFIRM, Say.NEED_CALL_PERMISSION) -> {
                        waiting = orders.drop(index + 1)
                        speakResults()
                    }
                    else -> next(index + 1)
                }
            }
            if (StoneOrder.matches(command)) {
                executingStone = true
                runStone({ brain.handle(order, byVoice, command) }, done)
            } else done(brain.handle(order, byVoice, command))
        }
        next(0)
    }

    internal fun retryContactPermission() {
        val request = last?.takeIf { it.reply.say == Say.NEED_CONTACTS } ?: return
        complete(brain.handle(request.heard, request.byVoice, request.command))
    }

    internal fun finishCallPermission(granted: Boolean) {
        val turn = last?.takeIf { it.reply.say == Say.NEED_CALL_PERMISSION } ?: return
        val rest = waiting.also { waiting = emptyList() }
        val (reply, ok) = brain.finishCallPermission(granted) ?: return
        turns = turns.dropLast(1) + turn.copy(reply = reply, ok = ok)
        if (turn.byVoice && voiceAnswers) speak(reply)
        if (reply.say == Say.CALL_STARTED && rest.isNotEmpty()) runAll(rest, turn.byVoice)
    }

    fun confirmHub(confirmed: Boolean) { local?.let { localTurns.confirm(it, confirmed) } }

    private fun complete(turn: Turn) = complete(turn, speakIt = true)

    private fun complete(turn: Turn, speakIt: Boolean) {
        turns = (turns + turn).takeLast(MAX)
        turn.mcp?.let {
            lastMcpTurn = turn
            diagnostics?.record(it)
        }
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
        turns = turns.dropLast(1) + t.copy(reply = reply.copy(volume = t.reply.volume), ok = false)
        pulse++
        if (t.byVoice && voiceAnswers) speak(reply)
    }

    fun sendTyped() { send(typed, byVoice = false); typed = "" }
    fun heard(text: String, final: Boolean) {
        partial = text
        if (final) {
            listening = false
            partial = ""
            lastTranscript = text
            send(text, byVoice = true)
        }
    }

    companion object { const val MAX = 40 }
}

/** Who understands what is said. RULES: Lucía's own rules (instant, offline, no model). MODEL: the local model reads
 * everything and turns it into her orders or talks. BOTH: the rules first, the model for what they miss. */
enum class AnswerMode { RULES, MODEL, BOTH; companion object { fun from(s: String?) = entries.firstOrNull { it.name == s } ?: BOTH } }

/** How the model carries out what it understood. FEELINGS: as Lucía's own orders (her rules do them and ask «¿…?»).
 * MCP: it picks a family app's tool itself (what changes something shows a card first). AUTO: orders first, MCP if none. */
enum class Interpreter { FEELINGS, MCP, AUTO; companion object { fun from(s: String?) = entries.firstOrNull { it.name == s } ?: FEELINGS } }
