package com.zirakusa.gatewayhub.assistant

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.provider.ContactsContract
import android.os.SystemClock
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Vosk, the offline Spanish recognizer, with its small model shipped inside the app (assets/model-es, Apache-2.0).
 * The model is copied out once and loaded once; everything runs on the phone, nothing is sent anywhere.
 */
object Vosk {
    const val RATE = 16_000
    @Volatile private var model: Model? = null
    private val waiting = mutableListOf<(Model?) -> Unit>()
    @Volatile private var loading = false

    /** Gives the model (null when it could not load), loading it the first time. */
    fun model(context: Context, ready: (Model?) -> Unit) {
        model?.let { return ready(it) }
        synchronized(waiting) { waiting += ready; if (loading) return; loading = true }
        StorageService.unpack(context.applicationContext, "model-es", "model", { m -> done(m) }, { _ -> done(null) })
    }

    private fun done(m: Model?) {
        model = m
        val all = synchronized(waiting) { loading = false; waiting.toList().also { waiting.clear() } }
        all.forEach { it(m) }
    }

    fun canListen(context: Context) = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // checked by canListen before every start
    fun microphone(): AudioRecord? = runCatching {
        val size = maxOf(AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), RATE / 5 * 2)
        AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size)
            .takeIf { it.state == AudioRecord.STATE_INITIALIZED }
    }.getOrNull()

    fun text(json: String, key: String = "text") = runCatching { JSONObject(json).optString(key) }.getOrDefault("")

    /**
     * The names of the contacts, so «llama a Ana» is heard: from the phone with the permission, otherwise from Teléfono (it
     * already has it), by each first letter. Kept 10 minutes.
     */
    fun people(context: Context): List<String> {
        val permitted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        cached?.let { if (it.permitted == permitted && System.currentTimeMillis() - it.at < it.lifetime) return it.names }
        val names = if (permitted) runCatching {
            context.contentResolver.query(ContactsContract.Contacts.CONTENT_URI, arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY), null, null, null)?.use { c ->
                buildList { while (c.moveToNext() && size < 500) add(c.getString(0).orEmpty()) }
            }.orEmpty()
        }.getOrDefault(emptyList()) else fromPhoneApp(context)
        cached = ContactCache(System.currentTimeMillis(), permitted, names)
        return names
    }

    private data class ContactCache(val at: Long, val permitted: Boolean, val names: List<String>) {
        val lifetime = if (names.isEmpty()) 30_000L else 600_000L
    }
    @Volatile private var cached: ContactCache? = null

    private fun fromPhoneApp(context: Context): List<String> {
        val hands = AndroidHands(context.applicationContext)
        val phone = Catalog.byId("telefono")
        val names = LinkedHashSet<String>()
        for (letter in "abcdefghijklmnñopqrstuvwxyz") {
            val list = runCatching { hands.tool(phone, "contacts_search", JSONObject().put("query", letter.toString()).put("limit", 50)) }
                .getOrNull()?.optJSONArray("contacts") ?: if (letter == 'a') return emptyList() else continue
            for (i in 0 until list.length()) list.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }?.let(names::add)
            if (names.size >= 500) break
        }
        return names.toList()
    }
}

/**
 * Hears one order: the microphone runs only from the tap (or «Oye Lucía») until a pause or 8 s. Two recognizers listen
 * at once, one limited to the assistant's words and one free; [Hearing] picks. [heard] gets the partial text and then
 * the final one (on the main thread of the caller's choosing).
 */
internal class VoskEars(private val context: Context, private val heard: (String, Boolean) -> Unit, private val level: (Float) -> Unit,
    private val failed: (Int) -> Unit, private val conversational: () -> Boolean = { false },
    private val decoding: () -> Unit = {}, private val compared: (DictationComparison) -> Unit = {}) {
    // Each recording owns its callbacks, even when a new recording starts as the old one is closing.
    private class Recording(val engines: List<DictationEngine>, val mode: DictationMode) {
        val cancelled = AtomicBoolean(false)
    }
    private val current = AtomicReference<Recording?>(null)

    fun start(confirmCall: Boolean = false, contactName: Boolean = false,
              engines: List<DictationEngine> = emptyList(), mode: DictationMode = DictationMode.VOSK) {
        val recording = Recording(if (confirmCall || contactName) emptyList() else engines, mode)
        if (!current.compareAndSet(null, recording)) return
        val freeConversation = conversational()
        Vosk.model(context) { model ->
            if (model == null) {
                if (current.compareAndSet(recording, null)) failed(NO_MODEL)
            } else if (current.get() === recording) {
                Thread({ listen(model, recording, freeConversation, confirmCall, contactName) }, "lucia-ears").start()
            }
        }
    }

    private fun listen(model: Model, recording: Recording, freeConversation: Boolean, confirmCall: Boolean, contactName: Boolean) {
        val mic = Vosk.microphone() ?: run {
            if (current.compareAndSet(recording, null)) failed(NO_MIC)
            return
        }
        val names = if (confirmCall) emptyList() else Vosk.people(context)
        val grammar = if (confirmCall) Hearing.callGrammar() else Vocabulary.grammar(names)
        val limited = Recognizer(model, Vosk.RATE.toFloat(), grammar)
        val free = Recognizer(model, Vosk.RATE.toFloat())
        val buffer = ShortArray(Vosk.RATE / 10)
        val pcm = if (recording.engines.isEmpty()) null else ShortArray(Vosk.RATE * 8)
        var sampleCount = 0
        var lastSound = 0L
        val gate = EnergyGate()
        val start = System.currentTimeMillis()
        var spoke = false
        var said: String? = null
        val freeSegments = mutableListOf<String>()
        val vosk = try {
            mic.startRecording()
            while (current.get() === recording && System.currentTimeMillis() - start < 8_000) {
                val n = mic.read(buffer, 0, buffer.size)
                if (n <= 0) continue
                val rms = gate.rms(buffer, n)
                level((rms / 3000).toFloat().coerceIn(0f, 1f))
                if (pcm != null) {
                    val copied = n.coerceAtMost(pcm.size - sampleCount)
                    buffer.copyInto(pcm, sampleCount, endIndex = copied)
                    sampleCount += copied
                    if (gate.open(rms)) lastSound = SystemClock.elapsedRealtime()
                }
                if (free.acceptWaveForm(buffer, n)) freeSegments += Vosk.text(free.result)
                val freePartial = if (freeConversation) Vosk.text(free.partialResult, "partial") else ""
                if (limited.acceptWaveForm(buffer, n)) {
                    Vosk.text(limited.result).takeIf { it.isNotBlank() }?.let { said = it; spoke = true }
                    if (spoke) break
                } else {
                    val partial = freePartial.ifBlank { Vosk.text(limited.partialResult, "partial") }
                    if (partial.isNotBlank()) { spoke = true; heard(partial, false) }
                }
                if (lastSound != 0L && SystemClock.elapsedRealtime() - lastSound >= 700) break
                if (!spoke && lastSound == 0L && System.currentTimeMillis() - start > 4_000) break
            }
            val a = said ?: Vosk.text(limited.finalResult)
            val b = (freeSegments + Vosk.text(free.finalResult)).filter(String::isNotBlank).joinToString(" ")
            when {
                confirmCall -> Hearing.callConfirmation(a, b)
                contactName -> PhoneSpeech.contactName(a, b, names)
                else -> Hearing.pick(a, b, freeConversation, names)
            }
        } catch (_: RuntimeException) {
            if (current.compareAndSet(recording, null)) failed(NO_MIC)
            return
        } finally {
            runCatching { mic.stop() }; mic.release(); limited.close(); free.close()
        }
        val comparison = if ((spoke || lastSound != 0L || vosk.isNotBlank()) &&
            current.get() === recording && pcm != null) {
            decoding()
            val endpointDelay = if (lastSound == 0L) 0 else SystemClock.elapsedRealtime() - lastSound
            val samples = recording.engines.map { engine ->
                val began = SystemClock.elapsedRealtime()
                val text = engine.transcriber.transcribe(pcm, sampleCount, recording.cancelled)
                VoiceSample(engine.model, text, endpointDelay + SystemClock.elapsedRealtime() - began)
            }
            DictationComparison(vosk, samples, recording.mode == DictationMode.COMPARE)
        } else null
        val chosen = comparison?.samples?.firstOrNull()?.text
        val answer = if (chosen == null) vosk else Hearing.dictated(vosk, chosen, names)
        if (current.compareAndSet(recording, null)) {
            level(0f)
            if (comparison != null) compared(comparison)
            heard(answer, true)
        }
    }

    fun stop() { current.getAndSet(null)?.cancelled?.set(true) }

    companion object { const val NO_MODEL = 1001; const val NO_MIC = 1002 }
}

/**
 * Listens without stopping while a count is open (the screen stays on, nothing runs once it closes). Each phrase is
 * given to [heard] when the speaker pauses. [muted] while Lucía speaks, so her own voice is not counted. Gives up after
 * 10 minutes with nothing said ([ended]).
 */
class CountEars(private val context: Context, private val grammar: () -> String, private val heard: (String) -> Unit,
    private val partial: (String) -> Unit, private val level: (Float) -> Unit, private val failed: (Int) -> Unit, private val ended: () -> Unit) {
    private val running = AtomicBoolean(false)
    @Volatile var muted = false
    @Volatile private var newWords = false

    val active get() = running.get()

    /** The list grew: listen for its words from the next phrase on. */
    fun refresh() { newWords = true }

    fun start() {
        if (!running.compareAndSet(false, true)) return
        Vosk.model(context) { model ->
            if (model == null) { running.set(false); return@model failed(VoskEars.NO_MODEL) }
            Thread({ listen(model) }, "lucia-count").start()
        }
    }

    private fun listen(model: Model) {
        val mic = Vosk.microphone() ?: run { running.set(false); return failed(VoskEars.NO_MIC) }
        val limited = Recognizer(model, Vosk.RATE.toFloat(), grammar())
        val free = Recognizer(model, Vosk.RATE.toFloat())
        val buffer = ShortArray(Vosk.RATE / 10)
        val freeSaid = StringBuilder()
        var lastSpeech = System.currentTimeMillis()
        var timedOut = false
        try {
            mic.startRecording()
            while (running.get()) {
                val n = mic.read(buffer, 0, buffer.size)
                if (n <= 0) continue
                if (muted) { level(0f); continue }
                level((EnergyGate().rms(buffer, n) / 3000).toFloat().coerceIn(0f, 1f))
                if (free.acceptWaveForm(buffer, n)) freeSaid.append(' ').append(Vosk.text(free.result))
                if (limited.acceptWaveForm(buffer, n)) {
                    val a = Vosk.text(limited.result)
                    val b = (freeSaid.toString() + " " + Vosk.text(free.finalResult)).trim()
                    freeSaid.setLength(0)
                    val said = TallyWords.pick(a, b)
                    partial("")
                    if (said.isNotBlank()) { lastSpeech = System.currentTimeMillis(); heard(said) }
                    if (newWords) { newWords = false; limited.setGrammar(grammar()) }
                } else Vosk.text(limited.partialResult, "partial").takeIf { it.isNotBlank() }?.let { lastSpeech = System.currentTimeMillis(); partial(it) }
                if (System.currentTimeMillis() - lastSpeech > 10 * 60_000) { timedOut = true; break }
            }
        } finally {
            runCatching { mic.stop() }; mic.release(); limited.close(); free.close(); running.set(false); level(0f)
        }
        if (timedOut) ended()
    }

    fun stop() { running.set(false) }
}
