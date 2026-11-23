package com.zirakusa.gatewayhub.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** The assistant's spoken answers, with the phone's own voice (works offline when the voice is installed). */
class Voice(context: Context) {
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext) { status -> ready = status == TextToSpeech.SUCCESS }

    fun say(text: String) {
        if (!ready || text.isBlank()) return
        tts.language = Locale.getDefault()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "assistant")
    }

    fun quiet() { if (ready) tts.stop() }
    fun close() = tts.shutdown()
}
