package com.game.roguelike.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LayerTransitionStateTest {

    @Test
    fun `begin locks next layer and ignores repeated begins`() {
        val transition = LayerTransitionState()

        assertTrue(transition.beginFrom(0))
        assertFalse(transition.beginFrom(1))

        assertEquals(1, transition.pendingTargetLayerIndex)
    }

    @Test
    fun `consume target clears pending transition`() {
        val transition = LayerTransitionState()
        transition.beginFrom(0)

        assertEquals(1, transition.consumeTargetLayerIndex())
        assertNull(transition.pendingTargetLayerIndex)
    }

    @Test
    fun `consume without pending target returns null`() {
        val transition = LayerTransitionState()

        assertNull(transition.consumeTargetLayerIndex())
    }
}
