package com.game.roguelike.core

import android.view.MotionEvent
import com.game.roguelike.util.Vector2
import kotlin.math.sqrt

class InputManager {
    var joystickDirection = Vector2.ZERO
        private set

    private var joystickTouchId: Int = -1
    private var joystickOrigin = Vector2.ZERO
    private val joystickMaxRadius = 100f

    // Button positions (set by ActionButtons.updateLayout)
    var atkX = 0f; var atkY = 0f; var atkR = 0f
    var spcX = 0f; var spcY = 0f; var spcR = 0f
    var dshX = 0f; var dshY = 0f; var dshR = 0f

    private var attackPressed = false
    private var specialPressed = false
    private var dashPressed = false

    fun onTouchEvent(event: MotionEvent, screenW: Int, screenH: Int): Boolean {
        val halfW = screenW / 2f
        val actionIdx = event.actionIndex
        val action = event.actionMasked

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(actionIdx)
                val y = event.getY(actionIdx)
                val id = event.getPointerId(actionIdx)

                if (x < halfW && joystickTouchId == -1) {
                    joystickTouchId = id
                    joystickOrigin = Vector2(x, y)
                } else if (x >= halfW) {
                    // Check actual button circles
                    val distAtk = sqrt((x - atkX) * (x - atkX) + (y - atkY) * (y - atkY))
                    val distSpc = sqrt((x - spcX) * (x - spcX) + (y - spcY) * (y - spcY))
                    val distDsh = sqrt((x - dshX) * (x - dshX) + (y - dshY) * (y - dshY))
                    when {
                        distAtk <= atkR -> attackPressed = true
                        distSpc <= spcR -> specialPressed = true
                        distDsh <= dshR -> dashPressed = true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    if (id == joystickTouchId) {
                        val x = event.getX(i)
                        val y = event.getY(i)
                        val dx = x - joystickOrigin.x
                        val dy = y - joystickOrigin.y
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist > 10f) {
                            val clamped = dist.coerceAtMost(joystickMaxRadius)
                            joystickDirection = Vector2(
                                dx / dist * (clamped / joystickMaxRadius),
                                dy / dist * (clamped / joystickMaxRadius)
                            )
                        } else {
                            joystickDirection = Vector2.ZERO
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(actionIdx)
                if (id == joystickTouchId) {
                    joystickTouchId = -1
                    joystickDirection = Vector2.ZERO
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                joystickTouchId = -1
                joystickDirection = Vector2.ZERO
            }
        }
        return true
    }

    fun consumeAttack(): Boolean {
        if (attackPressed) {
            attackPressed = false
            return true
        }
        return false
    }

    fun consumeSpecial(): Boolean {
        if (specialPressed) {
            specialPressed = false
            return true
        }
        return false
    }

    fun consumeDash(): Boolean {
        if (dashPressed) {
            dashPressed = false
            return true
        }
        return false
    }
}