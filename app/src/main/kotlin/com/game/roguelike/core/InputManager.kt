package com.game.roguelike.core

import android.view.MotionEvent
import com.game.roguelike.util.Vector2
import kotlin.math.sqrt

class InputManager {
    // 核心修复：只要没有绑定摇杆触摸ID，永远返回方向为零，从根源杜绝残留值
    var joystickDirection: Vector2
        get() = if (joystickTouchId == -1) Vector2.ZERO else _joystickDirection
        private set(value) { _joystickDirection = value }
    private var _joystickDirection = Vector2.ZERO

    var joystickTouchId: Int = -1
        private set
    private var joystickOrigin = Vector2.ZERO
    private val joystickMaxRadius = 100f

    // Button positions (set by ActionButtons.updateLayout)
    var atkX = 0f; var atkY = 0f; var atkR = 0f
    var spcX = 0f; var spcY = 0f; var spcR = 0f
    var dshX = 0f; var dshY = 0f; var dshR = 0f

    private var attackPressed = false
    private var specialPressed = false
    private var dashPressed = false

    /** 重置所有输入状态，切换游戏状态时调用 */
    fun reset() {
        joystickTouchId = -1
        joystickDirection = Vector2.ZERO
        attackPressed = false
        specialPressed = false
        dashPressed = false
    }

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
                var joystickFound = false
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    if (id == joystickTouchId) {
                        joystickFound = true
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
                // 修复：如果摇杆手指不在触摸列表里，重置方向
                if (joystickTouchId != -1 && !joystickFound) {
                    reset()
                }
            }
            MotionEvent.ACTION_UP -> {
                // 单点触摸结束，所有手指都抬起，直接重置所有输入
                reset()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(actionIdx)
                if (id == joystickTouchId) {
                    // 摇杆手指抬起，直接重置
                    reset()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                // 触摸事件被系统中断，重置所有输入
                reset()
            }
        }
        // 终极保险：只要没有绑定摇杆触摸ID，强制方向为零
        if (joystickTouchId == -1) {
            joystickDirection = Vector2.ZERO
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