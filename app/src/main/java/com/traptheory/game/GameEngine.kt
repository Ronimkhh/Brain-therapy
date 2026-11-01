package com.traptheory.game

import android.content.Context
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class GameEngine(
    context: Context,
    val mode: GameMode,
    initialLevelId: Int,
    val input: InputManager,
    val audio: AudioManager,
    private val onExit: () -> Unit
) {
    private val settingsManager = SettingsManager(context)
    private val saveManager = SaveManager(context)
    val settings: GameSettings = settingsManager.current()
    private val physics = PhysicsEngine()
    private val collisions = CollisionSystem()
    val particles = ParticleSystem()
    val traps = TrapSystem()
    var level: GameLevel = if (mode == GameMode.ENDLESS) LevelManager.createEndless(0) else LevelManager.create(initialLevelId)
        private set
    val player = Player(level.startX, level.startY)
    @Volatile var state: GameState = GameState.RUNNING
        private set
    var elapsedSeconds = 0f
        private set
    var deathsThisRun = 0
        private set
    var cameraX = 0f
        private set
    var screenShakeTime = 0f
        private set
    var screenFlashTime = 0f
        private set
    var completedStars = 0
        private set
    val bestTimeMillis: Long
        get() = if (level.id > 0) saveManager.bestTimeMillis(level.id) else 0L
    var endlessScore = 0
        private set
    var customizingControls = false
        private set
    val checkpointActive: Boolean get() = checkpointActivated
    val hasDiscoveredSecret: Boolean get() = secretDiscovered
    val hasTriggeredFakeExit: Boolean get() = fakeExitTriggered
    private var deathTimer = 0f
    private var checkpointX = level.startX
    private var checkpointY = level.startY
    private var checkpointActivated = false
    private var fakeCheckpointTriggered = false
    private var secretDiscovered = false
    private var fakeExitTriggered = false
    private var jumpSoundCooldown = 0f

    init {
        applySavedCheckpoint()
        player.reset(checkpointX, checkpointY)
        if (level.id > 0) saveManager.setLastPlayedLevel(level.id.coerceAtMost(LevelManager.STORY_LEVELS))
    }

    fun update(deltaSeconds: Float) {
        val dt = deltaSeconds.coerceIn(0f, 0.04f)
        if (state == GameState.RUNNING) {
            elapsedSeconds += dt
            jumpSoundCooldown = (jumpSoundCooldown - dt).coerceAtLeast(0f)
            screenShakeTime = (screenShakeTime - dt).coerceAtLeast(0f)
            screenFlashTime = (screenFlashTime - dt).coerceAtLeast(0f)

            traps.update(dt, level, player, particles)
            if (input.consumeActionPress()) {
                traps.activateAction(level, player, particles)
                audio.play("trap")
            }

            val wasGrounded = player.grounded
            physics.update(
                player = player,
                level = level,
                input = input,
                deltaSeconds = dt,
                gravityDirection = traps.gravityDirection,
                settings = settings,
                externalForceX = traps.windForce,
                invertedControls = level.puzzle.kind == TrapKind.MIRROR || level.puzzle.kind == TrapKind.INVERTED_CONTROLS
            )
            if (!wasGrounded && player.grounded) audio.play("land")
            if (player.jumpedThisFrame && jumpSoundCooldown <= 0f) {
                jumpSoundCooldown = 0.35f
                audio.play("jump")
            }

            when (collisions.detect(player, level, traps.teleporterCooldown)) {
                CollisionEvent.NONE -> Unit
                CollisionEvent.HAZARD -> beginDeath()
                CollisionEvent.FAKE_CHECKPOINT -> if (!fakeCheckpointTriggered) {
                    fakeCheckpointTriggered = true
                    traps.say("FAKE CHECKPOINT. OF COURSE.", 1.8f)
                    beginDeath()
                }
                CollisionEvent.CHECKPOINT -> activateCheckpoint()
                CollisionEvent.TELEPORT -> teleportPlayer()
                CollisionEvent.SECRET -> discoverSecret()
                CollisionEvent.GOAL -> reachGoal()
            }

            if (!player.alive && state == GameState.RUNNING) beginDeath()
            updateCamera(dt)
        } else if (state == GameState.DYING) {
            deathTimer += dt
            screenShakeTime = (screenShakeTime - dt).coerceAtLeast(0f)
            if (deathTimer >= 0.72f) {
                if (mode == GameMode.CHALLENGE) state = GameState.GAME_OVER else respawn()
            }
        } else if (state == GameState.COMPLETE) {
            screenFlashTime = (screenFlashTime - dt).coerceAtLeast(0f)
        }
        particles.update(dt)
    }

    fun handleScreenTouch(x: Float, y: Float): Boolean {
        when (state) {
            GameState.RUNNING -> {
                if (input.hitTest(ControlId.PAUSE, x, y)) {
                    state = GameState.PAUSED
                    input.clear()
                    audio.pause()
                    return true
                }
                return false
            }
            GameState.PAUSED -> {
                if (customizingControls) {
                    if (y >= 515f && x in 380f..620f) {
                        customizingControls = false
                        input.customizing = false
                    }
                    return true
                }
                when {
                    y in 205f..282f -> resume()
                    y in 292f..362f -> restart()
                    y in 372f..442f -> {
                        customizingControls = true
                        input.customizing = true
                    }
                    y in 452f..530f -> onExit()
                }
                return true
            }
            GameState.COMPLETE -> {
                when {
                    y in 300f..370f -> nextLevel()
                    y in 380f..445f -> restart()
                    y in 455f..530f -> onExit()
                }
                return true
            }
            GameState.GAME_OVER -> {
                when {
                    y in 330f..410f -> restart()
                    y in 420f..510f -> onExit()
                }
                return true
            }
            GameState.DYING -> return true
        }
    }

    fun pauseFromBack() {
        if (state == GameState.RUNNING) {
            state = GameState.PAUSED
            input.clear()
            audio.pause()
        } else if (customizingControls) {
            customizingControls = false
            input.customizing = false
        }
    }

    fun resume() {
        if (state == GameState.PAUSED) {
            state = GameState.RUNNING
            customizingControls = false
            input.customizing = false
            audio.resume()
        }
    }

    fun onHostResume() {
        if (state != GameState.PAUSED) audio.resume()
    }

    fun onHostPause() {
        if (state == GameState.RUNNING) state = GameState.PAUSED
        input.clear()
        audio.pause()
    }

    private fun beginDeath() {
        if (state != GameState.RUNNING || player.invulnerableTime > 0f) return
        state = GameState.DYING
        deathTimer = 0f
        deathsThisRun++
        player.alive = false
        screenShakeTime = if (settings.screenShake) 0.48f else 0f
        screenFlashTime = 0.22f
        particles.emit(player.x + player.width / 2f, player.y + player.height / 2f, 0xFFFF6D78.toInt(), 25, 240f)
        if (level.id > 0) saveManager.recordDeath(level.id)
        audio.play("death")
        audio.vibrate(42L)
    }

    private fun respawn() {
        traps.reset(level)
        player.reset(checkpointX, checkpointY)
        state = GameState.RUNNING
        deathTimer = 0f
        particles.emit(player.x + player.width / 2f, player.y + player.height / 2f, 0xFF61E6D2.toInt(), 12)
        audio.play("respawn")
        audio.resume()
        updateCamera(1f)
    }

    private fun activateCheckpoint() {
        if (checkpointActivated) return
        checkpointActivated = true
        checkpointX = level.checkpoint.left + 12f
        checkpointY = level.checkpoint.top + 5f
        if (level.id > 0) saveManager.saveCheckpoint(level.id, checkpointX, checkpointY)
        traps.say("CHECKPOINT SAVED", 1.4f)
        particles.emit(checkpointX + 16f, checkpointY + 16f, 0xFF70E5B2.toInt(), 16)
        audio.play("checkpoint")
    }

    private fun teleportPlayer() {
        val destination = level.teleporterOut ?: return
        player.x = destination.left + 12f
        player.y = destination.top + 34f
        player.velocityX = 0f
        player.velocityY = 0f
        traps.teleport()
        particles.emit(player.x + player.width / 2f, player.y + player.height / 2f, 0xFFBE7BFF.toInt(), 18)
        audio.play("trap")
    }

    private fun discoverSecret() {
        if (secretDiscovered) return
        secretDiscovered = true
        val secretId = 46 + (level.worldIndex % 3)
        saveManager.unlockSecret(secretId)
        traps.say("SECRET ROOM FOUND", 2f)
        particles.emit(player.x + player.width / 2f, player.y, 0xFFFFD36E.toInt(), 28, 240f)
        audio.play("secret")
        audio.vibrate(26L)
    }

    private fun reachGoal() {
        if (level.puzzle.kind == TrapKind.FAKE_EXIT && !fakeExitTriggered) {
            fakeExitTriggered = true
            player.x = 75f
            player.y = level.startY
            player.velocityX = 0f
            player.velocityY = 0f
            traps.say("THAT WAS THE FAKE EXIT", 2.1f)
            screenShakeTime = if (settings.screenShake) 0.36f else 0f
            audio.play("fail")
            return
        }
        if (state != GameState.RUNNING) return
        state = GameState.COMPLETE
        val runTime = elapsedSeconds
        completedStars = when {
            deathsThisRun == 0 && runTime < 45f -> 3
            deathsThisRun <= 2 && runTime < 90f -> 2
            else -> 1
        }
        if (!level.endless) {
            if (level.id > 0) saveManager.markCompleted(level.id, completedStars, (runTime * 1000f).toLong())
        } else {
            endlessScore++
        }
        particles.emit(player.x + player.width / 2f, player.y + player.height / 2f, 0xFFFFD36E.toInt(), 38, 250f)
        screenFlashTime = 0.32f
        audio.play("victory")
        audio.vibrate(36L)
    }

    private fun nextLevel() {
        if (mode == GameMode.ENDLESS) {
            loadLevel(LevelManager.createEndless(endlessScore))
        } else if (level.id in 1 until LevelManager.STORY_LEVELS) {
            loadLevel(LevelManager.create(level.id + 1))
        } else {
            onExit()
        }
    }

    private fun restart() {
        val next = if (level.endless) LevelManager.createEndless(endlessScore) else LevelManager.create(level.id)
        loadLevel(next)
        audio.resume()
    }

    private fun loadLevel(next: GameLevel) {
        level = next
        traps.reset(level)
        particles.clear()
        elapsedSeconds = 0f
        deathsThisRun = 0
        checkpointActivated = false
        fakeCheckpointTriggered = false
        secretDiscovered = false
        fakeExitTriggered = false
        completedStars = 0
        checkpointX = level.startX
        checkpointY = level.startY
        applySavedCheckpoint()
        player.reset(checkpointX, checkpointY)
        cameraX = 0f
        state = GameState.RUNNING
        input.clear()
    }

    private fun applySavedCheckpoint() {
        if (level.id <= 0) return
        val saved = saveManager.checkpoint(level.id) ?: return
        checkpointX = saved.first.coerceIn(0f, level.worldWidth - playerWidth())
        checkpointY = saved.second.coerceIn(30f, 550f)
        checkpointActivated = true
    }

    private fun updateCamera(deltaSeconds: Float) {
        val target = (player.x + player.width / 2f - 500f).coerceIn(0f, max(0f, level.worldWidth - 1000f))
        val blend = (deltaSeconds * 5.2f).coerceIn(0f, 1f)
        cameraX += (target - cameraX) * blend
    }

    private fun playerWidth() = player.width

}