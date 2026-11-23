package com.zirakusa.gatewayhub.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** The assistant's spoken answers, with the phone's own voice (works offline when the voice is installed). */
class Voice(context: Context) {
    private var ready = false
    private var done: () -> Unit = {}
    private val tts = TextToSpeech(context.applicationContext) { status -> ready = status == TextToSpeech.SUCCESS }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) = finished()
            @Deprecated("Still called by older phones") override fun onError(id: String?) = finished()
            override fun onStop(id: String?, interrupted: Boolean) = finished()
        })
    }

    private fun finished() { val d = done; done = {}; d() }

    /** Says [text]; [after] runs when it finished (at once when there is no voice). */
    fun say(text: String, after: () -> Unit = {}) {
        if (!ready || text.isBlank()) return after()
        finished()
        done = after
        tts.language = Locale.getDefault()
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "assistant") != TextToSpeech.SUCCESS) finished()
    }

    fun quiet() { if (ready) tts.stop() }
    fun close() = tts.shutdown()
}
