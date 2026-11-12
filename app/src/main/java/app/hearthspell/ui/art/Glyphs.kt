package app.hearthspell.ui.art

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Share
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor

/** A one-colour pixel icon for the UI ('x' is ink, anything else is empty), tinted when drawn. */
@Immutable
class Glyph(vararg val rows: String) {
    val w = rows.maxOf { it.length }
    val h = rows.size
}

/** Icons that have a full-colour picture (see [Art.icon]), by glyph. */
private val pictured: Map<Glyph, String> by lazy {
    mapOf(
        Glyphs.tower to "tower", Glyphs.book to "book", Glyphs.basket to "basket", Glyphs.letter to "letter",
        Glyphs.gear to "gear", Glyphs.question to "question", Glyphs.chest to "chest", Glyphs.pencil to "pencil",
        Glyphs.flask to "flask", Glyphs.star to "star", Glyphs.mana to "mana", Glyphs.imp to "imp",
        Glyphs.chronicle to "stats",
    )
}

/** The small functional icons, as smooth vectors. */
private val vectors: Map<Glyph, androidx.compose.ui.graphics.vector.ImageVector> by lazy {
    mapOf(
        Glyphs.back to Icons.AutoMirrored.Rounded.ArrowBack,
        Glyphs.plus to Icons.Rounded.Add,
        Glyphs.check to Icons.Rounded.Check,
        Glyphs.close to Icons.Rounded.Close,
        Glyphs.trash to Icons.Rounded.DeleteOutline,
        Glyphs.up to Icons.Rounded.KeyboardArrowUp,
        Glyphs.down to Icons.Rounded.KeyboardArrowDown,
        Glyphs.moon to Icons.Rounded.Bedtime,
        Glyphs.bolt to Icons.Rounded.Bolt,
        Glyphs.share to Icons.Rounded.Share,
    )
}

private val greyed = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.15f) })

/**
 * A UI icon. Icons with a drawn picture show it in full colour ([muted] greys it out, e.g. an unselected tab), and
 * take only [tint]'s alpha; the rest are one-colour pixel glyphs in [tint].
 */
@Composable
fun PixelIcon(g: Glyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp, muted: Boolean = false) {
    val art = pictured[g]?.let { Art.icon(it) }
    if (art != null) {
        Canvas(modifier.size(size)) {
            val k = minOf(this.size.width / art.rw, this.size.height / art.rh) * 1.15f
            val w = art.rw * k
            val h = art.rh * k
            drawImage(
                art.bitmap(), IntOffset.Zero, IntSize(art.rw, art.rh),
                IntOffset(((this.size.width - w) / 2).toInt(), ((this.size.height - h) / 2).toInt()), IntSize(w.toInt(), h.toInt()),
                alpha = tint.alpha * (if (muted) 0.6f else 1f), colorFilter = if (muted) greyed else null, filterQuality = FilterQuality.Low,
            )
        }
        return
    }
    vectors[g]?.let { v ->
        androidx.compose.material3.Icon(v, null, modifier.size(size), tint = tint)
        return
    }
    Canvas(modifier.size(size)) {
        val px = maxOf(1f, floor(minOf(this.size.width / g.w, this.size.height / g.h)))
        val left = floor((this.size.width - g.w * px) / 2)
        val top = floor((this.size.height - g.h * px) / 2)
        val cell = Size(px + 0.5f, px + 0.5f)
        g.rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch -> if (ch == 'x') drawRect(tint, Offset(left + x * px, top + y * px), cell) }
        }
    }
}

/** The UI icon set, 9x9 unless noted. */
object Glyphs {
    /** Share: three linked dots. */
    val share = Glyph(
        "......xx.",
        "....xxxx.",
        "..xx..xx.",
        "xx.......",
        "xx.......",
        "..xx..xx.",
        "....xxxx.",
        "......xx.",
        ".........",
    )
    /** The chronicle tab: a little bar chart. */
    val chronicle = Glyph(
        ".........",
        ".......x.",
        ".......x.",
        "....x..x.",
        "....x..x.",
        ".x..x..x.",
        ".x..x..x.",
        ".x..x..x.",
        "xxxxxxxxx",
    )

    val tower = Glyph(
        "...x.x...",
        "..xxxxx..",
        "..x...x..",
        "..x.x.x..",
        "..x...x..",
        ".xxxxxxx.",
        ".x..x..x.",
        ".x.xxx.x.",
        ".xxx.xxx.",
    )
    val book = Glyph(
        "xxxx.xxxx",
        "x...x...x",
        "x.x.x.x.x",
        "x...x...x",
        "x.x.x.x.x",
        "x...x...x",
        "x...x...x",
        "xxxx.xxxx",
        "....x....",
    )
    val basket = Glyph(
        "...xxx...",
        "..x...x..",
        ".x.....x.",
        "xxxxxxxxx",
        "x.x.x.x.x",
        "xx.x.x.xx",
        ".x.x.x.x.",
        ".xx.x.xx.",
        "..xxxxx..",
    )
    val letter = Glyph(
        ".........",
        "xxxxxxxxx",
        "xx.....xx",
        "x.x...x.x",
        "x..x.x..x",
        "x...x...x",
        "x.......x",
        "xxxxxxxxx",
        ".........",
    )
    val gear = Glyph(
        "...x.x...",
        ".x.xxx.x.",
        "..xxxxx..",
        "xxx...xxx",
        ".xx...xx.",
        "xxx...xxx",
        "..xxxxx..",
        ".x.xxx.x.",
        "...x.x...",
    )
    val back = Glyph(
        "...x.....",
        "..xx.....",
        ".xxxxxxxx",
        "xxxxxxxxx",
        ".xxxxxxxx",
        "..xx.....",
        "...x.....",
    )
    val plus = Glyph(
        "...xxx...",
        "...xxx...",
        "...xxx...",
        "xxxxxxxxx",
        "xxxxxxxxx",
        "xxxxxxxxx",
        "...xxx...",
        "...xxx...",
        "...xxx...",
    )
    val check = Glyph(
        "........x",
        ".......xx",
        "......xx.",
        "x....xx..",
        "xx..xx...",
        ".xxxx....",
        "..xx.....",
    )
    val close = Glyph(
        "xx.....xx",
        ".xx...xx.",
        "..xx.xx..",
        "...xxx...",
        "..xx.xx..",
        ".xx...xx.",
        "xx.....xx",
    )
    val mana = Glyph(
        "....x....",
        "...xxx...",
        "...xxx...",
        "..xxxxx..",
        ".xxxxxxx.",
        ".xx.xxxx.",
        ".xx.xxxx.",
        "..xxxxx..",
        "...xxx...",
    )
    val star = Glyph(
        "....x....",
        "....x....",
        "...xxx...",
        "xxxxxxxxx",
        ".xxxxxxx.",
        "..xxxxx..",
        "..xx.xx..",
        ".xx...xx.",
        ".x.....x.",
    )
    val pencil = Glyph(
        "......xx.",
        ".....xxxx",
        "....x.xx.",
        "...x.xx..",
        "..x.xx...",
        ".x.xx....",
        "xxxx.....",
        "xxx......",
    )
    val trash = Glyph(
        "...xxx...",
        "xxxxxxxxx",
        ".x.....x.",
        ".x.x.x.x.",
        ".x.x.x.x.",
        ".x.x.x.x.",
        ".x.....x.",
        "..xxxxx..",
    )
    val up = Glyph(
        "....x....",
        "...xxx...",
        "..xxxxx..",
        ".xxxxxxx.",
        "...xxx...",
        "...xxx...",
        "...xxx...",
    )
    val down = Glyph(
        "...xxx...",
        "...xxx...",
        "...xxx...",
        ".xxxxxxx.",
        "..xxxxx..",
        "...xxx...",
        "....x....",
    )
    val moon = Glyph(
        "..xxxx...",
        ".xxx.....",
        "xxx......",
        "xxx......",
        "xxx......",
        "xxxx...x.",
        ".xxxxxxx.",
        "..xxxxx..",
    )
    val chest = Glyph(
        ".xxxxxxx.",
        "x.......x",
        "xxxxxxxxx",
        "x...x...x",
        "x..xxx..x",
        "x...x...x",
        "xxxxxxxxx",
    )
    val bolt = Glyph(
        ".....xx..",
        "....xx...",
        "...xx....",
        "..xxxxx..",
        "....xx...",
        "...xx....",
        "..xx.....",
        ".x.......",
    )
    val imp = Glyph(
        "x.......x",
        "xx.....xx",
        ".xxxxxxx.",
        ".x.xxx.x.",
        ".xxxxxxx.",
        "..x.x.x..",
        "...xxx...",
    )
    val question = Glyph(
        "..xxxxx..",
        ".xx...xx.",
        ".xx...xx.",
        ".....xx..",
        "....xx...",
        "....xx...",
        ".........",
        "....xx...",
        "....xx...",
    )
    val flask = Glyph(
        "...xxx...",
        "....x....",
        "...xxx...",
        "...x.x...",
        "..x...x..",
        ".x.....x.",
        "x.xxxxx.x",
        "x.xxxxx.x",
        ".xxxxxxx.",
    )
    val hat = Glyph(
        "....x....",
        "...xx....",
        "...xxx...",
        "..xxxx...",
        "..xxxxx..",
        ".xxxxxx..",
        ".xxxxxxx.",
        "xxxxxxxxx",
        ".........",
    )
}
