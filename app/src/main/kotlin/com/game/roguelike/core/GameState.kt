package com.game.roguelike.core

enum class GameState {
    MENU,
    PLAYING,
    SHOP,
    EVENT,
    BLESSING_SELECT,
    LAYER_TRANSITION,
    BOSS_ENTRANCE,
    PLAYER_DEATH,
    GAME_OVER,
    VICTORY
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
    DEAD,
    CHARGING,
    WHIRLWIND
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
