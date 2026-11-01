package com.traptheory.game

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View

class MainActivity : Activity() {
    private lateinit var settingsManager: SettingsManager
    private lateinit var audioManager: AudioManager
    private lateinit var menuView: MainMenuView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)
        audioManager = AudioManager(this, settingsManager.current())
        setContentView(R.layout.activity_main)
        menuView = findViewById(R.id.main_menu)
        menuView.onLaunch = { mode, level ->
            startActivity(Intent(this, GameActivity::class.java).apply {
                putExtra(GameActivity.EXTRA_MODE, mode.wireValue)
                putExtra(GameActivity.EXTRA_LEVEL, level)
            })
        }
        menuView.onSettingsChanged = {
            applyFullscreen()
            audioManager.updateVolumes(settingsManager.current())
        }
        applyFullscreen()
    }

    override fun onResume() {
        super.onResume()
        if (::audioManager.isInitialized) {
            audioManager.updateVolumes(settingsManager.current())
            audioManager.resume()
        }
    }

    override fun onPause() {
        if (::audioManager.isInitialized) audioManager.pause()
        super.onPause()
    }

    override fun onDestroy() {
        if (::audioManager.isInitialized) audioManager.release()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun applyFullscreen() {
        if (!::settingsManager.isInitialized) return
        val flags = if (settingsManager.current().fullscreen) {
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        } else {
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        window.decorView.systemUiVisibility = flags
    }
}