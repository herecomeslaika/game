package com.game.roguelike.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BossRelicTest {

    @Test
    fun `each boss layer maps to its own relic reward`() {
        assertEquals(BossRelicType.GIANT_BONE_CORE, BossRelicType.forLayer(0))
        assertEquals(BossRelicType.TITAN_MOLTEN_HEART, BossRelicType.forLayer(1))
        assertEquals(BossRelicType.CROWN_OF_ETERNITY, BossRelicType.forLayer(2))
    }

    @Test
    fun `boss relic ids and combat roles are stable`() {
        assertEquals("giant_bone_core", BossRelicType.GIANT_BONE_CORE.id)
        assertEquals("titan_molten_heart", BossRelicType.TITAN_MOLTEN_HEART.id)
        assertEquals("crown_of_eternity", BossRelicType.CROWN_OF_ETERNITY.id)
        assertTrue(BossRelicType.GIANT_BONE_CORE.grantsCombatPower)
        assertTrue(BossRelicType.TITAN_MOLTEN_HEART.grantsCombatPower)
        assertFalse(BossRelicType.CROWN_OF_ETERNITY.grantsCombatPower)
    }
}
