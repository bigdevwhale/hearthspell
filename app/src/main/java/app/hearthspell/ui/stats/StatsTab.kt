package app.hearthspell.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.content.HelperKind
import app.hearthspell.data.FIND_GIFT
import app.hearthspell.data.FIND_IMP
import app.hearthspell.data.Spell
import app.hearthspell.domain.World
import app.hearthspell.ui.Go
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.ManaTag
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelBar
import app.hearthspell.ui.components.SectionTitle
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.floorName
import app.hearthspell.ui.grimoire.CirclePanel
import app.hearthspell.ui.locale
import app.hearthspell.ui.spellName
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayStyle
import kotlin.math.roundToInt

/** Days in the chart of casts. */
private const val DAYS = 14

/** The numbers behind the chronicle, worked out once per world. */
private class Chronicle(private val w: World) {
    private fun dayOf(at: Long): LocalDate = Instant.ofEpochMilli(at).atZone(w.zone).toLocalDate()

    private val castDays = w.casts.map { dayOf(it.at) to it }
    private val lastWeekStart = w.weekStart.minusWeeks(1)

    /** Casts per day, oldest first, ending today. */
    val days: List<Pair<LocalDate, Int>> = (DAYS - 1 downTo 0).map { back ->
        val d = w.today.minusDays(back.toLong())
        d to castDays.count { it.first == d }
    }

    private fun inWeek(d: LocalDate, start: LocalDate) = !d.isBefore(start) && d.isBefore(start.plusWeeks(1))

    val weekCasts = castDays.count { inWeek(it.first, w.weekStart) }
    val lastWeekCasts = castDays.count { inWeek(it.first, lastWeekStart) }
    val weekMana = castDays.filter { inWeek(it.first, w.weekStart) }.sumOf { it.second.mana }
    val lastWeekMana = castDays.filter { inWeek(it.first, lastWeekStart) }.sumOf { it.second.mana }
    private val impDays = w.finds.filter { it.type == FIND_IMP }.map { LocalDate.ofEpochDay(it.day) }
    val weekImps = impDays.count { inWeek(it, w.weekStart) }
    val lastWeekImps = impDays.count { inWeek(it, lastWeekStart) }

    val impsAll = impDays.size
    val giftsAll = w.finds.count { it.type == FIND_GIFT }
    val daysWithTower = (w.day - w.startDay + 1).coerceAtLeast(1)

    /** The spells cast most, all time. */
    val favourites: List<Pair<Spell, Int>> = w.spells.map { it to w.castCount(it.id) }.filter { it.second > 0 }
        .sortedByDescending { it.second }.take(5)
}

/** The apprentice's chronicle: how the tower has been kept, this week and all along. */
@Composable
fun StatsTab(data: WData, go: Go) {
    val w = data.world
    val ch = remember(w) { Chronicle(w) }
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.stats_title), null) { ManaTag(w.mana, Modifier.padding(end = 4.dp)) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            CirclePanel(w, data.settings, go::wardrobe)
            Gap(10.dp)
            Panel(Modifier.fillMaxWidth(), onClick = go::postcards, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    app.hearthspell.ui.art.PixelIcon(app.hearthspell.ui.art.Glyphs.share, U.c.accent, size = 28.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.cards_title), style = UType.Heading)
                        Text(stringResource(R.string.cards_sub), style = UType.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            var weekOpen by remember { androidx.compose.runtime.mutableStateOf(false) }
            SectionTitle(stringResource(R.string.stats_week), action = stringResource(R.string.card_share), onAction = { weekOpen = true })
            if (weekOpen) app.hearthspell.ui.share.PostcardDialog(app.hearthspell.ui.share.weekCard(data)) { weekOpen = false }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Figure(Modifier.weight(1f), "${ch.weekCasts}", stringResource(R.string.stats_casts), ch.weekCasts - ch.lastWeekCasts)
                Figure(Modifier.weight(1f), "${ch.weekMana}", stringResource(R.string.stats_mana), ch.weekMana - ch.lastWeekMana)
                Figure(Modifier.weight(1f), "${ch.weekImps}", stringResource(R.string.stats_imps), ch.weekImps - ch.lastWeekImps)
            }

            SectionTitle(stringResource(R.string.stats_days, DAYS))
            Panel(Modifier.fillMaxWidth()) { CastChart(ch.days) }

            SectionTitle(stringResource(R.string.stats_floors))
            Panel(Modifier.fillMaxWidth()) {
                if (w.floors.isEmpty()) Text(stringResource(R.string.stats_none), style = UType.Muted)
                w.floors.forEachIndexed { i, f ->
                    if (i > 0) Gap(12.dp)
                    val tidy = 1f - w.dust(f.id)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(floorName(f), style = UType.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${(tidy * 100).roundToInt()}%", style = UType.Strong)
                    }
                    Gap(6.dp)
                    PixelBar(tidy, Modifier.fillMaxWidth(), color = if (w.imps(f.id) > 0) U.c.imp else U.c.good, height = 10.dp)
                }
            }

            if (ch.favourites.isNotEmpty()) {
                SectionTitle(stringResource(R.string.stats_favourites))
                Panel(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                    ch.favourites.forEachIndexed { i, (s, n) ->
                        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}", style = UType.Heading.copy(color = U.c.accent), modifier = Modifier.width(26.dp))
                            SpriteImage(Art.helper(HelperKind.of(s.helper)), 34.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(spellName(s), style = UType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(floorName(w.floor(s.floorId)), style = UType.Small)
                            }
                            Text(pluralStringResource(R.plurals.stats_times, n, n), style = UType.Strong)
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.stats_all_time))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Figure(Modifier.weight(1f), "${w.totalCasts}", stringResource(R.string.stats_casts))
                Figure(Modifier.weight(1f), "${w.manaEarned}", stringResource(R.string.stats_mana))
            }
            Gap(8.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Figure(Modifier.weight(1f), "${ch.impsAll}", stringResource(R.string.stats_imps))
                Figure(Modifier.weight(1f), "${ch.giftsAll}", stringResource(R.string.stats_gifts))
                Figure(Modifier.weight(1f), "${ch.daysWithTower}", stringResource(R.string.stats_days_tower))
            }
            Gap(24.dp)
        }
    }
}

/** A headline number with its label, and how it compares with last week when [delta] is given. */
@Composable
private fun Figure(modifier: Modifier, value: String, label: String, delta: Int? = null) {
    Panel(modifier, padding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
        Text(value, style = UType.Title)
        Text(label, style = UType.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (delta != null) {
            Gap(2.dp)
            val (text, color) = when {
                delta > 0 -> "▲ $delta" to U.c.good
                delta < 0 -> "▼ ${-delta}" to U.c.muted
                else -> "=" to U.c.faint
            }
            Text(text, style = UType.Small.copy(color = color))
        }
    }
}

/** Casts per day as bars; a tap on a day shows its count (today is shown at first). */
@Composable
private fun CastChart(days: List<Pair<LocalDate, Int>>) {
    val c = U.c
    var picked by remember(days) { mutableIntStateOf(days.lastIndex) }
    val max = maxOf(1, days.maxOf { it.second })
    val (pd, pn) = days[picked]
    val fmt = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }
    val loc = locale()
    Text(
        pluralStringResource(R.plurals.stats_day_casts, pn, pn),
        style = UType.Heading,
    )
    Text(pd.format(fmt.withLocale(loc)).replaceFirstChar { it.titlecase(loc) }, style = UType.Muted)
    Gap(12.dp)
    Canvas(
        Modifier.fillMaxWidth().height(120.dp).pointerInput(days) {
            detectTapGestures { o -> picked = (o.x / (size.width / days.size)).toInt().coerceIn(0, days.lastIndex) }
        }
    ) {
        val slot = size.width / days.size
        val gap = 4.dp.toPx()
        val bw = slot - gap
        val r = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        // A recessive baseline and a faint mid guide.
        drawLine(c.faint.copy(alpha = 0.5f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        drawLine(c.faint.copy(alpha = 0.18f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
        days.forEachIndexed { i, (_, n) ->
            val x = i * slot + gap / 2
            val h = if (n == 0) 2.dp.toPx() else size.height * n / max
            val col = if (i == picked) c.accent else c.accent.copy(alpha = 0.45f)
            // Rounded at the top, square on the baseline.
            drawRoundRect(col, Offset(x, size.height - h), Size(bw, h), r)
            if (h > r.y) drawRect(col, Offset(x, size.height - r.y), Size(bw, r.y))
            if (i == picked) drawRoundRect(c.ink.copy(alpha = 0.5f), Offset(x, size.height - h), Size(bw, h), r, style = Stroke(1.dp.toPx()))
        }
    }
    Gap(6.dp)
    Row(Modifier.fillMaxWidth()) {
        days.forEachIndexed { i, (d, _) ->
            Text(
                d.dayOfWeek.getDisplayName(DayStyle.NARROW_STANDALONE, loc),
                style = UType.Small.copy(color = if (i == picked) c.ink else c.faint),
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
        }
    }
}
