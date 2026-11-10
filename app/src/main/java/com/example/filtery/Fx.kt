package com.example.filtery

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class Cat(val title: String, val icon: String) {
    FACES("Funny faces", "🤡"),
    WARPS("Funny warps", "🤪"),
    FX("Fun effects", "👾"),
    COLOR("Colors", "🎞️"),
    ART("Art", "🎨"),
    FRAMES("Frames", "🖼️"),
}

/** One filter. [draw] paints it onto the picture; [face] says it needs the face finder running. */
class Fx(val id: String, val name: String, val cat: Cat, val icon: String, val face: Boolean, val draw: FxCtx.() -> Unit)

/**
 * Applies filters to pictures with plain Android graphics (no internet, no GPU code), the same way for the live
 * preview, the photo and the video frames. Keep one engine per thread: it reuses its working memory.
 */
class FxEngine {
    internal val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    internal val fp = Paint(Paint.ANTI_ALIAS_FLAG)
    internal val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    internal val path = Path()
    internal val rectF = RectF()
    internal val d2 = FloatArray(2)
    private val ia = arrayOfNulls<IntArray>(4)
    private var workBmp: Bitmap? = null
    private var verts = FloatArray(0)
    private val tmpBmps = HashMap<Int, Bitmap>()
    private val luts = HashMap<String, IntArray>()

    fun ints(n: Int, slot: Int): IntArray {
        var a = ia[slot]
        if (a == null || a.size < n) { a = IntArray(n); ia[slot] = a }
        return a
    }

    fun lut(key: String, make: () -> IntArray): IntArray = luts.getOrPut(key, make)

    fun workBitmap(w: Int, h: Int): Bitmap {
        val b = workBmp
        if (b != null && b.width == w && b.height == h) return b
        b?.recycle()
        val n = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        workBmp = n
        return n
    }

    fun smallBitmap(slot: Int, w: Int, h: Int): Bitmap {
        val key = slot * 100000 + w * 7 + h
        val old = tmpBmps[slot]
        if (old != null && old.width == w && old.height == h) return old
        old?.recycle()
        val n = Bitmap.createBitmap(max(1, w), max(1, h), Bitmap.Config.ARGB_8888)
        tmpBmps[slot] = n
        return n
    }

    fun vertices(n: Int): FloatArray {
        if (verts.size < n) verts = FloatArray(n)
        return verts
    }

    /** Draws [src] with filter [fx] at strength [k] (0..1) into [out]. Both must be the same size; [out] must be mutable. */
    fun render(src: Bitmap, out: Bitmap, fx: Fx, k: Float, faces: List<Face>, t: Float, caption: String?) {
        val c = Canvas(out)
        c.drawBitmap(src, 0f, 0f, null)
        val W = out.width.toFloat(); val H = out.height.toFloat()
        val pf = ArrayList<PFace>(faces.size)
        for (f in faces) pf.add(PFace(f, W, H))
        val kk = if (fx.cat == Cat.FACES) 1f else k.coerceIn(0f, 1f)   // props are always solid; strength is for warps, colours and effects
        val ctx = FxCtx(this, src, out, c, pf, kk, t)
        fx.draw(ctx)
        if (!caption.isNullOrBlank()) drawCaption(c, out.width, out.height, caption)
    }

    private fun drawCaption(c: Canvas, w: Int, h: Int, text: String) {
        val p = tp
        p.reset(); p.isAntiAlias = true
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textAlign = Paint.Align.CENTER
        var size = w * 0.085f
        p.textSize = size
        val maxW = w * 0.92f
        val tw = p.measureText(text)
        if (tw > maxW) { size *= maxW / tw; p.textSize = size }
        val x = w / 2f; val y = h - h * 0.045f
        p.style = Paint.Style.STROKE; p.strokeWidth = size * 0.16f; p.strokeJoin = Paint.Join.ROUND; p.color = Color.BLACK
        c.drawText(text, x, y, p)
        p.style = Paint.Style.FILL; p.color = Color.WHITE
        c.drawText(text, x, y, p)
    }
}

/** Everything a filter can use while it draws one picture. */
class FxCtx(
    val e: FxEngine, val src: Bitmap, val out: Bitmap, val c: Canvas,
    val faces: List<PFace>, val k: Float, val t: Float,
) {
    val w = src.width
    val h = src.height
    val wf = w.toFloat()
    val hf = h.toFloat()
    /** The shorter side: sizes are given as fractions of it so a filter looks the same on a small preview and a big photo. */
    val s = min(wf, hf)
    val hasFace get() = faces.isNotEmpty()

    /** Faces found, or one imaginary face in the middle so that warps always show something. */
    val facesOrDefault: List<PFace>
        get() = if (faces.isNotEmpty()) faces else listOf(PFace(Face.fromBox(-1, 0.5f, 0.42f, 0.5f, 0.38f, 0f, wf / hf), wf, hf))

    // ---------------------------------------------------------------- paints
    fun paintA(a: Float): Paint {
        val p = e.paint
        p.colorFilter = null; p.xfermode = null; p.shader = null; p.maskFilter = null
        p.alpha = (a * 255f).roundToInt().coerceIn(0, 255)
        return p
    }

    fun fill(color: Int, a: Float = 1f): Paint {
        val p = e.fp
        p.style = Paint.Style.FILL; p.shader = null; p.maskFilter = null; p.xfermode = null; p.colorFilter = null
        p.color = color
        p.alpha = (Color.alpha(color) * a * k).roundToInt().coerceIn(0, 255)
        return p
    }

    fun stroke(color: Int, width: Float, a: Float = 1f): Paint {
        val p = fill(color, a)
        p.style = Paint.Style.STROKE; p.strokeWidth = width; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
        return p
    }

    // ---------------------------------------------------------------- shapes (all positions in picture pixels)
    inline fun at(x: Float, y: Float, rot: Float, body: () -> Unit) {
        c.save(); c.translate(x, y); c.rotate(rot); body(); c.restore()
    }

    fun disc(x: Float, y: Float, r: Float, color: Int, a: Float = 1f) = c.drawCircle(x, y, r, fill(color, a))
    fun ring(x: Float, y: Float, r: Float, width: Float, color: Int, a: Float = 1f) = c.drawCircle(x, y, r, stroke(color, width, a))
    fun oval(x: Float, y: Float, rx: Float, ry: Float, color: Int, a: Float = 1f) {
        e.rectF.set(x - rx, y - ry, x + rx, y + ry); c.drawOval(e.rectF, fill(color, a))
    }
    fun ovalLine(x: Float, y: Float, rx: Float, ry: Float, width: Float, color: Int, a: Float = 1f) {
        e.rectF.set(x - rx, y - ry, x + rx, y + ry); c.drawOval(e.rectF, stroke(color, width, a))
    }
    fun rrect(l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int, a: Float = 1f) {
        e.rectF.set(l, t, r, b); c.drawRoundRect(e.rectF, rad, rad, fill(color, a))
    }
    fun rect(l: Float, t: Float, r: Float, b: Float, color: Int, a: Float = 1f) = c.drawRect(l, t, r, b, fill(color, a))
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int, a: Float = 1f) =
        c.drawLine(x1, y1, x2, y2, stroke(color, width, a))

    /** Closed polygon from x,y pairs. */
    fun poly(color: Int, a: Float, vararg p: Float) {
        val pa = e.path
        pa.reset(); pa.moveTo(p[0], p[1])
        var i = 2
        while (i + 1 < p.size) { pa.lineTo(p[i], p[i + 1]); i += 2 }
        pa.close()
        c.drawPath(pa, fill(color, a))
    }

    fun star(x: Float, y: Float, r: Float, color: Int, points: Int = 5, a: Float = 1f) {
        val pa = e.path
        pa.reset()
        val n = points * 2
        for (i in 0 until n) {
            val rr = if (i % 2 == 0) r else r * 0.42f
            val ang = -Math.PI / 2 + i * Math.PI / points
            val px = x + (rr * cos(ang)).toFloat(); val py = y + (rr * sin(ang)).toFloat()
            if (i == 0) pa.moveTo(px, py) else pa.lineTo(px, py)
        }
        pa.close()
        c.drawPath(pa, fill(color, a))
    }

    fun sparkle(x: Float, y: Float, r: Float, color: Int, a: Float = 1f) {
        val pa = e.path
        pa.reset()
        pa.moveTo(x, y - r); pa.quadTo(x, y, x + r, y); pa.quadTo(x, y, x, y + r); pa.quadTo(x, y, x - r, y); pa.quadTo(x, y, x, y - r)
        pa.close()
        c.drawPath(pa, fill(color, a))
    }

    fun text(str: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = true, outline: Int = 0, align: Paint.Align = Paint.Align.CENTER) {
        val p = e.tp
        p.reset(); p.isAntiAlias = true
        p.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        p.textSize = size; p.textAlign = align
        if (outline != 0) {
            p.style = Paint.Style.STROKE; p.strokeWidth = size * 0.14f; p.strokeJoin = Paint.Join.ROUND
            p.color = outline; p.alpha = (255 * k).roundToInt()
            c.drawText(str, x, y, p)
        }
        p.style = Paint.Style.FILL; p.color = color; p.alpha = (Color.alpha(color) * k).roundToInt().coerceIn(0, 255)
        c.drawText(str, x, y, p)
    }

    fun emoji(str: String, x: Float, y: Float, size: Float, a: Float = 1f) {
        val p = e.tp
        p.reset(); p.isAntiAlias = true
        p.textSize = size; p.textAlign = Paint.Align.CENTER
        p.alpha = (255 * k * a).roundToInt().coerceIn(0, 255)
        c.drawText(str, x, y + size * 0.36f, p)
    }

    fun rnd(i: Int, salt: Int): Float = Px.hash(i, salt)

    // ---------------------------------------------------------------- whole-picture colour work
    fun tint(m: ColorMatrix, a: Float = k) {
        val p = paintA(a)
        p.colorFilter = ColorMatrixColorFilter(m)
        c.drawBitmap(src, 0f, 0f, p)
        p.colorFilter = null
    }

    fun look(sat: Float = 1f, con: Float = 1f, bri: Float = 0f, r: Float = 1f, g: Float = 1f, b: Float = 1f,
             tr: Float = 0f, tg: Float = 0f, tb: Float = 0f): ColorMatrix {
        val m = ColorMatrix()
        m.setSaturation(sat)
        val off = 255f * (0.5f * (1f - con) + bri)
        val t = ColorMatrix(floatArrayOf(
            con * r, 0f, 0f, 0f, off + tr,
            0f, con * g, 0f, 0f, off + tg,
            0f, 0f, con * b, 0f, off + tb,
            0f, 0f, 0f, 1f, 0f,
        ))
        m.postConcat(t)
        return m
    }

    fun hueMatrix(deg: Float): ColorMatrix {
        val a = Math.toRadians(deg.toDouble())
        val cs = cos(a).toFloat(); val sn = sin(a).toFloat()
        val lr = 0.213f; val lg = 0.715f; val lb = 0.072f
        return ColorMatrix(floatArrayOf(
            lr + cs * (1 - lr) + sn * (-lr), lg + cs * (-lg) + sn * (-lg), lb + cs * (-lb) + sn * (1 - lb), 0f, 0f,
            lr + cs * (-lr) + sn * 0.143f, lg + cs * (1 - lg) + sn * 0.140f, lb + cs * (-lb) + sn * (-0.283f), 0f, 0f,
            lr + cs * (-lr) + sn * (-(1 - lr)), lg + cs * (-lg) + sn * lg, lb + cs * (1 - lb) + sn * lb, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ))
    }

    fun vignette(strength: Float, a: Float = k) {
        val rad = hypot(wf, hf) / 2f
        val p = e.fp
        p.style = Paint.Style.FILL; p.xfermode = null; p.colorFilter = null; p.maskFilter = null
        p.color = Color.BLACK; p.alpha = 255
        p.shader = RadialGradient(wf / 2f, hf / 2f, rad, intArrayOf(Color.TRANSPARENT, Color.argb((255 * strength * a).roundToInt().coerceIn(0, 255), 0, 0, 0)), floatArrayOf(0.45f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, wf, hf, p)
        p.shader = null
    }

    /** Soft glow: a blurred copy of the picture added on top with the "screen" blend. */
    fun glow(a: Float = k, div: Int = 8) {
        val sw = max(1, w / div); val sh = max(1, h / div)
        // shrink in two steps so the result is really blurred, not just aliased
        val mid = e.smallBitmap(0, max(1, w / 2), max(1, h / 2))
        val small = e.smallBitmap(1, sw, sh)
        val mc = Canvas(mid); mc.drawBitmap(src, null, RectF(0f, 0f, mid.width.toFloat(), mid.height.toFloat()), e.paint.also { it.colorFilter = null; it.xfermode = null; it.alpha = 255 })
        val sc = Canvas(small); sc.drawBitmap(mid, null, RectF(0f, 0f, sw.toFloat(), sh.toFloat()), e.paint)
        val p = paintA(a)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        c.drawBitmap(small, null, RectF(0f, 0f, wf, hf), p)
        p.xfermode = null
    }

    // ---------------------------------------------------------------- per-pixel work (at a limited size, then stretched back)
    /** Runs [op] on a copy of the picture no bigger than [maxLong] pixels, then lays the result over the picture at strength [a]. */
    fun pix(maxLong: Int = 768, a: Float = k, op: (dst: IntArray, sp: IntArray, w: Int, h: Int) -> Unit) {
        val sc = min(1f, maxLong.toFloat() / max(w, h))
        val ww = max(8, (w * sc).roundToInt())
        val wh = max(8, (h * sc).roundToInt())
        val n = ww * wh
        val sp = e.ints(n, 0)
        if (ww == w && wh == h) src.getPixels(sp, 0, w, 0, 0, w, h)
        else {
            val sb = Bitmap.createScaledBitmap(src, ww, wh, true)
            sb.getPixels(sp, 0, ww, 0, 0, ww, wh)
            if (sb !== src) sb.recycle()
        }
        val dp = e.ints(n, 1)
        op(dp, sp, ww, wh)
        val wb = e.workBitmap(ww, wh)
        wb.setPixels(dp, 0, ww, 0, 0, ww, wh)
        c.drawBitmap(wb, null, RectF(0f, 0f, wf, hf), paintA(a))
    }

    // ---------------------------------------------------------------- warping
    /** Pushes the picture around: [f] adds to d[0], d[1] how far the point (x, y) should move. */
    fun warp(f: (x: Float, y: Float, d: FloatArray) -> Unit) {
        val cols = 36
        val rows = max(12, min(72, (cols * hf / wf).roundToInt()))
        val v = e.vertices((cols + 1) * (rows + 1) * 2)
        val d = e.d2
        var i = 0
        for (j in 0..rows) {
            val y = hf * j / rows
            for (xi in 0..cols) {
                val x = wf * xi / cols
                d[0] = 0f; d[1] = 0f
                f(x, y, d)
                var nx = x + d[0]; var ny = y + d[1]
                // the edge of the picture stays on the edge, so no gaps appear
                if (xi == 0) nx = 0f else if (xi == cols) nx = wf
                if (j == 0) ny = 0f else if (j == rows) ny = hf
                v[i++] = nx; v[i++] = ny
            }
        }
        c.drawBitmapMesh(src, cols, rows, v, 0, null, 0, paintA(1f))
    }

    /** Magnify (s > 0) or shrink (s < 0, down to -0.9) around a point inside radius [r]. */
    fun bump(x: Float, y: Float, cx: Float, cy: Float, r: Float, s: Float, d: FloatArray) {
        val dx = x - cx; val dy = y - cy
        val q = dx * dx + dy * dy
        if (q >= r * r) return
        val u = sqrt(q) / r
        val f = s * (1f - u) * (1f - u)
        d[0] += dx * f; d[1] += dy * f
    }

    /** Twist around a point: [ang] radians at the centre, fading to nothing at radius [r]. */
    fun twirl(x: Float, y: Float, cx: Float, cy: Float, r: Float, ang: Float, d: FloatArray) {
        val dx = x - cx; val dy = y - cy
        val q = dx * dx + dy * dy
        if (q >= r * r) return
        val u = sqrt(q) / r
        val a = ang * (1f - u) * (1f - u)
        val cs = cos(a); val sn = sin(a)
        d[0] += cx + dx * cs - dy * sn - x
        d[1] += cy + dx * sn + dy * cs - y
    }

    /** Slides everything near a point by (ax, ay); keep the slide under 0.45 x r so the picture does not fold. */
    fun slide(x: Float, y: Float, cx: Float, cy: Float, r: Float, ax: Float, ay: Float, d: FloatArray) {
        val dx = x - cx; val dy = y - cy
        val q = dx * dx + dy * dy
        if (q >= r * r) return
        val u = sqrt(q) / r
        val f = (1f - u) * (1f - u)
        d[0] += ax * f; d[1] += ay * f
    }

    /** Stretches (positive) or squeezes (negative) sideways by [sx] and up-down by [sy] inside an oval of radii [rx], [ry]. */
    fun stretch(x: Float, y: Float, cx: Float, cy: Float, rx: Float, ry: Float, sx: Float, sy: Float, d: FloatArray) {
        val dx = x - cx; val dy = y - cy
        val nx = dx / rx; val ny = dy / ry
        val q = nx * nx + ny * ny
        if (q >= 1f) return
        val u = sqrt(q)
        val f = (1f - u) * (1f - u)
        d[0] += dx * sx * f; d[1] += dy * sy * f
    }

    // ---------------------------------------------------------------- realism helpers (shading, shadows, glow, fur)
    fun shadeColor(c: Int, f: Float): Int {
        val r = (Color.red(c) * f).roundToInt().coerceIn(0, 255)
        val g = (Color.green(c) * f).roundToInt().coerceIn(0, 255)
        val b = (Color.blue(c) * f).roundToInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(c), r, g, b)
    }

    /** The colour of the picture around a point (used to match skin and hair). */
    fun skinAt(x: Float, y: Float): Int {
        var r = 0; var g = 0; var b = 0; var n = 0
        for (dy in -2..2 step 2) for (dx in -2..2 step 2) {
            val px = (x + dx * s * 0.004f).toInt().coerceIn(0, w - 1)
            val py = (y + dy * s * 0.004f).toInt().coerceIn(0, h - 1)
            val p = src.getPixel(px, py)
            r += Color.red(p); g += Color.green(p); b += Color.blue(p); n++
        }
        return Color.rgb(r / n, g / n, b / n)
    }

    /** A ball-like oval: light from the top left. */
    fun gradOval(x: Float, y: Float, rx: Float, ry: Float, light: Int, dark: Int, a: Float = 1f) {
        val p = e.fp
        p.style = Paint.Style.FILL; p.maskFilter = null; p.xfermode = null; p.colorFilter = null
        p.color = Color.BLACK; p.alpha = (255 * k * a).roundToInt().coerceIn(0, 255)
        p.shader = RadialGradient(x - rx * 0.35f, y - ry * 0.4f, max(rx, ry) * 1.3f, light, dark, Shader.TileMode.CLAMP)
        e.rectF.set(x - rx, y - ry, x + rx, y + ry)
        c.drawOval(e.rectF, p)
        p.shader = null
    }

    fun gradRRect(l: Float, t: Float, r: Float, b: Float, rad: Float, c1: Int, c2: Int, vertical: Boolean = true, a: Float = 1f) {
        val p = e.fp
        p.style = Paint.Style.FILL; p.maskFilter = null; p.xfermode = null; p.colorFilter = null
        p.color = Color.BLACK; p.alpha = (255 * k * a).roundToInt().coerceIn(0, 255)
        p.shader = if (vertical) LinearGradient(l, t, l, b, c1, c2, Shader.TileMode.CLAMP) else LinearGradient(l, t, r, t, c1, c2, Shader.TileMode.CLAMP)
        e.rectF.set(l, t, r, b)
        c.drawRoundRect(e.rectF, rad, rad, p)
        p.shader = null
    }

    /** Fills the path in [e.path] with a straight gradient from (x1,y1) to (x2,y2). */
    fun gradPath(x1: Float, y1: Float, x2: Float, y2: Float, c1: Int, c2: Int, a: Float = 1f) {
        val p = e.fp
        p.style = Paint.Style.FILL; p.maskFilter = null; p.xfermode = null; p.colorFilter = null
        p.color = Color.BLACK; p.alpha = (255 * k * a).roundToInt().coerceIn(0, 255)
        p.shader = LinearGradient(x1, y1, x2, y2, c1, c2, Shader.TileMode.CLAMP)
        c.drawPath(e.path, p)
        p.shader = null
    }

    /** A blurred oval: soft shadows, blush, paint on skin. */
    fun softOval(x: Float, y: Float, rx: Float, ry: Float, color: Int, a: Float, blur: Float) {
        val p = fill(color, a)
        p.maskFilter = BlurMaskFilter(max(1f, blur), BlurMaskFilter.Blur.NORMAL)
        e.rectF.set(x - rx, y - ry, x + rx, y + ry)
        c.drawOval(e.rectF, p)
        p.maskFilter = null
    }

    /** Light that adds up with the picture (fire, lasers, halos). */
    fun lightBall(x: Float, y: Float, r: Float, color: Int, a: Float = 1f) {
        val p = e.fp
        p.style = Paint.Style.FILL; p.maskFilter = null; p.colorFilter = null
        p.color = Color.BLACK; p.alpha = 255
        val cc = Color.argb((Color.alpha(color) * k * a).roundToInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
        p.shader = RadialGradient(x, y, max(1f, r), cc, Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)), Shader.TileMode.CLAMP)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        c.drawCircle(x, y, r, p)
        p.xfermode = null; p.shader = null
    }

    /** Hair or fur: many thin curved strokes inside an oval, pointing along [dir] degrees (90 = down), with natural variation. */
    fun fur(cx: Float, cy: Float, rx: Float, ry: Float, count: Int, len: Float, width: Float, base: Int, seed: Int,
            dir: Float = 90f, spread: Float = 40f, skip: ((Float, Float) -> Boolean)? = null) {
        val pa = e.path
        for (i in 0 until count) {
            val ang = (rnd(i, seed) * 6.2831855f)
            val rr = sqrt(rnd(i, seed + 1))
            val x = cx + cos(ang) * rx * rr
            val y = cy + sin(ang) * ry * rr
            if (skip != null && skip(x, y)) continue
            val d = Math.toRadians((dir + (rnd(i, seed + 2) - 0.5f) * spread).toDouble())
            val l = len * (0.6f + 0.7f * rnd(i, seed + 3))
            val shade = 0.62f + 0.7f * rnd(i, seed + 4)
            val col = shadeColor(base, shade)
            val ex = x + (cos(d) * l).toFloat(); val ey = y + (sin(d) * l).toFloat()
            val curv = (rnd(i, seed + 6) - 0.5f) * l * 0.7f
            pa.reset(); pa.moveTo(x, y)
            pa.quadTo((x + ex) / 2f - sin(d).toFloat() * curv, (y + ey) / 2f + cos(d).toFloat() * curv, ex, ey)
            c.drawPath(pa, stroke(col, width * (0.6f + 0.6f * rnd(i, seed + 5)), 0.9f))
        }
    }

    /** A soft, blurred small copy of the whole picture (for skin smoothing, glow). */
    fun blurredCopy(): Bitmap {
        val mid = e.smallBitmap(0, max(1, w / 2), max(1, h / 2))
        val small = e.smallBitmap(1, max(1, w / 6), max(1, h / 6))
        val p = paintA(1f)
        Canvas(mid).drawBitmap(src, null, RectF(0f, 0f, mid.width.toFloat(), mid.height.toFloat()), p)
        Canvas(small).drawBitmap(mid, null, RectF(0f, 0f, small.width.toFloat(), small.height.toFloat()), p)
        return small
    }
}
