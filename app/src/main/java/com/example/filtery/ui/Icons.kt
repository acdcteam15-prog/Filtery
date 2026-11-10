package com.example.filtery.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class Ic { Bolt, BoltOff, BoltAuto, Timer, Grid, Gear, Flip, Gallery, Star, StarFill, Dice, Close, Back, Share, Trash, Play, Check, Download, Photo, Video }

/** The app's own icon set on a 24 x 24 grid: no icon library needed. */
@Composable
fun AppIcon(ic: Ic, modifier: Modifier = Modifier, size: Dp = 24.dp, color: Color = Brand.Text) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 24f
        val sw = 2f * u
        val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun pt(x: Float, y: Float) = Offset(x * u, y * u)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color = color, start = pt(x1, y1), end = pt(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)
        fun poly(vararg c: Float, close: Boolean = false, fill: Boolean = false) {
            val p = Path()
            p.moveTo(c[0] * u, c[1] * u)
            var i = 2
            while (i + 1 < c.size) { p.lineTo(c[i] * u, c[i + 1] * u); i += 2 }
            if (close) p.close()
            drawPath(path = p, color = color, style = if (fill) Fill else st)
        }
        fun circle(cx: Float, cy: Float, r: Float, fill: Boolean = false) =
            drawCircle(color = color, radius = r * u, center = pt(cx, cy), style = if (fill) Fill else st)
        fun rrect(l: Float, t: Float, r: Float, b: Float, rad: Float) =
            drawRoundRect(color = color, topLeft = pt(l, t), size = Size((r - l) * u, (b - t) * u), cornerRadius = CornerRadius(rad * u, rad * u), style = st)
        fun bolt(fill: Boolean = false) = poly(13f, 2.5f, 5f, 13.5f, 11f, 13.5f, 10f, 21.5f, 19f, 10f, 13f, 10f, close = true, fill = fill)

        when (ic) {
            Ic.Bolt -> bolt(true)
            Ic.BoltOff -> { bolt(false); line(4f, 4f, 20f, 20f) }
            Ic.BoltAuto -> { bolt(false); line(16f, 20f, 18.5f, 14f); line(18.5f, 14f, 21f, 20f); line(17f, 18f, 20f, 18f) }
            Ic.Timer -> { circle(12f, 13.5f, 7.5f); line(12f, 13.5f, 12f, 9.5f); line(9.5f, 3f, 14.5f, 3f); line(18f, 7f, 19.5f, 5.5f) }
            Ic.Grid -> { rrect(3.5f, 3.5f, 20.5f, 20.5f, 2.5f); line(9.2f, 3.5f, 9.2f, 20.5f); line(14.8f, 3.5f, 14.8f, 20.5f); line(3.5f, 9.2f, 20.5f, 9.2f); line(3.5f, 14.8f, 20.5f, 14.8f) }
            Ic.Gear -> {
                circle(12f, 12f, 3f); circle(12f, 12f, 6.3f)
                for (k in 0 until 8) {
                    val a = k * Math.PI / 4
                    val c = cos(a).toFloat(); val s = sin(a).toFloat()
                    line(12f + c * 6.3f, 12f + s * 6.3f, 12f + c * 9.3f, 12f + s * 9.3f)
                }
            }
            Ic.Flip -> {
                drawArc(color = color, startAngle = 200f, sweepAngle = 125f, useCenter = false, topLeft = pt(4.5f, 4.5f), size = Size(15f * u, 15f * u), style = st)
                drawArc(color = color, startAngle = 20f, sweepAngle = 125f, useCenter = false, topLeft = pt(4.5f, 4.5f), size = Size(15f * u, 15f * u), style = st)
                poly(15.8f, 4.2f, 18.6f, 7.6f, 14.4f, 8.6f)
                poly(8.2f, 19.8f, 5.4f, 16.4f, 9.6f, 15.4f)
            }
            Ic.Gallery -> { rrect(3f, 4f, 21f, 20f, 3f); circle(8.5f, 9.5f, 1.7f); poly(4f, 18f, 10f, 12f, 14f, 16f, 17f, 13f, 20f, 16f) }
            Ic.Star -> poly(12f, 3f, 14.6f, 9f, 21f, 9.6f, 16.2f, 13.8f, 17.6f, 20.2f, 12f, 16.9f, 6.4f, 20.2f, 7.8f, 13.8f, 3f, 9.6f, 9.4f, 9f, close = true)
            Ic.StarFill -> poly(12f, 3f, 14.6f, 9f, 21f, 9.6f, 16.2f, 13.8f, 17.6f, 20.2f, 12f, 16.9f, 6.4f, 20.2f, 7.8f, 13.8f, 3f, 9.6f, 9.4f, 9f, close = true, fill = true)
            Ic.Dice -> { rrect(4f, 4f, 20f, 20f, 4f); circle(8.5f, 8.5f, 1.1f, true); circle(12f, 12f, 1.1f, true); circle(15.5f, 15.5f, 1.1f, true); circle(15.5f, 8.5f, 1.1f, true); circle(8.5f, 15.5f, 1.1f, true) }
            Ic.Close -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
            Ic.Back -> poly(15f, 5f, 8f, 12f, 15f, 19f)
            Ic.Share -> { circle(6f, 12f, 2.4f); circle(18f, 6f, 2.4f); circle(18f, 18f, 2.4f); line(8.1f, 10.9f, 15.9f, 7.1f); line(8.1f, 13.1f, 15.9f, 16.9f) }
            Ic.Trash -> { line(4f, 7f, 20f, 7f); poly(6f, 7f, 7f, 20f, 17f, 20f, 18f, 7f); line(10f, 3.5f, 14f, 3.5f); line(10f, 11f, 10f, 16f); line(14f, 11f, 14f, 16f) }
            Ic.Play -> poly(8f, 5f, 19f, 12f, 8f, 19f, close = true, fill = true)
            Ic.Check -> poly(5f, 12.5f, 10f, 17.5f, 19f, 7f)
            Ic.Download -> { line(12f, 4f, 12f, 15f); poly(7.5f, 10.5f, 12f, 15f, 16.5f, 10.5f); line(5f, 20f, 19f, 20f) }
            Ic.Photo -> { rrect(3f, 7f, 21f, 20f, 3f); circle(12f, 13.5f, 3.6f); poly(8.5f, 7f, 10f, 4f, 14f, 4f, 15.5f, 7f) }
            Ic.Video -> { rrect(3f, 6f, 15.5f, 18f, 3f); poly(15.5f, 10.5f, 21f, 7f, 21f, 17f, 15.5f, 13.5f, close = true) }
        }
    }
}
