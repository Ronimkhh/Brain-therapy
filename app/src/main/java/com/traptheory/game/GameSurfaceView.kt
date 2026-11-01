package com.traptheory.game

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    private val settingsManager = SettingsManager(context)
    private val inputManager = InputManager(settingsManager)
    private val soundManager = AudioManager(context, settingsManager.current())
    private val gameMode = GameMode.from((context as? Activity)?.intent?.getStringExtra(GameActivity.EXTRA_MODE))
    private val initialLevel = (context as? Activity)?.intent?.getIntExtra(GameActivity.EXTRA_LEVEL, 1) ?: 1
    val engine = GameEngine(context, gameMode, initialLevel, inputManager, soundManager) {
        (context as? Activity)?.runOnUiThread { (context as? Activity)?.finish() }
    }
    private val renderer = GameRenderer(engine.settings)
    @Volatile private var loop: GameLoop? = null
    @Volatile private var hostResumed = true

    init {
        holder.addCallback(this)
        isFocusable = true
        keepScreenOn = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startLoopIfReady()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    fun resumeGame() {
        hostResumed = true
        engine.onHostResume()
        startLoopIfReady()
    }

    fun pauseGame() {
        hostResumed = false
        engine.onHostPause()
        stopLoop()
    }

    fun releaseGame() {
        pauseGame()
        soundManager.release()
    }

    fun handleBack(): Boolean {
        if (engine.state == GameState.RUNNING || engine.customizingControls) {
            engine.pauseFromBack()
            return true
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val sx = width / InputManager.LOGICAL_WIDTH
        val sy = height / InputManager.LOGICAL_HEIGHT
        if (sx <= 0f || sy <= 0f) return true
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val logicalX = event.x / sx
            val logicalY = event.y / sy
            when (engine.state) {
                GameState.RUNNING -> if (engine.handleScreenTouch(logicalX, logicalY)) return true
                GameState.PAUSED -> {
                    if (!engine.customizingControls || (logicalY >= 515f && logicalX in 380f..620f)) {
                        return engine.handleScreenTouch(logicalX, logicalY)
                    }
                }
                GameState.COMPLETE -> return engine.handleScreenTouch(logicalX, logicalY)
                GameState.GAME_OVER -> return engine.handleScreenTouch(logicalX, logicalY)
                GameState.DYING -> Unit
            }
        }
        inputManager.onTouch(event, sx, sy)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun startLoopIfReady() {
        if (!hostResumed || !holder.surface.isValid) return
        val current = loop
        if (current?.isAlive == true) return
        val next = GameLoop(holder, engine, renderer, inputManager, engine.settings.targetFps)
        loop = next
        next.startLoop()
    }

    private fun stopLoop() {
        val current = loop ?: return
        loop = null
        current.stopAndJoin()
    }
}