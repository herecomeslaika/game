package com.game.roguelike.rendering

import android.content.Context
import android.graphics.*
import com.game.roguelike.core.PlayerState
import com.game.roguelike.entity.Player
import kotlin.math.sin
import kotlin.math.cos

class PlayerRenderer(private val renderer: IsometricRenderer, private val context: Context) {

    private val p: Paint get() = renderer.paint
    private val useShaders: Boolean get() = renderer.enableShaders

    fun renderPlayer(canvas: Canvas, player: Player) {
        val (sx, sy) = renderer.worldToScreen(player.position)
        val facingRight = player.facingRight
        val isRunning = player.stateMachine.currentState == PlayerState.RUN
        val isIdle = player.stateMachine.currentState == PlayerState.IDLE
        val isHurt = player.stateMachine.currentState == PlayerState.HURT
        val isDashing = player.isDashing
        val isCharging = player.isCharging
        val isWhirlwinding = player.isWhirlwinding
        val isDead = player.isDead

        val deathProgress = if (isDead) {
            (player.deathTimer / player.deathDuration).coerceIn(0f, 1f)
        } else 0f

        canvas.save()

        if (isDead) {
            val leanAngle = deathProgress * 25f
            val scale = 1f - deathProgress * 0.5f
            canvas.translate(sx, sy)
            canvas.rotate(-leanAngle)
            canvas.scale(scale, scale)
            canvas.translate(-sx, -sy)
        } else if (!facingRight) {
            canvas.scale(-1f, 1f, sx, sy)
        }

        val wb = player.walkBlend
        val phase = player.moveAnimPhase
        val walkSin = sin(phase * Math.PI * 2).toFloat()
        val bob = walkSin * 2f * wb
        val legSwing = walkSin * 4f * wb
        val breathe = if (isIdle) (sin(player.idleTime * 2.5f) * 1f).toFloat() else 0f
        val bodyOffset = bob + breathe

        val bodyColor = if (isHurt && player.hurtTimer % 0.1f < 0.05f) Color.WHITE else Color.parseColor("#CC3333")
        val bodyColorLight = renderer.lighten(bodyColor, 1.2f)
        val bodyColorDark = renderer.darken(bodyColor, 0.7f)

        // === GROUND SHADOW ===
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy + 8f, 12f, Color.argb(50, 0, 0, 0), Color.argb(0, 0, 0, 0))
        } else {
            p.color = Color.argb(40, 0, 0, 0)
        }
        p.style = Paint.Style.FILL
        val shadowPath = renderer.obtainPath()
        shadowPath.addOval(RectF(sx - 12f, sy + 2f, sx + 12f, sy + 10f), Path.Direction.CW)
        canvas.drawPath(shadowPath, p)
        renderer.recyclePath(shadowPath)
        p.shader = null

        // === LEGS (Path with knee joint and boot shape) ===
        // Left leg
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 8f, sy - 6f, sx - 2f, sy + 10f, renderer.lighten(Color.parseColor("#992222"), 1.15f), renderer.darken(Color.parseColor("#992222"), 0.7f))
        } else {
            p.color = Color.parseColor("#992222")
        }
        p.style = Paint.Style.FILL
        val leftLeg = renderer.obtainPath()
        leftLeg.moveTo(sx - 7f, sy - 6f)
        leftLeg.lineTo(sx - 2f, sy - 6f)
        leftLeg.quadTo(sx - 2.5f, sy + 2f + legSwing, sx - 3f, sy + 6f + legSwing)
        leftLeg.lineTo(sx - 9f, sy + 6f + legSwing)
        leftLeg.quadTo(sx - 8f, sy + 2f + legSwing, sx - 7f, sy - 2f)
        leftLeg.close()
        canvas.drawPath(leftLeg, p)
        renderer.recyclePath(leftLeg)
        p.shader = null

        // Right leg
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx + 2f, sy - 6f, sx + 8f, sy + 10f, Color.parseColor("#992222"), renderer.darken(Color.parseColor("#992222"), 0.65f))
        } else {
            p.color = Color.parseColor("#882020")
        }
        p.style = Paint.Style.FILL
        val rightLeg = renderer.obtainPath()
        rightLeg.moveTo(sx + 2f, sy - 6f)
        rightLeg.lineTo(sx + 7f, sy - 6f)
        rightLeg.quadTo(sx + 8f, sy - 2f, sx + 9f, sy + 6f - legSwing)
        rightLeg.lineTo(sx + 3f, sy + 6f - legSwing)
        rightLeg.quadTo(sx + 2.5f, sy + 2f - legSwing, sx + 2f, sy - 2f)
        rightLeg.close()
        canvas.drawPath(rightLeg, p)
        renderer.recyclePath(rightLeg)
        p.shader = null

        // Boots
        reset()
        p.color = Color.parseColor("#661111")
        p.style = Paint.Style.FILL
        val leftBoot = renderer.obtainPath()
        leftBoot.moveTo(sx - 9f, sy + 5f + legSwing)
        leftBoot.lineTo(sx - 1f, sy + 5f + legSwing)
        leftBoot.lineTo(sx - 1f, sy + 10f + legSwing)
        leftBoot.lineTo(sx - 10f, sy + 10f + legSwing)
        leftBoot.quadTo(sx - 9f, sy + 7f + legSwing, sx - 9f, sy + 5f + legSwing)
        leftBoot.close()
        canvas.drawPath(leftBoot, p)
        renderer.recyclePath(leftBoot)
        val rightBoot = renderer.obtainPath()
        rightBoot.moveTo(sx + 1f, sy + 5f - legSwing)
        rightBoot.lineTo(sx + 9f, sy + 5f - legSwing)
        rightBoot.quadTo(sx + 9f, sy + 7f - legSwing, sx + 10f, sy + 10f - legSwing)
        rightBoot.lineTo(sx + 1f, sy + 10f - legSwing)
        rightBoot.close()
        canvas.drawPath(rightBoot, p)
        renderer.recyclePath(rightBoot)

        // === CAPE (flowing Path with gradient) ===
        val capeWalk = walkSin * 3f * wb
        val capeIdle = if (isIdle) (sin(player.idleTime * 1.5f) * 1f).toFloat() else 0f
        val capeSwing = capeWalk + capeIdle
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx, sy - 36f + bodyOffset, sx, sy - 8f + bodyOffset, Color.parseColor("#AA2222"), Color.parseColor("#661111"))
        } else {
            p.color = Color.parseColor("#881111")
        }
        p.style = Paint.Style.FILL
        val capePath = renderer.obtainPath()
        capePath.moveTo(sx - 10f, sy - 36f + bodyOffset)
        capePath.quadTo(sx - 14f + capeSwing, sy - 22f + bodyOffset, sx - 13f + capeSwing, sy - 8f + bodyOffset)
        capePath.lineTo(sx + 13f - capeSwing, sy - 8f + bodyOffset)
        capePath.quadTo(sx + 14f - capeSwing, sy - 22f + bodyOffset, sx + 10f, sy - 36f + bodyOffset)
        capePath.close()
        canvas.drawPath(capePath, p)
        renderer.recyclePath(capePath)
        p.shader = null

        // Cape edge highlight
        reset()
        p.color = Color.parseColor("#AA2222")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        canvas.drawLine(sx - 13f + capeSwing, sy - 36f + bodyOffset, sx - 13f + capeSwing * 0.5f, sy - 8f + bodyOffset, p)
        canvas.drawLine(sx + 13f - capeSwing, sy - 36f + bodyOffset, sx + 13f - capeSwing * 0.5f, sy - 8f + bodyOffset, p)

        // === BODY (curved torso Path with gradient) ===
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 10f, sy - 38f + bodyOffset, sx + 10f, sy - 6f + bodyOffset, bodyColorLight, bodyColorDark)
        } else {
            p.color = bodyColor
        }
        p.style = Paint.Style.FILL
        val bodyPath = renderer.obtainPath()
        bodyPath.moveTo(sx - 9f, sy - 6f + bodyOffset)
        bodyPath.lineTo(sx - 10f, sy - 22f + bodyOffset)
        bodyPath.quadTo(sx - 10f, sy - 36f + bodyOffset, sx - 6f, sy - 38f + bodyOffset)
        bodyPath.lineTo(sx + 6f, sy - 38f + bodyOffset)
        bodyPath.quadTo(sx + 10f, sy - 36f + bodyOffset, sx + 10f, sy - 22f + bodyOffset)
        bodyPath.lineTo(sx + 9f, sy - 6f + bodyOffset)
        bodyPath.quadTo(sx, sy - 4f + bodyOffset, sx - 9f, sy - 6f + bodyOffset)
        bodyPath.close()
        canvas.drawPath(bodyPath, p)
        renderer.recyclePath(bodyPath)
        p.shader = null

        // Chest plate highlight
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 8f, sy - 36f + bodyOffset, sx + 8f, sy - 22f + bodyOffset, Color.parseColor("#DD4444"), Color.parseColor("#BB3333"))
        } else {
            p.color = Color.parseColor("#DD4444")
        }
        p.style = Paint.Style.FILL
        val chestPath = renderer.obtainPath()
        chestPath.moveTo(sx - 8f, sy - 34f + bodyOffset)
        chestPath.lineTo(sx + 8f, sy - 34f + bodyOffset)
        chestPath.quadTo(sx + 7f, sy - 28f + bodyOffset, sx + 6f, sy - 22f + bodyOffset)
        chestPath.lineTo(sx - 6f, sy - 22f + bodyOffset)
        chestPath.quadTo(sx - 7f, sy - 28f + bodyOffset, sx - 8f, sy - 34f + bodyOffset)
        chestPath.close()
        canvas.drawPath(chestPath, p)
        renderer.recyclePath(chestPath)
        p.shader = null

        // Top highlight band
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx, sy - 36f + bodyOffset, sx, sy - 32f + bodyOffset, Color.argb(50, 255, 255, 255), Color.argb(0, 255, 255, 255))
        } else {
            p.color = Color.parseColor("#EE6666")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 8f, sy - 36f + bodyOffset, sx + 8f, sy - 34f + bodyOffset, p)
        p.shader = null

        // Belt with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 10f, 0f, sx + 10f, 0f, Color.parseColor("#AA7733"), Color.parseColor("#886633"))
        } else {
            p.color = Color.parseColor("#886633")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 10f, sy - 10f + bodyOffset, sx + 10f, sy - 6f + bodyOffset, p)
        p.shader = null

        // Belt buckle diamond
        reset()
        p.color = Color.parseColor("#FFD700")
        p.style = Paint.Style.FILL
        val buckle = renderer.obtainPath()
        buckle.moveTo(sx, sy - 10f + bodyOffset)
        buckle.lineTo(sx + 2f, sy - 8f + bodyOffset)
        buckle.lineTo(sx, sy - 6f + bodyOffset)
        buckle.lineTo(sx - 2f, sy - 8f + bodyOffset)
        buckle.close()
        canvas.drawPath(buckle, p)
        renderer.recyclePath(buckle)

        // === ARMS (Path with shoulder joint and hand) ===
        val armWalk = sin(phase * Math.PI * 2 + 0.5f).toFloat() * 2f * wb

        // Left arm
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 14f, sy - 34f + bodyOffset, sx - 10f, sy - 14f + bodyOffset + armWalk, renderer.lighten(bodyColor, 1.1f), renderer.darken(bodyColor, 0.7f))
        } else {
            p.color = bodyColor
        }
        p.style = Paint.Style.FILL
        val leftArm = renderer.obtainPath()
        leftArm.moveTo(sx - 10f, sy - 34f + bodyOffset)
        leftArm.quadTo(sx - 14f, sy - 32f + bodyOffset, sx - 14f, sy - 26f + bodyOffset)
        leftArm.lineTo(sx - 12f, sy - 18f + bodyOffset + armWalk)
        leftArm.quadTo(sx - 12f, sy - 14f + bodyOffset + armWalk, sx - 10f, sy - 14f + bodyOffset + armWalk)
        leftArm.lineTo(sx - 9f, sy - 14f + bodyOffset + armWalk)
        leftArm.lineTo(sx - 10f, sy - 18f + bodyOffset + armWalk)
        leftArm.lineTo(sx - 10f, sy - 26f + bodyOffset)
        leftArm.quadTo(sx - 10f, sy - 32f + bodyOffset, sx - 10f, sy - 34f + bodyOffset)
        leftArm.close()
        canvas.drawPath(leftArm, p)
        renderer.recyclePath(leftArm)
        p.shader = null

        // Right arm
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx + 10f, sy - 34f + bodyOffset, sx + 14f, sy - 14f + bodyOffset - armWalk, bodyColor, renderer.darken(bodyColor, 0.65f))
        } else {
            p.color = bodyColor
        }
        p.style = Paint.Style.FILL
        val rightArm = renderer.obtainPath()
        rightArm.moveTo(sx + 10f, sy - 34f + bodyOffset)
        rightArm.quadTo(sx + 14f, sy - 32f + bodyOffset, sx + 14f, sy - 26f + bodyOffset)
        rightArm.lineTo(sx + 12f, sy - 18f + bodyOffset - armWalk)
        rightArm.quadTo(sx + 12f, sy - 14f + bodyOffset - armWalk, sx + 10f, sy - 14f + bodyOffset - armWalk)
        rightArm.lineTo(sx + 9f, sy - 14f + bodyOffset - armWalk)
        rightArm.lineTo(sx + 10f, sy - 18f + bodyOffset - armWalk)
        rightArm.lineTo(sx + 10f, sy - 26f + bodyOffset)
        rightArm.quadTo(sx + 10f, sy - 32f + bodyOffset, sx + 10f, sy - 34f + bodyOffset)
        rightArm.close()
        canvas.drawPath(rightArm, p)
        renderer.recyclePath(rightArm)
        p.shader = null

        // Shoulder pads
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx + 12f, sy - 33f + bodyOffset, 3f, Color.parseColor("#DD4444"), renderer.darken(Color.parseColor("#DD4444"), 0.6f))
        } else {
            p.color = Color.parseColor("#DD4444")
        }
        p.style = Paint.Style.FILL
        canvas.drawOval(RectF(sx + 9f, sy - 36f + bodyOffset, sx + 15f, sy - 30f + bodyOffset), p)
        p.shader = null

        // Gauntlets
        reset()
        p.color = Color.parseColor("#DD4444")
        p.style = Paint.Style.FILL
        canvas.drawOval(RectF(sx - 15f, sy - 18f + bodyOffset + armWalk, sx - 9f, sy - 14f + bodyOffset + armWalk), p)
        canvas.drawOval(RectF(sx + 9f, sy - 18f + bodyOffset - armWalk, sx + 15f, sy - 14f + bodyOffset - armWalk), p)

        // === HEAD (oval with spherical shading) ===
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 2f, sy - 46f + bodyOffset, 8f, renderer.lighten(Color.parseColor("#FFCC99"), 1.15f), renderer.darken(Color.parseColor("#FFCC99"), 0.7f))
        } else {
            p.color = Color.parseColor("#FFCC99")
        }
        p.style = Paint.Style.FILL
        canvas.drawOval(sx - 7f, sy - 50f + bodyOffset, sx + 7f, sy - 38f + bodyOffset, p)
        p.shader = null

        // === HAIR (curved Path with gradient) ===
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 8f, sy - 52f + bodyOffset, sx + 8f, sy - 40f + bodyOffset, Color.parseColor("#FFFFFF"), Color.parseColor("#CCCCCC"))
        } else {
            p.color = Color.parseColor("#FFFFFF")
        }
        p.style = Paint.Style.FILL
        val hairPath = renderer.obtainPath()
        hairPath.moveTo(sx - 7f, sy - 44f + bodyOffset)
        hairPath.quadTo(sx - 9f, sy - 52f + bodyOffset, sx, sy - 53f + bodyOffset)
        hairPath.quadTo(sx + 9f, sy - 52f + bodyOffset, sx + 7f, sy - 44f + bodyOffset)
        hairPath.lineTo(sx + 8f, sy - 46f + bodyOffset)
        hairPath.quadTo(sx + 10f, sy - 54f + bodyOffset, sx, sy - 55f + bodyOffset)
        hairPath.quadTo(sx - 10f, sy - 54f + bodyOffset, sx - 8f, sy - 46f + bodyOffset)
        hairPath.close()
        canvas.drawPath(hairPath, p)
        renderer.recyclePath(hairPath)
        p.shader = null

        // Side hair strands
        val hairWalk = walkSin * 1.5f * wb
        reset()
        p.color = Color.parseColor("#EEEEFF")
        p.style = Paint.Style.FILL
        val sideHairL = renderer.obtainPath()
        sideHairL.moveTo(sx - 7f, sy - 48f + bodyOffset)
        sideHairL.quadTo(sx - 11f + hairWalk, sy - 46f + bodyOffset, sx - 10f + hairWalk, sy - 40f + bodyOffset)
        sideHairL.lineTo(sx - 8f + hairWalk, sy - 40f + bodyOffset)
        sideHairL.quadTo(sx - 9f + hairWalk, sy - 46f + bodyOffset, sx - 6f, sy - 48f + bodyOffset)
        sideHairL.close()
        canvas.drawPath(sideHairL, p)
        renderer.recyclePath(sideHairL)
        val sideHairR = renderer.obtainPath()
        sideHairR.moveTo(sx + 7f, sy - 48f + bodyOffset)
        sideHairR.quadTo(sx + 11f - hairWalk, sy - 46f + bodyOffset, sx + 10f - hairWalk, sy - 42f + bodyOffset)
        sideHairR.lineTo(sx + 8f - hairWalk, sy - 42f + bodyOffset)
        sideHairR.quadTo(sx + 9f - hairWalk, sy - 46f + bodyOffset, sx + 6f, sy - 48f + bodyOffset)
        sideHairR.close()
        canvas.drawPath(sideHairR, p)
        renderer.recyclePath(sideHairR)

        // Hair highlight
        reset()
        p.color = Color.parseColor("#EEEEFF")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 4f, sy - 51f + bodyOffset, sx + 2f, sy - 46f + bodyOffset, p)

        // === FACE ===
        reset()
        p.color = Color.parseColor("#222266")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx + 3f, sy - 45f + bodyOffset, 1.5f, p)
        reset()
        p.color = Color.parseColor("#4444AA")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx + 3f, sy - 45.5f + bodyOffset, 0.8f, p)
        reset()
        p.color = Color.parseColor("#CC8866")
        p.style = Paint.Style.FILL
        canvas.drawPoint(sx + 2f, sy - 41f + bodyOffset, p)

        if (player.eternalCrown) {
            drawEternalCrown(canvas, sx, sy + bodyOffset, player)
        }

        // === SWORD (detailed blade with fuller and gradient) ===
        drawPlayerSword(canvas, sx, sy + bodyOffset, player)

        // === DASH TRAIL ===
        if (isDashing) {
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx, sy - 15f, 20f, Color.argb(80, 100, 150, 255), Color.argb(0, 100, 150, 255))
            } else {
                p.color = Color.argb(80, 100, 150, 255)
            }
            p.style = Paint.Style.FILL
            canvas.drawOval(sx - 18f, sy - 35f, sx + 18f, sy + 8f, p)
            p.shader = null
            reset()
            p.color = Color.argb(40, 150, 180, 255)
            p.style = Paint.Style.FILL
            canvas.drawOval(sx - 14f, sy - 30f, sx + 14f, sy + 4f, p)
        }

        // === RUNNING EFFECTS ===
        if (wb > 0.1f) {
            val dustAlpha = (wb * 60f).toInt().coerceIn(0, 60)
            reset()
            p.color = Color.argb(dustAlpha, 180, 160, 140)
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx - 6f, sy + 12f, 2.5f, p)
            canvas.drawCircle(sx + 6f, sy + 12f, 2.5f, p)
            reset()
            p.color = Color.argb(dustAlpha / 2, 200, 180, 160)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1f
            canvas.drawLine(sx - 16f, sy - 20f + bodyOffset, sx - 22f, sy - 18f + bodyOffset, p)
            canvas.drawLine(sx - 16f, sy - 10f + bodyOffset, sx - 22f, sy - 8f + bodyOffset, p)
        }

        // === IDLE GLOW ===
        if (isIdle && wb < 0.1f) {
            val glowPulse = (sin(player.idleTime * 2f) * 0.3f + 0.7f)
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx, sy - 25f + bodyOffset, 18f, Color.argb((25 * glowPulse).toInt(), 255, 200, 100), Color.argb(0, 255, 200, 100))
            } else {
                p.color = Color.argb((20 * glowPulse).toInt(), 255, 200, 100)
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 25f + bodyOffset, 18f, p)
            p.shader = null
        }

        // === CHARGING INDICATOR ===
        if (isCharging) {
            val chargeRatio = (player.chargeTime / 2.0f).coerceIn(0f, 1f)
            val r = 25f
            reset()
            p.color = Color.argb(60, 100, 100, 100)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 4f
            canvas.drawCircle(sx, sy - 25f, r, p)
            val arcColor = when {
                chargeRatio > 0.7f -> Color.parseColor("#FF4444")
                chargeRatio > 0.4f -> Color.parseColor("#FF8800")
                else -> Color.parseColor("#FFD700")
            }
            reset()
            p.color = arcColor
            p.style = Paint.Style.STROKE
            p.strokeWidth = 5f
            canvas.drawArc(sx - r, sy - 25f - r, sx + r, sy - 25f + r, -90f, chargeRatio * 360f, false, p)
            val pulse = (sin(player.chargeTime * 8f) * 0.3f + 0.7f)
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx, sy - 25f, r * chargeRatio * 0.8f, Color.argb((50 * pulse * chargeRatio).toInt(), 255, 200, 50), Color.argb(0, 255, 200, 50))
            } else {
                p.color = Color.argb((40 * pulse * chargeRatio).toInt(), 255, 200, 50)
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 25f, r * chargeRatio * 0.8f, p)
            p.shader = null
        }

        // === WHIRLWIND EFFECT (enhanced with gradient arcs) ===
        if (isWhirlwinding) {
            val wwAngle = player.whirlwindAngle
            val wwRadius = 55f
            for (i in 0..2) {
                val baseAngle = wwAngle + i * 2.094f
                val startDeg = Math.toDegrees(baseAngle.toDouble()).toFloat()
                // Outer arc with gradient
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(sx, sy - 25f, wwRadius, Color.argb(180, 255, 220, 80), Color.argb(60, 255, 220, 80))
                } else {
                    p.color = Color.argb(180, 255, 220, 80)
                }
                p.style = Paint.Style.STROKE
                p.strokeWidth = 4f
                canvas.drawArc(sx - wwRadius, sy - 25f - wwRadius, sx + wwRadius, sy - 25f + wwRadius, startDeg, 90f, false, p)
                p.shader = null
                // Inner glow
                reset()
                p.color = Color.argb(100, 255, 255, 150)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2f
                canvas.drawArc(sx - wwRadius * 0.7f, sy - 25f - wwRadius * 0.7f, sx + wwRadius * 0.7f, sy - 25f + wwRadius * 0.7f, startDeg, 90f, false, p)
            }
            // Center glow with radial gradient
            val wwPulse = (sin(wwAngle * 3f) * 0.3f + 0.7f)
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx, sy - 25f, 15f, Color.argb((80 * wwPulse).toInt(), 255, 220, 80), Color.argb(0, 255, 200, 50))
            } else {
                p.color = Color.argb((60 * wwPulse).toInt(), 255, 200, 50)
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 25f, 15f * wwPulse, p)
            p.shader = null
        }

        canvas.restore()

        // Death dissolve particles (enhanced with spiral)
        if (isDead) {
            val pAlpha = ((1f - deathProgress) * 200).toInt()
            val pSize = 2f + deathProgress * 4f
            for (i in 0..11) {
                val angle = renderer.globalTime * 3f + i * 0.52f
                val spiralR = 5f + deathProgress * 20f
                val px = sx + (cos(angle) * spiralR).toFloat()
                val py = sy - player.height / 2f + (sin(angle) * spiralR * 0.5f).toFloat() - deathProgress * 25f
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(px, py, pSize, Color.argb(pAlpha, 255, 200, 80), Color.argb(pAlpha / 3, 255, 150, 30))
                } else {
                    p.color = Color.argb(pAlpha, 255, 200, 80)
                }
                p.style = Paint.Style.FILL
                // Diamond-shaped soul fragment
                val frag = renderer.obtainPath()
                frag.moveTo(px, py - pSize)
                frag.lineTo(px + pSize, py)
                frag.lineTo(px, py + pSize)
                frag.lineTo(px - pSize, py)
                frag.close()
                canvas.drawPath(frag, p)
                renderer.recyclePath(frag)
                p.shader = null
            }
            if (deathProgress > 0.3f) {
                val fadeAlpha = ((deathProgress - 0.3f) / 0.7f * 160).toInt()
                reset()
                p.color = Color.argb(fadeAlpha, 5, 2, 15)
                p.style = Paint.Style.FILL
                val fadeScale = 1f - deathProgress * 0.5f
                canvas.drawCircle(sx, sy - player.height / 2f * fadeScale, player.width * 2f * fadeScale, p)
            }
        }

        // Health bar
        if (player.health < player.maxHealth && !isDead) {
            val barW = 30f
            val barH = 4f
            val hpRatio = player.health.toFloat() / player.maxHealth
            reset()
            p.color = Color.RED
            p.style = Paint.Style.FILL
            canvas.drawRect(sx - barW / 2, sy - 58f + bodyOffset, sx + barW / 2, sy - 58f + barH + bodyOffset, p)
            reset()
            if (useShaders) {
                p.shader = renderer.makeLinearGradient(sx - barW / 2, 0f, sx - barW / 2 + barW * hpRatio, 0f, Color.parseColor("#44FF44"), Color.parseColor("#22AA22"))
            } else {
                p.color = Color.GREEN
            }
            p.style = Paint.Style.FILL
            canvas.drawRect(sx - barW / 2, sy - 58f + bodyOffset, sx - barW / 2 + barW * hpRatio, sy - 58f + barH + bodyOffset, p)
            p.shader = null
            reset()
            p.color = Color.argb(100, 255, 255, 255)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1f
            canvas.drawRect(sx - barW / 2, sy - 58f + bodyOffset, sx + barW / 2, sy - 58f + barH + bodyOffset, p)
        }
    }

    private fun drawEternalCrown(canvas: Canvas, sx: Float, sy: Float, player: Player) {
        val crownY = sy - 68f + (sin(player.idleTime * 2.2f) * 1.4f).toFloat()
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, crownY, 20f, Color.argb(95, 255, 226, 95), Color.argb(0, 255, 210, 80))
        } else {
            p.color = Color.argb(80, 255, 226, 95)
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, crownY, 20f, p)
        p.shader = null

        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 13f, crownY - 9f, sx + 13f, crownY + 6f, Color.parseColor("#FFF2A8"), Color.parseColor("#D99A16"))
        } else {
            p.color = Color.parseColor("#FFD45A")
        }
        p.style = Paint.Style.FILL
        val crown = renderer.obtainPath()
        crown.moveTo(sx - 14f, crownY + 6f)
        crown.lineTo(sx - 12f, crownY - 8f)
        crown.lineTo(sx - 5f, crownY - 2f)
        crown.lineTo(sx, crownY - 12f)
        crown.lineTo(sx + 5f, crownY - 2f)
        crown.lineTo(sx + 12f, crownY - 8f)
        crown.lineTo(sx + 14f, crownY + 6f)
        crown.close()
        canvas.drawPath(crown, p)
        renderer.recyclePath(crown)
        p.shader = null

        reset()
        p.color = Color.parseColor("#D92323")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, crownY - 3f, 2.6f, p)
        canvas.drawCircle(sx - 9f, crownY + 2f, 2f, p)
        canvas.drawCircle(sx + 9f, crownY + 2f, 2f, p)

        reset()
        p.color = Color.argb(210, 255, 250, 200)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.4f
        canvas.drawLine(sx - 13f, crownY + 6f, sx + 13f, crownY + 6f, p)
    }

    private fun drawPlayerSword(canvas: Canvas, sx: Float, sy: Float, player: Player) {
        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.ROUND

        when {
            player.isAttacking1 -> {
                // Slash forward - blade with gradient
                reset()
                if (useShaders) {
                    p.shader = renderer.makeLinearGradient(sx + 10f, sy - 32f, sx + 38f, sy - 18f, Color.parseColor("#FFFFFF"), Color.parseColor("#AAAACC"))
                } else {
                    p.color = Color.parseColor("#EEEEEE")
                }
                p.style = Paint.Style.FILL
                val blade = renderer.obtainPath()
                blade.moveTo(sx + 10f, sy - 34f)
                blade.lineTo(sx + 38f, sy - 16f)
                blade.lineTo(sx + 36f, sy - 18f)
                blade.lineTo(sx + 12f, sy - 32f)
                blade.close()
                canvas.drawPath(blade, p)
                renderer.recyclePath(blade)
                p.shader = null
                // Blade edge highlight
                reset()
                p.color = Color.parseColor("#FFFFFF")
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.5f
                canvas.drawLine(sx + 12f, sy - 31f, sx + 36f, sy - 19f, p)
                // Handle with wrapping
                reset()
                p.color = Color.parseColor("#886633")
                p.style = Paint.Style.FILL
                canvas.drawRect(sx + 8f, sy - 34f, sx + 10f, sy - 28f, p)
                // Handle wrapping
                reset()
                p.color = Color.parseColor("#6B4423")
                p.style = Paint.Style.STROKE
                p.strokeWidth = 0.8f
                for (i in 0..2) {
                    val yy = sy - 33f + i * 2f
                    canvas.drawLine(sx + 8f, yy, sx + 10f, yy + 1f, p)
                }
                // Pommel with radial gradient
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(sx + 8f, sy - 35f, 2.5f, Color.parseColor("#FFE44D"), Color.parseColor("#B8860B"))
                } else {
                    p.color = Color.parseColor("#FFD700")
                }
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx + 8f, sy - 35f, 2f, p)
                p.shader = null
                // Slash effect arc with gradient
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(sx + 18f, sy - 22f, 30f, Color.argb(180, 255, 220, 100), Color.argb(0, 255, 200, 60))
                } else {
                    p.color = Color.argb(180, 255, 200, 100)
                }
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3f
                canvas.drawArc(sx - 5f, sy - 48f, sx + 42f, sy + 5f, -50f, 100f, false, p)
                p.shader = null
                reset()
                p.color = Color.argb(100, 255, 255, 200)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2f
                canvas.drawArc(sx, sy - 42f, sx + 36f, sy, -45f, 90f, false, p)
            }
            player.isAttacking2 -> {
                // Upward slash
                reset()
                if (useShaders) {
                    p.shader = renderer.makeLinearGradient(sx + 8f, sy - 16f, sx + 4f, sy - 48f, Color.parseColor("#FFFFFF"), Color.parseColor("#AAAACC"))
                } else {
                    p.color = Color.parseColor("#EEEEEE")
                }
                p.style = Paint.Style.STROKE
                p.strokeWidth = 4f
                canvas.drawLine(sx + 8f, sy - 16f, sx + 4f, sy - 48f, p)
                p.shader = null
                reset()
                p.color = Color.parseColor("#FFFFFF")
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2f
                canvas.drawLine(sx + 9f, sy - 18f, sx + 5f, sy - 46f, p)
                reset()
                p.color = Color.parseColor("#886633")
                p.style = Paint.Style.STROKE
                p.strokeWidth = 5f
                canvas.drawLine(sx + 9f, sy - 14f, sx + 8f, sy - 18f, p)
                reset()
                p.color = Color.parseColor("#FFD700")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx + 9f, sy - 13f, 2f, p)
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(sx + 6f, sy - 28f, 25f, Color.argb(180, 255, 200, 80), Color.argb(0, 255, 180, 40))
                } else {
                    p.color = Color.argb(180, 255, 180, 80)
                }
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3f
                canvas.drawArc(sx - 15f, sy - 55f, sx + 28f, sy, -65f, 130f, false, p)
                p.shader = null
            }
            player.isAttacking3 -> {
                // Spin slash
                reset()
                if (useShaders) {
                    p.shader = renderer.makeLinearGradient(sx - 18f, sy - 26f, sx + 22f, sy - 26f, Color.parseColor("#FFFFFF"), Color.parseColor("#BBBBCC"))
                } else {
                    p.color = Color.parseColor("#EEEEEE")
                }
                p.style = Paint.Style.STROKE
                p.strokeWidth = 5f
                canvas.drawLine(sx - 18f, sy - 26f, sx + 22f, sy - 26f, p)
                p.shader = null
                reset()
                p.color = Color.parseColor("#FFFFFF")
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2f
                canvas.drawLine(sx - 16f, sy - 27f, sx + 20f, sy - 27f, p)
                reset()
                p.color = Color.parseColor("#886633")
                p.style = Paint.Style.STROKE
                p.strokeWidth = 6f
                canvas.drawLine(sx - 20f, sy - 26f, sx - 18f, sy - 26f, p)
                // Spin effect with radial gradient
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(sx, sy - 26f, 28f, Color.argb(200, 255, 180, 50), Color.argb(0, 255, 150, 30))
                } else {
                    p.color = Color.argb(200, 255, 150, 50)
                }
                p.style = Paint.Style.STROKE
                p.strokeWidth = 4f
                canvas.drawCircle(sx, sy - 26f, 28f, p)
                p.shader = null
                reset()
                p.color = Color.argb(120, 255, 220, 100)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2f
                canvas.drawCircle(sx, sy - 26f, 22f, p)
            }
            else -> {
                // Idle sword - detailed blade with fuller
                reset()
                if (useShaders) {
                    p.shader = renderer.makeLinearGradient(sx + 12f, sy - 22f, sx + 16f, sy - 2f, Color.parseColor("#E0E0E8"), Color.parseColor("#999AAA"))
                } else {
                    p.color = Color.parseColor("#CCCCCC")
                }
                p.style = Paint.Style.FILL
                val idleBlade = renderer.obtainPath()
                idleBlade.moveTo(sx + 13f, sy - 22f)
                idleBlade.lineTo(sx + 17f, sy - 4f)
                idleBlade.lineTo(sx + 16f, sy - 2f)
                idleBlade.lineTo(sx + 15f, sy - 4f)
                idleBlade.lineTo(sx + 11f, sy - 22f)
                idleBlade.close()
                canvas.drawPath(idleBlade, p)
                renderer.recyclePath(idleBlade)
                p.shader = null
                // Fuller (blood groove)
                reset()
                p.color = Color.parseColor("#888899")
                p.style = Paint.Style.STROKE
                p.strokeWidth = 0.6f
                canvas.drawLine(sx + 14f, sy - 18f, sx + 15.5f, sy - 4f, p)
                // Handle
                reset()
                p.color = Color.parseColor("#886633")
                p.style = Paint.Style.FILL
                canvas.drawRect(sx + 10f, sy - 24f, sx + 18f, sy - 20f, p)
                // Handle wrapping
                reset()
                p.color = Color.parseColor("#6B4423")
                p.style = Paint.Style.STROKE
                p.strokeWidth = 0.6f
                for (i in 0..2) {
                    canvas.drawLine(sx + 10f, sy - 23.5f + i * 1.5f, sx + 18f, sy - 24f + i * 1.5f, p)
                }
                // Pommel gem
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(sx + 14f, sy - 22f, 2f, Color.parseColor("#FF6666"), Color.parseColor("#CC0000"))
                } else {
                    p.color = Color.parseColor("#FF4444")
                }
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx + 14f, sy - 22f, 1.5f, p)
                p.shader = null
                // Cross guard with gradient
                reset()
                if (useShaders) {
                    p.shader = renderer.makeLinearGradient(sx + 9f, 0f, sx + 19f, 0f, Color.parseColor("#FFE44D"), Color.parseColor("#B8860B"))
                } else {
                    p.color = Color.parseColor("#FFD700")
                }
                p.style = Paint.Style.FILL
                val guard = renderer.obtainPath()
                guard.moveTo(sx + 9f, sy - 20f)
                guard.quadTo(sx + 9f, sy - 18f, sx + 10f, sy - 18f)
                guard.lineTo(sx + 18f, sy - 18f)
                guard.quadTo(sx + 19f, sy - 18f, sx + 19f, sy - 20f)
                guard.lineTo(sx + 9f, sy - 20f)
                guard.close()
                canvas.drawPath(guard, p)
                renderer.recyclePath(guard)
                p.shader = null
            }
        }
        p.strokeWidth = 1f
        p.strokeCap = Paint.Cap.BUTT
    }

    private fun reset() {
        renderer.resetPaint()
    }
}
