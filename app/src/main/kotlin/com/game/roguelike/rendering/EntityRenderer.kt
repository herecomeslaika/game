package com.game.roguelike.rendering

import android.graphics.*
import com.game.roguelike.combat.Projectile
import com.game.roguelike.combat.ProjectileType
import com.game.roguelike.entity.Merchant
import com.game.roguelike.entity.Particle
import com.game.roguelike.level.Door
import com.game.roguelike.level.Room
import kotlin.math.cos
import kotlin.math.sin

class EntityRenderer(private val renderer: IsometricRenderer) {

    fun renderProjectile(canvas: Canvas, proj: Projectile) {
        val (sx, sy) = renderer.worldToScreen(proj.position)
        renderer.paint.style = Paint.Style.FILL

        when (proj.type) {
            ProjectileType.KNIFE -> {
                val knifeAngle = if (proj.target != null && !proj.target.isDead) {
                    proj.velocity.angle
                } else {
                    proj.angle
                }
                canvas.save()
                canvas.rotate(knifeAngle, sx, sy)
                // Blade
                renderer.paint.color = Color.parseColor("#CCCCCC")
                canvas.drawRect(sx - 8f, sy - 2f, sx + 8f, sy + 2f, renderer.paint)
                // Edge highlight
                renderer.paint.color = Color.parseColor("#EEEEEE")
                canvas.drawRect(sx - 6f, sy - 1f, sx + 6f, sy, renderer.paint)
                // Handle
                renderer.paint.color = Color.parseColor("#886633")
                canvas.drawRect(sx + 6f, sy - 3f, sx + 10f, sy + 3f, renderer.paint)
                canvas.restore()
                // Trail
                renderer.paint.color = Color.argb(80, 200, 200, 255)
                canvas.drawOval(sx - 10f, sy - 4f, sx + 10f, sy + 4f, renderer.paint)
            }
            ProjectileType.MAGIC_BOLT -> {
                // Outer glow
                renderer.paint.color = Color.argb(40, 153, 68, 255)
                canvas.drawCircle(sx, sy, 8f, renderer.paint)
                // Core
                renderer.paint.color = Color.parseColor("#9944FF")
                canvas.drawCircle(sx, sy, 5f, renderer.paint)
                // Inner bright
                renderer.paint.color = Color.parseColor("#CC88FF")
                canvas.drawCircle(sx, sy, 3f, renderer.paint)
                // Center
                renderer.paint.color = Color.parseColor("#EECCFF")
                canvas.drawCircle(sx, sy, 1.5f, renderer.paint)
            }
            ProjectileType.FIREBALL -> {
                // Outer glow
                renderer.paint.color = Color.argb(40, 255, 68, 0)
                canvas.drawCircle(sx, sy, 10f, renderer.paint)
                // Core
                renderer.paint.color = Color.parseColor("#FF4400")
                canvas.drawCircle(sx, sy, 7f, renderer.paint)
                // Mid
                renderer.paint.color = Color.parseColor("#FFAA00")
                canvas.drawCircle(sx, sy, 4f, renderer.paint)
                // Center
                renderer.paint.color = Color.parseColor("#FFFF44")
                canvas.drawCircle(sx, sy, 2f, renderer.paint)
            }
            ProjectileType.SPEAR -> {
                canvas.save()
                canvas.rotate(proj.angle, sx, sy)
                // Shaft
                renderer.paint.color = Color.parseColor("#886644")
                canvas.drawRect(sx - 12f, sy - 1.5f, sx + 12f, sy + 1.5f, renderer.paint)
                // Tip
                renderer.paint.color = Color.parseColor("#AAAAAA")
                canvas.drawRect(sx + 10f, sy - 3f, sx + 16f, sy + 3f, renderer.paint)
                // Tip point
                renderer.paint.color = Color.parseColor("#CCCCCC")
                canvas.drawRect(sx + 14f, sy - 1.5f, sx + 18f, sy + 1.5f, renderer.paint)
                canvas.restore()
            }
            ProjectileType.ZEUS_BOLT -> {
                // Outer glow
                renderer.paint.color = Color.argb(50, 68, 170, 255)
                canvas.drawCircle(sx, sy, 14f, renderer.paint)
                // Core
                renderer.paint.color = Color.parseColor("#44AAFF")
                canvas.drawCircle(sx, sy, 10f, renderer.paint)
                // Inner
                renderer.paint.color = Color.parseColor("#88CCFF")
                canvas.drawCircle(sx, sy, 6f, renderer.paint)
                // Center
                renderer.paint.color = Color.parseColor("#FFFFFF")
                canvas.drawCircle(sx, sy, 3f, renderer.paint)
                // Lightning tendrils
                renderer.paint.color = Color.argb(120, 150, 200, 255)
                renderer.paint.style = Paint.Style.STROKE
                renderer.paint.strokeWidth = 2f
                for (i in 0..3) {
                    val angle = renderer.globalTime * 8f + i * 1.57f
                    val tx = sx + cos(angle) * 12f
                    val ty = sy + sin(angle) * 12f
                    canvas.drawLine(sx, sy, tx, ty, renderer.paint)
                }
                renderer.paint.strokeWidth = 1f
                renderer.paint.style = Paint.Style.FILL
            }
            ProjectileType.METEOR -> {
                // Outer glow
                renderer.paint.color = Color.argb(50, 255, 68, 0)
                canvas.drawCircle(sx, sy, 14f, renderer.paint)
                // Core
                renderer.paint.color = Color.parseColor("#FF4400")
                canvas.drawCircle(sx, sy, 10f, renderer.paint)
                // Inner
                renderer.paint.color = Color.parseColor("#FFAA00")
                canvas.drawCircle(sx, sy, 5f, renderer.paint)
                // Center
                renderer.paint.color = Color.parseColor("#FFFF44")
                canvas.drawCircle(sx, sy, 2f, renderer.paint)
            }
        }
    }

    fun renderParticle(canvas: Canvas, particle: Particle) {
        val (sx, sy) = renderer.worldToScreen(particle.position)
        val alpha = ((particle.life / particle.maxLife) * 255).toInt().coerceIn(0, 255)
        renderer.paint.color = Color.argb(alpha, Color.red(particle.color), Color.green(particle.color), Color.blue(particle.color))
        renderer.paint.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - particle.heightOffset, particle.size, renderer.paint)
    }

    fun renderDoor(canvas: Canvas, door: Door, room: Room) {
        val (sx, sy) = renderer.worldToScreen(door.position)
        renderer.paint.style = Paint.Style.FILL

        if (door.isLocked) {
            renderer.paint.color = Color.argb(100, 100, 100, 100)
        } else {
            renderer.paint.color = Color.argb(180, 255, 215, 0)
        }

        // Door arch
        val hw = renderer.tileWidth / 2f
        val hh = renderer.tileHeight / 2f
        val path = Path().apply {
            moveTo(sx - hw / 2, sy)
            lineTo(sx - hw / 2, sy - 30f)
            quadTo(sx, sy - 42f, sx + hw / 2, sy - 30f)
            lineTo(sx + hw / 2, sy)
            close()
        }
        canvas.drawPath(path, renderer.paint)

        // Door frame
        renderer.paint.color = Color.argb(120, 139, 90, 43)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 2f
        canvas.drawPath(path, renderer.paint)
        renderer.paint.strokeWidth = 1f

        if (!door.isLocked) {
            // Glow effect
            val glowPulse = (sin(renderer.globalTime * 3f) * 0.3f + 0.7f)
            renderer.paint.color = Color.argb((50 * glowPulse).toInt(), 255, 215, 0)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 15f, 22f, renderer.paint)
            // Door rune
            renderer.paint.color = Color.argb((150 * glowPulse).toInt(), 255, 200, 50)
            canvas.drawCircle(sx, sy - 15f, 4f, renderer.paint)
        }
    }

    fun renderMerchant(canvas: Canvas, merchant: Merchant, isNearPlayer: Boolean = false) {
        val (sx, sy) = renderer.worldToScreen(merchant.position)

        renderer.paint.style = Paint.Style.FILL
        val breathe = sin(renderer.globalTime * 2f) * 1f

        // Legs
        renderer.paint.color = Color.parseColor("#664411")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f, renderer.paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f, renderer.paint)
        // Boots
        renderer.paint.color = Color.parseColor("#553311")
        canvas.drawRect(sx - 7f, sy + 4f, sx - 1f, sy + 8f, renderer.paint)
        canvas.drawRect(sx + 1f, sy + 4f, sx + 7f, sy + 8f, renderer.paint)

        // Body
        renderer.paint.color = Color.parseColor("#885522")
        canvas.drawRect(sx - 8f, sy - 30f + breathe, sx + 8f, sy - 4f + breathe, renderer.paint)
        // Vest
        renderer.paint.color = Color.parseColor("#996633")
        canvas.drawRect(sx - 6f, sy - 28f + breathe, sx + 6f, sy - 12f + breathe, renderer.paint)
        // Belt
        renderer.paint.color = Color.parseColor("#553311")
        canvas.drawRect(sx - 8f, sy - 8f + breathe, sx + 8f, sy - 4f + breathe, renderer.paint)
        // Belt pouch
        renderer.paint.color = Color.parseColor("#776633")
        canvas.drawCircle(sx + 6f, sy - 6f + breathe, 3f, renderer.paint)

        // Backpack
        renderer.paint.color = Color.parseColor("#557733")
        canvas.drawRect(sx - 14f, sy - 28f + breathe, sx - 8f, sy - 10f + breathe, renderer.paint)
        // Backpack strap
        renderer.paint.color = Color.parseColor("#446622")
        canvas.drawRect(sx - 12f, sy - 28f + breathe, sx - 10f, sy - 10f + breathe, renderer.paint)
        // Backpack items peeking
        renderer.paint.color = Color.parseColor("#FFD700")
        canvas.drawCircle(sx - 11f, sy - 24f + breathe, 2f, renderer.paint)
        renderer.paint.color = Color.parseColor("#FF4444")
        canvas.drawCircle(sx - 11f, sy - 18f + breathe, 2f, renderer.paint)

        // Arms
        renderer.paint.color = Color.parseColor("#885522")
        canvas.drawRect(sx + 8f, sy - 26f + breathe, sx + 12f, sy - 12f + breathe, renderer.paint)

        // Head
        renderer.paint.color = Color.parseColor("#FFCC99")
        canvas.drawCircle(sx, sy - 36f + breathe, 7f, renderer.paint)
        // Hat
        renderer.paint.color = Color.parseColor("#663300")
        canvas.drawRect(sx - 10f, sy - 46f + breathe, sx + 10f, sy - 40f + breathe, renderer.paint)
        canvas.drawRect(sx - 6f, sy - 52f + breathe, sx + 6f, sy - 46f + breathe, renderer.paint)
        // Hat brim detail
        renderer.paint.color = Color.parseColor("#552200")
        canvas.drawRect(sx - 10f, sy - 42f + breathe, sx + 10f, sy - 40f + breathe, renderer.paint)
        // Eyes
        renderer.paint.color = Color.parseColor("#443322")
        canvas.drawCircle(sx - 3f, sy - 37f + breathe, 1.5f, renderer.paint)
        canvas.drawCircle(sx + 3f, sy - 37f + breathe, 1.5f, renderer.paint)
        // Smile
        renderer.paint.color = Color.parseColor("#CC8866")
        canvas.drawPoint(sx, sy - 33f + breathe, renderer.paint)

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
}
