package com.example.filtery

import android.graphics.Path
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private fun col(v: Long): Int = v.toInt()
private val BLACK = col(0xFF000000)
private val WHITE = col(0xFFFFFFFF)
private val RED = col(0xFFE53935)
private val YELLOW = col(0xFFFFD600)
private val GOLD1 = col(0xFFFFE27A)
private val GOLD2 = col(0xFFB8860B)

private fun MutableList<Fx>.fx(id: String, name: String, icon: String, draw: FxCtx.() -> Unit) {
    add(Fx(id, name, Cat.FACES, icon, true, draw))
}

// ------------------------------------------------------------------------------------------ shared shapes
private fun FxCtx.heart(x: Float, y: Float, s: Float, a: Float = 1f) {
    val pa = e.path
    pa.reset()
    pa.moveTo(x, y + 0.55f * s)
    pa.cubicTo(x - 1.0f * s, y - 0.05f * s, x - 0.6f * s, y - 0.85f * s, x, y - 0.3f * s)
    pa.cubicTo(x + 0.6f * s, y - 0.85f * s, x + 1.0f * s, y - 0.05f * s, x, y + 0.55f * s)
    pa.close()
    gradPath(x - s, y - s, x + 0.6f * s, y + 0.6f * s, col(0xFFFF7C9A), col(0xFFC4122F), a)
    oval(x - 0.4f * s, y - 0.35f * s, 0.2f * s, 0.12f * s, WHITE, 0.55f * a)
}

private fun FxCtx.drop(x: Float, y: Float, r: Float, a: Float = 1f) {
    val pa = e.path
    pa.reset()
    pa.moveTo(x, y - 1.8f * r)
    pa.cubicTo(x + 0.5f * r, y - 0.8f * r, x + r, y - 0.4f * r, x + r, y + 0.2f * r)
    pa.cubicTo(x + r, y + 0.95f * r, x - r, y + 0.95f * r, x - r, y + 0.2f * r)
    pa.cubicTo(x - r, y - 0.4f * r, x - 0.5f * r, y - 0.8f * r, x, y - 1.8f * r)
    pa.close()
    gradPath(x - r, y - r, x + r, y + r, col(0xFFD9F4FF), col(0xFF3AA6E0), 0.85f * a)
    c.drawPath(pa, stroke(col(0xFF1E78B4), 0.012f * r * 10f, 0.5f * a))
    oval(x - 0.35f * r, y - 0.2f * r, 0.2f * r, 0.35f * r, WHITE, 0.85f * a)
}

private fun FxCtx.flower(x: Float, y: Float, r: Float, petal1: Int, petal2: Int, centre: Int) {
    softOval(x + 0.1f * r, y + 0.2f * r, r * 0.95f, r * 0.8f, BLACK, 0.3f, r * 0.35f)
    for (i in 0 until 6) {
        at(x, y, i * 60f + 15f) { gradOval(0f, -0.6f * r, 0.36f * r, 0.62f * r, petal1, petal2) }
    }
    gradOval(x, y, 0.3f * r, 0.3f * r, col(0xFFFFE680), centre)
}

private fun FxCtx.bat(x: Float, y: Float, s: Float, flap: Float) {
    val w = s * (0.35f + 0.65f * abs(flap))
    val pa = e.path
    pa.reset()
    pa.moveTo(x, y - 0.1f * s)
    pa.quadTo(x - 0.5f * w, y - 0.6f * s, x - w, y - 0.2f * s)
    pa.quadTo(x - 0.75f * w, y - 0.05f * s, x - 0.6f * w, y + 0.15f * s)
    pa.quadTo(x - 0.35f * w, y, x, y + 0.2f * s)
    pa.quadTo(x + 0.35f * w, y, x + 0.6f * w, y + 0.15f * s)
    pa.quadTo(x + 0.75f * w, y - 0.05f * s, x + w, y - 0.2f * s)
    pa.quadTo(x + 0.5f * w, y - 0.6f * s, x, y - 0.1f * s)
    pa.close()
    gradPath(x, y - 0.6f * s, x, y + 0.3f * s, col(0xFF3A3340), col(0xFF0D0A10))
    disc(x, y, 0.12f * s, col(0xFF1A1620))
}

/** A beard that follows the jaw and leaves the mouth free. Drawn in the head's own frame. */
private fun FxCtx.beard(f: PFace, base: Int, lengthF: Float, count: Int, seed: Int) {
    val u = f.w; val h = f.h
    at(f.cx, f.cy, f.rot) {
        softOval(0f, 0.6f * h, 0.5f * u, 0.1f * h, BLACK, 0.22f, 0.05f * u)
        val jaw = Path(); jaw.addOval(-0.56f * u, -0.06f * h, 0.56f * u, 0.66f * h, Path.Direction.CW)
        val hole1 = Path(); hole1.addOval(-0.3f * u, -0.17f * h, 0.3f * u, 0.33f * h, Path.Direction.CW)
        val hole2 = Path(); hole2.addOval(-0.21f * u, 0.205f * h, 0.21f * u, 0.355f * h, Path.Direction.CW)
        jaw.op(hole1, Path.Op.DIFFERENCE); jaw.op(hole2, Path.Op.DIFFERENCE)
        e.path.set(jaw)
        gradPath(0f, 0f, 0f, 0.7f * h, shadeColor(base, 0.8f), shadeColor(base, 0.45f), 0.92f)
        val skip = { x: Float, y: Float ->
            val a = x / (0.3f * u); val b = (y - 0.08f * h) / (0.25f * h)
            val m = x / (0.21f * u); val n = (y - 0.28f * h) / (0.075f * h)
            (a * a + b * b < 1f) || (m * m + n * n < 1f)
        }
        fur(0f, 0.3f * h, 0.58f * u, 0.38f * h, count, 0.16f * u * lengthF, 0.012f * u, base, seed, 90f, 50f, skip)
        fur(0f, 0.42f * h, 0.5f * u, 0.3f * h, count / 2, 0.2f * u * lengthF, 0.011f * u, shadeColor(base, 1.18f), seed + 40, 90f, 36f, skip)
    }
}

private fun FxCtx.mustacheFur(f: PFace, base: Int, size: Float, seed: Int) {
    val u = f.w
    at((f.nx + f.mx) / 2f, (f.ny + f.my) / 2f + 0.01f * u, f.rot) {
        val s = size
        softOval(0f, 0.07f * u * s, 0.3f * u * s, 0.04f * u * s, BLACK, 0.3f, 0.03f * u)
        val pa = e.path
        pa.reset()
        pa.moveTo(0f, -0.03f * u * s)
        pa.cubicTo(0.1f * u * s, -0.11f * u * s, 0.3f * u * s, -0.08f * u * s, 0.38f * u * s, 0.08f * u * s)
        pa.cubicTo(0.26f * u * s, 0.0f, 0.12f * u * s, 0.03f * u * s, 0f, 0.05f * u * s)
        pa.cubicTo(-0.12f * u * s, 0.03f * u * s, -0.26f * u * s, 0.0f, -0.38f * u * s, 0.08f * u * s)
        pa.cubicTo(-0.3f * u * s, -0.08f * u * s, -0.1f * u * s, -0.11f * u * s, 0f, -0.03f * u * s)
        pa.close()
        gradPath(0f, -0.1f * u * s, 0f, 0.08f * u * s, shadeColor(base, 1.1f), shadeColor(base, 0.55f))
        fur(0.17f * u * s, 0.0f, 0.2f * u * s, 0.045f * u * s, 60, 0.1f * u * s, 0.008f * u, base, seed, 15f, 40f)
        fur(-0.17f * u * s, 0.0f, 0.2f * u * s, 0.045f * u * s, 60, 0.1f * u * s, 0.008f * u, base, seed + 9, 165f, 40f)
        fur(0f, 0f, 0.3f * u * s, 0.05f * u * s, 40, 0.07f * u * s, 0.007f * u, shadeColor(base, 1.3f), seed + 20, 90f, 90f)
    }
}

internal fun MutableList<Fx>.addFaceFilters() {
    fx("party", "Party Hat", "🥳") {
        for (f in faces) {
            val u = f.w
            at(f.topX, f.topY, f.rot - 12f) {
                softOval(0f, 0.08f * u, 0.36f * u, 0.06f * u, BLACK, 0.35f, 0.05f * u)
                val pa = e.path
                pa.reset(); pa.moveTo(-0.3f * u, 0.06f * u); pa.lineTo(0.3f * u, 0.06f * u); pa.lineTo(0f, -0.88f * u); pa.close()
                val tri = Path(pa)
                gradPath(-0.3f * u, 0f, 0.3f * u, 0f, col(0xFFFF7BBF), col(0xFFC21A78))
                c.save(); c.clipPath(tri)
                for (i in -3..3) line(-0.5f * u + i * 0.2f * u, 0.1f * u, -0.1f * u + i * 0.2f * u, -0.9f * u, 0.07f * u, col(0xFFFFE14D), 0.9f)
                gradRRect(0.08f * u, -0.9f * u, 0.4f * u, 0.1f * u, 0f, col(0x00000000), col(0x55000000), false)
                c.restore()
                oval(0f, 0.06f * u, 0.3f * u, 0.035f * u, col(0xFFFFE14D))
                for (i in 0 until 26) { val a = rnd(i, 3) * 6.28f; disc(cos(a) * 0.05f * u * rnd(i, 4), -0.9f * u + sin(a) * 0.05f * u * rnd(i, 5), 0.018f * u, col(0xFFFFD21F)) }
                gradOval(0f, -0.9f * u, 0.07f * u, 0.07f * u, col(0xFFFFF19A), col(0xFFE0A800))
            }
        }
    }
    fx("tophat", "Top Hat", "🎩") {
        for (f in faces) {
            val u = f.w
            at(f.topX, f.topY + 0.03f * u, f.rot) {
                softOval(0f, 0.07f * u, 0.4f * u, 0.07f * u, BLACK, 0.45f, 0.06f * u)
                gradRRect(-0.27f * u, -0.56f * u, 0.27f * u, 0f, 0.03f * u, col(0xFF4A4A4F), col(0xFF0F0F12), false)
                gradRRect(-0.2f * u, -0.55f * u, -0.12f * u, -0.02f * u, 0.03f * u, col(0x66FFFFFF), col(0x00FFFFFF), false)
                gradRRect(-0.27f * u, -0.16f * u, 0.27f * u, -0.04f * u, 0f, col(0xFFB3202A), col(0xFF6B0F16))
                oval(0f, -0.56f * u, 0.27f * u, 0.05f * u, col(0xFF2A2A2E))
                ovalLine(0f, -0.56f * u, 0.25f * u, 0.04f * u, 0.008f * u, col(0x66FFFFFF))
                gradOval(0f, 0f, 0.46f * u, 0.085f * u, col(0xFF45454A), col(0xFF0A0A0C))
                ovalLine(0f, 0.005f * u, 0.45f * u, 0.08f * u, 0.01f * u, col(0x44FFFFFF))
            }
        }
    }
    fx("crown", "King Crown", "👑") {
        for (f in faces) {
            val u = f.w
            at(f.topX, f.topY + 0.02f * u, f.rot) {
                softOval(0f, 0.06f * u, 0.32f * u, 0.05f * u, BLACK, 0.4f, 0.05f * u)
                val pa = e.path
                pa.reset()
                pa.moveTo(-0.3f * u, 0.04f * u); pa.lineTo(-0.32f * u, -0.4f * u); pa.lineTo(-0.15f * u, -0.2f * u); pa.lineTo(0f, -0.54f * u)
                pa.lineTo(0.15f * u, -0.2f * u); pa.lineTo(0.32f * u, -0.4f * u); pa.lineTo(0.3f * u, 0.04f * u); pa.close()
                val shape = Path(pa)
                gradPath(-0.3f * u, 0f, 0.3f * u, 0f, GOLD2, GOLD1)
                gradPath(0f, -0.5f * u, 0f, 0.05f * u, col(0x66FFFFFF), col(0x33000000))
                c.drawPath(shape, stroke(col(0xFF8A6508), 0.016f * u))
                for (xx in floatArrayOf(-0.32f, 0f, 0.32f)) gradOval(xx * u, (if (xx == 0f) -0.55f else -0.42f) * u, 0.04f * u, 0.04f * u, col(0xFFFFF3B0), GOLD2)
                gradRRect(-0.3f * u, -0.04f * u, 0.3f * u, 0.05f * u, 0f, col(0xFF8B1A2B), col(0xFF4A0A14))
                gradOval(0f, -0.1f * u, 0.05f * u, 0.05f * u, col(0xFFFF8A8A), col(0xFFB0001A))
                gradOval(-0.19f * u, -0.06f * u, 0.035f * u, 0.035f * u, col(0xFFA8E0FF), col(0xFF1565C0))
                gradOval(0.19f * u, -0.06f * u, 0.035f * u, 0.035f * u, col(0xFFB6FFC4), col(0xFF1B8A3A))
            }
        }
    }
    fx("shades", "Cool Shades", "😎") {
        for (f in faces) {
            val d = f.eyeDist
            at(f.ex, f.ey, f.rot) {
                softOval(0f, 0.2f * d, 1.0f * d, 0.3f * d, BLACK, 0.28f, 0.12f * d)
                for (sg in intArrayOf(-1, 1)) {
                    val l = if (sg < 0) -1.02f * d else 0.05f * d; val r = if (sg < 0) -0.05f * d else 1.02f * d
                    gradRRect(l, -0.3f * d, r, 0.34f * d, 0.2f * d, col(0xFF20242B), col(0xFF050507), true, 0.96f)
                    gradRRect(l + 0.05f * d, -0.26f * d, r - 0.05f * d, 0.02f * d, 0.14f * d, col(0x44A8C8FF), col(0x00A8C8FF))
                    line(l + 0.12f * d, -0.2f * d, l + 0.32f * d, 0.05f * d, 0.05f * d, WHITE, 0.3f)
                    e.rectF.set(l, -0.3f * d, r, 0.34f * d)
                    c.drawRoundRect(e.rectF, 0.2f * d, 0.2f * d, stroke(col(0xFF111111), 0.06f * d))
                    c.drawRoundRect(e.rectF, 0.2f * d, 0.2f * d, stroke(col(0x55FFFFFF), 0.012f * d))
                }
                line(-0.07f * d, -0.18f * d, 0.07f * d, -0.18f * d, 0.07f * d, col(0xFF111111))
                line(-1.02f * d, -0.15f * d, -1.4f * d, -0.2f * d, 0.07f * d, col(0xFF111111))
                line(1.02f * d, -0.15f * d, 1.4f * d, -0.2f * d, 0.07f * d, col(0xFF111111))
            }
        }
    }
    fx("nerd", "Nerd Glasses", "🤓") {
        for (f in faces) {
            val d = f.eyeDist
            at(f.ex, f.ey, f.rot) {
                softOval(0f, 0.12f * d, 1.0f * d, 0.5f * d, BLACK, 0.22f, 0.1f * d)
                val frame = col(0xFF1B1A1C)
                for (sg in intArrayOf(-1, 1)) {
                    val cx = sg * 0.52f * d
                    disc(cx, 0f, 0.46f * d, col(0xFFCDE6FF), 0.1f)
                    gradOval(cx - 0.12f * d, -0.14f * d, 0.18f * d, 0.1f * d, col(0x66FFFFFF), col(0x00FFFFFF))
                    ring(cx, 0f, 0.5f * d, 0.1f * d, frame)
                    e.rectF.set(cx - 0.46f * d, -0.46f * d, cx + 0.46f * d, 0.46f * d)
                    c.drawArc(e.rectF, 200f, 80f, false, stroke(col(0x66FFFFFF), 0.025f * d))
                }
                line(-0.05f * d, -0.05f * d, 0.05f * d, -0.05f * d, 0.1f * d, frame)
                line(-1.02f * d, -0.05f * d, -1.4f * d, -0.1f * d, 0.08f * d, frame)
                line(1.02f * d, -0.05f * d, 1.4f * d, -0.1f * d, 0.08f * d, frame)
                // tape on the bridge
                gradRRect(-0.14f * d, -0.14f * d, 0.14f * d, 0.12f * d, 0.02f * d, col(0xFFF2EEE2), col(0xFFCFC9B8))
                line(-0.14f * d, -0.14f * d, 0.14f * d, 0.12f * d, 0.014f * d, col(0xFF8A8576))
                line(0.14f * d, -0.14f * d, -0.14f * d, 0.12f * d, 0.014f * d, col(0xFF8A8576))
            }
        }
    }
    fx("dealwithit", "Deal With It", "🕶️") {
        for (f in faces) {
            val d = f.eyeDist
            val p = ((t % 4.5f) / 0.8f).coerceIn(0f, 1f)
            val ease = 1f - (1f - p) * (1f - p) * (1f - p)
            val dy = (1f - ease) * -0.9f * f.h
            val px = 0.2f * d
            at(f.ex, f.ey + dy, f.rot) {
                val blk = col(0xFF0A0A0A)
                // pixel-art glasses: two 5x3 lenses, a bridge and arms
                for (sg in intArrayOf(-1, 1)) {
                    val x0 = if (sg < 0) -5.2f * px else 0.2f * px
                    for (i in 0 until 5) for (j in 0 until 3) {
                        val cut = (j == 2 && (i == 0 || i == 4))
                        if (!cut) rect(x0 + i * px, -1.2f * px + j * px, x0 + (i + 1) * px + 0.5f, -0.2f * px + j * px + 0.5f, blk)
                    }
                    rect(x0 + 0.5f * px, -1.1f * px, x0 + 1.5f * px, -0.6f * px, WHITE, 0.9f)
                }
                rect(-0.2f * px, -1.2f * px, 0.2f * px, -0.6f * px, blk)
                rect(-6.2f * px, -1.2f * px, -5.2f * px, -0.6f * px, blk); rect(5.2f * px, -1.2f * px, 6.2f * px, -0.6f * px, blk)
            }
        }
    }
    fx("hearteyes", "Heart Eyes", "😍") {
        for (f in faces) {
            val pulse = 1f + 0.12f * sin(t * 8f)
            val s = f.eyeDist * 0.42f * pulse
            lightBall(f.lx, f.ly, s * 1.9f, col(0xFFFF4D79), 0.35f); lightBall(f.rx, f.ry, s * 1.9f, col(0xFFFF4D79), 0.35f)
            heart(f.lx, f.ly, s); heart(f.rx, f.ry, s)
        }
    }
    fx("clown", "Clown", "🤡") {
        val wig = intArrayOf(col(0xFFFF5A3C), col(0xFFFFA11F), col(0xFFE23A7B))
        for (f in faces) {
            val u = f.w
            // face paint first, so it sits on the skin
            for (sg in intArrayOf(-1, 1)) {
                val ex = if (sg < 0) f.lx else f.rx; val ey = if (sg < 0) f.ly else f.ry
                softOval(ex, ey - 0.02f * u, 0.17f * u, 0.12f * u, WHITE, 0.55f, 0.04f * u)
                at(ex, ey - 0.15f * u, f.rot + sg * 8f) { poly(col(0xFF2A7FFF), 0.8f, 0f, -0.07f * u, 0.04f * u, 0f, 0f, 0.07f * u, -0.04f * u, 0f) }
                softOval(f.hx(sg * 0.3f * u, 0.14f * f.h), f.hy(sg * 0.3f * u, 0.14f * f.h), 0.1f * u, 0.07f * u, col(0xFFFF3B3B), 0.4f, 0.05f * u)
            }
            at(f.mx, f.my, f.rot) {
                e.rectF.set(-0.3f * u, -0.14f * u, 0.3f * u, 0.16f * u)
                val p = stroke(col(0xFFD81B2A), 0.075f * u); p.maskFilter = android.graphics.BlurMaskFilter(0.01f * u + 1f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                c.drawArc(e.rectF, 12f, 156f, false, p); p.maskFilter = null
            }
            for (sg in intArrayOf(-1, 1)) {
                for (n in 0 until 9) {
                    val ang = -0.5f + n * 0.2f
                    val bx = f.hx(sg * (0.52f + 0.1f * cos(n * 1.3f)) * u, (-0.1f - n * 0.07f) * f.h); val by = f.hy(sg * (0.52f + 0.1f * cos(n * 1.3f)) * u, (-0.1f - n * 0.07f) * f.h)
                    gradOval(bx, by, 0.12f * u, 0.12f * u, shadeColor(wig[n % 3], 1.35f), shadeColor(wig[n % 3], 0.6f))
                    if (ang > 9f) break
                }
            }
            for (n in 0 until 7) { val bx = f.hx((-0.36f + n * 0.12f) * u, -0.58f * f.h); val by = f.hy((-0.36f + n * 0.12f) * u, -0.58f * f.h); gradOval(bx, by, 0.11f * u, 0.11f * u, shadeColor(wig[(n + 1) % 3], 1.35f), shadeColor(wig[(n + 1) % 3], 0.6f)) }
            softOval(f.nx + 0.02f * u, f.ny + 0.1f * u, 0.14f * u, 0.05f * u, BLACK, 0.3f, 0.04f * u)
            gradOval(f.nx, f.ny, 0.15f * u, 0.15f * u, col(0xFFFF8A80), col(0xFFB3000F))
            oval(f.nx - 0.05f * u, f.ny - 0.06f * u, 0.04f * u, 0.025f * u, WHITE, 0.85f)
        }
    }
    fx("dog", "Puppy", "🐶") {
        val fur = col(0xFF8D5B2E)
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    c.save(); c.translate(sg * 0.52f * u, -0.38f * u); c.rotate(sg * 24f)
                    softOval(sg * 0.02f * u, 0.18f * u, 0.18f * u, 0.3f * u, BLACK, 0.25f, 0.05f * u)
                    gradOval(0f, 0.12f * u, 0.17f * u, 0.31f * u, shadeColor(fur, 1.25f), shadeColor(fur, 0.55f))
                    gradOval(0f, 0.15f * u, 0.09f * u, 0.2f * u, col(0xFFC9857F), col(0xFF5E3A2A), 0.7f)
                    fur(0f, 0.12f * u, 0.17f * u, 0.3f * u, 70, 0.07f * u, 0.008f * u, fur, 5 + sg, 90f, 25f)
                    c.restore()
                }
            }
            at(f.nx, f.ny, f.rot) {
                softOval(0f, 0.07f * u, 0.12f * u, 0.03f * u, BLACK, 0.3f, 0.03f * u)
                gradOval(0f, 0f, 0.13f * u, 0.095f * u, col(0xFF444444), col(0xFF050505))
                oval(-0.045f * u, -0.04f * u, 0.05f * u, 0.022f * u, WHITE, 0.7f)
                oval(-0.05f * u, 0.015f * u, 0.016f * u, 0.022f * u, BLACK, 0.8f); oval(0.05f * u, 0.015f * u, 0.016f * u, 0.022f * u, BLACK, 0.8f)
            }
            at(f.mx, f.my, f.rot) {
                val len = 0.17f * u + 0.04f * u * sin(t * 7f)
                softOval(0f, len * 0.6f, 0.08f * u, 0.05f * u, BLACK, 0.3f, 0.03f * u)
                gradRRect(-0.075f * u, -0.01f * u, 0.075f * u, len, 0.075f * u, col(0xFFFF8FA8), col(0xFFD6406C))
                line(0f, 0.02f * u, 0f, len * 0.7f, 0.012f * u, col(0xFFB52A55))
                oval(-0.03f * u, len * 0.35f, 0.015f * u, 0.03f * u, WHITE, 0.5f)
            }
        }
    }
    fx("cat", "Kitty", "🐱") {
        val fur = col(0xFF8E8E92)
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    val pa = e.path
                    pa.reset(); pa.moveTo(sg * 0.54f * u, -0.2f * u); pa.quadTo(sg * 0.5f * u, -0.62f * u, sg * 0.46f * u, -0.9f * u); pa.quadTo(sg * 0.22f * u, -0.7f * u, sg * 0.1f * u, -0.54f * u); pa.close()
                    softOval(sg * 0.35f * u, -0.55f * u, 0.18f * u, 0.3f * u, BLACK, 0.2f, 0.05f * u)
                    gradPath(sg * 0.54f * u, -0.9f * u, sg * 0.2f * u, -0.3f * u, shadeColor(fur, 1.25f), shadeColor(fur, 0.6f))
                    pa.reset(); pa.moveTo(sg * 0.44f * u, -0.3f * u); pa.quadTo(sg * 0.43f * u, -0.55f * u, sg * 0.42f * u, -0.74f * u); pa.quadTo(sg * 0.26f * u, -0.6f * u, sg * 0.2f * u, -0.52f * u); pa.close()
                    gradPath(sg * 0.44f * u, -0.74f * u, sg * 0.25f * u, -0.4f * u, col(0xFFFFC3CF), col(0xFFC77088))
                    fur(sg * 0.38f * u, -0.55f * u, 0.12f * u, 0.22f * u, 40, 0.06f * u, 0.007f * u, fur, 11 + sg, -90f, 30f)
                }
            }
            at(f.nx, f.ny, f.rot) {
                val pa = e.path
                pa.reset(); pa.moveTo(-0.065f * u, -0.03f * u); pa.quadTo(0f, -0.06f * u, 0.065f * u, -0.03f * u); pa.quadTo(0.03f * u, 0.03f * u, 0f, 0.05f * u); pa.quadTo(-0.03f * u, 0.03f * u, -0.065f * u, -0.03f * u); pa.close()
                gradPath(0f, -0.06f * u, 0f, 0.05f * u, col(0xFFFF9FB5), col(0xFFC24A6E))
                oval(-0.02f * u, -0.03f * u, 0.02f * u, 0.01f * u, WHITE, 0.6f)
                line(0f, 0.05f * u, 0f, 0.09f * u, 0.008f * u, col(0xFF6A2A3A), 0.8f)
                for (sg in intArrayOf(-1, 1)) {
                    e.rectF.set(sg * 0.0f - 0.06f * u + sg * 0.06f * u, 0.07f * u, sg * 0.0f + 0.06f * u + sg * 0.06f * u, 0.17f * u)
                    c.drawArc(e.rectF, if (sg < 0) 200f else -20f, 120f, false, stroke(col(0xFF5A3A3A), 0.008f * u, 0.7f))
                    for (j in -1..1) {
                        val yy = 0.06f * u + j * 0.055f * u
                        line(sg * 0.15f * u, yy, sg * 0.72f * u, yy + j * 0.1f * u - 0.02f * u, 0.014f * u, BLACK, 0.35f)
                        line(sg * 0.15f * u, yy - 0.008f * u, sg * 0.72f * u, yy + j * 0.1f * u - 0.028f * u, 0.009f * u, WHITE, 0.95f)
                    }
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
                    softOval(sg * 0.03f * u, -0.3f * u, 0.14f * u, 0.42f * u, BLACK, 0.22f, 0.05f * u)
                    gradOval(0f, -0.34f * u, 0.13f * u, 0.43f * u, col(0xFFFFFFFF), col(0xFFC9C9D0))
                    gradOval(0f, -0.32f * u, 0.07f * u, 0.32f * u, col(0xFFFFC9D6), col(0xFFD98AA0))
                    fur(0f, -0.34f * u, 0.13f * u, 0.43f * u, 60, 0.05f * u, 0.006f * u, col(0xFFF0F0F4), 3 + sg, -90f, 40f)
                    c.restore()
                }
            }
            at(f.mx, f.my, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    val l = if (sg < 0) -0.065f * u else 0f
                    gradRRect(l, -0.01f * u, l + 0.065f * u, 0.11f * u, 0.02f * u, WHITE, col(0xFFD8D8DE))
                    e.rectF.set(l, -0.01f * u, l + 0.065f * u, 0.11f * u)
                    c.drawRoundRect(e.rectF, 0.02f * u, 0.02f * u, stroke(col(0xFF9A9AA2), 0.006f * u))
                }
            }
            gradOval(f.nx, f.ny, 0.055f * u, 0.04f * u, col(0xFFFFB0C2), col(0xFFC24A6E))
            at(f.nx, f.ny, f.rot) {
                for (sg in intArrayOf(-1, 1)) for (j in -1..1) line(sg * 0.1f * u, 0.05f * u + j * 0.03f * u, sg * 0.55f * u, 0.04f * u + j * 0.09f * u, 0.008f * u, WHITE, 0.9f)
            }
        }
    }
    fx("devil", "Little Devil", "😈") {
        tint(look(sat = 1.15f, r = 1.12f, g = 0.92f, b = 0.9f, tr = 10f), 0.55f * k)
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    val pa = e.path
                    pa.reset(); pa.moveTo(sg * 0.06f * u, -0.5f * u); pa.cubicTo(sg * 0.1f * u, -0.8f * u, sg * 0.3f * u, -0.9f * u, sg * 0.42f * u, -1.06f * u)
                    pa.cubicTo(sg * 0.42f * u, -0.82f * u, sg * 0.4f * u, -0.62f * u, sg * 0.36f * u, -0.5f * u); pa.close()
                    softOval(sg * 0.22f * u, -0.62f * u, 0.14f * u, 0.18f * u, BLACK, 0.3f, 0.05f * u)
                    gradPath(sg * 0.06f * u, -0.5f * u, sg * 0.42f * u, -1.06f * u, col(0xFFE23A2E), col(0xFF3A0508))
                    line(sg * 0.14f * u, -0.56f * u, sg * 0.3f * u, -0.9f * u, 0.014f * u, WHITE, 0.4f)
                }
            }
            for (i in 0..1) lightBall(if (i == 0) f.lx else f.rx, if (i == 0) f.ly else f.ry, 0.12f * u, col(0xFFFF3B1F), 0.35f)
        }
    }
    fx("angel", "Angel", "😇") {
        tint(look(bri = 0.04f, sat = 0.9f, r = 1.04f), 0.4f * k)
        for (f in faces) {
            val u = f.w
            val bob = sin(t * 3f) * 0.03f * u
            at(f.topX, f.topY - 0.12f * u + bob, f.rot) {
                val g = stroke(col(0xFFFFE680), 0.12f * u, 0.5f)
                g.maskFilter = android.graphics.BlurMaskFilter(0.07f * u + 1f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                e.rectF.set(-0.3f * u, -0.08f * u, 0.3f * u, 0.08f * u); c.drawOval(e.rectF, g); g.maskFilter = null
                ovalLine(0f, 0f, 0.3f * u, 0.08f * u, 0.05f * u, GOLD2)
                ovalLine(0f, -0.005f * u, 0.3f * u, 0.08f * u, 0.03f * u, GOLD1)
                ovalLine(0f, -0.012f * u, 0.3f * u, 0.07f * u, 0.008f * u, WHITE, 0.9f)
            }
            for (i in 0 until 4) {
                val a = (t * 1.2f + i * 0.25f) % 1f
                val x = f.hx((i - 1.5f) * 0.45f * u, -0.35f * f.h - a * 0.3f * u); val y = f.hy((i - 1.5f) * 0.45f * u, -0.35f * f.h - a * 0.3f * u)
                lightBall(x, y, 0.1f * u, WHITE, 0.6f * (1f - a)); sparkle(x, y, 0.06f * u * (1f - a), WHITE)
            }
        }
    }
    fx("mustache", "Mustache", "👨") {
        for (f in faces) mustacheFur(f, col(0xFF3A2418), 1f, 21)
    }
    fx("beard", "Full Beard", "🧔") {
        for (f in faces) { beard(f, col(0xFF3A2418), 1f, 260, 31); mustacheFur(f, col(0xFF3A2418), 1.05f, 41) }
    }
    fx("pirate", "Pirate", "🏴\u200D☠️") {
        for (f in faces) {
            val u = f.w; val d = f.eyeDist
            at(f.cx, f.cy, f.rot) {
                softOval(0f, -0.3f * u, 0.6f * u, 0.07f * u, BLACK, 0.35f, 0.05f * u)
                val pa = e.path
                pa.reset(); pa.moveTo(-0.58f * u, -0.3f * u); pa.quadTo(0f, -0.5f * u, 0.58f * u, -0.3f * u); pa.lineTo(0.55f * u, -0.62f * u); pa.quadTo(0f, -0.95f * u, -0.55f * u, -0.62f * u); pa.close()
                gradPath(-0.5f * u, -0.9f * u, 0.5f * u, -0.3f * u, col(0xFFE03A3A), col(0xFF7A0E14))
                for (i in 0 until 14) disc(-0.5f * u + rnd(i, 7) * 1.0f * u, -0.62f * u + (rnd(i, 8) - 0.5f) * 0.25f * u, 0.017f * u, WHITE, 0.9f)
                line(-0.45f * u, -0.5f * u, 0.45f * u, -0.46f * u, 0.012f * u, col(0x55000000))
                poly(col(0xFFB52328), 1f, 0.55f * u, -0.42f * u, 0.95f * u, -0.62f * u, 0.88f * u, -0.3f * u)
                line(0.58f * u, -0.4f * u, 0.9f * u, -0.45f * u, 0.01f * u, col(0x55000000))
            }
            line(f.lx, f.ly, f.hx(-0.6f * u, -0.3f * u), f.hy(-0.6f * u, -0.3f * u), 0.03f * u, col(0xFF141414))
            line(f.lx, f.ly, f.hx(0.5f * u, -0.45f * u), f.hy(0.5f * u, -0.45f * u), 0.03f * u, col(0xFF141414))
            at(f.lx, f.ly, f.rot) {
                softOval(0.02f * d, 0.06f * d, 0.44f * d, 0.38f * d, BLACK, 0.35f, 0.08f * d)
                gradOval(0f, 0f, 0.42f * d, 0.36f * d, col(0xFF4A4A50), col(0xFF050506))
                ovalLine(0f, 0f, 0.36f * d, 0.3f * d, 0.012f * d, col(0x55FFFFFF))
                oval(-0.14f * d, -0.12f * d, 0.1f * d, 0.05f * d, WHITE, 0.3f)
            }
            at(f.cx + (f.rx - f.lx) * 0.9f, f.cy + 0.2f * f.h, f.rot) { ring(0f, 0f, 0.05f * u, 0.014f * u, GOLD2) ; ring(0f, 0f, 0.05f * u, 0.007f * u, GOLD1) }
        }
    }
    fx("reindeer", "Reindeer", "🦌") {
        val wood = col(0xFF7A5230)
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    fun branch(x1: Float, y1: Float, x2: Float, y2: Float, wd: Float) {
                        line(x1 + sg * 0.012f * u, y1 + 0.012f * u, x2 + sg * 0.012f * u, y2 + 0.012f * u, wd * 1.15f, BLACK, 0.25f)
                        line(x1, y1, x2, y2, wd, shadeColor(wood, 0.7f))
                        line(x1, y1, x2, y2, wd * 0.6f, wood)
                        line(x1 - sg * wd * 0.15f, y1 - wd * 0.1f, x2 - sg * wd * 0.15f, y2 - wd * 0.1f, wd * 0.22f, shadeColor(wood, 1.5f), 0.8f)
                    }
                    branch(sg * 0.2f * u, -0.55f * u, sg * 0.5f * u, -1.04f * u, 0.07f * u)
                    branch(sg * 0.31f * u, -0.76f * u, sg * 0.66f * u, -0.74f * u, 0.05f * u)
                    branch(sg * 0.42f * u, -0.91f * u, sg * 0.72f * u, -1.02f * u, 0.045f * u)
                    branch(sg * 0.25f * u, -0.66f * u, sg * 0.1f * u, -0.92f * u, 0.045f * u)
                }
            }
            val pulse = 0.5f + 0.5f * sin(t * 5f)
            lightBall(f.nx, f.ny, 0.3f * u, col(0xFFFF2A1F), 0.45f * pulse)
            softOval(f.nx + 0.02f * u, f.ny + 0.1f * u, 0.12f * u, 0.04f * u, BLACK, 0.3f, 0.04f * u)
            gradOval(f.nx, f.ny, 0.12f * u, 0.12f * u, col(0xFFFF7A6B), col(0xFFA80010))
            oval(f.nx - 0.04f * u, f.ny - 0.05f * u, 0.035f * u, 0.022f * u, WHITE, 0.85f)
        }
    }
    fx("flowers", "Flower Crown", "🌸") {
        val sets = arrayOf(
            intArrayOf(col(0xFFFFD6E6), col(0xFFE0568F), col(0xFFE0A100)),
            intArrayOf(col(0xFFFFFFFF), col(0xFFD2D2D8), col(0xFFE8A200)),
            intArrayOf(col(0xFFFFE08A), col(0xFFE0A100), col(0xFF8A4B00)),
            intArrayOf(col(0xFFD9B8FF), col(0xFF7B4DD0), col(0xFFE8C400)),
        )
        for (f in faces) {
            val u = f.w
            for (i in 0..8) {
                val a = Math.toRadians(6.0 + i * 21.0).toFloat()
                val dx = -cos(a) * 0.57f * u; val dy = -sin(a) * 0.62f * f.h - 0.03f * u
                val x = f.hx(dx, dy); val y = f.hy(dx, dy)
                at(x, y, f.rot + (i - 4) * 18f) {
                    gradOval(-0.1f * u, 0.02f * u, 0.11f * u, 0.045f * u, col(0xFF6FD06F), col(0xFF1E6B2A))
                    gradOval(0.1f * u, 0.02f * u, 0.11f * u, 0.045f * u, col(0xFF6FD06F), col(0xFF1E6B2A))
                }
                val s = sets[i % sets.size]
                flower(x, y, (0.1f + 0.025f * (i % 2)) * u, s[0], s[1], s[2])
            }
        }
    }
    fx("rainbow", "Rainbow Mouth", "🌈") {
        val bands = intArrayOf(col(0xFFFF1744), col(0xFFFF9100), col(0xFFFFEA00), col(0xFF00E676), col(0xFF00B0FF), col(0xFF304FFE), col(0xFFAA00FF))
        for (f in faces) {
            val u = f.w
            val sway = sin(t * 5f) * 0.1f * u
            for (pass in 0..1) for (i in 0..6) {
                val ox = (i - 3) * 0.04f * u
                val pa = e.path
                pa.reset()
                pa.moveTo(f.mx + ox, f.my + 0.04f * u)
                pa.cubicTo(f.mx + ox + sway, f.my + (hf - f.my) * 0.35f, f.mx + ox - sway, f.my + (hf - f.my) * 0.7f, f.mx + ox + sway * 0.5f, hf + 0.1f * u)
                if (pass == 0) { val g = stroke(bands[i], 0.07f * u, 0.35f); g.maskFilter = android.graphics.BlurMaskFilter(0.04f * u + 1f, android.graphics.BlurMaskFilter.Blur.NORMAL); c.drawPath(pa, g); g.maskFilter = null }
                else { c.drawPath(pa, stroke(bands[i], 0.046f * u, 0.9f)); c.drawPath(pa, stroke(WHITE, 0.01f * u, 0.22f)) }
            }
            lightBall(f.mx, f.my + 0.05f * u, 0.18f * u, WHITE, 0.4f)
        }
    }
    fx("tears", "Happy Tears", "😂") {
        for (f in faces) {
            val u = f.w
            for (sg in intArrayOf(-1, 1)) {
                val ex = if (sg < 0) f.lx else f.rx; val ey = if (sg < 0) f.ly else f.ry
                val w = stroke(col(0xFFBFE9FF), 0.035f * u, 0.3f); w.maskFilter = android.graphics.BlurMaskFilter(0.015f * u + 1f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                c.drawLine(ex, ey + 0.05f * u, ex + sg * 0.1f * u, ey + 0.45f * f.h, w); w.maskFilter = null
                softOval(ex, ey + 0.06f * u, 0.12f * u, 0.04f * u, col(0xFFBFE9FF), 0.4f, 0.03f * u)
                for (i in 0 until 4) {
                    val p = (t * 1.1f + i * 0.25f) % 1f
                    drop(ex + sg * (0.04f + 0.1f * p) * u, ey + 0.1f * u + p * 0.6f * f.h, 0.04f * u * (1f - 0.4f * p), 1f - p * 0.5f)
                }
            }
        }
    }
    fx("fireeyes", "Fire Eyes", "🔥") {
        for (f in faces) {
            val u = f.w
            for (i in 0..1) {
                val ex = if (i == 0) f.lx else f.rx; val ey = if (i == 0) f.ly else f.ry
                val fl = 1f + 0.16f * sin(t * 22f + i * 2f) + 0.1f * sin(t * 13f)
                lightBall(ex, ey, 0.4f * u * fl, col(0xFFFF6A00), 0.7f)
                for (n in 0 until 3) {
                    val sx = (n - 1) * 0.07f * u; val h = (0.34f - abs(n - 1) * 0.1f) * u * fl * (1f + 0.15f * sin(t * 17f + n * 2f))
                    val pa = e.path
                    pa.reset(); pa.moveTo(ex + sx - 0.07f * u, ey + 0.04f * u)
                    pa.cubicTo(ex + sx - 0.09f * u, ey - h * 0.4f, ex + sx - 0.02f * u, ey - h * 0.7f, ex + sx + 0.01f * u, ey - h)
                    pa.cubicTo(ex + sx + 0.04f * u, ey - h * 0.6f, ex + sx + 0.1f * u, ey - h * 0.3f, ex + sx + 0.07f * u, ey + 0.04f * u); pa.close()
                    gradPath(ex, ey + 0.04f * u, ex, ey - h, col(0xFFFF3B00), col(0xFFFFC21F), 0.85f)
                    val pb = Path(); pb.addOval(ex + sx - 0.035f * u, ey - h * 0.55f, ex + sx + 0.035f * u, ey + 0.03f * u, Path.Direction.CW)
                    e.path.set(pb); gradPath(ex, ey + 0.03f * u, ex, ey - h * 0.5f, col(0xFFFFFFE0), col(0xFFFFD000), 0.9f)
                }
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
                val g = stroke(col(0xFFFF1744), 0.12f * u * pulse, 0.5f); g.maskFilter = android.graphics.BlurMaskFilter(0.06f * u + 1f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                c.drawLine(ex, ey, tx, ty, g); g.maskFilter = null
                line(ex, ey, tx, ty, 0.05f * u * pulse, col(0xFFFF5A5A))
                line(ex, ey, tx, ty, 0.018f * u, WHITE)
                lightBall(ex, ey, 0.25f * u * pulse, col(0xFFFF2A2A), 0.9f); lightBall(ex, ey, 0.09f * u, WHITE, 0.9f)
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
                val x = f.hx(dx, dy); val y = f.hy(dx, dy)
                lightBall(x, y, 0.16f * u * z, col(0xFFFFD21F), 0.6f)
                star(x, y, 0.1f * u * z, col(0xFFFFB300)); star(x, y, 0.07f * u * z, col(0xFFFFF176)); sparkle(x - 0.02f * u, y - 0.02f * u, 0.03f * u, WHITE, 0.8f)
            }
        }
    }
    fx("vampire", "Vampire", "🧛") {
        tint(look(sat = 0.5f, con = 1.2f, bri = -0.02f, r = 1.05f, g = 1.0f, b = 1.05f), 0.6f * k)
        for (f in faces) {
            val u = f.w
            softOval(f.mx, f.my, 0.2f * u, 0.05f * u, col(0xFF7A0A1A), 0.35f, 0.03f * u)
            at(f.mx, f.my, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    val pa = e.path
                    pa.reset(); pa.moveTo(sg * 0.125f * u, -0.025f * u); pa.lineTo(sg * 0.05f * u, -0.025f * u); pa.quadTo(sg * 0.08f * u, 0.05f * u, sg * 0.09f * u, 0.13f * u); pa.quadTo(sg * 0.115f * u, 0.05f * u, sg * 0.125f * u, -0.025f * u); pa.close()
                    softOval(sg * 0.09f * u, 0.06f * u, 0.03f * u, 0.07f * u, BLACK, 0.3f, 0.02f * u)
                    gradPath(0f, -0.03f * u, 0f, 0.13f * u, WHITE, col(0xFFCFCFC8))
                    c.drawPath(pa, stroke(col(0xFF9A9A92), 0.006f * u))
                }
                val drip = (t * 0.8f) % 1f
                drop(0.15f * u, 0.05f * u + drip * 0.3f * u, 0.022f * u, 1f - drip)
            }
            val fl = sin(t * 9f)
            bat(f.hx(-0.7f * u, -0.62f * f.h + fl * 0.03f * u), f.hy(-0.7f * u, -0.62f * f.h + fl * 0.03f * u), 0.3f * u, fl)
            bat(f.hx(0.7f * u, -0.7f * f.h - fl * 0.03f * u), f.hy(0.7f * u, -0.7f * f.h - fl * 0.03f * u), 0.26f * u, fl)
        }
    }
    fx("mask", "Hero Mask", "🦸") {
        for (f in faces) {
            val d = f.eyeDist
            at(f.ex, f.ey, f.rot) {
                softOval(0f, 0.06f * d, 1.2f * d, 0.45f * d, BLACK, 0.25f, 0.08f * d)
                val pa = Path()
                pa.moveTo(-1.28f * d, -0.45f * d); pa.quadTo(0f, -0.78f * d, 1.28f * d, -0.45f * d); pa.lineTo(1.05f * d, 0.1f * d)
                pa.quadTo(0.7f * d, 0.58f * d, 0.25f * d, 0.4f * d); pa.quadTo(0f, 0.25f * d, -0.25f * d, 0.4f * d)
                pa.quadTo(-0.7f * d, 0.58f * d, -1.05f * d, 0.1f * d); pa.close()
                val holes = Path()
                holes.addOval(-0.84f * d, -0.22f * d, -0.18f * d, 0.3f * d, Path.Direction.CW); holes.addOval(0.18f * d, -0.22f * d, 0.84f * d, 0.3f * d, Path.Direction.CW)
                pa.op(holes, Path.Op.DIFFERENCE)
                e.path.set(pa)
                gradPath(0f, -0.7f * d, 0f, 0.5f * d, col(0xFF3A63F0), col(0xFF0E1E7A))
                c.drawPath(pa, stroke(col(0xFF08104A), 0.03f * d))
                e.path.set(pa)
                line(-1.0f * d, -0.35f * d, -0.2f * d, -0.5f * d, 0.03f * d, WHITE, 0.2f)
                line(1.0f * d, -0.35f * d, 0.2f * d, -0.5f * d, 0.03f * d, WHITE, 0.2f)
            }
        }
    }
    fx("santa", "Santa", "🎅") {
        for (f in faces) {
            val u = f.w
            beard(f, col(0xFFF1F1F4), 1.1f, 280, 51)
            mustacheFur(f, col(0xFFF4F4F6), 1.1f, 61)
            for (sg in intArrayOf(-1, 1)) softOval(f.hx(sg * 0.3f * u, 0.1f * f.h), f.hy(sg * 0.3f * u, 0.1f * f.h), 0.1f * u, 0.07f * u, col(0xFFFF6B6B), 0.3f, 0.05f * u)
            at(f.topX, f.topY + 0.04f * u, f.rot - 8f) {
                softOval(0f, 0.1f * u, 0.5f * u, 0.07f * u, BLACK, 0.35f, 0.05f * u)
                val pa = e.path
                pa.reset(); pa.moveTo(-0.5f * u, 0.0f); pa.lineTo(0.5f * u, 0.0f); pa.quadTo(0.55f * u, -0.45f * u, 0.3f * u, -0.62f * u); pa.quadTo(0.68f * u, -0.55f * u, 0.62f * u, -0.22f * u); pa.quadTo(0.1f * u, -0.85f * u, -0.5f * u, -0.35f * u); pa.close()
                gradPath(-0.5f * u, -0.6f * u, 0.5f * u, 0f, col(0xFFE53030), col(0xFF7E0A12))
                fur(0f, -0.3f * u, 0.4f * u, 0.3f * u, 90, 0.04f * u, 0.006f * u, col(0xFFB0181F), 71, 60f, 120f)
                gradRRect(-0.55f * u, -0.07f * u, 0.55f * u, 0.11f * u, 0.08f * u, WHITE, col(0xFFD9D9E0))
                fur(0f, 0.02f * u, 0.55f * u, 0.08f * u, 90, 0.04f * u, 0.007f * u, col(0xFFF5F5F8), 73, 90f, 180f)
                gradOval(0.62f * u, -0.4f * u, 0.1f * u, 0.1f * u, WHITE, col(0xFFC9C9D2))
                fur(0.62f * u, -0.4f * u, 0.1f * u, 0.1f * u, 30, 0.04f * u, 0.006f * u, col(0xFFF5F5F8), 75, 0f, 360f)
            }
        }
    }
    fx("thirdeye", "Third Eye", "👁️") {
        for (f in faces) {
            val u = f.w
            val px = f.hx(0f, -0.27f * f.h); val py = f.hy(0f, -0.27f * f.h)
            val blink = if ((t % 3f) < 0.15f) 0.12f else 1f
            val lid = f.hx(0f, 0f).let { skinAt(f.cx, f.cy - 0.2f * f.h) }
            at(px, py, f.rot) {
                softOval(0f, 0f, 0.17f * u, 0.1f * u, shadeColor(lid, 0.4f), 0.55f, 0.03f * u)
                gradOval(0f, 0f, 0.13f * u, 0.075f * u * blink, WHITE, col(0xFFD5CFCB))
                if (blink > 0.5f) {
                    gradOval(0f, 0f, 0.058f * u, 0.058f * u, col(0xFF7BC0F0), col(0xFF1C4F86))
                    disc(0f, 0f, 0.028f * u, BLACK); disc(-0.016f * u, -0.016f * u, 0.011f * u, WHITE)
                    ring(0f, 0f, 0.055f * u, 0.008f * u, col(0xFF123A66), 0.8f)
                }
                ovalLine(0f, 0f, 0.13f * u, 0.075f * u * blink, 0.014f * u, shadeColor(lid, 0.45f))
                for (i in -3..3) line(i * 0.035f * u, -0.07f * u * blink, i * 0.045f * u, -0.1f * u * blink, 0.006f * u, col(0xFF2A1A12))
            }
        }
    }
    fx("tongue", "Tongue Out", "😜") {
        for (f in faces) {
            val u = f.w
            at(f.mx, f.my, f.rot + sin(t * 6f) * 6f) {
                softOval(0.015f * u, 0.17f * u, 0.1f * u, 0.15f * u, BLACK, 0.3f, 0.04f * u)
                gradRRect(-0.09f * u, -0.01f * u, 0.09f * u, 0.3f * u, 0.09f * u, col(0xFFFF8FA8), col(0xFFD03C68))
                line(0f, 0.02f * u, 0f, 0.2f * u, 0.014f * u, col(0xFFB02A55))
                oval(-0.035f * u, 0.1f * u, 0.016f * u, 0.05f * u, WHITE, 0.45f)
            }
        }
    }
    fx("afro", "Big Hair", "👩\u200D🦱") {
        val hair = col(0xFF2B1B12)
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                for (i in 0 until 46) {
                    val a = Math.toRadians(-25.0 + rnd(i, 1) * 230.0).toFloat()
                    val rr = (0.55f + 0.28f * rnd(i, 2)) * u
                    val x = -cos(a) * rr; val y = -sin(a) * rr * 1.05f - 0.1f * f.h
                    val r = (0.12f + 0.09f * rnd(i, 3)) * u
                    gradOval(x, y, r, r, shadeColor(hair, 1.6f), shadeColor(hair, 0.4f))
                }
                fur(0f, -0.5f * f.h, 0.75f * u, 0.55f * u, 160, 0.06f * u, 0.008f * u, shadeColor(hair, 1.8f), 81, -90f, 360f)
            }
        }
    }
    fx("viking", "Viking", "🛡️") {
        for (f in faces) {
            val u = f.w
            beard(f, col(0xFFC98E3C), 1.5f, 300, 91); mustacheFur(f, col(0xFFD79B45), 1.2f, 95)
            at(f.topX, f.topY + 0.05f * u, f.rot) {
                for (sg in intArrayOf(-1, 1)) {
                    val pa = e.path
                    pa.reset(); pa.moveTo(sg * 0.38f * u, -0.1f * u); pa.cubicTo(sg * 0.62f * u, -0.05f * u, sg * 0.7f * u, -0.3f * u, sg * 0.62f * u, -0.55f * u)
                    pa.cubicTo(sg * 0.6f * u, -0.35f * u, sg * 0.5f * u, -0.22f * u, sg * 0.34f * u, -0.22f * u); pa.close()
                    softOval(sg * 0.5f * u, -0.25f * u, 0.14f * u, 0.22f * u, BLACK, 0.3f, 0.05f * u)
                    gradPath(sg * 0.35f * u, -0.1f * u, sg * 0.65f * u, -0.55f * u, col(0xFFF5EBD0), col(0xFF9A8A66))
                    line(sg * 0.5f * u, -0.12f * u, sg * 0.6f * u, -0.4f * u, 0.012f * u, WHITE, 0.35f)
                }
                softOval(0f, 0.08f * u, 0.42f * u, 0.06f * u, BLACK, 0.4f, 0.05f * u)
                val dome = Path(); dome.addArc(-0.4f * u, -0.55f * u, 0.4f * u, 0.3f * u, 180f, 180f); dome.close()
                e.path.set(dome)
                gradPath(-0.4f * u, -0.5f * u, 0.4f * u, 0f, col(0xFFD2D8E0), col(0xFF3F464F))
                gradRRect(-0.1f * u, -0.5f * u, 0.12f * u, -0.05f * u, 0.05f * u, col(0x66FFFFFF), col(0x00FFFFFF), false)
                gradRRect(-0.42f * u, -0.09f * u, 0.42f * u, 0.07f * u, 0.02f * u, col(0xFF6E757F), col(0xFF30353C))
                for (i in -3..3) gradOval(i * 0.12f * u, -0.01f * u, 0.017f * u, 0.017f * u, WHITE, col(0xFF555B63))
            }
        }
    }
    fx("cowboy", "Cowboy", "🤠") {
        val felt = col(0xFF8A5A2B)
        for (f in faces) {
            val u = f.w
            at(f.topX, f.topY + 0.05f * u, f.rot - 4f) {
                softOval(0f, 0.12f * u, 0.7f * u, 0.09f * u, BLACK, 0.4f, 0.07f * u)
                val brim = Path()
                brim.moveTo(-0.85f * u, -0.12f * u); brim.cubicTo(-0.6f * u, 0.14f * u, 0.6f * u, 0.14f * u, 0.85f * u, -0.12f * u)
                brim.cubicTo(0.7f * u, 0.0f, 0.5f * u, -0.06f * u, 0.0f, -0.06f * u); brim.cubicTo(-0.5f * u, -0.06f * u, -0.7f * u, 0.0f, -0.85f * u, -0.12f * u); brim.close()
                e.path.set(brim)
                gradPath(0f, -0.1f * u, 0f, 0.14f * u, shadeColor(felt, 1.2f), shadeColor(felt, 0.55f))
                val crown = Path()
                crown.moveTo(-0.34f * u, -0.04f * u); crown.cubicTo(-0.4f * u, -0.35f * u, -0.3f * u, -0.55f * u, -0.12f * u, -0.5f * u)
                crown.quadTo(0f, -0.4f * u, 0.12f * u, -0.5f * u); crown.cubicTo(0.3f * u, -0.55f * u, 0.4f * u, -0.35f * u, 0.34f * u, -0.04f * u); crown.close()
                e.path.set(crown)
                gradPath(-0.35f * u, 0f, 0.35f * u, 0f, shadeColor(felt, 1.25f), shadeColor(felt, 0.6f))
                gradRRect(-0.35f * u, -0.17f * u, 0.35f * u, -0.05f * u, 0.01f * u, col(0xFF2B1A0E), col(0xFF120A05))
                gradRRect(-0.04f * u, -0.18f * u, 0.05f * u, -0.04f * u, 0.01f * u, col(0xFFE8D28A), col(0xFF9A7B1E))
                line(-0.2f * u, -0.3f * u, -0.18f * u, -0.46f * u, 0.016f * u, WHITE, 0.25f)
            }
        }
    }
    fx("wizard", "Wizard", "🧙") {
        for (f in faces) {
            val u = f.w
            beard(f, col(0xFFEDEDF2), 1.9f, 320, 101); mustacheFur(f, col(0xFFF2F2F6), 1.2f, 105)
            at(f.topX, f.topY + 0.05f * u, f.rot - 6f) {
                softOval(0f, 0.1f * u, 0.55f * u, 0.07f * u, BLACK, 0.4f, 0.06f * u)
                val cone = Path()
                cone.moveTo(-0.42f * u, 0.02f * u); cone.cubicTo(-0.3f * u, -0.5f * u, -0.1f * u, -0.9f * u, 0.18f * u, -1.3f * u)
                cone.cubicTo(0.2f * u, -1.1f * u, 0.4f * u, -0.9f * u, 0.5f * u, -0.55f * u); cone.cubicTo(0.46f * u, -0.3f * u, 0.4f * u, -0.1f * u, 0.42f * u, 0.02f * u); cone.close()
                e.path.set(cone)
                gradPath(-0.4f * u, 0f, 0.45f * u, -0.6f * u, col(0xFF4A3CC0), col(0xFF1A1052))
                val sp = floatArrayOf(-0.1f, -0.3f, 0.12f, -0.6f, -0.05f, -0.85f, 0.2f, -0.42f)
                var i = 0
                while (i + 1 < sp.size) { star(sp[i] * u, sp[i + 1] * u, 0.05f * u * (0.7f + 0.3f * sin(t * 4f + i)), col(0xFFFFE066)); i += 2 }
                gradRRect(-0.45f * u, -0.1f * u, 0.45f * u, 0.0f, 0.01f * u, col(0xFFFFD35A), col(0xFF9A6A00))
                gradOval(0f, 0.03f * u, 0.62f * u, 0.07f * u, col(0xFF5A4AD0), col(0xFF150C48))
            }
        }
    }
    fx("chef", "Chef", "👨\u200D🍳") {
        for (f in faces) {
            val u = f.w
            mustacheFur(f, col(0xFF2A1A12), 1f, 111)
            at(f.topX, f.topY + 0.05f * u, f.rot) {
                softOval(0f, 0.08f * u, 0.38f * u, 0.06f * u, BLACK, 0.35f, 0.05f * u)
                val xs = floatArrayOf(-0.28f, 0f, 0.28f, -0.15f, 0.15f)
                val ys = floatArrayOf(-0.5f, -0.62f, -0.5f, -0.74f, -0.74f)
                for (i in xs.indices) gradOval(xs[i] * u, ys[i] * u, 0.2f * u, 0.2f * u, WHITE, col(0xFFC9C9D2))
                gradRRect(-0.34f * u, -0.46f * u, 0.34f * u, 0.04f * u, 0.02f * u, WHITE, col(0xFFD6D6DE), false)
                for (i in -2..2) line(i * 0.12f * u, -0.44f * u, i * 0.12f * u, 0.0f, 0.008f * u, col(0x33000000))
                gradRRect(-0.35f * u, -0.06f * u, 0.35f * u, 0.05f * u, 0.015f * u, col(0xFFEDEDF2), col(0xFFB8B8C4))
            }
        }
    }
    fx("tiger", "Tiger Paint", "🐯") {
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                softOval(0f, -0.2f * f.h, 0.4f * u, 0.18f * f.h, col(0xFFFF8A00), 0.45f, 0.08f * u)
                softOval(-0.3f * u, 0.12f * f.h, 0.2f * u, 0.2f * f.h, col(0xFFFF8A00), 0.45f, 0.08f * u); softOval(0.3f * u, 0.12f * f.h, 0.2f * u, 0.2f * f.h, col(0xFFFF8A00), 0.45f, 0.08f * u)
                softOval(0f, 0.28f * f.h, 0.2f * u, 0.12f * f.h, WHITE, 0.45f, 0.06f * u)
                for (sg in intArrayOf(-1, 1)) for (i in 0 until 4) {
                    val y = (-0.32f + i * 0.14f) * f.h
                    val pa = e.path
                    pa.reset(); pa.moveTo(sg * 0.5f * u, y); pa.quadTo(sg * 0.36f * u, y - 0.03f * u, sg * (0.22f - i * 0.02f) * u, y + 0.02f * u); pa.quadTo(sg * 0.36f * u, y + 0.04f * u, sg * 0.5f * u, y + 0.06f * u); pa.close()
                    val p = fill(col(0xFF1A0E08), 0.85f); p.maskFilter = android.graphics.BlurMaskFilter(0.006f * u + 1f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                    c.drawPath(pa, p); p.maskFilter = null
                }
                for (i in 0 until 3) { val p = fill(col(0xFF1A0E08), 0.8f); p.maskFilter = android.graphics.BlurMaskFilter(0.006f * u + 1f, android.graphics.BlurMaskFilter.Blur.NORMAL); c.drawLine((i - 1) * 0.07f * u, -0.46f * f.h, (i - 1) * 0.05f * u, -0.3f * f.h, stroke(col(0xFF1A0E08), 0.03f * u, 0.8f)); p.maskFilter = null }
            }
            at(f.nx, f.ny, f.rot) {
                val pa = e.path
                pa.reset(); pa.moveTo(-0.07f * u, -0.03f * u); pa.lineTo(0.07f * u, -0.03f * u); pa.lineTo(0f, 0.05f * u); pa.close()
                gradPath(0f, -0.04f * u, 0f, 0.05f * u, col(0xFFFF9FB0), col(0xFFC24A5E))
                for (sg in intArrayOf(-1, 1)) for (j in -1..1) {
                    line(sg * 0.14f * u, 0.07f * u + j * 0.05f * u, sg * 0.65f * u, 0.06f * u + j * 0.1f * u, 0.012f * u, WHITE, 0.9f)
                }
            }
        }
    }
    fx("freckles", "Freckles & Blush", "🌞") {
        for (f in faces) {
            val u = f.w
            for (sg in intArrayOf(-1, 1)) softOval(f.hx(sg * 0.3f * u, 0.14f * f.h), f.hy(sg * 0.3f * u, 0.14f * f.h), 0.14f * u, 0.09f * u, col(0xFFFF5C7A), 0.38f, 0.07f * u)
            val sk = skinAt(f.cx, f.cy)
            val dot = shadeColor(sk, 0.55f)
            at(f.cx, f.cy, f.rot) {
                for (i in 0 until 70) {
                    val sg = if (rnd(i, 3) < 0.5f) -1f else 1f
                    val x = sg * (0.04f + 0.3f * rnd(i, 1)) * u
                    val y = (0.0f + 0.2f * rnd(i, 2)) * f.h - 0.02f * u * (0.4f - abs(x) / u)
                    disc(x, y, (0.006f + 0.01f * rnd(i, 4)) * u, dot, 0.45f + 0.35f * rnd(i, 5))
                }
            }
        }
    }
    fx("butterflies", "Butterflies", "🦋") {
        val cols = arrayOf(intArrayOf(col(0xFFFFB347), col(0xFFC24A00)), intArrayOf(col(0xFF6EC6FF), col(0xFF1F4FD0)), intArrayOf(col(0xFFFF8AD0), col(0xFF9A1E8A)), intArrayOf(col(0xFFB6F36E), col(0xFF1E8A3A)))
        for (f in faces) {
            val u = f.w
            for (i in 0 until 4) {
                val a = t * (0.9f + i * 0.17f) + i * 1.57f
                val x = f.cx + cos(a) * (0.75f + 0.1f * i) * u; val y = f.cy - 0.35f * f.h + sin(a * 1.3f) * 0.45f * u
                val flap = 0.25f + 0.75f * abs(cos(t * 14f + i))
                val cc = cols[i]
                val s = 0.2f * u
                at(x, y, sin(a) * 18f) {
                    softOval(0.03f * u, 0.05f * u, 0.18f * u, 0.12f * u, BLACK, 0.22f, 0.04f * u)
                    for (sg in intArrayOf(-1, 1)) {
                        c.save(); c.scale(flap * sg, 1f)
                        gradOval(0.55f * s, -0.4f * s, 0.6f * s, 0.75f * s, cc[0], cc[1])
                        gradOval(0.45f * s, 0.55f * s, 0.45f * s, 0.55f * s, shadeColor(cc[0], 0.95f), shadeColor(cc[1], 0.8f))
                        disc(0.6f * s, -0.45f * s, 0.14f * s, WHITE, 0.55f)
                        c.restore()
                    }
                    oval(0f, 0f, 0.07f * s, 0.55f * s, col(0xFF1A1218))
                    line(0f, -0.5f * s, -0.25f * s, -0.95f * s, 0.03f * s, col(0xFF1A1218)); line(0f, -0.5f * s, 0.25f * s, -0.95f * s, 0.03f * s, col(0xFF1A1218))
                }
            }
        }
    }
    fx("headphones", "DJ Headphones", "🎧") {
        for (f in faces) {
            val u = f.w
            at(f.cx, f.cy, f.rot) {
                e.rectF.set(-0.64f * u, -0.78f * f.h, 0.64f * u, 0.45f * f.h)
                val band = stroke(col(0xFF1C1C20), 0.1f * u); c.drawArc(e.rectF, 190f, 160f, false, band)
                c.drawArc(e.rectF, 195f, 100f, false, stroke(col(0x55FFFFFF), 0.02f * u))
                c.drawArc(e.rectF, 190f, 160f, false, stroke(col(0xFFE53A5E), 0.02f * u, 0.9f))
                for (sg in intArrayOf(-1, 1)) {
                    val x = sg * 0.64f * u
                    softOval(x + sg * 0.03f * u, 0.12f * u, 0.17f * u, 0.27f * u, BLACK, 0.3f, 0.05f * u)
                    gradRRect(x - 0.15f * u, -0.17f * u, x + 0.15f * u, 0.17f * u + 0.12f * u, 0.1f * u, col(0xFF3A3A42), col(0xFF0E0E12))
                    gradRRect(x - 0.1f * u, -0.1f * u, x + 0.1f * u, 0.14f * u, 0.08f * u, col(0xFFE53A5E), col(0xFF8A1030))
                    line(x - 0.08f * u, -0.06f * u, x - 0.02f * u, -0.1f * u, 0.015f * u, WHITE, 0.5f)
                }
            }
        }
    }
}
