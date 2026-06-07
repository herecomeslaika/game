package com.game.roguelike.core

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoomClearRewardPolicyTest {

    @Test
    fun `boss rooms never grant blessings because they drop relics instead`() {
        assertFalse(RoomClearRewardPolicy.grantsBlessing(RoomType.BOSS, layerIndex = 1, bossFightStarted = false))
        assertFalse(RoomClearRewardPolicy.grantsBlessing(RoomType.BOSS, layerIndex = 0, bossFightStarted = true))
        assertFalse(RoomClearRewardPolicy.grantsBlessing(RoomType.BOSS, layerIndex = 1, bossFightStarted = true))
        assertFalse(RoomClearRewardPolicy.grantsBlessing(RoomType.BOSS, layerIndex = 2, bossFightStarted = true))
    }

    @Test
    fun `final boss can still clear the room without opening blessing select`() {
        assertTrue(RoomClearRewardPolicy.canClearAfterCombat(RoomType.BOSS, bossFightStarted = true))
        assertFalse(RoomClearRewardPolicy.grantsBlessing(RoomType.BOSS, layerIndex = 2, bossFightStarted = true))
    }

    @Test
    fun `regular combat rewards are unchanged`() {
        assertTrue(RoomClearRewardPolicy.canClearAfterCombat(RoomType.COMBAT, bossFightStarted = false))
        assertTrue(RoomClearRewardPolicy.canClearAfterCombat(RoomType.ELITE, bossFightStarted = false))
        assertTrue(RoomClearRewardPolicy.grantsBlessing(RoomType.COMBAT, layerIndex = 2, bossFightStarted = false))
        assertTrue(RoomClearRewardPolicy.grantsBlessing(RoomType.ELITE, layerIndex = 2, bossFightStarted = false))
    }
}
