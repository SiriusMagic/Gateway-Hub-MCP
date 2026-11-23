package com.zirakusa.gatewayhub.assistant

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

/**
 * Lucía as the phone's assistant: when the user picks her in Ajustes › Aplicaciones predeterminadas › Asistente digital,
 * Android opens her with its own gestures (swiping up from a bottom corner, holding the power or home button) and lets
 * her open herself when she hears «Oye Lucía».
 */
class LuciaService : VoiceInteractionService() {
    override fun onReady() { super.onReady(); current = this }
    override fun onShutdown() { current = null; super.onShutdown() }

    companion object {
        @Volatile private var current: LuciaService? = null
        /** Opens Lucía through Android (only while she is the phone's assistant). */
        fun show(args: Bundle): Boolean = current?.let { runCatching { it.showSession(args, 0); true }.getOrDefault(false) } ?: false
    }
}

class LuciaSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = LuciaSession(this)
}

/** The session only opens Lucía's screen, listening at once, and steps aside. */
class LuciaSession(service: LuciaSessionService) : VoiceInteractionSession(service) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        startAssistantActivity(Intent(context, AssistantActivity::class.java).setAction(AssistantActivity.LISTEN))
        hide()
    }
}

/**
 * Android asks every assistant for a speech recognizer: Lucía's is Vosk, on the phone. Other apps can use it too
 * (it only turns speech into text, nothing else).
 */
class LuciaRecognition : RecognitionService() {
    private var ears: VoskEars? = null

    override fun onStartListening(intent: Intent?, callback: Callback) {
        ears?.stop()
        ears = VoskEars(this, heard = { text, final ->
            val b = Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(text)) }
            runCatching { if (final) callback.results(b) else callback.partialResults(b) }
        }, level = { runCatching { callback.rmsChanged(it * 10f) } }, failed = { runCatching { callback.error(SpeechRecognizer.ERROR_CLIENT) } })
        runCatching { callback.readyForSpeech(Bundle()) }
        ears?.start()
    }

    override fun onStopListening(callback: Callback) { ears?.stop() }
    override fun onCancel(callback: Callback) { ears?.stop(); ears = null }
}
