package com.example.filtery

import android.graphics.BlurMaskFilter
import android.graphics.Path
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private fun col(v: Long): Int = v.toInt()
private val BLACK = col(0xFF000000)
private val WHITE = col(0xFFFFFFFF)

private fun MutableList<Fx>.fx(id: String, name: String, icon: String, draw: FxCtx.() -> Unit) {
    add(Fx(id, name, Cat.FACES, icon, true, draw))
}

private fun FxCtx.roundEar(f: PFace, sg: Int, x: Float, y: Float, r: Float, outer: Int, inner: Int, seed: Int) {
    at(f.cx, f.cy, f.rot) {
        softOval(sg * x + 0.02f * r, y + 0.15f * r, r * 1.05f, r * 1.05f, BLACK, 0.28f, r * 0.35f)
        gradOval(sg * x, y, r, r, shadeColor(outer, 1.35f), shadeColor(outer, 0.5f))
        gradOval(sg * x, y + 0.05f * r, r * 0.6f, r * 0.6f, shadeColor(inner, 1.2f), shadeColor(inner, 0.6f))
        fur(sg * x, y, r, r, 50, r * 0.3f, r * 0.07f, outer, 5 + sg, if (sg < 0) 200f else -20f, 60f)
    }
}

private fun FxCtx.whiskers(f: PFace, color: Int, a: Float = 0.95f) {
    val u = f.w
    at(f.nx, f.ny, f.rot) {
        for (sg in intArrayOf(-1, 1)) for (j in -1..1) {
            val yy = 0.06f * u + j * 0.05f * u
            line(sg * 0.14f * u, yy, sg * 0.68f * u, yy + j * 0.1f * u - 0.02f * u, 0.014f * u, BLACK, 0.3f)
            line(sg * 0.14f * u, yy - 0.008f * u, sg * 0.68f * u, yy + j * 0.1f * u - 0.028f * u, 0.009f * u, color, a)
        }
    }
}

internal fun MutableList<Fx>.addFaceFilters2() {
    fx("bear", "Teddy Bear", "🐻") {
        val brown = col(0xFF8A5A33)
        for (f in faces) {
            val u = f.w
            roundEar(f, -1, 0.44f * u, -0.5f * u, 0.17f * u, brown, col(0xFFD9A77A), 1)
            roundEar(f, 1, 0.44f * u, -0.5f * u, 0.17f * u, brown, col(0xFFD9A77A), 2)
            softOval(f.hx(0f, 0.2f * f.h), f.hy(0f, 0.2f * f.h), 0.26f * u, 0.18f * u, col(0xFFE8C9A0), 0.3f, 0.07f * u)
            softOval(f.nx + 0.02f * u, f.ny + 0.1f * u, 0.14f * u, 0.04f * u, BLACK, 0.3f, 0.04f * u)
            gradOval(f.nx, f.ny - 0.01f * u, 0.12f * u, 0.085f * u, col(0xFF4A4A4A), col(0xFF050505))
            oval(f.nx - 0.04f * u, f.ny - 0.04f * u, 0.045f * u, 0.02f * u, WHITE, 0.7f)
        }
    }
    fx("panda", "Panda", "🐼") {
        for (f in faces) {
            val u = f.w
            softOval(f.cx, f.cy, 0.5f * u, 0.5f * f.h, WHITE, 0.18f, 0.1f * u)
            roundEar(f, -1, 0.44f * u, -0.5f * u, 0.15f * u, col(0xFF1A1A1C), col(0xFF3A3A3E), 3)
            roundEar(f, 1, 0.44f * u, -0.5f * u, 0.15f * u, col(0xFF1A1A1C), col(0xFF3A3A3E), 4)
            for (i in 0..1) {
                val ex = if (i == 0) f.lx else f.rx; val ey = if (i == 0) f.ly else f.ry
                at(ex, ey, f.rot + (if (i == 0) 25f else -25f)) { softOval(0f, 0f, 0.2f * u, 0.27f * u, col(0xFF0E0E10), 0.88f, 0.03f * u) }
            }
            gradOval(f.nx, f.ny, 0.07f * u, 0.05f * u, col(0xFF444444), col(0xFF050505))
        }
    }
    fx("fox", "Fox", "🦊") {
        val orange = col(0xFFE8731A)
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    val pa = e.path
                    pa.reset(); pa.moveTo(sg * 0.5f * u, -0.18f * u); pa.quadTo(sg * 0.5f * u, -0.6f * u, sg * 0.42f * u, -0.98f * u); pa.quadTo(sg * 0.2f * u, -0.74f * u, sg * 0.1f * u, -0.52f * u); pa.close()
                    softOval(sg * 0.33f * u, -0.55f * u, 0.17f * u, 0.3f * u, BLACK, 0.2f, 0.05f * u)
                    gradPath(sg * 0.42f * u, -0.98f * u, sg * 0.2f * u, -0.3f * u, shadeColor(orange, 1.2f), shadeColor(orange, 0.65f))
                    pa.reset(); pa.moveTo(sg * 0.42f * u, -0.3f * u); pa.quadTo(sg * 0.42f * u, -0.55f * u, sg * 0.4f * u, -0.8f * u); pa.quadTo(sg * 0.26f * u, -0.6f * u, sg * 0.2f * u, -0.5f * u); pa.close()
                    gradPath(sg * 0.4f * u, -0.8f * u, sg * 0.25f * u, -0.4f * u, col(0xFFFFF4E6), col(0xFFE0B48E))
                    poly(col(0xFF1A1210), 0.95f, sg * 0.42f * u, -0.98f * u, sg * 0.4f * u, -0.8f * u, sg * 0.3f * u, -0.86f * u)
                    fur(sg * 0.33f * u, -0.55f * u, 0.12f * u, 0.22f * u, 40, 0.06f * u, 0.007f * u, orange, 13 + sg, -90f, 30f)
                    fur(sg * 0.38f * u, 0.12f * f.h, 0.2f * u, 0.1f * f.h, 50, 0.1f * u, 0.009f * u, col(0xFFFFF8F0), 15 + sg, if (sg < 0) 160f else 20f, 40f)
                }
            }
            gradOval(f.nx, f.ny, 0.07f * u, 0.05f * u, col(0xFF3A3A3A), col(0xFF050505))
            whiskers(f, WHITE)
        }
    }
    fx("lion", "Lion Mane", "🦁") {
        val mane = col(0xFF9A5A1E)
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                softOval(0f, 0.05f * f.h, 0.95f * u, 0.8f * f.h, BLACK, 0.25f, 0.12f * u)
                for (i in 0 until 520) {
                    val a = rnd(i, 1) * 6.2831855f
                    val r = (0.5f + 0.42f * rnd(i, 2)) * u
                    val x = cos(a) * r; val y = sin(a) * r * 1.0f * (f.h / u) * 1.05f
                    val l = (0.12f + 0.14f * rnd(i, 3)) * u
                    val col2 = shadeColor(mane, 0.5f + 1.0f * rnd(i, 4))
                    line(x, y, x + cos(a) * l, y + sin(a) * l, 0.016f * u, col2, 0.95f)
                }
            }
            roundEar(f, -1, 0.4f * u, -0.55f * u, 0.12f * u, col(0xFFB07030), col(0xFFE8B080), 5)
            roundEar(f, 1, 0.4f * u, -0.55f * u, 0.12f * u, col(0xFFB07030), col(0xFFE8B080), 6)
            at(f.nx, f.ny, f.rot) {
                poly(col(0xFFB05A5A), 1f, -0.08f * u, -0.03f * u, 0.08f * u, -0.03f * u, 0f, 0.06f * u)
                oval(-0.02f * u, -0.02f * u, 0.025f * u, 0.012f * u, WHITE, 0.6f)
            }
            whiskers(f, col(0xFFF4E6D0), 0.85f)
        }
    }
    fx("unicorn", "Unicorn", "🦄") {
        val bands = intArrayOf(col(0xFFFF5C8A), col(0xFFFFA64D), col(0xFFFFE14D), col(0xFF5CE08A), col(0xFF4DB8FF), col(0xFFB06CFF))
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    for (n in 0 until 6) {
                        val sway = sin(t * 2f + n) * 0.02f * u
                        for (j in 0 until 10) {
                            val y0 = (-0.55f + j * 0.08f) * u
                            line(sg * (0.5f + 0.02f * n) * u, y0, sg * (0.58f + 0.035f * n) * u + sway, y0 + 0.2f * u, 0.024f * u, shadeColor(bands[(n + j) % 6], 0.8f + 0.4f * rnd(j, n)), 0.9f)
                        }
                    }
                    roundEarShape(sg, u)
                }
                // horn
                c.save(); c.translate(0f, -0.58f * u); c.rotate(-6f)
                val pa = e.path
                pa.reset(); pa.moveTo(-0.09f * u, 0.04f * u); pa.lineTo(0.09f * u, 0.04f * u); pa.lineTo(0f, -0.6f * u); pa.close()
                val horn = Path(pa)
                gradPath(-0.09f * u, 0f, 0.09f * u, 0f, col(0xFFFFF6D0), col(0xFFE0A93A))
                c.save(); c.clipPath(horn)
                for (n in 0 until 6) line(-0.12f * u, (0.0f - n * 0.1f) * u, 0.12f * u, (-0.06f - n * 0.1f) * u, 0.022f * u, col(0xFFC98A1E), 0.8f)
                c.restore()
                c.drawPath(horn, stroke(col(0xFFB07A14), 0.008f * u))
                c.restore()
            }
            for (i in 0 until 6) { val a = (t * 0.8f + i * 0.17f) % 1f; sparkle(f.hx((i - 2.5f) * 0.3f * u, -0.7f * f.h - a * 0.3f * u), f.hy((i - 2.5f) * 0.3f * u, -0.7f * f.h - a * 0.3f * u), 0.05f * u * (1f - a), bands[i], 0.9f) }
        }
    }
    fx("heartcrown", "Heart Crown", "💕") {
        for (f in faces) {
            val u = f.w
            for (i in 0 until 7) {
                val a = Math.toRadians(10.0 + i * 26.7).toFloat()
                val dx = -cos(a) * 0.6f * u; val dy = -sin(a) * 0.66f * f.h - 0.05f * u
                val x = f.hx(dx, dy); val y = f.hy(dx, dy)
                val pulse = 1f + 0.15f * sin(t * 5f + i)
                lightBall(x, y, 0.16f * u * pulse, col(0xFFFF4D79), 0.45f)
                heart(x, y, (0.075f + 0.02f * (i % 2)) * u * pulse)
            }
        }
    }
    fx("sparkcrown", "Sparkle Crown", "✨") {
        val cols = intArrayOf(col(0xFFFFFFFF), col(0xFFFFE680), col(0xFFFFB3E6), col(0xFFB3E6FF))
        for (f in faces) {
            val u = f.w
            for (i in 0 until 11) {
                val a = Math.toRadians(4.0 + i * 17.2).toFloat()
                val dx = -cos(a) * 0.62f * u; val dy = -sin(a) * 0.68f * f.h - 0.04f * u
                val x = f.hx(dx, dy); val y = f.hy(dx, dy)
                val tw = 0.4f + 0.6f * abs(sin(t * 4f + i * 1.3f))
                lightBall(x, y, 0.12f * u * tw, cols[i % 4], 0.6f)
                sparkle(x, y, (0.05f + 0.04f * (i % 3)) * u * tw, cols[i % 4])
            }
        }
    }
    fx("animeeyes", "Anime Eyes", "🤩") {
        for (f in faces) {
            val d = f.eyeDist
            val hue = floatArrayOf((190f + 40f * sin(t * 0.7f)) % 360f, 0.7f, 0.95f)
            val iris = android.graphics.Color.HSVToColor(hue)
            for (i in 0..1) {
                val ex = if (i == 0) f.lx else f.rx; val ey = if (i == 0) f.ly else f.ry
                at(ex, ey, f.rot) {
                    gradOval(0f, 0f, 0.42f * d, 0.38f * d, WHITE, col(0xFFDCDCE6))
                    gradOval(0f, 0.02f * d, 0.27f * d, 0.31f * d, shadeColor(iris, 1.4f), shadeColor(iris, 0.45f))
                    gradOval(0f, 0.04f * d, 0.14f * d, 0.17f * d, col(0xFF0A0A14), col(0xFF000000))
                    disc(-0.1f * d, -0.1f * d, 0.085f * d, WHITE); disc(0.09f * d, 0.1f * d, 0.04f * d, WHITE, 0.85f)
                    ovalLine(0f, 0f, 0.42f * d, 0.38f * d, 0.045f * d, col(0xFF14101A))
                    e.rectF.set(-0.45f * d, -0.42f * d, 0.45f * d, 0.3f * d)
                    c.drawArc(e.rectF, 190f, 160f, false, stroke(col(0xFF0A0810), 0.09f * d))
                    for (n in 0..2) line((if (i == 0) -0.4f else 0.4f) * d, (-0.12f - n * 0.05f) * d, (if (i == 0) -0.58f else 0.58f) * d, (-0.2f - n * 0.1f) * d, 0.035f * d, col(0xFF0A0810))
                }
            }
        }
    }
    fx("glam", "Glam Makeup", "💄") {
        for (f in faces) {
            val u = f.w; val d = f.eyeDist
            for (sg in intArrayOf(-1, 1)) softOval(f.hx(sg * 0.3f * u, 0.14f * f.h), f.hy(sg * 0.3f * u, 0.14f * f.h), 0.14f * u, 0.09f * u, col(0xFFFF6B8A), 0.35f, 0.07f * u)
            for (i in 0..1) {
                val ex = if (i == 0) f.lx else f.rx; val ey = if (i == 0) f.ly else f.ry
                val sg = if (i == 0) -1 else 1
                softOval(ex, ey - 0.16f * d, 0.45f * d, 0.2f * d, col(0xFF8B3FD0), 0.4f, 0.1f * d)
                at(ex, ey, f.rot) {
                    val p = stroke(col(0xFF0A0A0A), 0.045f * d, 0.95f)
                    c.drawLine(-sg * 0.38f * d, -0.02f * d, sg * 0.5f * d, -0.1f * d, p)
                    c.drawLine(sg * 0.5f * d, -0.1f * d, sg * 0.72f * d, -0.28f * d, stroke(col(0xFF0A0A0A), 0.04f * d, 0.95f))
                    for (n in -2..2) line(n * 0.12f * d, -0.2f * d, n * 0.16f * d, -0.38f * d, 0.02f * d, col(0xFF0A0A0A), 0.9f)
                }
                softOval(f.hx(sg * 0.3f * u, 0.0f), f.hy(sg * 0.3f * u, 0.0f), 0.1f * u, 0.04f * u, WHITE, 0.3f, 0.04f * u)
            }
            at(f.mx, f.my, f.rot) {
                val lips = Path()
                lips.moveTo(-0.17f * u, 0f); lips.quadTo(-0.08f * u, -0.07f * u, 0f, -0.03f * u); lips.quadTo(0.08f * u, -0.07f * u, 0.17f * u, 0f)
                lips.quadTo(0.1f * u, 0.09f * u, 0f, 0.1f * u); lips.quadTo(-0.1f * u, 0.09f * u, -0.17f * u, 0f); lips.close()
                e.path.set(lips)
                val pp = fill(col(0xFFC8102E), 0.8f); pp.maskFilter = BlurMaskFilter(0.008f * u + 1f, BlurMaskFilter.Blur.NORMAL); c.drawPath(lips, pp); pp.maskFilter = null
                oval(-0.04f * u, 0.045f * u, 0.05f * u, 0.015f * u, WHITE, 0.45f)
            }
        }
    }
    fx("glitter", "Glitter Face", "💎") {
        val cols = intArrayOf(col(0xFFFFFFFF), col(0xFFFFE680), col(0xFFFFB3E6), col(0xFFB3E6FF), col(0xFFE0C0FF))
        for (f in faces) {
            val u = f.w
            for (i in 0 until 90) {
                val x = f.hx((rnd(i, 1) - 0.5f) * 0.9f * u, (rnd(i, 2) - 0.45f) * 0.9f * f.h)
                val y = f.hy((rnd(i, 1) - 0.5f) * 0.9f * u, (rnd(i, 2) - 0.45f) * 0.9f * f.h)
                val tw = abs(sin(t * (2f + 4f * rnd(i, 3)) + i))
                if (tw < 0.25f) continue
                val cc = cols[i % 5]
                disc(x, y, (0.006f + 0.008f * rnd(i, 4)) * u, cc, 0.9f * tw)
                if (i % 6 == 0) sparkle(x, y, 0.03f * u * tw, WHITE, 0.95f)
            }
        }
    }
    fx("neonglasses", "Neon Glasses", "🕶️") {
        for (f in faces) {
            val d = f.eyeDist
            val hue = floatArrayOf((t * 90f) % 360f, 0.85f, 1f)
            val neon = android.graphics.Color.HSVToColor(hue)
            at(f.ex, f.ey, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    val l = if (sg < 0) -1.02f * d else 0.05f * d; val r = if (sg < 0) -0.05f * d else 1.02f * d
                    gradRRect(l, -0.28f * d, r, 0.32f * d, 0.2f * d, col(0xAA101018), col(0xCC000008))
                    lightBall((l + r) / 2f, 0f, 0.8f * d, neon, 0.5f)
                    e.rectF.set(l, -0.28f * d, r, 0.32f * d)
                    c.drawRoundRect(e.rectF, 0.2f * d, 0.2f * d, stroke(neon, 0.075f * d))
                    c.drawRoundRect(e.rectF, 0.2f * d, 0.2f * d, stroke(WHITE, 0.02f * d, 0.9f))
                }
                line(-0.05f * d, -0.15f * d, 0.05f * d, -0.15f * d, 0.06f * d, neon)
                line(-1.02f * d, -0.12f * d, -1.35f * d, -0.18f * d, 0.06f * d, neon); line(1.02f * d, -0.12f * d, 1.35f * d, -0.18f * d, 0.06f * d, neon)
            }
        }
    }
    fx("robot", "Robot", "🤖") {
        tint(look(sat = 0.55f, con = 1.15f, r = 0.9f, g = 1.0f, b = 1.15f), 0.6f * k)
        for (f in faces) {
            val u = f.w; val h = f.h
            at(f.cx, f.cy, f.rot) {
                val pa = Path()
                pa.moveTo(-0.52f * u, -0.45f * h); pa.lineTo(0.02f * u, -0.5f * h); pa.lineTo(0.02f * u, 0.15f * h); pa.lineTo(-0.1f * u, 0.3f * h); pa.lineTo(-0.42f * u, 0.38f * h); pa.lineTo(-0.55f * u, 0.0f); pa.close()
                softOval(-0.25f * u, 0f, 0.3f * u, 0.5f * h, BLACK, 0.3f, 0.06f * u)
                e.path.set(pa)
                gradPath(-0.5f * u, -0.4f * h, 0f, 0.3f * h, col(0xFFE8EDF2), col(0xFF6A7380), 0.95f)
                c.drawPath(pa, stroke(col(0xFF2A3038), 0.012f * u))
                line(-0.3f * u, -0.45f * h, -0.3f * u, 0.35f * h, 0.008f * u, col(0xFF2A3038), 0.7f)
                line(-0.52f * u, -0.05f * h, 0f, -0.05f * h, 0.008f * u, col(0xFF2A3038), 0.7f)
                for (i in 0 until 4) gradOval((-0.45f + (i % 2) * 0.38f) * u, (-0.38f + (i / 2) * 0.55f) * h, 0.018f * u, 0.018f * u, WHITE, col(0xFF3A4048))
                line(0.1f * u, -0.55f * h, 0.12f * u, -0.85f * h, 0.015f * u, col(0xFF555B63)); disc(0.12f * u, -0.88f * h, 0.03f * u, col(0xFFFF2A2A))
            }
            val pulse = 0.6f + 0.4f * sin(t * 6f)
            lightBall(f.lx, f.ly, 0.5f * f.eyeDist, col(0xFFFF1E1E), 0.7f * pulse)
            gradOval(f.lx, f.ly, 0.2f * f.eyeDist, 0.2f * f.eyeDist, col(0xFFFF9A9A), col(0xFFC00000))
            disc(f.lx, f.ly, 0.07f * f.eyeDist, WHITE, 0.9f)
        }
    }
    fx("skeleton", "Skeleton", "💀") {
        tint(look(sat = 0.4f, con = 1.25f, bri = 0.02f), 0.55f * k)
        for (f in faces) {
            val u = f.w; val d = f.eyeDist
            for (i in 0..1) {
                val ex = if (i == 0) f.lx else f.rx; val ey = if (i == 0) f.ly else f.ry
                softOval(ex, ey, 0.38f * d, 0.34f * d, BLACK, 0.9f, 0.05f * d)
            }
            at(f.nx, f.ny, f.rot) { poly(BLACK, 0.9f, 0f, -0.07f * u, 0.05f * u, 0.05f * u, -0.05f * u, 0.05f * u) }
            for (sg in intArrayOf(-1, 1)) softOval(f.hx(sg * 0.32f * u, 0.12f * f.h), f.hy(sg * 0.32f * u, 0.12f * f.h), 0.12f * u, 0.12f * u, col(0xFF222222), 0.3f, 0.06f * u)
            at(f.mx, f.my, f.rot) {
                softOval(0f, 0.01f * u, 0.27f * u, 0.07f * u, BLACK, 0.5f, 0.02f * u)
                for (i in -4..3) gradRRect((i * 0.062f + 0.01f) * u, -0.04f * u, (i * 0.062f + 0.058f) * u, 0.06f * u, 0.015f * u, WHITE, col(0xFFCFCFC4), true, 0.95f)
                line(-0.27f * u, 0.01f * u, 0.27f * u, 0.01f * u, 0.008f * u, BLACK, 0.7f)
            }
        }
    }
    fx("witch", "Witch", "🧙\u200D♀️") {
        tint(look(sat = 1.1f, r = 0.8f, g = 1.1f, b = 0.82f), 0.5f * k)
        for (f in faces) {
            val u = f.w
            gradOval(f.hx(0.18f * u, 0.14f * f.h), f.hy(0.18f * u, 0.14f * f.h), 0.022f * u, 0.022f * u, col(0xFF8FB070), col(0xFF3A5A28))
            at(f.topX, f.topY + 0.05f * u, f.rot + 6f) {
                softOval(0f, 0.1f * u, 0.55f * u, 0.07f * u, BLACK, 0.4f, 0.06f * u)
                val cone = Path()
                cone.moveTo(-0.38f * u, 0.02f * u); cone.cubicTo(-0.3f * u, -0.4f * u, -0.1f * u, -0.8f * u, 0.3f * u, -1.15f * u)
                cone.cubicTo(0.2f * u, -0.9f * u, 0.3f * u, -0.5f * u, 0.38f * u, 0.02f * u); cone.close()
                e.path.set(cone)
                gradPath(-0.3f * u, 0f, 0.4f * u, -0.5f * u, col(0xFF3A2F4A), col(0xFF0C0814))
                gradRRect(-0.37f * u, -0.14f * u, 0.37f * u, -0.02f * u, 0.01f * u, col(0xFF7A3AB0), col(0xFF3A1060))
                gradRRect(-0.07f * u, -0.16f * u, 0.07f * u, 0.0f, 0.01f * u, col(0xFFE8D28A), col(0xFF9A7B1E))
                gradOval(0f, 0.03f * u, 0.6f * u, 0.07f * u, col(0xFF3A2F4A), col(0xFF08050E))
            }
        }
    }
    fx("bubblegum", "Bubblegum", "🎈") {
        for (f in faces) {
            val u = f.w
            val p = (t % 3.2f) / 3.2f
            if (p < 0.82f) {
                val r = (0.04f + 0.4f * (p / 0.82f)) * u
                val x = f.mx; val y = f.my - r * 0.2f
                softOval(x + 0.04f * u, y + r * 0.9f, r * 0.9f, r * 0.15f, BLACK, 0.2f, r * 0.2f)
                gradOval(x, y, r, r, col(0xFFFFB0D8), col(0xFFE0488C), 0.82f)
                ring(x, y, r, 0.01f * u, col(0xFFFF5CA8), 0.7f)
                oval(x - r * 0.4f, y - r * 0.45f, r * 0.28f, r * 0.16f, WHITE, 0.8f)
            } else {
                val q = (p - 0.82f) / 0.18f
                for (i in 0 until 14) {
                    val a = rnd(i, 3) * 6.28f; val rr = (0.3f + 0.3f * q) * u * (0.6f + 0.6f * rnd(i, 4))
                    disc(f.mx + cos(a) * rr, f.my + sin(a) * rr, 0.03f * u * (1f - q), col(0xFFFF7CB8), 1f - q)
                }
            }
        }
    }
    fx("sweat", "Nervous Sweat", "😰") {
        for (f in faces) {
            val u = f.w
            for (i in 0 until 5) {
                val p = (t * 0.6f + i * 0.2f) % 1f
                val sg = if (i % 2 == 0) -1 else 1
                val x = f.hx(sg * (0.28f + 0.12f * rnd(i, 1)) * u, (-0.4f + p * 0.8f) * f.h); val y = f.hy(sg * (0.28f + 0.12f * rnd(i, 1)) * u, (-0.4f + p * 0.8f) * f.h)
                drop(x, y, (0.035f + 0.015f * rnd(i, 2)) * u, 1f - p * 0.4f)
            }
            for (i in 0 until 3) { val p = (t * 0.9f + i * 0.33f) % 1f; drop(f.hx(0.62f * u, -0.45f * f.h + p * 0.3f * u), f.hy(0.62f * u, -0.45f * f.h + p * 0.3f * u), 0.04f * u, 1f - p) }
        }
    }
    fx("angry", "Angry", "😡") {
        tint(look(sat = 1.3f, r = 1.25f, g = 0.8f, b = 0.78f), 0.6f * k)
        for (f in faces) {
            val u = f.w; val d = f.eyeDist
            for (i in 0..1) {
                val ex = if (i == 0) f.lx else f.rx; val ey = if (i == 0) f.ly else f.ry
                val sg = if (i == 0) 1 else -1
                at(ex, ey - 0.3f * d, f.rot + sg * 18f) {
                    softOval(0f, 0.04f * d, 0.45f * d, 0.1f * d, BLACK, 0.3f, 0.06f * d)
                    gradRRect(-0.45f * d, -0.07f * d, 0.45f * d, 0.07f * d, 0.05f * d, col(0xFF3A2A22), col(0xFF120A06))
                    fur(0f, 0f, 0.42f * d, 0.06f * d, 30, 0.06f * d, 0.01f * d, col(0xFF2A1A12), 31 + i, -60f, 30f)
                }
            }
            for (i in 0..1) for (n in 0 until 4) {
                val p = (t * 0.7f + n * 0.25f + i * 0.1f) % 1f
                val sg = if (i == 0) -1 else 1
                val x = f.hx(sg * (0.62f + 0.12f * p) * u, -0.1f * f.h - p * 0.5f * u); val y = f.hy(sg * (0.62f + 0.12f * p) * u, -0.1f * f.h - p * 0.5f * u)
                val r = (0.05f + 0.08f * p) * u
                gradOval(x, y, r, r, col(0xFFFFFFFF), col(0xFFC8C8D0), 0.7f * (1f - p))
            }
            val pl = 1f + 0.15f * sin(t * 10f)
            at(f.hx(0.4f * u, -0.45f * f.h), f.hy(0.4f * u, -0.45f * f.h), f.rot) {
                for (n in 0 until 4) { val a = Math.toRadians(45.0 + n * 90.0).toFloat(); line(cos(a) * 0.02f * u, sin(a) * 0.02f * u, cos(a) * 0.1f * u * pl, sin(a) * 0.1f * u * pl, 0.025f * u, col(0xFFE0102A)) }
            }
        }
    }
    fx("boxer", "Boxer", "🥊") {
        for (f in faces) {
            val u = f.w; val d = f.eyeDist
            softOval(f.lx, f.ly + 0.05f * d, 0.5f * d, 0.38f * d, col(0xFF3A1A5A), 0.6f, 0.12f * d)
            softOval(f.lx, f.ly + 0.12f * d, 0.35f * d, 0.22f * d, col(0xFF1A0A2A), 0.5f, 0.08f * d)
            softOval(f.lx - 0.1f * d, f.ly - 0.15f * d, 0.3f * d, 0.12f * d, col(0xFFB04060), 0.3f, 0.08f * d)
            at(f.hx(0.3f * u, 0.12f * f.h), f.hy(0.3f * u, 0.12f * f.h), f.rot - 20f) {
                softOval(0.01f * u, 0.02f * u, 0.14f * u, 0.07f * u, BLACK, 0.3f, 0.02f * u)
                gradRRect(-0.14f * u, -0.06f * u, 0.14f * u, 0.06f * u, 0.03f * u, col(0xFFF0D2B0), col(0xFFD4A87C))
                gradRRect(-0.04f * u, -0.06f * u, 0.04f * u, 0.06f * u, 0f, col(0xFFFFE8D0), col(0xFFE0B88C))
                line(-0.14f * u, 0f, 0.14f * u, 0f, 0.006f * u, col(0x22000000))
            }
            softOval(f.mx + 0.12f * u, f.my + 0.02f * u, 0.03f * u, 0.015f * u, col(0xFFB00020), 0.7f, 0.01f * u)
        }
    }
    fx("cryriver", "Crying Rivers", "😭") {
        for (f in faces) {
            val u = f.w
            for (sg in intArrayOf(-1, 1)) {
                val ex = if (sg < 0) f.lx else f.rx; val ey = if (sg < 0) f.ly else f.ry
                softOval(ex, ey + 0.05f * u, 0.17f * u, 0.07f * u, col(0xFFFF4D5A), 0.35f, 0.04f * u)
                for (n in 0 until 3) {
                    val sway = sin(t * 4f + n + sg) * 0.02f * u
                    val pa = e.path
                    pa.reset(); pa.moveTo(ex + (n - 1) * 0.03f * u, ey + 0.05f * u)
                    pa.cubicTo(ex + sg * 0.08f * u + sway, ey + 0.4f * u, ex + sg * 0.14f * u - sway, ey + (hf - ey) * 0.5f, ex + sg * 0.1f * u + sway, hf + 0.1f * u)
                    val g = stroke(col(0xFF79CFFF), (0.07f - n * 0.015f) * u, 0.45f); g.maskFilter = BlurMaskFilter(0.02f * u + 1f, BlurMaskFilter.Blur.NORMAL); c.drawPath(pa, g); g.maskFilter = null
                    c.drawPath(pa, stroke(col(0xFFD9F4FF), 0.012f * u, 0.7f))
                }
                for (i in 0 until 4) { val p = (t * 1.3f + i * 0.25f) % 1f; drop(ex + sg * (0.05f + 0.12f * p) * u, ey + 0.1f * u + p * 0.9f * f.h, 0.04f * u * (1f - 0.3f * p), 1f - p * 0.5f) }
            }
        }
    }
    fx("cupid", "Cupid", "💘") {
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy - 0.38f * f.h, f.rot - 10f) {
                softOval(0f, 0.04f * u, 0.7f * u, 0.03f * u, BLACK, 0.3f, 0.03f * u)
                line(-0.65f * u, 0f, 0.62f * u, 0f, 0.03f * u, col(0xFF8A5A30))
                line(-0.65f * u, -0.008f * u, 0.62f * u, -0.008f * u, 0.01f * u, col(0xFFC9955E))
                poly(col(0xFFC0C6CC), 1f, 0.62f * u, -0.05f * u, 0.8f * u, 0f, 0.62f * u, 0.05f * u)
                for (n in 0..2) { poly(col(0xFFF7F0F0), 1f, (-0.65f + n * 0.07f) * u, 0f, (-0.58f + n * 0.07f) * u, -0.07f * u, (-0.72f + n * 0.07f) * u, -0.07f * u); poly(col(0xFFF7F0F0), 1f, (-0.65f + n * 0.07f) * u, 0f, (-0.58f + n * 0.07f) * u, 0.07f * u, (-0.72f + n * 0.07f) * u, 0.07f * u) }
            }
            for (i in 0 until 6) {
                val p = (t * 0.5f + i * 0.17f) % 1f
                val x = f.cx + (rnd(i, 1) - 0.5f) * 1.6f * u; val y = f.cy - p * 1.3f * f.h
                heart(x, y, (0.05f + 0.04f * rnd(i, 2)) * u, 1f - p)
            }
        }
    }
    fx("icequeen", "Ice Queen", "❄️") {
        tint(look(sat = 0.85f, r = 0.88f, g = 1.02f, b = 1.2f), 0.55f * k)
        for (f in faces) {
            val u = f.w
            at(f.topX, f.topY + 0.04f * u, f.rot) {
                softOval(0f, 0.06f * u, 0.4f * u, 0.05f * u, BLACK, 0.3f, 0.05f * u)
                val xs = floatArrayOf(-0.36f, -0.24f, -0.12f, 0f, 0.12f, 0.24f, 0.36f)
                val hs = floatArrayOf(0.3f, 0.5f, 0.38f, 0.72f, 0.38f, 0.5f, 0.3f)
                for (i in xs.indices) {
                    val pa = e.path
                    pa.reset(); pa.moveTo((xs[i] - 0.06f) * u, 0.03f * u); pa.lineTo(xs[i] * u, -hs[i] * u); pa.lineTo((xs[i] + 0.06f) * u, 0.03f * u); pa.close()
                    gradPath((xs[i] - 0.06f) * u, 0f, (xs[i] + 0.06f) * u, -hs[i] * u, col(0xCCFFFFFF), col(0xCC6EC8FF))
                    c.drawPath(pa, stroke(col(0xFFEAF8FF), 0.008f * u, 0.9f))
                    line(xs[i] * u - 0.01f * u, 0f, xs[i] * u - 0.005f * u, -hs[i] * 0.8f * u, 0.008f * u, WHITE, 0.6f)
                }
                gradRRect(-0.4f * u, -0.02f * u, 0.4f * u, 0.06f * u, 0.02f * u, col(0xFFE8F6FF), col(0xFF7AB8E8))
                lightBall(0f, -0.72f * u, 0.15f * u, col(0xFFBFEAFF), 0.7f)
            }
            for (i in 0 until 20) {
                val x = f.hx((rnd(i, 1) - 0.5f) * 0.8f * u, (rnd(i, 2) - 0.3f) * 0.9f * f.h); val y = f.hy((rnd(i, 1) - 0.5f) * 0.8f * u, (rnd(i, 2) - 0.3f) * 0.9f * f.h)
                val tw = abs(sin(t * 3f + i)); sparkle(x, y, 0.03f * u * tw, WHITE, 0.9f)
            }
        }
    }
}

private fun FxCtx.roundEarShape(sg: Int, u: Float) {
    val pa = e.path
    pa.reset(); pa.moveTo(sg * 0.5f * u, -0.2f * u); pa.quadTo(sg * 0.5f * u, -0.55f * u, sg * 0.44f * u, -0.82f * u); pa.quadTo(sg * 0.22f * u, -0.66f * u, sg * 0.1f * u, -0.54f * u); pa.close()
    gradPath(sg * 0.44f * u, -0.82f * u, sg * 0.2f * u, -0.3f * u, col(0xFFFFFFFF), col(0xFFC8C8D4))
    pa.reset(); pa.moveTo(sg * 0.42f * u, -0.32f * u); pa.quadTo(sg * 0.42f * u, -0.5f * u, sg * 0.4f * u, -0.7f * u); pa.quadTo(sg * 0.27f * u, -0.58f * u, sg * 0.2f * u, -0.52f * u); pa.close()
    gradPath(sg * 0.4f * u, -0.7f * u, sg * 0.25f * u, -0.4f * u, col(0xFFFFC9D6), col(0xFFD98AA0))
}
