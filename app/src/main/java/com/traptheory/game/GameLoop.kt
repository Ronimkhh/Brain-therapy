package com.traptheory.game

import android.view.SurfaceHolder
import kotlin.math.min

class GameLoop(
    private val holder: SurfaceHolder,
    private val engine: GameEngine,
    private val renderer: GameRenderer,
    private val input: InputManager,
    private val targetFps: Int
) : Thread("TrapTheory-GameLoop") {
    @Volatile private var running = false
    private val fixedStep = 1.0 / 120.0
    private val frameNanos = 1_000_000_000L / targetFps.coerceIn(30, 120)

    fun startLoop() {
        if (isAlive) return
        running = true
        start()
    }

    fun requestStop() {
        running = false
        interrupt()
    }

    fun stopAndJoin() {
        requestStop()
        if (Thread.currentThread() !== this) {
            runCatching { join(900L) }
        }
    }

    override fun run() {
        var last = System.nanoTime()
        var accumulator = 0.0
        while (running) {
            val frameStart = System.nanoTime()
            val elapsed = min((frameStart - last) / 1_000_000_000.0, 0.12)
            last = frameStart
            accumulator += elapsed
            while (accumulator >= fixedStep) {
                engine.update(fixedStep.toFloat())
                accumulator -= fixedStep
            }

            var canvas: android.graphics.Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) renderer.render(canvas, engine, input, canvas.width, canvas.height)
            } catch (_: IllegalArgumentException) {
                running = false
            } catch (_: IllegalStateException) {
                running = false
            } finally {
                if (canvas != null) {
                    runCatching { holder.unlockCanvasAndPost(canvas) }
                }
            }

            val remaining = frameNanos - (System.nanoTime() - frameStart)
            if (remaining > 1_000_000L) {
                try {
                    val millis = remaining / 1_000_000L
                    val nanos = (remaining % 1_000_000L).toInt()
                    sleep(millis, nanos)
                } catch (_: InterruptedException) {
                    if (!running) break
                }
            } else if (remaining > 0L) {
                Thread.yield()
            }
        }
    }
}