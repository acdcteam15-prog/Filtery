package com.example.filtery

import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private fun col(v: Long): Int = v.toInt()
private val BLACK = col(0xFF000000)
private val WHITE = col(0xFFFFFFFF)
private val RED = col(0xFFE53935)
private val PINK = col(0xFFFF4DA6)
private val YELLOW = col(0xFFFFD600)
private val ORANGE = col(0xFFFF9100)
private val BLUE = col(0xFF2979FF)
private val GREEN = col(0xFF43A047)
private val GOLD = col(0xFFFFC107)
private val DGOLD = col(0xFFB8860B)
private val BROWN = col(0xFF8D5B2E)
private val DBROWN = col(0xFF5D3A1A)
private val GRAYC = col(0xFF9E9E9E)
private val PINKIN = col(0xFFFFB6C8)

private fun MutableList<Fx>.fx(id: String, name: String, icon: String, draw: FxCtx.() -> Unit) {
    add(Fx(id, name, Cat.FACES, icon, true, draw))
}

internal fun MutableList<Fx>.addFaceFilters() {
    fx("party", "Party Hat", "🥳") {
        for (f in faces) {
            val u = f.w
            at(f.topX, f.topY, f.rot - 12f) {
                val pa = e.path
                pa.reset(); pa.moveTo(-0.3f * u, 0.06f * u); pa.lineTo(0.3f * u, 0.06f * u); pa.lineTo(0f, -0.85f * u); pa.close()
                c.save(); c.clipPath(pa)
                c.drawPath(pa, fill(PINK))
                for (i in -3..3) line(-0.5f * u + i * 0.18f * u, 0.1f * u, -0.1f * u + i * 0.18f * u, -0.9f * u, 0.07f * u, YELLOW)
                c.restore()
                disc(0f, -0.86f * u, 0.07f * u, YELLOW)
            }
        }
    }
    fx("tophat", "Top Hat", "🎩") {
        for (f in faces) {
            val u = f.w
            at(f.topX, f.topY + 0.03f * u, f.rot) {
                rrect(-0.27f * u, -0.55f * u, 0.27f * u, 0f, 0.04f * u, col(0xFF1B1B1B))
                rect(-0.27f * u, -0.15f * u, 0.27f * u, -0.04f * u, RED)
                oval(0f, 0f, 0.46f * u, 0.08f * u, col(0xFF111111))
                oval(0f, -0.55f * u, 0.27f * u, 0.05f * u, col(0xFF333333))
            }
        }
    }
    fx("crown", "King Crown", "👑") {
        for (f in faces) {
            val u = f.w
            at(f.topX, f.topY + 0.02f * u, f.rot) {
                poly(GOLD, 1f, -0.3f * u, 0.04f * u, -0.3f * u, -0.38f * u, -0.15f * u, -0.2f * u, 0f, -0.52f * u, 0.15f * u, -0.2f * u, 0.3f * u, -0.38f * u, 0.3f * u, 0.04f * u)
                c.drawPath(e.path, stroke(DGOLD, 0.02f * u))
                disc(0f, -0.06f * u, 0.04f * u, RED); disc(-0.18f * u, -0.06f * u, 0.03f * u, BLUE); disc(0.18f * u, -0.06f * u, 0.03f * u, GREEN)
                disc(0f, -0.56f * u, 0.03f * u, WHITE); disc(-0.3f * u, -0.4f * u, 0.03f * u, WHITE); disc(0.3f * u, -0.4f * u, 0.03f * u, WHITE)
            }
        }
    }
    fx("shades", "Cool Shades", "😎") {
        for (f in faces) {
            val d = f.eyeDist
            at(f.ex, f.ey, f.rot) {
                val dark = col(0xF0101010)
                rrect(-1.0f * d, -0.28f * d, -0.06f * d, 0.32f * d, 0.18f * d, dark)
                rrect(0.06f * d, -0.28f * d, 1.0f * d, 0.32f * d, 0.18f * d, dark)
                rect(-0.09f * d, -0.2f * d, 0.09f * d, -0.1f * d, BLACK)
                line(-1.0f * d, -0.14f * d, -1.35f * d, -0.2f * d, 0.08f * d, BLACK)
                line(1.0f * d, -0.14f * d, 1.35f * d, -0.2f * d, 0.08f * d, BLACK)
                line(-0.82f * d, -0.18f * d, -0.56f * d, 0.12f * d, 0.05f * d, WHITE, 0.35f)
                line(0.3f * d, -0.18f * d, 0.56f * d, 0.12f * d, 0.05f * d, WHITE, 0.35f)
            }
        }
    }
    fx("nerd", "Nerd Glasses", "🤓") {
        for (f in faces) {
            val d = f.eyeDist
            at(f.ex, f.ey, f.rot) {
                disc(-0.5f * d, 0f, 0.46f * d, WHITE, 0.14f); disc(0.5f * d, 0f, 0.46f * d, WHITE, 0.14f)
                ring(-0.5f * d, 0f, 0.5f * d, 0.1f * d, col(0xFF2B1B0E)); ring(0.5f * d, 0f, 0.5f * d, 0.1f * d, col(0xFF2B1B0E))
                line(-0.06f * d, -0.05f * d, 0.06f * d, -0.05f * d, 0.09f * d, col(0xFF2B1B0E))
                line(-1.0f * d, -0.05f * d, -1.35f * d, -0.1f * d, 0.08f * d, col(0xFF2B1B0E))
                line(1.0f * d, -0.05f * d, 1.35f * d, -0.1f * d, 0.08f * d, col(0xFF2B1B0E))
                rect(-0.1f * d, -0.14f * d, 0.1f * d, 0.1f * d, WHITE, 0.9f)
                line(-0.1f * d, -0.14f * d, 0.1f * d, 0.1f * d, 0.015f * d, GRAYC)
            }
        }
    }
    fx("hearteyes", "Heart Eyes", "😍") {
        for (f in faces) {
            val size = f.eyeDist * 1.05f * (1f + 0.12f * sin(t * 8f))
            emoji("\u2764\uFE0F", f.lx, f.ly, size); emoji("\u2764\uFE0F", f.rx, f.ry, size)
        }
    }
    fx("clown", "Clown", "🤡") {
        val wig = intArrayOf(col(0xFFFF5252), col(0xFFFFB300), col(0xFF7C4DFF))
        for (f in faces) {
            val u = f.w
            for (sg in intArrayOf(-1, 1)) {
                disc(f.hx(sg * 0.55f * u, -0.38f * f.h), f.hy(sg * 0.55f * u, -0.38f * f.h), 0.21f * u, wig[0])
                disc(f.hx(sg * 0.68f * u, -0.14f * f.h), f.hy(sg * 0.68f * u, -0.14f * f.h), 0.19f * u, wig[1])
                disc(f.hx(sg * 0.5f * u, -0.55f * f.h), f.hy(sg * 0.5f * u, -0.55f * f.h), 0.18f * u, wig[2])
            }
            at(f.mx, f.my, f.rot) {
                e.rectF.set(-0.28f * u, -0.12f * u, 0.28f * u, 0.14f * u)
                c.drawArc(e.rectF, 15f, 150f, false, stroke(RED, 0.07f * u))
            }
            disc(f.nx, f.ny, 0.15f * u, RED)
            disc(f.nx - 0.05f * u, f.ny - 0.05f * u, 0.04f * u, WHITE, 0.7f)
        }
    }
    fx("dog", "Puppy", "🐶") {
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    c.save(); c.translate(sg * 0.52f * u, -0.4f * u); c.rotate(sg * 22f)
                    oval(0f, 0.12f * u, 0.17f * u, 0.3f * u, BROWN)
                    oval(0f, 0.14f * u, 0.09f * u, 0.2f * u, DBROWN)
                    c.restore()
                }
            }
            at(f.nx, f.ny, f.rot) {
                oval(0f, 0f, 0.13f * u, 0.09f * u, BLACK)
                oval(-0.04f * u, -0.03f * u, 0.04f * u, 0.025f * u, WHITE, 0.55f)
            }
            at(f.mx, f.my, f.rot) {
                val len = 0.17f * u + 0.04f * u * sin(t * 7f)
                rrect(-0.075f * u, 0.0f, 0.075f * u, len, 0.075f * u, col(0xFFFF6F91))
                line(0f, 0.02f * u, 0f, len * 0.7f, 0.012f * u, col(0xFFD64F7A))
            }
        }
    }
    fx("cat", "Kitty", "🐱") {
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    poly(GRAYC, 1f, sg * 0.52f * u, -0.22f * u, sg * 0.12f * u, -0.54f * u, sg * 0.44f * u, -0.9f * u)
                    poly(PINKIN, 1f, sg * 0.44f * u, -0.32f * u, sg * 0.2f * u, -0.54f * u, sg * 0.42f * u, -0.74f * u)
                }
            }
            at(f.nx, f.ny, f.rot) {
                poly(col(0xFFFF7FA0), 1f, -0.06f * u, -0.03f * u, 0.06f * u, -0.03f * u, 0f, 0.04f * u)
                for (sg in intArrayOf(-1, 1)) for (j in -1..1) {
                    line(sg * 0.14f * u, 0.05f * u + j * 0.05f * u, sg * 0.66f * u, 0.0f + j * 0.12f * u, 0.022f * u, BLACK, 0.6f)
                    line(sg * 0.14f * u, 0.05f * u + j * 0.05f * u, sg * 0.66f * u, 0.0f + j * 0.12f * u, 0.011f * u, WHITE)
                }
            }
        }
    }
    fx("bunny", "Bunny", "🐰") {
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    c.save(); c.translate(sg * 0.2f * u, -0.6f * u); c.rotate(sg * 10f)
                    oval(0f, -0.34f * u, 0.13f * u, 0.42f * u, col(0xFFF5F5F5))
                    oval(0f, -0.34f * u, 0.07f * u, 0.32f * u, PINKIN)
                    c.restore()
                }
            }
            at(f.mx, f.my, f.rot) {
                rrect(-0.06f * u, -0.01f * u, 0f, 0.1f * u, 0.02f * u, WHITE)
                rrect(0f, -0.01f * u, 0.06f * u, 0.1f * u, 0.02f * u, WHITE)
                line(0f, -0.01f * u, 0f, 0.1f * u, 0.008f * u, GRAYC)
            }
            disc(f.nx, f.ny, 0.05f * u, col(0xFFFF7FA0))
        }
    }
    fx("pig", "Piggy", "🐷") {
        val pk = col(0xFFFF9EB5)
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (sg in intArrayOf(-1, 1)) poly(pk, 1f, sg * 0.18f * u, -0.5f * u, sg * 0.52f * u, -0.62f * u, sg * 0.5f * u, -0.2f * u)
            }
            at(f.nx, f.ny, f.rot) {
                oval(0f, 0f, 0.2f * u, 0.15f * u, pk)
                ovalLine(0f, 0f, 0.2f * u, 0.15f * u, 0.015f * u, col(0xFFD6648A))
                oval(-0.07f * u, 0f, 0.035f * u, 0.06f * u, col(0xFFB5476A)); oval(0.07f * u, 0f, 0.035f * u, 0.06f * u, col(0xFFB5476A))
            }
        }
    }
    fx("devil", "Little Devil", "😈") {
        tint(look(sat = 1.2f, r = 1.15f, g = 0.9f, b = 0.9f, tr = 12f), 0.5f * k)
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    poly(RED, 1f, sg * 0.08f * u, -0.5f * u, sg * 0.36f * u, -0.54f * u, sg * 0.33f * u, -1.0f * u)
                    poly(col(0xFFFF8A80), 0.8f, sg * 0.16f * u, -0.54f * u, sg * 0.3f * u, -0.56f * u, sg * 0.31f * u, -0.82f * u)
                }
            }
        }
    }
    fx("angel", "Angel", "😇") {
        tint(look(bri = 0.04f, sat = 0.9f, r = 1.05f), 0.4f * k)
        for (f in faces) {
            val u = f.w
            val bob = sin(t * 3f) * 0.03f * u
            at(f.topX, f.topY - 0.12f * u + bob, f.rot) {
                ovalLine(0f, 0f, 0.3f * u, 0.08f * u, 0.09f * u, YELLOW, 0.3f)
                ovalLine(0f, 0f, 0.3f * u, 0.08f * u, 0.045f * u, GOLD)
            }
            for (i in 0 until 3) {
                val a = (t * 1.5f + i * 0.33f) % 1f
                sparkle(f.hx((i - 1) * 0.55f * u, -0.3f * u - a * 0.3f * u), f.hy((i - 1) * 0.55f * u, -0.3f * u - a * 0.3f * u), 0.07f * u * (1f - a), WHITE)
            }
        }
    }
    fx("mustache", "Mustache", "👨") {
        for (f in faces) {
            val u = f.w
            at((f.nx + f.mx) / 2f, (f.ny + f.my) / 2f + 0.01f * u, f.rot) {
                val pa = e.path
                pa.reset()
                pa.moveTo(0f, -0.02f * u)
                pa.cubicTo(0.1f * u, -0.1f * u, 0.3f * u, -0.07f * u, 0.36f * u, 0.07f * u)
                pa.cubicTo(0.26f * u, 0.0f, 0.12f * u, 0.03f * u, 0f, 0.05f * u)
                pa.cubicTo(-0.12f * u, 0.03f * u, -0.26f * u, 0.0f, -0.36f * u, 0.07f * u)
                pa.cubicTo(-0.3f * u, -0.07f * u, -0.1f * u, -0.1f * u, 0f, -0.02f * u)
                pa.close()
                c.drawPath(pa, fill(col(0xFF3E2723)))
            }
        }
    }
    fx("pirate", "Pirate", "🏴\u200D☠️") {
        for (f in faces) {
            val u = f.w; val d = f.eyeDist
            // bandana
            at(f.cx, f.cy, f.rot) {
                poly(RED, 1f, -0.56f * u, -0.55f * u, 0.56f * u, -0.55f * u, 0.6f * u, -0.3f * u, -0.6f * u, -0.3f * u)
                poly(RED, 1f, 0.56f * u, -0.42f * u, 0.9f * u, -0.58f * u, 0.84f * u, -0.26f * u)
                for (i in -2..2) disc(i * 0.2f * u, -0.43f * u, 0.03f * u, WHITE)
            }
            // eye patch on the eye at the left of the picture
            line(f.lx, f.ly, f.hx(-0.6f * u, -0.3f * u), f.hy(-0.6f * u, -0.3f * u), 0.03f * u, BLACK)
            line(f.lx, f.ly, f.hx(0.5f * u, -0.45f * u), f.hy(0.5f * u, -0.45f * u), 0.03f * u, BLACK)
            at(f.lx, f.ly, f.rot) { oval(0f, 0f, 0.42f * d, 0.36f * d, col(0xFF111111)) }
        }
    }
    fx("reindeer", "Reindeer", "🦌") {
        val br = col(0xFF6D4C41)
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    line(sg * 0.18f * u, -0.55f * u, sg * 0.48f * u, -1.02f * u, 0.06f * u, br)
                    line(sg * 0.3f * u, -0.76f * u, sg * 0.64f * u, -0.74f * u, 0.045f * u, br)
                    line(sg * 0.41f * u, -0.9f * u, sg * 0.72f * u, -1.0f * u, 0.04f * u, br)
                    line(sg * 0.24f * u, -0.66f * u, sg * 0.1f * u, -0.9f * u, 0.04f * u, br)
                }
            }
            val pulse = 0.5f + 0.5f * sin(t * 5f)
            disc(f.nx, f.ny, 0.22f * u, RED, 0.25f * pulse)
            disc(f.nx, f.ny, 0.12f * u, RED)
            disc(f.nx - 0.04f * u, f.ny - 0.04f * u, 0.035f * u, WHITE, 0.7f)
        }
    }
    fx("flowers", "Flower Crown", "🌸") {
        val fl = arrayOf("🌸", "🌼", "🌺", "🌷", "🌻")
        for (f in faces) {
            val u = f.w
            for (i in 0..8) {
                val a = Math.toRadians(8.0 + i * 20.5).toFloat()
                val dx = -cos(a) * 0.56f * u; val dy = -sin(a) * 0.6f * f.h - 0.04f * u
                disc(f.hx(dx, dy), f.hy(dx, dy), 0.07f * u, GREEN)
                emoji(fl[i % fl.size], f.hx(dx, dy), f.hy(dx, dy), 0.27f * u)
            }
        }
    }
    fx("rainbow", "Rainbow Mouth", "🌈") {
        val bands = intArrayOf(col(0xFFFF1744), col(0xFFFF9100), col(0xFFFFEA00), col(0xFF00E676), col(0xFF00B0FF), col(0xFF304FFE), col(0xFFAA00FF))
        for (f in faces) {
            val u = f.w
            val sway = sin(t * 5f) * 0.1f * u
            for (i in 0..6) {
                val ox = (i - 3) * 0.04f * u
                val pa = e.path
                pa.reset()
                pa.moveTo(f.mx + ox, f.my + 0.04f * u)
                pa.cubicTo(f.mx + ox + sway, f.my + (hf - f.my) * 0.35f, f.mx + ox - sway, f.my + (hf - f.my) * 0.7f, f.mx + ox + sway * 0.5f, hf + 0.1f * u)
                c.drawPath(pa, stroke(bands[i], 0.048f * u))
            }
        }
    }
    fx("tears", "Happy Tears", "😂") {
        val water = col(0xFF4FC3F7)
        for (f in faces) {
            val u = f.w
            for (sg in intArrayOf(-1, 1)) {
                val ex = if (sg < 0) f.lx else f.rx; val ey = if (sg < 0) f.ly else f.ry
                for (i in 0 until 4) {
                    val p = (t * 1.1f + i * 0.25f) % 1f
                    val x = ex + sg * (0.04f + 0.1f * p) * u
                    val y = ey + 0.1f * u + p * 0.6f * f.h
                    disc(x, y, 0.045f * u * (1f - 0.5f * p), water, 1f - p * 0.6f)
                    disc(x - 0.012f * u, y - 0.012f * u, 0.014f * u, WHITE, 0.8f)
                }
            }
        }
    }
    fx("fireeyes", "Fire Eyes", "🔥") {
        for (f in faces) {
            val u = f.w
            for (i in 0..1) {
                val ex = if (i == 0) f.lx else f.rx; val ey = if (i == 0) f.ly else f.ry
                val fl = 1f + 0.18f * sin(t * 22f + i * 2f) + 0.1f * sin(t * 13f)
                disc(ex, ey, 0.2f * u, ORANGE, 0.3f)
                emoji("🔥", ex, ey - 0.08f * u * fl, 0.4f * u * fl)
            }
        }
    }
    fx("laser", "Laser Eyes", "⚡") {
        for (f in faces) {
            val u = f.w
            val pulse = 0.75f + 0.25f * sin(t * 18f)
            for (i in 0..1) {
                val ex = if (i == 0) f.lx else f.rx; val ey = if (i == 0) f.ly else f.ry
                val tx = if (i == 0) -0.1f * wf else 1.1f * wf
                val ty = ey + 0.25f * hf
                line(ex, ey, tx, ty, 0.09f * u * pulse, col(0xFFFF1744), 0.35f)
                line(ex, ey, tx, ty, 0.04f * u * pulse, col(0xFFFF8A80))
                line(ex, ey, tx, ty, 0.015f * u, WHITE)
                disc(ex, ey, 0.1f * u * pulse, col(0xFFFF1744), 0.6f)
                disc(ex, ey, 0.05f * u, WHITE)
            }
        }
    }
    fx("dizzy", "Dizzy Stars", "💫") {
        for (f in faces) {
            val u = f.w
            for (i in 0 until 5) {
                val a = t * 3f + i * 1.2566f
                val dx = cos(a) * 0.5f * u; val dy = -0.58f * f.h + sin(a) * 0.12f * u
                val z = 0.75f + 0.25f * sin(a)
                star(f.hx(dx, dy), f.hy(dx, dy), 0.1f * u * z, YELLOW)
            }
        }
    }
    fx("vampire", "Vampire", "🧛") {
        tint(look(sat = 0.55f, con = 1.2f, bri = -0.02f, r = 1.05f, g = 1.0f, b = 1.05f), 0.6f * k)
        for (f in faces) {
            val u = f.w
            at(f.mx, f.my, f.rot) {
                for (sg in intArrayOf(-1, 1)) poly(WHITE, 1f, sg * 0.12f * u, -0.02f * u, sg * 0.05f * u, -0.02f * u, sg * 0.085f * u, 0.12f * u)
                val drip = (t * 0.8f) % 1f
                disc(0.15f * u, 0.05f * u + drip * 0.3f * u, 0.022f * u, col(0xFFB71C1C), 1f - drip)
            }
            val fl = sin(t * 9f)
            emoji("🦇", f.hx(-0.7f * u, -0.62f * f.h + fl * 0.03f * u), f.hy(-0.7f * u, -0.62f * f.h + fl * 0.03f * u), 0.28f * u * (0.85f + 0.15f * abs(fl)))
            emoji("🦇", f.hx(0.7f * u, -0.7f * f.h - fl * 0.03f * u), f.hy(0.7f * u, -0.7f * f.h - fl * 0.03f * u), 0.24f * u * (0.85f + 0.15f * abs(fl)))
        }
    }
    fx("mask", "Hero Mask", "🦸") {
        for (f in faces) {
            val d = f.eyeDist
            at(f.ex, f.ey, f.rot) {
                val pa = e.path
                pa.reset()
                pa.moveTo(-1.25f * d, -0.45f * d)
                pa.quadTo(0f, -0.75f * d, 1.25f * d, -0.45f * d)
                pa.lineTo(1.05f * d, 0.1f * d)
                pa.quadTo(0.7f * d, 0.55f * d, 0.25f * d, 0.4f * d)
                pa.quadTo(0f, 0.25f * d, -0.25f * d, 0.4f * d)
                pa.quadTo(-0.7f * d, 0.55f * d, -1.05f * d, 0.1f * d)
                pa.close()
                pa.addOval(-0.82f * d, -0.2f * d, -0.2f * d, 0.3f * d, android.graphics.Path.Direction.CW)
                pa.addOval(0.2f * d, -0.2f * d, 0.82f * d, 0.3f * d, android.graphics.Path.Direction.CW)
                pa.fillType = android.graphics.Path.FillType.EVEN_ODD
                c.drawPath(pa, fill(col(0xFF1E3FD8)))
                c.drawPath(pa, stroke(col(0xFFFFD600), 0.035f * d))
            }
        }
    }
    fx("santa", "Santa", "🎅") {
        for (f in faces) {
            val u = f.w
            // beard under the mouth and by the cheeks, moustache above it
            val bx = floatArrayOf(-0.46f, -0.4f, -0.28f, -0.12f, 0f, 0.12f, 0.28f, 0.4f, 0.46f)
            val by = floatArrayOf(0.12f, 0.3f, 0.45f, 0.55f, 0.6f, 0.55f, 0.45f, 0.3f, 0.12f)
            for (i in bx.indices) disc(f.hx(bx[i] * u, by[i] * u), f.hy(bx[i] * u, by[i] * u), 0.17f * u, WHITE)
            for (sg in intArrayOf(-1, 1)) {
                val mxx = f.hx(sg * 0.1f * u, 0.18f * u); val myy = f.hy(sg * 0.1f * u, 0.18f * u)
                oval(mxx, myy, 0.13f * u, 0.065f * u, WHITE)
            }
            at(f.topX, f.topY + 0.04f * u, f.rot - 8f) {
                poly(RED, 1f, -0.5f * u, 0.0f, 0.5f * u, 0.0f, 0.3f * u, -0.55f * u, 0.65f * u, -0.3f * u, 0.5f * u, -0.62f * u, -0.2f * u, -0.72f * u)
                rrect(-0.55f * u, -0.06f * u, 0.55f * u, 0.1f * u, 0.08f * u, WHITE)
                disc(0.62f * u, -0.45f * u, 0.09f * u, WHITE)
            }
        }
    }
    fx("thirdeye", "Third Eye", "👁️") {
        for (f in faces) {
            val u = f.w
            val px = f.hx(0f, -0.27f * f.h); val py = f.hy(0f, -0.27f * f.h)
            val blink = if ((t % 3f) < 0.15f) 0.15f else 1f
            at(px, py, f.rot) {
                oval(0f, 0f, 0.13f * u, 0.075f * u * blink, WHITE)
                if (blink > 0.5f) {
                    disc(0f, 0f, 0.055f * u, col(0xFF3B8EDB)); disc(0f, 0f, 0.028f * u, BLACK); disc(-0.015f * u, -0.015f * u, 0.01f * u, WHITE)
                }
                ovalLine(0f, 0f, 0.13f * u, 0.075f * u * blink, 0.012f * u, col(0xFF4E342E))
            }
        }
    }
    fx("tongue", "Tongue Out", "😜") {
        for (f in faces) {
            val u = f.w
            at(f.mx, f.my, f.rot + sin(t * 6f) * 6f) {
                rrect(-0.09f * u, -0.01f * u, 0.09f * u, 0.3f * u, 0.09f * u, col(0xFFFF5C8D))
                line(0f, 0.02f * u, 0f, 0.2f * u, 0.014f * u, col(0xFFD6356B))
            }
        }
    }
}
