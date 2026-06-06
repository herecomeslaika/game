package com.game.roguelike.entity

import com.game.roguelike.combat.ProjectileType

data class EnemyConfig(
    val type: EnemyType,
    val name: String,
    val maxHealth: Int,
    val speed: Float,
    val attackDamage: Int,
    val attackRange: Float,
    val attackCooldown: Float,
    val goldDrop: Int,
    val width: Float = 16f,
    val height: Float = 32f,
    val isRanged: Boolean = false,
    val projectileSpeed: Float = 200f,
    val projectileType: ProjectileType = ProjectileType.MAGIC_BOLT,
    val hasShield: Boolean = false,
    val aggroRange: Float = 250f,
    val canSummon: Boolean = false,
    val summonCooldown: Float = 5f,
    val summonCount: Int = 2,
    val leavesFireTrail: Boolean = false,
    val canCharge: Boolean = false,
    val chargeSpeed: Float = 400f,
    val canPhaseShift: Boolean = false,
    val phaseShiftCooldown: Float = 4f,
    val canFlameDash: Boolean = false,
    val flameDashCooldown: Float = 2.5f,
    val canGroundSlam: Boolean = false,
    val groundSlamCooldown: Float = 3f,
    val canMeteor: Boolean = false,
    val meteorCooldown: Float = 8f,
    val canDodgeRoll: Boolean = false,
    val dodgeRollCooldown: Float = 8f,
    val chargeComboMax: Int = 1,
    val shieldBashCooldown: Float = 3f,
    val bossName: String = "",
    val bossTitle: String = "",
    val bossPhaseThresholds: FloatArray = floatArrayOf(),
    val dissolveColor: Int = 0xFF88AA66.toInt(),
    val phaseTransitionColor: Int = 0xFFFFFFFF.toInt(),
    val spawnLayers: IntArray = intArrayOf(),
    val spawnWeight: Float = 1f,
    val bossLayer: Int = -1
) {
    companion object {
        private val registry = mutableMapOf<EnemyType, EnemyConfig>()

        fun forType(type: EnemyType): EnemyConfig = registry[type]!!

        fun bossForLayer(layerIndex: Int): EnemyConfig? =
            registry.values.find { it.bossLayer == layerIndex }

        fun spawnTypesForLayer(layerIndex: Int): List<EnemyConfig> =
            registry.values.filter { layerIndex in it.spawnLayers }

        private fun register(config: EnemyConfig) {
            registry[config.type] = config
        }

        init {
            register(EnemyConfig(
                type = EnemyType.SKELETON,
                name = "骷髅兵",
                maxHealth = 30, speed = 80f, attackDamage = 8,
                attackRange = 40f, attackCooldown = 1.2f, goldDrop = 8,
                spawnLayers = intArrayOf(0), spawnWeight = 1f,
                dissolveColor = 0xFF88AA66.toInt()
            ))
            register(EnemyConfig(
                type = EnemyType.WRAITH,
                name = "幽灵",
                maxHealth = 20, speed = 60f, attackDamage = 6,
                attackRange = 200f, attackCooldown = 2f, goldDrop = 12,
                isRanged = true, projectileSpeed = 180f, projectileType = ProjectileType.MAGIC_BOLT,
                canPhaseShift = true, phaseShiftCooldown = 4f,
                spawnLayers = intArrayOf(0), spawnWeight = 0.4f,
                dissolveColor = 0xFFAA66EE.toInt()
            ))
            register(EnemyConfig(
                type = EnemyType.MEGA_SKELETON,
                name = "巨型骷髅",
                maxHealth = 320, speed = 66f, attackDamage = 18,
                attackRange = 58f, attackCooldown = 1.3f, goldDrop = 100,
                width = 48f, height = 90f,
                canSummon = true, summonCooldown = 5f, summonCount = 2,
                canGroundSlam = false, groundSlamCooldown = 3f,
                bossName = "冥骨巨灵", bossTitle = "塔耳塔洛斯之主",
                bossPhaseThresholds = floatArrayOf(0.6f, 0.3f),
                bossLayer = 0,
                dissolveColor = 0xFF88AA66.toInt(),
                phaseTransitionColor = 0xFF66FF66.toInt()
            ))
            register(EnemyConfig(
                type = EnemyType.FLAME_DANCER,
                name = "火焰舞者",
                maxHealth = 40, speed = 140f, attackDamage = 10,
                attackRange = 35f, attackCooldown = 0.8f, goldDrop = 15,
                leavesFireTrail = true,
                canFlameDash = true, flameDashCooldown = 2.5f,
                spawnLayers = intArrayOf(1), spawnWeight = 1f,
                dissolveColor = 0xFFFF6622.toInt()
            ))
            register(EnemyConfig(
                type = EnemyType.LAVA_CASTER,
                name = "熔岩术士",
                maxHealth = 35, speed = 50f, attackDamage = 12,
                attackRange = 250f, attackCooldown = 1.8f, goldDrop = 18,
                isRanged = true, projectileSpeed = 200f, projectileType = ProjectileType.FIREBALL,
                spawnLayers = intArrayOf(1), spawnWeight = 0.4f,
                dissolveColor = 0xFFFF6622.toInt()
            ))
            register(EnemyConfig(
                type = EnemyType.INFERNO_TITAN,
                name = "炼狱泰坦",
                maxHealth = 650, speed = 60f, attackDamage = 27,
                attackRange = 220f, attackCooldown = 1.55f, goldDrop = 150,
                width = 60f, height = 105f,
                isRanged = true, projectileSpeed = 190f, projectileType = ProjectileType.FIREBALL,
                canCharge = true, chargeSpeed = 480f,
                canMeteor = false, meteorCooldown = 6.5f,
                chargeComboMax = 3,
                bossName = "炼狱泰坦", bossTitle = "阿斯福德的烈焰",
                bossPhaseThresholds = floatArrayOf(0.5f),
                bossLayer = 1,
                dissolveColor = 0xFFFF6622.toInt(),
                phaseTransitionColor = 0xFFFF6633.toInt()
            ))
            register(EnemyConfig(
                type = EnemyType.SHIELD_BEARER,
                name = "持盾守卫",
                maxHealth = 60, speed = 70f, attackDamage = 10,
                attackRange = 40f, attackCooldown = 1.5f, goldDrop = 20,
                hasShield = true, shieldBashCooldown = 3f,
                spawnLayers = intArrayOf(2), spawnWeight = 1f,
                dissolveColor = 0xFF6699DD.toInt()
            ))
            register(EnemyConfig(
                type = EnemyType.SPEAR_THROWER,
                name = "投矛手",
                maxHealth = 45, speed = 60f, attackDamage = 14,
                attackRange = 300f, attackCooldown = 2f, goldDrop = 22,
                isRanged = true, projectileSpeed = 250f, projectileType = ProjectileType.SPEAR,
                spawnLayers = intArrayOf(2), spawnWeight = 0.4f,
                dissolveColor = 0xFF6699DD.toInt()
            ))
            register(EnemyConfig(
                type = EnemyType.CHAMPION,
                name = "冠军勇士",
                maxHealth = 1050, speed = 96f, attackDamage = 36,
                attackRange = 190f, attackCooldown = 0.9f, goldDrop = 200,
                width = 48f, height = 90f,
                isRanged = true, projectileSpeed = 310f, projectileType = ProjectileType.SPEAR,
                hasShield = true,
                canDodgeRoll = false, dodgeRollCooldown = 8f,
                bossName = "永恒冠军", bossTitle = "伊利西昂的荣光",
                bossPhaseThresholds = floatArrayOf(0.65f, 0.35f),
                bossLayer = 2,
                dissolveColor = 0xFF6699DD.toInt(),
                phaseTransitionColor = 0xFF6699FF.toInt()
            ))
        }
    }
}
