package com.traptheory.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.PI
import kotlin.math.sin

class AudioManager(context: Context, initialSettings: GameSettings) {
    private val appContext = context.applicationContext
    private val vibrator = appContext.getSystemService(Vibrator::class.java)
    @Volatile private var settings = initialSettings
    private var musicTrack: AudioTrack? = null
    private val effects = HashMap<String, AudioTrack>()
    @Volatile private var released = false

    init {
        runCatching {
            musicTrack = makeTrack(buildMusic(), true)
            listOf("jump", "land", "trap", "fail", "death", "respawn", "checkpoint", "victory", "secret")
                .forEachIndexed { index, key -> effects[key] = makeTrack(buildEffect(index), false) }
            updateVolumes(initialSettings)
            musicTrack?.play()
        }
    }

    fun updateVolumes(value: GameSettings) {
        settings = value
        val musicLevel = (value.masterVolume * value.musicVolume).coerceIn(0f, 1f)
        runCatching { musicTrack?.setVolume(musicLevel) }
    }

    fun play(name: String) {
        if (released) return
        val track = effects[name] ?: effects["trap"] ?: return
        val volume = (settings.masterVolume * settings.sfxVolume).coerceIn(0f, 1f)
        if (volume <= 0f) return
        runCatching {
            track.setVolume(volume)
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.pause()
            track.playbackHeadPosition = 0
            track.play()
        }
    }

    fun pause() {
        runCatching { musicTrack?.pause() }
        effects.values.forEach { runCatching { it.pause() } }
    }

    fun resume() {
        if (released) return
        val volume = settings.masterVolume * settings.musicVolume
        if (volume > 0f) runCatching { musicTrack?.play() }
    }

    fun vibrate(milliseconds: Long = 28L) {
        if (!settings.vibration || released || vibrator?.hasVibrator() != true) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds)
            }
        }
    }

    fun release() {
        if (released) return
        released = true
        runCatching { musicTrack?.stop() }
        runCatching { musicTrack?.release() }
        musicTrack = null
        effects.values.forEach { track ->
            runCatching { track.stop() }
            runCatching { track.release() }
        }
        effects.clear()
    }

    private fun makeTrack(pcm: ShortArray, looping: Boolean): AudioTrack {
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(if (looping) AudioAttributes.CONTENT_TYPE_MUSIC else AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        if (looping) track.setLoopPoints(0, pcm.size, -1)
        return track
    }

    private fun buildMusic(): ShortArray {
        val length = SAMPLE_RATE * 5
        val notes = doubleArrayOf(196.0, 246.94, 293.66, 246.94, 220.0, 261.63, 329.63, 261.63)
        return ShortArray(length) { index ->
            val time = index.toDouble() / SAMPLE_RATE
            val beatLength = 0.34
            val beat = ((time / beatLength).toInt() % notes.size)
            val local = (time % beatLength) / beatLength
            val envelope = (1.0 - local * 0.72).coerceAtLeast(0.0)
            val melody = sin(2.0 * PI * notes[beat] * time) * 0.095 * envelope
            val bass = sin(2.0 * PI * (notes[beat] / 2.0) * time) * 0.045
            val tick = if (local < 0.025) sin(2.0 * PI * 880.0 * time) * 0.012 else 0.0
            ((melody + bass + tick).coerceIn(-0.22, 0.22) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun buildEffect(index: Int): ShortArray {
        val durations = intArrayOf(6500, 3600, 5200, 7500, 9000, 8200, 7000, 16000, 9500)
        val startFrequencies = doubleArrayOf(560.0, 300.0, 190.0, 180.0, 260.0, 380.0, 580.0, 392.0, 760.0)
        val endFrequencies = doubleArrayOf(820.0, 190.0, 105.0, 520.0, 80.0, 530.0, 860.0, 784.0, 1280.0)
        val count = durations[index]
        return ShortArray(count) { sample ->
            val progress = sample.toDouble() / count
            val fade = (1.0 - progress).coerceAtLeast(0.0)
            val frequency = startFrequencies[index] + (endFrequencies[index] - startFrequencies[index]) * progress
            val wave = sin(2.0 * PI * frequency * sample / SAMPLE_RATE) * fade * 0.18
            (wave * Short.MAX_VALUE).toInt().toShort()
        }
    }

    companion object {
        private const val SAMPLE_RATE = 22050
    }
}