package com.example.filtery

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private fun MutableList<Fx>.color(id: String, name: String, icon: String, draw: FxCtx.() -> Unit) { add(Fx(id, name, Cat.COLOR, icon, false, draw)) }
private fun MutableList<Fx>.art(id: String, name: String, icon: String, draw: FxCtx.() -> Unit) { add(Fx(id, name, Cat.ART, icon, false, draw)) }
private fun MutableList<Fx>.frame(id: String, name: String, icon: String, face: Boolean = false, draw: FxCtx.() -> Unit) { add(Fx(id, name, Cat.FRAMES, icon, face, draw)) }

internal fun MutableList<Fx>.addColorFilters() {
    color("bw", "Black & White", "⚫") { tint(look(sat = 0f, con = 1.05f)) }
    color("noir", "Noir", "🎩") { tint(look(sat = 0f, con = 1.5f, bri = -0.04f)); vignette(0.55f) }
    color("sepia", "Sepia", "📜") {
        tint(ColorMatrix(floatArrayOf(0.393f, 0.769f, 0.189f, 0f, 0f, 0.349f, 0.686f, 0.168f, 0f, 0f, 0.272f, 0.534f, 0.131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
    }
    color("vintage", "Vintage", "📻") { tint(look(sat = 0.75f, con = 0.9f, bri = 0.03f, r = 1.08f, b = 0.85f, tr = 10f, tb = -5f)); vignette(0.35f) }
    color("retro70", "Retro 70s", "🕺") { tint(look(sat = 1.2f, con = 0.95f, r = 1.15f, g = 1.0f, b = 0.8f, tr = 10f, tb = -10f)); glow(0.25f * k) }
    color("polaroid", "Instant", "📷") { tint(look(sat = 1.1f, con = 1.1f, r = 1.05f, b = 0.95f, tr = 8f, tb = 12f)) }
    color("warm", "Warm", "☀️") { tint(look(r = 1.12f, g = 1.02f, b = 0.88f)) }
    color("cool", "Cool", "❄️") { tint(look(r = 0.9f, g = 1.0f, b = 1.15f)) }
    color("golden", "Golden Hour", "🌇") { tint(look(sat = 1.15f, con = 1.05f, r = 1.2f, g = 1.05f, b = 0.8f)); glow(0.3f * k) }
    color("tealorange", "Teal & Orange", "🎬") { tint(look(sat = 1.2f, con = 1.15f, r = 1.1f, g = 0.98f, b = 1.05f, tr = -8f, tg = 4f, tb = 10f)) }
    color("cinema", "Cinematic", "🎞️") { tint(look(sat = 0.9f, con = 1.2f, bri = -0.03f, r = 1.05f, b = 1.1f)); vignette(0.35f) }
    color("vivid", "Vivid", "🌈") { tint(look(sat = 1.6f, con = 1.12f)) }
    color("fade", "Faded", "🌫️") { tint(look(sat = 0.7f, con = 0.8f, bri = 0.08f)) }
    color("cross", "Cross Process", "🧪") { tint(look(sat = 1.3f, con = 1.25f, r = 1.2f, g = 1.0f, b = 0.8f, tb = 20f)) }
    color("moody", "Moody", "🌒") { tint(look(sat = 0.75f, con = 1.25f, bri = -0.07f, r = 0.95f, b = 1.05f)); vignette(0.5f) }
    color("cyber", "Cyber Neon", "🟣") { tint(look(sat = 1.4f, con = 1.2f, r = 1.1f, g = 0.85f, b = 1.25f)); glow(0.25f * k) }
}

internal fun MutableList<Fx>.addArtFilters() {
    art("sketch", "Pencil Sketch", "✏️") {
        pix(768) { dst, sp, ww, wh ->
            val n = ww * wh
            val g = e.ints(n, 2); val m = e.ints(n, 3)
            Px.gray(sp, g, n); Px.sobel(g, ww, wh, m, 3)
            for (i in 0 until n) dst[i] = Px.gray(255 - m[i])
        }
    }
    art("pencil", "Color Pencil", "🖍️") {
        pix(768) { dst, sp, ww, wh ->
            val n = ww * wh
            val g = e.ints(n, 2); val m = e.ints(n, 3)
            Px.gray(sp, g, n); Px.sobel(g, ww, wh, m, 3)
            for (i in 0 until n) {
                val p = sp[i]
                val inv = 255 - m[i]
                val r = (((p shr 16) and 255) * 140 + 255 * 115) shr 8
                val gg = (((p shr 8) and 255) * 140 + 255 * 115) shr 8
                val b = ((p and 255) * 140 + 255 * 115) shr 8
                dst[i] = Px.rgb(r * inv / 255, gg * inv / 255, b * inv / 255)
            }
        }
    }
    art("ink", "Ink", "🖋️") {
        pix(768) { dst, sp, ww, wh ->
            val n = ww * wh
            val g = e.ints(n, 2); val m = e.ints(n, 3)
            Px.gray(sp, g, n); Px.sobel(g, ww, wh, m, 4)
            for (i in 0 until n) dst[i] = if (g[i] < 105 || m[i] > 90) 0xFF101010.toInt() else 0xFFF6F2E8.toInt()
        }
    }
    art("blueprint", "Blueprint", "📐") {
        pix(640) { dst, sp, ww, wh ->
            val n = ww * wh
            val g = e.ints(n, 2); val m = e.ints(n, 3)
            Px.gray(sp, g, n); Px.sobel(g, ww, wh, m, 5)
            val bg = 0xFF1B3F8C.toInt()
            for (i in 0 until n) dst[i] = Px.mix(bg, 0xFFFFFFFF.toInt(), m[i] * 256 / 255)
        }
    }
    art("neon", "Neon Edges", "💡") {
        pix(512) { dst, sp, ww, wh ->
            val n = ww * wh
            val g = e.ints(n, 2); val m = e.ints(n, 3)
            Px.gray(sp, g, n); Px.sobel(g, ww, wh, m, 6)
            val tbl = IntArray(64) { Color.HSVToColor(floatArrayOf(((it / 64f + t * 0.2f) % 1f) * 360f, 0.85f, 1f)) }
            for (y in 0 until wh) for (x in 0 until ww) {
                val i = y * ww + x
                val cc = tbl[x * 64 / ww]
                val mm = m[i]
                val base = sp[i]
                dst[i] = Px.rgb((((cc shr 16) and 255) * mm shr 8) + (((base shr 16) and 255) shr 3),
                    (((cc shr 8) and 255) * mm shr 8) + (((base shr 8) and 255) shr 3),
                    ((cc and 255) * mm shr 8) + ((base and 255) shr 3))
            }
        }
    }
    art("oil", "Oil Paint", "🖼️") {
        pix(320) { dst, sp, ww, wh ->
            val cnt = IntArray(8); val sr = IntArray(8); val sg = IntArray(8); val sb = IntArray(8)
            for (y in 0 until wh) for (x in 0 until ww) {
                java.util.Arrays.fill(cnt, 0); java.util.Arrays.fill(sr, 0); java.util.Arrays.fill(sg, 0); java.util.Arrays.fill(sb, 0)
                for (dy in -2..2) {
                    val yy = (y + dy).coerceIn(0, wh - 1)
                    for (dx in -2..2) {
                        val p = sp[yy * ww + (x + dx).coerceIn(0, ww - 1)]
                        val bin = Px.lum(p) shr 5
                        cnt[bin]++; sr[bin] += (p shr 16) and 255; sg[bin] += (p shr 8) and 255; sb[bin] += p and 255
                    }
                }
                var best = 0
                for (b in 1 until 8) if (cnt[b] > cnt[best]) best = b
                val cn = max(1, cnt[best])
                dst[y * ww + x] = Px.rgb(sr[best] / cn, sg[best] / cn, sb[best] / cn)
            }
        }
    }
    art("watercolor", "Watercolor", "💦") {
        pix(256) { dst, sp, ww, wh ->
            val n = ww * wh
            val g = e.ints(n, 2); val m = e.ints(n, 3)
            Px.gray(sp, g, n); Px.sobel(g, ww, wh, m, 2)
            for (i in 0 until n) {
                val p = sp[i]
                val lift = 24
                val r = level8((p shr 16) and 255) + lift
                val gg = level8((p shr 8) and 255) + lift
                val b = level8(p and 255) + lift
                val dark = 255 - m[i] / 4
                dst[i] = Px.rgb(r * dark / 255, gg * dark / 255, b * dark / 255)
            }
        }
    }
    art("popart", "Pop Art", "🟡") {
        pix(512) { dst, sp, ww, wh ->
            val n = ww * wh
            val g = e.ints(n, 2); val m = e.ints(n, 3)
            Px.gray(sp, g, n); Px.sobel(g, ww, wh, m, 5)
            for (i in 0 until n) {
                val p = sp[i]
                val r = if (((p shr 16) and 255) > 128) 255 else 20
                val gg = if (((p shr 8) and 255) > 128) 255 else 20
                val b = if ((p and 255) > 128) 255 else 20
                dst[i] = if (m[i] > 100) 0xFF101010.toInt() else Px.rgb(r, gg, b)
            }
        }
    }
    art("halftone", "Halftone", "🔘") {
        pix(640) { dst, sp, ww, wh -> Px.halftone(sp, dst, ww, wh, max(4, ww / 70), false) }
    }
    art("emboss", "Emboss", "🪙") {
        pix(768) { dst, sp, ww, wh ->
            val n = ww * wh
            val g = e.ints(n, 2)
            Px.gray(sp, g, n)
            for (y in 0 until wh) {
                val y0 = max(0, y - 1) * ww; val y1 = min(wh - 1, y + 1) * ww
                for (x in 0 until ww) {
                    val v = 128 + (g[y1 + min(ww - 1, x + 1)] - g[y0 + max(0, x - 1)]) * 2
                    dst[y * ww + x] = Px.gray(Px.clamp(v))
                }
            }
        }
    }
    art("dream", "Dream Glow", "✨") {
        tint(look(sat = 1.15f, con = 0.92f, bri = 0.04f), 0.6f * k)
        glow(k)
    }
    art("duotone", "Duotone", "🟪") {
        val pal = e.lut("duo") { Px.palette(0xFF2B0A4A.toInt(), 0xFFD81B60.toInt(), 0xFFFFB74D.toInt()) }
        pix(768) { dst, sp, ww, wh -> for (i in 0 until ww * wh) dst[i] = pal[Px.lum(sp[i])] }
    }
}

private fun level8(v: Int): Int = ((v * 7 + 127) / 255) * 255 / 7

internal fun MutableList<Fx>.addFrameFilters() {
    frame("polaroidf", "Polaroid", "🖼️") {
        val side = 0.05f * wf; val bottom = 0.17f * hf
        val cream = 0xFFFBF7EE.toInt()
        rect(0f, 0f, wf, side, cream); rect(0f, 0f, side, hf, cream); rect(wf - side, 0f, wf, hf, cream); rect(0f, hf - bottom, wf, hf, cream)
        rect(side, side, wf - side, side + 0.004f * hf, 0x22000000, 1f)
    }
    frame("filmstrip", "Film Strip", "🎞️") {
        val band = 0.09f * wf
        rect(0f, 0f, band, hf, 0xFF111111.toInt()); rect(wf - band, 0f, wf, hf, 0xFF111111.toInt())
        val n = 14
        val sh = band * 0.5f; val gap = hf / n
        for (i in 0 until n) {
            val y = gap * i + gap / 2f
            rrect(band * 0.25f, y - sh * 0.5f, band * 0.75f, y + sh * 0.5f, band * 0.12f, 0xFFF2F2F2.toInt())
            rrect(wf - band * 0.75f, y - sh * 0.5f, wf - band * 0.25f, y + sh * 0.5f, band * 0.12f, 0xFFF2F2F2.toInt())
        }
    }
    frame("softframe", "Soft Frame", "⬜") {
        val m = 0.045f * s
        val pa = Path()
        pa.addRect(0f, 0f, wf, hf, Path.Direction.CW)
        pa.addRoundRect(RectF(m, m, wf - m, hf - m), 0.07f * s, 0.07f * s, Path.Direction.CW)
        pa.fillType = Path.FillType.EVEN_ODD
        c.drawPath(pa, fill(0xFFFFFFFF.toInt()))
    }
    frame("neonframe", "Neon Frame", "🟩") {
        val hsv = floatArrayOf((t * 80f) % 360f, 0.8f, 1f)
        val colr = Color.HSVToColor(hsv)
        val m = 0.04f * s
        e.rectF.set(m, m, wf - m, hf - m)
        val p = stroke(colr, 0.03f * s, 0.5f)
        p.maskFilter = android.graphics.BlurMaskFilter(0.03f * s, android.graphics.BlurMaskFilter.Blur.NORMAL)
        c.drawRoundRect(e.rectF, 0.06f * s, 0.06f * s, p)
        p.maskFilter = null
        c.drawRoundRect(e.rectF, 0.06f * s, 0.06f * s, stroke(colr, 0.012f * s))
        c.drawRoundRect(e.rectF, 0.06f * s, 0.06f * s, stroke(0xFFFFFFFF.toInt(), 0.004f * s, 0.9f))
    }
    frame("heartframe", "Heart Frame", "💗") {
        val hearts = arrayOf("❤️", "💖", "💗", "💕")
        val sz = 0.075f * s
        val nx = 8; val ny = 11
        for (i in 0..nx) {
            val x = wf * i / nx
            val bump = 1f + 0.12f * sin(t * 4f + i)
            emoji(hearts[i % 4], x, sz * 0.5f, sz * bump); emoji(hearts[(i + 1) % 4], x, hf - sz * 0.5f, sz * bump)
        }
        for (j in 1 until ny) {
            val y = hf * j / ny
            val bump = 1f + 0.12f * sin(t * 4f + j)
            emoji(hearts[j % 4], sz * 0.5f, y, sz * bump); emoji(hearts[(j + 2) % 4], wf - sz * 0.5f, y, sz * bump)
        }
    }
    frame("datestamp", "Date Stamp", "📅") {
        val d = java.text.SimpleDateFormat("''yy  MM  dd", java.util.Locale.US).format(java.util.Date())
        tint(look(sat = 1.1f, con = 1.05f, r = 1.05f, b = 0.92f), 0.8f * k)
        text(d, wf - 0.05f * s, hf - 0.06f * s, 0.065f * s, 0xFFFF9A1F.toInt(), true, 0x66000000, Paint.Align.RIGHT)
    }
    frame("cinemabars", "Cinema Bars", "🎥") {
        val bar = 0.12f * hf
        tint(look(sat = 0.95f, con = 1.12f, r = 1.04f, b = 1.08f), 0.8f * k)
        rect(0f, 0f, wf, bar, 0xFF000000.toInt()); rect(0f, hf - bar, wf, hf, 0xFF000000.toInt())
    }
    frame("lol", "LOL Banner", "😆") {
        val y = 0.17f * hf
        c.save(); c.rotate(-6f, wf / 2f, y)
        text("LOL", wf / 2f, y, 0.22f * s, 0xFFFFD600.toInt(), true, 0xFF111111.toInt())
        c.restore()
        emoji("😂", wf * 0.14f, hf * 0.3f, 0.14f * s); emoji("🤣", wf * 0.86f, hf * 0.3f, 0.14f * s)
    }
    frame("snow", "Snowfall", "❄️") {
        val n = (70 * k).toInt() + 10
        for (i in 0 until n) {
            val sp = 0.04f + 0.09f * rnd(i, 3)
            val x = rnd(i, 1) * wf + sin(t * 0.8f + i) * 0.02f * wf
            val y = ((rnd(i, 2) + t * sp) % 1f) * hf
            disc(x, y, (0.004f + 0.011f * rnd(i, 4)) * s, 0xFFFFFFFF.toInt(), 0.9f)
        }
    }
    frame("confetti", "Confetti", "🎊") {
        val cols = intArrayOf(0xFFFF1744.toInt(), 0xFFFFEA00.toInt(), 0xFF00E676.toInt(), 0xFF00B0FF.toInt(), 0xFFD500F9.toInt(), 0xFFFF9100.toInt())
        val n = (60 * k).toInt() + 10
        for (i in 0 until n) {
            val sp = 0.08f + 0.12f * rnd(i, 3)
            val x = rnd(i, 1) * wf + sin(t * 2f + i) * 0.015f * wf
            val y = ((rnd(i, 2) + t * sp) % 1f) * hf
            c.save(); c.rotate(t * 180f * (rnd(i, 5) - 0.5f) + i * 37f, x, y)
            val sz = (0.01f + 0.012f * rnd(i, 6)) * s
            rect(x - sz, y - sz * 0.5f, x + sz, y + sz * 0.5f, cols[i % cols.size])
            c.restore()
        }
    }
    frame("sparkles", "Sparkles", "✨") {
        val n = (28 * k).toInt() + 6
        for (i in 0 until n) {
            val x = rnd(i, 1) * wf; val y = rnd(i, 2) * hf
            val tw = 0.5f + 0.5f * sin(t * (2f + 3f * rnd(i, 3)) + i * 1.7f)
            sparkle(x, y, (0.012f + 0.03f * rnd(i, 4)) * s * tw, 0xFFFFFFFF.toInt(), 0.95f)
        }
    }
    frame("heartrain", "Hearts Rising", "💞") {
        val hs = arrayOf("❤️", "💖", "💗", "💕", "💘")
        val n = (22 * k).toInt() + 4
        for (i in 0 until n) {
            val sp = 0.06f + 0.1f * rnd(i, 3)
            val x = rnd(i, 1) * wf + sin(t * 1.5f + i) * 0.03f * wf
            val p = (rnd(i, 2) + t * sp) % 1f
            val y = hf * (1.05f - p * 1.1f)
            emoji(hs[i % hs.size], x, y, (0.05f + 0.07f * rnd(i, 4)) * s, 1f - p * 0.5f)
        }
    }
    frame("bubbles", "Bubbles", "🔵") {
        val n = (24 * k).toInt() + 4
        for (i in 0 until n) {
            val sp = 0.05f + 0.08f * rnd(i, 3)
            val x = rnd(i, 1) * wf + sin(t * 1.2f + i) * 0.03f * wf
            val p = (rnd(i, 2) + t * sp) % 1f
            val y = hf * (1.05f - p * 1.1f)
            val r = (0.02f + 0.05f * rnd(i, 4)) * s
            disc(x, y, r, 0xFFBFE9FF.toInt(), 0.18f)
            ring(x, y, r, 0.004f * s, 0xFFFFFFFF.toInt(), 0.8f)
            disc(x - r * 0.35f, y - r * 0.35f, r * 0.18f, 0xFFFFFFFF.toInt(), 0.9f)
        }
    }
}
