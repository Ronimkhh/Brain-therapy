package com.traptheory.game

import android.app.Activity
import android.os.Bundle
import android.view.View

class GameActivity : Activity() {
    private lateinit var gameView: GameSurfaceView
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)
        setContentView(R.layout.activity_game)
        gameView = findViewById(R.id.game_surface)
        applyFullscreen()
    }

    override fun onResume() {
        super.onResume()
        if (::gameView.isInitialized) gameView.resumeGame()
    }

    override fun onPause() {
        if (::gameView.isInitialized) gameView.pauseGame()
        super.onPause()
    }

    override fun onDestroy() {
        if (::gameView.isInitialized) gameView.releaseGame()
        super.onDestroy()
    }

    @Deprecated("Use the system back dispatcher on newer Android versions")
    override fun onBackPressed() {
        if (!::gameView.isInitialized || !gameView.handleBack()) super.onBackPressed()
    }

    @Suppress("DEPRECATION")
    private fun applyFullscreen() {
        val flags = if (settingsManager.current().fullscreen) {
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        } else {
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        window.decorView.systemUiVisibility = flags
    }

    companion object {
        const val EXTRA_MODE = "game_mode"
        const val EXTRA_LEVEL = "level_id"
    }
}