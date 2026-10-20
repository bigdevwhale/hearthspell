package app.hearthspell.ui.extras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.HelperKind
import app.hearthspell.ui.Go
import app.hearthspell.ui.LocalFeel
import app.hearthspell.ui.Tone
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Looks
import app.hearthspell.ui.art.drawPixels
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelButton
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.floorName
import app.hearthspell.ui.gp
import app.hearthspell.ui.gs
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import app.hearthspell.ui.tower.hash
import app.hearthspell.ui.tower.rememberTicker
import kotlinx.coroutines.launch
import kotlin.math.floor

private const val TW = 128
private const val TH = 64
private const val TFLOOR = 58

/** Sunday tea: the helpers gather round the table and the week is told in a few warm lines. No charts. */
@Composable
fun TeaScreen(data: WData, go: Go) {
    val w = data.world
    val s = data.settings
    val container = LocalContext.current.container
    val feel = LocalFeel.current
    val week = w.teaWeek ?: w.weekStart.minusWeeks(1)
    val r = w.teaReport(week)
    val tick = rememberTicker(300)
    Column(Modifier.fillMaxSize()) {
        TopBar(gs(R.string.tea_title), go::back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(TW / TH.toFloat()).pixelBox(Color(0xFF14112B), U.c.outline)) {
                val px = floor(minOf(size.width / TW, size.height / TH))
                val ox = (size.width - TW * px) / 2
                val oy = (size.height - TH * px) / 2
                fun rect(x: Int, y: Int, ww: Int, hh: Int, c: Color, a: Float = 1f) =
                    drawRect(c, Offset(ox + x * px, oy + y * px), Size(ww * px + 0.5f, hh * px + 0.5f), alpha = a)
                val t = tick.value
                // A warm wall, a window with the night outside, floorboards.
                rect(0, 0, TW, TFLOOR, Color(0xFF3A2A4A))
                for (y in 0 until TFLOOR step 6) for (x in 0 until TW step 10) rect(x + (y / 6 % 2) * 5, y, 1, 6, Color(0xFF2E2240))
                rect(52, 6, 24, 18, Color(0xFF0B0920))
                for (k in 0 until 6) {
                    val h = hash(9, k.toLong())
                    rect(53 + h % 22, 7 + h / 22 % 16, 1, 1, Color(0xFFFFF4D6), if ((t / 2 + k) % 9 == 0L) 0.3f else 1f)
                }
                rect(63, 6, 2, 18, Color(0xFF7A422E))
                rect(52, 14, 24, 2, Color(0xFF7A422E))
                rect(50, 24, 28, 2, Color(0xFFA2623A))
                rect(0, TFLOOR, TW, TH - TFLOOR, Color(0xFF7A422E))
                rect(0, TFLOOR, TW, 1, Color(0xFFCA8C52))
                // A warm glow over the table.
                for (k in 4 downTo 1) rect(64 - k * 12, 30 - k * 4, k * 24, k * 8, Color(0xFFFFDA62), 0.035f)
                val table = Art.teaTable
                val tx = (TW - table.w) / 2
                val ty = TFLOOR - table.h + 1
                // Helpers behind the table, bobbing.
                val behind = listOf(26, 52, 76)
                r.helpers.take(3).forEachIndexed { i, k ->
                    val sp = Art.helper(k, ((t / 2 + i) % 4).toInt())
                    drawPixels(sp, ox + (behind[i] + 13 - sp.w / 2) * px, oy + (ty + 14 - sp.h) * px, px)
                }
                drawPixels(table, ox + tx * px, oy + ty * px, px)
                // The teapot always comes to tea, wide awake.
                val pot = Art.helper(HelperKind.TEAPOT, ((t / 3) % 4).toInt())
                if (HelperKind.TEAPOT !in r.helpers.take(3)) drawPixels(pot, ox + (tx + 54) * px, oy + (ty + 8 - pot.h) * px, px * 0.75f)
                // Steam from the cups.
                for (k in 0..3) {
                    val life = ((t + k * 5) % 16).toInt()
                    rect(tx + listOf(14, 30, 52, 66)[k] + (if (life % 4 < 2) 0 else 1), ty + 2 - life / 2, 1, 1, Color(0xFFE8ECF4), 1f - life / 16f)
                }
                // The apprentice on the left, a fourth helper on the right, the cat dozing in front.
                val ap = Looks.apprentice(s.robe, s.hat, (t / 3 % 2).toInt())
                drawPixels(ap, ox + 2 * px, oy + (TFLOOR + 2 - ap.h) * px, px)
                r.helpers.getOrNull(3)?.let { k ->
                    val sp = Art.helper(k, ((t / 2) % 4).toInt())
                    drawPixels(sp, ox + (TW - sp.w - 2) * px, oy + (TFLOOR + 2 - sp.h) * px, px, flipX = true)
                }
                if (s.catName.isNotBlank()) {
                    val cat = Art.catAsleep((t / 4 % 2).toInt())
                    drawPixels(cat, ox + (TW / 2 - cat.w / 2) * px, oy + (TH - cat.h) * px, px)
                }
            }
            Gap(12.dp)
            Panel(Modifier.fillMaxWidth()) {
                Text(gs(R.string.tea_week, week.dayOfMonth, week.plusDays(6).dayOfMonth), style = UType.Eyebrow.copy(color = U.c.accent))
                Gap(10.dp)
                Line(gp(R.plurals.tea_casts, r.casts, r.casts))
                r.helpers.firstOrNull()?.let { Line(gs(R.string.tea_helper, t(it.title))) }
                r.floorId?.let { id -> w.floor(id)?.let { Line(gs(R.string.tea_floor, floorName(it))) } }
                Line(if (r.impsBanished > 0) gp(R.plurals.tea_imps, r.impsBanished, r.impsBanished) else gs(R.string.tea_no_imps))
                if (r.gifts.isNotEmpty()) Line(gs(R.string.tea_gifts, r.gifts.distinct().map { t(it.title).replaceFirstChar { c -> c.lowercaseChar() } }.joinToString(", ")))
                if (s.catName.isNotBlank()) Line(gs(R.string.tea_cat, s.catName))
                Gap(6.dp)
                val closings = listOf(R.string.tea_close_1, R.string.tea_close_2, R.string.tea_close_3, R.string.tea_close_4)
                Text(gs(closings[(week.toEpochDay() / 7 % closings.size).toInt()]), style = UType.Body.copy(color = U.c.muted))
            }
            Gap(14.dp)
            PixelButton(gs(R.string.tea_clink), {
                feel?.tone(Tone.CLINK, 0.3f)
                container.scope.launch { container.settings.update { it.copy(seen = it.seen + "tea:${week.toEpochDay()}") } }
                go.back()
            }, Modifier.fillMaxWidth())
            Gap(24.dp)
        }
    }
}

@Composable
private fun Line(text: String) {
    Text("· $text", style = UType.Body, modifier = Modifier.padding(bottom = 6.dp))
}
