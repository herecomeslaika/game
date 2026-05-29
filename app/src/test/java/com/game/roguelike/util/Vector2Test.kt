package com.game.roguelike.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class Vector2Test {

    @Test
    fun `ZERO has zero components`() {
        assertEquals(0f, Vector2.ZERO.x)
        assertEquals(0f, Vector2.ZERO.y)
    }

    @Test
    fun `constructor sets components`() {
        val v = Vector2(3f, 4f)
        assertEquals(3f, v.x)
        assertEquals(4f, v.y)
    }

    @Test
    fun `magnitude returns correct length`() {
        val v = Vector2(3f, 4f)
        assertEquals(5f, v.magnitude(), 0.001f)
    }

    @Test
    fun `magnitude of zero vector is zero`() {
        assertEquals(0f, Vector2.ZERO.magnitude(), 0.001f)
    }

    @Test
    fun `normalized returns unit vector`() {
        val v = Vector2(3f, 4f)
        val n = v.normalized()
        assertEquals(1f, n.magnitude(), 0.001f)
    }

    @Test
    fun `normalized of zero vector returns zero`() {
        val n = Vector2.ZERO.normalized()
        assertEquals(0f, n.x, 0.001f)
        assertEquals(0f, n.y, 0.001f)
    }

    @Test
    fun `distanceTo returns correct distance`() {
        val a = Vector2(0f, 0f)
        val b = Vector2(3f, 4f)
        assertEquals(5f, a.distanceTo(b), 0.001f)
    }

    @Test
    fun `addition combines components`() {
        val a = Vector2(1f, 2f)
        val b = Vector2(3f, 4f)
        val result = a + b
        assertEquals(4f, result.x)
        assertEquals(6f, result.y)
    }

    @Test
    fun `subtraction subtracts components`() {
        val a = Vector2(5f, 7f)
        val b = Vector2(2f, 3f)
        val result = a - b
        assertEquals(3f, result.x)
        assertEquals(4f, result.y)
    }

    @Test
    fun `scalar multiplication scales both components`() {
        val v = Vector2(2f, 3f)
        val result = v * 2f
        assertEquals(4f, result.x)
        assertEquals(6f, result.y)
    }

    @Test
    fun `scalar division divides both components`() {
        val v = Vector2(6f, 8f)
        val result = v / 2f
        assertEquals(3f, result.x)
        assertEquals(4f, result.y)
    }

    @Test
    fun `fromAngle returns correct direction`() {
        val right = Vector2.fromAngle(0.0)
        assertEquals(1f, right.x, 0.001f)
        assertEquals(0f, right.y, 0.001f)

        val down = Vector2.fromAngle(Math.PI / 2)
        assertEquals(0f, down.x, 0.001f)
        assertEquals(1f, down.y, 0.001f)
    }

    @Test
    fun `lerp interpolates between vectors`() {
        val a = Vector2(0f, 0f)
        val b = Vector2(10f, 20f)
        val mid = a.lerp(b, 0.5f)
        assertEquals(5f, mid.x, 0.001f)
        assertEquals(10f, mid.y, 0.001f)
    }

    @Test
    fun `lerp at zero returns start`() {
        val a = Vector2(1f, 2f)
        val b = Vector2(10f, 20f)
        val result = a.lerp(b, 0f)
        assertEquals(1f, result.x, 0.001f)
        assertEquals(2f, result.y, 0.001f)
    }

    @Test
    fun `lerp at one returns end`() {
        val a = Vector2(1f, 2f)
        val b = Vector2(10f, 20f)
        val result = a.lerp(b, 1f)
        assertEquals(10f, result.x, 0.001f)
        assertEquals(20f, result.y, 0.001f)
    }
}
