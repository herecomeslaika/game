package com.game.roguelike.event

import android.graphics.Color
import kotlin.random.Random

object EventPool {

    private val universalEvents = listOf(
        GameEvent(
            "gamble_invitation", "赌徒的邀请",
            "一个幽灵般的赌徒向你伸出手，指尖闪烁着金色光芒...",
            "赌徒", Color.parseColor("#88AACC"),
            listOf(
                EventOption(
                    "赌一把",
                    "失去50%金币",
                    "50%概率翻倍",
                    EventEffect.GambleGold(0.5f, 2f)
                ),
                EventOption(
                    "拒绝",
                    "无代价",
                    "获得15金币安慰奖",
                    EventEffect.RestHeal(0f)
                )
            )
        ),
        GameEvent(
            "wounded_warrior", "受伤的战士",
            "一个重伤的战士倒在路边，身上还有不少金币...",
            "战士", Color.parseColor("#CC4444"),
            listOf(
                EventOption(
                    "夺取金币",
                    "良心不安",
                    "获得40金币",
                    EventEffect.LoseHpGiveGold(0.2f, 40)
                ),
                EventOption(
                    "救治他",
                    "无代价",
                    "恢复15%生命",
                    EventEffect.RestHeal(0.15f)
                )
            )
        ),
        GameEvent(
            "mysterious_spring", "神秘泉水",
            "一眼闪烁微光的泉水从石缝间涌出...",
            "泉水", Color.parseColor("#4488CC"),
            listOf(
                EventOption(
                    "饮用泉水",
                    "无代价",
                    "恢复30%生命",
                    EventEffect.RestHeal(0.3f)
                ),
                EventOption(
                    "投入金币",
                    "花费30金币",
                    "恢复50%生命",
                    EventEffect.SpendGoldGiveDamage(30, 0f)
                )
            )
        ),
        GameEvent(
            "dark_altar", "黑暗祭坛",
            "一座刻有古老符文的祭坛，散发着不祥的气息...",
            "祭坛", Color.parseColor("#664488"),
            listOf(
                EventOption(
                    "献祭生命力",
                    "失去15%最大生命",
                    "攻击+3",
                    EventEffect.LoseMaxHpGiveDamage(0.15f, 3f)
                ),
                EventOption(
                    "离开",
                    "无代价",
                    "无收益",
                    EventEffect.RestHeal(0f)
                )
            )
        ),
        GameEvent(
            "traveler_relic", "旅商的遗物",
            "一个行囊散落在地上，里面似乎藏着什么...",
            "旅商", Color.parseColor("#AA8844"),
            listOf(
                EventOption(
                    "购买遗物",
                    "花费60金币",
                    "获得随机祝福",
                    EventEffect.LoseGoldGiveBlessing(60)
                ),
                EventOption(
                    "无视",
                    "无代价",
                    "无收益",
                    EventEffect.RestHeal(0f)
                )
            )
        )
    )

    private val layer1Events = listOf(
        GameEvent(
            "dead_deal", "亡者的交易",
            "一个骷髅向你招手，声称可以交换力量...",
            "骷髅商人", Color.parseColor("#88AA66"),
            listOf(
                EventOption(
                    "以血换力",
                    "失去25%生命",
                    "获得一个随机祝福",
                    EventEffect.LoseHpGiveBlessing(0.25f)
                ),
                EventOption(
                    "拒绝交易",
                    "无代价",
                    "获得10金币",
                    EventEffect.RestHeal(0f)
                )
            )
        ),
        GameEvent(
            "soul_well", "灵魂之井",
            "一口幽深的井，里面传来回响的歌声...",
            "灵魂井", Color.parseColor("#6644AA"),
            listOf(
                EventOption(
                    "投入金币",
                    "花费50金币",
                    "75%攻击+2",
                    EventEffect.CoinFlipBlessing(50, 0.75f)
                ),
                EventOption(
                    "离开",
                    "无代价",
                    "无收益",
                    EventEffect.RestHeal(0f)
                )
            )
        ),
        GameEvent(
            "ghost_whisper", "幽灵的低语",
            "一个幽灵在你耳边低语，许诺各种诱惑...",
            "幽灵", Color.parseColor("#AA66EE"),
            listOf(
                EventOption(
                    "接受诱惑",
                    "速度-5%",
                    "获得20金币",
                    EventEffect.SpendGoldGiveDamage(0, 0f)
                ),
                EventOption(
                    "聆听低语",
                    "无代价",
                    "获得10金币",
                    EventEffect.RestHeal(0f)
                )
            )
        )
    )

    private val layer2Events = listOf(
        GameEvent(
            "lava_forge", "熔岩锻造",
            "一把锤子插在熔岩中，似乎可以锻造武器...",
            "锻造师", Color.parseColor("#FF6622"),
            listOf(
                EventOption(
                    "锻造武器",
                    "失去20%生命",
                    "攻击+4",
                    EventEffect.LoseMaxHpGiveDamage(0.2f, 4f)
                ),
                EventOption(
                    "离开",
                    "无代价",
                    "恢复10%生命",
                    EventEffect.RestHeal(0.1f)
                )
            )
        ),
        GameEvent(
            "fire_divination", "火焰占卜",
            "火焰中浮现出模糊的命运影像...",
            "占卜师", Color.parseColor("#FF8844"),
            listOf(
                EventOption(
                    "占卜命运",
                    "花费80金币",
                    "获得随机祝福",
                    EventEffect.LoseGoldGiveBlessing(80)
                ),
                EventOption(
                    "离开",
                    "无代价",
                    "无收益",
                    EventEffect.RestHeal(0f)
                )
            )
        ),
        GameEvent(
            "alchemist", "炼金术士的实验",
            "一个疯狂的炼金术士要你试喝他的新药水...",
            "炼金术士", Color.parseColor("#44AA88"),
            listOf(
                EventOption(
                    "喝下药水",
                    "失去30%生命",
                    "75%恢复50%生命",
                    EventEffect.AlchemistExperiment(0.3f, 0.5f)
                ),
                EventOption(
                    "拒绝",
                    "无代价",
                    "获得5金币",
                    EventEffect.RestHeal(0f)
                )
            )
        )
    )

    private val layer3Events = listOf(
        GameEvent(
            "holy_judgment", "圣光的审判",
            "一束圣光降临，要求你献上金币以换取力量...",
            "圣光", Color.parseColor("#FFDDAA"),
            listOf(
                EventOption(
                    "献上金币",
                    "花费100金币",
                    "攻击+5",
                    EventEffect.SpendGoldGiveDamage(100, 5f)
                ),
                EventOption(
                    "拒绝",
                    "无代价",
                    "恢复5%生命",
                    EventEffect.RestHeal(0.05f)
                )
            )
        ),
        GameEvent(
            "fallen_angel", "堕落天使",
            "一个翅膀残破的天使，仍在散发神圣光辉...",
            "天使", Color.parseColor("#CCAAFF"),
            listOf(
                EventOption(
                    "接受赐福",
                    "失去25%最大生命",
                    "获得一个稀有祝福",
                    EventEffect.LoseHpGiveBlessing(0.25f)
                ),
                EventOption(
                    "医治天使",
                    "失去10%生命",
                    "恢复40%生命",
                    EventEffect.AlchemistExperiment(0.1f, 0.4f)
                )
            )
        ),
        GameEvent(
            "fate_wheel", "命运之轮",
            "一个巨大的命运之轮缓缓转动...",
            "命运", Color.parseColor("#FFD700"),
            listOf(
                EventOption(
                    "转动命运之轮",
                    "无代价",
                    "随机命运",
                    EventEffect.FateWheel(listOf(
                        FateOutcome("获得30金币") { _ -> },
                        FateOutcome("攻击+3") { _ -> },
                        FateOutcome("恢复40%生命") { _ -> },
                        FateOutcome("什么都没有") { _ -> }
                    ))
                ),
                EventOption(
                    "离开",
                    "无代价",
                    "无收益",
                    EventEffect.RestHeal(0f)
                )
            )
        ),
        GameEvent(
            "final_altar", "最终祭坛",
            "最后一座祭坛，散发着无与伦比的力量...",
            "祭坛", Color.parseColor("#BB88FF"),
            listOf(
                EventOption(
                    "倾尽所有",
                    "失去所有金币",
                    "恢复50%生命+攻击+2",
                    EventEffect.LoseAllGoldHealAndDamage(0.5f, 2f)
                ),
                EventOption(
                    "谨慎离开",
                    "无代价",
                    "恢复10%生命",
                    EventEffect.RestHeal(0.1f)
                )
            )
        )
    )

    fun rollEvent(layerIndex: Int): GameEvent {
        val pool = when (layerIndex) {
            0 -> universalEvents + layer1Events
            1 -> universalEvents + layer2Events
            2 -> universalEvents + layer3Events
            else -> universalEvents
        }
        return pool[Random.nextInt(pool.size)]
    }
}