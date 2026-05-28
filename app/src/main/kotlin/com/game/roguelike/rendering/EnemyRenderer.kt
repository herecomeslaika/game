package com.game.roguelike.rendering

import android.graphics.*
import com.game.roguelike.core.EnemyState
import com.game.roguelike.entity.Enemy
import com.game.roguelike.entity.EnemyType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class EnemyRenderer(private val renderer: IsometricRenderer) {

    fun renderEnemy(canvas: Canvas, enemy: Enemy) {
        val (sx, sy) = renderer.worldToScreen(enemy.position)

        // Attack telegraph effects (drawn before body, in world space)
        drawAttackTelegraphs(canvas, enemy, sx, sy)

        canvas.save()
        if (!enemy.facingRight) {
            canvas.scale(-1f, 1f, sx, sy)
        }

        val isMoving = enemy.stateMachine.currentState == EnemyState.CHASE || enemy.stateMachine.currentState == EnemyState.PATROL
        val isIdle = enemy.stateMachine.currentState == EnemyState.IDLE
        val isHurt = enemy.stateMachine.currentState == EnemyState.HURT
        val wb = enemy.walkBlend
        val walkSin = sin(enemy.moveAnimPhase * Math.PI * 2).toFloat()
        val bob = walkSin * 2f * wb
        val breathe = if (isIdle) (sin(enemy.idleTime * 2f) * 1f).toFloat() else 0f
        val bodyOffset = bob + breathe
        val legSwing = walkSin * 3f * wb

        // Hurt flash
        val hurtFlash = isHurt && enemy.stateTimer % 0.1f < 0.05f

        // Phase transition flash
        val phaseFlash = enemy.isPhaseTransitioning

        // Enemy body - drawn by type
        when (enemy.type) {
            EnemyType.SKELETON -> drawSkeleton(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
            EnemyType.WRAITH -> drawWraith(canvas, sx, sy, bodyOffset, isMoving, isIdle, hurtFlash)
            EnemyType.MEGA_SKELETON -> drawMegaSkeleton(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
            EnemyType.FLAME_DANCER -> drawFlameDancer(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
            EnemyType.LAVA_CASTER -> drawLavaCaster(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
            EnemyType.INFERNO_TITAN -> drawInfernoTitan(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
            EnemyType.SHIELD_BEARER -> drawShieldBearer(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash, enemy.shieldDirection, enemy.shieldThrown)
            EnemyType.SPEAR_THROWER -> drawSpearThrower(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
            EnemyType.CHAMPION -> drawChampion(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash, enemy.shieldThrown)
        }

        // Phase transition white flash overlay
        if (phaseFlash) {
            renderer.paint.color = Color.argb(200, 255, 255, 255)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - enemy.height / 2f, enemy.width * 2f, renderer.paint)
        }

        // PREPARE_ATTACK wind-up glow
        if (enemy.stateMachine.currentState == EnemyState.PREPARE_ATTACK) {
            val glowPulse = sin(renderer.globalTime * 10f) * 0.3f + 0.7f
            renderer.paint.color = Color.argb((60 * glowPulse).toInt(), 255, 50, 50)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - enemy.height / 2f, enemy.width * 1.5f, renderer.paint)
        }

        // Flame dash glow
        if (enemy.isFlameDashing) {
            renderer.paint.color = Color.argb(80, 255, 100, 0)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - enemy.height / 2f, enemy.width * 2f, renderer.paint)
        }

        // Shield bash glow
        if (enemy.isShieldBashing) {
            renderer.paint.color = Color.argb(80, 100, 150, 255)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - enemy.height / 2f, enemy.width * 2f, renderer.paint)
        }

        // Dodge roll afterimage
        if (enemy.isDodging) {
            renderer.paint.color = Color.argb(60, 200, 200, 255)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawOval(sx - 16f, sy - 30f, sx + 16f, sy + 6f, renderer.paint)
        }

        canvas.restore()

        // Health bar
        if (enemy.health < enemy.maxHealth) {
            val barW = enemy.width.toFloat()
            val barH = 4f
            val hpRatio = enemy.health.toFloat() / enemy.maxHealth
            renderer.paint.color = Color.RED
            renderer.paint.style = Paint.Style.FILL
            canvas.drawRect(sx - barW / 2, sy - enemy.height - 8f, sx + barW / 2, sy - enemy.height - 8f + barH, renderer.paint)
            renderer.paint.color = Color.GREEN
            canvas.drawRect(sx - barW / 2, sy - enemy.height - 8f, sx - barW / 2 + barW * hpRatio, sy - enemy.height - 8f + barH, renderer.paint)
            // Border
            renderer.paint.color = Color.argb(80, 255, 255, 255)
            renderer.paint.style = Paint.Style.STROKE
            renderer.paint.strokeWidth = 1f
            canvas.drawRect(sx - barW / 2, sy - enemy.height - 8f, sx + barW / 2, sy - enemy.height - 8f + barH, renderer.paint)
        }

        // Boss name
        if (enemy.isBoss) {
            renderer.textPaint.color = Color.parseColor("#FF6644")
            renderer.textPaint.textSize = 28f
            canvas.drawText(enemy.name, sx - renderer.textPaint.measureText(enemy.name) / 2, sy - enemy.height - 16f, renderer.textPaint)
        }
    }

    private fun drawAttackTelegraphs(canvas: Canvas, enemy: Enemy, sx: Float, sy: Float) {
        // Ground slam telegraph: red expanding circle under Mega Skeleton
        if (enemy.isGroundSlamming && enemy.groundSlamPhase == 0) {
            val slamRadius = if (enemy.phase >= 2) 100f else 80f
            val progress = 1f - enemy.groundSlamHoverTimer / 0.5f
            val radius = slamRadius * progress
            renderer.paint.color = Color.argb(60, 255, 50, 50)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy, radius, renderer.paint)
            renderer.paint.color = Color.argb(120, 255, 100, 100)
            renderer.paint.style = Paint.Style.STROKE
            renderer.paint.strokeWidth = 2f
            canvas.drawCircle(sx, sy, radius, renderer.paint)
            renderer.paint.strokeWidth = 1f
            renderer.paint.style = Paint.Style.FILL
        }

        // Meteor telegraph: orange pulsing circle at target position
        if (enemy.isCastingMeteor) {
            val (mx, my) = renderer.worldToScreen(enemy.meteorTargetPos)
            val progress = 1f - enemy.meteorCastTimer / 1.2f
            val radius = 30f + progress * 30f
            val pulse = sin(renderer.globalTime * 6f) * 5f
            renderer.paint.color = Color.argb(80, 255, 100, 0)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(mx, my, radius + pulse, renderer.paint)
            renderer.paint.color = Color.argb(160, 255, 150, 0)
            renderer.paint.style = Paint.Style.STROKE
            renderer.paint.strokeWidth = 3f
            canvas.drawCircle(mx, my, radius, renderer.paint)
            // Inner warning ring
            renderer.paint.color = Color.argb(200, 255, 200, 50)
            renderer.paint.strokeWidth = 2f
            canvas.drawCircle(mx, my, 20f, renderer.paint)
            renderer.paint.strokeWidth = 1f
            renderer.paint.style = Paint.Style.FILL
        }
    }

    private fun drawSkeleton(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        renderer.paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs (bone-like)
        renderer.paint.color = flash ?: Color.parseColor("#B0A480")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 2f, sy + 6f + legSwing, renderer.paint)
        canvas.drawRect(sx + 2f, sy - 4f, sx + 6f, sy + 6f - legSwing, renderer.paint)
        // Feet
        renderer.paint.color = flash ?: Color.parseColor("#908060")
        canvas.drawRect(sx - 7f, sy + 4f + legSwing, sx - 1f, sy + 8f + legSwing, renderer.paint)
        canvas.drawRect(sx + 1f, sy + 4f - legSwing, sx + 7f, sy + 8f - legSwing, renderer.paint)

        // Ribcage body
        renderer.paint.color = flash ?: Color.parseColor("#D4C8A0")
        canvas.drawRect(sx - 8f, sy - 30f + bob, sx + 8f, sy - 4f + bob, renderer.paint)
        // Rib lines
        renderer.paint.color = flash ?: Color.parseColor("#B0A480")
        for (i in 0..2) {
            val ribY = sy - 26f + i * 7f + bob
            canvas.drawLine(sx - 6f, ribY, sx + 6f, ribY, renderer.paint)
        }
        // Spine
        canvas.drawLine(sx, sy - 28f + bob, sx, sy - 6f + bob, renderer.paint)

        // Arms
        renderer.paint.color = flash ?: Color.parseColor("#C4B890")
        canvas.drawRect(sx - 12f, sy - 26f + bob, sx - 8f, sy - 10f + bob, renderer.paint)
        canvas.drawRect(sx + 8f, sy - 26f + bob, sx + 12f, sy - 10f + bob, renderer.paint)

        // Skull
        renderer.paint.color = flash ?: Color.parseColor("#E8DCC0")
        canvas.drawCircle(sx, sy - 36f + bob, 8f, renderer.paint)
        // Jaw
        renderer.paint.color = flash ?: Color.parseColor("#D8CCB0")
        canvas.drawRect(sx - 5f, sy - 32f + bob, sx + 5f, sy - 29f + bob, renderer.paint)
        // Eye sockets
        renderer.paint.color = Color.RED
        canvas.drawCircle(sx - 3f, sy - 37f + bob, 2.5f, renderer.paint)
        canvas.drawCircle(sx + 3f, sy - 37f + bob, 2.5f, renderer.paint)
        // Eye glow
        renderer.paint.color = Color.parseColor("#FF4444")
        canvas.drawCircle(sx - 3f, sy - 37f + bob, 1.2f, renderer.paint)
        canvas.drawCircle(sx + 3f, sy - 37f + bob, 1.2f, renderer.paint)
        // Nose hole
        renderer.paint.color = flash ?: Color.parseColor("#A09070")
        canvas.drawPoint(sx, sy - 34f + bob, renderer.paint)

        // Sword
        renderer.paint.color = Color.parseColor("#888888")
        renderer.paint.strokeWidth = 2f
        canvas.drawLine(sx + 12f, sy - 20f + bob, sx + 22f, sy - 8f + bob, renderer.paint)
        // Sword edge
        renderer.paint.color = Color.parseColor("#AAAAAA")
        renderer.paint.strokeWidth = 1f
        canvas.drawLine(sx + 13f, sy - 19f + bob, sx + 21f, sy - 9f + bob, renderer.paint)
        // Handle
        renderer.paint.color = Color.parseColor("#554433")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(sx + 10f, sy - 22f + bob, sx + 14f, sy - 18f + bob, renderer.paint)

        // Idle sway
        if (isIdle) {
            renderer.paint.color = Color.argb(30, 255, 100, 100)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 20f + bob, 12f, renderer.paint)
        }
    }

    private fun drawWraith(canvas: Canvas, sx: Float, sy: Float, bob: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        renderer.paint.style = Paint.Style.FILL
        val floatBob = bob + sin(renderer.globalTime * 3f) * 2f
        val flash = if (hurtFlash) 220 else 160

        // Ghostly body with wavy edges
        renderer.paint.color = Color.argb(flash, 100, 50, 150)
        val waveOffset = sin(renderer.globalTime * 4f) * 2f
        val path = Path().apply {
            moveTo(sx - 10f, sy - 4f + floatBob)
            lineTo(sx - 10f, sy - 30f + floatBob)
            quadTo(sx, sy - 50f + floatBob, sx + 10f, sy - 30f + floatBob)
            lineTo(sx + 10f, sy - 4f + floatBob)
            // Wavy bottom
            lineTo(sx + 7f, sy + 4f + waveOffset + floatBob)
            lineTo(sx + 3f, sy - 2f - waveOffset + floatBob)
            lineTo(sx, sy + 5f + waveOffset + floatBob)
            lineTo(sx - 3f, sy - 2f - waveOffset + floatBob)
            lineTo(sx - 7f, sy + 4f + waveOffset + floatBob)
            close()
        }
        canvas.drawPath(path, renderer.paint)

        // Inner glow
        renderer.paint.color = Color.argb(flash / 2, 150, 100, 200)
        val innerPath = Path().apply {
            moveTo(sx - 6f, sy - 8f + floatBob)
            lineTo(sx - 6f, sy - 26f + floatBob)
            quadTo(sx, sy - 42f + floatBob, sx + 6f, sy - 26f + floatBob)
            lineTo(sx + 6f, sy - 8f + floatBob)
            close()
        }
        canvas.drawPath(innerPath, renderer.paint)

        // Eyes
        renderer.paint.color = Color.parseColor("#FF44FF")
        canvas.drawCircle(sx - 4f, sy - 35f + floatBob, 3.5f, renderer.paint)
        canvas.drawCircle(sx + 4f, sy - 35f + floatBob, 3.5f, renderer.paint)
        // Eye inner glow
        renderer.paint.color = Color.parseColor("#FF88FF")
        canvas.drawCircle(sx - 4f, sy - 35f + floatBob, 1.5f, renderer.paint)
        canvas.drawCircle(sx + 4f, sy - 35f + floatBob, 1.5f, renderer.paint)

        // Floating particles around wraith
        for (i in 0..2) {
            val px = sx + sin(renderer.globalTime * 2f + i * 2.1f) * 14f
            val py = sy - 20f + cos(renderer.globalTime * 2.5f + i * 1.7f) * 12f + floatBob
            renderer.paint.color = Color.argb(60, 180, 100, 255)
            canvas.drawCircle(px, py, 2f, renderer.paint)
        }
    }

    private fun drawMegaSkeleton(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        renderer.paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        renderer.paint.color = flash ?: Color.parseColor("#988858")
        canvas.drawRect(sx - 12f, sy - 4f, sx - 4f, sy + 10f + legSwing, renderer.paint)
        canvas.drawRect(sx + 4f, sy - 4f, sx + 12f, sy + 10f - legSwing, renderer.paint)
        // Feet
        renderer.paint.color = flash ?: Color.parseColor("#787038")
        canvas.drawRect(sx - 14f, sy + 6f + legSwing, sx - 3f, sy + 12f + legSwing, renderer.paint)
        canvas.drawRect(sx + 3f, sy + 6f - legSwing, sx + 14f, sy + 12f - legSwing, renderer.paint)

        // Large body
        renderer.paint.color = flash ?: Color.parseColor("#B8A878")
        canvas.drawRect(sx - 16f, sy - 50f + bob, sx + 16f, sy - 4f + bob, renderer.paint)
        // Rib lines
        renderer.paint.color = flash ?: Color.parseColor("#988858")
        for (i in 0..3) {
            val ribY = sy - 44f + i * 10f + bob
            canvas.drawLine(sx - 12f, ribY, sx + 12f, ribY, renderer.paint)
        }
        // Spine
        canvas.drawLine(sx, sy - 48f + bob, sx, sy - 6f + bob, renderer.paint)

        // Arms
        renderer.paint.color = flash ?: Color.parseColor("#A89868")
        canvas.drawRect(sx - 22f, sy - 44f + bob, sx - 16f, sy - 16f + bob, renderer.paint)
        canvas.drawRect(sx + 16f, sy - 44f + bob, sx + 22f, sy - 16f + bob, renderer.paint)
        // Fists
        renderer.paint.color = flash ?: Color.parseColor("#C8B888")
        canvas.drawCircle(sx - 19f, sy - 14f + bob, 4f, renderer.paint)
        canvas.drawCircle(sx + 19f, sy - 14f + bob, 4f, renderer.paint)

        // Skull
        renderer.paint.color = flash ?: Color.parseColor("#D8C898")
        canvas.drawCircle(sx, sy - 58f + bob, 14f, renderer.paint)
        // Eye sockets (glowing)
        renderer.paint.color = Color.parseColor("#FF4400")
        canvas.drawCircle(sx - 5f, sy - 60f + bob, 4f, renderer.paint)
        canvas.drawCircle(sx + 5f, sy - 60f + bob, 4f, renderer.paint)
        // Eye inner fire
        renderer.paint.color = Color.parseColor("#FF8800")
        canvas.drawCircle(sx - 5f, sy - 60f + bob, 2f, renderer.paint)
        canvas.drawCircle(sx + 5f, sy - 60f + bob, 2f, renderer.paint)
        // Jaw
        renderer.paint.color = flash ?: Color.parseColor("#A89868")
        canvas.drawRect(sx - 10f, sy - 50f + bob, sx + 10f, sy - 46f + bob, renderer.paint)
        // Teeth
        renderer.paint.color = flash ?: Color.parseColor("#E8D8A8")
        for (i in -3..3) {
            canvas.drawRect(sx + i * 3f - 1f, sy - 50f + bob, sx + i * 3f + 1f, sy - 47f + bob, renderer.paint)
        }

        // Giant sword
        renderer.paint.color = Color.parseColor("#666666")
        renderer.paint.strokeWidth = 5f
        canvas.drawLine(sx + 22f, sy - 48f + bob, sx + 38f, sy - 8f + bob, renderer.paint)
        // Sword edge
        renderer.paint.color = Color.parseColor("#888888")
        renderer.paint.strokeWidth = 2f
        canvas.drawLine(sx + 23f, sy - 46f + bob, sx + 37f, sy - 10f + bob, renderer.paint)
        renderer.paint.strokeWidth = 1f
        // Handle
        renderer.paint.color = Color.parseColor("#443322")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(sx + 18f, sy - 50f + bob, sx + 26f, sy - 46f + bob, renderer.paint)
    }

    private fun drawFlameDancer(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        renderer.paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        renderer.paint.color = flash ?: Color.parseColor("#CC4400")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f + legSwing, renderer.paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f - legSwing, renderer.paint)
        // Burning feet
        renderer.paint.color = Color.argb(150, 255, 100, 0)
        canvas.drawCircle(sx - 4f, sy + 7f + legSwing, 3f, renderer.paint)
        canvas.drawCircle(sx + 4f, sy + 7f - legSwing, 3f, renderer.paint)

        // Body
        renderer.paint.color = flash ?: Color.parseColor("#FF6622")
        canvas.drawRect(sx - 8f, sy - 32f + bob, sx + 8f, sy - 4f + bob, renderer.paint)
        // Lava cracks on body
        renderer.paint.color = Color.parseColor("#FFAA00")
        canvas.drawRect(sx - 2f, sy - 28f + bob, sx + 2f, sy - 10f + bob, renderer.paint)
        canvas.drawRect(sx - 5f, sy - 20f + bob, sx + 5f, sy - 18f + bob, renderer.paint)

        // Arms with flame
        renderer.paint.color = flash ?: Color.parseColor("#FF5511")
        canvas.drawRect(sx - 12f, sy - 28f + bob, sx - 8f, sy - 12f + bob, renderer.paint)
        canvas.drawRect(sx + 8f, sy - 28f + bob, sx + 12f, sy - 12f + bob, renderer.paint)
        // Hand flames
        val flamePulse = sin(renderer.globalTime * 8f) * 2f
        renderer.paint.color = Color.parseColor("#FFAA00")
        canvas.drawCircle(sx - 10f, sy - 14f + bob + flamePulse, 4f, renderer.paint)
        canvas.drawCircle(sx + 10f, sy - 14f + bob - flamePulse, 4f, renderer.paint)
        renderer.paint.color = Color.parseColor("#FFDD44")
        canvas.drawCircle(sx - 10f, sy - 14f + bob + flamePulse, 2f, renderer.paint)
        canvas.drawCircle(sx + 10f, sy - 14f + bob - flamePulse, 2f, renderer.paint)

        // Head
        renderer.paint.color = flash ?: Color.parseColor("#FF8844")
        canvas.drawCircle(sx, sy - 38f + bob, 7f, renderer.paint)
        // Flame crown (animated)
        renderer.paint.color = Color.parseColor("#FFAA00")
        val crownWave = sin(renderer.globalTime * 6f)
        canvas.drawCircle(sx - 4f, sy - 45f + bob + crownWave, 4f, renderer.paint)
        canvas.drawCircle(sx + 4f, sy - 45f + bob - crownWave, 4f, renderer.paint)
        canvas.drawCircle(sx, sy - 48f + bob + crownWave * 0.5f, 5f, renderer.paint)
        // Crown tips
        renderer.paint.color = Color.parseColor("#FFDD44")
        canvas.drawCircle(sx - 4f, sy - 46f + bob + crownWave, 2f, renderer.paint)
        canvas.drawCircle(sx + 4f, sy - 46f + bob - crownWave, 2f, renderer.paint)
        canvas.drawCircle(sx, sy - 49f + bob + crownWave * 0.5f, 2.5f, renderer.paint)

        // Eyes
        renderer.paint.color = Color.parseColor("#FFFF00")
        canvas.drawCircle(sx - 3f, sy - 39f + bob, 1.5f, renderer.paint)
        canvas.drawCircle(sx + 3f, sy - 39f + bob, 1.5f, renderer.paint)

        // Fire trail glow
        renderer.paint.color = Color.argb(100, 255, 100, 0)
        canvas.drawOval(sx - 14f, sy + 2f, sx + 14f, sy + 12f, renderer.paint)
    }

    private fun drawLavaCaster(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        renderer.paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs (hidden under robe, slight peek)
        renderer.paint.color = flash ?: Color.parseColor("#551100")
        canvas.drawRect(sx - 5f, sy - 2f, sx - 1f, sy + 5f + legSwing, renderer.paint)
        canvas.drawRect(sx + 1f, sy - 2f, sx + 5f, sy + 5f - legSwing, renderer.paint)

        // Robe body (trapezoid)
        renderer.paint.color = flash ?: Color.parseColor("#882200")
        val robe = Path().apply {
            moveTo(sx - 10f, sy + 6f)
            lineTo(sx - 8f, sy - 28f + bob)
            lineTo(sx + 8f, sy - 28f + bob)
            lineTo(sx + 10f, sy + 6f)
            close()
        }
        canvas.drawPath(robe, renderer.paint)
        // Robe pattern
        renderer.paint.color = Color.argb(40, 255, 100, 0)
        canvas.drawRect(sx - 4f, sy - 24f + bob, sx + 4f, sy - 8f + bob, renderer.paint)
        // Robe trim
        renderer.paint.color = Color.parseColor("#AA4400")
        canvas.drawRect(sx - 10f, sy + 4f, sx + 10f, sy + 6f, renderer.paint)

        // Arms
        renderer.paint.color = flash ?: Color.parseColor("#772200")
        canvas.drawRect(sx - 13f, sy - 24f + bob, sx - 9f, sy - 10f + bob, renderer.paint)
        canvas.drawRect(sx + 9f, sy - 24f + bob, sx + 13f, sy - 10f + bob, renderer.paint)

        // Head
        renderer.paint.color = flash ?: Color.parseColor("#FF4400")
        canvas.drawCircle(sx, sy - 34f + bob, 7f, renderer.paint)
        // Glowing eyes
        renderer.paint.color = Color.parseColor("#FFFF00")
        canvas.drawCircle(sx - 3f, sy - 35f + bob, 2f, renderer.paint)
        canvas.drawCircle(sx + 3f, sy - 35f + bob, 2f, renderer.paint)
        // Eye glow aura
        renderer.paint.color = Color.argb(60, 255, 255, 0)
        canvas.drawCircle(sx - 3f, sy - 35f + bob, 4f, renderer.paint)
        canvas.drawCircle(sx + 3f, sy - 35f + bob, 4f, renderer.paint)

        // Hood
        renderer.paint.color = flash ?: Color.parseColor("#661100")
        val hood = Path().apply {
            moveTo(sx - 9f, sy - 30f + bob)
            quadTo(sx, sy - 48f + bob, sx + 9f, sy - 30f + bob)
            close()
        }
        canvas.drawPath(hood, renderer.paint)

        // Staff
        renderer.paint.color = Color.parseColor("#553300")
        renderer.paint.strokeWidth = 3f
        canvas.drawLine(sx + 12f, sy - 38f + bob, sx + 12f, sy + 5f, renderer.paint)
        renderer.paint.strokeWidth = 1f
        // Staff orb
        val orbPulse = sin(renderer.globalTime * 4f) * 2f
        renderer.paint.color = Color.parseColor("#FF6600")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawCircle(sx + 12f, sy - 40f + bob + orbPulse, 6f, renderer.paint)
        renderer.paint.color = Color.parseColor("#FFAA00")
        canvas.drawCircle(sx + 12f, sy - 40f + bob + orbPulse, 4f, renderer.paint)
        renderer.paint.color = Color.parseColor("#FFFF44")
        canvas.drawCircle(sx + 12f, sy - 40f + bob + orbPulse, 2f, renderer.paint)
        // Orb glow
        renderer.paint.color = Color.argb(40, 255, 150, 0)
        canvas.drawCircle(sx + 12f, sy - 40f + bob + orbPulse, 10f, renderer.paint)
    }

    private fun drawInfernoTitan(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        renderer.paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        renderer.paint.color = flash ?: Color.parseColor("#661100")
        canvas.drawRect(sx - 14f, sy - 4f, sx - 4f, sy + 12f + legSwing, renderer.paint)
        canvas.drawRect(sx + 4f, sy - 4f, sx + 14f, sy + 12f - legSwing, renderer.paint)
        // Lava on legs
        renderer.paint.color = Color.parseColor("#FF4400")
        canvas.drawRect(sx - 10f, sy - 2f, sx - 6f, sy + 8f + legSwing, renderer.paint)
        canvas.drawRect(sx + 6f, sy - 2f, sx + 10f, sy + 8f - legSwing, renderer.paint)

        // Massive body
        renderer.paint.color = flash ?: Color.parseColor("#882200")
        canvas.drawRect(sx - 20f, sy - 55f + bob, sx + 20f, sy - 4f + bob, renderer.paint)
        // Lava cracks
        renderer.paint.color = Color.parseColor("#FF4400")
        canvas.drawRect(sx - 5f, sy - 50f + bob, sx + 5f, sy - 20f + bob, renderer.paint)
        canvas.drawRect(sx - 15f, sy - 35f + bob, sx + 15f, sy - 30f + bob, renderer.paint)
        canvas.drawRect(sx - 12f, sy - 18f + bob, sx + 12f, sy - 15f + bob, renderer.paint)
        // Lava glow
        renderer.paint.color = Color.parseColor("#FFAA00")
        canvas.drawRect(sx - 3f, sy - 45f + bob, sx + 3f, sy - 25f + bob, renderer.paint)

        // Arms
        renderer.paint.color = flash ?: Color.parseColor("#771100")
        canvas.drawRect(sx - 28f, sy - 50f + bob, sx - 20f, sy - 15f + bob, renderer.paint)
        canvas.drawRect(sx + 20f, sy - 50f + bob, sx + 28f, sy - 15f + bob, renderer.paint)
        // Fists
        renderer.paint.color = flash ?: Color.parseColor("#993300")
        canvas.drawCircle(sx - 24f, sy - 13f + bob, 6f, renderer.paint)
        canvas.drawCircle(sx + 24f, sy - 13f + bob, 6f, renderer.paint)
        // Fist lava glow
        renderer.paint.color = Color.argb(80, 255, 100, 0)
        canvas.drawCircle(sx - 24f, sy - 13f + bob, 8f, renderer.paint)
        canvas.drawCircle(sx + 24f, sy - 13f + bob, 8f, renderer.paint)

        // Head
        renderer.paint.color = flash ?: Color.parseColor("#AA3300")
        canvas.drawCircle(sx, sy - 63f + bob, 16f, renderer.paint)
        // Fire eyes
        renderer.paint.color = Color.parseColor("#FFFF00")
        canvas.drawCircle(sx - 6f, sy - 65f + bob, 5f, renderer.paint)
        canvas.drawCircle(sx + 6f, sy - 65f + bob, 5f, renderer.paint)
        // Eye inner
        renderer.paint.color = Color.parseColor("#FFFFFF")
        canvas.drawCircle(sx - 6f, sy - 65f + bob, 2f, renderer.paint)
        canvas.drawCircle(sx + 6f, sy - 65f + bob, 2f, renderer.paint)
        // Mouth
        renderer.paint.color = Color.parseColor("#FF4400")
        canvas.drawRect(sx - 6f, sy - 56f + bob, sx + 6f, sy - 52f + bob, renderer.paint)

        // Flame crown (animated)
        renderer.paint.color = Color.parseColor("#FF8800")
        val crownWave = sin(renderer.globalTime * 5f)
        for (i in -2..2) {
            val wave = sin(renderer.globalTime * 6f + i * 1.2f) * 3f
            canvas.drawCircle(sx + i * 8f, sy - 78f - abs(i) * 3f + bob + wave, 6f, renderer.paint)
        }
        // Crown tips
        renderer.paint.color = Color.parseColor("#FFCC00")
        for (i in -2..2) {
            val wave = sin(renderer.globalTime * 6f + i * 1.2f) * 3f
            canvas.drawCircle(sx + i * 8f, sy - 79f - abs(i) * 3f + bob + wave, 3f, renderer.paint)
        }
    }

    private fun drawShieldBearer(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean, shieldDir: Int, shieldThrown: Boolean = false) {
        renderer.paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        renderer.paint.color = flash ?: Color.parseColor("#224477")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f + legSwing, renderer.paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f - legSwing, renderer.paint)
        // Boots
        renderer.paint.color = flash ?: Color.parseColor("#112255")
        canvas.drawRect(sx - 7f, sy + 4f + legSwing, sx - 1f, sy + 8f + legSwing, renderer.paint)
        canvas.drawRect(sx + 1f, sy + 4f - legSwing, sx + 7f, sy + 8f - legSwing, renderer.paint)

        // Body
        renderer.paint.color = flash ?: Color.parseColor("#336699")
        canvas.drawRect(sx - 8f, sy - 32f + bob, sx + 8f, sy - 4f + bob, renderer.paint)
        // Armor plate
        renderer.paint.color = flash ?: Color.parseColor("#4488BB")
        canvas.drawRect(sx - 6f, sy - 30f + bob, sx + 6f, sy - 16f + bob, renderer.paint)
        // Belt
        renderer.paint.color = Color.parseColor("#554422")
        canvas.drawRect(sx - 8f, sy - 8f + bob, sx + 8f, sy - 4f + bob, renderer.paint)

        // Arms
        renderer.paint.color = flash ?: Color.parseColor("#336699")
        canvas.drawRect(sx - 12f, sy - 28f + bob, sx - 8f, sy - 12f + bob, renderer.paint)
        canvas.drawRect(sx + 8f, sy - 28f + bob, sx + 12f, sy - 12f + bob, renderer.paint)

        // Head
        renderer.paint.color = flash ?: Color.parseColor("#FFCC99")
        canvas.drawCircle(sx, sy - 38f + bob, 7f, renderer.paint)
        // Helmet
        renderer.paint.color = flash ?: Color.parseColor("#4488BB")
        canvas.drawArc(sx - 9f, sy - 48f + bob, sx + 9f, sy - 34f + bob, 180f, 180f, true, renderer.paint)
        // Helmet visor
        renderer.paint.color = flash ?: Color.parseColor("#336699")
        canvas.drawRect(sx - 7f, sy - 40f + bob, sx + 7f, sy - 38f + bob, renderer.paint)
        // Helmet crest
        renderer.paint.color = Color.parseColor("#2255AA")
        canvas.drawRect(sx - 1f, sy - 50f + bob, sx + 1f, sy - 44f + bob, renderer.paint)
        // Eyes
        renderer.paint.color = Color.parseColor("#222244")
        canvas.drawCircle(sx + 3f, sy - 39f + bob, 1.5f, renderer.paint)

        // Shield
        if (!shieldThrown) {
            renderer.paint.color = Color.parseColor("#5599DD")
            canvas.drawOval(sx + 8f, sy - 30f + bob, sx + 24f, sy - 8f + bob, renderer.paint)
            // Shield inner
            renderer.paint.color = Color.parseColor("#77BBFF")
            canvas.drawOval(sx + 11f, sy - 27f + bob, sx + 21f, sy - 11f + bob, renderer.paint)
            // Shield boss (center)
            renderer.paint.color = Color.parseColor("#FFD700")
            canvas.drawCircle(sx + 16f, sy - 19f + bob, 3f, renderer.paint)
            // Shield rim
            renderer.paint.color = Color.parseColor("#3377BB")
            renderer.paint.style = Paint.Style.STROKE
            renderer.paint.strokeWidth = 2f
            canvas.drawOval(sx + 8f, sy - 30f + bob, sx + 24f, sy - 8f + bob, renderer.paint)
            renderer.paint.strokeWidth = 1f
            renderer.paint.style = Paint.Style.FILL
        }
    }

    private fun drawSpearThrower(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        renderer.paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        renderer.paint.color = flash ?: Color.parseColor("#336633")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f + legSwing, renderer.paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f - legSwing, renderer.paint)
        // Sandals
        renderer.paint.color = flash ?: Color.parseColor("#886644")
        canvas.drawRect(sx - 7f, sy + 4f + legSwing, sx - 1f, sy + 7f + legSwing, renderer.paint)
        canvas.drawRect(sx + 1f, sy + 4f - legSwing, sx + 7f, sy + 7f - legSwing, renderer.paint)

        // Body
        renderer.paint.color = flash ?: Color.parseColor("#448844")
        canvas.drawRect(sx - 8f, sy - 32f + bob, sx + 8f, sy - 4f + bob, renderer.paint)
        // Tunic
        renderer.paint.color = flash ?: Color.parseColor("#55AA55")
        canvas.drawRect(sx - 6f, sy - 28f + bob, sx + 6f, sy - 14f + bob, renderer.paint)
        // Belt
        renderer.paint.color = Color.parseColor("#665533")
        canvas.drawRect(sx - 8f, sy - 8f + bob, sx + 8f, sy - 4f + bob, renderer.paint)

        // Arms
        renderer.paint.color = flash ?: Color.parseColor("#448844")
        canvas.drawRect(sx - 12f, sy - 28f + bob, sx - 8f, sy - 12f + bob, renderer.paint)
        canvas.drawRect(sx + 8f, sy - 28f + bob, sx + 12f, sy - 12f + bob, renderer.paint)

        // Head
        renderer.paint.color = flash ?: Color.parseColor("#FFCC99")
        canvas.drawCircle(sx, sy - 38f + bob, 7f, renderer.paint)
        // Helm
        renderer.paint.color = flash ?: Color.parseColor("#66AA66")
        canvas.drawRect(sx - 8f, sy - 46f + bob, sx + 8f, sy - 40f + bob, renderer.paint)
        // Helm crest
        renderer.paint.color = Color.parseColor("#448844")
        canvas.drawRect(sx - 1f, sy - 50f + bob, sx + 1f, sy - 46f + bob, renderer.paint)
        // Eyes
        renderer.paint.color = Color.parseColor("#224422")
        canvas.drawCircle(sx + 3f, sy - 39f + bob, 1.5f, renderer.paint)

        // Spear
        renderer.paint.color = Color.parseColor("#886644")
        renderer.paint.strokeWidth = 2f
        canvas.drawLine(sx + 10f, sy - 42f + bob, sx + 32f, sy - 58f + bob, renderer.paint)
        // Spear tip
        renderer.paint.color = Color.parseColor("#AAAAAA")
        renderer.paint.strokeWidth = 2f
        canvas.drawLine(sx + 30f, sy - 56f + bob, sx + 38f, sy - 62f + bob, renderer.paint)
        // Spear tip point
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(sx + 36f, sy - 63f + bob, sx + 40f, sy - 61f + bob, renderer.paint)
        renderer.paint.strokeWidth = 1f
        // Spear binding
        renderer.paint.color = Color.parseColor("#665533")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(sx + 28f, sy - 54f + bob, sx + 32f, sy - 52f + bob, renderer.paint)
    }

    private fun drawChampion(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean, shieldThrown: Boolean = false) {
        renderer.paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        renderer.paint.color = flash ?: Color.parseColor("#AA8833")
        canvas.drawRect(sx - 12f, sy - 4f, sx - 4f, sy + 10f + legSwing, renderer.paint)
        canvas.drawRect(sx + 4f, sy - 4f, sx + 12f, sy + 10f - legSwing, renderer.paint)
        // Greaves
        renderer.paint.color = flash ?: Color.parseColor("#CCAA44")
        canvas.drawRect(sx - 11f, sy - 2f, sx - 5f, sy + 4f + legSwing, renderer.paint)
        canvas.drawRect(sx + 5f, sy - 2f, sx + 11f, sy + 4f - legSwing, renderer.paint)

        // Large body
        renderer.paint.color = flash ?: Color.parseColor("#CCAA44")
        canvas.drawRect(sx - 16f, sy - 50f + bob, sx + 16f, sy - 4f + bob, renderer.paint)
        // Armor plate
        renderer.paint.color = flash ?: Color.parseColor("#DDCC66")
        canvas.drawRect(sx - 12f, sy - 46f + bob, sx + 12f, sy - 24f + bob, renderer.paint)
        // Armor detail
        renderer.paint.color = Color.parseColor("#FFE888")
        canvas.drawRect(sx - 4f, sy - 42f + bob, sx + 4f, sy - 28f + bob, renderer.paint)
        // Belt
        renderer.paint.color = Color.parseColor("#886633")
        canvas.drawRect(sx - 16f, sy - 8f + bob, sx + 16f, sy - 4f + bob, renderer.paint)
        // Belt buckle
        renderer.paint.color = Color.parseColor("#FFD700")
        canvas.drawRect(sx - 3f, sy - 8f + bob, sx + 3f, sy - 4f + bob, renderer.paint)

        // Arms
        renderer.paint.color = flash ?: Color.parseColor("#BBAA44")
        canvas.drawRect(sx - 22f, sy - 46f + bob, sx - 16f, sy - 18f + bob, renderer.paint)
        canvas.drawRect(sx + 16f, sy - 46f + bob, sx + 22f, sy - 18f + bob, renderer.paint)
        // Gauntlets
        renderer.paint.color = flash ?: Color.parseColor("#DDCC66")
        canvas.drawRect(sx - 24f, sy - 22f + bob, sx - 16f, sy - 16f + bob, renderer.paint)
        canvas.drawRect(sx + 16f, sy - 22f + bob, sx + 24f, sy - 16f + bob, renderer.paint)

        // Head
        renderer.paint.color = flash ?: Color.parseColor("#FFCC99")
        canvas.drawCircle(sx, sy - 58f + bob, 12f, renderer.paint)
        // Crown
        renderer.paint.color = Color.parseColor("#FFD700")
        canvas.drawRect(sx - 12f, sy - 72f + bob, sx + 12f, sy - 66f + bob, renderer.paint)
        canvas.drawRect(sx - 8f, sy - 78f + bob, sx - 4f, sy - 72f + bob, renderer.paint)
        canvas.drawRect(sx - 2f, sy - 80f + bob, sx + 2f, sy - 72f + bob, renderer.paint)
        canvas.drawRect(sx + 4f, sy - 78f + bob, sx + 8f, sy - 72f + bob, renderer.paint)
        // Crown gems
        renderer.paint.color = Color.parseColor("#FF2222")
        canvas.drawCircle(sx, sy - 76f + bob, 2f, renderer.paint)
        // Eyes
        renderer.paint.color = Color.parseColor("#442200")
        canvas.drawCircle(sx - 4f, sy - 60f + bob, 2f, renderer.paint)
        canvas.drawCircle(sx + 4f, sy - 60f + bob, 2f, renderer.paint)
        // Determined brow
        renderer.paint.color = flash ?: Color.parseColor("#DDAA66")
        canvas.drawRect(sx - 6f, sy - 63f + bob, sx - 2f, sy - 61f + bob, renderer.paint)
        canvas.drawRect(sx + 2f, sy - 63f + bob, sx + 6f, sy - 61f + bob, renderer.paint)

        // Shield & weapons
        if (!shieldThrown) {
            // Shield (phase 1)
            renderer.paint.color = Color.parseColor("#DDAA22")
            canvas.drawOval(sx + 14f, sy - 42f + bob, sx + 34f, sy - 12f + bob, renderer.paint)
            renderer.paint.color = Color.parseColor("#FFCC44")
            canvas.drawOval(sx + 18f, sy - 38f + bob, sx + 30f, sy - 16f + bob, renderer.paint)
            renderer.paint.color = Color.parseColor("#FFD700")
            canvas.drawCircle(sx + 24f, sy - 27f + bob, 4f, renderer.paint)
            renderer.paint.color = Color.parseColor("#BB8811")
            renderer.paint.style = Paint.Style.STROKE
            renderer.paint.strokeWidth = 2f
            canvas.drawOval(sx + 14f, sy - 42f + bob, sx + 34f, sy - 12f + bob, renderer.paint)
            renderer.paint.strokeWidth = 1f
            renderer.paint.style = Paint.Style.FILL

            // Spear (phase 1)
            renderer.paint.color = Color.parseColor("#886644")
            renderer.paint.strokeWidth = 3f
            canvas.drawLine(sx - 18f, sy - 52f + bob, sx - 38f, sy - 72f + bob, renderer.paint)
            renderer.paint.strokeWidth = 1f
            renderer.paint.color = Color.parseColor("#CCCCCC")
            renderer.paint.strokeWidth = 2f
            canvas.drawLine(sx - 36f, sy - 70f + bob, sx - 44f, sy - 78f + bob, renderer.paint)
            renderer.paint.strokeWidth = 1f
        } else {
            // Dual swords (phase 2 - shield thrown away)
            // Right sword
            renderer.paint.color = Color.parseColor("#CCCCCC")
            renderer.paint.strokeWidth = 3f
            canvas.drawLine(sx + 16f, sy - 40f + bob, sx + 30f, sy - 12f + bob, renderer.paint)
            renderer.paint.color = Color.parseColor("#FFFFFF")
            renderer.paint.strokeWidth = 1f
            canvas.drawLine(sx + 17f, sy - 38f + bob, sx + 29f, sy - 14f + bob, renderer.paint)
            // Right handle
            renderer.paint.color = Color.parseColor("#886633")
            renderer.paint.style = Paint.Style.FILL
            canvas.drawRect(sx + 14f, sy - 42f + bob, sx + 18f, sy - 38f + bob, renderer.paint)

            // Left sword
            renderer.paint.color = Color.parseColor("#CCCCCC")
            renderer.paint.strokeWidth = 3f
            canvas.drawLine(sx - 16f, sy - 40f + bob, sx - 30f, sy - 12f + bob, renderer.paint)
            renderer.paint.color = Color.parseColor("#FFFFFF")
            renderer.paint.strokeWidth = 1f
            canvas.drawLine(sx - 17f, sy - 38f + bob, sx - 29f, sy - 14f + bob, renderer.paint)
            // Left handle
            renderer.paint.color = Color.parseColor("#886633")
            renderer.paint.style = Paint.Style.FILL
            canvas.drawRect(sx - 18f, sy - 42f + bob, sx - 14f, sy - 38f + bob, renderer.paint)

            // Red eye glow (enraged)
            renderer.paint.color = Color.argb(80, 255, 50, 50)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 58f + bob, 16f, renderer.paint)
        }
    }
}
