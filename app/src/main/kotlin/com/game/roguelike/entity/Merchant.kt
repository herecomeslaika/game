package com.game.roguelike.entity

import android.graphics.Canvas
import com.game.roguelike.core.Game
import com.game.roguelike.rendering.IsometricRenderer
import com.game.roguelike.util.Vector2

class Merchant(spawnPos: Vector2) : Entity() {

    override var width = 16f
    override var height = 40f

    var talked = false
    var isNearPlayer = false

    init {
        position = Vector2(spawnPos.x, spawnPos.y)
    }

    override fun update(dt: Float, game: Game) {
        isNearPlayer = position.distanceTo(game.player.position) < 60f
    }

    override fun render(canvas: Canvas, renderer: IsometricRenderer) {
        renderer.renderMerchant(canvas, this, isNearPlayer)
    }
}