package com.traptheory.game

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.max

class SaveManager(context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences("trap_theory_progress", Context.MODE_PRIVATE)

    val highestUnlockedLevel: Int
        get() = preferences.getInt(KEY_UNLOCKED, 1).coerceIn(1, LevelManager.STORY_LEVELS)

    val lastPlayedLevel: Int
        get() = preferences.getInt(KEY_LAST_LEVEL, 1).coerceIn(1, LevelManager.STORY_LEVELS)

    val totalDeaths: Int
        get() = preferences.getInt(KEY_TOTAL_DEATHS, 0)

    fun isLevelUnlocked(levelId: Int): Boolean =
        levelId in 1..highestUnlockedLevel || levelId in unlockedSecretLevels()

    fun unlockedSecretLevels(): Set<Int> {
        val stored = preferences.getStringSet(KEY_SECRETS, emptySet()) ?: emptySet()
        return stored.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun unlockSecret(levelId: Int) {
        if (levelId !in LevelManager.SECRET_LEVELS) return
        val updated = unlockedSecretLevels().toMutableSet().apply { add(levelId) }.map(Int::toString).toSet()
        preferences.edit().putStringSet(KEY_SECRETS, updated).apply()
    }

    fun setLastPlayedLevel(levelId: Int) {
        preferences.edit().putInt(KEY_LAST_LEVEL, levelId.coerceIn(1, LevelManager.STORY_LEVELS)).apply()
    }

    fun starsFor(levelId: Int): Int = preferences.getInt(starKey(levelId), 0).coerceIn(0, 3)

    fun bestTimeMillis(levelId: Int): Long = preferences.getLong(timeKey(levelId), 0L)

    fun deathsFor(levelId: Int): Int = preferences.getInt(deathKey(levelId), 0)

    fun recordDeath(levelId: Int) {
        preferences.edit()
            .putInt(deathKey(levelId), deathsFor(levelId) + 1)
            .putInt(KEY_TOTAL_DEATHS, totalDeaths + 1)
            .apply()
    }

    fun markCompleted(levelId: Int, stars: Int, elapsedMillis: Long) {
        if (levelId !in 1..LevelManager.STORY_LEVELS && levelId !in LevelManager.SECRET_LEVELS) return
        val editor = preferences.edit()
        editor.putInt(starKey(levelId), max(starsFor(levelId), stars.coerceIn(1, 3)))
        val previousBest = bestTimeMillis(levelId)
        if (previousBest == 0L || elapsedMillis < previousBest) editor.putLong(timeKey(levelId), elapsedMillis)
        if (levelId in 1..LevelManager.STORY_LEVELS) {
            editor.putInt(KEY_UNLOCKED, max(highestUnlockedLevel, (levelId + 1).coerceAtMost(LevelManager.STORY_LEVELS)))
            editor.putInt(KEY_LAST_LEVEL, (levelId + 1).coerceAtMost(LevelManager.STORY_LEVELS))
        }
        editor.remove(checkpointKey(levelId))
        editor.apply()
    }

    fun saveCheckpoint(levelId: Int, x: Float, y: Float) {
        preferences.edit().putString(checkpointKey(levelId), "$x,$y").apply()
    }

    fun checkpoint(levelId: Int): Pair<Float, Float>? {
        val value = preferences.getString(checkpointKey(levelId), null) ?: return null
        val pieces = value.split(',')
        if (pieces.size != 2) return null
        val x = pieces[0].toFloatOrNull() ?: return null
        val y = pieces[1].toFloatOrNull() ?: return null
        return x to y
    }

    fun clearCheckpoint(levelId: Int) {
        preferences.edit().remove(checkpointKey(levelId)).apply()
    }

    fun resetProgress() {
        preferences.edit().clear().apply()
    }

    private fun starKey(level: Int) = "stars_$level"
    private fun timeKey(level: Int) = "best_time_$level"
    private fun deathKey(level: Int) = "deaths_$level"
    private fun checkpointKey(level: Int) = "checkpoint_$level"

    companion object {
        private const val KEY_UNLOCKED = "unlocked_level"
        private const val KEY_LAST_LEVEL = "last_level"
        private const val KEY_TOTAL_DEATHS = "total_deaths"
        private const val KEY_SECRETS = "unlocked_secrets"
    }
}