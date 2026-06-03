package com.game.roguelike.rendering

import android.graphics.*
import com.game.roguelike.core.EnemyState
import com.game.roguelike.entity.Enemy
import com.game.roguelike.entity.EnemyType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

typealias EnemyDrawFunc = (canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean, enemy: Enemy, renderer: EnemyRenderer) -> Unit

class EnemyRenderer(private val renderer: IsometricRenderer) {

    private val p: Paint get() = renderer.paint
    private val useShaders: Boolean get() = renderer.enableShaders

    private val drawRegistry = mutableMapOf<EnemyType, EnemyDrawFunc>()

    init {
        drawRegistry[EnemyType.SKELETON] = { c, sx, sy, bob, ls, mv, idle, hf, e, r -> r.drawSkeleton(c, sx, sy, bob, ls, mv, idle, hf) }
        drawRegistry[EnemyType.WRAITH] = { c, sx, sy, bob, ls, mv, idle, hf, e, r -> r.drawWraith(c, sx, sy, bob, mv, idle, hf) }
        drawRegistry[EnemyType.MEGA_SKELETON] = { c, sx, sy, bob, ls, mv, idle, hf, e, r -> r.drawMegaSkeleton(c, sx, sy, bob, ls, mv, idle, hf) }
        drawRegistry[EnemyType.FLAME_DANCER] = { c, sx, sy, bob, ls, mv, idle, hf, e, r -> r.drawFlameDancer(c, sx, sy, bob, ls, mv, idle, hf) }
        drawRegistry[EnemyType.LAVA_CASTER] = { c, sx, sy, bob, ls, mv, idle, hf, e, r -> r.drawLavaCaster(c, sx, sy, bob, ls, mv, idle, hf) }
        drawRegistry[EnemyType.INFERNO_TITAN] = { c, sx, sy, bob, ls, mv, idle, hf, e, r -> r.drawInfernoTitan(c, sx, sy, bob, ls, mv, idle, hf) }
        drawRegistry[EnemyType.SHIELD_BEARER] = { c, sx, sy, bob, ls, mv, idle, hf, e, r -> r.drawShieldBearer(c, sx, sy, bob, ls, mv, idle, hf, e.shieldDirection, e.shieldThrown) }
        drawRegistry[EnemyType.SPEAR_THROWER] = { c, sx, sy, bob, ls, mv, idle, hf, e, r -> r.drawSpearThrower(c, sx, sy, bob, ls, mv, idle, hf) }
        drawRegistry[EnemyType.CHAMPION] = { c, sx, sy, bob, ls, mv, idle, hf, e, r -> r.drawChampion(c, sx, sy, bob, ls, mv, idle, hf, e.shieldThrown) }
    }

    fun renderEnemy(canvas: Canvas, enemy: Enemy) {
        val (sx, sy) = renderer.worldToScreen(enemy.position)

        drawAttackTelegraphs(canvas, enemy, sx, sy)

        val isDying = enemy.isDead && !enemy.deathAnimationDone
        val deathProgress = if (isDying) {
            1f - (enemy.deathAnimationTimer / enemy.deathAnimationDuration)
        } else 0f

        canvas.save()

        if (isDying) {
            val scale = 1f - deathProgress * 0.6f
            val driftY = -deathProgress * 15f
            canvas.scale(scale, scale, sx, sy - enemy.height / 2f)
            canvas.translate(0f, driftY)
        }

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

        val hurtFlash = if (isDying && deathProgress < 0.3f) true
            else isHurt && enemy.stateTimer % 0.1f < 0.05f

        val phaseFlash = enemy.isPhaseTransitioning

        val drawFunc = drawRegistry[enemy.type]
        if (drawFunc != null) {
            drawFunc(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash, enemy, this)
        }

        if (phaseFlash) {
            reset()
            p.color = Color.argb(200, 255, 255, 255)
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - enemy.height / 2f, enemy.width * 2f, p)
        }

        if (enemy.stateMachine.currentState == EnemyState.PREPARE_ATTACK) {
            val glowPulse = sin(renderer.globalTime * 10f) * 0.3f + 0.7f
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx, sy - enemy.height / 2f, enemy.width * 1.5f, Color.argb((80 * glowPulse).toInt(), 255, 50, 50), Color.argb(0, 255, 50, 50))
            } else {
                p.color = Color.argb((60 * glowPulse).toInt(), 255, 50, 50)
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - enemy.height / 2f, enemy.width * 1.5f, p)
            p.shader = null
        }

        if (enemy.isFlameDashing) {
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx, sy - enemy.height / 2f, enemy.width * 2f, Color.argb(100, 255, 100, 0), Color.argb(0, 255, 100, 0))
            } else {
                p.color = Color.argb(80, 255, 100, 0)
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - enemy.height / 2f, enemy.width * 2f, p)
            p.shader = null
        }

        if (enemy.isShieldBashing) {
            reset()
            p.color = Color.argb(80, 100, 150, 255)
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - enemy.height / 2f, enemy.width * 2f, p)
        }

        if (enemy.isDodging) {
            reset()
            p.color = Color.argb(60, 200, 200, 255)
            p.style = Paint.Style.FILL
            canvas.drawOval(sx - 16f, sy - 30f, sx + 16f, sy + 6f, p)
        }

        canvas.restore()

        // Health bar
        if (!isDying && enemy.health < enemy.maxHealth) {
            val barW = enemy.width.toFloat()
            val barH = 4f
            val hpRatio = enemy.health.toFloat() / enemy.maxHealth
            reset()
            p.color = Color.RED
            p.style = Paint.Style.FILL
            canvas.drawRect(sx - barW / 2, sy - enemy.height - 8f, sx + barW / 2, sy - enemy.height - 8f + barH, p)
            reset()
            if (useShaders) {
                p.shader = renderer.makeLinearGradient(sx - barW / 2, 0f, sx - barW / 2 + barW * hpRatio, 0f, Color.parseColor("#44FF44"), Color.parseColor("#22AA22"))
            } else {
                p.color = Color.GREEN
            }
            p.style = Paint.Style.FILL
            canvas.drawRect(sx - barW / 2, sy - enemy.height - 8f, sx - barW / 2 + barW * hpRatio, sy - enemy.height - 8f + barH, p)
            p.shader = null
            reset()
            p.color = Color.argb(80, 255, 255, 255)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1f
            canvas.drawRect(sx - barW / 2, sy - enemy.height - 8f, sx + barW / 2, sy - enemy.height - 8f + barH, p)
        }

        if (enemy.isBoss && !isDying) {
            renderer.textPaint.color = Color.parseColor("#FF6644")
            renderer.textPaint.textSize = 28f
            canvas.drawText(enemy.name, sx - renderer.textPaint.measureText(enemy.name) / 2, sy - enemy.height - 16f, renderer.textPaint)
        }

        // Death animation: enhanced spiral particles
        if (isDying) {
            val dissolveColor = enemy.config.dissolveColor
            val particleAlpha = ((1f - deathProgress) * 200).toInt()
            val particleSize = 2f + deathProgress * 4f
            for (i in 0..11) {
                val angle = renderer.globalTime * 3f + i * 0.52f
                val spiralR = 5f + deathProgress * 20f
                val px = sx + (cos(angle) * spiralR).toFloat()
                val py = sy - enemy.height / 2f + (sin(angle) * spiralR * 0.5f).toFloat() - deathProgress * 25f
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(px, py, particleSize, Color.argb(particleAlpha, Color.red(dissolveColor), Color.green(dissolveColor), Color.blue(dissolveColor)), Color.argb(particleAlpha / 3, Color.red(dissolveColor), Color.green(dissolveColor), Color.blue(dissolveColor)))
                } else {
                    p.color = Color.argb(particleAlpha, Color.red(dissolveColor), Color.green(dissolveColor), Color.blue(dissolveColor))
                }
                p.style = Paint.Style.FILL
                val frag = renderer.obtainPath()
                frag.moveTo(px, py - particleSize)
                frag.lineTo(px + particleSize, py)
                frag.lineTo(px, py + particleSize)
                frag.lineTo(px - particleSize, py)
                frag.close()
                canvas.drawPath(frag, p)
                renderer.recyclePath(frag)
                p.shader = null
            }
            if (deathProgress > 0.3f) {
                val fadeAlpha = ((deathProgress - 0.3f) / 0.7f * 180).toInt()
                reset()
                p.color = Color.argb(fadeAlpha, 5, 2, 15)
                p.style = Paint.Style.FILL
                val fadeScale = 1f - deathProgress * 0.6f
                canvas.drawCircle(sx, sy - enemy.height / 2f * fadeScale, enemy.width * 2f * fadeScale, p)
            }
        }
    }

    private fun drawAttackTelegraphs(canvas: Canvas, enemy: Enemy, sx: Float, sy: Float) {
        if (enemy.isGroundSlamming && enemy.groundSlamPhase == 0) {
            val slamRadius = if (enemy.phase >= 2) 100f else 80f
            val progress = 1f - enemy.groundSlamHoverTimer / 0.5f
            val radius = slamRadius * progress
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx, sy, radius, Color.argb(80, 255, 50, 50), Color.argb(0, 255, 50, 50))
            } else {
                p.color = Color.argb(60, 255, 50, 50)
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy, radius, p)
            p.shader = null
            reset()
            p.color = Color.argb(120, 255, 100, 100)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            canvas.drawCircle(sx, sy, radius, p)
            p.strokeWidth = 1f
            p.style = Paint.Style.FILL
        }

        if (enemy.isCastingMeteor) {
            val (mx, my) = renderer.worldToScreen(enemy.meteorTargetPos)
            val progress = 1f - enemy.meteorCastTimer / 1.2f
            val radius = 30f + progress * 30f
            val pulse = sin(renderer.globalTime * 6f) * 5f
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(mx, my, radius + pulse, Color.argb(100, 255, 100, 0), Color.argb(0, 255, 100, 0))
            } else {
                p.color = Color.argb(80, 255, 100, 0)
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(mx, my, radius + pulse, p)
            p.shader = null
            reset()
            p.color = Color.argb(160, 255, 150, 0)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3f
            canvas.drawCircle(mx, my, radius, p)
            reset()
            p.color = Color.argb(200, 255, 200, 50)
            p.strokeWidth = 2f
            canvas.drawCircle(mx, my, 20f, p)
            p.strokeWidth = 1f
            p.style = Paint.Style.FILL
        }
    }

    // ==================== SKELETON ====================
    private fun drawSkeleton(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        reset()
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs (bone-like with knee joint)
        reset()
        p.color = flash ?: Color.parseColor("#B0A480")
        p.style = Paint.Style.FILL
        val leftLeg = renderer.obtainPath()
        leftLeg.moveTo(sx - 5f, sy - 4f)
        leftLeg.lineTo(sx - 2f, sy - 4f)
        leftLeg.quadTo(sx - 2.5f, sy + 2f + legSwing, sx - 3f, sy + 6f + legSwing)
        leftLeg.lineTo(sx - 7f, sy + 6f + legSwing)
        leftLeg.quadTo(sx - 6f, sy + 2f + legSwing, sx - 5f, sy - 1f)
        leftLeg.close()
        canvas.drawPath(leftLeg, p)
        renderer.recyclePath(leftLeg)
        val rightLeg = renderer.obtainPath()
        rightLeg.moveTo(sx + 2f, sy - 4f)
        rightLeg.lineTo(sx + 5f, sy - 4f)
        rightLeg.quadTo(sx + 6f, sy - 1f, sx + 7f, sy + 6f - legSwing)
        rightLeg.lineTo(sx + 3f, sy + 6f - legSwing)
        rightLeg.quadTo(sx + 2.5f, sy + 2f - legSwing, sx + 2f, sy - 1f)
        rightLeg.close()
        canvas.drawPath(rightLeg, p)
        renderer.recyclePath(rightLeg)

        // Feet
        reset()
        p.color = flash ?: Color.parseColor("#908060")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 7f, sy + 4f + legSwing, sx - 1f, sy + 8f + legSwing, p)
        canvas.drawRect(sx + 1f, sy + 4f - legSwing, sx + 7f, sy + 8f - legSwing, p)

        // Ribcage body with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 8f, sy - 30f + bob, sx + 8f, sy - 4f + bob, renderer.lighten(Color.parseColor("#D4C8A0"), 1.1f), renderer.darken(Color.parseColor("#D4C8A0"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#D4C8A0")
        }
        p.style = Paint.Style.FILL
        val ribBody = renderer.obtainPath()
        ribBody.moveTo(sx - 7f, sy - 4f + bob)
        ribBody.lineTo(sx - 8f, sy - 18f + bob)
        ribBody.quadTo(sx - 8f, sy - 28f + bob, sx - 5f, sy - 30f + bob)
        ribBody.lineTo(sx + 5f, sy - 30f + bob)
        ribBody.quadTo(sx + 8f, sy - 28f + bob, sx + 8f, sy - 18f + bob)
        ribBody.lineTo(sx + 7f, sy - 4f + bob)
        ribBody.close()
        canvas.drawPath(ribBody, p)
        renderer.recyclePath(ribBody)
        p.shader = null

        // Curved rib lines
        reset()
        p.color = flash ?: Color.parseColor("#B0A480")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        for (i in 0..2) {
            val ribY = sy - 26f + i * 7f + bob
            val ribPath = renderer.obtainPath()
            ribPath.moveTo(sx - 6f, ribY)
            ribPath.quadTo(sx - 3f, ribY - 2f, sx, ribY - 1f)
            ribPath.quadTo(sx + 3f, ribY - 2f, sx + 6f, ribY)
            canvas.drawPath(ribPath, p)
            renderer.recyclePath(ribPath)
        }
        // Spine
        canvas.drawLine(sx, sy - 28f + bob, sx, sy - 6f + bob, p)

        // Arms
        reset()
        p.color = flash ?: Color.parseColor("#C4B890")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 12f, sy - 26f + bob, sx - 8f, sy - 10f + bob, p)
        canvas.drawRect(sx + 8f, sy - 26f + bob, sx + 12f, sy - 10f + bob, p)

        // Skull with spherical shading
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 2f, sy - 38f + bob, 9f, renderer.lighten(Color.parseColor("#E8DCC0"), 1.15f), renderer.darken(Color.parseColor("#E8DCC0"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#E8DCC0")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - 36f + bob, 8f, p)
        p.shader = null

        // Jaw
        reset()
        p.color = flash ?: Color.parseColor("#D8CCB0")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 5f, sy - 32f + bob, sx + 5f, sy - 29f + bob, p)

        // Eye sockets with radial glow
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 3f, sy - 37f + bob, 3f, Color.parseColor("#FF4444"), Color.argb(0, 255, 0, 0))
        } else {
            p.color = Color.RED
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 3f, sy - 37f + bob, 2.5f, p)
        canvas.drawCircle(sx + 3f, sy - 37f + bob, 2.5f, p)
        p.shader = null
        reset()
        p.color = Color.parseColor("#FF4444")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 3f, sy - 37f + bob, 1.2f, p)
        canvas.drawCircle(sx + 3f, sy - 37f + bob, 1.2f, p)

        // Nose hole
        reset()
        p.color = flash ?: Color.parseColor("#A09070")
        p.style = Paint.Style.FILL
        canvas.drawPoint(sx, sy - 34f + bob, p)

        // Sword with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx + 12f, sy - 20f + bob, sx + 22f, sy - 8f + bob, Color.parseColor("#CCCCCC"), Color.parseColor("#888888"))
        } else {
            p.color = Color.parseColor("#888888")
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        canvas.drawLine(sx + 12f, sy - 20f + bob, sx + 22f, sy - 8f + bob, p)
        p.shader = null
        reset()
        p.color = Color.parseColor("#AAAAAA")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        canvas.drawLine(sx + 13f, sy - 19f + bob, sx + 21f, sy - 9f + bob, p)
        reset()
        p.color = Color.parseColor("#554433")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx + 10f, sy - 22f + bob, sx + 14f, sy - 18f + bob, p)

        if (isIdle) {
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx, sy - 20f + bob, 12f, Color.argb(40, 255, 100, 100), Color.argb(0, 255, 100, 100))
            } else {
                p.color = Color.argb(30, 255, 100, 100)
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 20f + bob, 12f, p)
            p.shader = null
        }
    }

    // ==================== WRAITH ====================
    private fun drawWraith(canvas: Canvas, sx: Float, sy: Float, bob: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        reset()
        val floatBob = bob + sin(renderer.globalTime * 3f) * 2f
        val flash = if (hurtFlash) 220 else 160

        // Ghostly body with wavy edges
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy - 25f + floatBob, 15f, Color.argb(flash, 150, 100, 200), Color.argb(flash / 2, 80, 30, 120))
        } else {
            p.color = Color.argb(flash, 100, 50, 150)
        }
        p.style = Paint.Style.FILL
        val waveOffset = sin(renderer.globalTime * 4f) * 2f
        val waveOffset2 = sin(renderer.globalTime * 3.5f + 1f) * 1.5f
        val path = renderer.obtainPath()
        path.moveTo(sx - 10f, sy - 4f + floatBob)
        path.quadTo(sx - 11f, sy - 18f + floatBob, sx - 8f, sy - 30f + floatBob)
        path.quadTo(sx, sy - 50f + floatBob, sx + 8f, sy - 30f + floatBob)
        path.quadTo(sx + 11f, sy - 18f + floatBob, sx + 10f, sy - 4f + floatBob)
        // Wavy bottom
        path.lineTo(sx + 7f, sy + 4f + waveOffset + floatBob)
        path.lineTo(sx + 3f, sy - 2f - waveOffset2 + floatBob)
        path.lineTo(sx, sy + 5f + waveOffset + floatBob)
        path.lineTo(sx - 3f, sy - 2f - waveOffset2 + floatBob)
        path.lineTo(sx - 7f, sy + 4f + waveOffset + floatBob)
        path.close()
        canvas.drawPath(path, p)
        renderer.recyclePath(path)
        p.shader = null

        // Inner glow
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy - 25f + floatBob, 10f, Color.argb(flash / 2, 200, 130, 255), Color.argb(0, 150, 80, 200))
        } else {
            p.color = Color.argb(flash / 2, 150, 100, 200)
        }
        p.style = Paint.Style.FILL
        val innerPath = renderer.obtainPath()
        innerPath.moveTo(sx - 6f, sy - 8f + floatBob)
        innerPath.quadTo(sx - 7f, sy - 18f + floatBob, sx - 5f, sy - 26f + floatBob)
        innerPath.quadTo(sx, sy - 42f + floatBob, sx + 5f, sy - 26f + floatBob)
        innerPath.quadTo(sx + 7f, sy - 18f + floatBob, sx + 6f, sy - 8f + floatBob)
        innerPath.close()
        canvas.drawPath(innerPath, p)
        renderer.recyclePath(innerPath)
        p.shader = null

        // Eyes with radial glow
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 4f, sy - 35f + floatBob, 5f, Color.parseColor("#FF88FF"), Color.argb(0, 255, 68, 255))
        } else {
            p.color = Color.parseColor("#FF44FF")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 4f, sy - 35f + floatBob, 3.5f, p)
        canvas.drawCircle(sx + 4f, sy - 35f + floatBob, 3.5f, p)
        p.shader = null
        reset()
        p.color = Color.parseColor("#FF88FF")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 4f, sy - 35f + floatBob, 1.5f, p)
        canvas.drawCircle(sx + 4f, sy - 35f + floatBob, 1.5f, p)

        // Floating particles
        for (i in 0..3) {
            val px = sx + sin(renderer.globalTime * 2f + i * 1.57f) * 16f
            val py = sy - 20f + cos(renderer.globalTime * 2.5f + i * 1.7f) * 14f + floatBob
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(px, py, 3f, Color.argb(80, 180, 100, 255), Color.argb(0, 180, 100, 255))
            } else {
                p.color = Color.argb(60, 180, 100, 255)
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(px, py, 2f, p)
            p.shader = null
        }
    }

    // ==================== MEGA SKELETON ====================
    private fun drawMegaSkeleton(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        reset()
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        reset()
        p.color = flash ?: Color.parseColor("#988858")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 12f, sy - 4f, sx - 4f, sy + 10f + legSwing, p)
        canvas.drawRect(sx + 4f, sy - 4f, sx + 12f, sy + 10f - legSwing, p)
        reset()
        p.color = flash ?: Color.parseColor("#787038")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 14f, sy + 6f + legSwing, sx - 3f, sy + 12f + legSwing, p)
        canvas.drawRect(sx + 3f, sy + 6f - legSwing, sx + 14f, sy + 12f - legSwing, p)

        // Large body with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 16f, sy - 50f + bob, sx + 16f, sy - 4f + bob, renderer.lighten(Color.parseColor("#B8A878"), 1.1f), renderer.darken(Color.parseColor("#B8A878"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#B8A878")
        }
        p.style = Paint.Style.FILL
        val megaBody = renderer.obtainPath()
        megaBody.moveTo(sx - 14f, sy - 4f + bob)
        megaBody.lineTo(sx - 16f, sy - 25f + bob)
        megaBody.quadTo(sx - 16f, sy - 46f + bob, sx - 10f, sy - 50f + bob)
        megaBody.lineTo(sx + 10f, sy - 50f + bob)
        megaBody.quadTo(sx + 16f, sy - 46f + bob, sx + 16f, sy - 25f + bob)
        megaBody.lineTo(sx + 14f, sy - 4f + bob)
        megaBody.close()
        canvas.drawPath(megaBody, p)
        renderer.recyclePath(megaBody)
        p.shader = null

        // Curved rib lines
        reset()
        p.color = flash ?: Color.parseColor("#988858")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f
        for (i in 0..3) {
            val ribY = sy - 44f + i * 10f + bob
            val ribPath = renderer.obtainPath()
            ribPath.moveTo(sx - 12f, ribY)
            ribPath.quadTo(sx - 6f, ribY - 3f, sx, ribY - 1f)
            ribPath.quadTo(sx + 6f, ribY - 3f, sx + 12f, ribY)
            canvas.drawPath(ribPath, p)
            renderer.recyclePath(ribPath)
        }
        canvas.drawLine(sx, sy - 48f + bob, sx, sy - 6f + bob, p)

        // Arms
        reset()
        p.color = flash ?: Color.parseColor("#A89868")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 22f, sy - 44f + bob, sx - 16f, sy - 16f + bob, p)
        canvas.drawRect(sx + 16f, sy - 44f + bob, sx + 22f, sy - 16f + bob, p)

        // Fists with radial gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 19f, sy - 14f + bob, 5f, renderer.lighten(Color.parseColor("#C8B888"), 1.2f), renderer.darken(Color.parseColor("#C8B888"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#C8B888")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 19f, sy - 14f + bob, 4f, p)
        canvas.drawCircle(sx + 19f, sy - 14f + bob, 4f, p)
        p.shader = null

        // Skull with spherical shading
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 3f, sy - 62f + bob, 16f, renderer.lighten(Color.parseColor("#D8C898"), 1.15f), renderer.darken(Color.parseColor("#D8C898"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#D8C898")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - 58f + bob, 14f, p)
        p.shader = null

        // Eye sockets with radial glow
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 5f, sy - 60f + bob, 5f, Color.parseColor("#FF8800"), Color.argb(0, 255, 68, 0))
        } else {
            p.color = Color.parseColor("#FF4400")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 5f, sy - 60f + bob, 4f, p)
        canvas.drawCircle(sx + 5f, sy - 60f + bob, 4f, p)
        p.shader = null
        reset()
        p.color = Color.parseColor("#FF8800")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 5f, sy - 60f + bob, 2f, p)
        canvas.drawCircle(sx + 5f, sy - 60f + bob, 2f, p)

        // Jaw and teeth
        reset()
        p.color = flash ?: Color.parseColor("#A89868")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 10f, sy - 50f + bob, sx + 10f, sy - 46f + bob, p)
        reset()
        p.color = flash ?: Color.parseColor("#E8D8A8")
        p.style = Paint.Style.FILL
        for (i in -3..3) {
            canvas.drawRect(sx + i * 3f - 1f, sy - 50f + bob, sx + i * 3f + 1f, sy - 47f + bob, p)
        }

        // Giant sword with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx + 22f, sy - 48f + bob, sx + 38f, sy - 8f + bob, Color.parseColor("#AAAAAA"), Color.parseColor("#555555"))
        } else {
            p.color = Color.parseColor("#666666")
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        canvas.drawLine(sx + 22f, sy - 48f + bob, sx + 38f, sy - 8f + bob, p)
        p.shader = null
        reset()
        p.color = Color.parseColor("#888888")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        canvas.drawLine(sx + 23f, sy - 46f + bob, sx + 37f, sy - 10f + bob, p)
        p.strokeWidth = 1f
        reset()
        p.color = Color.parseColor("#443322")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx + 18f, sy - 50f + bob, sx + 26f, sy - 46f + bob, p)
    }

    // ==================== FLAME DANCER ====================
    private fun drawFlameDancer(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        reset()
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        reset()
        p.color = flash ?: Color.parseColor("#CC4400")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f + legSwing, p)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f - legSwing, p)
        // Burning feet
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 4f, sy + 7f + legSwing, 4f, Color.argb(200, 255, 200, 0), Color.argb(0, 255, 100, 0))
        } else {
            p.color = Color.argb(150, 255, 100, 0)
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 4f, sy + 7f + legSwing, 3f, p)
        canvas.drawCircle(sx + 4f, sy + 7f - legSwing, 3f, p)
        p.shader = null

        // Body with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 8f, sy - 32f + bob, sx + 8f, sy - 4f + bob, renderer.lighten(Color.parseColor("#FF6622"), 1.1f), renderer.darken(Color.parseColor("#FF6622"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#FF6622")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 8f, sy - 32f + bob, sx + 8f, sy - 4f + bob, p)
        p.shader = null

        // Lava cracks (jagged Path with gradient)
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx, sy - 28f + bob, sx, sy - 10f + bob, Color.parseColor("#FFFF44"), Color.parseColor("#FF4400"))
        } else {
            p.color = Color.parseColor("#FFAA00")
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        val crack1 = renderer.obtainPath()
        crack1.moveTo(sx - 2f, sy - 28f + bob)
        crack1.lineTo(sx + 1f, sy - 24f + bob)
        crack1.lineTo(sx - 1f, sy - 20f + bob)
        crack1.lineTo(sx + 2f, sy - 16f + bob)
        crack1.lineTo(sx - 1f, sy - 12f + bob)
        crack1.lineTo(sx + 1f, sy - 10f + bob)
        canvas.drawPath(crack1, p)
        renderer.recyclePath(crack1)
        p.shader = null
        reset()
        p.color = Color.parseColor("#FFAA00")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 5f, sy - 20f + bob, sx + 5f, sy - 18f + bob, p)

        // Arms with flame
        reset()
        p.color = flash ?: Color.parseColor("#FF5511")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 12f, sy - 28f + bob, sx - 8f, sy - 12f + bob, p)
        canvas.drawRect(sx + 8f, sy - 28f + bob, sx + 12f, sy - 12f + bob, p)
        // Hand flames with radial gradient
        val flamePulse = sin(renderer.globalTime * 8f) * 2f
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 10f, sy - 14f + bob + flamePulse, 5f, Color.argb(200, 255, 220, 68), Color.argb(0, 255, 170, 0))
        } else {
            p.color = Color.parseColor("#FFAA00")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 10f, sy - 14f + bob + flamePulse, 4f, p)
        canvas.drawCircle(sx + 10f, sy - 14f + bob - flamePulse, 4f, p)
        p.shader = null
        reset()
        p.color = Color.parseColor("#FFDD44")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 10f, sy - 14f + bob + flamePulse, 2f, p)
        canvas.drawCircle(sx + 10f, sy - 14f + bob - flamePulse, 2f, p)

        // Head
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 1f, sy - 40f + bob, 8f, renderer.lighten(Color.parseColor("#FF8844"), 1.15f), renderer.darken(Color.parseColor("#FF8844"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#FF8844")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - 38f + bob, 7f, p)
        p.shader = null

        // Flame crown with radial gradient per flame
        val crownWave = sin(renderer.globalTime * 6f)
        for ((cx, cy, r) in listOf(Triple(sx - 4f, sy - 45f + bob + crownWave, 4f), Triple(sx + 4f, sy - 45f + bob - crownWave, 4f), Triple(sx, sy - 48f + bob + crownWave * 0.5f, 5f))) {
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(cx, cy, r + 1f, Color.argb(200, 255, 220, 68), Color.argb(0, 255, 170, 0))
            } else {
                p.color = Color.parseColor("#FFAA00")
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, r, p)
            p.shader = null
        }
        // Crown tips
        reset()
        p.color = Color.parseColor("#FFDD44")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 4f, sy - 46f + bob + crownWave, 2f, p)
        canvas.drawCircle(sx + 4f, sy - 46f + bob - crownWave, 2f, p)
        canvas.drawCircle(sx, sy - 49f + bob + crownWave * 0.5f, 2.5f, p)

        // Eyes
        reset()
        p.color = Color.parseColor("#FFFF00")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 3f, sy - 39f + bob, 1.5f, p)
        canvas.drawCircle(sx + 3f, sy - 39f + bob, 1.5f, p)

        // Fire trail glow
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy + 7f, 14f, Color.argb(120, 255, 100, 0), Color.argb(0, 255, 100, 0))
        } else {
            p.color = Color.argb(100, 255, 100, 0)
        }
        p.style = Paint.Style.FILL
        canvas.drawOval(sx - 14f, sy + 2f, sx + 14f, sy + 12f, p)
        p.shader = null
    }

    // ==================== LAVA CASTER ====================
    private fun drawLavaCaster(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        reset()
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        reset()
        p.color = flash ?: Color.parseColor("#551100")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 5f, sy - 2f, sx - 1f, sy + 5f + legSwing, p)
        canvas.drawRect(sx + 1f, sy - 2f, sx + 5f, sy + 5f - legSwing, p)

        // Robe body (trapezoid with gradient)
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 10f, sy + 6f, sx + 10f, sy - 28f + bob, renderer.lighten(Color.parseColor("#882200"), 1.1f), renderer.darken(Color.parseColor("#882200"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#882200")
        }
        p.style = Paint.Style.FILL
        val robe = renderer.obtainPath()
        robe.moveTo(sx - 10f, sy + 6f)
        robe.lineTo(sx - 8f, sy - 28f + bob)
        robe.lineTo(sx + 8f, sy - 28f + bob)
        robe.lineTo(sx + 10f, sy + 6f)
        robe.close()
        canvas.drawPath(robe, p)
        renderer.recyclePath(robe)
        p.shader = null

        // Robe pattern
        reset()
        p.color = Color.argb(40, 255, 100, 0)
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 4f, sy - 24f + bob, sx + 4f, sy - 8f + bob, p)
        reset()
        p.color = Color.parseColor("#AA4400")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 10f, sy + 4f, sx + 10f, sy + 6f, p)

        // Arms
        reset()
        p.color = flash ?: Color.parseColor("#772200")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 13f, sy - 24f + bob, sx - 9f, sy - 10f + bob, p)
        canvas.drawRect(sx + 9f, sy - 24f + bob, sx + 13f, sy - 10f + bob, p)

        // Head
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 1f, sy - 36f + bob, 8f, renderer.lighten(Color.parseColor("#FF4400"), 1.15f), renderer.darken(Color.parseColor("#FF4400"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#FF4400")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - 34f + bob, 7f, p)
        p.shader = null

        // Glowing eyes with radial gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 3f, sy - 35f + bob, 4f, Color.parseColor("#FFFF44"), Color.argb(0, 255, 255, 0))
        } else {
            p.color = Color.parseColor("#FFFF00")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 3f, sy - 35f + bob, 2f, p)
        canvas.drawCircle(sx + 3f, sy - 35f + bob, 2f, p)
        p.shader = null
        reset()
        p.color = Color.argb(60, 255, 255, 0)
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 3f, sy - 35f + bob, 4f, p)
        canvas.drawCircle(sx + 3f, sy - 35f + bob, 4f, p)

        // Hood
        reset()
        p.color = flash ?: Color.parseColor("#661100")
        p.style = Paint.Style.FILL
        val hood = renderer.obtainPath()
        hood.moveTo(sx - 9f, sy - 30f + bob)
        hood.quadTo(sx, sy - 48f + bob, sx + 9f, sy - 30f + bob)
        hood.close()
        canvas.drawPath(hood, p)
        renderer.recyclePath(hood)

        // Staff
        reset()
        p.color = Color.parseColor("#553300")
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        canvas.drawLine(sx + 12f, sy - 38f + bob, sx + 12f, sy + 5f, p)
        p.strokeWidth = 1f
        // Staff orb with multi-layer radial gradient
        val orbPulse = sin(renderer.globalTime * 4f) * 2f
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx + 12f, sy - 40f + bob + orbPulse, 10f, Color.argb(50, 255, 150, 0), Color.argb(0, 255, 150, 0))
        } else {
            p.color = Color.argb(40, 255, 150, 0)
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx + 12f, sy - 40f + bob + orbPulse, 10f, p)
        p.shader = null
        reset()
        p.color = Color.parseColor("#FF6600")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx + 12f, sy - 40f + bob + orbPulse, 6f, p)
        reset()
        p.color = Color.parseColor("#FFAA00")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx + 12f, sy - 40f + bob + orbPulse, 4f, p)
        reset()
        p.color = Color.parseColor("#FFFF44")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx + 12f, sy - 40f + bob + orbPulse, 2f, p)
    }

    // ==================== INFERNO TITAN ====================
    private fun drawInfernoTitan(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        reset()
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        reset()
        p.color = flash ?: Color.parseColor("#661100")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 14f, sy - 4f, sx - 4f, sy + 12f + legSwing, p)
        canvas.drawRect(sx + 4f, sy - 4f, sx + 14f, sy + 12f - legSwing, p)
        reset()
        p.color = Color.parseColor("#FF4400")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 10f, sy - 2f, sx - 6f, sy + 8f + legSwing, p)
        canvas.drawRect(sx + 6f, sy - 2f, sx + 10f, sy + 8f - legSwing, p)

        // Massive body with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 20f, sy - 55f + bob, sx + 20f, sy - 4f + bob, renderer.lighten(Color.parseColor("#882200"), 1.15f), renderer.darken(Color.parseColor("#882200"), 0.6f))
        } else {
            p.color = flash ?: Color.parseColor("#882200")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 20f, sy - 55f + bob, sx + 20f, sy - 4f + bob, p)
        p.shader = null

        // Lava cracks (jagged paths with gradient)
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx, sy - 50f + bob, sx, sy - 15f + bob, Color.parseColor("#FFFF44"), Color.parseColor("#FF4400"))
        } else {
            p.color = Color.parseColor("#FF4400")
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f
        val crack = renderer.obtainPath()
        crack.moveTo(sx - 5f, sy - 50f + bob)
        crack.lineTo(sx + 2f, sy - 42f + bob)
        crack.lineTo(sx - 3f, sy - 35f + bob)
        crack.lineTo(sx + 4f, sy - 28f + bob)
        crack.lineTo(sx - 2f, sy - 20f + bob)
        crack.lineTo(sx + 3f, sy - 15f + bob)
        canvas.drawPath(crack, p)
        renderer.recyclePath(crack)
        p.shader = null
        reset()
        p.color = Color.parseColor("#FF4400")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 15f, sy - 35f + bob, sx + 15f, sy - 30f + bob, p)
        canvas.drawRect(sx - 12f, sy - 18f + bob, sx + 12f, sy - 15f + bob, p)
        reset()
        p.color = Color.parseColor("#FFAA00")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 3f, sy - 45f + bob, sx + 3f, sy - 25f + bob, p)

        // Arms
        reset()
        p.color = flash ?: Color.parseColor("#771100")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 28f, sy - 50f + bob, sx - 20f, sy - 15f + bob, p)
        canvas.drawRect(sx + 20f, sy - 50f + bob, sx + 28f, sy - 15f + bob, p)
        // Fists
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 24f, sy - 13f + bob, 7f, Color.parseColor("#BB4400"), renderer.darken(Color.parseColor("#993300"), 0.6f))
        } else {
            p.color = flash ?: Color.parseColor("#993300")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 24f, sy - 13f + bob, 6f, p)
        canvas.drawCircle(sx + 24f, sy - 13f + bob, 6f, p)
        p.shader = null
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 24f, sy - 13f + bob, 10f, Color.argb(100, 255, 100, 0), Color.argb(0, 255, 100, 0))
        } else {
            p.color = Color.argb(80, 255, 100, 0)
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 24f, sy - 13f + bob, 8f, p)
        canvas.drawCircle(sx + 24f, sy - 13f + bob, 8f, p)
        p.shader = null

        // Head
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 3f, sy - 67f + bob, 18f, renderer.lighten(Color.parseColor("#AA3300"), 1.15f), renderer.darken(Color.parseColor("#AA3300"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#AA3300")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - 63f + bob, 16f, p)
        p.shader = null

        // Fire eyes
        reset()
        p.color = Color.parseColor("#FFFF00")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 6f, sy - 65f + bob, 5f, p)
        canvas.drawCircle(sx + 6f, sy - 65f + bob, 5f, p)
        reset()
        p.color = Color.parseColor("#FFFFFF")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 6f, sy - 65f + bob, 2f, p)
        canvas.drawCircle(sx + 6f, sy - 65f + bob, 2f, p)
        reset()
        p.color = Color.parseColor("#FF4400")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 6f, sy - 56f + bob, sx + 6f, sy - 52f + bob, p)

        // Flame crown with radial gradient
        val crownWave = sin(renderer.globalTime * 5f)
        for (i in -2..2) {
            val wave = sin(renderer.globalTime * 6f + i * 1.2f) * 3f
            val cx = sx + i * 8f
            val cy = sy - 78f - abs(i) * 3f + bob + wave
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(cx, cy, 7f, Color.argb(200, 255, 200, 0), Color.argb(0, 255, 136, 0))
            } else {
                p.color = Color.parseColor("#FF8800")
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, 6f, p)
            p.shader = null
        }
        reset()
        p.color = Color.parseColor("#FFCC00")
        p.style = Paint.Style.FILL
        for (i in -2..2) {
            val wave = sin(renderer.globalTime * 6f + i * 1.2f) * 3f
            canvas.drawCircle(sx + i * 8f, sy - 79f - abs(i) * 3f + bob + wave, 3f, p)
        }
    }

    // ==================== SHIELD BEARER ====================
    private fun drawShieldBearer(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean, shieldDir: Int, shieldThrown: Boolean = false) {
        reset()
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        reset()
        p.color = flash ?: Color.parseColor("#224477")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f + legSwing, p)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f - legSwing, p)
        reset()
        p.color = flash ?: Color.parseColor("#112255")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 7f, sy + 4f + legSwing, sx - 1f, sy + 8f + legSwing, p)
        canvas.drawRect(sx + 1f, sy + 4f - legSwing, sx + 7f, sy + 8f - legSwing, p)

        // Body with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 8f, sy - 32f + bob, sx + 8f, sy - 4f + bob, renderer.lighten(Color.parseColor("#336699"), 1.15f), renderer.darken(Color.parseColor("#336699"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#336699")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 8f, sy - 32f + bob, sx + 8f, sy - 4f + bob, p)
        p.shader = null

        // Armor plate with metallic gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 6f, sy - 30f + bob, sx + 6f, sy - 16f + bob, renderer.lighten(Color.parseColor("#4488BB"), 1.2f), renderer.darken(Color.parseColor("#4488BB"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#4488BB")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 6f, sy - 30f + bob, sx + 6f, sy - 16f + bob, p)
        p.shader = null

        // Belt
        reset()
        p.color = Color.parseColor("#554422")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 8f, sy - 8f + bob, sx + 8f, sy - 4f + bob, p)

        // Arms
        reset()
        p.color = flash ?: Color.parseColor("#336699")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 12f, sy - 28f + bob, sx - 8f, sy - 12f + bob, p)
        canvas.drawRect(sx + 8f, sy - 28f + bob, sx + 12f, sy - 12f + bob, p)

        // Head
        reset()
        p.color = flash ?: Color.parseColor("#FFCC99")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - 38f + bob, 7f, p)

        // Helmet with metallic gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy - 42f + bob, 10f, renderer.lighten(Color.parseColor("#4488BB"), 1.3f), renderer.darken(Color.parseColor("#4488BB"), 0.6f))
        } else {
            p.color = flash ?: Color.parseColor("#4488BB")
        }
        p.style = Paint.Style.FILL
        canvas.drawArc(sx - 9f, sy - 48f + bob, sx + 9f, sy - 34f + bob, 180f, 180f, true, p)
        p.shader = null
        reset()
        p.color = flash ?: Color.parseColor("#336699")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 7f, sy - 40f + bob, sx + 7f, sy - 38f + bob, p)
        reset()
        p.color = Color.parseColor("#2255AA")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 1f, sy - 50f + bob, sx + 1f, sy - 44f + bob, p)
        reset()
        p.color = Color.parseColor("#222244")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx + 3f, sy - 39f + bob, 1.5f, p)

        // Shield with radial gradient
        if (!shieldThrown) {
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx + 16f, sy - 19f + bob, 10f, renderer.lighten(Color.parseColor("#5599DD"), 1.3f), renderer.darken(Color.parseColor("#5599DD"), 0.6f))
            } else {
                p.color = Color.parseColor("#5599DD")
            }
            p.style = Paint.Style.FILL
            canvas.drawOval(sx + 8f, sy - 30f + bob, sx + 24f, sy - 8f + bob, p)
            p.shader = null
            reset()
            p.color = Color.parseColor("#77BBFF")
            p.style = Paint.Style.FILL
            canvas.drawOval(sx + 11f, sy - 27f + bob, sx + 21f, sy - 11f + bob, p)
            // Shield cross
            reset()
            p.color = Color.argb(80, 200, 230, 255)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.5f
            canvas.drawLine(sx + 16f, sy - 26f + bob, sx + 16f, sy - 12f + bob, p)
            canvas.drawLine(sx + 12f, sy - 19f + bob, sx + 20f, sy - 19f + bob, p)
            // Rivets
            reset()
            p.color = Color.parseColor("#AACCEE")
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx + 12f, sy - 26f + bob, 1f, p)
            canvas.drawCircle(sx + 20f, sy - 26f + bob, 1f, p)
            canvas.drawCircle(sx + 12f, sy - 12f + bob, 1f, p)
            canvas.drawCircle(sx + 20f, sy - 12f + bob, 1f, p)
            // Shield boss
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx + 16f, sy - 19f + bob, 4f, Color.parseColor("#FFE44D"), Color.parseColor("#B8860B"))
            } else {
                p.color = Color.parseColor("#FFD700")
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx + 16f, sy - 19f + bob, 3f, p)
            p.shader = null
            // Shield rim
            reset()
            p.color = Color.parseColor("#3377BB")
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            canvas.drawOval(sx + 8f, sy - 30f + bob, sx + 24f, sy - 8f + bob, p)
            p.strokeWidth = 1f
            p.style = Paint.Style.FILL
        }
    }

    // ==================== SPEAR THROWER ====================
    private fun drawSpearThrower(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        reset()
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        reset()
        p.color = flash ?: Color.parseColor("#336633")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f + legSwing, p)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f - legSwing, p)
        reset()
        p.color = flash ?: Color.parseColor("#886644")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 7f, sy + 4f + legSwing, sx - 1f, sy + 7f + legSwing, p)
        canvas.drawRect(sx + 1f, sy + 4f - legSwing, sx + 7f, sy + 7f - legSwing, p)

        // Body with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 8f, sy - 32f + bob, sx + 8f, sy - 4f + bob, renderer.lighten(Color.parseColor("#448844"), 1.1f), renderer.darken(Color.parseColor("#448844"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#448844")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 8f, sy - 32f + bob, sx + 8f, sy - 4f + bob, p)
        p.shader = null
        reset()
        p.color = flash ?: Color.parseColor("#55AA55")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 6f, sy - 28f + bob, sx + 6f, sy - 14f + bob, p)
        reset()
        p.color = Color.parseColor("#665533")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 8f, sy - 8f + bob, sx + 8f, sy - 4f + bob, p)

        // Arms
        reset()
        p.color = flash ?: Color.parseColor("#448844")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 12f, sy - 28f + bob, sx - 8f, sy - 12f + bob, p)
        canvas.drawRect(sx + 8f, sy - 28f + bob, sx + 12f, sy - 12f + bob, p)

        // Head
        reset()
        p.color = flash ?: Color.parseColor("#FFCC99")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - 38f + bob, 7f, p)
        reset()
        p.color = flash ?: Color.parseColor("#66AA66")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 8f, sy - 46f + bob, sx + 8f, sy - 40f + bob, p)
        reset()
        p.color = Color.parseColor("#448844")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 1f, sy - 50f + bob, sx + 1f, sy - 46f + bob, p)
        reset()
        p.color = Color.parseColor("#224422")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx + 3f, sy - 39f + bob, 1.5f, p)

        // Spear with tapered shaft
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx + 10f, sy - 42f + bob, sx + 32f, sy - 58f + bob, Color.parseColor("#AA8855"), Color.parseColor("#886644"))
        } else {
            p.color = Color.parseColor("#886644")
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        canvas.drawLine(sx + 10f, sy - 42f + bob, sx + 32f, sy - 58f + bob, p)
        p.shader = null
        // Spear tip (triangle path with gradient)
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx + 30f, sy - 56f + bob, sx + 40f, sy - 62f + bob, Color.parseColor("#DDDDDD"), Color.parseColor("#888888"))
        } else {
            p.color = Color.parseColor("#AAAAAA")
        }
        p.style = Paint.Style.FILL
        val spearTip = renderer.obtainPath()
        spearTip.moveTo(sx + 30f, sy - 56f + bob)
        spearTip.lineTo(sx + 40f, sy - 62f + bob)
        spearTip.lineTo(sx + 32f, sy - 54f + bob)
        spearTip.close()
        canvas.drawPath(spearTip, p)
        renderer.recyclePath(spearTip)
        p.shader = null
        reset()
        p.color = Color.parseColor("#665533")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx + 28f, sy - 54f + bob, sx + 32f, sy - 52f + bob, p)
    }

    // ==================== CHAMPION ====================
    private fun drawChampion(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean, shieldThrown: Boolean = false) {
        reset()
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        reset()
        p.color = flash ?: Color.parseColor("#AA8833")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 12f, sy - 4f, sx - 4f, sy + 10f + legSwing, p)
        canvas.drawRect(sx + 4f, sy - 4f, sx + 12f, sy + 10f - legSwing, p)
        // Greaves with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 11f, sy - 2f, sx - 5f, sy + 4f + legSwing, renderer.lighten(Color.parseColor("#CCAA44"), 1.2f), renderer.darken(Color.parseColor("#CCAA44"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#CCAA44")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 11f, sy - 2f, sx - 5f, sy + 4f + legSwing, p)
        canvas.drawRect(sx + 5f, sy - 2f, sx + 11f, sy + 4f - legSwing, p)
        p.shader = null

        // Large body with golden gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 16f, sy - 50f + bob, sx + 16f, sy - 4f + bob, renderer.lighten(Color.parseColor("#CCAA44"), 1.15f), renderer.darken(Color.parseColor("#CCAA44"), 0.65f))
        } else {
            p.color = flash ?: Color.parseColor("#CCAA44")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 16f, sy - 50f + bob, sx + 16f, sy - 4f + bob, p)
        p.shader = null

        // Armor plate with metallic gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 12f, sy - 46f + bob, sx + 12f, sy - 24f + bob, renderer.lighten(Color.parseColor("#DDCC66"), 1.2f), renderer.darken(Color.parseColor("#DDCC66"), 0.7f))
        } else {
            p.color = flash ?: Color.parseColor("#DDCC66")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 12f, sy - 46f + bob, sx + 12f, sy - 24f + bob, p)
        p.shader = null
        reset()
        p.color = Color.parseColor("#FFE888")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 4f, sy - 42f + bob, sx + 4f, sy - 28f + bob, p)

        // Belt
        reset()
        p.color = Color.parseColor("#886633")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 16f, sy - 8f + bob, sx + 16f, sy - 4f + bob, p)
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy - 6f + bob, 4f, Color.parseColor("#FFE44D"), Color.parseColor("#B8860B"))
        } else {
            p.color = Color.parseColor("#FFD700")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 3f, sy - 8f + bob, sx + 3f, sy - 4f + bob, p)
        p.shader = null

        // Arms
        reset()
        p.color = flash ?: Color.parseColor("#BBAA44")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 22f, sy - 46f + bob, sx - 16f, sy - 18f + bob, p)
        canvas.drawRect(sx + 16f, sy - 46f + bob, sx + 22f, sy - 18f + bob, p)
        // Gauntlets
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 24f, sy - 22f + bob, sx - 16f, sy - 16f + bob, renderer.lighten(Color.parseColor("#DDCC66"), 1.2f), Color.parseColor("#DDCC66"))
        } else {
            p.color = flash ?: Color.parseColor("#DDCC66")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 24f, sy - 22f + bob, sx - 16f, sy - 16f + bob, p)
        canvas.drawRect(sx + 16f, sy - 22f + bob, sx + 24f, sy - 16f + bob, p)
        p.shader = null

        // Head
        reset()
        p.color = flash ?: Color.parseColor("#FFCC99")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - 58f + bob, 12f, p)

        // Crown with jagged path and gems
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 12f, sy - 78f + bob, sx + 12f, sy - 66f + bob, Color.parseColor("#FFE44D"), Color.parseColor("#B8860B"))
        } else {
            p.color = Color.parseColor("#FFD700")
        }
        p.style = Paint.Style.FILL
        val crown = renderer.obtainPath()
        crown.moveTo(sx - 12f, sy - 66f + bob)
        crown.lineTo(sx - 8f, sy - 78f + bob)
        crown.lineTo(sx - 4f, sy - 72f + bob)
        crown.lineTo(sx, sy - 80f + bob)
        crown.lineTo(sx + 4f, sy - 72f + bob)
        crown.lineTo(sx + 8f, sy - 78f + bob)
        crown.lineTo(sx + 12f, sy - 66f + bob)
        crown.lineTo(sx - 12f, sy - 66f + bob)
        crown.close()
        canvas.drawPath(crown, p)
        renderer.recyclePath(crown)
        p.shader = null

        // Crown gems with radial gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy - 76f + bob, 3f, Color.parseColor("#FF6666"), Color.parseColor("#FF0000"))
        } else {
            p.color = Color.parseColor("#FF2222")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - 76f + bob, 2f, p)
        p.shader = null

        // Eyes
        reset()
        p.color = Color.parseColor("#442200")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 4f, sy - 60f + bob, 2f, p)
        canvas.drawCircle(sx + 4f, sy - 60f + bob, 2f, p)
        // Determined brow
        reset()
        p.color = flash ?: Color.parseColor("#DDAA66")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 6f, sy - 63f + bob, sx - 2f, sy - 61f + bob, p)
        canvas.drawRect(sx + 2f, sy - 63f + bob, sx + 6f, sy - 61f + bob, p)

        // Shield & weapons
        if (!shieldThrown) {
            // Shield with radial gradient
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx + 24f, sy - 27f + bob, 12f, renderer.lighten(Color.parseColor("#DDAA22"), 1.3f), renderer.darken(Color.parseColor("#DDAA22"), 0.6f))
            } else {
                p.color = Color.parseColor("#DDAA22")
            }
            p.style = Paint.Style.FILL
            canvas.drawOval(sx + 14f, sy - 42f + bob, sx + 34f, sy - 12f + bob, p)
            p.shader = null
            reset()
            p.color = Color.parseColor("#FFCC44")
            p.style = Paint.Style.FILL
            canvas.drawOval(sx + 18f, sy - 38f + bob, sx + 30f, sy - 16f + bob, p)
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx + 24f, sy - 27f + bob, 5f, Color.parseColor("#FFE44D"), Color.parseColor("#B8860B"))
            } else {
                p.color = Color.parseColor("#FFD700")
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx + 24f, sy - 27f + bob, 4f, p)
            p.shader = null
            reset()
            p.color = Color.parseColor("#BB8811")
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            canvas.drawOval(sx + 14f, sy - 42f + bob, sx + 34f, sy - 12f + bob, p)
            p.strokeWidth = 1f
            p.style = Paint.Style.FILL

            // Spear
            reset()
            p.color = Color.parseColor("#886644")
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3f
            canvas.drawLine(sx - 18f, sy - 52f + bob, sx - 38f, sy - 72f + bob, p)
            p.strokeWidth = 1f
            reset()
            if (useShaders) {
                p.shader = renderer.makeLinearGradient(sx - 36f, sy - 70f + bob, sx - 44f, sy - 78f + bob, Color.parseColor("#DDDDDD"), Color.parseColor("#888888"))
            } else {
                p.color = Color.parseColor("#CCCCCC")
            }
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            canvas.drawLine(sx - 36f, sy - 70f + bob, sx - 44f, sy - 78f + bob, p)
            p.strokeWidth = 1f
            p.shader = null
        } else {
            // Dual swords with gradient
            for (dir in listOf(-1, 1)) {
                reset()
                if (useShaders) {
                    p.shader = renderer.makeLinearGradient(sx + dir * 16f, sy - 40f + bob, sx + dir * 30f, sy - 12f + bob, Color.parseColor("#EEEEEE"), Color.parseColor("#888899"))
                } else {
                    p.color = Color.parseColor("#CCCCCC")
                }
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3f
                canvas.drawLine(sx + dir * 16f, sy - 40f + bob, sx + dir * 30f, sy - 12f + bob, p)
                p.shader = null
                reset()
                p.color = Color.parseColor("#FFFFFF")
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1f
                canvas.drawLine(sx + dir * 17f, sy - 38f + bob, sx + dir * 29f, sy - 14f + bob, p)
                reset()
                p.color = Color.parseColor("#886633")
                p.style = Paint.Style.FILL
                canvas.drawRect(sx + dir * 14f, sy - 42f + bob, sx + dir * 18f, sy - 38f + bob, p)
            }

            // Red eye glow
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx, sy - 58f + bob, 16f, Color.argb(100, 255, 50, 50), Color.argb(0, 255, 50, 50))
            } else {
                p.color = Color.argb(80, 255, 50, 50)
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 58f + bob, 16f, p)
            p.shader = null
        }
    }

    private fun reset() {
        renderer.resetPaint()
    }
}
