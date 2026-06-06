package com.game.roguelike.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BossConfigScaleTest {

    @Test
    fun `boss configs use one and half body dimensions`() {
        val skeleton = EnemyConfig.forType(EnemyType.MEGA_SKELETON)
        val titan = EnemyConfig.forType(EnemyType.INFERNO_TITAN)
        val champion = EnemyConfig.forType(EnemyType.CHAMPION)

        assertEquals(48f, skeleton.width)
        assertEquals(90f, skeleton.height)
        assertEquals(60f, titan.width)
        assertEquals(105f, titan.height)
        assertEquals(48f, champion.width)
        assertEquals(90f, champion.height)
    }
}
