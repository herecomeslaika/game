package com.game.roguelike.entity

import com.game.roguelike.util.Vector2

enum class BossRelicType(
    val id: String,
    val displayName: String,
    val abilityName: String,
    val description: String,
    val grantsCombatPower: Boolean
) {
    GIANT_BONE_CORE(
        id = "giant_bone_core",
        displayName = "巨灵骸骨晶核",
        abilityName = "亡灵冲刺",
        description = "冲刺阶段无法被选中，并可穿越障碍与危险地形。",
        grantsCombatPower = true
    ),
    TITAN_MOLTEN_HEART(
        id = "titan_molten_heart",
        displayName = "泰坦熔心",
        abilityName = "熔火打击",
        description = "特殊技能的投射物变为火球，最终物理伤害提升50%，飞行速度与穿透逻辑保持不变。",
        grantsCombatPower = true
    ),
    CROWN_OF_ETERNITY(
        id = "crown_of_eternity",
        displayName = "永恒皇冠",
        abilityName = "无上荣耀",
        description = "获得称号【极乐之主】，头顶悬浮皇冠，移动路径留下金色星光。",
        grantsCombatPower = false
    );

    companion object {
        fun forLayer(layerIndex: Int): BossRelicType {
            return when (layerIndex.coerceIn(0, 2)) {
                0 -> GIANT_BONE_CORE
                1 -> TITAN_MOLTEN_HEART
                else -> CROWN_OF_ETERNITY
            }
        }

        fun fromId(id: String): BossRelicType? = entries.firstOrNull { it.id == id }
    }
}

class BossRelic(
    val type: BossRelicType,
    position: Vector2
) {
    val position = Vector2(position.x, position.y)
    var animTime = 0f

    fun update(dt: Float) {
        animTime += dt
    }

    fun toSaveString(): String {
        return "${type.id}:${position.x}:${position.y}"
    }

    companion object {
        fun fromSaveString(raw: String): BossRelic? {
            val parts = raw.split(":")
            if (parts.size != 3) return null
            val type = BossRelicType.fromId(parts[0]) ?: return null
            val x = parts[1].toFloatOrNull() ?: return null
            val y = parts[2].toFloatOrNull() ?: return null
            return BossRelic(type, Vector2(x, y))
        }
    }
}
