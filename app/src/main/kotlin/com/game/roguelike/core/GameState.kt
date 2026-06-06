package com.game.roguelike.core

enum class GameState {
    MENU,
    INTRO_STORY,
    PLAYING,
    SHOP,
    EVENT,
    BLESSING_SELECT,
    LAYER_TRANSITION,
    BOSS_ENTRANCE,
    PLAYER_DEATH,
    FAILURE_STORY,
    GAME_OVER,
    VICTORY,
    ENDING_STORY,
    MULTIPLAYER_LOBBY,
    ROOM_LIST,
    ROOM_WAITING,
    OPTIONS,
    EXIT_CONFIRM
}

enum class LayerId {
    TARTARUS,
    ASPHODEL,
    ELYSIUM
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT,
    UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT
}
