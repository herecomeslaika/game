package com.game.roguelike.blessing

import com.game.roguelike.core.GodType
import com.game.roguelike.core.BlessingRarity
import com.game.roguelike.core.BlessingType
import com.game.roguelike.entity.Player

data class Blessing(
    val id: String,
    val name: String,
    val description: String,
    val type: BlessingType,
    val god: GodType,
    val rarity: BlessingRarity,
    val duoPair: Pair<GodType, GodType>? = null,
    val onApply: ((Player) -> Unit)? = null
) {
    companion object {
        // ========== ZEUS (宙斯) — 特殊攻击系 ==========
        val SWIFT_THUNDER = Blessing(
            "swift_thunder", "迅雷", "飞刀冷却-30%",
            BlessingType.SPECIAL, GodType.ZEUS, BlessingRarity.COMMON,
            onApply = { it.specialCooldown *= 0.7f }
        )
        val LIGHTNING_CHAIN = Blessing(
            "lightning_chain", "闪电链", "飞刀命中闪电弹跳至附近敌人",
            BlessingType.SPECIAL, GodType.ZEUS, BlessingRarity.COMMON,
            onApply = { it.lightningBounce = true }
        )
        val TRIPLE_DAGGER = Blessing(
            "triple_dagger", "三重飞刀", "一次投掷3把飞刀",
            BlessingType.SPECIAL, GodType.ZEUS, BlessingRarity.RARE,
            onApply = { it.knifeCount = 3 }
        )
        val ZEUS_WRATH = Blessing(
            "zeus_wrath", "宙斯之怒", "飞刀命中产生雷暴范围伤害",
            BlessingType.SPECIAL, GodType.ZEUS, BlessingRarity.EPIC,
            onApply = { it.knifeExplosive = true; it.specialDamage += 5f }
        )

        // ========== APHRODITE (阿佛洛狄忒) — 攻击系 ==========
        val HEART_STRIKE = Blessing(
            "heart_strike", "心之打击", "普攻伤害+30%",
            BlessingType.ATTACK, GodType.APHRODITE, BlessingRarity.COMMON,
            onApply = { it.attackDamage1 *= 1.3f; it.attackDamage2 *= 1.3f; it.attackDamage3 *= 1.3f }
        )
        val TENDER_BLOOM = Blessing(
            "tender_bloom", "柔情", "连招范围+20%",
            BlessingType.ATTACK, GodType.APHRODITE, BlessingRarity.COMMON,
            onApply = { it.attackRangeBonus = 15f }
        )
        val HEARTBREAK = Blessing(
            "heartbreak", "心碎", "暴击率20%，暴击伤害×2",
            BlessingType.ATTACK, GodType.APHRODITE, BlessingRarity.RARE,
            onApply = { it.critChance = 0.2f; it.critMultiplier = 2f }
        )
        val EMPTY_HEART = Blessing(
            "empty_heart", "空虚之心", "暴击时回复5生命",
            BlessingType.ATTACK, GodType.APHRODITE, BlessingRarity.EPIC,
            onApply = { it.critHealAmount = 5f }
        )

        // ========== ARES (阿瑞斯) — 攻击系 ==========
        val WAR_SPIRIT = Blessing(
            "war_spirit", "战意", "攻击速度+25%",
            BlessingType.ATTACK, GodType.ARES, BlessingRarity.COMMON,
            onApply = { it.attackSpeedMultiplier += 0.25f }
        )
        val BLOODLUST = Blessing(
            "bloodlust", "嗜血", "杀敌回复10生命",
            BlessingType.ATTACK, GodType.ARES, BlessingRarity.RARE,
            onApply = { it.killHealAmount = 10f }
        )
        val WAR_FURY = Blessing(
            "war_fury", "战神之怒", "击杀后+25%伤害5秒",
            BlessingType.ATTACK, GodType.ARES, BlessingRarity.RARE,
            onApply = { it.warCryDamageBonus = it.attackDamage1 * 0.25f }
        )
        val BLOOD_MASSACRE = Blessing(
            "blood_massacre", "血色屠杀", "连招3击全部暴击",
            BlessingType.ATTACK, GodType.ARES, BlessingRarity.EPIC,
            onApply = { it.comboAlwaysCrit = true }
        )

        // ========== ATHENA (雅典娜) — 冲刺系 ==========
        val DIVINE_SHIELD = Blessing(
            "divine_shield", "神盾", "每10秒抵挡1次攻击",
            BlessingType.DASH, GodType.ATHENA, BlessingRarity.COMMON,
            onApply = { it.athenaShieldActive = true; it.athenaShieldCooldown = 10f }
        )
        val WISDOM_DASH = Blessing(
            "wisdom_dash", "智慧冲刺", "冲刺冷却-40%",
            BlessingType.DASH, GodType.ATHENA, BlessingRarity.COMMON,
            onApply = { it.dashCooldown *= 0.6f }
        )
        val REFLECT_DASH = Blessing(
            "reflect_dash", "反弹冲刺", "冲刺反弹敌人攻击，无敌延长",
            BlessingType.DASH, GodType.ATHENA, BlessingRarity.RARE,
            onApply = { it.dashInvincibleExtension = 0.3f }
        )
        val DIVINE_PROTECTION = Blessing(
            "divine_protection", "神圣庇护", "挡攻击后3秒无敌",
            BlessingType.DASH, GodType.ATHENA, BlessingRarity.EPIC,
            onApply = { it.shieldInvincibleDuration = 3f }
        )

        // ========== HERMES (赫尔墨斯) — 冲刺系 ==========
        val SWIFT_STEP = Blessing(
            "swift_step", "快步", "冲刺冷却-50%",
            BlessingType.DASH, GodType.HERMES, BlessingRarity.COMMON,
            onApply = { it.dashCooldown *= 0.5f }
        )
        val LONG_DASH = Blessing(
            "long_dash", "长距冲刺", "冲刺距离+40%",
            BlessingType.DASH, GodType.HERMES, BlessingRarity.COMMON,
            onApply = { it.dashDuration *= 1.4f }
        )
        val DODGE_MASTER = Blessing(
            "dodge_master", "闪避大师", "冲刺后2秒攻速+50%",
            BlessingType.DASH, GodType.HERMES, BlessingRarity.RARE,
            onApply = { it.dashSpeedBoostDuration = 2f }
        )
        val TIME_SLOW = Blessing(
            "time_slow", "时之缓流", "冲刺时减速周围敌人3秒",
            BlessingType.DASH, GodType.HERMES, BlessingRarity.EPIC,
            onApply = { it.dashSlowNearby = 3f }
        )

        // ========== DEMETER (得墨忒耳) — 支援系 ==========
        val HEALING_GRACE = Blessing(
            "healing_grace", "治愈恩典", "恢复30%生命",
            BlessingType.SUPPORT, GodType.DEMETER, BlessingRarity.COMMON,
            onApply = { it.health = (it.health + (it.maxHealth * 0.3f).toInt()).coerceAtMost(it.maxHealth) }
        )
        val FROST_HEART = Blessing(
            "frost_heart", "冰霜之心", "攻击减速敌人2秒",
            BlessingType.SUPPORT, GodType.DEMETER, BlessingRarity.COMMON,
            onApply = { it.slowOnHit = 2f }
        )
        val ICE_FIELD = Blessing(
            "ice_field", "寒冰领域", "每8秒冰冻最近敌人1.5秒",
            BlessingType.SUPPORT, GodType.DEMETER, BlessingRarity.RARE,
            onApply = { it.supportFreeze = true; it.freezeDuration = 1.5f }
        )
        val DEAD_OF_WINTER = Blessing(
            "dead_of_winter", "冥冬", "冰冻敌人受到伤害×2",
            BlessingType.SUPPORT, GodType.DEMETER, BlessingRarity.EPIC,
            onApply = { it.freezeDamageMultiplier = 2f }
        )

        // ========== HADES (哈迪斯) — 支援系 ==========
        val UNDERWORLD_POWER = Blessing(
            "underworld_power", "冥界之力", "所有伤害+15%",
            BlessingType.SUPPORT, GodType.HADES, BlessingRarity.COMMON,
            onApply = { it.allDamageBonus = it.attackDamage1 * 0.15f }
        )
        val DEATH_GAZE = Blessing(
            "death_gaze", "死亡凝视", "低血量(<30%)时伤害×2",
            BlessingType.SUPPORT, GodType.HADES, BlessingRarity.RARE,
            onApply = { it.lowHpDamageMultiplier = 2f }
        )
        val UNDERWORLD_SUMMON = Blessing(
            "underworld_summon", "冥界召唤", "杀敌召唤幽灵助战3秒",
            BlessingType.SUPPORT, GodType.HADES, BlessingRarity.RARE,
            onApply = { it.hasSummon = true }
        )
        val FINAL_CALAMITY = Blessing(
            "final_calamity", "终末之灾", "Boss伤害+50%",
            BlessingType.SUPPORT, GodType.HADES, BlessingRarity.EPIC,
            onApply = { it.bossDamageBonus = 0.5f }
        )

        // ========== DUO 组合祝福 ==========
        val DUO_HEART_LIGHTNING = Blessing(
            "duo_heart_lightning", "心电感应", "暴击时闪电连锁所有附近敌人",
            BlessingType.ATTACK, GodType.ZEUS, BlessingRarity.DUO,
            duoPair = Pair(GodType.ZEUS, GodType.APHRODITE),
            onApply = { it.duoHeartLightning = true }
        )
        val DUO_THUNDER_SHIELD = Blessing(
            "duo_thunder_shield", "雷盾反弹", "挡攻击时释放闪电",
            BlessingType.DASH, GodType.ATHENA, BlessingRarity.DUO,
            duoPair = Pair(GodType.ZEUS, GodType.ATHENA),
            onApply = { it.duoThunderShield = true }
        )
        val DUO_BLOOD_HEART = Blessing(
            "duo_blood_heart", "嗜血之心", "暴击回复10生命+伤害+30%",
            BlessingType.ATTACK, GodType.ARES, BlessingRarity.DUO,
            duoPair = Pair(GodType.APHRODITE, GodType.ARES),
            onApply = { it.duoBloodHeart = true; it.attackDamage1 *= 1.3f }
        )
        val DUO_SPEED_SHIELD = Blessing(
            "duo_speed_shield", "神速之盾", "冲刺自动挡+冷却减半",
            BlessingType.DASH, GodType.HERMES, BlessingRarity.DUO,
            duoPair = Pair(GodType.HERMES, GodType.ATHENA),
            onApply = { it.duoSpeedShield = true; it.dashCooldown *= 0.5f }
        )
        val DUO_ICE_BLOOD = Blessing(
            "duo_ice_blood", "冰血战意", "减速敌人受伤×1.5+杀敌回复",
            BlessingType.ATTACK, GodType.DEMETER, BlessingRarity.DUO,
            duoPair = Pair(GodType.DEMETER, GodType.ARES),
            onApply = { it.duoIceBlood = true; it.killHealAmount += 5f }
        )
        val DUO_DEATH_LOVE = Blessing(
            "duo_death_love", "死亡之爱", "低血量暴击率100%",
            BlessingType.ATTACK, GodType.HADES, BlessingRarity.DUO,
            duoPair = Pair(GodType.HADES, GodType.APHRODITE),
            onApply = { it.duoDeathLove = true }
        )
        val DUO_JUDGEMENT = Blessing(
            "duo_judgement", "冥雷审判", "Boss击杀闪电全屏伤害",
            BlessingType.SPECIAL, GodType.HADES, BlessingRarity.DUO,
            duoPair = Pair(GodType.HADES, GodType.ZEUS),
            onApply = { it.duoJudgement = true }
        )

        val ALL_BLESSINGS = listOf(
            // Zeus
            SWIFT_THUNDER, LIGHTNING_CHAIN, TRIPLE_DAGGER, ZEUS_WRATH,
            // Aphrodite
            HEART_STRIKE, TENDER_BLOOM, HEARTBREAK, EMPTY_HEART,
            // Ares
            WAR_SPIRIT, BLOODLUST, WAR_FURY, BLOOD_MASSACRE,
            // Athena
            DIVINE_SHIELD, WISDOM_DASH, REFLECT_DASH, DIVINE_PROTECTION,
            // Hermes
            SWIFT_STEP, LONG_DASH, DODGE_MASTER, TIME_SLOW,
            // Demeter
            HEALING_GRACE, FROST_HEART, ICE_FIELD, DEAD_OF_WINTER,
            // Hades
            UNDERWORLD_POWER, DEATH_GAZE, UNDERWORLD_SUMMON, FINAL_CALAMITY
        )

        val ALL_DUO_BLESSINGS = listOf(
            DUO_HEART_LIGHTNING, DUO_THUNDER_SHIELD, DUO_BLOOD_HEART,
            DUO_SPEED_SHIELD, DUO_ICE_BLOOD, DUO_DEATH_LOVE, DUO_JUDGEMENT
        )

        /** Get blessings by god */
        fun getByGod(god: GodType) = ALL_BLESSINGS.filter { it.god == god }

        /** Get blessings by rarity */
        fun getByRarity(rarity: BlessingRarity) = ALL_BLESSINGS.filter { it.rarity == rarity }

        /** Check if duo blessing is available based on owned blessings */
        fun getAvailableDuo(ownedGods: Set<GodType>): List<Blessing> {
            return ALL_DUO_BLESSINGS.filter { duo ->
                val (g1, g2) = duo.duoPair!!
                g1 in ownedGods && g2 in ownedGods
            }
        }
    }
}