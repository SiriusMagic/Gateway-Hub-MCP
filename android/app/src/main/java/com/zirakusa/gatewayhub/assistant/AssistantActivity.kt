package com.zirakusa.gatewayhub.assistant

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubTheme

/**
 * «Asistente»: its own icon, and what Android opens with the assist gesture (holding the power or home button) when it is
 * chosen as the phone's assistant. Opened that way it starts listening at once. Nothing listens in the background.
 */
class AssistantActivity : ComponentActivity() {
    private lateinit var state: AssistantState
    private lateinit var ears: Ears
    private lateinit var voice: Voice
    private val mic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) listen() }
    private val contacts = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) state.last?.let { state.send(it.heard, it.byVoice) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        voice = Voice(this)
        state = AssistantState(Brain(AndroidHands(this))) { voice.say(Talk.text(it, resources)) }
        ears = Ears(this, heard = { text, final -> state.heard(text, final); if (final) afterTurn() }, level = { state.level = it }, failed = ::failed)
        setContent { HubTheme { AssistantScreen(state, AssistantActions(listen = ::listen, stop = ::stop, makeDefault = ::makeDefault)) } }
        if (savedInstanceState == null && intent?.action in setOf(Intent.ACTION_ASSIST, Intent.ACTION_VOICE_COMMAND)) listen()
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); if (intent.action in setOf(Intent.ACTION_ASSIST, Intent.ACTION_VOICE_COMMAND)) listen() }
    override fun onStop() { stop(); voice.quiet(); super.onStop() }
    override fun onDestroy() { ears.stop(); voice.close(); super.onDestroy() }

    private fun listen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return mic.launch(Manifest.permission.RECORD_AUDIO)
        if (!ears.available) return Toast.makeText(this, R.string.no_recognizer, Toast.LENGTH_LONG).show()
        voice.quiet(); state.partial = ""; state.listening = true
        ears.start()
    }

    private fun stop() { ears.stop(); state.listening = false; state.level = 0f }

    /** «Permite los contactos» answers ask for it right away, then retry the same order. */
    private fun afterTurn() { if (state.last?.reply?.say == Say.NEED_CONTACTS) contacts.launch(Manifest.permission.READ_CONTACTS) }

    private fun failed(error: Int) {
        state.listening = false
        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT && error != SpeechRecognizer.ERROR_CLIENT)
            Toast.makeText(this, getString(R.string.listen_failed, error), Toast.LENGTH_SHORT).show()
    }

    /** Android's own page to choose the phone's assistant (apps can't take that role by themselves). */
    private fun makeDefault() {
        try { startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) } catch (_: ActivityNotFoundException) {
            try { startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) } catch (_: ActivityNotFoundException) { }
        }
    }
}
