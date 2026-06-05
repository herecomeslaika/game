package com.game.roguelike.entity

import android.graphics.Color
import com.game.roguelike.util.Vector2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BossWarningTest {

    @Test
    fun `circle warning tracks countdown ratio and containment`() {
        val warning = BossWarning.circle(
            position = Vector2(100f, 100f),
            radius = 50f,
            warnDuration = 1f,
            damage = 10,
            color = Color.WHITE
        )

        assertTrue(warning.contains(Vector2(130f, 120f)))
        assertFalse(warning.contains(Vector2(180f, 100f)))

        warning.updateTimer(0.25f)

        assertEquals(0.75f, warning.warningRatio, 0.001f)
        assertFalse(warning.readyToResolve)
    }

    @Test
    fun `line warning contains points close to the segment only`() {
        val warning = BossWarning.line(
            start = Vector2(0f, 0f),
            end = Vector2(100f, 0f),
            width = 20f,
            warnDuration = 0.6f,
            damage = 12,
            color = Color.WHITE
        )

        assertTrue(warning.contains(Vector2(50f, 8f)))
        assertFalse(warning.contains(Vector2(50f, 18f)))
        assertFalse(warning.contains(Vector2(130f, 0f)))
    }
}
