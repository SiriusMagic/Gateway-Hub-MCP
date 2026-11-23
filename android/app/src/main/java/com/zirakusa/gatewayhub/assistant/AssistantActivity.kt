package com.zirakusa.gatewayhub.assistant

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubTheme

/**
 * Lucía: her own icon, what Android opens with its assistant gestures when she is the phone's assistant, and what
 * «Oye Lucía» opens. Opened by voice or gesture she starts listening at once. Voice is recognised on the phone (Vosk).
 */
class AssistantActivity : ComponentActivity() {
    private lateinit var state: AssistantState
    private lateinit var ears: VoskEars
    private lateinit var voice: Voice
    private lateinit var prefs: Prefs
    private val mic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) listen() }
    private val contacts = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) state.last?.let { state.send(it.heard, it.byVoice) } }
    private val forWake = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r[Manifest.permission.RECORD_AUDIO] == true) setWake(true) else Toast.makeText(this, R.string.wake_needs_mic, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = Prefs(this)
        voice = Voice(this)
        state = AssistantState(Brain(AndroidHands(this))) { voice.say(Talk.text(it, resources)) }
        state.voiceAnswers = prefs.voiceAnswers; state.wake = prefs.wake; state.name = prefs.name; state.onlyCharging = prefs.onlyCharging
        ears = VoskEars(this, heard = { text, final -> runOnUiThread { state.heard(text, final); if (final) afterTurn() } },
            level = { runOnUiThread { state.level = it } }, failed = { runOnUiThread { failed(it) } })
        Vosk.model(this) { m -> runOnUiThread { state.modelReady = m != null } }   // warm up while the screen opens
        setContent {
            HubTheme {
                AssistantScreen(state, AssistantActions(listen = ::listen, stop = ::stop, makeDefault = ::makeDefault, setWake = ::askWake,
                    setName = { prefs.name = it; state.name = it; restartWake() }, setOnlyCharging = { prefs.onlyCharging = it; state.onlyCharging = it; restartWake() },
                    allowAppear = ::allowAppear, setVoice = { prefs.voiceAnswers = it; state.voiceAnswers = it }))
            }
        }
        if (savedInstanceState == null && wantsListening(intent)) listen()
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); if (wantsListening(intent)) listen() }
    override fun onStart() { super.onStart(); WakeService.paused = true }         // Lucía takes the microphone while open
    override fun onResume() { super.onResume(); state.isDefault = isDefault(); state.canAppear = Settings.canDrawOverlays(this) }
    override fun onStop() { stop(); voice.quiet(); WakeService.paused = false; super.onStop() }
    override fun onDestroy() { ears.stop(); voice.close(); super.onDestroy() }

    private fun wantsListening(i: Intent?) = i?.action in setOf(LISTEN, Intent.ACTION_ASSIST, Intent.ACTION_VOICE_COMMAND)

    private fun listen() {
        if (!Vosk.canListen(this)) return mic.launch(Manifest.permission.RECORD_AUDIO)
        voice.quiet(); state.partial = ""; state.listening = true
        ears.start()
    }

    private fun stop() { ears.stop(); state.listening = false; state.level = 0f }

    private fun afterTurn() { if (state.last?.reply?.say == Say.NEED_CONTACTS) contacts.launch(Manifest.permission.READ_CONTACTS) }

    private fun failed(error: Int) {
        state.listening = false
        Toast.makeText(this, if (error == VoskEars.NO_MODEL) R.string.no_model else R.string.no_mic, Toast.LENGTH_LONG).show()
    }

    /** «Oye Lucía» needs the microphone and, to tell the user it is listening, notifications. */
    private fun askWake(on: Boolean) {
        if (!on) return setWake(false)
        forWake.launch(buildList { add(Manifest.permission.RECORD_AUDIO); if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }.toTypedArray())
    }

    private fun setWake(on: Boolean) {
        prefs.wake = on; state.wake = on
        if (on) WakeService.start(this) else WakeService.stop(this)
    }

    private fun restartWake() { if (prefs.wake) { WakeService.stop(this); WakeService.start(this) } }

    private fun isDefault(): Boolean =
        if (Build.VERSION.SDK_INT >= 29) getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
        else Settings.Secure.getString(contentResolver, "assistant").orEmpty().startsWith(packageName)

    /** Android's own page to choose the phone's assistant (apps can't take that role by themselves). */
    private fun makeDefault() {
        for (action in listOf(Settings.ACTION_VOICE_INPUT_SETTINGS, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, Settings.ACTION_SETTINGS)) {
            try { startActivity(Intent(action)); return } catch (_: ActivityNotFoundException) { }
        }
    }

    private fun allowAppear() {
        try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())) }
        catch (_: ActivityNotFoundException) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }
    }

    companion object { const val LISTEN = "com.zirakusa.gatewayhub.assistant.LISTEN" }
}
