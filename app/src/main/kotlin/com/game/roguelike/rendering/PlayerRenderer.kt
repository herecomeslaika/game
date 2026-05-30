package com.game.roguelike.rendering

import android.content.Context
import android.graphics.*
import com.game.roguelike.core.PlayerState
import com.game.roguelike.entity.Player
import kotlin.math.sin
import kotlin.math.cos

class PlayerRenderer(private val renderer: IsometricRenderer, private val context: Context) {

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

        // Death animation: compute progress
        val deathProgress = if (isDead) {
            (player.deathTimer / player.deathDuration).coerceIn(0f, 1f)
        } else 0f

        canvas.save()

        // Death animation: lean back, shrink, rise
        if (isDead) {
            val leanAngle = deathProgress * 25f  // lean back 25°
            val scale = 1f - deathProgress * 0.5f  // shrink to 50%
            canvas.translate(sx, sy)
            canvas.rotate(-leanAngle)
            canvas.scale(scale, scale)
            canvas.translate(-sx, -sy)
        } else if (!facingRight) {
            canvas.scale(-1f, 1f, sx, sy)
        }

        // Smooth animation using walkBlend for transitions
        val wb = player.walkBlend
        val phase = player.moveAnimPhase
        val walkSin = sin(phase * Math.PI * 2).toFloat()

        // Walk bob scales with blend (2px max, gentle)
        val bob = walkSin * 2f * wb
        // Leg swing scales with blend (4px max)
        val legSwing = walkSin * 4f * wb
        // Idle breathing (separate, always active)
        val breathe = if (isIdle) (sin(player.idleTime * 2.5f) * 1f).toFloat() else 0f
        val bodyOffset = bob + breathe

        // Flash when hurt
        val bodyColor = if (isHurt && player.hurtTimer % 0.1f < 0.05f) Color.WHITE else Color.parseColor("#CC3333")

        // === LEGS ===
        renderer.paint.style = Paint.Style.FILL
        renderer.paint.color = Color.parseColor("#992222")
        canvas.drawRect(sx - 8f, sy - 6f, sx - 2f, sy + 8f + legSwing, renderer.paint)
        canvas.drawRect(sx + 2f, sy - 6f, sx + 8f, sy + 8f - legSwing, renderer.paint)
        // Boots
        renderer.paint.color = Color.parseColor("#661111")
        canvas.drawRect(sx - 9f, sy + 5f + legSwing, sx - 1f, sy + 10f + legSwing, renderer.paint)
        canvas.drawRect(sx + 1f, sy + 5f - legSwing, sx + 9f, sy + 10f - legSwing, renderer.paint)

        // === CAPE ===
        renderer.paint.color = Color.parseColor("#881111")
        val capeWalk = walkSin * 3f * wb
        val capeIdle = if (isIdle) (sin(player.idleTime * 1.5f) * 1f).toFloat() else 0f
        val capeSwing = capeWalk + capeIdle
        canvas.drawRect(sx - 13f + capeSwing, sy - 36f + bodyOffset, sx - 6f + capeSwing * 0.5f, sy - 8f + bodyOffset, renderer.paint)
        canvas.drawRect(sx + 6f - capeSwing * 0.5f, sy - 36f + bodyOffset, sx + 13f - capeSwing, sy - 8f + bodyOffset, renderer.paint)
        renderer.paint.color = Color.parseColor("#AA2222")
        canvas.drawRect(sx - 13f + capeSwing, sy - 36f + bodyOffset, sx - 12f + capeSwing, sy - 8f + bodyOffset, renderer.paint)
        canvas.drawRect(sx + 12f - capeSwing, sy - 36f + bodyOffset, sx + 13f - capeSwing, sy - 8f + bodyOffset, renderer.paint)

        // === BODY (torso) ===
        renderer.paint.color = bodyColor
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(sx - 10f, sy - 38f + bodyOffset, sx + 10f, sy - 6f + bodyOffset, renderer.paint)
        renderer.paint.color = Color.parseColor("#DD4444")
        canvas.drawRect(sx - 8f, sy - 36f + bodyOffset, sx + 8f, sy - 22f + bodyOffset, renderer.paint)
        renderer.paint.color = Color.parseColor("#EE6666")
        canvas.drawRect(sx - 8f, sy - 36f + bodyOffset, sx + 8f, sy - 34f + bodyOffset, renderer.paint)
        renderer.paint.color = Color.parseColor("#886633")
        canvas.drawRect(sx - 10f, sy - 10f + bodyOffset, sx + 10f, sy - 6f + bodyOffset, renderer.paint)
        renderer.paint.color = Color.parseColor("#FFD700")
        canvas.drawRect(sx - 2f, sy - 10f + bodyOffset, sx + 2f, sy - 6f + bodyOffset, renderer.paint)

        // === ARMS ===
        renderer.paint.color = bodyColor
        val armWalk = sin(phase * Math.PI * 2 + 0.5f).toFloat() * 2f * wb
        canvas.drawRect(sx - 14f, sy - 34f + bodyOffset, sx - 10f, sy - 14f + bodyOffset + armWalk, renderer.paint)
        canvas.drawRect(sx + 10f, sy - 34f + bodyOffset, sx + 14f, sy - 14f + bodyOffset - armWalk, renderer.paint)
        renderer.paint.color = Color.parseColor("#DD4444")
        canvas.drawRect(sx - 15f, sy - 18f + bodyOffset + armWalk, sx - 9f, sy - 14f + bodyOffset + armWalk, renderer.paint)
        canvas.drawRect(sx + 9f, sy - 18f + bodyOffset - armWalk, sx + 15f, sy - 14f + bodyOffset - armWalk, renderer.paint)

        // === HEAD ===
        renderer.paint.color = Color.parseColor("#FFCC99")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawOval(sx - 7f, sy - 50f + bodyOffset, sx + 7f, sy - 38f + bodyOffset, renderer.paint)

        // === HAIR ===
        renderer.paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(sx - 8f, sy - 52f + bodyOffset, sx + 8f, sy - 44f + bodyOffset, renderer.paint)
        val hairWalk = walkSin * 1.5f * wb
        canvas.drawRect(sx - 10f + hairWalk, sy - 50f + bodyOffset, sx - 6f + hairWalk, sy - 40f + bodyOffset, renderer.paint)
        canvas.drawRect(sx + 6f - hairWalk, sy - 50f + bodyOffset, sx + 10f - hairWalk, sy - 42f + bodyOffset, renderer.paint)
        renderer.paint.color = Color.parseColor("#EEEEFF")
        canvas.drawRect(sx - 4f, sy - 51f + bodyOffset, sx + 2f, sy - 46f + bodyOffset, renderer.paint)

        // === FACE ===
        renderer.paint.color = Color.parseColor("#222266")
        canvas.drawCircle(sx + 3f, sy - 45f + bodyOffset, 1.5f, renderer.paint)
        renderer.paint.color = Color.parseColor("#4444AA")
        canvas.drawCircle(sx + 3f, sy - 45.5f + bodyOffset, 0.8f, renderer.paint)
        renderer.paint.color = Color.parseColor("#CC8866")
        canvas.drawPoint(sx + 2f, sy - 41f + bodyOffset, renderer.paint)

        // === SWORD ===
        drawPlayerSword(canvas, sx, sy + bodyOffset, player)

        // === DASH TRAIL ===
        if (isDashing) {
            renderer.paint.color = Color.argb(80, 100, 150, 255)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawOval(sx - 18f, sy - 35f, sx + 18f, sy + 8f, renderer.paint)
            renderer.paint.color = Color.argb(40, 150, 180, 255)
            canvas.drawOval(sx - 14f, sy - 30f, sx + 14f, sy + 4f, renderer.paint)
        }

        // === RUNNING EFFECTS ===
        if (wb > 0.1f) {
            val dustAlpha = (wb * 60f).toInt().coerceIn(0, 60)
            renderer.paint.color = Color.argb(dustAlpha, 180, 160, 140)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx - 6f, sy + 12f, 2.5f, renderer.paint)
            canvas.drawCircle(sx + 6f, sy + 12f, 2.5f, renderer.paint)
            renderer.paint.color = Color.argb(dustAlpha / 2, 200, 180, 160)
            renderer.paint.style = Paint.Style.STROKE
            renderer.paint.strokeWidth = 1f
            canvas.drawLine(sx - 16f, sy - 20f + bodyOffset, sx - 22f, sy - 18f + bodyOffset, renderer.paint)
            canvas.drawLine(sx - 16f, sy - 10f + bodyOffset, sx - 22f, sy - 8f + bodyOffset, renderer.paint)
        }

        // === IDLE GLOW ===
        if (isIdle && wb < 0.1f) {
            val glowPulse = (sin(player.idleTime * 2f) * 0.3f + 0.7f)
            renderer.paint.color = Color.argb((20 * glowPulse).toInt(), 255, 200, 100)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 25f + bodyOffset, 18f, renderer.paint)
        }

        // === CHARGING INDICATOR ===
        if (isCharging) {
            val chargeRatio = (player.chargeTime / 2.0f).coerceIn(0f, 1f)
            val r = 25f
            // Background ring
            renderer.paint.color = Color.argb(60, 100, 100, 100)
            renderer.paint.style = Paint.Style.STROKE
            renderer.paint.strokeWidth = 4f
            canvas.drawCircle(sx, sy - 25f, r, renderer.paint)
            // Progress arc
            val arcColor = when {
                chargeRatio > 0.7f -> Color.parseColor("#FF4444")
                chargeRatio > 0.4f -> Color.parseColor("#FF8800")
                else -> Color.parseColor("#FFD700")
            }
            renderer.paint.color = arcColor
            renderer.paint.strokeWidth = 5f
            canvas.drawArc(sx - r, sy - 25f - r, sx + r, sy - 25f + r,
                -90f, chargeRatio * 360f, false, renderer.paint)
            // Glow pulse
            val pulse = (sin(player.chargeTime * 8f) * 0.3f + 0.7f)
            renderer.paint.color = Color.argb((40 * pulse * chargeRatio).toInt(), 255, 200, 50)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 25f, r * chargeRatio * 0.8f, renderer.paint)
        }

        // === WHIRLWIND EFFECT ===
        if (isWhirlwinding) {
            val wwAngle = player.whirlwindAngle
            val wwRadius = 55f
            // Spinning blade arcs
            for (i in 0..2) {
                val baseAngle = wwAngle + i * 2.094f  // 120° apart
                val startDeg = Math.toDegrees(baseAngle.toDouble()).toFloat()
                renderer.paint.color = Color.argb(180, 255, 220, 80)
                renderer.paint.style = Paint.Style.STROKE
                renderer.paint.strokeWidth = 4f
                canvas.drawArc(sx - wwRadius, sy - 25f - wwRadius, sx + wwRadius, sy - 25f + wwRadius,
                    startDeg, 90f, false, renderer.paint)
                // Inner glow
                renderer.paint.color = Color.argb(100, 255, 255, 150)
                renderer.paint.strokeWidth = 2f
                canvas.drawArc(sx - wwRadius * 0.7f, sy - 25f - wwRadius * 0.7f,
                    sx + wwRadius * 0.7f, sy - 25f + wwRadius * 0.7f,
                    startDeg, 90f, false, renderer.paint)
            }
            // Center glow
            val wwPulse = (sin(wwAngle * 3f) * 0.3f + 0.7f)
            renderer.paint.color = Color.argb((60 * wwPulse).toInt(), 255, 200, 50)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 25f, 15f * wwPulse, renderer.paint)
        }

        canvas.restore()

        // Death dissolve particles and fade overlay
        if (isDead) {
            val pAlpha = ((1f - deathProgress) * 200).toInt()
            val pSize = 2f + deathProgress * 4f
            // Soul particles (gold/white rising)
            for (i in 0..6) {
                val px = sx + sin(renderer.globalTime * 5f + i * 1.5f) * (8f + deathProgress * 15f)
                val py = sy - player.height / 2f + cos(renderer.globalTime * 4f + i * 2f) * (10f + deathProgress * 20f) - deathProgress * 20f
                renderer.paint.color = Color.argb(pAlpha, 255, 200, 80)
                renderer.paint.style = Paint.Style.FILL
                canvas.drawCircle(px, py, pSize, renderer.paint)
            }
            // Fade overlay
            if (deathProgress > 0.3f) {
                val fadeAlpha = ((deathProgress - 0.3f) / 0.7f * 160).toInt()
                renderer.paint.color = Color.argb(fadeAlpha, 5, 2, 15)
                renderer.paint.style = Paint.Style.FILL
                val fadeScale = 1f - deathProgress * 0.5f
                canvas.drawCircle(sx, sy - player.height / 2f * fadeScale, player.width * 2f * fadeScale, renderer.paint)
            }
        }

        // Health bar above player (not flipped)
        if (player.health < player.maxHealth && !isDead) {
            val barW = 30f
            val barH = 4f
            val hpRatio = player.health.toFloat() / player.maxHealth
            renderer.paint.color = Color.RED
            renderer.paint.style = Paint.Style.FILL
            canvas.drawRect(sx - barW / 2, sy - 58f + bodyOffset, sx + barW / 2, sy - 58f + barH + bodyOffset, renderer.paint)
            renderer.paint.color = Color.GREEN
            canvas.drawRect(sx - barW / 2, sy - 58f + bodyOffset, sx - barW / 2 + barW * hpRatio, sy - 58f + barH + bodyOffset, renderer.paint)
            // Health bar border
            renderer.paint.color = Color.argb(100, 255, 255, 255)
            renderer.paint.style = Paint.Style.STROKE
            renderer.paint.strokeWidth = 1f
            canvas.drawRect(sx - barW / 2, sy - 58f + bodyOffset, sx + barW / 2, sy - 58f + barH + bodyOffset, renderer.paint)
        }
    }

    private fun drawPlayerSword(canvas: Canvas, sx: Float, sy: Float, player: Player) {
        renderer.paint.style = Paint.Style.FILL
        renderer.paint.strokeCap = Paint.Cap.ROUND

        when {
            player.isAttacking1 -> {
                // Slash forward - blade
                renderer.paint.color = Color.parseColor("#EEEEEE")
                renderer.paint.strokeWidth = 4f
                canvas.drawLine(sx + 10f, sy - 32f, sx + 38f, sy - 18f, renderer.paint)
                // Blade edge highlight
                renderer.paint.color = Color.parseColor("#FFFFFF")
                renderer.paint.strokeWidth = 2f
                canvas.drawLine(sx + 12f, sy - 31f, sx + 36f, sy - 19f, renderer.paint)
                // Handle
                renderer.paint.color = Color.parseColor("#886633")
                renderer.paint.strokeWidth = 5f
                canvas.drawLine(sx + 8f, sy - 34f, sx + 10f, sy - 28f, renderer.paint)
                // Pommel
                renderer.paint.color = Color.parseColor("#FFD700")
                renderer.paint.style = Paint.Style.FILL
                canvas.drawCircle(sx + 8f, sy - 35f, 2f, renderer.paint)
                // Slash effect arc
                renderer.paint.color = Color.argb(180, 255, 200, 100)
                renderer.paint.style = Paint.Style.STROKE
                renderer.paint.strokeWidth = 3f
                canvas.drawArc(sx - 5f, sy - 48f, sx + 42f, sy + 5f, -50f, 100f, false, renderer.paint)
                // Inner arc glow
                renderer.paint.color = Color.argb(100, 255, 255, 200)
                renderer.paint.strokeWidth = 2f
                canvas.drawArc(sx, sy - 42f, sx + 36f, sy, -45f, 90f, false, renderer.paint)
            }
            player.isAttacking2 -> {
                // Upward slash - blade
                renderer.paint.color = Color.parseColor("#EEEEEE")
                renderer.paint.strokeWidth = 4f
                canvas.drawLine(sx + 8f, sy - 16f, sx + 4f, sy - 48f, renderer.paint)
                // Blade highlight
                renderer.paint.color = Color.parseColor("#FFFFFF")
                renderer.paint.strokeWidth = 2f
                canvas.drawLine(sx + 9f, sy - 18f, sx + 5f, sy - 46f, renderer.paint)
                // Handle
                renderer.paint.color = Color.parseColor("#886633")
                renderer.paint.strokeWidth = 5f
                canvas.drawLine(sx + 9f, sy - 14f, sx + 8f, sy - 18f, renderer.paint)
                // Pommel
                renderer.paint.color = Color.parseColor("#FFD700")
                renderer.paint.style = Paint.Style.FILL
                canvas.drawCircle(sx + 9f, sy - 13f, 2f, renderer.paint)
                // Slash arc
                renderer.paint.color = Color.argb(180, 255, 180, 80)
                renderer.paint.style = Paint.Style.STROKE
                renderer.paint.strokeWidth = 3f
                canvas.drawArc(sx - 15f, sy - 55f, sx + 28f, sy, -65f, 130f, false, renderer.paint)
            }
            player.isAttacking3 -> {
                // Spin slash - blade
                renderer.paint.color = Color.parseColor("#EEEEEE")
                renderer.paint.strokeWidth = 5f
                canvas.drawLine(sx - 18f, sy - 26f, sx + 22f, sy - 26f, renderer.paint)
                // Blade highlight
                renderer.paint.color = Color.parseColor("#FFFFFF")
                renderer.paint.strokeWidth = 2f
                canvas.drawLine(sx - 16f, sy - 27f, sx + 20f, sy - 27f, renderer.paint)
                // Handle
                renderer.paint.color = Color.parseColor("#886633")
                renderer.paint.strokeWidth = 6f
                canvas.drawLine(sx - 20f, sy - 26f, sx - 18f, sy - 26f, renderer.paint)
                // Spin effect
                renderer.paint.color = Color.argb(200, 255, 150, 50)
                renderer.paint.style = Paint.Style.STROKE
                renderer.paint.strokeWidth = 4f
                canvas.drawCircle(sx, sy - 26f, 28f, renderer.paint)
                // Inner glow
                renderer.paint.color = Color.argb(120, 255, 220, 100)
                renderer.paint.strokeWidth = 2f
                canvas.drawCircle(sx, sy - 26f, 22f, renderer.paint)
            }
            else -> {
                // Idle sword - held at side
                renderer.paint.color = Color.parseColor("#CCCCCC")
                renderer.paint.strokeWidth = 3f
                canvas.drawLine(sx + 12f, sy - 22f, sx + 16f, sy - 2f, renderer.paint)
                // Blade tip
                renderer.paint.color = Color.parseColor("#EEEEEE")
                renderer.paint.strokeWidth = 2f
                canvas.drawLine(sx + 15f, sy - 6f, sx + 16f, sy - 2f, renderer.paint)
                // Handle
                renderer.paint.color = Color.parseColor("#886633")
                renderer.paint.style = Paint.Style.FILL
                canvas.drawRect(sx + 10f, sy - 24f, sx + 18f, sy - 20f, renderer.paint)
                // Pommel gem
                renderer.paint.color = Color.parseColor("#FF4444")
                canvas.drawCircle(sx + 14f, sy - 22f, 1.5f, renderer.paint)
                // Cross guard
                renderer.paint.color = Color.parseColor("#FFD700")
                canvas.drawRect(sx + 9f, sy - 20f, sx + 19f, sy - 18f, renderer.paint)
            }
        }
        renderer.paint.strokeWidth = 1f
        renderer.paint.strokeCap = Paint.Cap.BUTT
    }
}
