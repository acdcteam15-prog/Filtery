package com.example.filtery

import kotlin.math.abs

/** Small pixel-by-pixel tools on packed ARGB int arrays. Pure Kotlin, no Android calls. */
object Px {
    fun clamp(v: Int): Int = if (v < 0) 0 else if (v > 255) 255 else v

    fun lum(p: Int): Int = (((p shr 16) and 255) * 77 + ((p shr 8) and 255) * 151 + (p and 255) * 28) shr 8

    fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (clamp(r) shl 16) or (clamp(g) shl 8) or clamp(b)

    fun gray(v: Int): Int = rgb(v, v, v)

    /** [t] from 0 to 256: 0 gives [a], 256 gives [b]. */
    fun mix(a: Int, b: Int, t: Int): Int {
        val u = 256 - t
        val r = (((a shr 16) and 255) * u + ((b shr 16) and 255) * t) shr 8
        val g = (((a shr 8) and 255) * u + ((b shr 8) and 255) * t) shr 8
        val bl = ((a and 255) * u + (b and 255) * t) shr 8
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }

    /** A 256-entry colour ramp through equally spaced [stops]. */
    fun palette(vararg stops: Int): IntArray {
        val out = IntArray(256)
        val seg = stops.size - 1
        for (i in 0 until 256) {
            val pos = i * seg / 255f
            val s = pos.toInt().coerceAtMost(seg - 1)
            val t = ((pos - s) * 256f).toInt().coerceIn(0, 256)
            out[i] = mix(stops[s], stops[s + 1], t)
        }
        return out
    }

    fun gray(src: IntArray, g: IntArray, n: Int) {
        for (i in 0 until n) g[i] = lum(src[i])
    }

    /** Sobel edge strength of the grey picture [g] into [m] (0..255). A bigger [gain] draws more lines. */
    fun sobel(g: IntArray, w: Int, h: Int, m: IntArray, gain: Int) {
        for (y in 0 until h) {
            val y0 = (if (y > 0) y - 1 else 0) * w
            val yc = y * w
            val y1 = (if (y < h - 1) y + 1 else h - 1) * w
            for (x in 0 until w) {
                val x0 = if (x > 0) x - 1 else 0
                val x1 = if (x < w - 1) x + 1 else w - 1
                val a = g[y0 + x0]; val b = g[y0 + x]; val c = g[y0 + x1]
                val d = g[yc + x0]; val f = g[yc + x1]
                val gg = g[y1 + x0]; val hh = g[y1 + x]; val ii = g[y1 + x1]
                val gx = (c + 2 * f + ii) - (a + 2 * d + gg)
                val gy = (gg + 2 * hh + ii) - (a + 2 * b + c)
                val v = (abs(gx) + abs(gy)) * gain / 8
                m[yc + x] = if (v > 255) 255 else v
            }
        }
    }

    /** Cheap deterministic noise 0..1 from two numbers. */
    fun hash(a: Int, b: Int): Float {
        var x = a * 374761393 + b * 668265263
        x = (x xor (x ushr 13)) * 1274126177
        x = x xor (x ushr 16)
        return (x and 0xFFFFFF) / 16777216f
    }

    /** Average colour of the block [x0,x1) x [y0,y1) sampled on a coarse grid. */
    fun blockAvg(src: IntArray, w: Int, x0: Int, y0: Int, x1: Int, y1: Int): Int {
        var r = 0; var g = 0; var b = 0; var n = 0
        val sx = maxOf(1, (x1 - x0) / 4)
        val sy = maxOf(1, (y1 - y0) / 4)
        var y = y0
        while (y < y1) {
            var x = x0
            while (x < x1) {
                val p = src[y * w + x]
                r += (p shr 16) and 255; g += (p shr 8) and 255; b += p and 255; n++
                x += sx
            }
            y += sy
        }
        if (n == 0) return 0xFF000000.toInt()
        return rgb(r / n, g / n, b / n)
    }

    /** Mosaic of [bs] x [bs] blocks inside the rectangle (all of it when the rectangle is the whole picture). */
    fun pixelate(src: IntArray, dst: IntArray, w: Int, h: Int, bs: Int, rx0: Int, ry0: Int, rx1: Int, ry1: Int) {
        var by = ry0
        while (by < ry1) {
            val ey = minOf(by + bs, ry1)
            var bx = rx0
            while (bx < rx1) {
                val ex = minOf(bx + bs, rx1)
                val c = blockAvg(src, w, bx, by, ex, ey)
                for (y in by until ey) for (x in bx until ex) dst[y * w + x] = c
                bx += bs
            }
            by += bs
        }
    }

    /** Dots whose size follows the brightness; [color] keeps each cell's own colour, otherwise black on white. */
    fun halftone(src: IntArray, dst: IntArray, w: Int, h: Int, cs: Int, color: Boolean) {
        var by = 0
        while (by < h) {
            val ey = minOf(by + cs, h)
            var bx = 0
            while (bx < w) {
                val ex = minOf(bx + cs, w)
                val avg = blockAvg(src, w, bx, by, ex, ey)
                val l = lum(avg)
                val rad = cs * 0.78f * Math.sqrt((1.0 - l / 255.0)).toFloat() + 0.3f
                val cxm = (bx + ex) / 2f; val cym = (by + ey) / 2f
                val ink = if (color) avg else 0xFF101010.toInt()
                for (y in by until ey) for (x in bx until ex) {
                    val dx = x + 0.5f - cxm; val dy = y + 0.5f - cym
                    dst[y * w + x] = if (dx * dx + dy * dy <= rad * rad) ink else 0xFFFFFFFF.toInt()
                }
                bx += cs
            }
            by += cs
        }
    }
}
