package com.zirakusa.gatewayhub.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Uses an installed Spanish voice without sending the text to a network TTS service. */
class Voice(context: Context, private val onAvailable: (Boolean) -> Unit = {}) {
    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var closed = false
    var available = false
        private set
    private var active: String? = null
    private var next = 0L
    private var done: () -> Unit = {}
    private val tts = TextToSpeech(context.applicationContext) { status ->
        main.post {
            if (!closed) {
                ready = status == TextToSpeech.SUCCESS
                refresh()
            }
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) { main.post { finished(id) } }
            @Deprecated("Still called by older phones") override fun onError(id: String?) { main.post { finished(id) } }
            override fun onStop(id: String?, interrupted: Boolean) { main.post { finished(id) } }
        })
    }

    fun refresh() {
        val voice = if (ready) tts.voices.orEmpty().filter {
            it.locale.language == "es" && !it.isNetworkConnectionRequired && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()
        }.maxWithOrNull(compareBy({ it.locale == Locale.getDefault() }, { it.quality })) else null
        available = voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS
        onAvailable(available)
    }

    private fun finished(id: String?) {
        if (id == null || id != active) return
        active = null
        val callback = done
        done = {}
        callback()
    }

    fun say(text: String, after: () -> Unit = {}) {
        if (!available || text.isBlank()) return after()
        finished(active)
        val id = "lucia-${++next}"
        active = id
        done = after
        if (tts.speak(text.take(TextToSpeech.getMaxSpeechInputLength()), TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) finished(id)
    }

    fun quiet() {
        if (ready) tts.stop()
        finished(active)
    }

    fun close() {
        closed = true
        quiet()
        tts.shutdown()
    }
}
