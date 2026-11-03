package com.example.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

enum class AudioMode(val labelEn: String, val labelZh: String) {
    OFF("Off", "关闭"),
    SIMULATOR("Rhythm Beat (Auto)", "节奏律动 (自动)"),
    MICROPHONE("Microphone (Live)", "麦克风 (实时)")
}

class OrbAudioEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    @Volatile
    var currentMode: AudioMode = AudioMode.OFF
        private set

    @Volatile
    var bass: Float = 0f
        private set

    @Volatile
    var mid: Float = 0f
        private set

    @Volatile
    var treble: Float = 0f
        private set

    private var audioRecord: AudioRecord? = null

    fun setMode(mode: AudioMode) {
        if (currentMode == mode) return
        stop()
        currentMode = mode
        when (mode) {
            AudioMode.OFF -> {
                bass = 0f
                mid = 0f
                treble = 0f
            }
            AudioMode.SIMULATOR -> {
                startSimulator()
            }
            AudioMode.MICROPHONE -> {
                if (hasRecordAudioPermission()) {
                    startMicrophone()
                } else {
                    // Fall back to simulator if permission not granted
                    currentMode = AudioMode.SIMULATOR
                    startSimulator()
                }
            }
        }
    }

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startSimulator() {
        job = scope.launch {
            var simTime = 0.0
            while (isActive) {
                simTime += 0.03
                // Rhythmic synthetic beat generator:
                // Heavy punchy bass kick every ~1.2s + syncopated beat
                val kick = (sin(simTime * 4.2) * 0.5 + 0.5)
                val kickBump = if (kick > 0.75) ((kick - 0.75) / 0.25).toFloat() else 0.0f
                val ambientBass = (sin(simTime * 2.1) * 0.5 + 0.5).toFloat() * 0.3f
                val targetBass = (kickBump * 0.9f + ambientBass).coerceIn(0f, 1f)

                // Mid vocal modulation
                val targetMid = ((sin(simTime * 3.7 + 1.2) * 0.5 + 0.5) * 0.7f + (sin(simTime * 5.8) * 0.3f)).toFloat().coerceIn(0f, 1f)

                // High shimmer / treble sparkles
                val targetTreble = ((sin(simTime * 8.4) * 0.5 + 0.5) * (kickBump * 0.4f + 0.5f)).toFloat().coerceIn(0f, 1f)

                // Smooth interpolation
                bass = bass * 0.75f + targetBass * 0.25f
                mid = mid * 0.78f + targetMid * 0.22f
                treble = treble * 0.70f + targetTreble * 0.30f

                delay(16) // ~60 Hz update
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startMicrophone() {
        job = scope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufSize * 2).coerceAtLeast(2048)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("OrbAudioEngine", "AudioRecord initialization failed, falling back to simulator")
                    currentMode = AudioMode.SIMULATOR
                    startSimulator()
                    return@launch
                }

                audioRecord?.startRecording()
                val buffer = ShortArray(bufferSize)

                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Calculate frequency band energies using zero-crossing and block segmentation
                        var sumAmp = 0f
                        var zeroCrossings = 0
                        var prevSign = buffer[0] >= 0

                        for (i in 0 until read) {
                            val v = abs(buffer[i].toFloat())
                            sumAmp += v
                            val sign = buffer[i] >= 0
                            if (sign != prevSign) {
                                zeroCrossings++
                                prevSign = sign
                            }
                        }

                        val avgAmp = (sumAmp / read) / 32768.0f // 0.0 to 1.0
                        // Normalize zero crossing density to estimate dominant pitch band
                        val zcr = zeroCrossings.toFloat() / read

                        val amplifiedAmp = (avgAmp * 5.0f).coerceIn(0f, 1f)

                        // Low pitch dominance (bass)
                        val targetBass = if (zcr < 0.08f) amplifiedAmp * 1.5f else amplifiedAmp * 0.4f
                        // Mid pitch dominance (voice, 0.08 - 0.25)
                        val targetMid = if (zcr in 0.06f..0.28f) amplifiedAmp * 1.4f else amplifiedAmp * 0.6f
                        // High pitch dominance (treble, > 0.25)
                        val targetTreble = if (zcr > 0.20f) amplifiedAmp * 1.6f else amplifiedAmp * 0.3f

                        bass = (bass * 0.7f + targetBass.coerceIn(0f, 1f) * 0.3f)
                        mid = (mid * 0.75f + targetMid.coerceIn(0f, 1f) * 0.25f)
                        treble = (treble * 0.65f + targetTreble.coerceIn(0f, 1f) * 0.35f)
                    }
                    delay(20)
                }
            } catch (e: Exception) {
                Log.e("OrbAudioEngine", "Error reading audio", e)
                currentMode = AudioMode.SIMULATOR
                startSimulator()
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (ignored: Exception) {}
                audioRecord = null
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (ignored: Exception) {}
        audioRecord = null
        bass = 0f
        mid = 0f
        treble = 0f
    }
}
