package app.hearthspell.ui.grimoire

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.content.DecorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.content.Spells
import app.hearthspell.data.Spell
import app.hearthspell.domain.HelperState
import app.hearthspell.domain.Mana
import app.hearthspell.domain.Rank
import app.hearthspell.domain.World
import app.hearthspell.ui.Go
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.PixelIcon
import app.hearthspell.ui.art.Sprite
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.ui.components.ButtonKind
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.ManaTag
import app.hearthspell.ui.components.PX
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelBar
import app.hearthspell.ui.components.PixelButton
import app.hearthspell.ui.components.SectionTitle
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.floorName
import app.hearthspell.ui.gp
import app.hearthspell.ui.gs
import app.hearthspell.ui.spellName
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate

// The book of everything learned: the apprentice's circle, the helpers, and a page for every spell.

@Composable
fun GrimoireTab(data: WData, go: Go) {
    val w = data.world
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.grimoire_title), null) { ManaTag(w.mana, Modifier.padding(end = 4.dp)) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            CirclePanel(w)
            Gap(10.dp)
            Panel(Modifier.fillMaxWidth(), onClick = { go.decor(0) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SpriteImage(Art.decor(DecorKind.CHEST), 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.grimoire_decor), style = UType.Heading)
                        Text(stringResource(R.string.grimoire_decor_sub), style = UType.Muted)
                    }
                    PixelIcon(Glyphs.back, U.c.faint, Modifier.graphicsLayer { scaleX = -1f }, size = 16.dp)
                }
            }

            val awake = w.helpers.count { it.awake }
            SectionTitle(stringResource(R.string.grimoire_helpers) + " · " + stringResource(R.string.grimoire_helpers_awake, awake, w.helpers.size))
            HelperGrid(w.helpers) { go.helper(it.name) }

            SectionTitle(stringResource(R.string.grimoire_spells))
            if (w.spells.isEmpty()) {
                Text(gs(R.string.grimoire_empty), style = UType.Sub)
            }
            w.floors.forEach { f ->
                val spells = w.spellsOf(f.id)
                if (spells.isEmpty()) return@forEach
                Text(floorName(f), style = UType.Eyebrow.copy(color = U.c.accent), modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
                spells.forEach { s ->
                    SpellRow(s, w) { go.spell(s.id) }
                    Gap(8.dp)
                }
            }
            Gap(24.dp)
        }
    }
}

@Composable
private fun CirclePanel(w: World) {
    val circle = w.circle
    val from = Mana.circles.getOrElse(circle - 1) { 0 }
    val to = Mana.circles.getOrNull(circle)
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedSprite({ Art.apprentice(it) }, 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.grimoire_circle, circle).uppercase(), style = UType.Title.copy(color = U.c.accent))
                Gap(4.dp)
                Text(gp(R.plurals.grimoire_total, w.totalCasts, w.totalCasts), style = UType.Sub)
            }
        }
        Gap(12.dp)
        if (to != null) {
            PixelBar((w.totalCasts - from).toFloat() / (to - from), Modifier.fillMaxWidth(), color = U.c.accent)
            Gap(6.dp)
            Text(gp(R.plurals.grimoire_circle_next, to - w.totalCasts, to - w.totalCasts, circle + 1), style = UType.Muted)
        } else {
            PixelBar(1f, Modifier.fillMaxWidth())
            Gap(6.dp)
            Text(stringResource(R.string.grimoire_circle_max), style = UType.Muted)
        }
    }
}

@Composable
private fun HelperGrid(helpers: List<HelperState>, onClick: (HelperKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        helpers.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { h -> HelperCell(h, Modifier.weight(1f)) { onClick(h.kind) } }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HelperCell(h: HelperState, modifier: Modifier, onClick: () -> Unit) {
    val c = U.c
    Column(
        modifier
            .pixelBox(if (h.awake) c.surface2 else c.surface, c.outline, c.bevel)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (h.awake) AnimatedSprite({ Art.helper(h.kind, it) }, 44.dp)
        else SpriteImage(Art.helper(h.kind).muted(), 44.dp, alpha = 0.7f)
        Gap(6.dp)
        Text(t(h.kind.title), style = UType.Small.copy(color = if (h.awake) c.ink else c.faint), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Gap(4.dp)
        LevelPips(h.level)
    }
}

/** Four little squares, lit up to the helper's level. */
@Composable
internal fun LevelPips(level: Int, size: Dp = 6.dp) {
    val c = U.c
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(HelperState.HELPER_LEVELS.size) { i ->
            Box(Modifier.size(size).background(if (i < level) c.accent else c.outline.copy(alpha = 0.45f)))
        }
    }
}

@Composable
private fun SpellRow(s: Spell, w: World, onClick: () -> Unit) {
    val count = w.castCount(s.id)
    val rank = Rank.of(count)
    Panel(Modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SpriteImage(Art.helper(HelperKind.of(s.helper)).let { if (w.helper(HelperKind.of(s.helper)).awake) it else it.muted() }, 34.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(spellName(s), style = UType.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t(rank.title), style = UType.Small.copy(color = U.c.accent))
                    Text("  ·  " + gp(R.plurals.grimoire_casts, count, count), style = UType.Small)
                }
                Gap(6.dp)
                RankBar(count, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun RankBar(count: Int, modifier: Modifier = Modifier) {
    val rank = Rank.of(count)
    val next = rank.next
    val f = if (next == null) 1f else (count - rank.casts).toFloat() / (next.casts - rank.casts)
    PixelBar(f, modifier, color = U.c.accent, segments = 8, height = 10.dp)
}

/** Flips between a sprite's two frames, for idle animation. */
@Composable
internal fun AnimatedSprite(frame: (Int) -> Sprite, size: Dp, periodMs: Long = 520) {
    var f by remember { mutableIntStateOf(0) }
    LaunchedEffect(periodMs) {
        while (true) {
            delay(periodMs)
            f = 1 - f
        }
    }
    SpriteImage(frame(f), size)
}

// A spell's page.

@Composable
fun SpellScreen(id: Long, data: WData, go: Go) {
    val w = data.world
    val s = w.spell(id)
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.grimoire_title), go::back)
        if (s == null) {
            Text(stringResource(R.string.spell_missing), style = UType.Sub, modifier = Modifier.padding(16.dp))
            return@Column
        }
        val count = w.castCount(s.id)
        val rank = Rank.of(count)
        val helper = w.helper(HelperKind.of(s.helper))
        val preset = Spells.of(s.presetKey)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            // The page itself: parchment in both themes, like a real book.
            Column(
                Modifier.fillMaxWidth().pixelBox(Parchment, U.c.outline, Color(0xFFFFFBEF), Color(0xFFD9C08E)).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(spellName(s), style = UType.Strong.copy(color = Ink), textAlign = TextAlign.Center)
                Gap(4.dp)
                Text(w.floor(s.floorId)?.let { floorName(it) }.orEmpty().uppercase(), style = UType.Eyebrow.copy(color = InkFaint))
                Gap(14.dp)
                Box(Modifier.clickable { go.helper(helper.kind.name) }) {
                    if (helper.awake) AnimatedSprite({ Art.helper(helper.kind, it) }, 84.dp)
                    else SpriteImage(Art.helper(helper.kind).muted(), 84.dp)
                }
                Gap(14.dp)
                Text(stringResource(R.string.spell_chant).uppercase(), style = UType.Eyebrow.copy(color = InkFaint))
                Gap(6.dp)
                Text(
                    preset?.let { t(it.chant) } ?: gs(R.string.spell_own),
                    style = UType.Body.copy(color = Ink, fontStyle = FontStyle.Italic),
                    textAlign = TextAlign.Center,
                )
                Gap(14.dp)
                Text(t(rank.title).uppercase(), style = UType.Heading.copy(color = Color(0xFFA9500F)))
                Gap(8.dp)
                RankBar(count, Modifier.fillMaxWidth())
                Gap(6.dp)
                val next = rank.next
                Text(
                    if (next == null) stringResource(R.string.grimoire_rank_max)
                    else stringResource(R.string.grimoire_rank_next, next.casts - count, t(next.title)),
                    style = UType.Muted.copy(color = InkMuted),
                )
            }
            Gap(12.dp)
            Panel(Modifier.fillMaxWidth()) {
                Fact(stringResource(R.string.spell_interval), every(s.intervalDays))
                Fact(stringResource(R.string.spell_effort), effortName(s.effort))
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.spell_mana), style = UType.Sub, modifier = Modifier.weight(1f))
                    if (w.isPendingRequest(s.id)) Text(stringResource(R.string.spell_mana_double) + "  ", style = UType.Small.copy(color = U.c.accent))
                    ManaTag(w.manaFor(s), prefix = "+")
                }
                Fact(stringResource(R.string.spell_last), lastCast(w, s))
                Fact(stringResource(R.string.spell_helper), t(helper.kind.title))
                if (s.paused) Fact(stringResource(R.string.spell_paused), "✓")
            }
            SectionTitle(stringResource(R.string.spell_calendar))
            Panel(Modifier.fillMaxWidth()) {
                CastCalendar(w, s.id)
                Gap(10.dp)
                val monthStart = w.today.withDayOfMonth(1).atStartOfDay(w.zone).toInstant().toEpochMilli()
                Text(stringResource(R.string.spell_month, w.castsOf(s.id).count { it.at >= monthStart }), style = UType.Muted)
            }
            Gap(16.dp)
            PixelButton(stringResource(R.string.spell_edit), { go.spellForm(s.id) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, icon = Glyphs.pencil)
            Gap(24.dp)
        }
    }
}

/** Grimoire pages are always parchment, whatever the theme. */
internal val Parchment = Color(0xFFF4E4BE)
internal val Ink = Color(0xFF2B1D12)
internal val InkMuted = Color(0xFF6E5640)
internal val InkFaint = Color(0xFF9A8060)

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = UType.Sub, modifier = Modifier.weight(1f))
        Text(value, style = UType.Body, textAlign = TextAlign.End)
    }
}

@Composable
internal fun every(days: Int): String =
    if (days == 1) stringResource(R.string.spell_every_day) else pluralStringResource(R.plurals.spell_every, days, days)

@Composable
internal fun effortName(effort: Int): String = stringResource(
    when (effort) {
        1 -> R.string.spell_effort_1
        3 -> R.string.spell_effort_3
        else -> R.string.spell_effort_2
    }
)

@Composable
private fun lastCast(w: World, s: Spell): String {
    val last = w.castsOf(s.id).firstOrNull()?.at ?: return stringResource(R.string.spell_never)
    val day = Instant.ofEpochMilli(last).atZone(w.zone).toLocalDate()
    val ago = (w.today.toEpochDay() - day.toEpochDay()).toInt()
    return when {
        ago <= 0 -> stringResource(R.string.spell_today)
        ago == 1 -> stringResource(R.string.spell_yesterday)
        else -> pluralStringResource(R.plurals.spell_days_ago, ago, ago)
    }
}

/** Twelve weeks of casts as a grid of squares, a column a week, Monday on top; darker squares for more casts that day. */
@Composable
private fun CastCalendar(w: World, spellId: Long) {
    val c = U.c
    val weeks = 12
    val start: LocalDate = w.weekStart.minusWeeks((weeks - 1).toLong())
    val counts = remember(w, spellId) {
        w.castsOf(spellId).groupingBy { Instant.ofEpochMilli(it.at).atZone(w.zone).toLocalDate().toEpochDay() }.eachCount()
    }
    Canvas(Modifier.fillMaxWidth().aspectRatio(weeks / 7f)) {
        val gap = PX.toPx() * 0.67f
        val cell = (size.width - gap * (weeks - 1)) / weeks
        for (wk in 0 until weeks) for (d in 0 until 7) {
            val date = start.plusDays((wk * 7 + d).toLong())
            val n = counts[date.toEpochDay()] ?: 0
            val future = date.isAfter(w.today)
            val color = when {
                future -> Color.Transparent
                n == 0 -> c.surface2
                n == 1 -> c.accent.copy(alpha = 0.65f)
                else -> c.accent
            }
            val o = Offset(wk * (cell + gap), d * (cell + gap))
            drawRect(color, o, Size(cell, cell))
            if (date == w.today) drawRect(c.ink, o, Size(cell, cell / 6))
        }
    }
}

// A helper's page.

@Composable
fun HelperScreen(kind: String, data: WData, go: Go) {
    val w = data.world
    val k = HelperKind.of(kind)
    val h = w.helper(k)
    Column(Modifier.fillMaxSize()) {
        TopBar(t(k.title), go::back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Gap(6.dp)
                    if (h.awake) AnimatedSprite({ Art.helper(k, it) }, 150.dp, periodMs = 420)
                    else SpriteImage(Art.helper(k).muted(), 150.dp, alpha = 0.8f)
                    Gap(12.dp)
                    Text(t(k.title).uppercase(), style = UType.Title.copy(color = if (h.awake) U.c.accent else U.c.muted))
                    Gap(6.dp)
                    Text(t(k.blurb), style = UType.Sub, textAlign = TextAlign.Center)
                    Gap(14.dp)
                    Text(
                        (if (h.awake) stringResource(R.string.helper_level, h.level) else stringResource(R.string.helper_asleep)).uppercase(),
                        style = UType.Heading,
                    )
                    Gap(8.dp)
                    LevelPips(h.level, 12.dp)
                    Gap(10.dp)
                    val next = h.nextAt
                    val prev = HelperState.HELPER_LEVELS.getOrElse(h.level - 1) { 0 }
                    if (next != null) {
                        PixelBar((h.casts - prev).toFloat() / (next - prev), Modifier.fillMaxWidth())
                        Gap(6.dp)
                        Text(
                            if (h.awake) gp(R.plurals.helper_to_level, next - h.casts, next - h.casts, h.level + 1)
                            else gp(R.plurals.helper_to_wake, next - h.casts, next - h.casts),
                            style = UType.Muted,
                        )
                    } else {
                        Text(stringResource(R.string.helper_max), style = UType.Muted)
                    }
                    Gap(8.dp)
                    val home = w.floor(h.homeFloorId)
                    Text(
                        if (home != null) stringResource(R.string.helper_home, floorName(home)) else stringResource(R.string.helper_homeless),
                        style = UType.Muted,
                    )
                }
            }
            SectionTitle(stringResource(R.string.helper_spells))
            val spells = h.spellIds.mapNotNull { w.spell(it) }
            if (spells.isEmpty()) Text(gs(R.string.helper_none), style = UType.Sub)
            spells.forEach { s ->
                SpellRow(s, w) { go.spell(s.id) }
                Gap(8.dp)
            }
            Gap(24.dp)
        }
    }
}
