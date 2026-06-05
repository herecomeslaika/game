package com.game.roguelike.rendering

import android.graphics.*
import com.game.roguelike.combat.Projectile
import com.game.roguelike.combat.ProjectileType
import com.game.roguelike.entity.BossWarning
import com.game.roguelike.entity.BossWarningShape
import com.game.roguelike.entity.Merchant
import com.game.roguelike.entity.Particle
import com.game.roguelike.level.Door
import com.game.roguelike.level.Room
import kotlin.math.cos
import kotlin.math.sin

class EntityRenderer(private val renderer: IsometricRenderer) {

    private val p: Paint get() = renderer.paint
    private val useShaders: Boolean get() = renderer.enableShaders

    fun renderProjectile(canvas: Canvas, proj: Projectile) {
        val (sx, sy) = renderer.worldToScreen(proj.position)
        reset()
        p.style = Paint.Style.FILL

        when (proj.type) {
            ProjectileType.KNIFE -> {
                val knifeAngle = if (proj.target != null && !proj.target.isDead) {
                    proj.velocity.angle
                } else {
                    proj.angle
                }
                canvas.save()
                canvas.rotate(knifeAngle, sx, sy)
                // Blade with gradient
                reset()
                if (useShaders) {
                    p.shader = renderer.makeLinearGradient(sx - 8f, sy, sx + 8f, sy, Color.parseColor("#EEEEEE"), Color.parseColor("#999999"))
                } else {
                    p.color = Color.parseColor("#CCCCCC")
                }
                p.style = Paint.Style.FILL
                val blade = renderer.obtainPath()
                blade.moveTo(sx - 8f, sy - 2f)
                blade.lineTo(sx + 6f, sy - 2f)
                blade.lineTo(sx + 8f, sy)
                blade.lineTo(sx + 6f, sy + 2f)
                blade.lineTo(sx - 8f, sy + 2f)
                blade.close()
                canvas.drawPath(blade, p)
                renderer.recyclePath(blade)
                p.shader = null
                // Edge highlight
                reset()
                p.color = Color.parseColor("#FFFFFF")
                p.style = Paint.Style.STROKE
                p.strokeWidth = 0.5f
                canvas.drawLine(sx - 6f, sy - 0.5f, sx + 6f, sy - 0.5f, p)
                // Handle
                reset()
                p.color = Color.parseColor("#886633")
                p.style = Paint.Style.FILL
                canvas.drawRect(sx + 6f, sy - 3f, sx + 10f, sy + 3f, p)
                canvas.restore()
                // Trail with gradient
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(sx, sy, 10f, Color.argb(80, 200, 200, 255), Color.argb(0, 200, 200, 255))
                } else {
                    p.color = Color.argb(80, 200, 200, 255)
                }
                p.style = Paint.Style.FILL
                canvas.drawOval(sx - 10f, sy - 4f, sx + 10f, sy + 4f, p)
                p.shader = null
            }
            ProjectileType.MAGIC_BOLT -> {
                // Outer glow with radial gradient
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(sx, sy, 10f, Color.argb(60, 153, 68, 255), Color.argb(0, 100, 40, 200))
                } else {
                    p.color = Color.argb(40, 153, 68, 255)
                }
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 10f, p)
                p.shader = null
                // Core
                reset()
                p.color = Color.parseColor("#9944FF")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 5f, p)
                reset()
                p.color = Color.parseColor("#CC88FF")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 3f, p)
                reset()
                p.color = Color.parseColor("#EECCFF")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 1.5f, p)
            }
            ProjectileType.FIREBALL -> {
                // Outer glow
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(sx, sy, 12f, Color.argb(60, 255, 100, 0), Color.argb(0, 255, 68, 0))
                } else {
                    p.color = Color.argb(40, 255, 68, 0)
                }
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 12f, p)
                p.shader = null
                // Core layers
                reset()
                p.color = Color.parseColor("#FF4400")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 7f, p)
                reset()
                p.color = Color.parseColor("#FFAA00")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 4f, p)
                reset()
                p.color = Color.parseColor("#FFFF44")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 2f, p)
            }
            ProjectileType.SPEAR -> {
                canvas.save()
                canvas.rotate(proj.angle, sx, sy)
                // Shaft with gradient
                reset()
                if (useShaders) {
                    p.shader = renderer.makeLinearGradient(sx - 12f, sy, sx + 12f, sy, Color.parseColor("#AA8855"), Color.parseColor("#886644"))
                } else {
                    p.color = Color.parseColor("#886644")
                }
                p.style = Paint.Style.FILL
                canvas.drawRect(sx - 12f, sy - 1.5f, sx + 12f, sy + 1.5f, p)
                p.shader = null
                // Tip with gradient
                reset()
                if (useShaders) {
                    p.shader = renderer.makeLinearGradient(sx + 10f, sy, sx + 18f, sy, Color.parseColor("#CCCCCC"), Color.parseColor("#888888"))
                } else {
                    p.color = Color.parseColor("#AAAAAA")
                }
                p.style = Paint.Style.FILL
                val tip = renderer.obtainPath()
                tip.moveTo(sx + 10f, sy - 3f)
                tip.lineTo(sx + 18f, sy)
                tip.lineTo(sx + 10f, sy + 3f)
                tip.close()
                canvas.drawPath(tip, p)
                renderer.recyclePath(tip)
                p.shader = null
                canvas.restore()
            }
            ProjectileType.ZEUS_BOLT -> {
                // Outer glow
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(sx, sy, 16f, Color.argb(70, 68, 170, 255), Color.argb(0, 40, 100, 200))
                } else {
                    p.color = Color.argb(50, 68, 170, 255)
                }
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 16f, p)
                p.shader = null
                // Core layers
                reset()
                p.color = Color.parseColor("#44AAFF")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 10f, p)
                reset()
                p.color = Color.parseColor("#88CCFF")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 6f, p)
                reset()
                p.color = Color.parseColor("#FFFFFF")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 3f, p)
                // Lightning tendrils
                reset()
                p.color = Color.argb(120, 150, 200, 255)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2f
                for (i in 0..3) {
                    val angle = renderer.globalTime * 8f + i * 1.57f
                    val tx = sx + cos(angle) * 12f
                    val ty = sy + sin(angle) * 12f
                    canvas.drawLine(sx, sy, tx, ty, p)
                }
                p.strokeWidth = 1f
                p.style = Paint.Style.FILL
            }
            ProjectileType.METEOR -> {
                // Outer glow
                reset()
                if (useShaders) {
                    p.shader = renderer.makeRadialGradient(sx, sy, 16f, Color.argb(60, 255, 100, 0), Color.argb(0, 255, 68, 0))
                } else {
                    p.color = Color.argb(50, 255, 68, 0)
                }
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 16f, p)
                p.shader = null
                // Core layers
                reset()
                p.color = Color.parseColor("#FF4400")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 10f, p)
                reset()
                p.color = Color.parseColor("#FFAA00")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 5f, p)
                reset()
                p.color = Color.parseColor("#FFFF44")
                p.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, 2f, p)
            }
        }
    }

    fun renderParticle(canvas: Canvas, particle: Particle) {
        val (sx, sy) = renderer.worldToScreen(particle.position)
        val alpha = ((particle.life / particle.maxLife) * 255).toInt().coerceIn(0, 255)
        reset()
        if (useShaders && particle.size > 1.5f) {
            p.shader = renderer.makeRadialGradient(sx, sy - particle.heightOffset, particle.size, Color.argb(alpha, Color.red(particle.color), Color.green(particle.color), Color.blue(particle.color)), Color.argb(alpha / 3, Color.red(particle.color), Color.green(particle.color), Color.blue(particle.color)))
        } else {
            p.color = Color.argb(alpha, Color.red(particle.color), Color.green(particle.color), Color.blue(particle.color))
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - particle.heightOffset, particle.size, p)
        p.shader = null
    }

    fun renderBossWarning(canvas: Canvas, warning: BossWarning) {
        val alpha = if (warning.resolved) 125 else (70 + 120 * (1f - warning.warningRatio)).toInt().coerceIn(70, 190)
        val strokeAlpha = if (warning.resolved) 210 else 235
        reset()
        p.style = Paint.Style.FILL
        p.color = Color.argb(alpha, Color.red(warning.color), Color.green(warning.color), Color.blue(warning.color))

        when (warning.shape) {
            BossWarningShape.CIRCLE -> drawWarningCircle(canvas, warning, alpha, strokeAlpha)
            BossWarningShape.LINE, BossWarningShape.WALL -> drawWarningLine(canvas, warning, alpha, strokeAlpha)
            BossWarningShape.CROSS -> {
                val horizontal = BossWarning.line(
                    start = com.game.roguelike.util.Vector2(warning.position.x - warning.radius, warning.position.y),
                    end = com.game.roguelike.util.Vector2(warning.position.x + warning.radius, warning.position.y),
                    width = warning.width,
                    warnDuration = warning.warnDuration,
                    damage = warning.damage,
                    color = warning.color
                )
                val vertical = BossWarning.line(
                    start = com.game.roguelike.util.Vector2(warning.position.x, warning.position.y - warning.radius),
                    end = com.game.roguelike.util.Vector2(warning.position.x, warning.position.y + warning.radius),
                    width = warning.width,
                    warnDuration = warning.warnDuration,
                    damage = warning.damage,
                    color = warning.color
                )
                drawWarningLine(canvas, horizontal, alpha, strokeAlpha)
                drawWarningLine(canvas, vertical, alpha, strokeAlpha)
            }
            BossWarningShape.FAN -> drawWarningFan(canvas, warning, alpha, strokeAlpha)
        }
    }

    private fun drawWarningCircle(canvas: Canvas, warning: BossWarning, alpha: Int, strokeAlpha: Int) {
        val (sx, sy) = renderer.worldToScreen(warning.position)
        val rx = warning.radius
        val ry = warning.radius * 0.5f
        reset()
        p.color = Color.argb(alpha, Color.red(warning.color), Color.green(warning.color), Color.blue(warning.color))
        p.style = Paint.Style.FILL
        canvas.drawOval(sx - rx, sy - ry, sx + rx, sy + ry, p)
        reset()
        p.color = Color.argb(strokeAlpha, Color.red(warning.color), Color.green(warning.color), Color.blue(warning.color))
        p.style = Paint.Style.STROKE
        p.strokeWidth = if (warning.resolved) 4f else 2f + (1f - warning.warningRatio) * 5f
        canvas.drawOval(sx - rx, sy - ry, sx + rx, sy + ry, p)
    }

    private fun drawWarningLine(canvas: Canvas, warning: BossWarning, alpha: Int, strokeAlpha: Int) {
        val (sx1, sy1) = renderer.worldToScreen(warning.start)
        val (sx2, sy2) = renderer.worldToScreen(warning.end)
        reset()
        p.color = Color.argb(alpha, Color.red(warning.color), Color.green(warning.color), Color.blue(warning.color))
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = warning.width
        canvas.drawLine(sx1, sy1, sx2, sy2, p)
        reset()
        p.color = Color.argb(strokeAlpha, Color.red(warning.color), Color.green(warning.color), Color.blue(warning.color))
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = if (warning.resolved) 5f else 2f + (1f - warning.warningRatio) * 6f
        canvas.drawLine(sx1, sy1, sx2, sy2, p)
    }

    private fun drawWarningFan(canvas: Canvas, warning: BossWarning, alpha: Int, strokeAlpha: Int) {
        val fan = renderer.obtainPath()
        val (cx, cy) = renderer.worldToScreen(warning.position)
        fan.moveTo(cx, cy)
        val steps = 16
        for (i in 0..steps) {
            val a = warning.angle - warning.arc / 2f + warning.arc * i / steps
            val point = com.game.roguelike.util.Vector2(
                warning.position.x + cos(a) * warning.radius,
                warning.position.y + sin(a) * warning.radius
            )
            val (sx, sy) = renderer.worldToScreen(point)
            fan.lineTo(sx, sy)
        }
        fan.close()
        reset()
        p.color = Color.argb(alpha, Color.red(warning.color), Color.green(warning.color), Color.blue(warning.color))
        p.style = Paint.Style.FILL
        canvas.drawPath(fan, p)
        reset()
        p.color = Color.argb(strokeAlpha, Color.red(warning.color), Color.green(warning.color), Color.blue(warning.color))
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f + (1f - warning.warningRatio) * 4f
        canvas.drawPath(fan, p)
        renderer.recyclePath(fan)
    }

    fun renderDoor(canvas: Canvas, door: Door, room: Room) {
        val (sx, sy) = renderer.worldToScreen(door.position)
        reset()
        p.style = Paint.Style.FILL
        p.color = if (door.isLocked) Color.argb(100, 100, 100, 100) else Color.argb(180, 255, 215, 0)

        // Door arch
        val hw = renderer.tileWidth / 2f
        val path = renderer.obtainPath()
        path.moveTo(sx - hw / 2, sy)
        path.lineTo(sx - hw / 2, sy - 30f)
        path.quadTo(sx, sy - 42f, sx + hw / 2, sy - 30f)
        path.lineTo(sx + hw / 2, sy)
        path.close()
        canvas.drawPath(path, p)
        renderer.recyclePath(path)

        // Door frame
        reset()
        p.color = Color.argb(120, 139, 90, 43)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        canvas.drawPath(path, p)
        p.strokeWidth = 1f

        if (!door.isLocked) {
            // Glow effect with radial gradient
            val glowPulse = (sin(renderer.globalTime * 3f) * 0.3f + 0.7f)
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(sx, sy - 15f, 22f, Color.argb((60 * glowPulse).toInt(), 255, 215, 0), Color.argb(0, 255, 215, 0))
            } else {
                p.color = Color.argb((50 * glowPulse).toInt(), 255, 215, 0)
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 15f, 22f, p)
            p.shader = null
            // Door rune
            reset()
            p.color = Color.argb((150 * glowPulse).toInt(), 255, 200, 50)
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 15f, 4f, p)
        }
    }

    fun renderMerchant(canvas: Canvas, merchant: Merchant, isNearPlayer: Boolean = false) {
        val (sx, sy) = renderer.worldToScreen(merchant.position)
        reset()
        val breathe = sin(renderer.globalTime * 2f) * 1f

        // Legs
        reset()
        p.color = Color.parseColor("#664411")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f, p)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f, p)
        // Boots
        reset()
        p.color = Color.parseColor("#553311")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 7f, sy + 4f, sx - 1f, sy + 8f, p)
        canvas.drawRect(sx + 1f, sy + 4f, sx + 7f, sy + 8f, p)

        // Body with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 8f, sy - 30f + breathe, sx + 8f, sy - 4f + breathe, renderer.lighten(Color.parseColor("#885522"), 1.1f), renderer.darken(Color.parseColor("#885522"), 0.7f))
        } else {
            p.color = Color.parseColor("#885522")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 8f, sy - 30f + breathe, sx + 8f, sy - 4f + breathe, p)
        p.shader = null
        // Vest
        reset()
        p.color = Color.parseColor("#996633")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 6f, sy - 28f + breathe, sx + 6f, sy - 12f + breathe, p)
        // Belt
        reset()
        p.color = Color.parseColor("#553311")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 8f, sy - 8f + breathe, sx + 8f, sy - 4f + breathe, p)
        // Belt pouch
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx + 6f, sy - 6f + breathe, 4f, renderer.lighten(Color.parseColor("#776633"), 1.2f), Color.parseColor("#776633"))
        } else {
            p.color = Color.parseColor("#776633")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx + 6f, sy - 6f + breathe, 3f, p)
        p.shader = null

        // Backpack
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 14f, sy - 28f + breathe, sx - 8f, sy - 10f + breathe, renderer.lighten(Color.parseColor("#557733"), 1.1f), renderer.darken(Color.parseColor("#557733"), 0.7f))
        } else {
            p.color = Color.parseColor("#557733")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 14f, sy - 28f + breathe, sx - 8f, sy - 10f + breathe, p)
        p.shader = null
        reset()
        p.color = Color.parseColor("#446622")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 12f, sy - 28f + breathe, sx - 10f, sy - 10f + breathe, p)
        // Backpack items
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 11f, sy - 24f + breathe, 3f, Color.parseColor("#FFE44D"), Color.parseColor("#DAA520"))
        } else {
            p.color = Color.parseColor("#FFD700")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 11f, sy - 24f + breathe, 2f, p)
        p.shader = null
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 11f, sy - 18f + breathe, 3f, Color.parseColor("#FF6666"), Color.parseColor("#CC0000"))
        } else {
            p.color = Color.parseColor("#FF4444")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 11f, sy - 18f + breathe, 2f, p)
        p.shader = null

        // Arms
        reset()
        p.color = Color.parseColor("#885522")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx + 8f, sy - 26f + breathe, sx + 12f, sy - 12f + breathe, p)

        // Head with spherical shading
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 1f, sy - 38f + breathe, 8f, renderer.lighten(Color.parseColor("#FFCC99"), 1.1f), renderer.darken(Color.parseColor("#FFCC99"), 0.7f))
        } else {
            p.color = Color.parseColor("#FFCC99")
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - 36f + breathe, 7f, p)
        p.shader = null
        // Hat with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - 10f, sy - 52f + breathe, sx + 10f, sy - 40f + breathe, renderer.lighten(Color.parseColor("#663300"), 1.1f), renderer.darken(Color.parseColor("#663300"), 0.7f))
        } else {
            p.color = Color.parseColor("#663300")
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 10f, sy - 46f + breathe, sx + 10f, sy - 40f + breathe, p)
        canvas.drawRect(sx - 6f, sy - 52f + breathe, sx + 6f, sy - 46f + breathe, p)
        p.shader = null
        reset()
        p.color = Color.parseColor("#552200")
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 10f, sy - 42f + breathe, sx + 10f, sy - 40f + breathe, p)
        // Eyes
        reset()
        p.color = Color.parseColor("#443322")
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 3f, sy - 37f + breathe, 1.5f, p)
        canvas.drawCircle(sx + 3f, sy - 37f + breathe, 1.5f, p)
        // Smile
        reset()
        p.color = Color.parseColor("#CC8866")
        p.style = Paint.Style.FILL
        canvas.drawPoint(sx, sy - 33f + breathe, p)

        // Interaction indicator
        val bounce = sin(renderer.globalTime * 4f) * 3f
        if (isNearPlayer && !merchant.talked) {
            renderer.textPaint.color = Color.parseColor("#FFD700")
            renderer.textPaint.textSize = 18f
            renderer.textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("按攻对话", sx, sy - 55f + bounce, renderer.textPaint)
        } else if (!merchant.talked) {
            renderer.textPaint.color = Color.parseColor("#FFD700")
            renderer.textPaint.textSize = 20f
            renderer.textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("!", sx, sy - 55f + bounce, renderer.textPaint)
        }
    }

    private fun reset() {
        renderer.resetPaint()
    }
}
