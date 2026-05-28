package com.game.roguelike.core

enum class GameState {
    MENU,
    PLAYING,
    PAUSED,
    BLESSING_SELECT,
    SHOP,
    GAME_OVER,
    VICTORY,
    LAYER_TRANSITION
}

enum class PlayerState {
    IDLE,
    RUN,
    ATTACK1,
    ATTACK2,
    ATTACK3,
    SPECIAL,
    DASH,
    HURT,
    DEAD
}

enum class EnemyState {
    IDLE,
    PATROL,
    CHASE,
    PREPARE_ATTACK,
    ATTACK,
    HURT,
    DEAD
}

enum class RoomType {
    ENTRY,
    COMBAT,
    REWARD,
    SHOP,
    BOSS,
    ELITE,
    TREASURE,
    EVENT,
    REST,
    HIDDEN
}

enum class LayerId {
    TARTARUS,
    ASPHODEL,
    ELYSIUM
}

enum class BlessingType {
    ATTACK,
    SPECIAL,
    DASH,
    SUPPORT
}

enum class BlessingRarity {
    COMMON,
    RARE,
    EPIC,
    DUO
}

enum class GodType {
    ZEUS,
    APHRODITE,
    ARES,
    ATHENA,
    HERMES,
    DEMETER,
    HADES
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT,
    UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT
}
