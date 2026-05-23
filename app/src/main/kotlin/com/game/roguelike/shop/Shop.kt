package com.game.roguelike.shop

import com.game.roguelike.core.Game
import com.game.roguelike.entity.Player

data class ShopItem(
    val name: String,
    val description: String,
    val cost: Int,
    val applyEffect: (Player, Game) -> Unit
)

class Shop {
    var isOpen = false
    var items: List<ShopItem> = listOf()

    fun open(currentGold: Int) {
        isOpen = true
        items = generateItems()
    }

    fun close() {
        isOpen = false
    }

    private fun generateItems(): List<ShopItem> {
        return listOf(
            ShopItem("生命药水", "恢复30点生命值", 30) { p, _ ->
                p.health = (p.health + 30).coerceAtMost(p.maxHealth)
            },
            ShopItem("冲刺充能", "重置冲刺冷却", 20) { p, _ ->
                p.dashCooldownTimer = 0f
            },
            ShopItem("生命上限提升", "+25最大生命", 80) { p, _ ->
                p.maxHealth += 25; p.health += 25
            },
            ShopItem("攻击强化", "+5攻击力", 60) { p, _ ->
                p.attackDamage1 += 5f; p.attackDamage2 += 5f; p.attackDamage3 += 5f
            },
            ShopItem("移速提升", "+20%移速", 50) { p, _ ->
                p.speed *= 1.2f
            },
        )
    }
}