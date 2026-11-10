package com.example.filtery

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** A detected face in normalised picture coordinates (0..1 across the width and across the height). */
class Face(
    var id: Int,
    var cx: Float, var cy: Float, var w: Float, var h: Float,
    /** Degrees to turn the canvas so that drawing follows the head (clockwise is positive). */
    var rot: Float,
    var lx: Float, var ly: Float, var rx: Float, var ry: Float,   // eyes, lx is the one on the left of the picture
    var nx: Float, var ny: Float, var mx: Float, var my: Float,   // nose base and mouth centre
) {
    /** Moves this face a fraction [a] of the way to [o] (used to calm the jitter between frames). */
    fun lerp(o: Face, a: Float) {
        cx += (o.cx - cx) * a; cy += (o.cy - cy) * a; w += (o.w - w) * a; h += (o.h - h) * a
        rot += (o.rot - rot) * a
        lx += (o.lx - lx) * a; ly += (o.ly - ly) * a; rx += (o.rx - rx) * a; ry += (o.ry - ry) * a
        nx += (o.nx - nx) * a; ny += (o.ny - ny) * a; mx += (o.mx - mx) * a; my += (o.my - my) * a
    }

    fun duplicate() = Face(id, cx, cy, w, h, rot, lx, ly, rx, ry, nx, ny, mx, my)

    companion object {
        /** A face built from just its box: eyes, nose and mouth are placed where they usually are. [aspect] = picture width / height. */
        fun fromBox(id: Int, cx: Float, cy: Float, w: Float, h: Float, rot: Float, aspect: Float): Face {
            // measure everything in units where the picture is [aspect] wide and 1 high, so angles stay true
            val cxu = cx * aspect; val wu = w * aspect
            val r = Math.toRadians(rot.toDouble())
            val c = cos(r).toFloat(); val s = sin(r).toFloat()
            fun px(dx: Float, dy: Float) = (cxu + c * dx - s * dy) / aspect
            fun py(dx: Float, dy: Float) = cy + s * dx + c * dy
            return Face(
                id, cx, cy, w, h, rot,
                px(-0.22f * wu, -0.12f * h), py(-0.22f * wu, -0.12f * h), px(0.22f * wu, -0.12f * h), py(0.22f * wu, -0.12f * h),
                px(0f, 0.12f * h), py(0f, 0.12f * h), px(0f, 0.28f * h), py(0f, 0.28f * h),
            )
        }
    }
}

/** A face in the pixels of the picture being drawn, with the anchor points the filters use. */
class PFace(f: Face, W: Float, H: Float) {
    val cx = f.cx * W
    val cy = f.cy * H
    val w = f.w * W
    val h = f.h * H
    val rot = f.rot
    val lx = f.lx * W
    val ly = f.ly * H
    val rx = f.rx * W
    val ry = f.ry * H
    val nx = f.nx * W
    val ny = f.ny * H
    val mx = f.mx * W
    val my = f.my * H
    val ex = (lx + rx) / 2f           // middle between the eyes
    val ey = (ly + ry) / 2f
    val eyeDist = hypot(rx - lx, ry - ly).coerceAtLeast(w * 0.2f)

    private val sr = sin(Math.toRadians(rot.toDouble())).toFloat()
    private val cr = cos(Math.toRadians(rot.toDouble())).toFloat()

    /** Picture x of a point given in the head's own frame (dx to the head's right, dy down), centred on the face. */
    fun hx(dx: Float, dy: Float) = cx + cr * dx - sr * dy
    fun hy(dx: Float, dy: Float) = cy + sr * dx + cr * dy

    val topX get() = hx(0f, -0.55f * h)
    val topY get() = hy(0f, -0.55f * h)
}
