package com.game.roguelike.entity

import com.game.roguelike.util.Vector2
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class EnemyDamageTest {

    private fun createSkeleton(): Enemy {
        return Enemy(EnemyType.SKELETON, Vector2(100f, 100f))
    }

    private fun createShieldBearer(): Enemy {
        return Enemy(EnemyType.SHIELD_BEARER, Vector2(100f, 100f))
    }

    @Test
    fun `skeleton has correct initial stats`() {
        val enemy = createSkeleton()
        assertEquals(30, enemy.maxHealth)
        assertEquals(30, enemy.health)
        assertEquals(8, enemy.attackDamage)
        assertFalse(enemy.hasShield)
    }

    @Test
    fun `shield bearer has shield and more health`() {
        val bearer = createShieldBearer()
        assertTrue(bearer.hasShield)
        assertEquals(60, bearer.maxHealth)
    }

    @Test
    fun `death starts animation timer not immediate removal`() {
        val enemy = createSkeleton()
        enemy.isDead = true
        enemy.deathAnimationTimer = enemy.deathAnimationDuration
        assertFalse(enemy.deathAnimationDone)
        assertTrue(enemy.deathAnimationTimer > 0f)
    }

    @Test
    fun `death animation timer is zero by default`() {
        val enemy = createSkeleton()
        assertEquals(0f, enemy.deathAnimationTimer)
    }

    @Test
    fun `death animation duration is 0 point 6 seconds`() {
        val enemy = createSkeleton()
        assertEquals(0.6f, enemy.deathAnimationDuration)
    }

    @Test
    fun `effectiveSpeed returns zero when frozen`() {
        val enemy = createSkeleton()
        enemy.freezeTimer = 2f
        assertEquals(0f, enemy.effectiveSpeed(80f))
    }

    @Test
    fun `effectiveSpeed returns reduced speed when slowed`() {
        val enemy = createSkeleton()
        enemy.slowTimer = 2f
        assertEquals(32f, enemy.effectiveSpeed(80f), 0.001f)
    }

    @Test
    fun `effectiveSpeed returns normal speed when not slowed or frozen`() {
        val enemy = createSkeleton()
        assertEquals(80f, enemy.effectiveSpeed(80f))
    }

    @Test
    fun `bossName returns correct names`() {
        assertEquals("冥骨巨灵", EnemyType.MEGA_SKELETON.bossName)
        assertEquals("炼狱泰坦", EnemyType.INFERNO_TITAN.bossName)
        assertEquals("永恒冠军", EnemyType.CHAMPION.bossName)
    }

    @Test
    fun `bossTitle returns correct titles`() {
        assertEquals("塔耳塔洛斯之主", EnemyType.MEGA_SKELETON.bossTitle)
        assertEquals("阿斯福德的烈焰", EnemyType.INFERNO_TITAN.bossTitle)
        assertEquals("伊利西昂的荣光", EnemyType.CHAMPION.bossTitle)
    }

    @Test
    fun `non-boss enemy types have empty boss name and title`() {
        assertEquals("", EnemyType.SKELETON.bossName)
        assertEquals("", EnemyType.WRAITH.bossTitle)
    }

    @Test
    fun `enemy initial state is IDLE`() {
        val enemy = createSkeleton()
        assertEquals(EnemyState.IDLE, enemy.stateMachine.currentState)
    }

    @Test
    fun `enemy initial position matches spawn`() {
        val pos = Vector2(42f, 84f)
        val enemy = Enemy(EnemyType.SKELETON, pos)
        assertEquals(42f, enemy.position.x, 0.001f)
        assertEquals(84f, enemy.position.y, 0.001f)
    }

    @Test
    fun `boss flag is false for regular enemies`() {
        val enemy = createSkeleton()
        assertFalse(enemy.isBoss)
    }
}