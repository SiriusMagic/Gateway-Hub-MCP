package com.zirakusa.gatewayhub.assistant

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.zirakusa.gatewayhub.assistant.local.LocalAssistant
import com.zirakusa.gatewayhub.MainActivity
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.ui.HubTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Lucía: her own icon, what Android opens with its assistant gestures when she is the phone's assistant, and what
 * «Oye Lucía» opens. Opened by voice or gesture she starts listening at once. Voice is recognised on the phone (Vosk).
 */
class AssistantActivity : ComponentActivity() {
    private lateinit var state: AssistantState
    private lateinit var local: LocalAssistant
    private lateinit var ears: VoskEars
    private lateinit var voice: Voice
    private lateinit var prefs: Prefs
    private lateinit var hands: AndroidHands
    private lateinit var count: CountEars
    private var beeper: ToneGenerator? = null
    private var resumeCount = false
    private val mic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) { if (state.tally != null) countOn(greet = true) else listen() } }
    private val contacts = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) state.last?.let { state.send(it.heard, it.byVoice) } }
    private val phone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val forWake = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r[Manifest.permission.RECORD_AUDIO] == true) setWake(true) else Toast.makeText(this, R.string.wake_needs_mic, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = Prefs(this)
        voice = Voice(this) { ready -> if (::state.isInitialized) state.offlineVoiceReady = ready }
        local = LocalAssistant(this, lifecycleScope)
        hands = AndroidHands(this)
        state = AssistantState(Brain(hands)) { if (it.say != Say.COUNTING) voice.say(Talk.text(it, resources)) }
        state.local = local
        state.message = { getString(it) }
        state.offlineVoiceReady = voice.available
        // Music asked for but silent: Lucía says so instead of «Reproduciendo».
        hands.onSilent = { runOnUiThread { state.late(Reply(Say.NOT_PLAYING, listOf("Stone"))) } }
        state.onTurn = { t ->
            if (t.command.action == Action.COUNT) { stop(); countOn(greet = true) }
            if (t.byVoice) afterTurn()
        }
        count = CountEars(this, grammar = { TallyWords.grammar(state.tallyLines) }, heard = { runOnUiThread { counted(it) } },
            partial = { runOnUiThread { state.partial = it } }, level = { runOnUiThread { state.level = it } },
            failed = { runOnUiThread { countOff(); failed(it) } }, ended = { runOnUiThread { countOff() } })
        state.voiceAnswers = prefs.voiceAnswers; state.wake = prefs.wake; state.name = prefs.name; state.onlyCharging = prefs.onlyCharging
        ears = VoskEars(this, heard = { text, final -> runOnUiThread { if (state.listening) state.heard(text, final) } },
            level = { runOnUiThread { state.level = it } }, failed = { runOnUiThread { failed(it) } },
            conversational = { local.models.supported })
        Vosk.model(this) { m -> runOnUiThread { state.modelReady = m != null } }   // warm up while the screen opens
        setContent {
            HubTheme {
                AssistantScreen(state, AssistantActions(listen = ::listen, stop = ::stop, makeDefault = ::makeDefault, setWake = ::askWake,
                    setName = { prefs.name = it; state.name = it; restartWake() }, setOnlyCharging = { prefs.onlyCharging = it; state.onlyCharging = it; restartWake() },
                    allowAppear = ::allowAppear, setVoice = { prefs.voiceAnswers = it; state.voiceAnswers = it },
                    installVoice = ::installVoice, manageHub = {
                        startActivity(Intent(this, MainActivity::class.java).setAction(MainActivity.ACTION_MANAGE_HUB))
                    }),
                    CountActions(pause = ::countOff, resume = { countOn(greet = false) }, save = ::saveCount, discard = { countOff(); state.endTally() },
                        openNote = { state.tallySavedId?.let(hands::openNote) }, share = ::shareCount, again = { state.startTally(""); countOn(greet = true) }))
            }
        }
        if (savedInstanceState == null && wantsListening(intent)) listen()
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); if (wantsListening(intent)) listen() }
    override fun onStart() {
        super.onStart(); WakeService.paused = true                                   // Lucía takes the microphone while open
        if (resumeCount && state.tally != null) countOn(greet = false)
        resumeCount = false
    }
    override fun onResume() { super.onResume(); voice.refresh(); state.isDefault = isDefault(); state.canAppear = Settings.canDrawOverlays(this) }
    override fun onStop() {
        resumeCount = state.tallyListening
        local.leave(); stop(); countOff(); voice.quiet(); WakeService.paused = false; super.onStop()
    }
    override fun onDestroy() { local.close(); ears.stop(); count.stop(); voice.close(); beeper?.release(); super.onDestroy() }

    private fun wantsListening(i: Intent?) = i?.action in setOf(LISTEN, Intent.ACTION_ASSIST, Intent.ACTION_VOICE_COMMAND)

    private fun listen() {
        if (local.busy || local.pending != null) return
        if (!Vosk.canListen(this)) return mic.launch(Manifest.permission.RECORD_AUDIO)
        voice.quiet(); state.partial = ""; state.listening = true
        ears.start()
    }

    private fun stop() { ears.stop(); state.listening = false; state.level = 0f }

    // ---- «Modo conteo» ----

    /** Listens without stopping while the count is open; the screen stays on meanwhile. */
    private fun countOn(greet: Boolean) {
        if (!Vosk.canListen(this)) return mic.launch(Manifest.permission.RECORD_AUDIO)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        state.tallyListening = true
        count.start()
        if (greet) speak(getString(R.string.say_counting))
    }

    private fun countOff() {
        count.stop(); state.tallyListening = false; state.partial = ""; state.level = 0f
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** Lucía's own voice must not be counted: the ears are muted until she finishes. */
    private fun speak(text: String) {
        count.muted = true
        voice.say(text) { count.muted = false }
    }

    private fun beep(tone: Int) {
        if (beeper == null) beeper = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull()
        beeper?.startTone(tone, 90)
    }

    private fun counted(text: String) {
        val o = state.hearTally(text) ?: return
        when (o.kind) {
            Tally.Kind.ADDED -> { beep(ToneGenerator.TONE_PROP_BEEP); count.refresh() }
            Tally.Kind.REMOVED, Tally.Kind.UNDONE -> beep(ToneGenerator.TONE_PROP_NACK)
            Tally.Kind.READ -> speak(read(o.lines, all = o.lines == state.tallyLines))
            Tally.Kind.NOT_FOUND -> speak(getString(R.string.count_not_found))
            Tally.Kind.FINISH -> saveCount()
            Tally.Kind.CANCEL -> countOff()
            Tally.Kind.NOTHING -> Unit
        }
    }

    private fun read(lines: List<Tallied>, all: Boolean): String {
        if (lines.isEmpty()) return getString(R.string.count_nothing)
        val each = lines.joinToString(". ") { getString(R.string.count_line, it.label, it.n) }
        val total = lines.sumOf { it.n }
        return if (all && lines.size > 1) each + ". " + resources.getQuantityString(R.plurals.count_total_said, total, total) else each
    }

    private fun noteTitle(t: Tally): String = if (t.title.isBlank())
        getString(R.string.count_note_title, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
        else getString(R.string.count_note_title_named, t.title.replaceFirstChar(Char::uppercase))

    private fun noteBody(t: Tally) = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n\n" +
        t.table(getString(R.string.count_thing), getString(R.string.count_amount), getString(R.string.count_total))

    /** «Termina» or the button: a new note in Notas (folder «Conteos»); if Notas can't take it, the count stays to share. */
    private fun saveCount() {
        countOff()
        val t = state.tally ?: return
        if (t.lines.isEmpty()) return speak(getString(R.string.count_nothing))
        val id = hands.saveNote(noteTitle(t), noteBody(t), getString(R.string.count_folder))
        if (id != null) { state.tallySavedId = id; speak(resources.getQuantityString(R.plurals.count_saved_said, t.total, t.total)) }
        else { state.tallyFailed = true; speak(getString(R.string.count_no_notas)) }
    }

    private fun shareCount() {
        val t = state.tally ?: return
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, noteTitle(t)).putExtra(Intent.EXTRA_TEXT, noteTitle(t) + "\n" + noteBody(t))
        try { startActivity(Intent.createChooser(send, getString(R.string.count_share))) } catch (_: ActivityNotFoundException) { }
    }

    private fun afterTurn() = when (state.last?.reply?.say) {
        Say.NEED_CONTACTS -> contacts.launch(Manifest.permission.READ_CONTACTS)
        Say.NEED_PHONE -> phone.launch(Manifest.permission.ANSWER_PHONE_CALLS)
        else -> Unit
    }

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

    private fun installVoice() {
        try { startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) }
        catch (_: ActivityNotFoundException) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    private fun allowAppear() {
        try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())) }
        catch (_: ActivityNotFoundException) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }
    }

    companion object { const val LISTEN = "com.zirakusa.gatewayhub.assistant.LISTEN" }
}
