package com.zirakusa.gatewayhub.assistant

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal object NativeWhisper {
    init { System.loadLibrary("lucia_whisper") }

    external fun recognize(path: String, samples: ShortArray, length: Int, threads: Int, cancelled: AtomicBoolean): ByteArray?
}

internal object WhisperDictation {
    private val annotation = Regex("""\[[^\]]+]|\([^)]*\)|♪""")

    fun transcribe(file: File, samples: ShortArray, length: Int, cancelled: AtomicBoolean): String? {
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
