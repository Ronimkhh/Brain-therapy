package com.traptheory.game

enum class GameMode(val wireValue: String) {
    STORY("story"), CHALLENGE("challenge"), TIME_ATTACK("time_attack"), ENDLESS("endless");

    companion object {
        fun from(value: String?): GameMode = entries.firstOrNull { it.wireValue == value } ?: STORY
    }
}

enum class GameState { RUNNING, PAUSED, DYING, COMPLETE, GAME_OVER }
enum class GraphicsQuality { LOW, MEDIUM, HIGH, ULTRA }
enum class PlatformKind { SOLID, CRUMBLE, INVISIBLE, MOVING, TIMED, BOUNCE }
enum class TrapKind {
    CLASSIC, FAKE_EXIT, CRUMBLE, INVISIBLE, FAKE_CHECKPOINT, TELEPORTER, GRAVITY,
    BUTTON, MOVING_HAZARD, HIDDEN_ROUTE, FALSE_WALL, WIND, BOUNCE, TIMED_FLOOR,
    MIRROR, ICE, DARKNESS, VANISHING_BRIDGE, INVERTED_CONTROLS, SPRING, DECOY_PATH,
    LOOP_DOOR, FALSE_BUTTON, SHOCKWAVE, MOVING_PLATFORM, SPEED_ZONE, SHADOW_EXIT,
    GRAVITY_SCHEDULE, SECRET_SIGNAL
}

enum class ControlId(val title: String) { LEFT("LEFT"), RIGHT("RIGHT"), JUMP("JUMP"), ACTION("ACTION"), PAUSE("PAUSE") }

data class GameSettings(
    val graphics: GraphicsQuality,
    val targetFps: Int,
    val particles: Boolean,
    val screenShake: Boolean,
    val lighting: Boolean,
    val vibration: Boolean,
    val fullscreen: Boolean,
    val language: String,
    val masterVolume: Float,
    val musicVolume: Float,
    val sfxVolume: Float,
    val controlSensitivity: Float
)

class ControlPosition(
    @Volatile var x: Float,
    @Volatile var y: Float,
    @Volatile var size: Float,
    @Volatile var alpha: Float
) {
    fun snapshot() = ControlPosition(x, y, size, alpha)
}

class Platform(
    x: Float,
    y: Float,
    val width: Float,
    val height: Float,
    var kind: PlatformKind = PlatformKind.SOLID,
    var movementRange: Float = 0f,
    var movementSpeed: Float = 1f
) {
    var x: Float = x
    var y: Float = y
    val baseX: Float = x
    val baseY: Float = y
    var active: Boolean = true
    var revealed: Boolean = kind != PlatformKind.INVISIBLE
    var steppedOn: Boolean = false
    var crumbleTime: Float = 0f
    var respawnTime: Float = 0f
}

class Hazard(
    x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    var movementRange: Float = 0f,
    var movementSpeed: Float = 1f
) {
    var x: Float = x
    val baseX: Float = x
    var active: Boolean = true
}

class Region(var left: Float, var top: Float, var right: Float, var bottom: Float) {
    fun overlaps(x: Float, y: Float, width: Float, height: Float): Boolean =
        x < right && x + width > left && y < bottom && y + height > top
}

data class PuzzleSpec(val title: String, val hint: String, val kind: TrapKind, val variant: Int)

class GameLevel(
    val id: Int,
    val worldIndex: Int,
    val worldName: String,
    val title: String,
    val puzzle: PuzzleSpec,
    val platforms: MutableList<Platform>,
    val hazards: MutableList<Hazard>,
    val goal: Region,
    val checkpoint: Region,
    val fakeCheckpoint: Region?,
    val teleporterIn: Region?,
    val teleporterOut: Region?,
    val button: Region?,
    val secret: Region?,
    val worldWidth: Float,
    val startX: Float = 72f,
    val startY: Float = 450f,
    val endless: Boolean = false
)

object GameWorlds {
    val names = listOf(
        "The Fake Factory",
        "Gravity Gone Wrong",
        "The Haunted Office",
        "Reality Glitch",
        "The Impossible Museum"
    )
}