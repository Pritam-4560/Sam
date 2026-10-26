package com.example.samuraisprint.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundManager {
    private var isMuted = false
    private var isVibrationEnabled = true
    private val scope = CoroutineScope(Dispatchers.Default)
    private var vibrator: Vibrator? = null

    private val sampleRate = 44100
    private val soundBuffers = ConcurrentHashMap<SoundType, ShortArray>()

    enum class SoundType {
        SLASH,
        PARRY,
        COIN,
        JUMP,
        DRAGON_SPRINT,
        HURT,
        GAME_OVER
    }

    fun init(context: Context) {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {}

        // Pre-generate audio waveforms asynchronously
        scope.launch {
            soundBuffers[SoundType.SLASH] = generateSlashSound()
            soundBuffers[SoundType.PARRY] = generateParrySound()
            soundBuffers[SoundType.COIN] = generateCoinSound()
            soundBuffers[SoundType.JUMP] = generateJumpSound()
            soundBuffers[SoundType.DRAGON_SPRINT] = generateDragonSound()
            soundBuffers[SoundType.HURT] = generateHurtSound()
            soundBuffers[SoundType.GAME_OVER] = generateGameOverSound()
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun isMuted(): Boolean = isMuted

    fun setVibrationEnabled(enabled: Boolean) {
        isVibrationEnabled = enabled
    }

    fun isVibrationEnabled(): Boolean = isVibrationEnabled

    fun playSound(type: SoundType) {
        if (isMuted) return
        scope.launch {
            val buffer = soundBuffers[type] ?: when (type) {
                SoundType.SLASH -> generateSlashSound()
                SoundType.PARRY -> generateParrySound()
                SoundType.COIN -> generateCoinSound()
                SoundType.JUMP -> generateJumpSound()
                SoundType.DRAGON_SPRINT -> generateDragonSound()
                SoundType.HURT -> generateHurtSound()
                SoundType.GAME_OVER -> generateGameOverSound()
            }
            playPcm(buffer)
        }
    }

    fun vibrate(durationMs: Long = 40, intensity: Int = 120) {
        if (!isVibrationEnabled || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val clampedIntensity = intensity.coerceIn(1, 255)
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, clampedIntensity))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun playPcm(pcm: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(pcm, 0, pcm.size)
            audioTrack.play()
            // Track will release after playing
            audioTrack.setNotificationMarkerPosition(pcm.size)
            audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    track?.stop()
                    track?.release()
                }
                override fun onPeriodicNotification(track: AudioTrack?) {}
            })
        } catch (_: Exception) {}
    }

    // Audio synthesis generators
    private fun generateSlashSound(): ShortArray {
        val durationMs = 150
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        val random = java.util.Random(42)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            // Frequency sweeps from 900Hz down to 180Hz
            val freq = 900.0 * (1.0 - t * 0.8)
            phase += 2.0 * PI * freq / sampleRate
            val tone = sin(phase)
            val noise = (random.nextDouble() * 2.0 - 1.0) * 0.45
            val envelope = (1.0 - t) * (1.0 - t)
            val sample = ((tone * 0.55 + noise) * envelope * 0.7 * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateParrySound(): ShortArray {
        // Metallic clash bell
        val durationMs = 350
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        var phase1 = 0.0
        var phase2 = 0.0
        var phase3 = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase1 += 2.0 * PI * 1480.0 / sampleRate
            phase2 += 2.0 * PI * 2240.0 / sampleRate
            phase3 += 2.0 * PI * 3720.0 / sampleRate

            val envelope = exp(-t * 9.0)
            val sampleVal = (sin(phase1) * 0.5 + sin(phase2) * 0.35 + sin(phase3) * 0.15) * envelope
            val sample = (sampleVal * 0.85 * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateCoinSound(): ShortArray {
        val durationMs = 180
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val half = numSamples / 2
        var phase = 0.0

        for (i in 0 until numSamples) {
            val freq = if (i < half) 987.77 else 1318.51 // B5 to E6
            phase += 2.0 * PI * freq / sampleRate
            val env = 1.0 - (i.toDouble() / numSamples)
            val sampleVal = sin(phase) * env
            buffer[i] = (sampleVal * 0.6 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateJumpSound(): ShortArray {
        val durationMs = 120
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 180.0 + (progress * 420.0) // 180 to 600Hz sweep up
            phase += 2.0 * PI * freq / sampleRate
            val env = sin(progress * PI)
            val sampleVal = sin(phase) * env
            buffer[i] = (sampleVal * 0.5 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateDragonSound(): ShortArray {
        val durationMs = 600
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        var phase1 = 0.0
        var phase2 = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val prog = i.toDouble() / numSamples
            val freq1 = 220.0 + (prog * 330.0)
            val freq2 = 440.0 + (prog * 660.0)
            phase1 += 2.0 * PI * freq1 / sampleRate
            phase2 += 2.0 * PI * freq2 / sampleRate

            val env = exp(-t * 2.5) * (1.0 - prog)
            val sampleVal = (sin(phase1) * 0.6 + sin(phase2) * 0.4) * env
            buffer[i] = (sampleVal * 0.8 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateHurtSound(): ShortArray {
        val durationMs = 200
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val random = java.util.Random(1337)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val freq = 140.0 * (1.0 - t * 0.5)
            phase += 2.0 * PI * freq / sampleRate
            val tone = sin(phase)
            val noise = (random.nextDouble() * 2.0 - 1.0) * 0.6
            val env = (1.0 - t)
            buffer[i] = ((tone * 0.4 + noise) * env * 0.7 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateGameOverSound(): ShortArray {
        val durationMs = 800
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        var phase1 = 0.0
        var phase2 = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase1 += 2.0 * PI * 110.0 / sampleRate
            phase2 += 2.0 * PI * 164.8 / sampleRate
            val env = exp(-t * 2.2)
            val sampleVal = (sin(phase1) * 0.7 + sin(phase2) * 0.3) * env
            buffer[i] = (sampleVal * 0.85 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }
}
