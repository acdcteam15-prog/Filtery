package com.example.filtery

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

private fun MutableList<Fx>.fx(id: String, name: String, icon: String, face: Boolean = true, draw: FxCtx.() -> Unit) {
    add(Fx(id, name, Cat.WARPS, icon, face, draw))
}

internal fun MutableList<Fx>.addWarpFilters() {
    fx("bighead", "Big Head", "🎈") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) bump(x, y, f.cx, f.cy, max(f.w, f.h) * 1.05f, 0.95f * k, d) }
    }
    fx("tinyhead", "Pin Head", "📍") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) bump(x, y, f.cx, f.cy, max(f.w, f.h) * 1.1f, -0.75f * k, d) }
    }
    fx("bigeyes", "Big Eyes", "👀") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) { val r = f.eyeDist * 0.62f; bump(x, y, f.lx, f.ly, r, 1.5f * k, d); bump(x, y, f.rx, f.ry, r, 1.5f * k, d) } }
    }
    fx("bugeyes", "Bug Eyes", "🐞") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) { val r = f.eyeDist * 0.72f; bump(x, y, f.lx, f.ly, r, 2.6f * k, d); bump(x, y, f.rx, f.ry, r, 2.6f * k, d) } }
    }
    fx("tinyeyes", "Tiny Eyes", "🔍") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) { val r = f.eyeDist * 0.6f; bump(x, y, f.lx, f.ly, r, -0.85f * k, d); bump(x, y, f.rx, f.ry, r, -0.85f * k, d) } }
    }
    fx("bignose", "Big Nose", "🤥") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) bump(x, y, f.nx, f.ny, f.w * 0.34f, 1.9f * k, d) }
    }
    fx("bigmouth", "Big Mouth", "😮") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) bump(x, y, f.mx, f.my, f.w * 0.4f, 1.8f * k, d) }
    }
    fx("tinymouth", "Tiny Mouth", "🤏") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) bump(x, y, f.mx, f.my, f.w * 0.4f, -0.8f * k, d) }
    }
    fx("chipmunk", "Chipmunk", "🐿️") {
        val fs = facesOrDefault
        warp { x, y, d ->
            for (f in fs) {
                val r = f.w * 0.3f
                bump(x, y, f.hx(-0.34f * f.w, 0.12f * f.h), f.hy(-0.34f * f.w, 0.12f * f.h), r, 1.5f * k, d)
                bump(x, y, f.hx(0.34f * f.w, 0.12f * f.h), f.hy(0.34f * f.w, 0.12f * f.h), r, 1.5f * k, d)
            }
        }
    }
    fx("alien", "Alien Head", "👽") {
        val fs = facesOrDefault
        warp { x, y, d ->
            for (f in fs) {
                bump(x, y, f.hx(0f, -0.3f * f.h), f.hy(0f, -0.3f * f.h), f.w * 0.7f, 1.4f * k, d)
                bump(x, y, f.hx(0f, 0.42f * f.h), f.hy(0f, 0.42f * f.h), f.w * 0.55f, -0.6f * k, d)
            }
        }
        rect(0f, 0f, wf, hf, 0x4000FF55, 0.9f)
    }
    fx("longchin", "Long Chin", "🗿") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) { val r = f.w * 0.95f; slide(x, y, f.hx(0f, 0.42f * f.h), f.hy(0f, 0.42f * f.h), r, 0f, 0.42f * r * k, d) } }
    }
    fx("conehead", "Cone Head", "🔺") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) { val r = f.w * 0.95f; slide(x, y, f.hx(0f, -0.38f * f.h), f.hy(0f, -0.38f * f.h), r, 0f, -0.42f * r * k, d) } }
    }
    fx("widef", "Wide Face", "🐸") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) stretch(x, y, f.cx, f.cy, f.w * 1.0f, f.h * 0.9f, 0.8f * k, 0f, d) }
    }
    fx("tallskinny", "Tall & Skinny", "🦒") {
        val fs = facesOrDefault
        warp { x, y, d -> val f = fs[0]; stretch(x, y, f.cx, f.cy, wf * 0.95f, hf * 0.7f, -0.25f * k, 0.9f * k, d) }
    }
    fx("shortfat", "Short & Wide", "🍩") {
        val fs = facesOrDefault
        warp { x, y, d -> val f = fs[0]; stretch(x, y, f.cx, f.cy, wf * 0.95f, hf * 0.7f, 0.9f * k, -0.25f * k, d) }
    }
    fx("swirl", "Swirl", "🌀") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) twirl(x, y, f.cx, f.cy, max(f.w, f.h) * 1.25f, 2.4f * k, d) }
    }
    fx("fisheye", "Fisheye", "🐟", false) {
        warp { x, y, d -> bump(x, y, wf / 2f, hf / 2f, hypot(wf, hf) * 0.46f, 0.95f * k, d) }
    }
    fx("squeeze", "Squeeze", "🗜️", false) {
        warp { x, y, d -> bump(x, y, wf / 2f, hf / 2f, hypot(wf, hf) * 0.55f, -0.7f * k, d) }
    }
    fx("wave", "Funhouse", "🌊", false) {
        warp { x, y, d ->
            d[0] += sin(y / hf * 14f + t * 3f) * 0.022f * wf * k
            d[1] += cos(x / wf * 9f + t * 2.5f) * 0.008f * hf * k
        }
    }
    fx("ripple", "Ripple", "💧", false) {
        warp { x, y, d ->
            val dx = x - wf / 2f; val dy = y - hf / 2f
            val dist = hypot(dx, dy)
            if (dist > 1f) {
                val a = sin(dist / s * 18f - t * 5f) * 0.014f * s * k
                d[0] += dx / dist * a; d[1] += dy / dist * a
            }
        }
    }
    fx("melt", "Melt", "🍦", false) {
        warp { x, y, d -> d[1] += (0.5f + 0.5f * sin(x / wf * 10f + t * 2f)) * 0.07f * hf * k * (y / hf) }
    }

    fx("baby", "Baby Face", "👶") {
        val fs = facesOrDefault
        warp { x, y, d ->
            for (f in fs) {
                val r = f.eyeDist * 0.7f
                bump(x, y, f.lx, f.ly, r, 0.9f * k, d); bump(x, y, f.rx, f.ry, r, 0.9f * k, d)
                bump(x, y, f.nx, f.ny, f.w * 0.3f, -0.5f * k, d)
                bump(x, y, f.mx, f.my, f.w * 0.35f, -0.45f * k, d)
                bump(x, y, f.hx(0f, -0.3f * f.h), f.hy(0f, -0.3f * f.h), f.w * 0.6f, 0.35f * k, d)
                bump(x, y, f.hx(0f, 0.42f * f.h), f.hy(0f, 0.42f * f.h), f.w * 0.5f, -0.4f * k, d)
            }
        }
    }
    fx("bigsmile", "Big Smile", "😁") {
        val fs = facesOrDefault
        warp { x, y, d ->
            for (f in fs) {
                for (sg in intArrayOf(-1, 1)) {
                    slide(x, y, f.hx(sg * 0.2f * f.w, 0.28f * f.h), f.hy(sg * 0.2f * f.w, 0.28f * f.h), f.w * 0.3f, sg * 0.08f * f.w * k, -0.1f * f.w * k, d)
                    bump(x, y, f.hx(sg * 0.34f * f.w, 0.1f * f.h), f.hy(sg * 0.34f * f.w, 0.1f * f.h), f.w * 0.25f, 0.5f * k, d)
                }
            }
        }
    }
    fx("surprised", "Surprised", "😲") {
        val fs = facesOrDefault
        warp { x, y, d ->
            for (f in fs) {
                val r = f.eyeDist * 0.65f
                bump(x, y, f.lx, f.ly, r, 1.0f * k, d); bump(x, y, f.rx, f.ry, r, 1.0f * k, d)
                for (sg in intArrayOf(-1, 1)) {
                    val bx = if (sg < 0) f.lx else f.rx; val by = if (sg < 0) f.ly else f.ry
                    slide(x, y, bx, by - 0.2f * f.w, f.w * 0.28f, 0f, -0.1f * f.w * k, d)
                }
                stretch(x, y, f.mx, f.my + 0.03f * f.w, f.w * 0.24f, f.w * 0.2f, -0.2f * k, 0.9f * k, d)
            }
        }
    }
    fx("frogeyes", "Frog Eyes", "🐸") {
        val fs = facesOrDefault
        warp { x, y, d ->
            for (f in fs) {
                val r = f.eyeDist * 0.6f
                for (i in 0..1) {
                    val ex = if (i == 0) f.lx else f.rx; val ey = if (i == 0) f.ly else f.ry
                    slide(x, y, ex, ey, r, 0f, -0.2f * r * k, d)
                    bump(x, y, ex, ey - 0.2f * r * k, r * 0.9f, 1.4f * k, d)
                }
            }
        }
    }
    fx("bigforehead", "Big Forehead", "🥚") {
        val fs = facesOrDefault
        warp { x, y, d -> for (f in fs) bump(x, y, f.hx(0f, -0.32f * f.h), f.hy(0f, -0.32f * f.h), f.w * 0.75f, 1.5f * k, d) }
    }
    fx("pinocchio", "Long Nose", "👃") {
        val fs = facesOrDefault
        warp { x, y, d ->
            for (f in fs) {
                bump(x, y, f.nx, f.ny, f.w * 0.3f, 1.2f * k, d)
                slide(x, y, f.nx, f.ny, f.w * 0.4f, 0f, 0.4f * f.w * 0.4f * k, d)
            }
        }
    }
}
