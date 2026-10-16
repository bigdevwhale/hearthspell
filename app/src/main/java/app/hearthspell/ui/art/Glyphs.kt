package app.hearthspell.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor

/** A one-colour pixel icon for the UI ('x' is ink, anything else is empty), tinted when drawn. */
@Immutable
class Glyph(vararg val rows: String) {
    val w = rows.maxOf { it.length }
    val h = rows.size
}

@Composable
fun PixelIcon(g: Glyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
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
}
