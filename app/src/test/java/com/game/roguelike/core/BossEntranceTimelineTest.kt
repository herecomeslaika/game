package com.game.roguelike.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BossEntranceTimelineTest {

    @Test
    fun `cinematic phase runs before title phase`() {
        assertEquals(BossEntranceTimeline.CINEMATIC_PHASE, BossEntranceTimeline.initialPhase)
        assertFalse(BossEntranceTimeline.showsTitle(BossEntranceTimeline.CINEMATIC_PHASE))
        assertEquals(
            BossEntranceTimeline.TITLE_PHASE,
            BossEntranceTimeline.nextPhase(BossEntranceTimeline.CINEMATIC_PHASE, BossEntranceTimeline.CINEMATIC_DURATION)
        )
        assertTrue(BossEntranceTimeline.showsTitle(BossEntranceTimeline.TITLE_PHASE))
    }

    @Test
    fun `boss spawn waits until fade phase finishes`() {
        assertFalse(BossEntranceTimeline.shouldSpawnBoss(BossEntranceTimeline.CINEMATIC_PHASE, BossEntranceTimeline.CINEMATIC_DURATION))
        assertFalse(BossEntranceTimeline.shouldSpawnBoss(BossEntranceTimeline.TITLE_PHASE, BossEntranceTimeline.TITLE_DURATION))
        assertFalse(BossEntranceTimeline.shouldSpawnBoss(BossEntranceTimeline.FADE_PHASE, BossEntranceTimeline.FADE_DURATION - 0.01f))
        assertTrue(BossEntranceTimeline.shouldSpawnBoss(BossEntranceTimeline.FADE_PHASE, BossEntranceTimeline.FADE_DURATION))
    }
}
