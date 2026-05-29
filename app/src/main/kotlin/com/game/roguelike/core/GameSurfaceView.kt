package com.game.roguelike.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
        game.vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
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
        // 所有状态下都先处理UI触摸（包括游戏状态的返回按钮）
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            game.handleTouch(event.x, event.y)
        }

        // 非游戏状态下不再处理摇杆输入
        if (game.gameState != GameState.PLAYING) {
            return true
        }

        // 游戏状态下处理摇杆输入
        return inputManager.onTouchEvent(event, width, height)
    }
}