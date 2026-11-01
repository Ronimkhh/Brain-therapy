package com.traptheory.game

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.max
import kotlin.math.min

class SettingsManager(context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences("trap_theory_settings", Context.MODE_PRIVATE)

    fun current(): GameSettings {
        val quality = runCatching {
            GraphicsQuality.valueOf(preferences.getString(KEY_GRAPHICS, GraphicsQuality.HIGH.name)!!)
        }.getOrDefault(GraphicsQuality.HIGH)
        return GameSettings(
            graphics = quality,
            targetFps = preferences.getInt(KEY_FPS, 60),
            particles = preferences.getBoolean(KEY_PARTICLES, true),
            screenShake = preferences.getBoolean(KEY_SHAKE, true),
            lighting = preferences.getBoolean(KEY_LIGHTING, true),
            vibration = preferences.getBoolean(KEY_VIBRATION, true),
            fullscreen = preferences.getBoolean(KEY_FULLSCREEN, true),
            language = preferences.getString(KEY_LANGUAGE, "en") ?: "en",
            masterVolume = preferences.getFloat(KEY_MASTER, 0.8f),
            musicVolume = preferences.getFloat(KEY_MUSIC, 0.55f),
            sfxVolume = preferences.getFloat(KEY_SFX, 0.8f),
            controlSensitivity = preferences.getFloat(KEY_SENSITIVITY, 0.85f)
        )
    }

    fun setGraphics(value: GraphicsQuality) = preferences.edit().putString(KEY_GRAPHICS, value.name).apply()
    fun setTargetFps(value: Int) = preferences.edit().putInt(KEY_FPS, value.coerceIn(30, 120)).apply()
    fun setParticles(value: Boolean) = preferences.edit().putBoolean(KEY_PARTICLES, value).apply()
    fun setScreenShake(value: Boolean) = preferences.edit().putBoolean(KEY_SHAKE, value).apply()
    fun setLighting(value: Boolean) = preferences.edit().putBoolean(KEY_LIGHTING, value).apply()
    fun setVibration(value: Boolean) = preferences.edit().putBoolean(KEY_VIBRATION, value).apply()
    fun setFullscreen(value: Boolean) = preferences.edit().putBoolean(KEY_FULLSCREEN, value).apply()
    fun setLanguage(value: String) = preferences.edit().putString(KEY_LANGUAGE, if (value == "es") "es" else "en").apply()

    fun setVolume(key: String, value: Float) {
        val prefKey = when (key) {
            "master" -> KEY_MASTER
            "music" -> KEY_MUSIC
            else -> KEY_SFX
        }
        preferences.edit().putFloat(prefKey, value.coerceIn(0f, 1f)).apply()
    }

    fun setControlSensitivity(value: Float) =
        preferences.edit().putFloat(KEY_SENSITIVITY, value.coerceIn(0.35f, 1.5f)).apply()

    fun control(id: ControlId): ControlPosition {
        val defaults = defaultControl(id)
        return ControlPosition(
            preferences.getFloat("control_${id.name}_x", defaults.x),
            preferences.getFloat("control_${id.name}_y", defaults.y),
            preferences.getFloat("control_${id.name}_size", defaults.size),
            preferences.getFloat("control_${id.name}_alpha", defaults.alpha)
        )
    }

    fun controls(): Array<ControlPosition> = Array(ControlId.entries.size) { index ->
        control(ControlId.entries[index])
    }

    fun saveControl(id: ControlId, position: ControlPosition) {
        preferences.edit()
            .putFloat("control_${id.name}_x", position.x.coerceIn(0.04f, 0.96f))
            .putFloat("control_${id.name}_y", position.y.coerceIn(0.10f, 0.94f))
            .putFloat("control_${id.name}_size", position.size.coerceIn(0.055f, 0.15f))
            .putFloat("control_${id.name}_alpha", position.alpha.coerceIn(0.15f, 0.9f))
            .apply()
    }

    fun resetControls() {
        val editor = preferences.edit()
        ControlId.entries.forEach { id ->
            editor.remove("control_${id.name}_x")
                .remove("control_${id.name}_y")
                .remove("control_${id.name}_size")
                .remove("control_${id.name}_alpha")
        }
        editor.apply()
    }

    fun setLeftHandedLayout(leftHanded: Boolean) {
        ControlId.entries.forEach { id ->
            val position = defaultControl(id)
            if (leftHanded) {
                when (id) {
                    ControlId.LEFT -> position.x = 0.78f
                    ControlId.RIGHT -> position.x = 0.89f
                    ControlId.JUMP -> position.x = 0.12f
                    ControlId.ACTION -> position.x = 0.23f
                    ControlId.PAUSE -> Unit
                }
            }
            saveControl(id, position)
        }
        preferences.edit().putBoolean(KEY_LEFT_HANDED, leftHanded).apply()
    }

    fun isLeftHandedLayout(): Boolean = preferences.getBoolean(KEY_LEFT_HANDED, false)

    fun resetSettings() {
        preferences.edit().clear().apply()
    }

    private fun defaultControl(id: ControlId): ControlPosition = when (id) {
        ControlId.LEFT -> ControlPosition(0.105f, 0.80f, 0.077f, 0.48f)
        ControlId.RIGHT -> ControlPosition(0.215f, 0.80f, 0.077f, 0.48f)
        ControlId.JUMP -> ControlPosition(0.88f, 0.82f, 0.083f, 0.55f)
        ControlId.ACTION -> ControlPosition(0.765f, 0.82f, 0.070f, 0.42f)
        ControlId.PAUSE -> ControlPosition(0.95f, 0.13f, 0.064f, 0.65f)
    }

    companion object {
        private const val KEY_GRAPHICS = "graphics"
        private const val KEY_FPS = "target_fps"
        private const val KEY_PARTICLES = "particles"
        private const val KEY_SHAKE = "screen_shake"
        private const val KEY_LIGHTING = "lighting"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_FULLSCREEN = "fullscreen"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_MASTER = "master_volume"
        private const val KEY_MUSIC = "music_volume"
        private const val KEY_SFX = "sfx_volume"
        private const val KEY_SENSITIVITY = "control_sensitivity"
        private const val KEY_LEFT_HANDED = "left_handed"
    }
}