package com.traptheory.game

enum class CollisionEvent { NONE, HAZARD, FAKE_CHECKPOINT, CHECKPOINT, TELEPORT, SECRET, GOAL }

class CollisionSystem {
    fun detect(player: Player, level: GameLevel, teleportCooldown: Float): CollisionEvent {
        if (player.invulnerableTime <= 0f) {
            for (hazard in level.hazards) {
                if (overlaps(player.x, player.y, player.width, player.height, hazard.x, hazard.y, hazard.width, hazard.height)) {
                    return CollisionEvent.HAZARD
                }
            }
        }
        if (level.fakeCheckpoint?.overlaps(player.x, player.y, player.width, player.height) == true) {
            return CollisionEvent.FAKE_CHECKPOINT
        }
        if (level.teleporterIn?.overlaps(player.x, player.y, player.width, player.height) == true && teleportCooldown <= 0f) {
            return CollisionEvent.TELEPORT
        }
        if (level.checkpoint.overlaps(player.x, player.y, player.width, player.height)) return CollisionEvent.CHECKPOINT
        if (level.secret?.overlaps(player.x, player.y, player.width, player.height) == true) return CollisionEvent.SECRET
        if (level.goal.overlaps(player.x, player.y, player.width, player.height)) return CollisionEvent.GOAL
        return CollisionEvent.NONE
    }

    private fun overlaps(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        otherLeft: Float,
        otherTop: Float,
        otherWidth: Float,
        otherHeight: Float
    ): Boolean =
        left < otherLeft + otherWidth && left + width > otherLeft &&
            top < otherTop + otherHeight && top + height > otherTop
}