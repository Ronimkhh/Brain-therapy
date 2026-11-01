package com.traptheory.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PhysicsEngine {
    fun update(
        player: Player,
        level: GameLevel,
        input: InputManager,
        deltaSeconds: Float,
        gravityDirection: Int,
        settings: GameSettings,
        externalForceX: Float,
        invertedControls: Boolean
    ) {
        player.jumpedThisFrame = false
        val horizontal = (if (input.leftDown) -1f else 0f) + (if (input.rightDown) 1f else 0f)
        val direction = if (invertedControls) -horizontal else horizontal
        val hasIce = level.puzzle.kind == TrapKind.ICE
        val speedMultiplier = if (level.puzzle.kind == TrapKind.SPEED_ZONE) 1.28f else 1f
        val maxSpeed = 330f * settings.controlSensitivity * speedMultiplier
        val acceleration = if (player.grounded) 1850f else 1320f

        if (direction != 0f) {
            player.velocityX += direction * acceleration * deltaSeconds
            player.facing = if (direction > 0f) 1 else -1
        } else {
            val friction = if (hasIce) 0.985f else if (player.grounded) 0.76f else 0.96f
            player.velocityX *= friction
            if (abs(player.velocityX) < 9f) player.velocityX = 0f
        }

        player.velocityX = (player.velocityX + externalForceX * deltaSeconds).coerceIn(-maxSpeed * 1.35f, maxSpeed * 1.35f)

        if (input.consumeJumpPress() && player.grounded) {
            player.velocityY = if (gravityDirection > 0) -555f else 555f
            player.grounded = false
            player.jumpedThisFrame = true
        }

        val previousX = player.x
        val previousY = player.y
        val previousBottom = previousY + player.height
        player.x = (player.x + player.velocityX * deltaSeconds).coerceIn(0f, max(0f, level.worldWidth - player.width))

        if (player.velocityX > 0f) {
            for (platform in level.platforms) {
                if (!platform.active) continue
                val overlapsY = player.y < platform.y + platform.height && player.y + player.height > platform.y
                val crossesRight = previousX + player.width <= platform.x && player.x + player.width > platform.x
                if (overlapsY && crossesRight) {
                    player.x = platform.x - player.width
                    player.velocityX = 0f
                }
            }
        } else if (player.velocityX < 0f) {
            for (platform in level.platforms) {
                if (!platform.active) continue
                val overlapsY = player.y < platform.y + platform.height && player.y + player.height > platform.y
                val crossesLeft = previousX >= platform.x + platform.width && player.x < platform.x + platform.width
                if (overlapsY && crossesLeft) {
                    player.x = platform.x + platform.width
                    player.velocityX = 0f
                }
            }
        }

        player.velocityY += 1450f * gravityDirection * deltaSeconds
        player.velocityY = player.velocityY.coerceIn(-1050f, 1050f)
        player.y += player.velocityY * deltaSeconds
        player.grounded = false

        for (platform in level.platforms) {
            if (!platform.active) continue
            val overlapsX = player.x + player.width > platform.x && player.x < platform.x + platform.width
            if (!overlapsX) continue

            if (gravityDirection > 0 && player.velocityY >= 0f) {
                val crossedTop = previousBottom <= platform.y + 12f && player.y + player.height >= platform.y
                if (crossedTop) {
                    player.y = platform.y - player.height
                    player.velocityY = if (platform.kind == PlatformKind.BOUNCE) -720f else 0f
                    player.grounded = platform.kind != PlatformKind.BOUNCE
                    platform.steppedOn = true
                }
            } else if (gravityDirection < 0 && player.velocityY <= 0f) {
                val oldTop = previousY
                val newTop = player.y
                val underside = platform.y + platform.height
                if (oldTop >= underside - 10f && newTop <= underside) {
                    player.y = underside
                    player.velocityY = if (platform.kind == PlatformKind.BOUNCE) 720f else 0f
                    player.grounded = platform.kind != PlatformKind.BOUNCE
                    platform.steppedOn = true
                }
            }
        }

        if (player.y < -180f || player.y > 820f) player.alive = false
        if (player.invulnerableTime > 0f) player.invulnerableTime = max(0f, player.invulnerableTime - deltaSeconds)
        player.animationTime += deltaSeconds * (if (abs(player.velocityX) > 12f) 9f else 3f)
    }
}