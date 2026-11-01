package com.traptheory.game

import kotlin.math.max

object LevelManager {
    const val STORY_LEVELS = 45
    val SECRET_LEVELS = setOf(46, 47, 48)

    private val puzzles = listOf(
        PuzzleSpec("Read the sign", "The safest-looking floor is probably safe. Probably.", TrapKind.CLASSIC, 1),
        PuzzleSpec("Exit interview", "A door can look convincing and still be a punchline.", TrapKind.FAKE_EXIT, 2),
        PuzzleSpec("Cost of crossing", "This bridge remembers every footstep.", TrapKind.CRUMBLE, 3),
        PuzzleSpec("Cloud credentials", "Some platforms only exist when you believe.", TrapKind.INVISIBLE, 4),
        PuzzleSpec("Button of consequences", "The button works. That is the problem.", TrapKind.BUTTON, 5),
        PuzzleSpec("Needle shift", "The spikes are watching your rhythm.", TrapKind.MOVING_HAZARD, 6),
        PuzzleSpec("Trust issues", "A checkpoint can lie about being a checkpoint.", TrapKind.FAKE_CHECKPOINT, 7),
        PuzzleSpec("Exit velocity", "Two doors. One destination. Maybe.", TrapKind.TELEPORTER, 8),
        PuzzleSpec("The missing page", "Look above the obvious route for a quieter one.", TrapKind.HIDDEN_ROUTE, 9),
        PuzzleSpec("Up is optional", "Gravity is only a suggestion in this building.", TrapKind.GRAVITY, 10),
        PuzzleSpec("The floor plan", "The wall is real until you stop treating it that way.", TrapKind.FALSE_WALL, 11),
        PuzzleSpec("Fan club", "The office ventilation has opinions about your route.", TrapKind.WIND, 12),
        PuzzleSpec("Bounce back", "A spring can be an elevator or an ambush.", TrapKind.BOUNCE, 13),
        PuzzleSpec("Closing time", "The floor operates on a very short schedule.", TrapKind.TIMED_FLOOR, 14),
        PuzzleSpec("Mirror test", "Your reflection may have different plans.", TrapKind.MIRROR, 15),
        PuzzleSpec("Ice breaker", "Momentum is a terrible thing to waste.", TrapKind.ICE, 16),
        PuzzleSpec("Lights out", "The dark hides more than the room dimensions.", TrapKind.DARKNESS, 17),
        PuzzleSpec("Bridge to nowhere", "The bridge is real. Its timing is not.", TrapKind.VANISHING_BRIDGE, 18),
        PuzzleSpec("Left is right", "The labels are honest. Your controls are not.", TrapKind.INVERTED_CONTROLS, 19),
        PuzzleSpec("Spring loaded", "A harmless pad can launch you into a bad idea.", TrapKind.SPRING, 20),
        PuzzleSpec("Wrong turn", "The shortest-looking path is the longest one.", TrapKind.DECOY_PATH, 21),
        PuzzleSpec("Door number zero", "The teleporter has a sense of humour.", TrapKind.LOOP_DOOR, 22),
        PuzzleSpec("Double negative", "This button does the opposite of what it promises.", TrapKind.FALSE_BUTTON, 23),
        PuzzleSpec("Shock value", "A quiet floor can still have a loud opinion.", TrapKind.SHOCKWAVE, 24),
        PuzzleSpec("Walking hazard", "The platform is moving. The spikes are too.", TrapKind.MOVING_PLATFORM, 25),
        PuzzleSpec("High-speed lie", "Run fast, but do not trust the finish line.", TrapKind.SPEED_ZONE, 26),
        PuzzleSpec("Shadow protocol", "A shadow can be a map or a warning.", TrapKind.SHADOW_EXIT, 27),
        PuzzleSpec("Gravity schedule", "The ceiling gets a turn every few seconds.", TrapKind.GRAVITY_SCHEDULE, 28),
        PuzzleSpec("Signal and noise", "The secret signal is hidden in plain sight.", TrapKind.SECRET_SIGNAL, 29),
        PuzzleSpec("Last factory shift", "The factory saved its least polite trick for last.", TrapKind.FAKE_EXIT, 30),
        PuzzleSpec("The unhelpful assistant", "The office assistant always points somewhere.", TrapKind.DECOY_PATH, 31),
        PuzzleSpec("Meeting room B", "This meeting has a moving agenda.", TrapKind.MOVING_PLATFORM, 32),
        PuzzleSpec("Printer jam", "Paper jams are contagious, apparently.", TrapKind.CRUMBLE, 33),
        PuzzleSpec("Out of office", "The exit is available. It is just not here.", TrapKind.TELEPORTER, 34),
        PuzzleSpec("After-hours policy", "The lights go out when the floor wakes up.", TrapKind.DARKNESS, 35),
        PuzzleSpec("The haunted inbox", "Do not open every message you find.", TrapKind.FALSE_BUTTON, 36),
        PuzzleSpec("Unscheduled maintenance", "The maintenance crew removed one useful thing.", TrapKind.INVISIBLE, 37),
        PuzzleSpec("Floor zero", "The building has more directions than you remember.", TrapKind.GRAVITY_SCHEDULE, 38),
        PuzzleSpec("The quiet hallway", "A hidden shortcut is never completely quiet.", TrapKind.HIDDEN_ROUTE, 39),
        PuzzleSpec("Afterimage", "Your previous move may return as a hazard.", TrapKind.MIRROR, 40),
        PuzzleSpec("Curator's choice", "The museum labels are part of the exhibit.", TrapKind.FAKE_CHECKPOINT, 41),
        PuzzleSpec("Please do not touch", "The display button is not decorative.", TrapKind.BUTTON, 42),
        PuzzleSpec("Fragile history", "Some exhibits cannot support a second visit.", TrapKind.VANISHING_BRIDGE, 43),
        PuzzleSpec("The moving portrait", "The frame changes when you are not looking.", TrapKind.MOVING_HAZARD, 44),
        PuzzleSpec("Exit through the gift shop", "The final exhibit is the exit. Probably.", TrapKind.FAKE_EXIT, 45),
        PuzzleSpec("Curator's secret", "The collection has a room that is not on the map.", TrapKind.SECRET_SIGNAL, 46),
        PuzzleSpec("The fourth wall", "This room is less solid than it looks.", TrapKind.FALSE_WALL, 47),
        PuzzleSpec("The last last exit", "The real exit is the one you stop chasing.", TrapKind.LOOP_DOOR, 48)
    )

    fun create(levelId: Int): GameLevel {
        val safeId = levelId.coerceIn(1, puzzles.size)
        val spec = puzzles[safeId - 1]
        val secret = safeId > STORY_LEVELS
        val worldIndex = if (secret) 4 else ((safeId - 1) / 9).coerceIn(0, 4)
        val ordinal = if (secret) safeId - STORY_LEVELS else (safeId - 1) % 9
        val offset = ((safeId * 19) % 45).toFloat()
        val platforms = mutableListOf(
            Platform(0f, 500f, 420f, 34f),
            Platform(490f + offset * 0.18f, 445f, 185f, 25f),
            Platform(765f + offset * 0.22f, 390f, 180f, 25f),
            Platform(1030f + offset * 0.14f, 455f, 240f, 25f),
            Platform(1350f, 500f, 470f, 34f)
        )

        val featurePlatform = platforms[1 + (ordinal % 2)]
        when (spec.kind) {
            TrapKind.CRUMBLE, TrapKind.VANISHING_BRIDGE -> featurePlatform.kind = PlatformKind.CRUMBLE
            TrapKind.INVISIBLE, TrapKind.SHADOW_EXIT, TrapKind.HIDDEN_ROUTE,
            TrapKind.SECRET_SIGNAL, TrapKind.FALSE_WALL, TrapKind.DECOY_PATH ->
                featurePlatform.kind = PlatformKind.INVISIBLE
            TrapKind.MOVING_PLATFORM, TrapKind.MIRROR -> {
                featurePlatform.kind = PlatformKind.MOVING
                featurePlatform.movementRange = 42f + (ordinal * 3f)
                featurePlatform.movementSpeed = 1.0f + ordinal * 0.08f
            }
            TrapKind.TIMED_FLOOR, TrapKind.DARKNESS -> featurePlatform.kind = PlatformKind.TIMED
            TrapKind.BOUNCE, TrapKind.SPRING -> featurePlatform.kind = PlatformKind.BOUNCE
            else -> Unit
        }

        if (safeId % 4 == 0) {
            val extra = platforms[3]
            extra.kind = PlatformKind.MOVING
            extra.movementRange = 22f + ordinal * 2f
            extra.movementSpeed = 0.8f + ordinal * 0.05f
        }

        val hazards = mutableListOf<Hazard>()
        val hazardCount = 1 + (safeId % 3) + (worldIndex / 2)
        repeat(hazardCount.coerceAtMost(4)) { index ->
            val hazardX = 348f + index * 365f + ((safeId * 13 + index * 17) % 44)
            val moving = spec.kind == TrapKind.MOVING_HAZARD || spec.kind == TrapKind.SHOCKWAVE
            hazards += Hazard(
                hazardX,
                if (index % 2 == 0) 466f else 356f,
                38f,
                34f,
                if (moving) 35f + ordinal * 2f else 0f,
                1.1f + ordinal * 0.08f
            )
        }

        val teleporter = spec.kind == TrapKind.TELEPORTER || spec.kind == TrapKind.LOOP_DOOR
        val hasButton = spec.kind == TrapKind.BUTTON || spec.kind == TrapKind.FALSE_BUTTON
        val hasSecret = spec.kind == TrapKind.HIDDEN_ROUTE || spec.kind == TrapKind.SECRET_SIGNAL || safeId > STORY_LEVELS
        val fakeCheckpoint = if (spec.kind == TrapKind.FAKE_CHECKPOINT) Region(820f, 385f, 900f, 500f) else null
        val hiddenSecret = if (hasSecret) Region(875f, 292f, 985f, 378f) else null

        return GameLevel(
            id = safeId,
            worldIndex = worldIndex,
            worldName = GameWorlds.names[worldIndex],
            title = if (secret) "Secret ${ordinal}: ${spec.title}" else "${safeId.toString().padStart(2, '0')} / ${spec.title}",
            puzzle = spec,
            platforms = platforms,
            hazards = hazards,
            goal = Region(1650f, 415f, 1710f, 500f),
            checkpoint = Region(1115f, 405f, 1190f, 500f),
            fakeCheckpoint = fakeCheckpoint,
            teleporterIn = if (teleporter) Region(760f, 328f, 812f, 445f) else null,
            teleporterOut = if (teleporter) Region(if (spec.kind == TrapKind.LOOP_DOOR) 470f else 1215f, 350f, if (spec.kind == TrapKind.LOOP_DOOR) 530f else 1275f, 455f) else null,
            button = if (hasButton) Region(710f, 365f, 770f, 445f) else null,
            secret = hiddenSecret,
            worldWidth = 1820f,
            startX = 72f,
            startY = 450f
        )
    }

    fun createEndless(seed: Int): GameLevel {
        val base = create((seed % STORY_LEVELS) + 1)
        return GameLevel(
            id = -(seed + 1),
            worldIndex = base.worldIndex,
            worldName = base.worldName,
            title = "Run ${(seed + 1).toString().padStart(2, '0')} / ${base.puzzle.title}",
            puzzle = base.puzzle,
            platforms = base.platforms,
            hazards = base.hazards,
            goal = base.goal,
            checkpoint = base.checkpoint,
            fakeCheckpoint = base.fakeCheckpoint,
            teleporterIn = base.teleporterIn,
            teleporterOut = base.teleporterOut,
            button = base.button,
            secret = base.secret,
            worldWidth = base.worldWidth + max(0, seed - STORY_LEVELS / 2) * 5f,
            startX = base.startX,
            startY = base.startY,
            endless = true
        )
    }

    fun puzzleFor(levelId: Int): PuzzleSpec = puzzles[(levelId.coerceIn(1, puzzles.size)) - 1]
}