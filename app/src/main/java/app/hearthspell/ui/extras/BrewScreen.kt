package app.hearthspell.ui.extras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.Aisle
import app.hearthspell.content.DecorKind
import app.hearthspell.content.Potion
import app.hearthspell.data.brew
import app.hearthspell.ui.Go
import app.hearthspell.ui.LocalFeel
import app.hearthspell.ui.Tone
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.ui.art.drawPixels
import app.hearthspell.ui.components.ButtonKind
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelButton
import app.hearthspell.ui.components.SectionTitle
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.gp
import app.hearthspell.ui.gs
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import app.hearthspell.ui.tips.Tip
import app.hearthspell.ui.tower.rememberTicker
import kotlinx.coroutines.delay
import kotlin.math.floor

/** Each shop section's essence has its own colour on the shelf. */
internal fun essenceColor(a: Aisle): Color = Color(
    when (a) {
        Aisle.PRODUCE -> 0xFF7DDB5A
        Aisle.BAKERY -> 0xFFE8C88A
        Aisle.DAIRY -> 0xFFF4F0E6
        Aisle.MEAT -> 0xFFD8584A
        Aisle.PANTRY -> 0xFFC88440
        Aisle.FROZEN -> 0xFF9CE4FF
        Aisle.DRINKS -> 0xFF5FD6FF
        Aisle.SWEETS -> 0xFFF878B8
        Aisle.HOUSEHOLD -> 0xFF46B8BC
        Aisle.CARE -> 0xFFB062E0
        Aisle.PETS -> 0xFFFCB040
        Aisle.OTHER -> 0xFFA8A8B8
    }
)

/** The cauldron in the Alchemy Lab: essences from the market brew into potions that make the tower prettier. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrewScreen(data: WData, go: Go) {
    val w = data.world
    val container = LocalContext.current.container
    val feel = LocalFeel.current
    var brewing by remember { mutableStateOf<Potion?>(null) }
    LaunchedEffect(brewing) {
        if (brewing != null) {
            delay(2400)
            brewing = null
        }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar(gs(R.string.brew_title), go::back, Tip.BREW)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Cauldron(brewing)
            Gap(10.dp)
            Text(gs(R.string.brew_sub), style = UType.Muted)

            SectionTitle(gs(R.string.brew_shelf))
            val shelf = Aisle.entries.filter { it != Aisle.OTHER }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shelf.forEach { a -> Essence(a, w.essences[a] ?: 0) }
            }

            val active = w.potions
            if (active.isNotEmpty()) {
                SectionTitle(gs(R.string.brew_active))
                active.forEach { p ->
                    Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp), fill = U.c.surface2) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Flask(Color(p.color.toInt()), 30.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t(p.title), style = UType.Strong)
                                Text(t(p.effect), style = UType.Muted)
                            }
                            Text(gp(R.plurals.brew_days_left, w.potionDaysLeft(p), w.potionDaysLeft(p)), style = UType.Small.copy(color = U.c.mana))
                        }
                    }
                }
            }

            SectionTitle(gs(R.string.brew_recipes))
            Potion.entries.forEach { p ->
                val can = w.canBrew(p)
                Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Flask(Color(p.color.toInt()), 34.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t(p.title), style = UType.Strong)
                            Text(t(p.effect), style = UType.Muted)
                        }
                    }
                    Gap(10.dp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        p.needs.forEach { (a, n) ->
                            val have = w.essences[a] ?: 0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).pixelBox(essenceColor(a), U.c.outline))
                                Spacer(Modifier.width(5.dp))
                                Text("${t(a.title)} $have/$n", style = UType.Small.copy(color = if (have >= n) U.c.good else U.c.faint))
                            }
                        }
                        p.trinket?.let { tr ->
                            val have = w.trinketsLeft(tr)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SpriteImage(Art.trinket(tr.name), 16.dp)
                                Spacer(Modifier.width(4.dp))
                                Text("${t(tr.title)} $have/1", style = UType.Small.copy(color = if (have >= 1) U.c.good else U.c.faint))
                            }
                        }
                    }
                    Gap(10.dp)
                    PixelButton(
                        if (w.potionDaysLeft(p) > 0) gs(R.string.brew_again) else gs(R.string.brew_do),
                        {
                            container.write { brew(p, w) }
                            feel?.tone(Tone.BREW, 0.5f)
                            brewing = p
                        },
                        Modifier.fillMaxWidth(), kind = ButtonKind.MANA, enabled = can,
                    )
                }
            }
            Gap(24.dp)
        }
    }
}

@Composable
private fun Essence(a: Aisle, n: Int) {
    Row(
        Modifier.pixelBox(U.c.surface2, U.c.outline, U.c.bevel).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Flask(essenceColor(a), 18.dp, dim = n == 0)
        Spacer(Modifier.width(6.dp))
        Text("${t(a.title)} · $n", style = UType.Small.copy(color = if (n > 0) U.c.ink else U.c.faint))
    }
}

/** A round little flask of [color], drawn in pixels. */
@Composable
internal fun Flask(color: Color, size: androidx.compose.ui.unit.Dp, dim: Boolean = false) {
    val rows = listOf("..##..", "..##..", ".#..#.", "#....#", "#....#", ".####.")
    Canvas(Modifier.size(size)) {
        val px = floor(this.size.width / 6)
        val ox = (this.size.width - px * 6) / 2
        val a = if (dim) 0.35f else 1f
        rows.forEachIndexed { y, r ->
            r.forEachIndexed { x, ch ->
                val c = when {
                    ch == '#' -> Color(0xFF07060F)
                    y >= 3 && x in 1..4 -> color
                    y == 2 && x in 2..3 -> color.copy(alpha = 0.5f)
                    else -> null
                }
                if (c != null) drawRect(c, Offset(ox + x * px, y * px), Size(px + 0.5f, px + 0.5f), alpha = a)
            }
        }
        drawRect(Color.White, Offset(ox + 2 * px, 3 * px), Size(px, px), alpha = 0.6f * a)
    }
}

/** The big cauldron, bubbling harder (and in the potion's colour) while something brews. */
@Composable
private fun Cauldron(brewing: Potion?) {
    val tick = rememberTicker(160)
    Box(Modifier.fillMaxWidth().height(150.dp).pixelBox(Color(0xFF14112B), U.c.outline), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = Art.decor(DecorKind.CAULDRON, ((tick.value / 3) % 2).toInt())
            val px = floor(minOf(size.width / (s.w + 8), size.height / (s.h + 16)))
            val ox = (size.width - s.w * px) / 2
            val oy = size.height - (s.h + 4) * px
            drawPixels(s, ox, oy, px)
            val col = brewing?.let { Color(it.color.toInt()) } ?: Color(0xFF7DDB5A)
            val n = if (brewing != null) 10 else 4
            for (k in 0 until n) {
                val life = ((tick.value * (if (brewing != null) 2 else 1) + k * 9) % 30).toInt()
                val x = ox + (8 + (k * 5) % 16) * px + kotlin.math.sin((tick.value + k * 7) / 4.0).toFloat() * px
                val y = oy + 6 * px - life * px * 0.9f
                val r = px * (1 + (k % 2))
                drawRect(col, Offset(x, y), Size(r, r), alpha = (1f - life / 30f))
            }
        }
        if (brewing != null) Text(gs(R.string.brew_done, t(brewing.title)), style = UType.Heading.copy(color = Color(brewing.color.toInt())), modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
    }
}
