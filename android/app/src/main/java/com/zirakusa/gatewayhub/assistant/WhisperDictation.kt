package com.zirakusa.gatewayhub.assistant

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal data class VoiceSample(val model: WhisperModel, val text: String?, val endToTextMs: Long)
internal data class DictationComparison(val vosk: String, val samples: List<VoiceSample>, val comparison: Boolean)

internal object NativeWhisper {
    init { System.loadLibrary("lucia_whisper") }

    external fun recognize(path: String, samples: ShortArray, length: Int, threads: Int, cancelled: AtomicBoolean): ByteArray?
}

internal fun interface UtteranceTranscriber {
    fun transcribe(samples: ShortArray, length: Int, cancelled: AtomicBoolean): String?
}

internal class WhisperDictation(private val file: File) : UtteranceTranscriber {
    private val annotation = Regex("""\[[^\]]+]|\([^)]*\)|♪""")

    override fun transcribe(samples: ShortArray, length: Int, cancelled: AtomicBoolean): String? {
        if (cancelled.get() || length < Vosk.RATE / 4) return null
        return try {
            val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
            NativeWhisper.recognize(file.absolutePath, samples, length, threads, cancelled)
                ?.toString(Charsets.UTF_8)?.replace(annotation, "")?.trim()?.takeIf(String::isNotBlank)
        } catch (_: LinkageError) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }
}
