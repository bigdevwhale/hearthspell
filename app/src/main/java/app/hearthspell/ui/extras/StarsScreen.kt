package app.hearthspell.ui.extras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.content.Constellation
import app.hearthspell.content.Stars
import app.hearthspell.ui.Go
import app.hearthspell.ui.WData
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelBar
import app.hearthspell.ui.components.SectionTitle
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.gs
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.Nes
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import app.hearthspell.ui.tips.Tip
import app.hearthspell.ui.tower.hash
import app.hearthspell.ui.tower.rememberTicker
import kotlin.math.abs
import kotlin.math.floor

private val Night = Color(0xFF0B0920)
private val NightLow = Color(0xFF211A4A)
private val Star = Color(0xFFFFF4D6)
private val StarDim = Color(0xFF5B4BA8)
private val Line = Color(0xFF9B8AE0)

/** The star chart over the Observatory: every constellation, lit as spells climb their ranks. */
@Composable
fun StarsScreen(data: WData, go: Go) {
    val w = data.world
    val lit = w.starsLit
    val tick = rememberTicker(200)
    Column(Modifier.fillMaxSize()) {
        TopBar(gs(R.string.stars_title), go::back, Tip.STARS)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().aspectRatio(128f / 72f).pixelBox(Night, U.c.outline)) {
                Canvas(Modifier.fillMaxSize().padding(3.dp)) {
                    val px = floor(minOf(size.width / 128f, size.height / 72f))
                    val ox = (size.width - 128 * px) / 2
                    val oy = (size.height - 72 * px) / 2
                    // A dim sky full of background stars.
                    for (k in 0 until 90) {
                        val h = hash(3, k.toLong())
                        dot(ox, oy, px, h % 128, h / 128 % 72, StarDim, 0.6f)
                    }
                    Stars.all.forEachIndexed { i, c -> drawConstellation(c, Stars.litIn(i, lit), ox, oy, px, tick.value + i * 5) }
                }
            }
            Gap(10.dp)
            Text(gs(R.string.stars_count, lit, Stars.total), style = UType.Sub)
            Gap(6.dp)
            PixelBar(lit / Stars.total.toFloat(), Modifier.fillMaxWidth(), color = U.c.accent, segments = 12)
            Gap(6.dp)
            Text(gs(R.string.stars_how), style = UType.Muted)

            SectionTitle(gs(R.string.stars_list))
            Stars.all.forEachIndexed { i, c ->
                val n = Stars.litIn(i, lit)
                val done = n == c.stars.size
                Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp), fill = if (done) U.c.surface2 else U.c.surface) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(84.dp, 50.dp).pixelBox(Night, U.c.outline)) {
                            ConstellationArt(c, n, Modifier.fillMaxSize().padding(3.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (n > 0) t(c.title) else "???", style = UType.Strong.copy(color = if (done) U.c.accent else U.c.ink))
                            Text("$n / ${c.stars.size}", style = UType.Small)
                            if (done) {
                                Gap(4.dp)
                                Text(t(c.line), style = UType.Muted)
                            }
                        }
                    }
                }
            }
            Gap(24.dp)
        }
    }
}

private fun DrawScope.dot(ox: Float, oy: Float, px: Float, x: Int, y: Int, c: Color, alpha: Float = 1f) =
    drawRect(c, Offset(ox + x * px, oy + y * px), Size(px + 0.5f, px + 0.5f), alpha = alpha)

private fun DrawScope.drawConstellation(c: Constellation, lit: Int, ox: Float, oy: Float, px: Float, tick: Long) {
    c.lines.forEach { (a, b) ->
        val on = a < lit && b < lit
        val (x0, y0) = c.stars[a]
        val (x1, y1) = c.stars[b]
        val steps = maxOf(abs(x1 - x0), abs(y1 - y0)).coerceAtLeast(1)
        for (k in 0..steps) {
            if (!on && k % 2 == 1) continue
            dot(ox, oy, px, x0 + (x1 - x0) * k / steps, y0 + (y1 - y0) * k / steps, Line, if (on) 0.5f else 0.12f)
        }
    }
    c.stars.forEachIndexed { i, (x, y) ->
        if (i < lit) {
            val twinkle = (tick / 2 + i * 7) % 23 == 0L
            dot(ox, oy, px, x, y, Star)
            if (twinkle) listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).forEach { (dx, dy) -> dot(ox, oy, px, x + dx, y + dy, Star, 0.6f) }
            else listOf(-1 to 0, 1 to 0).forEach { (dx, dy) -> dot(ox, oy, px, x + dx, y + dy, Nes.yellow, 0.25f) }
        } else {
            dot(ox, oy, px, x, y, StarDim, 0.7f)
        }
    }
}

/** One constellation alone, cropped to its stars, for lists and dialogs. */
@Composable
fun ConstellationArt(c: Constellation, lit: Int, modifier: Modifier) {
    val tick = rememberTicker(200)
    Canvas(modifier) {
        val minX = c.stars.minOf { it.first } - 2
        val minY = c.stars.minOf { it.second } - 2
        val w = c.stars.maxOf { it.first } - minX + 3
        val h = c.stars.maxOf { it.second } - minY + 3
        val px = maxOf(1f, floor(minOf(size.width / w, size.height / h)))
        val ox = (size.width - w * px) / 2 - minX * px
        val oy = (size.height - h * px) / 2 - minY * px
        drawConstellation(c, lit, ox, oy, px, tick.value)
    }
}
