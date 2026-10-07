package com.soundmorph.voicechanger.audio

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavUtils {

    /**
     * Reads a 16-bit mono PCM WAV file and returns audio samples as ShortArray and sampleRate.
     */
    fun readWavFile(file: File): Pair<ShortArray, Int> {
        val bytes = file.readBytes()
        if (bytes.size < 44) return Pair(ShortArray(0), 44100)

        val sampleRate = ByteBuffer.wrap(bytes, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val numChannels = ByteBuffer.wrap(bytes, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
        val bitsPerSample = ByteBuffer.wrap(bytes, 34, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

        // Find data chunk
        var dataOffset = 12
        while (dataOffset + 8 <= bytes.size) {
            val chunkId = String(bytes, dataOffset, 4)
            val chunkSize = ByteBuffer.wrap(bytes, dataOffset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (chunkId == "data") {
                dataOffset += 8
                val numSamples = chunkSize / (bitsPerSample / 8) / numChannels
                val shorts = ShortArray(numSamples)
                val buffer = ByteBuffer.wrap(bytes, dataOffset, chunkSize.coerceAtMost(bytes.size - dataOffset))
                    .order(ByteOrder.LITTLE_ENDIAN)

                for (i in 0 until numSamples) {
                    if (buffer.remaining() >= 2) {
                        shorts[i] = buffer.short
                        // Skip extra channels if stereo
                        if (numChannels > 1 && buffer.remaining() >= 2 * (numChannels - 1)) {
                            buffer.position(buffer.position() + 2 * (numChannels - 1))
                        }
                    }
                }
                return Pair(shorts, sampleRate)
            }
            dataOffset += 8 + chunkSize
        }

        // Fallback: assume header is 44 bytes
        val pcmLength = (bytes.size - 44) / 2
        val shorts = ShortArray(pcmLength)
        val buffer = ByteBuffer.wrap(bytes, 44, bytes.size - 44).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until pcmLength) {
            if (buffer.remaining() >= 2) {
                shorts[i] = buffer.short
            }
        }
        return Pair(shorts, if (sampleRate > 0) sampleRate else 44100)
    }

    /**
     * Writes ShortArray samples to a mono 16-bit PCM WAV file.
     */
    fun writeWavFile(samples: ShortArray, sampleRate: Int, outputFile: File) {
        val totalAudioLen = samples.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // SubChunk1Size (16 for PCM)
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat 1 = PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // BlockAlign
        header[33] = 0
        header[34] = 16 // BitsPerSample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val byteBuffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) {
            byteBuffer.putShort(s)
        }

        FileOutputStream(outputFile).use { fos ->
            fos.write(header)
            fos.write(byteBuffer.array())
        }
    }
}
