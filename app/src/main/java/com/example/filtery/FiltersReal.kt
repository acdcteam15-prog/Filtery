package com.example.filtery

import kotlin.math.sin

private fun MutableList<Fx>.fx(id: String, name: String, icon: String, draw: FxCtx.() -> Unit) {
    add(Fx("real_$id", name, Cat.FACES, icon, true, draw))
}

/** Funny faces made only from your own real pixels: pieces of your face copied somewhere else. */
internal fun MutableList<Fx>.addRealFaces() {
    fx("thirdeye", "Real 3rd Eye", "👁️") {
        for (f in faces) patch(f.lx, f.ly, f.eyeDist * 0.62f, f.hx(0f, -0.3f * f.h), f.hy(0f, -0.3f * f.h), 1.0f, f.rot)
    }
    fx("cheekeyes", "Eyes on Cheeks", "🙃") {
        for (f in faces) for (sg in intArrayOf(-1, 1)) {
            val sx = if (sg < 0) f.lx else f.rx; val sy = if (sg < 0) f.ly else f.ry
            patch(sx, sy, f.eyeDist * 0.6f, f.hx(sg * 0.34f * f.w, 0.16f * f.h), f.hy(sg * 0.34f * f.w, 0.16f * f.h), 0.95f, f.rot)
        }
    }
    fx("foreheadmouth", "Mouth on Forehead", "👄") {
        for (f in faces) patch(f.mx, f.my, f.w * 0.3f, f.hx(0f, -0.3f * f.h), f.hy(0f, -0.3f * f.h), 0.95f, f.rot)
    }
    fx("cyclops", "Cyclops", "🧿") {
        for (f in faces) {
            val skin = skinAt(f.hx(0f, 0.02f * f.h), f.hy(0f, 0.02f * f.h))
            softOval(f.lx, f.ly, f.eyeDist * 0.55f, f.eyeDist * 0.4f, skin, 1f, f.eyeDist * 0.15f)
            softOval(f.rx, f.ry, f.eyeDist * 0.55f, f.eyeDist * 0.4f, skin, 1f, f.eyeDist * 0.15f)
            patch(f.lx, f.ly, f.eyeDist * 0.62f, f.ex, f.ey, 2.1f, f.rot)
        }
    }
    fx("twonoses", "Two Noses", "👃") {
        for (f in faces) for (sg in intArrayOf(-1, 1)) patch(f.nx, f.ny, f.w * 0.2f, f.hx(sg * 0.42f * f.w, 0.1f * f.h), f.hy(sg * 0.42f * f.w, 0.1f * f.h), 0.9f, f.rot)
    }
    fx("minime", "Mini Me", "👥") {
        for (f in faces) {
            val r = maxOf(f.w, f.h) * 0.62f
            for (i in 0 until 4) {
                val a = t * 0.8f + i * 1.5708f
                patch(f.cx, f.cy, r, f.cx + kotlin.math.cos(a) * f.w * 1.05f, f.cy + sin(a) * f.h * 0.85f, 0.38f, f.rot + sin(a) * 15f)
            }
        }
    }
    fx("twin", "Upside-Down Twin", "🙃") {
        for (f in faces) {
            val r = maxOf(f.w, f.h) * 0.7f
            val side = if (f.cx < wf / 2f) 1f else -1f
            patch(f.cx, f.cy, r, f.cx + side * f.w * 1.1f, f.cy, 0.9f, 180f + f.rot)
        }
    }
    fx("noseonforehead", "Nose on Forehead", "👃") {
        for (f in faces) patch(f.nx, f.ny, f.w * 0.22f, f.hx(0f, -0.38f * f.h), f.hy(0f, -0.38f * f.h), 0.95f, f.rot)
    }
    fx("mouthonchin", "Mouth on Chin", "💋") {
        for (f in faces) patch(f.mx, f.my, f.w * 0.3f, f.hx(0f, 0.42f * f.h), f.hy(0f, 0.42f * f.h), 0.95f, f.rot)
    }
    fx("eyesonchin", "Eyes on Chin", "👀") {
        for (f in faces) for (sg in intArrayOf(-1, 1)) {
            val sx = if (sg < 0) f.lx else f.rx; val sy = if (sg < 0) f.ly else f.ry
            patch(sx, sy, f.eyeDist * 0.55f, f.hx(sg * 0.3f * f.w, 0.45f * f.h), f.hy(sg * 0.3f * f.w, 0.45f * f.h), 0.9f, f.rot)
        }
    }
    fx("cheekonnose", "Cheek on Nose", "😊") {
        for (f in faces) {
            val cx = (f.lx + f.rx) / 2f; val cy = (f.ly + f.ry) / 2f
            patch(cx, cy, f.eyeDist * 0.5f, f.nx, f.ny, 1.1f, f.rot)
        }
    }
    fx("chinonforehead", "Chin on Forehead", "🤨") {
        for (f in faces) {
            val r = f.w * 0.4f
            patch(f.cx, f.my + f.h * 0.35f, r, f.hx(0f, -0.4f * f.h), f.hy(0f, -0.4f * f.h), 0.9f, f.rot)
        }
    }
}
