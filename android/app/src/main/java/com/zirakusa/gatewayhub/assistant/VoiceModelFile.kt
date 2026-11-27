package com.zirakusa.gatewayhub.assistant

import java.io.File
import java.io.RandomAccessFile

/** Check the legacy whisper.cpp layout before handing third-party weights to the native loader. */
internal object VoiceModelFile {
    const val MIN_BYTES = 8_000_000L
    const val MAX_BYTES = 1_600_000_000L

    private val widthByLayers = mapOf(4 to 384, 6 to 512, 12 to 768, 24 to 1024, 32 to 1280)
    private val bytesByType = mapOf(
        0 to (1 to 4), 1 to (1 to 2), 2 to (32 to 18), 3 to (32 to 20),
        6 to (32 to 22), 7 to (32 to 24), 8 to (32 to 34), 10 to (256 to 84),
        11 to (256 to 110), 12 to (256 to 144), 13 to (256 to 176), 14 to (256 to 210),
    )
    private val typeByFormat = mapOf(7 to 8, 8 to 6, 9 to 7)
    private val tensorName = Regex("[a-z0-9_.]+")

    fun hasLayout(file: File): Boolean {
        if (file.length() !in MIN_BYTES..MAX_BYTES) return false
        return runCatching { RandomAccessFile(file, "r").use(::checkLayout) }.getOrDefault(false)
    }

    private fun checkLayout(file: RandomAccessFile): Boolean = with(file) {
        fun number() = Integer.reverseBytes(readInt())
        fun skip(bytes: Long) {
            require(bytes in 0..(length() - filePointer))
            seek(filePointer + bytes)
        }
        if (number() != 0x67676d6c) return false
        val vocab = number()
        val audioContext = number()
        val width = number()
        val audioHeads = number()
        val audioLayers = number()
        val textContext = number()
        val textWidth = number()
        val textHeads = number()
        val textLayers = number()
        val mels = number()
        val format = number()
        val formatType = format % 1000
        val block = bytesByType[typeByFormat[formatType] ?: formatType]?.first ?: return false
        if (vocab !in 51865..51866 || audioContext != 1500 || textContext != 448 ||
            widthByLayers[audioLayers] != width || textWidth != width ||
            audioHeads != width / 64 || textHeads != audioHeads || textLayers !in 2..audioLayers ||
            mels !in listOf(80, 128) || format !in 0..2014 || width % block != 0) return false
        if (number() != mels || number() != 201) return false
        skip(mels * 201L * 4)
        val tokens = number()
        if (tokens !in 50_000..vocab) return false
        repeat(tokens) { skip(number().toLong().also { require(it in 0..4096) }) }

        val names = mutableSetOf<String>()
        val expectedTensors = 11 + audioLayers * 15 + textLayers * 24
        repeat(expectedTensors) {
            val dimensions = number()
            val nameLength = number()
            val packing = bytesByType[number()] ?: return false
            if (dimensions !in 1..3 || nameLength !in 1..128) return false
            val row = number()
            if (row !in 1..vocab || row % packing.first != 0) return false
            var bytes = row.toLong() / packing.first * packing.second
            var elements = row.toLong()
            repeat(dimensions - 1) {
                val axis = number()
                if (axis !in 1..vocab) return false
                bytes *= axis
                elements *= axis
                if (elements > Int.MAX_VALUE) return false
            }
            val tensor = ByteArray(nameLength)
            readFully(tensor)
            val name = tensor.toString(Charsets.US_ASCII)
            if (!tensorName.matches(name) || !names.add(name)) return false
            skip(bytes)
        }
        filePointer == length()
    }
}
