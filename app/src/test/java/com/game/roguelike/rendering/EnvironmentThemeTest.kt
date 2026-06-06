package com.game.roguelike.rendering

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class EnvironmentThemeTest {

    @Test
    fun `each layer maps to a distinct outside environment`() {
        val tartarus = EnvironmentTheme.forLayer(0)
        val asphodel = EnvironmentTheme.forLayer(1)
        val elysium = EnvironmentTheme.forLayer(2)

        assertEquals("Tartarus Abyss", tartarus.name)
        assertEquals("Asphodel Inferno", asphodel.name)
        assertEquals("Elysium Gardens", elysium.name)

        assertNotEquals(tartarus.accent, asphodel.accent)
        assertNotEquals(asphodel.accent, elysium.accent)
    }

    @Test
    fun `invalid layer indices clamp to available themes`() {
        assertEquals(EnvironmentTheme.forLayer(0), EnvironmentTheme.forLayer(-3))
        assertEquals(EnvironmentTheme.forLayer(2), EnvironmentTheme.forLayer(8))
    }

    @Test
    fun `layer themes describe their outside landmarks`() {
        assertEquals(
            listOf(EnvironmentLandmarkKind.GRAVE, EnvironmentLandmarkKind.GHOST, EnvironmentLandmarkKind.BONE),
            EnvironmentTheme.forLayer(0).landmarks
        )
        assertEquals(
            listOf(EnvironmentLandmarkKind.LAVA_POOL, EnvironmentLandmarkKind.LAVA_BEAST),
            EnvironmentTheme.forLayer(1).landmarks
        )
        assertEquals(
            listOf(EnvironmentLandmarkKind.BANNER, EnvironmentLandmarkKind.CASTLE),
            EnvironmentTheme.forLayer(2).landmarks
        )
    }
}
