package com.zirakusa.gatewayhub.assistant

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Listens only while the button is held or until a pause: Android's recognizer, on the phone when it can (Android 12+),
 * so nothing is sent and almost no battery is used. Nothing listens in the background.
 */
class Ears(private val context: Context, private val heard: (partial: String, final: Boolean) -> Unit, private val level: (Float) -> Unit,
    private val failed: (Int) -> Unit) {
    private var recognizer: SpeechRecognizer? = null
    val available get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        stop()
        val onDevice = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        val r = if (onDevice) SpeechRecognizer.createOnDeviceSpeechRecognizer(context) else SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = level(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = level(0f)
            override fun onError(error: Int) { level(0f); failed(error) }
            override fun onResults(results: Bundle?) = heard(best(results), true)
            override fun onPartialResults(partial: Bundle?) = heard(best(partial), false)
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1))
    }

    private fun best(b: Bundle?) = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()

    fun stop() { recognizer?.run { runCatching { cancel() }; destroy() }; recognizer = null }
}
