package com.game.roguelike.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PlayerDamageTest {

    private fun createPlayer(): Player {
        return Player()
    }

    @Test
    fun `player starts with full health`() {
        val player = createPlayer()
        assertEquals(100, player.maxHealth)
        assertEquals(100, player.health)
        assertFalse(player.isDead)
    }

    @Test
    fun `player initial attack damage values`() {
        val player = createPlayer()
        assertEquals(8f, player.attackDamage1)
        assertEquals(10f, player.attackDamage2)
        assertEquals(15f, player.attackDamage3)
    }

    @Test
    fun `player initial special cooldown`() {
        val player = createPlayer()
        assertEquals(0.8f, player.specialCooldown)
    }

    @Test
    fun `player initial dash stats`() {
        val player = createPlayer()
        assertEquals(700f, player.dashSpeed)
        assertEquals(0.15f, player.dashDuration)
        assertEquals(0.5f, player.dashCooldown)
    }

    @Test
    fun `player initial blessing effects are zero`() {
        val player = createPlayer()
        assertEquals(0f, player.critChance)
        assertEquals(2f, player.critMultiplier)
        assertEquals(0f, player.attackRangeBonus)
        assertEquals(0f, player.slowOnHit)
        assertEquals(0f, player.bossDamageBonus)
        assertFalse(player.lightningBounce)
        assertFalse(player.hasSummon)
    }

    @Test
    fun `athena shield defaults to inactive`() {
        val player = createPlayer()
        assertFalse(player.athenaShieldActive)
    }

    @Test
    fun `reset restores health`() {
        val player = createPlayer()
        player.health = 50
        player.reset()
        assertEquals(100, player.health)
    }

    @Test
    fun `isDead starts as false`() {
        val player = createPlayer()
        assertFalse(player.isDead)
    }

    @Test
    fun `reset clears owned gods`() {
        val player = createPlayer()
        player.ownedGods.add(com.game.roguelike.core.GodType.ZEUS)
        player.reset()
        assertTrue(player.ownedGods.isEmpty())
    }

    @Test
    fun `reset restores default attack stats`() {
        val player = createPlayer()
        player.attackDamage1 = 100f
        player.attackDamage2 = 200f
        player.attackDamage3 = 300f
        player.reset()
        assertEquals(8f, player.attackDamage1)
        assertEquals(10f, player.attackDamage2)
        assertEquals(15f, player.attackDamage3)
    }

    @Test
    fun `reset restores blessing effects`() {
        val player = createPlayer()
        player.critChance = 0.5f
        player.hasSummon = true
        player.lightningBounce = true
        player.reset()
        assertEquals(0f, player.critChance)
        assertFalse(player.hasSummon)
        assertFalse(player.lightningBounce)
    }

    @Test
    fun `comboStep starts at zero`() {
        val player = createPlayer()
        assertEquals(0, player.comboStep)
    }

    @Test
    fun `player initial state is IDLE`() {
        val player = createPlayer()
        assertEquals(com.game.roguelike.core.PlayerState.IDLE, player.stateMachine.currentState)
    }
}