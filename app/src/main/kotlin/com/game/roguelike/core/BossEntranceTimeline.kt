package com.game.roguelike.core

object BossEntranceTimeline {
    const val CINEMATIC_PHASE = 0
    const val TITLE_PHASE = 1
    const val FADE_PHASE = 2

    const val CINEMATIC_DURATION = 2.4f
    const val TITLE_DURATION = 2.0f
    const val FADE_DURATION = 0.5f

    val initialPhase: Int = CINEMATIC_PHASE

    fun nextPhase(phase: Int, timer: Float): Int? {
        return when {
            phase == CINEMATIC_PHASE && timer >= CINEMATIC_DURATION -> TITLE_PHASE
            phase == TITLE_PHASE && timer >= TITLE_DURATION -> FADE_PHASE
            else -> null
        }
    }

    fun showsTitle(phase: Int): Boolean {
        return phase == TITLE_PHASE || phase == FADE_PHASE
    }

    fun shouldSpawnBoss(phase: Int, timer: Float): Boolean {
        return phase == FADE_PHASE && timer >= FADE_DURATION
    }
}
