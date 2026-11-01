package com.traptheory.game

class Player(startX: Float, startY: Float) {
    val width = 32f
    val height = 44f
    var x = startX
    var y = startY
    var velocityX = 0f
    var velocityY = 0f
    var grounded = false
    var jumpedThisFrame = false
    var facing = 1
    var animationTime = 0f
    var invulnerableTime = 0f
    var alive = true

    fun reset(startX: Float, startY: Float) {
        x = startX
        y = startY
        velocityX = 0f
        velocityY = 0f
        grounded = false
        jumpedThisFrame = false
        alive = true
        invulnerableTime = 1f
    }
}