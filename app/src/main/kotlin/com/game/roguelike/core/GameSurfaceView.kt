package com.game.roguelike.core

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    val game = Game(context)
    val inputManager = InputManager()
    private var surfaceReady = false

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        game.inputManager = inputManager
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Wait for surfaceChanged to provide dimensions
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        game.onScreenResize(width, height)
        if (!surfaceReady) {
            surfaceReady = true
            game.start(holder)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        game.stop()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle UI-state touches first (menu, blessing select, shop, game over)
        if (game.gameState == GameState.MENU ||
            game.gameState == GameState.BLESSING_SELECT ||
            game.gameState == GameState.SHOP ||
            game.gameState == GameState.GAME_OVER ||
            game.gameState == GameState.VICTORY
        ) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    game.handleTouch(event.x, event.y)
                }
            }
            // Don't process gameplay input for UI states
            return true
        }

        // Only process gameplay input when playing
        return inputManager.onTouchEvent(event, width, height)
    }
}