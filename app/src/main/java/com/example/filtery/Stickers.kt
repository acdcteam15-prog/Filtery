package com.example.filtery

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF

/**
 * Real-photo props. Put transparent PNG pictures (a real hat, beard, glasses...) in app/src/main/assets/stickers/ and each one
 * becomes a face filter by itself. The start of the file name says what it is and where it goes:
 * hat_, crown_, ears_, glasses_, mask_, beard_, mustache_, nose_, mouth_, face_   (example: hat_wizard.png, beard_white.png)
 */
object Stickers {
    private var am: AssetManager? = null
    private val cache = HashMap<String, Bitmap?>()

    fun init(ctx: Context) { am = ctx.applicationContext.assets }

    private fun load(file: String): Bitmap? = cache.getOrPut(file) {
        try { am?.open("stickers/$file")?.use { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null }
    }

    private fun icon(type: String) = when (type) {
        "hat" -> "🎩"; "crown" -> "👑"; "ears" -> "🐰"; "glasses" -> "😎"; "mask" -> "🦸"; "beard" -> "🧔"
        "mustache" -> "👨"; "nose" -> "👃"; "mouth" -> "👄"; else -> "📸"
    }

    fun build(): List<Fx> {
        val out = ArrayList<Fx>()
        val files = try { am?.list("stickers")?.filter { it.endsWith(".png", true) }?.sorted() ?: emptyList() } catch (e: Exception) { emptyList() }
        for (file in files) {
            val base = file.substringBeforeLast('.')
            val type = base.substringBefore('_').lowercase()
            val name = base.substringAfter('_', base).replace('_', ' ').replaceFirstChar { it.uppercase() }
            out.add(Fx("png_$base", name, Cat.FACES, icon(type), true) {
                val bmp = load(file)
                if (bmp != null) for (f in faces) {
                    val u = f.w; val d = f.eyeDist
                    val ratio = bmp.height.toFloat() / bmp.width
                    // x, y = anchor point, width of the picture, and which edge of the picture sits on the anchor (0 centre, 1 bottom, 2 top)
                    val x: Float; val y: Float; val wd: Float; val edge: Int
                    when (type) {
                        "hat" -> { x = f.topX; y = f.topY + 0.06f * u; wd = 1.3f * u; edge = 1 }
                        "crown" -> { x = f.topX; y = f.topY + 0.04f * u; wd = 0.85f * u; edge = 1 }
                        "ears" -> { x = f.topX; y = f.topY + 0.12f * u; wd = 1.5f * u; edge = 1 }
                        "glasses" -> { x = f.ex; y = f.ey; wd = 2.5f * d; edge = 0 }
                        "mask" -> { x = f.ex; y = f.ey; wd = 3.0f * d; edge = 0 }
                        "beard" -> { x = f.nx; y = f.ny + 0.02f * u; wd = 1.3f * u; edge = 2 }
                        "mustache" -> { x = (f.nx + f.mx) / 2f; y = (f.ny + f.my) / 2f; wd = 0.75f * u; edge = 0 }
                        "nose" -> { x = f.nx; y = f.ny; wd = 0.4f * u; edge = 0 }
                        "mouth" -> { x = f.mx; y = f.my; wd = 0.55f * u; edge = 0 }
                        else -> { x = f.cx; y = f.cy; wd = 1.3f * u; edge = 0 }
                    }
                    val hh = wd * ratio
                    val top = when (edge) { 1 -> -hh; 2 -> 0f; else -> -hh / 2f }
                    at(x, y, f.rot) { c.drawBitmap(bmp, null, RectF(-wd / 2f, top, wd / 2f, top + hh), paintA(1f)) }
                }
            })
        }
        return out
    }
}
