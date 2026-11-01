package com.traptheory.game

import android.view.MotionEvent
import kotlin.math.hypot

class InputManager(private val settingsManager: SettingsManager) {
    val positions: Array<ControlPosition> = settingsManager.controls()
    private val pointerIds = IntArray(MAX_POINTERS) { -1 }
    private val pointerButtons = arrayOfNulls<ControlId>(MAX_POINTERS)
    private val pointerX = FloatArray(MAX_POINTERS)
    private val pointerY = FloatArray(MAX_POINTERS)
    private val lastPinchDistance = FloatArray(MAX_POINTERS)
    private val dirty = BooleanArray(ControlId.entries.size)

    @Volatile var leftDown = false
        private set
    @Volatile var rightDown = false
        private set
    @Volatile private var jumpPressed = false
    @Volatile private var actionPressed = false
    @Volatile var customizing = false

    fun onTouch(event: MotionEvent, scaleX: Float, scaleY: Float) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                beginPointer(event.getPointerId(index), event.getX(index) / scaleX, event.getY(index) / scaleY)
            }
            MotionEvent.ACTION_MOVE -> movePointers(event, scaleX, scaleY)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val index = event.actionIndex
                endPointer(event.getPointerId(index))
            }
            MotionEvent.ACTION_CANCEL -> clear()
        }
        refreshButtons()
    }

    fun consumeJumpPress(): Boolean {
        val value = jumpPressed
        jumpPressed = false
        return value
    }

    fun consumeActionPress(): Boolean {
        val value = actionPressed
        actionPressed = false
        return value
    }

    fun isDown(id: ControlId): Boolean {
        for (button in pointerButtons) if (button == id) return true
        return false
    }

    fun hitTest(id: ControlId, x: Float, y: Float): Boolean = nearButton(id, x, y, 1.08f)

    fun clear() {
        for (index in dirty.indices) {
            if (dirty[index]) {
                settingsManager.saveControl(ControlId.entries[index], positions[index])
                dirty[index] = false
            }
        }
        for (index in pointerIds.indices) {
            pointerIds[index] = -1
            pointerButtons[index] = null
        }
        leftDown = false
        rightDown = false
        jumpPressed = false
        actionPressed = false
    }

    private fun beginPointer(pointerId: Int, x: Float, y: Float) {
        val slot = findFreeSlot() ?: return
        pointerIds[slot] = pointerId
        pointerX[slot] = x
        pointerY[slot] = y
        var button = hitButton(x, y)
        if (customizing && button == null) {
            val activeButton = pointerButtons.firstOrNull { it != null }
            if (activeButton != null && nearButton(activeButton, x, y, 1.9f)) button = activeButton
        }
        pointerButtons[slot] = button
        if (!customizing && button == ControlId.JUMP) jumpPressed = true
        if (!customizing && button == ControlId.ACTION) actionPressed = true
        if (customizing && button != null) dirty[button.ordinal] = true
    }

    private fun movePointers(event: MotionEvent, scaleX: Float, scaleY: Float) {
        for (index in 0 until event.pointerCount) {
            val pointerId = event.getPointerId(index)
            val slot = findSlot(pointerId) ?: continue
            pointerX[slot] = event.getX(index) / scaleX
            pointerY[slot] = event.getY(index) / scaleY
        }
        if (!customizing) return

        for (button in ControlId.entries) {
            var first = -1
            var second = -1
            for (slot in pointerButtons.indices) {
                if (pointerButtons[slot] != button) continue
                if (first < 0) first = slot else if (second < 0) second = slot
            }
            if (first < 0) continue
            val position = positions[button.ordinal]
            if (second >= 0) {
                val distance = hypot(pointerX[first] - pointerX[second], pointerY[first] - pointerY[second])
                val previous = lastPinchDistance[first]
                if (previous > 0f) {
                    position.size = (position.size + (distance - previous) / 1200f).coerceIn(0.055f, 0.15f)
                    dirty[button.ordinal] = true
                }
                lastPinchDistance[first] = distance
                lastPinchDistance[second] = distance
            } else {
                position.x = (pointerX[first] / LOGICAL_WIDTH).coerceIn(0.04f, 0.96f)
                position.y = (pointerY[first] / LOGICAL_HEIGHT).coerceIn(0.10f, 0.94f)
                dirty[button.ordinal] = true
            }
        }
    }

    private fun endPointer(pointerId: Int) {
        val slot = findSlot(pointerId) ?: return
        val button = pointerButtons[slot]
        pointerIds[slot] = -1
        pointerButtons[slot] = null
        lastPinchDistance[slot] = 0f
        for (index in lastPinchDistance.indices) {
            if (pointerButtons[index] == button) lastPinchDistance[index] = 0f
        }
        if (button != null && pointerButtons.none { it == button } && dirty[button.ordinal]) {
            settingsManager.saveControl(button, positions[button.ordinal])
            dirty[button.ordinal] = false
        }
    }

    private fun hitButton(x: Float, y: Float): ControlId? {
        var best: ControlId? = null
        var bestDistance = Float.MAX_VALUE
        for (id in ControlId.entries) {
            val position = positions[id.ordinal]
            val centerX = position.x * LOGICAL_WIDTH
            val centerY = position.y * LOGICAL_HEIGHT
            val distance = hypot(x - centerX, y - centerY)
            val radius = position.size * LOGICAL_HEIGHT * 1.08f
            if (distance <= radius && distance < bestDistance) {
                best = id
                bestDistance = distance
            }
        }
        return best
    }

    private fun nearButton(id: ControlId, x: Float, y: Float, multiplier: Float): Boolean {
        val position = positions[id.ordinal]
        val distance = hypot(x - position.x * LOGICAL_WIDTH, y - position.y * LOGICAL_HEIGHT)
        return distance <= position.size * LOGICAL_HEIGHT * multiplier
    }

    private fun refreshButtons() {
        leftDown = isDown(ControlId.LEFT)
        rightDown = isDown(ControlId.RIGHT)
    }

    private fun findSlot(pointerId: Int): Int? {
        val index = pointerIds.indexOf(pointerId)
        return if (index < 0) null else index
    }

    private fun findFreeSlot(): Int? {
        val index = pointerIds.indexOf(-1)
        return if (index < 0) null else index
    }

    companion object {
        const val LOGICAL_WIDTH = 1000f
        const val LOGICAL_HEIGHT = 600f
        private const val MAX_POINTERS = 10
    }
}