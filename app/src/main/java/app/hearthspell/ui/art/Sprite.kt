package app.hearthspell.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.max

/**
 * A pixel-art sprite: rows of characters, each a colour from [palette] ('.' and ' ' are transparent).
 * Drawn as crisp squares, so it stays sharp at any scale.
 */
@Immutable
class Sprite(val w: Int, val h: Int, private val cells: Array<Color?>) {
    operator fun get(x: Int, y: Int): Color? = cells[y * w + x]

    /** A copy with some colours swapped (palette swaps: a red imp, a sleepy grey helper). */
    fun recolor(map: Map<Color, Color>): Sprite = Sprite(w, h, Array(cells.size) { i -> cells[i]?.let { map[it] ?: it } })

    /** Every pixel in one colour: a silhouette. */
    fun recolorAll(c: Color): Sprite = Sprite(w, h, Array(cells.size) { i -> cells[i]?.let { c } })

    /** [top] drawn over this sprite; both must be the same size. */
    fun under(top: Sprite): Sprite {
        require(top.w == w && top.h == h)
        return Sprite(w, h, Array(cells.size) { i -> top.cells[i] ?: cells[i] })
    }

    /** Every colour multiplied towards grey: used for helpers that are still asleep. */
    fun muted(): Sprite = Sprite(w, h, Array(cells.size) { i ->
        cells[i]?.let { c ->
            val l = 0.3f * c.red + 0.59f * c.green + 0.11f * c.blue
            Color(red = l * 0.75f + 0.08f, green = l * 0.72f + 0.08f, blue = l * 0.9f + 0.12f, alpha = c.alpha)
        }
    })

    companion object {
        /** Builds a sprite from rows of equal length. Unknown characters throw, to catch typos early. */
        fun of(palette: Map<Char, Color>, vararg rows: String): Sprite {
            val w = rows.maxOf { it.length }
            val cells = Array<Color?>(w * rows.size) { null }
            rows.forEachIndexed { y, row ->
                row.forEachIndexed { x, ch ->
                    cells[y * w + x] = when (ch) {
                        '.', ' ' -> null
                        else -> palette[ch] ?: error("Sprite colour '$ch' is not in the palette")
                    }
                }
            }
            return Sprite(w, rows.size, cells)
        }
    }
}

/** Draws [s] with its top-left at [topLeft], each sprite pixel [px] screen pixels wide. */
fun DrawScope.drawSprite(s: Sprite, topLeft: Offset, px: Float, flipX: Boolean = false, alpha: Float = 1f) {
    // Overlap each square by a hair so no seams show between pixels at fractional scales.
    val size = Size(px + 0.5f, px + 0.5f)
    for (y in 0 until s.h) for (x in 0 until s.w) {
        val c = s[x, y] ?: continue
        val sx = if (flipX) s.w - 1 - x else x
        drawRect(c, Offset(topLeft.x + sx * px, topLeft.y + y * px), size, alpha = alpha * c.alpha)
    }
}

/** The largest whole number of screen pixels per sprite pixel that fits [s] in the box (at least 1). */
fun pixelScale(s: Sprite, width: Float, height: Float): Float = max(1f, floor(minOf(width / s.w, height / s.h)))

/** A sprite as a composable, centred in [size], scaled by whole pixels. */
@Composable
fun SpriteImage(s: Sprite, size: Dp, modifier: Modifier = Modifier, flipX: Boolean = false, alpha: Float = 1f) {
    Canvas(modifier.size(size)) {
        val px = pixelScale(s, this.size.width, this.size.height)
        val left = floor((this.size.width - s.w * px) / 2)
        val top = floor((this.size.height - s.h * px) / 2)
        drawSprite(s, Offset(left, top), px, flipX, alpha)
    }
}

/** A sprite drawn at a fixed number of dp per pixel, e.g. 3.dp. */
@Composable
fun SpriteAt(s: Sprite, pixel: Dp = 3.dp, modifier: Modifier = Modifier, flipX: Boolean = false, alpha: Float = 1f) {
    Canvas(modifier.size(pixel * s.w, pixel * s.h)) {
        drawSprite(s, Offset.Zero, this.size.width / s.w, flipX, alpha)
    }
}
