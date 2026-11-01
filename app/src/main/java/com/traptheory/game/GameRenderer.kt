package com.traptheory.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.sin

class GameRenderer(private val settings: GameSettings) {
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val silhouettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val platformPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hazardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mutedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trianglePath = Path()
    private val skyGradients = arrayOfNulls<Shader>(5)
    private val glows = arrayOfNulls<Array<Shader>>(5)
    private val worldAccents = intArrayOf(
        Color.rgb(244, 177, 71), Color.rgb(180, 120, 255), Color.rgb(102, 224, 204),
        Color.rgb(255, 88, 178), Color.rgb(243, 202, 116)
    )
    private var cachedSecond = -1
    private var timerLabel = "00:00"
    private var cachedStars = -1
    private var starsLabel = "- - -"

    init {
        val top = intArrayOf(
            Color.rgb(15, 28, 42), Color.rgb(24, 15, 49), Color.rgb(17, 31, 43),
            Color.rgb(26, 12, 44), Color.rgb(37, 33, 39)
        )
        val bottom = intArrayOf(
            Color.rgb(34, 56, 69), Color.rgb(63, 32, 88), Color.rgb(38, 64, 64),
            Color.rgb(69, 24, 85), Color.rgb(82, 68, 57)
        )
        val accents = intArrayOf(
            Color.rgb(244, 177, 71), Color.rgb(180, 120, 255), Color.rgb(102, 224, 204),
            Color.rgb(255, 88, 178), Color.rgb(243, 202, 116)
        )
        for (index in 0..4) {
            skyGradients[index] = LinearGradient(0f, 0f, 0f, 600f, top[index], bottom[index], Shader.TileMode.CLAMP)
            glows[index] = arrayOf(
                RadialGradient(770f, 104f, 350f, accents[index], Color.TRANSPARENT, Shader.TileMode.CLAMP),
                RadialGradient(210f, 340f, 290f, accents[index], Color.TRANSPARENT, Shader.TileMode.CLAMP)
            )
        }
        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        mutedTextPaint.color = Color.rgb(195, 203, 216)
        mutedTextPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        shadowPaint.color = Color.argb(60, 2, 5, 10)
        edgePaint.style = Paint.Style.STROKE
        edgePaint.strokeWidth = 2f
        edgePaint.strokeCap = Paint.Cap.ROUND
        accentPaint.style = Paint.Style.STROKE
        accentPaint.strokeWidth = 3f
        accentPaint.strokeCap = Paint.Cap.ROUND
    }

    fun render(canvas: Canvas, engine: GameEngine, input: InputManager, width: Int, height: Int) {
        val sx = width / InputManager.LOGICAL_WIDTH
        val sy = height / InputManager.LOGICAL_HEIGHT
        canvas.save()
        canvas.scale(sx, sy)
        val world = engine.level.worldIndex.coerceIn(0, 4)
        backgroundPaint.shader = skyGradients[world]
        canvas.drawRect(0f, 0f, 1000f, 600f, backgroundPaint)
        drawBackground(canvas, engine, world)

        val shake = if (settings.screenShake && engine.screenShakeTime > 0f) {
            sin(engine.screenShakeTime * 52f) * engine.screenShakeTime * 17f
        } else 0f
        canvas.save()
        canvas.translate(-engine.cameraX + shake, 0f)
        drawWorldObjects(canvas, engine, world)
        drawParticles(canvas, engine.particles)
        drawPlayer(canvas, engine)
        canvas.restore()

        if (engine.traps.darkness > 0f && settings.lighting) {
            overlayPaint.color = Color.argb((engine.traps.darkness * 255f).toInt().coerceIn(0, 220), 5, 7, 15)
            canvas.drawRect(0f, 0f, 1000f, 600f, overlayPaint)
            glowPaint.shader = null
            glowPaint.color = Color.argb(38, 255, 218, 146)
            canvas.drawCircle(engine.player.x + engine.player.width / 2f - engine.cameraX, engine.player.y + 18f, 145f, glowPaint)
            glowPaint.shader = null
        }

        drawHud(canvas, engine)
        drawControls(canvas, input)
        if (engine.traps.messageTime > 0f && engine.traps.message != null) {
            drawBanner(canvas, engine.traps.message!!)
        }
        when (engine.state) {
            GameState.PAUSED -> drawPauseOverlay(canvas, engine)
            GameState.COMPLETE -> drawCompleteOverlay(canvas, engine)
            GameState.DYING -> drawDeathFlash(canvas, engine)
            GameState.GAME_OVER -> drawGameOverOverlay(canvas)
            GameState.RUNNING -> Unit
        }
        if (engine.screenFlashTime > 0f) {
            overlayPaint.color = Color.argb((engine.screenFlashTime * 160f).toInt().coerceIn(0, 90), 255, 235, 188)
            canvas.drawRect(0f, 0f, 1000f, 600f, overlayPaint)
        }
        canvas.restore()
    }

    private fun drawBackground(canvas: Canvas, engine: GameEngine, world: Int) {
        val accent = worldAccents[world]
        val phase = engine.elapsedSeconds * 18f
        if (settings.lighting && settings.graphics != GraphicsQuality.LOW) {
            glowPaint.shader = glows[world]?.get(0)
            canvas.drawCircle(770f + sin(phase * 0.02f) * 28f, 104f, 300f, glowPaint)
            glowPaint.shader = glows[world]?.get(1)
            canvas.drawCircle(210f, 340f + sin(phase * 0.015f) * 22f, 250f, glowPaint)
            glowPaint.shader = null
        }

        silhouettePaint.color = Color.argb(34, 4, 8, 16)
        for (index in 0..9) {
            val parallaxX = (index * 154f - engine.cameraX * 0.18f) % 1400f
            val x = if (parallaxX < -130f) parallaxX + 1400f else parallaxX - 140f
            val buildingHeight = 115f + (index % 4) * 31f
            canvas.drawRect(x, 470f - buildingHeight, x + 98f, 485f, silhouettePaint)
            if (index % 2 == 0) {
                silhouettePaint.color = Color.argb(60, Color.red(accent), Color.green(accent), Color.blue(accent))
                canvas.drawRect(x + 18f, 455f - buildingHeight, x + 22f, 461f - buildingHeight, silhouettePaint)
                canvas.drawRect(x + 54f, 455f - buildingHeight, x + 58f, 461f - buildingHeight, silhouettePaint)
                silhouettePaint.color = Color.argb(34, 4, 8, 16)
            }
        }

        if (settings.graphics != GraphicsQuality.LOW) {
            silhouettePaint.color = Color.argb(80, Color.red(accent), Color.green(accent), Color.blue(accent))
            for (index in 0..7) {
                val starX = (index * 173f + phase * (0.2f + (index % 3) * 0.06f)) % 1000f
                val starY = 68f + (index * 71f % 300f)
                canvas.drawCircle(starX, starY, if (index % 3 == 0) 2.4f else 1.4f, silhouettePaint)
            }
        }
    }

    private fun drawWorldObjects(canvas: Canvas, engine: GameEngine, world: Int) {
        val accent = worldAccent(world)
        val left = engine.cameraX - 90f
        val right = engine.cameraX + 1090f
        for (platform in engine.level.platforms) {
            if (!platform.active || platform.x + platform.width < left || platform.x > right) continue
            if (platform.kind == PlatformKind.INVISIBLE && !platform.revealed) continue
            shadowPaint.color = Color.argb(80, 3, 5, 11)
            canvas.drawRoundRect(platform.x, platform.y + 7f, platform.x + platform.width, platform.y + platform.height + 10f, 8f, 8f, shadowPaint)
            platformPaint.color = when (platform.kind) {
                PlatformKind.CRUMBLE -> Color.rgb(116, 82, 62)
                PlatformKind.TIMED -> if ((engine.elapsedSeconds * 3f).toInt() % 2 == 0) Color.rgb(98, 112, 127) else Color.rgb(59, 67, 83)
                PlatformKind.BOUNCE -> Color.rgb(62, 116, 116)
                else -> Color.rgb(60 + world * 4, 78 + world * 2, 89 + world * 3)
            }
            canvas.drawRoundRect(platform.x, platform.y, platform.x + platform.width, platform.y + platform.height, 8f, 8f, platformPaint)
            edgePaint.color = if (platform.kind == PlatformKind.INVISIBLE) Color.argb(150, Color.red(accent), Color.green(accent), Color.blue(accent)) else accent
            edgePaint.alpha = if (platform.kind == PlatformKind.INVISIBLE) 170 else 210
            canvas.drawLine(platform.x + 9f, platform.y + 2f, platform.x + platform.width - 9f, platform.y + 2f, edgePaint)
            if (platform.kind == PlatformKind.CRUMBLE && platform.crumbleTime > 0f) {
                edgePaint.color = Color.rgb(255, 120, 100)
                canvas.drawLine(platform.x + 30f, platform.y + 10f, platform.x + 42f, platform.y + 22f, edgePaint)
            }
        }

        for (hazard in engine.level.hazards) {
            if (!hazard.active || hazard.x + hazard.width < left || hazard.x > right) continue
            hazardPaint.color = Color.rgb(245, 89, 103)
            for (spike in 0..2) {
                val x = hazard.x + spike * (hazard.width / 3f)
                trianglePath.rewind()
                trianglePath.moveTo(x, hazard.y + hazard.height)
                trianglePath.lineTo(x + hazard.width / 6f, hazard.y)
                trianglePath.lineTo(x + hazard.width / 3f, hazard.y + hazard.height)
                trianglePath.close()
                canvas.drawPath(trianglePath, hazardPaint)
            }
        }

        drawCheckpoint(canvas, engine, accent)
        drawGoal(canvas, engine, accent)
        drawSpecials(canvas, engine, accent)
    }

    private fun drawCheckpoint(canvas: Canvas, engine: GameEngine, accent: Int) {
        val checkpoint = engine.level.checkpoint
        val active = engine.checkpointActive
        val glow = if (active) Color.rgb(112, 229, 178) else accent
        glowPaint.color = Color.argb(58, Color.red(glow), Color.green(glow), Color.blue(glow))
        canvas.drawCircle(checkpoint.left + 35f, checkpoint.top + 42f, 42f, glowPaint)
        platformPaint.color = Color.rgb(30, 40, 53)
        canvas.drawRoundRect(checkpoint.left + 20f, checkpoint.top + 5f, checkpoint.left + 50f, checkpoint.top + 58f, 8f, 8f, platformPaint)
        accentPaint.color = glow
        canvas.drawCircle(checkpoint.left + 35f, checkpoint.top + 22f, 8f, accentPaint)
        textPaint.textSize = 12f
        textPaint.color = Color.rgb(199, 212, 219)
        canvas.drawText(if (active) "SAVED" else "CHECKPOINT", checkpoint.left - 2f, checkpoint.top - 5f, textPaint)
    }

    private fun drawGoal(canvas: Canvas, engine: GameEngine, accent: Int) {
        val goal = engine.level.goal
        val fake = engine.level.puzzle.kind == TrapKind.FAKE_EXIT && !engine.hasTriggeredFakeExit
        val doorColor = if (fake) Color.rgb(241, 104, 99) else accent
        glowPaint.color = Color.argb(45, Color.red(doorColor), Color.green(doorColor), Color.blue(doorColor))
        canvas.drawCircle((goal.left + goal.right) / 2f, goal.top + 42f, 78f, glowPaint)
        platformPaint.color = Color.rgb(16, 23, 34)
        canvas.drawRoundRect(goal.left, goal.top, goal.right, goal.bottom, 12f, 12f, platformPaint)
        edgePaint.color = doorColor
        edgePaint.strokeWidth = 4f
        canvas.drawRoundRect(goal.left, goal.top, goal.right, goal.bottom, 12f, 12f, edgePaint)
        platformPaint.color = Color.argb(42, Color.red(doorColor), Color.green(doorColor), Color.blue(doorColor))
        canvas.drawRoundRect(goal.left + 8f, goal.top + 8f, goal.right - 8f, goal.bottom - 7f, 8f, 8f, platformPaint)
        textPaint.color = Color.WHITE
        textPaint.textSize = 12f
        canvas.drawText(if (fake) "EXIT?" else "EXIT", goal.left + 9f, goal.top - 9f, textPaint)
    }

    private fun drawSpecials(canvas: Canvas, engine: GameEngine, accent: Int) {
        val level = engine.level
        level.teleporterIn?.let { door ->
            glowPaint.color = Color.argb(50, 183, 129, 255)
            canvas.drawCircle(door.left + 25f, door.top + 54f, 52f, glowPaint)
            edgePaint.color = Color.rgb(190, 133, 255)
            edgePaint.strokeWidth = 3f
            canvas.drawOval(door.left, door.top, door.right, door.bottom, edgePaint)
        }
        level.button?.let { button ->
            val color = if (engine.traps.doorOpen) Color.rgb(112, 229, 178) else Color.rgb(244, 177, 71)
            platformPaint.color = Color.rgb(32, 39, 52)
            canvas.drawRoundRect(button.left + 8f, button.top + 15f, button.right - 8f, button.bottom - 5f, 9f, 9f, platformPaint)
            accentPaint.color = color
            accentPaint.strokeWidth = 3f
            canvas.drawCircle(button.left + 30f, button.top + 31f, 12f, accentPaint)
        }
        level.fakeCheckpoint?.let { fake ->
            edgePaint.color = Color.rgb(252, 102, 113)
            edgePaint.strokeWidth = 2f
            canvas.drawRoundRect(fake.left + 18f, fake.top + 10f, fake.left + 48f, fake.bottom - 2f, 6f, 6f, edgePaint)
            textPaint.textSize = 10f
            textPaint.color = Color.rgb(252, 128, 133)
            canvas.drawText("SAVE?", fake.left + 7f, fake.top, textPaint)
        }
        level.secret?.let { secret ->
            if (!engine.hasDiscoveredSecret) {
                accentPaint.color = Color.argb(150, Color.red(accent), Color.green(accent), Color.blue(accent))
                accentPaint.strokeWidth = 2f
                canvas.drawCircle((secret.left + secret.right) / 2f, (secret.top + secret.bottom) / 2f, 18f + sin(engine.elapsedSeconds * 3f) * 4f, accentPaint)
                textPaint.color = Color.argb(210, 255, 220, 130)
                textPaint.textSize = 11f
                canvas.drawText("?", secret.left + 49f, secret.top + 36f, textPaint)
            }
        }
        if (level.puzzle.kind == TrapKind.HIDDEN_ROUTE || level.puzzle.kind == TrapKind.SECRET_SIGNAL) {
            edgePaint.color = Color.argb(115, Color.red(accent), Color.green(accent), Color.blue(accent))
            edgePaint.strokeWidth = 2f
            canvas.drawLine(955f, 388f, 1020f, 350f, edgePaint)
        }
    }

    private fun drawPlayer(canvas: Canvas, engine: GameEngine) {
        val player = engine.player
        val bob = if (player.grounded) sin(player.animationTime) * 1.8f else 0f
        val x = player.x
        val y = player.y + bob
        val moving = abs(player.velocityX) > 22f && player.grounded
        val stride = if (moving) sin(player.animationTime * 0.9f) * 4f else 0f
        val alpha = if (player.invulnerableTime > 0f && (player.invulnerableTime * 13f).toInt() % 2 == 0) 110 else 255

        shadowPaint.color = Color.argb(78, 0, 0, 0)
        canvas.drawOval(x - 5f, y + player.height - 3f, x + player.width + 5f, y + player.height + 5f, shadowPaint)
        playerPaint.color = Color.argb(alpha, 248, 194, 97)
        canvas.drawRoundRect(x, y, x + player.width, y + player.height, 11f, 11f, playerPaint)
        playerPaint.color = Color.argb(alpha, 252, 219, 158)
        canvas.drawRoundRect(x + 5f, y + 5f, x + player.width - 4f, y + 20f, 7f, 7f, playerPaint)
        playerPaint.color = Color.argb(alpha, 24, 31, 42)
        val eyeX = if (player.facing >= 0) x + 21f else x + 9f
        canvas.drawCircle(eyeX, y + 13f, 2.5f, playerPaint)
        canvas.drawLine(x + 9f, y + 32f, x + 8f + stride, y + 42f, playerPaint)
        canvas.drawLine(x + 24f, y + 32f, x + 25f - stride, y + 42f, playerPaint)
        playerPaint.strokeWidth = 2.5f
        canvas.drawLine(x + 2f, y + 25f, x - 4f, y + 33f + stride, playerPaint)
        canvas.drawLine(x + player.width - 2f, y + 25f, x + player.width + 5f, y + 32f - stride, playerPaint)
    }

    private fun drawParticles(canvas: Canvas, system: ParticleSystem) {
        if (!settings.particles || settings.graphics == GraphicsQuality.LOW) return
        for (index in system.particles.indices) {
            if (settings.graphics == GraphicsQuality.MEDIUM && index % 2 != 0) continue
            val particle = system.particles[index]
            if (!particle.active) continue
            val alpha = ((particle.life / particle.maxLife) * 220f).toInt().coerceIn(0, 220)
            buttonPaint.color = (particle.color and 0x00FFFFFF) or (alpha shl 24)
            canvas.drawCircle(particle.x, particle.y, particle.size * (particle.life / particle.maxLife), buttonPaint)
        }
    }

    private fun drawHud(canvas: Canvas, engine: GameEngine) {
        val level = engine.level
        val second = engine.elapsedSeconds.toInt()
        if (second != cachedSecond) {
            cachedSecond = second
            timerLabel = "${(second / 60).toString().padStart(2, '0')}:${(second % 60).toString().padStart(2, '0')}"
        }
        textPaint.color = Color.WHITE
        textPaint.textSize = 20f
        canvas.drawText(level.worldName.uppercase(), 28f, 36f, textPaint)
        mutedTextPaint.textSize = 15f
        canvas.drawText(level.title, 28f, 60f, mutedTextPaint)
        textPaint.textSize = 20f
        canvas.drawText(timerLabel, 755f, 36f, textPaint)
        mutedTextPaint.textSize = 14f
        val runLabel = when {
            level.endless -> "RUN ${engine.endlessScore + 1}"
            engine.mode == GameMode.TIME_ATTACK -> "BEST ${formatMillis(engine.bestTimeMillis)}"
            else -> "${TextCatalog.get("deaths", settings.language)} ${engine.deathsThisRun}"
        }
        canvas.drawText(runLabel, 755f, 58f, mutedTextPaint)

        if (engine.traps.gravityDirection < 0) {
            textPaint.color = Color.rgb(193, 149, 255)
            textPaint.textSize = 13f
            canvas.drawText(TextCatalog.get("ceiling_mode", settings.language), 410f, 35f, textPaint)
        }
    }

    private fun drawControls(canvas: Canvas, input: InputManager) {
        for (id in ControlId.entries) {
            val position = input.positions[id.ordinal]
            val cx = position.x * 1000f
            val cy = position.y * 600f
            val radius = position.size * 600f
            val pressed = input.isDown(id)
            buttonPaint.color = if (pressed) Color.argb((position.alpha * 170f).toInt(), 255, 210, 128)
            else Color.argb((position.alpha * 130f).toInt(), 17, 24, 37)
            canvas.drawCircle(cx, cy, radius, buttonPaint)
            edgePaint.color = if (pressed) Color.argb(210, 255, 220, 151) else Color.argb((position.alpha * 210f).toInt(), 228, 235, 244)
            edgePaint.strokeWidth = if (pressed) 3f else 1.5f
            canvas.drawCircle(cx, cy, radius, edgePaint)
            textPaint.color = Color.argb((position.alpha * 255f).toInt().coerceIn(80, 255), 255, 255, 255)
            when (id) {
                ControlId.LEFT -> drawChevron(canvas, cx, cy, radius * 0.25f, -1f)
                ControlId.RIGHT -> drawChevron(canvas, cx, cy, radius * 0.25f, 1f)
                ControlId.JUMP -> {
                    textPaint.textSize = radius * 0.78f
                    canvas.drawText("J", cx - radius * 0.22f, cy + radius * 0.28f, textPaint)
                }
                ControlId.ACTION -> {
                    textPaint.textSize = radius * 0.76f
                    canvas.drawText("A", cx - radius * 0.29f, cy + radius * 0.27f, textPaint)
                }
                ControlId.PAUSE -> {
                    textPaint.textSize = radius * 0.82f
                    canvas.drawText("||", cx - radius * 0.28f, cy + radius * 0.30f, textPaint)
                }
            }
        }
    }

    private fun drawChevron(canvas: Canvas, cx: Float, cy: Float, size: Float, direction: Float) {
        trianglePath.rewind()
        if (direction < 0f) {
            trianglePath.moveTo(cx + size * 0.52f, cy - size)
            trianglePath.lineTo(cx - size * 0.48f, cy)
            trianglePath.lineTo(cx + size * 0.52f, cy + size)
        } else {
            trianglePath.moveTo(cx - size * 0.52f, cy - size)
            trianglePath.lineTo(cx + size * 0.48f, cy)
            trianglePath.lineTo(cx - size * 0.52f, cy + size)
        }
        trianglePath.close()
        buttonPaint.color = Color.WHITE
        canvas.drawPath(trianglePath, buttonPaint)
    }

    private fun drawBanner(canvas: Canvas, message: String) {
        val alpha = (engineMessageAlpha(message)).coerceIn(120, 230)
        overlayPaint.color = Color.argb(alpha, 12, 18, 30)
        canvas.drawRoundRect(270f, 76f, 730f, 121f, 15f, 15f, overlayPaint)
        textPaint.color = Color.rgb(255, 220, 149)
        textPaint.textSize = 17f
        textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(message, 500f, 105f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun engineMessageAlpha(message: String): Int = 205 + (message.length % 26)

    private fun drawPauseOverlay(canvas: Canvas, engine: GameEngine) {
        overlayPaint.color = Color.argb(196, 8, 12, 22)
        canvas.drawRect(0f, 0f, 1000f, 600f, overlayPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.WHITE
        textPaint.textSize = 36f
        canvas.drawText(TextCatalog.get(if (engine.customizingControls) "move_controls" else "paused", settings.language), 500f, 115f, textPaint)
        mutedTextPaint.textAlign = Paint.Align.CENTER
        mutedTextPaint.textSize = 16f
        canvas.drawText(if (engine.customizingControls) "Drag a button to move it. Pinch two fingers to resize." else engine.level.puzzle.hint, 500f, 148f, mutedTextPaint)
        if (engine.customizingControls) {
            drawOverlayButton(canvas, 400f, 510f, 600f, 558f, TextCatalog.get("done", settings.language), true)
        } else {
            drawOverlayButton(canvas, 340f, 205f, 660f, 276f, TextCatalog.get("resume", settings.language), true)
            drawOverlayButton(canvas, 340f, 291f, 660f, 356f, TextCatalog.get("restart_level", settings.language), false)
            drawOverlayButton(canvas, 340f, 371f, 660f, 436f, TextCatalog.get("customize_controls", settings.language), false)
            drawOverlayButton(canvas, 340f, 451f, 660f, 516f, TextCatalog.get("main_menu", settings.language), false)
        }
        textPaint.textAlign = Paint.Align.LEFT
        mutedTextPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawCompleteOverlay(canvas: Canvas, engine: GameEngine) {
        overlayPaint.color = Color.argb(205, 8, 12, 22)
        canvas.drawRect(0f, 0f, 1000f, 600f, overlayPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.rgb(255, 222, 149)
        textPaint.textSize = 38f
        canvas.drawText(TextCatalog.get("level_cleared", settings.language), 500f, 112f, textPaint)
        textPaint.textSize = 34f
        textPaint.color = Color.rgb(255, 211, 105)
        if (cachedStars != engine.completedStars) {
            cachedStars = engine.completedStars
            starsLabel = when (engine.completedStars) {
                3 -> "* * *"
                2 -> "* * -"
                1 -> "* - -"
                else -> "- - -"
            }
        }
        canvas.drawText(starsLabel, 500f, 165f, textPaint)
        mutedTextPaint.textAlign = Paint.Align.CENTER
        mutedTextPaint.textSize = 17f
        canvas.drawText("$timerLabel    /    ${engine.deathsThisRun} ${TextCatalog.get("fails", settings.language)}", 500f, 194f, mutedTextPaint)
        drawOverlayButton(canvas, 340f, 304f, 660f, 370f, TextCatalog.get(if (engine.level.endless) "next_run" else "next_level", settings.language), true)
        drawOverlayButton(canvas, 340f, 384f, 660f, 446f, TextCatalog.get("play_again", settings.language), false)
        drawOverlayButton(canvas, 340f, 460f, 660f, 520f, TextCatalog.get("main_menu", settings.language), false)
        textPaint.textAlign = Paint.Align.LEFT
        mutedTextPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawDeathFlash(canvas: Canvas, engine: GameEngine) {
        overlayPaint.color = Color.argb((engine.screenFlashTime * 255f).toInt().coerceIn(0, 90), 244, 78, 92)
        canvas.drawRect(0f, 0f, 1000f, 600f, overlayPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 30f
        textPaint.color = Color.argb(220, 255, 238, 232)
        canvas.drawText("NICE TRY", 500f, 292f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawGameOverOverlay(canvas: Canvas) {
        overlayPaint.color = Color.argb(212, 8, 12, 22)
        canvas.drawRect(0f, 0f, 1000f, 600f, overlayPaint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.rgb(255, 123, 126)
        textPaint.textSize = 39f
        canvas.drawText(TextCatalog.get("run_over", settings.language), 500f, 142f, textPaint)
        mutedTextPaint.textAlign = Paint.Align.CENTER
        mutedTextPaint.textSize = 17f
        canvas.drawText(TextCatalog.get("challenge_life", settings.language), 500f, 178f, mutedTextPaint)
        drawOverlayButton(canvas, 340f, 330f, 660f, 395f, TextCatalog.get("try_again", settings.language), true)
        drawOverlayButton(canvas, 340f, 420f, 660f, 485f, TextCatalog.get("main_menu", settings.language), false)
        textPaint.textAlign = Paint.Align.LEFT
        mutedTextPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawOverlayButton(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, label: String, primary: Boolean) {
        buttonPaint.color = if (primary) Color.rgb(242, 184, 76) else Color.rgb(33, 42, 57)
        canvas.drawRoundRect(left, top, right, bottom, 14f, 14f, buttonPaint)
        edgePaint.color = if (primary) Color.rgb(255, 224, 160) else Color.rgb(79, 95, 117)
        edgePaint.strokeWidth = 1.5f
        canvas.drawRoundRect(left, top, right, bottom, 14f, 14f, edgePaint)
        textPaint.color = if (primary) Color.rgb(31, 30, 33) else Color.WHITE
        textPaint.textSize = 18f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, (left + right) / 2f, (top + bottom) / 2f + 6f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun formatMillis(milliseconds: Long): String {
        if (milliseconds <= 0L) return "--:--"
        val seconds = milliseconds / 1000L
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }

    private fun worldAccent(index: Int): Int = worldAccents[index.coerceIn(0, worldAccents.lastIndex)]
}