package com.zirakusa.gatewayhub.assistant

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.util.concurrent.atomic.AtomicBoolean

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

    /** The names of the contacts, so «llama a Ana» is heard (only with the contacts permission). */
    fun people(context: Context): List<String> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return emptyList()
        return runCatching {
            context.contentResolver.query(ContactsContract.Contacts.CONTENT_URI, arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY), null, null, null)?.use { c ->
                buildList { while (c.moveToNext() && size < 500) add(c.getString(0).orEmpty()) }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }
}

/**
 * Hears one order: the microphone runs only from the tap (or «Oye Lucía») until a pause or 8 s. Two recognizers listen
 * at once, one limited to the assistant's words and one free; [Hearing] picks. [heard] gets the partial text and then
 * the final one (on the main thread of the caller's choosing).
 */
class VoskEars(private val context: Context, private val heard: (String, Boolean) -> Unit, private val level: (Float) -> Unit,
    private val failed: (Int) -> Unit) {
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        Vosk.model(context) { model ->
            if (model == null) { running.set(false); return@model failed(NO_MODEL) }
            thread = Thread({ listen(model) }, "lucia-ears").apply { start() }
        }
    }

    private fun listen(model: Model) {
        val mic = Vosk.microphone() ?: run { running.set(false); return failed(NO_MIC) }
        val limited = Recognizer(model, Vosk.RATE.toFloat(), Vocabulary.grammar(Vosk.people(context)))
        val free = Recognizer(model, Vosk.RATE.toFloat())
        val buffer = ShortArray(Vosk.RATE / 10)
        val gate = EnergyGate()
        val start = System.currentTimeMillis()
        var spoke = false
        var said: String? = null
        try {
            mic.startRecording()
            while (running.get() && System.currentTimeMillis() - start < 8_000) {
                val n = mic.read(buffer, 0, buffer.size)
                if (n <= 0) continue
                val rms = gate.rms(buffer, n)
                level((rms / 3000).toFloat().coerceIn(0f, 1f))
                free.acceptWaveForm(buffer, n)
                if (limited.acceptWaveForm(buffer, n)) {
                    // A pause: this phrase is finished. Keep it (asking again would give nothing).
                    Vosk.text(limited.result).takeIf { it.isNotBlank() }?.let { said = it; spoke = true }
                    if (spoke) break
                }
                else Vosk.text(limited.partialResult, "partial").takeIf { it.isNotBlank() }?.let { spoke = true; heard(it, false) }
                // Nothing said for 4 s: give up quietly.
                if (!spoke && System.currentTimeMillis() - start > 4_000) break
            }
            val a = said ?: Vosk.text(limited.finalResult); val b = Vosk.text(free.finalResult)
            level(0f)
            heard(Hearing.pick(a, b), true)
        } finally {
            runCatching { mic.stop() }; mic.release(); limited.close(); free.close(); running.set(false)
        }
    }

    fun stop() { running.set(false) }

    companion object { const val NO_MODEL = 1001; const val NO_MIC = 1002 }
}
