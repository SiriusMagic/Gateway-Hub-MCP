package com.zirakusa.gatewayhub.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The only owner of the assistant's screen: the conversation, what is being heard, the last order (it lights the graph). */
class AssistantState(private val brain: Brain, private val speak: (Reply) -> Unit = {}) {
    var turns by mutableStateOf(emptyList<Turn>()); private set
    var typed by mutableStateOf("")
    var listening by mutableStateOf(false)
    var partial by mutableStateOf("")
    var level by mutableFloatStateOf(0f)
    var voiceAnswers by mutableStateOf(true)
    /** Lucía's settings page and what the phone allows (the activity keeps these up to date). */
    var settingsOpen by mutableStateOf(false)
    /** «Mapa de Lucía», opened from her settings. */
    var brainOpen by mutableStateOf(false)
    var wake by mutableStateOf(false)
    var name by mutableStateOf(WakeName.LUCIA)
    var onlyCharging by mutableStateOf(false)
    var isDefault by mutableStateOf(false)
    var canAppear by mutableStateOf(false)
    var modelReady by mutableStateOf(false)
    /** The count open on screen (null: talking), its lines, what was just heard, and the note it was saved as. */
    var tally by mutableStateOf<Tally?>(null); private set
    var tallyLines by mutableStateOf(emptyList<Tallied>()); private set
    var tallyHeard by mutableStateOf("")
    var tallyListening by mutableStateOf(false)
    var tallySavedId by mutableStateOf<Long?>(null)
    var tallyFailed by mutableStateOf(false)
    /** Each new order restarts the light running through the graph. */
    var pulse by mutableStateOf(0); private set
    val last get() = turns.lastOrNull()
    val health get() = Graph.health(turns)
    val heat get() = Graph.heat(turns)

    fun send(text: String, byVoice: Boolean) {
        if (text.isBlank()) return
        val turn = brain.handle(text.trim(), byVoice)
        turns = (turns + turn).takeLast(MAX)
        pulse++
        if (byVoice && voiceAnswers) speak(turn.reply)
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
        turns = turns.dropLast(1) + t.copy(reply = reply, ok = false)
        pulse++
        if (t.byVoice && voiceAnswers) speak(reply)
    }

    fun sendTyped() { send(typed, byVoice = false); typed = "" }
    fun heard(text: String, final: Boolean) { partial = text; if (final) { listening = false; partial = ""; send(text, byVoice = true) } }

    companion object { const val MAX = 40 }
}
