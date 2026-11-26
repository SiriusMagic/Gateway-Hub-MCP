package com.zirakusa.gatewayhub.assistant

import androidx.compose.runtime.getValue
import com.zirakusa.gatewayhub.assistant.local.LocalAssistant
import com.zirakusa.gatewayhub.assistant.local.LocalTurns
import kotlinx.coroutines.runBlocking
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
    private data class Waiting(val plan: IntentPlan, val from: Int)
    private var waiting: Waiting? = null
    private var musicPrompt: Turn? = null
    private var musicUntil = 0L
    internal var executeOrders: (suspend () -> Unit) -> Unit = { runBlocking { it() } }
    internal var executing by mutableStateOf(false)
        private set
    private var callNameUntil = 0L
    internal var local: LocalAssistant? = null
    internal var message: (Int) -> String = { "" }
    private val localTurns = LocalTurns({ message(it) }, ::complete)
    var turns by mutableStateOf(emptyList<Turn>()); private set
    var typed by mutableStateOf("")
    var listening by mutableStateOf(false)
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

    internal val canRestartChat get() = !executing && local?.busy != true &&
        (turns.isNotEmpty() || typed.isNotEmpty() || listening)

    internal fun restartChat() {
        if (executing || local?.busy == true) return
        brain.cancelQuestion()
        localTurns.clearReview()
        local?.clearConversation()
        waiting = null
        musicPrompt = null
        musicUntil = 0L
        callNameUntil = 0L
        proposal = null
        correction = null
        votes = emptyMap()
        feedbackFailed = false
        contactNotice = ""
        turns = emptyList()
        typed = ""
        partial = ""
        listening = false
        level = 0f
        pulse = 0
    }

    fun send(text: String, byVoice: Boolean) {
        val heard = text.trim()
        val assistant = local
        if (heard.isBlank() || proposal != null || executing || assistant?.busy == true || assistant?.pending != null) return
        musicPrompt?.let { prompt ->
            val choice = prompt.musicChoice ?: return@let
            val number = Words.normalize(heard).toIntOrNull()
            if (System.currentTimeMillis() <= musicUntil) {
                if (choice.volumeBlocked && Intents.yes(heard)) return chooseMusic(prompt, 0)
                if (!choice.volumeBlocked && number != null && number in 1..choice.options.size) return chooseMusic(prompt, number - 1)
            }
            musicPrompt = null
        }

        val nameReply = last?.takeIf { askingCallName }
        callNameUntil = 0L
        if (nameReply != null && brain.commandFor(heard).action == Action.UNKNOWN) {
            PhoneSpeech.answerName(heard)?.let { name ->
                val command = nameReply.command.copy(slots = nameReply.command.slots + ("who" to name))
                val confirmedName = nameReply.evidence?.plan?.let { previous ->
                    val steps = previous.steps.toMutableList()
                    val index = nameReply.evidence?.step ?: 0
                    steps[index] = IntentStep(steps[index].phrase, command)
                    IntentEvidence(previous.copy(steps = steps), index)
                }
                finish(brain.handle(heard, byVoice, command).also { it.evidence = confirmedName })
                return
            }
        }

        val after = waiting.also { waiting = null }?.takeIf {
            if (brain.askingToCall) Confirmation.callAnswer(heard) == true else brain.asking && Intents.yes(heard)
        }
        if (brain.asking) {
            val answer = if (brain.askingToCall) Confirmation.callAnswer(heard)
                else if (Intents.yes(heard)) true else false.takeIf { Confirmation.no.matches(Words.normalize(heard)) }
            if (answer != null) {
                val order = brain.commandFor(heard)
                val turn = brain.handle(heard, byVoice, order)
                if (turn.reply.say == Say.NEED_CALL_PERMISSION) waiting = after
                val original = last?.evidence
                if (original != null && turn.command.action == original.plan.steps[original.step].command.action) turn.evidence = original
                if (after != null && turn.reply.say != Say.NEED_CALL_PERMISSION) {
                    complete(turn, speakIt = false)
                    return runOrders { runSteps(after.plan, byVoice, after.from, listOf(turn.reply)) }
                }
                finish(turn)
                return
            }
            brain.cancelQuestion()
        }

        val plan = IntentPlan.validated(heard, Intents.split(heard, brain::commandFor), brain::commandFor)
        if (plan != null) return runAll(plan, byVoice)

        val mode = modeNow()
        if (mode != AnswerMode.RULES && assistant != null) {
            if (localTurns.route(assistant, heard, byVoice)) return
            localTurns.interpret(assistant, heard, byVoice, interpreter, ::knows) { orders, source ->
                val proposal = IntentPlan.validated(heard, orders, brain::commandFor, source) ?: return@interpret false
                this.proposal = proposal
                proposalByVoice = byVoice
                true
            }
        } else finish(brain.handle(heard, byVoice))
    }

    private fun knows(text: String) = brain.commandFor(text).action != Action.UNKNOWN

    private fun runAll(plan: IntentPlan, byVoice: Boolean, from: Int = 0) = runOrders { runSteps(plan, byVoice, from) }

    private suspend fun runSteps(plan: IntentPlan, byVoice: Boolean, from: Int, previous: List<Reply> = emptyList()) {
        val replies = previous.toMutableList()
        for (index in from until plan.steps.size) {
            val step = plan.steps[index]
            val turn = brain.execute(step.phrase, byVoice, step).also { it.evidence = IntentEvidence(plan, index) }
            complete(turn, speakIt = false)
            replies += turn.reply
            if (turn.reply.asksCallName) break
            if (turn.musicChoice != null || turn.reply.asksToCall || turn.reply.say in setOf(Say.CONFIRM, Say.NEED_CALL_PERMISSION)) {
                waiting = Waiting(plan, index + 1)
                break
            }
        }
        if (byVoice && voiceAnswers) speakAll(replies.filter { it.say != Say.COUNTING })
    }

    internal fun chooseMusic(prompt: Turn, index: Int, cancel: Boolean = false) {
        if (executing || prompt !== musicPrompt || System.currentTimeMillis() > musicUntil) return
        val choice = prompt.musicChoice ?: return
        val selected = choice.options.getOrNull(index)
        if (!cancel && !choice.volumeBlocked && selected == null) return
        val remaining = waiting
        waiting = null
        musicPrompt = null
        runOrders {
            val turn = if (cancel) prompt.copy(reply = Reply(Say.CANCELLED), ok = false)
                else brain.music(prompt.heard, prompt.byVoice, prompt.command, selected, choice.volumeBlocked)
            turn.evidence = prompt.evidence
            finish(turn)
            if (turn.musicChoice != null) waiting = remaining
            else if (remaining != null && !cancel) runSteps(remaining.plan, prompt.byVoice, remaining.from)
        }
    }

    private fun runOrders(work: suspend () -> Unit) {
        executing = true
        executeOrders {
            try { work() } finally { executing = false }
        }
    }

    internal var proposal by mutableStateOf<IntentPlan?>(null)
        private set
    private var proposalByVoice = false

    internal fun confirmPlan(approved: Boolean) {
        val plan = proposal ?: return
        proposal = null
        if (approved) runAll(plan, proposalByVoice)
    }

    private fun finish(turn: Turn) = complete(turn)

    internal fun retryContactPermission() {
        val request = last?.takeIf { it.reply.say == Say.NEED_CONTACTS } ?: return
        complete(brain.handle(request.heard, request.byVoice, request.command).also { it.evidence = request.evidence })
    }

    internal fun finishCallPermission(granted: Boolean) {
        val turn = last?.takeIf { it.reply.say == Say.NEED_CALL_PERMISSION } ?: return
        val rest = waiting.also { waiting = null }
        val (reply, ok) = brain.finishCallPermission(granted) ?: return
        turns = turns.dropLast(1) + turn.copy(reply = reply, ok = ok).also { it.evidence = turn.evidence }
        if (turn.byVoice && voiceAnswers) speak(reply)
        last?.let { feedback?.save(it, feedbackText(it)) }
        if (reply.say == Say.CALL_STARTED && rest != null) runAll(rest.plan, turn.byVoice, rest.from)
    }

    fun confirmHub(confirmed: Boolean) { local?.let { localTurns.confirm(it, confirmed) } }

    private fun complete(turn: Turn) = complete(turn, speakIt = true)

    internal var feedback: IntentFeedback? = null
    internal var feedbackText: (Turn) -> String = { it.reply.args.joinToString(" ") }
    internal var correction by mutableStateOf<Turn?>(null)
    internal var votes by mutableStateOf(emptyMap<String, IntentVote>())
        private set
    internal var feedbackFailed by mutableStateOf(false)
        private set
    internal var shareFeedback by mutableStateOf(false)
    internal var editAliases by mutableStateOf(false)
    internal var savedAliases by mutableStateOf(emptyList<String>())
    internal var chooseContact: (String) -> Unit = {}
    internal var contactNotice by mutableStateOf("")

    internal fun vote(turn: Turn, opinion: IntentVote, wrong: String = "", expected: String = "") {
        val id = turn.evidence?.id ?: return
        if (feedback?.save(turn, feedbackText(turn), opinion, wrong, expected) != true) {
            feedbackFailed = true
            return
        }
        feedbackFailed = false
        votes = votes + (id to opinion)
    }

    internal fun updateSharing(value: Boolean) {
        shareFeedback = value
        feedback?.share(value)
    }

    internal fun removeAlias(name: String) {
        feedback?.putAlias(name, null)
        savedAliases = feedback?.aliases().orEmpty()
    }

    private fun complete(turn: Turn, speakIt: Boolean) {
        if (turn.evidence == null && turn.reply.say !in setOf(Say.CONFIRM, Say.CANCELLED)) {
            turn.evidence = IntentEvidence(IntentPlan(turn.heard, listOf(IntentStep(turn.heard, turn.command)), turn.modelSource), 0)
        }
        if (turn.evidence != null) feedback?.save(turn, feedbackText(turn))
        if (turn.musicChoice != null) {
            musicPrompt = turn
            musicUntil = System.currentTimeMillis() + Brain.CONFIRM_MS
        }
        turns = (turns + turn).takeLast(MAX)
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
        if (t.command.action == Action.PLAY_MUSIC && (t.command["query"] != null || t.command["equalizer"] != null)) return
        if (t.command.action !in setOf(Action.PLAY_MUSIC, Action.PLAY_RECOMMENDED, Action.MEDIA)) return
        turns = turns.dropLast(1) + t.copy(reply = reply.copy(volume = t.reply.volume), ok = false).also { it.evidence = t.evidence }
        last?.let { feedback?.save(it, feedbackText(it)) }
        pulse++
        if (t.byVoice && voiceAnswers) speak(reply)
    }

    fun sendTyped() { send(typed, byVoice = false); typed = "" }
    fun heard(text: String, final: Boolean) { partial = text; if (final) { listening = false; partial = ""; send(text, byVoice = true) } }

    companion object { const val MAX = 40 }
}

/** Who understands what is said. RULES: Lucía's own rules (instant, offline, no model). MODEL: the local model interprets or chats after the local rules. BOTH: the rules first, the model for what they miss. */
enum class AnswerMode { RULES, MODEL, BOTH; companion object { fun from(s: String?) = entries.firstOrNull { it.name == s } ?: BOTH } }

/** How the model carries out what it understood. FEELINGS: as Lucía's own orders (her rules do them and ask «¿…?»).
 * MCP: it picks a family app's tool itself (what changes something shows a card first). AUTO: orders first, MCP if none. */
enum class Interpreter { FEELINGS, MCP, AUTO; companion object { fun from(s: String?) = entries.firstOrNull { it.name == s } ?: FEELINGS } }
