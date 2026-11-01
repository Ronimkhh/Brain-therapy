package com.traptheory.game

import kotlin.math.abs
import kotlin.math.sin

class TrapSystem {
    var gravityDirection = 1
        private set
    var windForce = 0f
        private set
    var teleporterCooldown = 0f
    var message: String? = null
        private set
    var messageTime = 0f
        private set
    var doorOpen = false
        private set
    var darkness = 0f
        private set
    private var elapsed = 0f
    private var doorTime = 0f
    private var lastGravityDirection = 1

    fun update(deltaSeconds: Float, level: GameLevel, player: Player, particles: ParticleSystem) {
        elapsed += deltaSeconds
        teleporterCooldown = (teleporterCooldown - deltaSeconds).coerceAtLeast(0f)
        messageTime = (messageTime - deltaSeconds).coerceAtLeast(0f)
        if (messageTime == 0f) message = null

        val kind = level.puzzle.kind
        windForce = if (kind == TrapKind.WIND) sin(elapsed * 1.7f) * 165f else 0f
        darkness = if (kind == TrapKind.DARKNESS) 0.64f + sin(elapsed * 1.2f) * 0.1f else 0f

        if (kind == TrapKind.GRAVITY || kind == TrapKind.GRAVITY_SCHEDULE) {
            val cycle = if (kind == TrapKind.GRAVITY) 5.2f else 3.8f
            gravityDirection = if ((elapsed / cycle).toInt() % 2 == 0) 1 else -1
            if (gravityDirection != lastGravityDirection) {
                say(if (gravityDirection < 0) "CEILING SHIFT" else "GRAVITY RESTORED", 1.15f)
                particles.emit(player.x + player.width / 2f, player.y + player.height / 2f, 0xFFBE7BFF.toInt(), 14)
                lastGravityDirection = gravityDirection
            }
        }

        for (platform in level.platforms) {
            if (platform.movementRange > 0f) {
                platform.x = platform.baseX + sin(elapsed * platform.movementSpeed + platform.baseX) * platform.movementRange
            }
            if (platform.kind == PlatformKind.INVISIBLE && !platform.revealed) {
                val centerX = platform.x + platform.width / 2f
                val centerY = platform.y + platform.height / 2f
                if (abs(player.x + player.width / 2f - centerX) < 155f && abs(player.y + player.height / 2f - centerY) < 210f) {
                    platform.revealed = true
                    say("A PLATFORM APPEARED", 1.4f)
                    particles.emit(centerX, centerY, 0xFF61E6D2.toInt(), 12)
                }
            }
            if (platform.kind == PlatformKind.TIMED) {
                platform.active = (elapsed % 4.2f) < 2.9f
            }
            if (platform.kind == PlatformKind.CRUMBLE) {
                if (platform.steppedOn && platform.active) {
                    platform.crumbleTime += deltaSeconds
                    if (platform.crumbleTime >= 0.62f) {
                        platform.active = false
                        platform.respawnTime = 2.1f
                        platform.crumbleTime = 0f
                        particles.emit(platform.x + platform.width / 2f, platform.y, 0xFFE5AD62.toInt(), 10)
                    }
                } else if (!platform.active) {
                    platform.respawnTime -= deltaSeconds
                    if (platform.respawnTime <= 0f) {
                        platform.active = true
                        platform.crumbleTime = 0f
                    }
                }
            }
            platform.steppedOn = false
        }

        for (hazard in level.hazards) {
            if (hazard.movementRange > 0f) {
                hazard.x = hazard.baseX + sin(elapsed * hazard.movementSpeed) * hazard.movementRange
            }
        }

        if (doorTime > 0f) {
            doorTime -= deltaSeconds
            if (doorTime <= 0f) {
                doorOpen = false
                level.hazards.forEach { it.active = true }
            }
        }
    }

    fun activateAction(level: GameLevel, player: Player, particles: ParticleSystem) {
        if (level.button?.overlaps(player.x - 42f, player.y - 22f, player.width + 84f, player.height + 44f) == true) {
            if (level.puzzle.kind == TrapKind.FALSE_BUTTON) {
                doorOpen = false
                windForce = if (player.x < level.worldWidth / 2f) 260f else -260f
                say("THE BUTTON WAS NOT HELPING", 1.8f)
                particles.emit(player.x + player.width / 2f, player.y, 0xFFFF6675.toInt(), 18)
            } else {
                doorOpen = true
                doorTime = 3.2f
                level.hazards.forEach { it.active = false }
                say("TEMPORARY CLEARANCE", 1.6f)
                particles.emit(player.x + player.width / 2f, player.y, 0xFF70E5B2.toInt(), 16)
            }
            return
        }

        when (level.puzzle.kind) {
            TrapKind.GRAVITY, TrapKind.GRAVITY_SCHEDULE -> {
                gravityDirection *= -1
                lastGravityDirection = gravityDirection
                say(if (gravityDirection < 0) "CEILING MODE" else "FLOOR MODE", 1.3f)
                particles.emit(player.x + player.width / 2f, player.y + player.height / 2f, 0xFFBE7BFF.toInt(), 14)
            }
            TrapKind.HIDDEN_ROUTE, TrapKind.SECRET_SIGNAL, TrapKind.FALSE_WALL -> {
                var revealedAny = false
                level.platforms.forEach {
                    if (it.kind == PlatformKind.INVISIBLE) {
                        it.revealed = true
                        revealedAny = true
                    }
                }
                say(if (revealedAny) "HIDDEN ROUTE REVEALED" else "THE WALL BLINKED", 1.5f)
                particles.emit(player.x + player.width / 2f, player.y, 0xFF61E6D2.toInt(), 14)
            }
            else -> say("NOTHING HERE. OR IS THERE?", 1.15f)
        }
    }

    fun teleport() {
        teleporterCooldown = 1.2f
        say("DOOR TRANSIT", 1f)
    }

    fun say(text: String, duration: Float) {
        message = text.uppercase()
        messageTime = duration
    }

    fun reset(level: GameLevel) {
        elapsed = 0f
        gravityDirection = 1
        lastGravityDirection = 1
        windForce = 0f
        teleporterCooldown = 0f
        message = null
        messageTime = 0f
        doorOpen = false
        doorTime = 0f
        darkness = 0f
        level.platforms.forEach {
            it.active = true
            it.crumbleTime = 0f
            it.respawnTime = 0f
            it.steppedOn = false
            if (it.kind == PlatformKind.INVISIBLE) it.revealed = false
        }
        level.hazards.forEach {
            it.active = true
            it.x = it.baseX
        }
    }
}