package app.hearthspell.ui.art

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Art pixels per scene cell: every bitmap of the game (sprites and scenes) is drawn this many times finer than the grid. */
const val ART_D = 4

/**
 * A piece of art: [rw] x [rh] ARGB pixels. It takes [w] x [h] cells of the scene grid ([ART_D] pixels per cell), so
 * layout code works in cells while the art itself stays fine.
 */
@Immutable
class Sprite(val rw: Int, val rh: Int, internal val argb: IntArray) {
    val w: Int get() = (rw + ART_D - 1) / ART_D
    val h: Int get() = (rh + ART_D - 1) / ART_D

    /** Every pixel run through [f] (ARGB in, ARGB out); clear pixels stay clear. */
    fun map(f: (Int) -> Int): Sprite = Sprite(rw, rh, IntArray(argb.size) { i -> argb[i].let { if (it ushr 24 == 0) it else f(it) } })

    /** Every pixel in one colour, keeping its alpha: a silhouette. */
    fun recolorAll(c: Color): Sprite {
        val rgb = c.toArgb() and 0xFFFFFF
        return map { (it and 0xFF000000.toInt()) or rgb }
    }

    /** [top] drawn over this sprite, its bottom centre at ([x], [y]) in this sprite's pixels. */
    fun under(top: Sprite, x: Int, y: Int): Sprite {
        val left = minOf(0, x - top.rw / 2)
        val upper = minOf(0, y - top.rh)
        val nw = maxOf(rw, x + (top.rw + 1) / 2) - left
        val nh = rh - upper
        val out = IntArray(nw * nh)
        for (j in 0 until rh) for (i in 0 until rw) out[(j - upper) * nw + (i - left)] = argb[j * rw + i]
        val tx = x - top.rw / 2 - left
        val ty = y - top.rh - upper
        for (j in 0 until top.rh) for (i in 0 until top.rw) {
            val c = top.argb[j * top.rw + i]
            val a = c ushr 24
            if (a == 0) continue
            val k = (ty + j) * nw + (tx + i)
            if (k < 0 || k >= out.size || tx + i !in 0 until nw) continue
            out[k] = if (a == 255) c else blend(out[k], c)
        }
        return Sprite(nw, nh, out)
    }

    /** Every colour pulled towards a cool grey: helpers that are still asleep. */
    fun muted(): Sprite = map { c ->
        val r = (c shr 16 and 0xFF) / 255f
        val g = (c shr 8 and 0xFF) / 255f
        val b = (c and 0xFF) / 255f
        val l = 0.3f * r + 0.59f * g + 0.11f * b
        Color(red = l * 0.75f + 0.08f, green = l * 0.72f + 0.08f, blue = l * 0.9f + 0.12f, alpha = (c ushr 24) / 255f).toArgb()
    }

    /** Pixels whose hue is in [hues] (degrees, wrapping) and saturation at least [minSat] repainted along [ramp] by lightness. */
    fun repaint(hues: ClosedFloatingPointRange<Float>, minSat: Float, ramp: List<Color>): Sprite {
        val hsv = FloatArray(3)
        val cols = ramp.map { it.toArgb() }
        return map { c ->
            android.graphics.Color.colorToHSV(c, hsv)
            val h = if (hues.start > hues.endInclusive) hsv[0] >= hues.start || hsv[0] <= hues.endInclusive else hsv[0] in hues
            if (!h || hsv[1] < minSat) c
            else {
                val t = (hsv[2] * (cols.size - 1)).coerceIn(0f, cols.size - 1f)
                val i = t.toInt().coerceAtMost(cols.size - 2)
                (c and 0xFF000000.toInt()) or (lerp(cols[i], cols[i + 1], t - i) and 0xFFFFFF)
            }
        }
    }

    companion object {
        private fun lerp(a: Int, b: Int, t: Float): Int {
            fun ch(s: Int) = ((a shr s and 0xFF) + ((b shr s and 0xFF) - (a shr s and 0xFF)) * t).toInt().coerceIn(0, 255)
            return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }

        private fun blend(under: Int, over: Int): Int {
            val a = (over ushr 24) / 255f
            val ua = (under ushr 24) / 255f
            val oa = a + ua * (1 - a)
            if (oa == 0f) return 0
            fun ch(s: Int) = (((over shr s and 0xFF) * a + (under shr s and 0xFF) * ua * (1 - a)) / oa).toInt().coerceIn(0, 255)
            return ((oa * 255).toInt() shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }
    }
}
