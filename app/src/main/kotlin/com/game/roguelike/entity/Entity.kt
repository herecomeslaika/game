package com.game.roguelike.entity

import android.graphics.Canvas
import com.game.roguelike.core.Game
import com.game.roguelike.rendering.IsometricRenderer
import com.game.roguelike.util.Vector2

abstract class Entity {
    var position = Vector2(0f, 0f)
    var velocity = Vector2(0f, 0f)
    open var width = 16f
    open var height = 32f

    abstract fun update(dt: Float, game: Game)
    abstract fun render(canvas: Canvas, renderer: IsometricRenderer)
}
