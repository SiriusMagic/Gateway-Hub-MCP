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
    var wake by mutableStateOf(false)
    var name by mutableStateOf(WakeName.LUCIA)
    var onlyCharging by mutableStateOf(false)
    var isDefault by mutableStateOf(false)
    var canAppear by mutableStateOf(false)
    var modelReady by mutableStateOf(false)
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
    }

    fun sendTyped() { send(typed, byVoice = false); typed = "" }
    fun heard(text: String, final: Boolean) { partial = text; if (final) { listening = false; partial = ""; send(text, byVoice = true) } }

    companion object { const val MAX = 40 }
}
