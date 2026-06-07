package com.game.roguelike.save

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GameSaveCodecTest {

    @Test
    fun `save data round trips through json`() {
        val save = GameSaveData(
            version = 1,
            layerIndex = 1,
            roomId = 7,
            roomCleared = true,
            playerHealth = 83,
            playerMaxHealth = 120,
            gold = 240,
            blessingIds = listOf("swift_thunder", "heart_strike"),
            state = "BLESSING_SELECT",
            offeringIds = listOf("war_spirit", "wisdom_dash"),
            bossRelicIds = listOf("giant_bone_core"),
            droppedBossRelics = listOf("titan_molten_heart:640.0:320.0")
        )

        assertEquals(save, GameSaveCodec.decode(GameSaveCodec.encode(save)))
    }

    @Test
    fun `invalid save json decodes to null`() {
        assertNull(GameSaveCodec.decode("{not-json"))
    }
}
