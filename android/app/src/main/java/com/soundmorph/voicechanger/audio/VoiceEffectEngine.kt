package com.soundmorph.voicechanger.audio

import com.soundmorph.voicechanger.model.VoiceEffect
import kotlin.math.*

object VoiceEffectEngine {

    /**
     * Applies a specific voice effect to the input 16-bit PCM samples.
     */
    fun applyEffect(samples: ShortArray, sampleRate: Int, effect: VoiceEffect): ShortArray {
        if (samples.isEmpty()) return ShortArray(0)

        return when (effect) {
            VoiceEffect.ORIGINAL -> samples.clone()
            VoiceEffect.CHIPMUNK -> applyPitchAndSpeed(samples, 1.55f)
            VoiceEffect.MONSTER -> applyMonsterEffect(samples, sampleRate)
            VoiceEffect.ROBOT -> applyRobotEffect(samples, sampleRate)
            VoiceEffect.ALIEN -> applyAlienEffect(samples, sampleRate)
            VoiceEffect.ECHO -> applyEcho(samples, sampleRate, delayMs = 240, decay = 0.5f)
            VoiceEffect.CAVE -> applyCaveReverb(samples, sampleRate)
            VoiceEffect.REVERSE -> applyReverse(samples)
            VoiceEffect.FAST -> applyPitchAndSpeed(samples, 1.4f)
            VoiceEffect.SLOW -> applyPitchAndSpeed(samples, 0.72f)
            VoiceEffect.UNDERWATER -> applyUnderwater(samples, sampleRate)
            VoiceEffect.RADIO -> applyRadioFilter(samples, sampleRate)
            VoiceEffect.MEGAPHONE -> applyMegaphone(samples, sampleRate)
            VoiceEffect.GHOST -> applyGhostEffect(samples, sampleRate)
            VoiceEffect.GIRLY_FLIRTY -> applyGirlyEffect(samples, sampleRate)
        }
    }

    /**
     * Changes pitch & playback rate via linear interpolation resampling.
     */
    private fun applyPitchAndSpeed(samples: ShortArray, factor: Float): ShortArray {
        val newLength = (samples.size / factor).toInt()
        if (newLength <= 0) return ShortArray(0)
        val result = ShortArray(newLength)

        for (i in 0 until newLength) {
            val srcIndex = i * factor
            val i0 = srcIndex.toInt().coerceIn(0, samples.size - 1)
            val i1 = (i0 + 1).coerceIn(0, samples.size - 1)
            val frac = srcIndex - i0
            val val0 = samples[i0].toFloat()
            val val1 = samples[i1].toFloat()
            val interpolated = val0 + frac * (val1 - val0)
            result[i] = interpolated.toInt().coerceIn(-32768, 32767).toShort()
        }
        return result
    }

    /**
     * Monster: Pitch down + distortion + bass boost
     */
    private fun applyMonsterEffect(samples: ShortArray, sampleRate: Int): ShortArray {
        val pitchedDown = applyPitchAndSpeed(samples, 0.7f)
        val result = ShortArray(pitchedDown.size)

        // Low-pass filter (RC filter) for dark rumble
        val cutoff = 1200f
        val rc = 1.0f / (2.0f * Math.PI.toFloat() * cutoff)
        val dt = 1.0f / sampleRate
        val alpha = dt / (rc + dt)

        var lastVal = 0f
        for (i in pitchedDown.indices) {
            val sample = pitchedDown[i].toFloat()
            lastVal += alpha * (sample - lastVal)
            // Soft saturation / growl
            var saturated = lastVal * 1.3f
            if (saturated > 26000f) saturated = 26000f + (saturated - 26000f) * 0.2f
            if (saturated < -26000f) saturated = -26000f + (saturated + 26000f) * 0.2f

            result[i] = saturated.toInt().coerceIn(-32768, 32767).toShort()
        }
        return result
    }

    /**
     * Robot: Ring modulation with 50Hz sine carrier wave
     */
    private fun applyRobotEffect(samples: ShortArray, sampleRate: Int): ShortArray {
        val result = ShortArray(samples.size)
        val carrierFreq = 50.0 // 50 Hz drone
        val twoPiF = 2.0 * Math.PI * carrierFreq / sampleRate

        for (i in samples.indices) {
            val carrier = sin(twoPiF * i)
            // Blend dry signal (30%) + modulated signal (70%)
            val sample = samples[i].toFloat()
            val modulated = (sample * 0.35f) + (sample * carrier.toFloat() * 0.75f)
            result[i] = modulated.toInt().coerceIn(-32768, 32767).toShort()
        }
        return result
    }

    /**
     * Alien: Frequency & amplitude vibrato modulation
     */
    private fun applyAlienEffect(samples: ShortArray, sampleRate: Int): ShortArray {
        val result = ShortArray(samples.size)
        val lfoFreq = 14.0 // 14 Hz fast vibrato
        val twoPiF = 2.0 * Math.PI * lfoFreq / sampleRate

        for (i in samples.indices) {
            val lfo = (sin(twoPiF * i) * 0.5 + 0.5).toFloat()
            val sample = samples[i].toFloat()
            // Tremolo modulation + slight pitch buzz
            val out = sample * (0.3f + 0.7f * lfo)
            result[i] = out.toInt().coerceIn(-32768, 32767).toShort()
        }
        return result
    }

    /**
     * Echo: Single or repeating delay line
     */
    private fun applyEcho(samples: ShortArray, sampleRate: Int, delayMs: Int, decay: Float): ShortArray {
        val delaySamples = (sampleRate * (delayMs / 1000.0)).toInt()
        val totalLength = samples.size + delaySamples * 2
        val result = ShortArray(totalLength)

        val buffer = FloatArray(totalLength)
        for (i in samples.indices) {
            buffer[i] += samples[i].toFloat()
        }

        for (i in delaySamples until totalLength) {
            buffer[i] += buffer[i - delaySamples] * decay
        }

        for (i in 0 until totalLength) {
            result[i] = buffer[i].toInt().coerceIn(-32768, 32767).toShort()
        }
        return result
    }

    /**
     * Cave Reverb: Multi-tap delay
     */
    private fun applyCaveReverb(samples: ShortArray, sampleRate: Int): ShortArray {
        val taps = intArrayOf(
            (sampleRate * 0.040).toInt(),
            (sampleRate * 0.085).toInt(),
            (sampleRate * 0.145).toInt(),
            (sampleRate * 0.220).toInt(),
            (sampleRate * 0.310).toInt()
        )
        val gains = floatArrayOf(0.45f, 0.35f, 0.25f, 0.18f, 0.12f)
        val maxTap = taps.maxOrNull() ?: 0
        val totalLength = samples.size + maxTap
        val result = ShortArray(totalLength)
        val buffer = FloatArray(totalLength)

        for (i in samples.indices) {
            val s = samples[i].toFloat()
            buffer[i] += s * 0.7f
            for (t in taps.indices) {
                val tapIdx = i + taps[t]
                if (tapIdx < totalLength) {
                    buffer[tapIdx] += s * gains[t]
                }
            }
        }

        for (i in 0 until totalLength) {
            result[i] = buffer[i].toInt().coerceIn(-32768, 32767).toShort()
        }
        return result
    }

    /**
     * Reverse audio
     */
    private fun applyReverse(samples: ShortArray): ShortArray {
        val result = ShortArray(samples.size)
        val lastIdx = samples.size - 1
        for (i in samples.indices) {
            result[i] = samples[lastIdx - i]
        }
        return result
    }

    /**
     * Underwater: Low-pass filter at 450Hz
     */
    private fun applyUnderwater(samples: ShortArray, sampleRate: Int): ShortArray {
        val result = ShortArray(samples.size)
        val cutoff = 450f
        val rc = 1.0f / (2.0f * Math.PI.toFloat() * cutoff)
        val dt = 1.0f / sampleRate
        val alpha = dt / (rc + dt)

        var lastVal = 0f
        for (i in samples.indices) {
            val sample = samples[i].toFloat()
            lastVal += alpha * (sample - lastVal)
            result[i] = (lastVal * 1.4f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return result
    }

    /**
     * Vintage Radio / Walkie-talkie: Band-pass (400Hz - 2800Hz) + subtle clipping
     */
    private fun applyRadioFilter(samples: ShortArray, sampleRate: Int): ShortArray {
        val result = ShortArray(samples.size)
        // High-pass first to cut rumble below 400Hz
        val hpCutoff = 450f
        val rcHp = 1.0f / (2.0f * Math.PI.toFloat() * hpCutoff)
        val dt = 1.0f / sampleRate
        val alphaHp = rcHp / (rcHp + dt)

        var prevX = 0f
        var prevY = 0f
        val highPassed = FloatArray(samples.size)

        for (i in samples.indices) {
            val x = samples[i].toFloat()
            prevY = alphaHp * (prevY + x - prevX)
            prevX = x
            highPassed[i] = prevY
        }

        // Low-pass to cut hiss above 2600Hz
        val lpCutoff = 2600f
        val rcLp = 1.0f / (2.0f * Math.PI.toFloat() * lpCutoff)
        val alphaLp = dt / (rcLp + dt)
        var lastLp = 0f

        for (i in highPassed.indices) {
            lastLp += alphaLp * (highPassed[i] - lastLp)
            // Saturation for vintage radio grit
            var out = lastLp * 1.8f
            if (out > 20000f) out = 20000f
            if (out < -20000f) out = -20000f
            result[i] = out.toInt().toShort()
        }
        return result
    }

    /**
     * Megaphone: High saturation & distortion
     */
    private fun applyMegaphone(samples: ShortArray, sampleRate: Int): ShortArray {
        val radioFiltered = applyRadioFilter(samples, sampleRate)
        val result = ShortArray(radioFiltered.size)

        for (i in radioFiltered.indices) {
            val s = radioFiltered[i].toFloat() * 2.2f
            // Hard clipping
            val clipped = s.coerceIn(-24000f, 24000f)
            result[i] = clipped.toInt().toShort()
        }
        return result
    }

    /**
     * Ghost: Eerie pitch drop + cavernous reverb
     */
    private fun applyGhostEffect(samples: ShortArray, sampleRate: Int): ShortArray {
        val slowed = applyPitchAndSpeed(samples, 0.82f)
        return applyCaveReverb(slowed, sampleRate)
    }

    /**
     * Giọng Nữ Dẹo / Điệu Đà / Nũng Nịu:
     * - Tăng độ cao giọng lên chuẩn nữ ngọt ngào (~1.28x)
     * - Tăng dải tần sáng (treble boost / airy presence)
     * - LFO Vibrato/Chorus nhẹ (~3.2Hz) tạo độ rung ngân luyến láy, dẹo dẹo
     * - Thêm chút vang nhẹ ngọt ngào (subtle warm ambience)
     */
    private fun applyGirlyEffect(samples: ShortArray, sampleRate: Int): ShortArray {
        val pitched = applyPitchAndSpeed(samples, 1.28f)
        if (pitched.isEmpty()) return ShortArray(0)

        val result = ShortArray(pitched.size)
        val lfoFreq = 3.2
        val maxDelaySamples = (sampleRate * 0.0032).toInt() // 3.2ms delay
        val twoPiF = 2.0 * Math.PI * lfoFreq / sampleRate

        // High-pass filter for bright breathy air
        val cutoff = 3000f
        val rc = 1.0f / (2.0f * Math.PI.toFloat() * cutoff)
        val dt = 1.0f / sampleRate
        val alpha = dt / (rc + dt)
        var lp = 0f

        for (i in pitched.indices) {
            val s = pitched[i].toFloat()
            lp += alpha * (s - lp)
            val hp = s - lp
            val brightened = s + hp * 0.45f

            // LFO delay modulation for the "dẹo" swaying vibrato
            val lfo = (sin(twoPiF * i) * 0.5 + 0.5)
            val delayTap = (lfo * maxDelaySamples).toInt()
            val delayedIdx = (i - delayTap).coerceIn(0, pitched.size - 1)
            val delayedSample = pitched[delayedIdx].toFloat()

            val out = brightened * 0.72f + delayedSample * 0.42f
            result[i] = out.toInt().coerceIn(-32768, 32767).toShort()
        }

        // Subtle sweet room ambience
        return applyEcho(result, sampleRate, delayMs = 65, decay = 0.22f)
    }
}
