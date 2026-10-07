package com.soundmorph.voicechanger.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorder(
    private val outputFile: File,
    private val onAmplitudeChanged: ((amplitude: Int) -> Unit)? = null
) {
    companion object {
        const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordJob: Job? = null

    @SuppressLint("MissingPermission")
    fun startRecording(coroutineScope: CoroutineScope) {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        val rawPcmFile = File(outputFile.parentFile, "temp_recording.pcm")

        recordJob = coroutineScope.launch(Dispatchers.IO) {
            val audioData = ShortArray(bufferSize / 2)
            FileOutputStream(rawPcmFile).use { outputStream ->
                val byteBuffer = ByteBuffer.allocate(audioData.size * 2).order(ByteOrder.LITTLE_ENDIAN)

                while (isRecording && isActive) {
                    val readCount = audioRecord?.read(audioData, 0, audioData.size) ?: 0
                    if (readCount > 0) {
                        byteBuffer.clear()
                        var maxAmp = 0
                        for (i in 0 until readCount) {
                            val sample = audioData[i]
                            byteBuffer.putShort(sample)
                            val absSample = Math.abs(sample.toInt())
                            if (absSample > maxAmp) maxAmp = absSample
                        }
                        outputStream.write(byteBuffer.array(), 0, readCount * 2)
                        onAmplitudeChanged?.invoke(maxAmp)
                    }
                }
            }

            // Convert raw PCM to proper WAV file
            if (rawPcmFile.exists()) {
                val pcmBytes = rawPcmFile.readBytes()
                val samples = ShortArray(pcmBytes.size / 2)
                ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
                WavUtils.writeWavFile(samples, SAMPLE_RATE, outputFile)
                rawPcmFile.delete()
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}
