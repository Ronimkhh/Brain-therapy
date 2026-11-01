package com.traptheory.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class MainMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private enum class Page { HOME, MODES, LEVELS, SETTINGS, CONTROLS }

    private val settingsManager = SettingsManager(context)
    private val saveManager = SaveManager(context)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG)
    private val softText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val artPath = Path()
    private val controlPositions = settingsManager.controls()
    private var page = Page.HOME
    private var selectedMode = GameMode.STORY
    private var selectedWorld = 0
    private var secretView = false
    private var selectedControl = ControlId.JUMP
    private var logicalHeight = 1800f
    private var touchDownY = 0f
    private var previousTouchY = 0f
    private var touchDownX = 0f
    private var hasDragged = false
    private var draggingControl = false
    private var settingsScroll = 0f
    private var animation = 0f
    private var resetArmed = false
    private var volumeSliderIndex = -1

    var onLaunch: ((GameMode, Int) -> Unit)? = null
    var onSettingsChanged: (() -> Unit)? = null

    init {
        isFocusable = true
        text.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        softText.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        text.color = Color.WHITE
        softText.color = Color.rgb(187, 196, 207)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = width / 1000f
        logicalHeight = if (scale > 0f) height / scale else 1800f
        animation += 0.035f
        canvas.save()
        canvas.scale(scale, scale)
        canvas.drawColor(Color.rgb(15, 19, 28))
        drawAtmosphere(canvas)
        when (page) {
            Page.HOME -> drawHome(canvas)
            Page.MODES -> drawModes(canvas)
            Page.LEVELS -> drawLevels(canvas)
            Page.SETTINGS -> drawSettings(canvas)
            Page.CONTROLS -> drawControls(canvas)
        }
        canvas.restore()
        postInvalidateDelayed(if (page == Page.HOME) 34L else 90L)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val scale = width / 1000f
        if (scale <= 0f) return true
        val x = event.x / scale
        val y = event.y / scale
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = x
                touchDownY = y
                previousTouchY = y
                hasDragged = false
                volumeSliderIndex = if (page == Page.SETTINGS) {
                    val index = ((y + settingsScroll - 204f) / 83f).toInt()
                    if (index in 8..10 && x >= 500f) index else -1
                } else -1
                draggingControl = page == Page.CONTROLS && beginControlDrag(x, y)
                if (volumeSliderIndex in 8..10) updateVolumeSlider(volumeSliderIndex, x)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (page == Page.CONTROLS && draggingControl) {
                    moveSelectedControl(x, y)
                    invalidate()
                    return true
                }
                if (page == Page.SETTINGS && volumeSliderIndex in 8..10) {
                    updateVolumeSlider(volumeSliderIndex, x)
                    onSettingsChanged?.invoke()
                    invalidate()
                    return true
                }
                if (page == Page.SETTINGS && abs(y - touchDownY) > 16f) {
                    settingsScroll = (settingsScroll + previousTouchY - y).coerceIn(0f, 360f)
                    hasDragged = true
                    invalidate()
                }
                previousTouchY = y
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (volumeSliderIndex in 8..10) {
                    volumeSliderIndex = -1
                    onSettingsChanged?.invoke()
                    invalidate()
                    return true
                }
                if (draggingControl) {
                    saveSelectedControl()
                    draggingControl = false
                    invalidate()
                    return true
                }
                if (!hasDragged && event.actionMasked == MotionEvent.ACTION_UP) dispatchTap(x, y)
                return true
            }
        }
        return true
    }

    private fun drawAtmosphere(canvas: Canvas) {
        fill.color = Color.rgb(19, 26, 38)
        canvas.drawRect(0f, 0f, 1000f, logicalHeight, fill)
        fill.color = Color.argb(42, 242, 184, 76)
        canvas.drawCircle(790f, logicalHeight * 0.20f, 330f, fill)
        fill.color = Color.argb(22, 93, 120, 197)
        canvas.drawCircle(110f, logicalHeight * 0.56f, 420f, fill)
        fill.color = Color.argb(30, 255, 255, 255)
        for (index in 0..28) {
            val x = (index * 137f + animation * (8f + index % 4)) % 1000f
            val y = 70f + (index * 79f % (logicalHeight - 120f).coerceAtLeast(1f))
            canvas.drawCircle(x, y, if (index % 4 == 0) 2.1f else 1.1f, fill)
        }
        fill.color = Color.argb(28, 4, 6, 10)
        canvas.drawRect(0f, logicalHeight - 160f, 1000f, logicalHeight, fill)
    }

    private fun drawHome(canvas: Canvas) {
        text.textAlign = Paint.Align.LEFT
        text.color = Color.rgb(245, 188, 79)
        text.textSize = 102f
        canvas.drawText("TRAP", 88f, 188f, text)
        canvas.drawText("THEORY", 88f, 284f, text)
        softText.textSize = 20f
        softText.color = Color.rgb(214, 221, 231)
        canvas.drawText(TextCatalog.get("nothing", settingsManager.current().language), 94f, 325f, softText)
        drawHeroScene(canvas)

        val base = logicalHeight - 500f
        drawActionButton(canvas, 112f, base, 888f, base + 88f, TextCatalog.get("play", settingsManager.current().language), true)
        text.textAlign = Paint.Align.CENTER
        text.textSize = 15f
        text.color = Color.rgb(221, 226, 236)
        canvas.drawText("${TextCatalog.get("continue", settingsManager.current().language)}  /  LEVEL ${saveManager.lastPlayedLevel.toString().padStart(2, '0')}", 500f, base + 119f, text)
        drawTextButton(canvas, 155f, base + 190f, TextCatalog.get("modes", settingsManager.current().language))
        drawTextButton(canvas, 500f, base + 190f, TextCatalog.get("levels", settingsManager.current().language))
        drawTextButton(canvas, 845f, base + 190f, TextCatalog.get("settings", settingsManager.current().language))
        text.color = Color.rgb(144, 154, 169)
        text.textSize = 13f
        canvas.drawText("45 LEVELS  /  5 WORLDS  /  0 SAFE ASSUMPTIONS", 500f, logicalHeight - 88f, text)
        text.textAlign = Paint.Align.LEFT
    }

    private fun drawHeroScene(canvas: Canvas) {
        val sceneTop = logicalHeight * 0.39f
        val floorY = sceneTop + 280f
        val accent = Color.rgb(241, 179, 72)
        fill.color = Color.argb(80, 6, 9, 15)
        canvas.drawRoundRect(78f, sceneTop + 30f, 922f, floorY + 50f, 28f, 28f, fill)
        stroke.color = Color.argb(85, 161, 181, 208)
        stroke.strokeWidth = 2f
        canvas.drawRoundRect(78f, sceneTop + 30f, 922f, floorY + 50f, 28f, 28f, stroke)

        fill.color = Color.rgb(41, 55, 72)
        canvas.drawRoundRect(184f, floorY, 822f, floorY + 20f, 8f, 8f, fill)
        fill.color = Color.argb(54, 241, 179, 72)
        canvas.drawRect(204f, floorY + 18f, 806f, floorY + 24f, fill)

        val doorLeft = 700f
        fill.color = Color.rgb(20, 28, 41)
        canvas.drawRoundRect(doorLeft, floorY - 190f, doorLeft + 80f, floorY, 12f, 12f, fill)
        stroke.color = Color.argb(220, 245, 188, 79)
        stroke.strokeWidth = 4f
        canvas.drawRoundRect(doorLeft, floorY - 190f, doorLeft + 80f, floorY, 12f, 12f, stroke)
        fill.color = accent
        canvas.drawCircle(doorLeft + 63f, floorY - 95f, 4f, fill)
        text.color = Color.rgb(255, 221, 151)
        text.textSize = 13f
        text.textAlign = Paint.Align.CENTER
        canvas.drawText("EXIT?", doorLeft + 40f, floorY - 205f, text)

        val playerX = 430f
        val playerY = floorY - 78f + sin(animation * 1.4f) * 4f
        fill.color = Color.rgb(244, 193, 105)
        canvas.drawRoundRect(playerX, playerY, playerX + 46f, floorY, 15f, 15f, fill)
        fill.color = Color.rgb(29, 35, 46)
        canvas.drawCircle(playerX + 31f, playerY + 24f, 3.4f, fill)
        stroke.color = Color.rgb(30, 35, 45)
        stroke.strokeWidth = 3f
        canvas.drawLine(playerX + 12f, floorY - 28f, playerX + 6f + sin(animation * 3f) * 8f, floorY - 4f, stroke)
        canvas.drawLine(playerX + 34f, floorY - 28f, playerX + 38f - sin(animation * 3f) * 8f, floorY - 4f, stroke)

        fill.color = Color.rgb(235, 93, 106)
        for (index in 0..4) {
            val x = 252f + index * 30f
            artPath.rewind()
            artPath.moveTo(x, floorY)
            artPath.lineTo(x + 14f, floorY - 28f)
            artPath.lineTo(x + 28f, floorY)
            artPath.close()
            canvas.drawPath(artPath, fill)
        }
        text.textAlign = Paint.Align.LEFT
    }

    private fun drawModes(canvas: Canvas) {
        drawHeader(canvas, TextCatalog.get("select_mode", settingsManager.current().language))
        val modes = listOf(
            GameMode.STORY to "A trick-filled 45-level campaign",
            GameMode.CHALLENGE to "One life. One attempt. No mercy.",
            GameMode.TIME_ATTACK to "Beat your best time",
            GameMode.ENDLESS to "Keep going. The traps will."
        )
        modes.forEachIndexed { index, item ->
            val top = 245f + index * 126f
            fill.color = if (index == 0) Color.rgb(242, 184, 76) else Color.rgb(29, 40, 56)
            canvas.drawRoundRect(100f, top, 900f, top + 98f, 18f, 18f, fill)
            stroke.color = if (index == 0) Color.rgb(255, 225, 167) else Color.rgb(75, 93, 118)
            canvas.drawRoundRect(100f, top, 900f, top + 98f, 18f, 18f, stroke)
            text.textAlign = Paint.Align.LEFT
            text.color = if (index == 0) Color.rgb(35, 33, 31) else Color.WHITE
            text.textSize = 20f
            canvas.drawText(TextCatalog.get(item.first.wireValue, settingsManager.current().language), 145f, top + 42f, text)
            softText.textAlign = Paint.Align.CENTER
            softText.textSize = 16f
            canvas.drawText(item.second, 500f, top + 68f, softText)
        }
        text.textAlign = Paint.Align.CENTER
        text.textSize = 14f
        text.color = Color.rgb(140, 153, 174)
        canvas.drawText("CHALLENGE MODE USES YOUR CURRENTLY UNLOCKED LEVELS", 500f, logicalHeight - 110f, text)
        text.textAlign = Paint.Align.LEFT
    }

    private fun drawLevels(canvas: Canvas) {
        val language = settingsManager.current().language
        drawHeader(canvas, if (secretView) TextCatalog.get("secret_rooms", language) else TextCatalog.get("select_level", language))
        if (!secretView) {
            val tabWidth = 177f
            GameWorlds.names.forEachIndexed { index, name ->
                val x = 32f + index * 187f
                fill.color = if (index == selectedWorld) Color.rgb(241, 179, 72) else Color.rgb(30, 40, 56)
                canvas.drawRoundRect(x, 188f, x + tabWidth, 246f, 12f, 12f, fill)
                text.color = if (index == selectedWorld) Color.rgb(27, 30, 37) else Color.rgb(224, 231, 240)
                text.textAlign = Paint.Align.CENTER
                text.textSize = 13f
                val abbreviated = when (index) {
                    0 -> "FAKE FACTORY"
                    1 -> "GRAVITY GONE"
                    2 -> "HAUNTED OFFICE"
                    3 -> "REALITY GLITCH"
                    else -> "IMPOSSIBLE MUSEUM"
                }
                canvas.drawText(abbreviated, x + tabWidth / 2f, 224f, text)
                stroke.color = if (index == selectedWorld) Color.TRANSPARENT else Color.rgb(63, 77, 98)
                canvas.drawRoundRect(x, 188f, x + tabWidth, 246f, 12f, 12f, stroke)
            }
            val first = selectedWorld * 9 + 1
            repeat(9) { localIndex ->
                val row = localIndex / 3
                val column = localIndex % 3
                val left = 74f + column * 286f
                val top = 300f + row * 147f
                val id = first + localIndex
                drawLevelButton(canvas, left, top, id, saveManager.isLevelUnlocked(id), saveManager.starsFor(id))
            }
        } else {
            val unlocked = saveManager.unlockedSecretLevels().sorted()
            if (unlocked.isEmpty()) {
                text.textAlign = Paint.Align.CENTER
                text.color = Color.rgb(226, 230, 238)
                text.textSize = 24f
                canvas.drawText(TextCatalog.get("find_secrets", language), 500f, 420f, text)
                softText.textAlign = Paint.Align.CENTER
                softText.textSize = 16f
                canvas.drawText("Look for hidden doors and strange signals in Story Mode.", 500f, 462f, softText)
                text.textAlign = Paint.Align.LEFT
            } else {
                unlocked.forEachIndexed { index, id ->
                    drawLevelButton(canvas, 130f, 300f + index * 145f, id, true, saveManager.starsFor(id), true)
                }
            }
        }
        val toggleLabel = if (secretView) "STORY LEVELS" else TextCatalog.get("secret_rooms", language)
        drawActionButton(canvas, 250f, logicalHeight - 205f, 750f, logicalHeight - 130f, toggleLabel, false)
        text.textAlign = Paint.Align.CENTER
        text.textSize = 13f
        text.color = Color.rgb(134, 147, 166)
        canvas.drawText("UNLOCKED THROUGH PLAY. LOCKS ARE NOT DECORATION.", 500f, logicalHeight - 68f, text)
        text.textAlign = Paint.Align.LEFT
    }

    private fun drawLevelButton(canvas: Canvas, left: Float, top: Float, levelId: Int, unlocked: Boolean, stars: Int, secret: Boolean = false) {
        val right = left + 245f
        val bottom = top + 112f
        fill.color = if (unlocked) Color.rgb(27, 37, 52) else Color.rgb(23, 28, 38)
        canvas.drawRoundRect(left, top, right, bottom, 16f, 16f, fill)
        stroke.color = if (unlocked) Color.rgb(93, 112, 139) else Color.rgb(47, 55, 69)
        stroke.strokeWidth = 1.5f
        canvas.drawRoundRect(left, top, right, bottom, 16f, 16f, stroke)
        text.color = if (unlocked) Color.rgb(246, 191, 87) else Color.rgb(105, 115, 132)
        text.textSize = 24f
        text.textAlign = Paint.Align.LEFT
        val indexLabel = if (secret) "S$levelId" else levelId.toString().padStart(2, '0')
        canvas.drawText(indexLabel, left + 18f, top + 40f, text)
        text.color = if (unlocked) Color.WHITE else Color.rgb(112, 120, 135)
        text.textSize = 13f
        val title = LevelManager.puzzleFor(levelId).title
        canvas.drawText(if (unlocked) title.take(19) else "LOCKED", left + 18f, top + 67f, text)
        softText.textAlign = Paint.Align.RIGHT
        softText.color = if (unlocked) Color.rgb(255, 211, 105) else Color.rgb(91, 99, 113)
        softText.textSize = 15f
        canvas.drawText(if (unlocked) "*".repeat(stars) + "-".repeat(3 - stars) else "LOCK", right - 16f, top + 40f, softText)
        softText.textAlign = Paint.Align.LEFT
    }

    private fun drawSettings(canvas: Canvas) {
        drawHeader(canvas, TextCatalog.get("settings", settingsManager.current().language))
        val value = settingsManager.current()
        val labels = listOf("quality", "fps", "particles", "shake", "lighting", "vibration", "fullscreen", "language", "master", "music", "sfx", "layout", "reset_settings", "reset_progress")
        labels.forEachIndexed { index, key ->
            val top = 204f + index * 83f - settingsScroll
            if (top < 140f || top > logicalHeight - 90f) return@forEachIndexed
            val bottom = top + 68f
            fill.color = Color.argb(105, 29, 40, 56)
            canvas.drawRoundRect(55f, top, 945f, bottom, 12f, 12f, fill)
            text.color = Color.rgb(230, 235, 244)
            text.textAlign = Paint.Align.LEFT
            text.textSize = 16f
            canvas.drawText(TextCatalog.get(key, value.language), 78f, top + 43f, text)
            when (index) {
                0 -> drawValue(canvas, qualityLabel(value.graphics), top)
                1 -> drawValue(canvas, "${value.targetFps}", top)
                2 -> drawValue(canvas, onOff(value.particles), top)
                3 -> drawValue(canvas, onOff(value.screenShake), top)
                4 -> drawValue(canvas, onOff(value.lighting), top)
                5 -> drawValue(canvas, onOff(value.vibration), top)
                6 -> drawValue(canvas, onOff(value.fullscreen), top)
                7 -> drawValue(canvas, if (value.language == "es") "ESPANOL" else "ENGLISH", top)
                8 -> drawSlider(canvas, 530f, top + 34f, 865f, value.masterVolume)
                9 -> drawSlider(canvas, 530f, top + 34f, 865f, value.musicVolume)
                10 -> drawSlider(canvas, 530f, top + 34f, 865f, value.sfxVolume)
                11 -> drawValue(canvas, "EDIT", top)
                12 -> drawValue(canvas, "RESET", top)
                13 -> drawValue(canvas, if (resetArmed) "TAP AGAIN" else "RESET", top)
            }
        }
        text.textAlign = Paint.Align.CENTER
        text.color = Color.rgb(132, 146, 166)
        text.textSize = 12f
        canvas.drawText("CHANGES SAVE AUTOMATICALLY ON THIS DEVICE", 500f, logicalHeight - 42f, text)
        text.textAlign = Paint.Align.LEFT
    }

    private fun drawControls(canvas: Canvas) {
        val language = settingsManager.current().language
        drawHeader(canvas, TextCatalog.get("move_controls", language))
        softText.textAlign = Paint.Align.CENTER
        softText.textSize = 15f
        canvas.drawText(TextCatalog.get("drag_hint", language), 500f, 158f, softText)

        val boardLeft = 70f
        val boardTop = 225f
        val boardWidth = 860f
        val boardHeight = 484f
        fill.color = Color.rgb(16, 24, 36)
        canvas.drawRoundRect(boardLeft, boardTop, boardLeft + boardWidth, boardTop + boardHeight, 22f, 22f, fill)
        stroke.color = Color.rgb(84, 105, 132)
        stroke.strokeWidth = 2f
        canvas.drawRoundRect(boardLeft, boardTop, boardLeft + boardWidth, boardTop + boardHeight, 22f, 22f, stroke)
        fill.color = Color.rgb(38, 52, 68)
        canvas.drawRoundRect(boardLeft + 20f, boardTop + 356f, boardLeft + boardWidth - 20f, boardTop + 378f, 8f, 8f, fill)
        fill.color = Color.argb(33, 242, 184, 76)
        canvas.drawCircle(boardLeft + boardWidth * 0.76f, boardTop + 110f, 105f, fill)
        text.textAlign = Paint.Align.LEFT
        text.color = Color.rgb(164, 178, 197)
        text.textSize = 12f
        canvas.drawText("LANDSCAPE GAME PREVIEW", boardLeft + 22f, boardTop + 30f, text)

        for (id in ControlId.entries) {
            val position = controlPositions[id.ordinal]
            val cx = boardLeft + position.x * boardWidth
            val cy = boardTop + position.y * boardHeight
            val radius = position.size * boardHeight
            fill.color = Color.argb((position.alpha * 210).toInt().coerceIn(35, 220), 242, 184, 76)
            canvas.drawCircle(cx, cy, radius, fill)
            stroke.color = if (id == selectedControl) Color.WHITE else Color.argb(180, 245, 219, 168)
            stroke.strokeWidth = if (id == selectedControl) 3f else 1.5f
            canvas.drawCircle(cx, cy, radius, stroke)
            text.textAlign = Paint.Align.CENTER
            text.color = Color.rgb(28, 32, 40)
            text.textSize = 13f
            canvas.drawText(controlSymbol(id), cx, cy + 5f, text)
        }

        val spec = controlPositions[selectedControl.ordinal]
        text.textAlign = Paint.Align.CENTER
        text.color = Color.rgb(247, 198, 103)
        text.textSize = 18f
        canvas.drawText("${selectedControl.title}  /  ${TextCatalog.get("size", language)} ${"%.2f".format(spec.size)}", 500f, 770f, text)
        drawSmallButton(canvas, 230f, 795f, 335f, 848f, "<")
        drawSmallButton(canvas, 665f, 795f, 770f, 848f, ">")
        drawSmallButton(canvas, 354f, 795f, 478f, 848f, "SIZE -")
        drawSmallButton(canvas, 522f, 795f, 646f, 848f, "SIZE +")

        text.color = Color.rgb(228, 233, 241)
        text.textSize = 14f
        canvas.drawText(TextCatalog.get("alpha", language), 105f, 906f, text)
        drawSlider(canvas, 410f, 900f, 900f, spec.alpha)
        text.color = Color.rgb(228, 233, 241)
        canvas.drawText(TextCatalog.get("sensitivity", language), 105f, 976f, text)
        drawSlider(canvas, 410f, 970f, 900f, ((settingsManager.current().controlSensitivity - 0.35f) / 1.15f).coerceIn(0f, 1f))

        val layoutKey = if (settingsManager.isLeftHandedLayout()) "layout_left" else "layout_classic"
        drawActionButton(canvas, 235f, 1030f, 765f, 1097f, TextCatalog.get(layoutKey, language), false)
        drawActionButton(canvas, 235f, 1122f, 765f, 1189f, TextCatalog.get("save_controls", language), true)
        drawActionButton(canvas, 235f, 1200f, 765f, 1267f, TextCatalog.get("reset_control_layout", language), false)
        softText.textAlign = Paint.Align.CENTER
        softText.textSize = 13f
        canvas.drawText("Drag any button above. Pinch with two fingers in game to resize individual buttons.", 500f, 1310f, softText)
        text.textAlign = Paint.Align.LEFT
        softText.textAlign = Paint.Align.LEFT
    }

    private fun drawHeroSceneDivider(canvas: Canvas, y: Float) {
        fill.color = Color.argb(110, 109, 128, 153)
        canvas.drawRect(125f, y, 875f, y + 1.5f, fill)
    }

    private fun drawHeader(canvas: Canvas, title: String) {
        text.color = Color.WHITE
        text.textSize = 29f
        text.textAlign = Paint.Align.CENTER
        canvas.drawText(title, 500f, 106f, text)
        drawSmallButton(canvas, 48f, 64f, 178f, 126f, "<  ${TextCatalog.get("back", settingsManager.current().language)}")
        drawHeroSceneDivider(canvas, 145f)
        text.textAlign = Paint.Align.LEFT
    }

    private fun drawActionButton(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, label: String, primary: Boolean) {
        fill.color = if (primary) Color.rgb(242, 184, 76) else Color.rgb(29, 40, 56)
        canvas.drawRoundRect(left, top, right, bottom, 18f, 18f, fill)
        stroke.color = if (primary) Color.rgb(255, 225, 167) else Color.rgb(75, 93, 118)
        stroke.strokeWidth = 1.5f
        canvas.drawRoundRect(left, top, right, bottom, 18f, 18f, stroke)
        text.textAlign = Paint.Align.CENTER
        text.color = if (primary) Color.rgb(35, 33, 31) else Color.rgb(235, 240, 247)
        text.textSize = 19f
        canvas.drawText(label, (left + right) / 2f, (top + bottom) / 2f + 7f, text)
        text.textAlign = Paint.Align.LEFT
    }

    private fun drawSmallButton(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, label: String) {
        fill.color = Color.rgb(30, 40, 56)
        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, fill)
        stroke.color = Color.rgb(78, 96, 121)
        stroke.strokeWidth = 1f
        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, stroke)
        text.color = Color.rgb(233, 239, 246)
        text.textAlign = Paint.Align.CENTER
        text.textSize = 14f
        canvas.drawText(label, (left + right) / 2f, (top + bottom) / 2f + 5f, text)
        text.textAlign = Paint.Align.LEFT
    }

    private fun drawTextButton(canvas: Canvas, centerX: Float, y: Float, label: String) {
        text.color = Color.rgb(224, 231, 240)
        text.textAlign = Paint.Align.CENTER
        text.textSize = 15f
        canvas.drawText(label, centerX, y, text)
    }

    private fun drawValue(canvas: Canvas, value: String, top: Float) {
        text.color = Color.rgb(246, 194, 91)
        text.textAlign = Paint.Align.RIGHT
        text.textSize = 15f
        canvas.drawText(value, 910f, top + 43f, text)
        text.textAlign = Paint.Align.LEFT
    }

    private fun drawSlider(canvas: Canvas, left: Float, y: Float, right: Float, value: Float) {
        fill.color = Color.rgb(62, 76, 98)
        canvas.drawRoundRect(left, y - 3f, right, y + 3f, 3f, 3f, fill)
        fill.color = Color.rgb(242, 184, 76)
        canvas.drawRoundRect(left, y - 3f, left + (right - left) * value.coerceIn(0f, 1f), y + 3f, 3f, 3f, fill)
        canvas.drawCircle(left + (right - left) * value.coerceIn(0f, 1f), y, 10f, fill)
    }

    private fun dispatchTap(x: Float, y: Float) {
        when (page) {
            Page.HOME -> {
                val base = logicalHeight - 500f
                if (inside(x, y, 112f, base, 888f, base + 88f)) launch(GameMode.STORY, saveManager.lastPlayedLevel)
                else if (y in (base + 145f)..(base + 235f)) {
                    when {
                        x < 350f -> page = Page.MODES
                        x < 700f -> {
                            selectedMode = GameMode.STORY
                            page = Page.LEVELS
                            secretView = false
                        }
                        else -> {
                            page = Page.SETTINGS
                            settingsScroll = 0f
                        }
                    }
                }
            }
            Page.MODES -> {
                if (y < 160f && x < 210f) page = Page.HOME
                else if (y in 220f..760f) {
                    val index = ((y - 245f) / 126f).toInt()
                    val modes = GameMode.entries
                    if (index in modes.indices) {
                        selectedMode = modes[index]
                        if (selectedMode == GameMode.ENDLESS) launch(GameMode.ENDLESS, 1)
                        else {
                            page = Page.LEVELS
                            secretView = false
                        }
                    }
                }
            }
            Page.LEVELS -> handleLevelTap(x, y)
            Page.SETTINGS -> handleSettingsTap(x, y)
            Page.CONTROLS -> handleControlsTap(x, y)
        }
        invalidate()
    }

    private fun handleLevelTap(x: Float, y: Float) {
        if (y < 145f && x < 210f) {
            page = if (selectedMode == GameMode.STORY) Page.HOME else Page.MODES
            return
        }
        if (inside(x, y, 250f, logicalHeight - 205f, 750f, logicalHeight - 130f)) {
            secretView = !secretView
            return
        }
        if (secretView) {
            val unlocked = saveManager.unlockedSecretLevels().sorted()
            unlocked.forEachIndexed { index, levelId ->
                if (inside(x, y, 130f, 300f + index * 145f, 375f, 412f + index * 145f)) launch(selectedMode, levelId)
            }
            return
        }
        if (y in 180f..255f) {
            val index = ((x - 32f) / 187f).toInt()
            if (index in GameWorlds.names.indices) selectedWorld = index
            return
        }
        repeat(9) { localIndex ->
            val row = localIndex / 3
            val column = localIndex % 3
            val left = 74f + column * 286f
            val top = 300f + row * 147f
            if (inside(x, y, left, top, left + 245f, top + 112f)) {
                val levelId = selectedWorld * 9 + localIndex + 1
                if (saveManager.isLevelUnlocked(levelId)) launch(selectedMode, levelId)
                else toast("Clear earlier levels to unlock this one.")
            }
        }
    }

    private fun handleSettingsTap(x: Float, y: Float) {
        if (y < 145f && x < 210f) {
            page = Page.HOME
            return
        }
        val adjustedY = y + settingsScroll
        val index = ((adjustedY - 204f) / 83f).toInt()
        if (index !in 0..13) return
        val current = settingsManager.current()
        when (index) {
            0 -> settingsManager.setGraphics(GraphicsQuality.entries[(current.graphics.ordinal + 1) % GraphicsQuality.entries.size])
            1 -> settingsManager.setTargetFps(when (current.targetFps) { 30 -> 60; 60 -> 120; else -> 30 })
            2 -> settingsManager.setParticles(!current.particles)
            3 -> settingsManager.setScreenShake(!current.screenShake)
            4 -> settingsManager.setLighting(!current.lighting)
            5 -> settingsManager.setVibration(!current.vibration)
            6 -> settingsManager.setFullscreen(!current.fullscreen)
            7 -> settingsManager.setLanguage(if (current.language == "en") "es" else "en")
            8, 9, 10 -> {
                val value = ((x - 530f) / 335f).coerceIn(0f, 1f)
                settingsManager.setVolume(when (index) { 8 -> "master"; 9 -> "music"; else -> "sfx" }, value)
            }
            11 -> page = Page.CONTROLS
            12 -> {
                settingsManager.resetSettings()
                settingsManager.resetControls()
                refreshControls()
                resetArmed = false
                toast("Settings reset to defaults.")
            }
            13 -> {
                if (resetArmed) {
                    saveManager.resetProgress()
                    resetArmed = false
                    toast("Game progress reset.")
                } else {
                    resetArmed = true
                    toast("Tap RESET PROGRESS again to confirm.")
                }
            }
        }
        onSettingsChanged?.invoke()
    }

    private fun updateVolumeSlider(index: Int, x: Float) {
        val key = when (index) { 8 -> "master"; 9 -> "music"; else -> "sfx" }
        settingsManager.setVolume(key, ((x - 530f) / 335f).coerceIn(0f, 1f))
    }

    private fun handleControlsTap(x: Float, y: Float) {
        if (y < 145f && x < 210f) {
            page = Page.SETTINGS
            return
        }
        if (y in 785f..858f) {
            when {
                x in 210f..345f -> selectedControl = ControlId.entries[(selectedControl.ordinal - 1 + ControlId.entries.size) % ControlId.entries.size]
                x in 650f..790f -> selectedControl = ControlId.entries[(selectedControl.ordinal + 1) % ControlId.entries.size]
                x in 345f..510f -> changeSelectedSize(-0.006f)
                x in 510f..665f -> changeSelectedSize(0.006f)
            }
        } else if (y in 875f..928f && x >= 390f) {
            controlPositions[selectedControl.ordinal].alpha = ((x - 410f) / 490f).coerceIn(0.15f, 0.9f)
            saveSelectedControl()
        } else if (y in 945f..1000f && x >= 390f) {
            settingsManager.setControlSensitivity(0.35f + ((x - 410f) / 490f).coerceIn(0f, 1f) * 1.15f)
        } else if (inside(x, y, 235f, 1030f, 765f, 1097f)) {
            settingsManager.setLeftHandedLayout(!settingsManager.isLeftHandedLayout())
            refreshControls()
        } else if (inside(x, y, 235f, 1122f, 765f, 1189f)) {
            ControlId.entries.forEach { id -> settingsManager.saveControl(id, controlPositions[id.ordinal]) }
            toast("Custom control positions saved.")
        } else if (inside(x, y, 235f, 1200f, 765f, 1267f)) {
            settingsManager.resetControls()
            settingsManager.setLeftHandedLayout(false)
            refreshControls()
            toast("Controls saved and reset to the classic layout.")
        } else if (inside(x, y, 70f, 225f, 930f, 709f)) {
            val id = hitControl(x, y)
            if (id != null) selectedControl = id
        }
        onSettingsChanged?.invoke()
    }

    private fun beginControlDrag(x: Float, y: Float): Boolean {
        if (!inside(x, y, 70f, 225f, 930f, 709f)) return false
        val id = hitControl(x, y) ?: return false
        selectedControl = id
        moveSelectedControl(x, y)
        return true
    }

    private fun moveSelectedControl(x: Float, y: Float) {
        val left = 70f
        val top = 225f
        val width = 860f
        val height = 484f
        val position = controlPositions[selectedControl.ordinal]
        position.x = ((x - left) / width).coerceIn(0.04f, 0.96f)
        position.y = ((y - top) / height).coerceIn(0.1f, 0.94f)
    }

    private fun hitControl(x: Float, y: Float): ControlId? {
        val left = 70f
        val top = 225f
        val width = 860f
        val height = 484f
        var best: ControlId? = null
        var closest = Float.MAX_VALUE
        for (id in ControlId.entries) {
            val point = controlPositions[id.ordinal]
            val distance = hypot(x - (left + point.x * width), y - (top + point.y * height))
            val hitRadius = min(80f, point.size * height * 1.5f)
            if (distance < hitRadius && distance < closest) {
                best = id
                closest = distance
            }
        }
        return best
    }

    private fun changeSelectedSize(delta: Float) {
        val position = controlPositions[selectedControl.ordinal]
        position.size = (position.size + delta).coerceIn(0.055f, 0.15f)
        saveSelectedControl()
    }

    private fun saveSelectedControl() {
        settingsManager.saveControl(selectedControl, controlPositions[selectedControl.ordinal])
    }

    private fun refreshControls() {
        val fresh = settingsManager.controls()
        fresh.forEachIndexed { index, position ->
            controlPositions[index].x = position.x
            controlPositions[index].y = position.y
            controlPositions[index].size = position.size
            controlPositions[index].alpha = position.alpha
        }
    }

    private fun launch(mode: GameMode, levelId: Int) {
        onLaunch?.invoke(mode, levelId)
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun inside(x: Float, y: Float, left: Float, top: Float, right: Float, bottom: Float): Boolean =
        x in left..right && y in top..bottom

    private fun qualityLabel(value: GraphicsQuality): String = when (value) {
        GraphicsQuality.LOW -> "LOW"
        GraphicsQuality.MEDIUM -> "MEDIUM"
        GraphicsQuality.HIGH -> "HIGH"
        GraphicsQuality.ULTRA -> "ULTRA"
    }

    private fun onOff(value: Boolean): String = if (value) "ON" else "OFF"

    private fun controlSymbol(id: ControlId): String = when (id) {
        ControlId.LEFT -> "<"
        ControlId.RIGHT -> ">"
        ControlId.JUMP -> "J"
        ControlId.ACTION -> "A"
        ControlId.PAUSE -> "||"
    }
}