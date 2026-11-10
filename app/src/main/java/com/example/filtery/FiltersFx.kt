package com.example.filtery

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private fun MutableList<Fx>.fx(id: String, name: String, icon: String, face: Boolean = false, draw: FxCtx.() -> Unit) {
    add(Fx(id, name, Cat.FX, icon, face, draw))
}

private fun level(v: Int, n: Int): Int = ((v * (n - 1) + 127) / 255) * 255 / (n - 1)

private fun poster(p: Int, n: Int): Int =
    Px.rgb(level((p shr 16) and 255, n), level((p shr 8) and 255, n), level(p and 255, n))

internal fun MutableList<Fx>.addFxFilters() {
    fx("cartoon", "Cartoon", "🖍️") {
        pix(384) { dst, sp, ww, wh ->
            val n = ww * wh
            val g = e.ints(n, 2); val m = e.ints(n, 3)
            Px.gray(sp, g, n); Px.sobel(g, ww, wh, m, 4)
            for (i in 0 until n) dst[i] = if (m[i] > 70) 0xFF101010.toInt() else poster(sp[i], 6)
        }
    }
    fx("comic", "Comic Book", "💥") {
        pix(512) { dst, sp, ww, wh ->
            val n = ww * wh
            Px.halftone(sp, dst, ww, wh, max(4, ww / 60), true)
            val g = e.ints(n, 2); val m = e.ints(n, 3)
            Px.gray(sp, g, n); Px.sobel(g, ww, wh, m, 5)
            for (i in 0 until n) if (m[i] > 110) dst[i] = 0xFF101010.toInt()
        }
    }
    fx("glitch", "Glitch", "📺") {
        pix(512) { dst, sp, ww, wh ->
            val seed = (t * 9f).toInt()
            val nb = 16
            val bh = max(1, wh / nb)
            val split = (ww * 0.012f).toInt() + 1
            for (y in 0 until wh) {
                val band = y / bh
                val shift = if (Px.hash(band, seed) > 0.72f) ((Px.hash(band, seed + 99) - 0.5f) * 0.2f * ww).toInt() else 0
                val flash = Px.hash(y / 3, seed + 7) > 0.985f
                val row = y * ww
                for (x in 0 until ww) {
                    val sx = x + shift
                    val xr = (sx + split).coerceIn(0, ww - 1); val xg = sx.coerceIn(0, ww - 1); val xb = (sx - split).coerceIn(0, ww - 1)
                    val r = (sp[row + xr] shr 16) and 255
                    val g = (sp[row + xg] shr 8) and 255
                    val b = sp[row + xb] and 255
                    dst[row + x] = if (flash) Px.rgb(r + 90, g + 90, b + 90) else Px.rgb(r, g, b)
                }
            }
        }
    }
    fx("vhs", "Old VHS", "📼") {
        pix(480) { dst, sp, ww, wh ->
            val frame = (t * 20f).toInt()
            val barY = ((t * 0.3f) % 1f * wh).toInt()
            val barH = max(2, wh / 28)
            for (y in 0 until wh) {
                val inBar = y >= barY && y < barY + barH
                val off = if (inBar) (ww * 0.04f).toInt() else 0
                val dim = if (y % 3 == 0) 0.78f else 1f
                val row = y * ww
                for (x in 0 until ww) {
                    val xs = (x + off).coerceIn(0, ww - 1)
                    val r = (sp[row + (xs - 3).coerceIn(0, ww - 1)] shr 16) and 255
                    val g = (sp[row + xs] shr 8) and 255
                    val b = sp[row + (xs + 3).coerceIn(0, ww - 1)] and 255
                    val nz = ((Px.hash(x + frame * 31, y) - 0.5f) * 36f).toInt()
                    val lift = if (inBar) 40 else 0
                    dst[row + x] = Px.rgb(((r * dim).toInt() + nz + lift), ((g * dim).toInt() + nz + lift), ((b * dim).toInt() + nz + lift))
                }
            }
        }
    }
    fx("pixelate", "Pixelate", "🧱") {
        pix(768) { dst, sp, ww, wh -> Px.pixelate(sp, dst, ww, wh, max(6, ww / 26), 0, 0, ww, wh) }
    }
    fx("facepixel", "Censored", "🙈", true) {
        val fs = facesOrDefault
        pix(768) { dst, sp, ww, wh ->
            System.arraycopy(sp, 0, dst, 0, ww * wh)
            val sc = ww.toFloat() / w
            for (f in fs) {
                val x0 = ((f.cx - f.w * 0.62f) * sc).toInt().coerceIn(0, ww - 1)
                val x1 = ((f.cx + f.w * 0.62f) * sc).toInt().coerceIn(x0 + 1, ww)
                val y0 = ((f.cy - f.h * 0.62f) * sc).toInt().coerceIn(0, wh - 1)
                val y1 = ((f.cy + f.h * 0.62f) * sc).toInt().coerceIn(y0 + 1, wh)
                Px.pixelate(sp, dst, ww, wh, max(4, ((x1 - x0) / 7)), x0, y0, x1, y1)
            }
        }
    }
    fx("thermal", "Heat Vision", "🌡️") {
        val pal = e.lut("thermal") { Px.palette(0xFF000040.toInt(), 0xFF4B00A0.toInt(), 0xFFC0105A.toInt(), 0xFFFF5A00.toInt(), 0xFFFFD000.toInt(), 0xFFFFFFFF.toInt()) }
        pix(512) { dst, sp, ww, wh -> for (i in 0 until ww * wh) dst[i] = pal[Px.lum(sp[i])] }
    }
    fx("night", "Night Vision", "🌙") {
        val pal = e.lut("night") { Px.palette(0xFF001400.toInt(), 0xFF1E8C1E.toInt(), 0xFF8CFF8C.toInt()) }
        val frame = (t * 25f).toInt()
        pix(512) { dst, sp, ww, wh ->
            for (y in 0 until wh) {
                val dim = if (y % 4 == 0) 0.85f else 1f
                for (x in 0 until ww) {
                    val l = (Px.lum(sp[y * ww + x]) * 1.5f * dim).toInt() + ((Px.hash(x + frame * 17, y) - 0.5f) * 40f).toInt()
                    dst[y * ww + x] = pal[Px.clamp(l)]
                }
            }
        }
        vignette(0.7f)
    }
    fx("xray", "X-Ray", "☠️") {
        val pal = e.lut("xray") { Px.palette(0xFFEAF6FF.toInt(), 0xFF5AA0D8.toInt(), 0xFF0A1A40.toInt()) }
        pix(512) { dst, sp, ww, wh -> for (i in 0 until ww * wh) dst[i] = pal[Px.lum(sp[i])] }
    }
    fx("kaleido", "Kaleidoscope", "🔮") {
        pix(512) { dst, sp, ww, wh ->
            val cx = ww / 2f; val cy = wh / 2f
            val ang = t * 0.35f
            val cs = cos(ang); val sn = sin(ang)
            for (y in 0 until wh) {
                val dy = y - cy
                for (x in 0 until ww) {
                    val dx = x - cx
                    var ax = abs(dx * cs - dy * sn); var ay = abs(dx * sn + dy * cs)
                    if (ay > ax) { val tmp = ax; ax = ay; ay = tmp }
                    val sx = (cx + ax * 0.75f).toInt().coerceIn(0, ww - 1)
                    val sy = (cy + ay * 0.75f).toInt().coerceIn(0, wh - 1)
                    dst[y * ww + x] = sp[sy * ww + sx]
                }
            }
        }
    }
    fx("mirrorl", "Mirror Left", "🪞") {
        c.save(); c.clipRect(wf / 2f, 0f, wf, hf); c.translate(wf, 0f); c.scale(-1f, 1f); c.drawBitmap(src, 0f, 0f, paintA(k)); c.restore()
    }
    fx("mirrorr", "Mirror Right", "🪞") {
        c.save(); c.clipRect(0f, 0f, wf / 2f, hf); c.translate(wf, 0f); c.scale(-1f, 1f); c.drawBitmap(src, 0f, 0f, paintA(k)); c.restore()
    }
    fx("mirrort", "Mirror Top", "🪞") {
        c.save(); c.clipRect(0f, hf / 2f, wf, hf); c.translate(0f, hf); c.scale(1f, -1f); c.drawBitmap(src, 0f, 0f, paintA(k)); c.restore()
    }
    fx("mirror4", "Mirror x4", "🔲") {
        val p = paintA(k)
        c.save(); c.clipRect(wf / 2f, 0f, wf, hf / 2f); c.translate(wf, 0f); c.scale(-1f, 1f); c.drawBitmap(src, 0f, 0f, p); c.restore()
        c.save(); c.clipRect(0f, hf / 2f, wf / 2f, hf); c.translate(0f, hf); c.scale(1f, -1f); c.drawBitmap(src, 0f, 0f, p); c.restore()
        c.save(); c.clipRect(wf / 2f, hf / 2f, wf, hf); c.translate(wf, hf); c.scale(-1f, -1f); c.drawBitmap(src, 0f, 0f, p); c.restore()
    }
    fx("ghost", "Ghost Twins", "👻") {
        for (i in 1..2) {
            val dx = sin(t * 2f + i * 2f) * 0.035f * wf * i
            val dy = cos(t * 1.7f + i) * 0.012f * hf * i
            c.drawBitmap(src, dx, dy, paintA(0.33f * k))
            c.drawBitmap(src, -dx, -dy, paintA(0.33f * k))
        }
    }
    fx("zoomburst", "Zoom Burst", "💨", true) {
        val fx0 = if (faces.isNotEmpty()) faces[0].cx else wf / 2f
        val fy0 = if (faces.isNotEmpty()) faces[0].cy else hf / 2f
        for (i in 1..8) {
            val sc = 1f + i * 0.025f * k
            c.save(); c.scale(sc, sc, fx0, fy0); c.drawBitmap(src, 0f, 0f, paintA(0.16f)); c.restore()
        }
    }
    fx("rainbowcycle", "Rainbow Cycle", "🌈") {
        tint(hueMatrix((t * 90f) % 360f), 1f)
    }
    fx("disco", "Disco", "🪩") {
        rect(0f, 0f, wf, hf, 0x66000000, 1f)
        val p = e.fp
        for (i in 0 until 5) {
            val a = t * (0.9f + i * 0.2f) + i * 1.3f
            val bx = wf / 2f + cos(a) * wf * 0.4f
            val by = hf / 2f + sin(a * 1.3f) * hf * 0.38f
            val hsv = floatArrayOf(((t * 60f + i * 72f) % 360f), 0.9f, 1f)
            val colr = Color.HSVToColor((255 * k).roundToInt().coerceIn(0, 255), hsv)
            p.reset(); p.isAntiAlias = true
            p.shader = RadialGradient(bx, by, s * 0.55f, colr, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            p.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            c.drawCircle(bx, by, s * 0.55f, p)
        }
        p.shader = null; p.xfermode = null
    }
    fx("negative", "Negative", "🔁") {
        val inv = ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f))
        tint(inv)
    }
    fx("drunk", "Drunk", "🥴") {
        warp { x, y, d ->
            d[0] += sin(y / hf * 5f + t * 2f) * 0.018f * wf * k
            d[1] += cos(x / wf * 4f + t * 1.6f) * 0.012f * hf * k
        }
        val dx = sin(t * 1.8f) * 0.05f * wf * k
        c.drawBitmap(src, dx, 0f, paintA(0.35f))
        c.drawBitmap(src, -dx, 0f, paintA(0.25f))
    }

    fx("oldage", "Old Age", "🧓", true) {
        val fs = facesOrDefault
        pix(768) { dst, sp, ww, wh ->
            val n = ww * wh
            val g = e.ints(n, 2); val m = e.ints(n, 3)
            Px.gray(sp, g, n); Px.sobel(g, ww, wh, m, 7)
            System.arraycopy(sp, 0, dst, 0, n)
            val sc = ww.toFloat() / w
            for (f in fs) {
                val cx = f.cx * sc; val cy = f.cy * sc; val rx = f.w * 0.62f * sc; val ry = f.h * 0.68f * sc
                val x0 = (cx - rx).toInt().coerceIn(0, ww - 1); val x1 = (cx + rx).toInt().coerceIn(0, ww - 1)
                val y0 = (cy - ry).toInt().coerceIn(0, wh - 1); val y1 = (cy + ry).toInt().coerceIn(0, wh - 1)
                for (y in y0..y1) for (x in x0..x1) {
                    val dx = (x - cx) / rx; val dy = (y - cy) / ry
                    val q = dx * dx + dy * dy
                    if (q >= 1f) continue
                    val wgt = ((1f - q) * 3f).coerceAtMost(1f)
                    val i = y * ww + x
                    val p = sp[i]
                    val l = Px.lum(p)
                    val dark = (m[i] * 0.6f).toInt()
                    val nr = (((p shr 16) and 255) * 0.55f + l * 0.45f).toInt() - dark
                    val ng = (((p shr 8) and 255) * 0.55f + l * 0.45f).toInt() - dark
                    val nb = ((p and 255) * 0.55f + l * 0.45f).toInt() - dark
                    dst[i] = Px.mix(p, Px.rgb(nr, ng, nb), (wgt * 256).toInt())
                }
            }
        }
        for (f in fs) {
            val ed = f.eyeDist
            softOval(f.lx, f.ly + 0.2f * ed, 0.38f * ed, 0.12f * ed, 0xFF5A3A4A.toInt(), 0.4f, 0.1f * ed)
            softOval(f.rx, f.ry + 0.2f * ed, 0.38f * ed, 0.12f * ed, 0xFF5A3A4A.toInt(), 0.4f, 0.1f * ed)
            at(f.cx, f.cy, f.rot) {
                fur(0f, -0.52f * f.h, 0.58f * f.w, 0.16f * f.h, 150, 0.1f * f.w, 0.009f * f.w, 0xFFE9E9EE.toInt(), 7, -80f, 160f)
                fur(-0.5f * f.w, -0.22f * f.h, 0.08f * f.w, 0.2f * f.h, 50, 0.08f * f.w, 0.009f * f.w, 0xFFDADAE0.toInt(), 8, 100f, 60f)
                fur(0.5f * f.w, -0.22f * f.h, 0.08f * f.w, 0.2f * f.h, 50, 0.08f * f.w, 0.009f * f.w, 0xFFDADAE0.toInt(), 9, 80f, 60f)
            }
        }
    }
    fx("zombie", "Zombie", "🧟", true) {
        val fs = facesOrDefault
        pix(768) { dst, sp, ww, wh ->
            val n = ww * wh
            val g = e.ints(n, 2); val m = e.ints(n, 3)
            Px.gray(sp, g, n); Px.sobel(g, ww, wh, m, 6)
            System.arraycopy(sp, 0, dst, 0, n)
            val sc = ww.toFloat() / w
            for (f in fs) {
                val cx = f.cx * sc; val cy = f.cy * sc; val rx = f.w * 0.62f * sc; val ry = f.h * 0.68f * sc
                val x0 = (cx - rx).toInt().coerceIn(0, ww - 1); val x1 = (cx + rx).toInt().coerceIn(0, ww - 1)
                val y0 = (cy - ry).toInt().coerceIn(0, wh - 1); val y1 = (cy + ry).toInt().coerceIn(0, wh - 1)
                for (y in y0..y1) for (x in x0..x1) {
                    val dx = (x - cx) / rx; val dy = (y - cy) / ry
                    val q = dx * dx + dy * dy
                    if (q >= 1f) continue
                    val wgt = ((1f - q) * 3f).coerceAtMost(1f)
                    val i = y * ww + x
                    val p = sp[i]
                    val l = Px.lum(p)
                    val dark = (m[i] * 0.5f).toInt()
                    val nr = (l * 0.78f).toInt() + 6 - dark
                    val ng = (l * 0.95f).toInt() + 14 - dark
                    val nb = (l * 0.68f).toInt() - dark
                    dst[i] = Px.mix(p, Px.rgb(nr, ng, nb), (wgt * 256).toInt())
                }
            }
        }
        for (f in fs) {
            val ed = f.eyeDist
            for (i in 0..1) {
                val ex = if (i == 0) f.lx else f.rx; val ey = if (i == 0) f.ly else f.ry
                softOval(ex, ey, 0.4f * ed, 0.3f * ed, 0xFF0F1A0F.toInt(), 0.7f, 0.1f * ed)
                gradOval(ex, ey, 0.22f * ed, 0.15f * ed, 0xFFF1F2C0.toInt(), 0xFF8A8F50.toInt())
                disc(ex, ey, 0.05f * ed, 0xFF111111.toInt())
            }
            softOval(f.mx, f.my, 0.22f * f.w, 0.05f * f.w, 0xFF2A0A0A.toInt(), 0.5f, 0.03f * f.w)
            softOval(f.mx + 0.12f * f.w, f.my + 0.08f * f.w, 0.03f * f.w, 0.12f * f.w, 0xFF8A0A14.toInt(), 0.6f, 0.02f * f.w)
        }
    }
}
