package com.game.roguelike.core

import android.graphics.Color

val GodType.color: Int
    get() = when (this) {
        GodType.ZEUS -> Color.parseColor("#44AAFF")
        GodType.APHRODITE -> Color.parseColor("#FF4488")
        GodType.ARES -> Color.parseColor("#FF4444")
        GodType.ATHENA -> Color.parseColor("#FFAA44")
        GodType.HERMES -> Color.parseColor("#44FF88")
        GodType.DEMETER -> Color.parseColor("#88CCFF")
        GodType.HADES -> Color.parseColor("#AA44FF")
    }

val GodType.icon: String
    get() = when (this) {
        GodType.ZEUS -> "⚡"
        GodType.APHRODITE -> "♥"
        GodType.ARES -> "⚔"
        GodType.ATHENA -> "◆"
        GodType.HERMES -> "→"
        GodType.DEMETER -> "❆"
        GodType.HADES -> "☠"
    }

val GodType.displayName: String
    get() = when (this) {
        GodType.ZEUS -> "宙斯"
        GodType.APHRODITE -> "阿佛洛狄忒"
        GodType.ARES -> "阿瑞斯"
        GodType.ATHENA -> "雅典娜"
        GodType.HERMES -> "赫尔墨斯"
        GodType.DEMETER -> "得墨忒耳"
        GodType.HADES -> "哈迪斯"
    }
