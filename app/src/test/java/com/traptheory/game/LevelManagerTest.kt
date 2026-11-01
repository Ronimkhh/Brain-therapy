package com.traptheory.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelManagerTest {
    @Test
    fun campaignContainsFortyFiveLevelsAcrossFiveWorlds() {
        val levels = (1..LevelManager.STORY_LEVELS).map(LevelManager::create)

        assertEquals(45, levels.size)
        assertEquals(GameWorlds.names, levels.map { it.worldName }.distinct())
        assertTrue(levels.all { it.platforms.size >= 5 && it.hazards.isNotEmpty() })
    }

    @Test
    fun everyCampaignPuzzleHasItsOwnTitleAndHint() {
        val levels = (1..LevelManager.STORY_LEVELS).map(LevelManager::create)

        assertEquals(45, levels.map { it.puzzle.title }.distinct().size)
        assertEquals(45, levels.map { it.puzzle.hint }.distinct().size)
        assertNotEquals(levels[0].puzzle.kind, levels[1].puzzle.kind)
    }

    @Test
    fun secretRoomsAndEndlessGenerationAreAvailable() {
        assertEquals(setOf(46, 47, 48), LevelManager.SECRET_LEVELS)
        assertTrue(LevelManager.SECRET_LEVELS.all { LevelManager.create(it).worldIndex == 4 })
        assertTrue(LevelManager.createEndless(2).endless)
        assertNotEquals(LevelManager.createEndless(0).title, LevelManager.createEndless(1).title)
    }
}