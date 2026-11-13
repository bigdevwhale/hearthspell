package app.hearthspell.ui.grimoire

import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import app.hearthspell.ui.components.Chip
import app.hearthspell.ui.tips.HelpTile
import app.hearthspell.ui.tips.TipHost
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import app.hearthspell.content.ImpKind
import app.hearthspell.content.Stars
import app.hearthspell.content.Trinket
import app.hearthspell.ui.art.Looks
import app.hearthspell.container
import app.hearthspell.data.Cast
import app.hearthspell.data.Spell
import app.hearthspell.data.addSpell
import app.hearthspell.data.undoCast
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
import app.hearthspell.ui.art.bitmap
import app.hearthspell.ui.components.ButtonKind
import app.hearthspell.ui.components.ConfirmDialog
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
import app.hearthspell.ui.locale
import app.hearthspell.ui.spellName
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import app.hearthspell.ui.tips.Tip
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// The book of everything learned: the apprentice's circle, the helpers, and a page for every spell.

/** The grimoire's three pages, picked from the switch under the banner. */
private enum class Page { SPELLS, HELPERS, COLLECTIONS }

@Composable
fun GrimoireTab(data: WData, go: Go) {
    val w = data.world
    var page by rememberSaveable { mutableStateOf(Page.SPELLS) }
    TipHost(Tip.GRIMOIRE)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GrimoireBanner(w)
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            val awake = w.helpers.count { it.awake }
            PageSwitch(
                listOf(
                    stringResource(R.string.grimoire_spells) to "${w.spells.size}",
                    stringResource(R.string.grimoire_helpers) to "$awake/${w.helpers.size}",
                    stringResource(R.string.grimoire_collections) to "",
                ),
                page.ordinal,
            ) { page = Page.entries[it] }
            Gap(14.dp)
            when (page) {
                Page.SPELLS -> SpellsPage(w, go)
                Page.HELPERS -> HelpersPage(w, go)
                Page.COLLECTIONS -> CollectionsPage(w, go)
            }
            Gap(24.dp)
        }
    }
}

/** The picture over the grimoire: an open book on a lectern, with the title, the circle and the mana on it. */
@Composable
private fun GrimoireBanner(w: World) {
    val c = U.c
    val pic = Art.grimoireBanner
    Box(Modifier.fillMaxWidth().height(if (pic != null) 210.dp else 120.dp)) {
        if (pic != null) {
            Image(
                pic.bitmap(), null, Modifier.matchParentSize(),
                contentScale = ContentScale.Crop, filterQuality = FilterQuality.Low,
            )
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to c.bg)))
        }
        Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.pixelBox(c.surface.copy(alpha = 0.85f), c.outline, c.bevel).padding(horizontal = 10.dp, vertical = 6.dp)) { ManaTag(w.mana) }
            Spacer(Modifier.width(8.dp))
            HelpTile(Tip.GRIMOIRE)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
            Text(stringResource(R.string.grimoire_title), style = UType.Display)
            Text(
                stringResource(R.string.grimoire_circle, w.circle) + "  ·  " + gp(R.plurals.grimoire_total, w.totalCasts, w.totalCasts),
                style = UType.Sub.copy(color = c.ink),
            )
        }
    }
    Gap(10.dp)
}

/** A row of tabs in one rounded track; the picked one is filled. Each may carry a small count. */
@Composable
private fun PageSwitch(items: List<Pair<String, String>>, picked: Int, onPick: (Int) -> Unit) {
    val c = U.c
    Row(Modifier.fillMaxWidth().pixelBox(c.surface, c.outline, c.bevel).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { i, (label, count) ->
            val on = i == picked
            val fill by animateColorAsState(if (on) c.accent else Color.Transparent, label = "page")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(fill)
                    .clickable { onPick(i) }.padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = UType.Strong.copy(fontSize = 14.sp, color = if (on) c.onAccent else c.ink),
                )
                Text(count.ifEmpty { " " }, style = UType.Small.copy(color = if (on) c.onAccent.copy(alpha = 0.8f) else c.faint))
            }
        }
    }
}

/** Spells, a card per floor; chips on top narrow it to one floor. Due and overdue spells come first (#5). */
@Composable
private fun SpellsPage(w: World, go: Go) {
    if (w.spells.isEmpty()) {
        Text(gs(R.string.grimoire_empty), style = UType.Sub)
        return
    }
    val floors = w.floors.filter { w.spellsOf(it.id).isNotEmpty() }
    var only by rememberSaveable { mutableLongStateOf(-1L) }
    val shown = floors.filter { only == -1L || it.id == only }.ifEmpty { floors }
    if (floors.size > 1) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(R.string.grimoire_all), shown.size == floors.size, { only = -1L })
            floors.forEach { f -> Chip(floorName(f), shown.size == 1 && only == f.id, { only = f.id }) }
        }
        Gap(12.dp)
    }
    shown.forEach { f ->
        val spells = w.spellsOf(f.id).sortedBy { w.freshness(it) }
        Panel(Modifier.fillMaxWidth(), padding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(floorName(f), style = UType.Heading.copy(fontSize = UType.Heading.fontSize * 0.85f, color = U.c.accent), modifier = Modifier.weight(1f))
                Text("${spells.size}", style = UType.Small)
            }
            Gap(4.dp)
            spells.forEachIndexed { i, s ->
                if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 44.dp).height(1.dp).background(U.c.bevel.copy(alpha = 0.35f)))
                SpellLine(s, w) { go.spell(s.id) }
            }
        }
        Gap(10.dp)
    }
}

/** One spell inside a floor's card: its helper, name, rank and a thin bar to the next rank. */
@Composable
private fun SpellLine(s: Spell, w: World, onClick: () -> Unit) {
    val count = w.castCount(s.id)
    val rank = Rank.of(count)
    val awake = w.helper(HelperKind.of(s.helper)).awake
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        SpriteImage(Art.helper(HelperKind.of(s.helper)).let { if (awake) it else it.muted() }, 32.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(spellName(s), style = UType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t(rank.title) + "  ·  " + gp(R.plurals.grimoire_casts, count, count), style = UType.Small)
            Gap(5.dp)
            RankBar(count, Modifier.fillMaxWidth(), height = 6.dp)
        }
        Spacer(Modifier.width(8.dp))
        PixelIcon(Glyphs.back, U.c.faint, Modifier.graphicsLayer { scaleX = -1f }, size = 14.dp)
    }
}

/** Every helper, awake ones and the most grown first, three to a row. */
@Composable
private fun HelpersPage(w: World, go: Go) {
    Text(gs(R.string.tip_grimoire_2), style = UType.Muted)
    Gap(12.dp)
    HelperGrid(w.helpers.sortedWith(compareByDescending<HelperState> { it.awake }.thenByDescending { it.casts })) { go.helper(it.name) }
}

/** The places the grimoire opens onto: the decor shop first, then the collections two to a row. */
@Composable
private fun CollectionsPage(w: World, go: Go) {
    Panel(Modifier.fillMaxWidth(), onClick = { go.decor(0) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SpriteImage(Art.decor(DecorKind.CHEST), 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.grimoire_decor), style = UType.Heading)
                Text(stringResource(R.string.grimoire_decor_sub), style = UType.Muted)
            }
        }
    }
    Gap(10.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tile(Modifier.weight(1f), { SpriteImage(Art.decor(DecorKind.TELESCOPE), 56.dp) }, stringResource(R.string.stars_title), "${w.starsLit}/${Stars.total}", go::stars)
        Tile(Modifier.weight(1f), { SpriteImage(Art.decor(DecorKind.CAULDRON), 56.dp) }, stringResource(R.string.brew_title), if (w.potions.isEmpty()) "" else "${w.potions.size}", go::brew)
    }
    Gap(10.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tile(Modifier.weight(1f), { SpriteImage(Looks.imp(ImpKind.PIP, 0), 56.dp) }, stringResource(R.string.bestiary_title), "${w.impsMet.size}/${ImpKind.entries.size}", go::bestiary)
        Tile(Modifier.weight(1f), { SpriteImage(Art.trinket(Trinket.BUTTON.name), 46.dp) }, stringResource(R.string.treasures_title), "${w.trinketsFound.size}/${Trinket.entries.size}", go::treasures)
    }
    Gap(10.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tile(Modifier.weight(1f), { SpriteImage(Art.envelope("sealed") ?: Art.master(), 56.dp) }, stringResource(R.string.letters_open), "", go::letters)
        Tile(Modifier.weight(1f), { PixelIcon(Glyphs.share, U.c.accent, size = 40.dp) }, stringResource(R.string.cards_open), "", go::postcards)
    }
}

/** A collection's card: a big picture, its name and how much of it is found. */
@Composable
private fun Tile(modifier: Modifier, icon: @Composable () -> Unit, title: String, sub: String, onClick: () -> Unit) {
    Panel(modifier, onClick = onClick, padding = PaddingValues(horizontal = 10.dp, vertical = 14.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.height(58.dp), contentAlignment = Alignment.Center) { icon() }
            Gap(8.dp)
            Text(title, style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(sub.ifEmpty { " " }, style = UType.Small.copy(color = U.c.accent))
        }
    }
}

@Composable
internal fun CirclePanel(w: World, s: app.hearthspell.data.Settings, onWardrobe: () -> Unit) {
    val circle = w.circle
    val from = Mana.circles.getOrElse(circle - 1) { 0 }
    val to = Mana.circles.getOrNull(circle)
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable(onClick = onWardrobe)) { AnimatedSprite({ Looks.apprentice(s.robe, s.hat, it) }, 64.dp) }
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
        helpers.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { h -> HelperCell(h, Modifier.weight(1f)) { onClick(h.kind) } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
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
        if (h.awake) AnimatedSprite({ Art.helper(h.kind, it) }, 56.dp)
        else SpriteImage(Art.helper(h.kind).muted(), 56.dp, alpha = 0.7f)
        Gap(6.dp)
        Text(t(h.kind.title), style = UType.Small.copy(color = if (h.awake) c.ink else c.faint), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Gap(4.dp)
        LevelPips(h.level)
    }
}

/** Four little dots, lit up to the helper's level. */
@Composable
internal fun LevelPips(level: Int, size: Dp = 6.dp) {
    val c = U.c
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(HelperState.HELPER_LEVELS.size) { i ->
            Box(Modifier.size(size).background(if (i < level) c.accent else c.outline.copy(alpha = 0.45f), androidx.compose.foundation.shape.CircleShape))
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
internal fun RankBar(count: Int, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val rank = Rank.of(count)
    val next = rank.next
    val f = if (next == null) 1f else (count - rank.casts).toFloat() / (next.casts - rank.casts)
    PixelBar(f, modifier, color = U.c.accent, segments = 8, height = height)
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
    var copying by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.grimoire_title), go::back, Tip.SPELL) {
            if (s != null) app.hearthspell.ui.share.ShareTile { app.hearthspell.ui.share.spellCard(data, s) }
        }
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
            RecentCasts(w, s)
            Gap(16.dp)
            PixelButton(stringResource(R.string.spell_edit), { go.spellForm(s.id) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, icon = Glyphs.pencil)
            if (w.floors.size > 1) {
                Gap(10.dp)
                PixelButton(stringResource(R.string.spell_copy), { copying = true }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, icon = Glyphs.plus)
            }
            Gap(24.dp)
        }
        if (copying) CopySpellDialog(s, w) { copying = false }
    }
}

/** Copies a spell to other floors: same name, rhythm, effort and helper; each copy keeps its own casts and due day (#9). */
@Composable
private fun CopySpellDialog(s: Spell, w: World, onDismiss: () -> Unit) {
    val container = LocalContext.current.container
    var picked by remember { mutableStateOf(emptySet<Long>()) }
    // The copies' name, editable: "Wash the windows" can become "Wash the windows in the bedroom".
    val original = spellName(s)
    var name by remember { mutableStateOf(original) }
    val others = w.floors.filter { it.id != s.floorId }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        title = { Text(stringResource(R.string.spell_copy_title), style = UType.Heading) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.spell_copy_text), style = UType.Muted)
                Gap(10.dp)
                app.hearthspell.ui.components.PixelField(
                    name, { name = it.take(60) }, Modifier.fillMaxWidth(), placeholder = original,
                    keyboard = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                )
                Gap(8.dp)
                others.forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().clickable { picked = if (f.id in picked) picked - f.id else picked + f.id }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        app.hearthspell.ui.setup.CheckBox(f.id in picked)
                        Spacer(Modifier.width(12.dp))
                        Text(floorName(f), style = UType.Body)
                    }
                }
            }
        },
        confirmButton = {
            PixelButton(stringResource(R.string.spell_copy_go), {
                val now = System.currentTimeMillis()
                val preset = Spells.of(s.presetKey)
                val targets = picked.toList()
                // Unchanged (or emptied): keep the original's own name, so a catalog spell stays translated.
                val newName = name.trim().takeIf { it.isNotEmpty() && it != original } ?: s.name
                container.write {
                    for (f in targets) addSpell(f, preset, now, name = newName, intervalDays = s.intervalDays, effort = s.effort, helper = s.helper)
                }
                onDismiss()
            }, enabled = picked.isNotEmpty())
        },
        dismissButton = { PixelButton(stringResource(R.string.cancel), onDismiss, kind = ButtonKind.SECONDARY) },
    )
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

/** The last few casts, each of which can be taken back: a tap by mistake shouldn't stay forever. */
@Composable
private fun RecentCasts(w: World, s: Spell) {
    val recent = w.castsOf(s.id).take(RECENT_CASTS)
    if (recent.isEmpty()) return
    val container = LocalContext.current.container
    var asking by remember { mutableStateOf<Cast?>(null) }
    SectionTitle(stringResource(R.string.spell_recent))
    Panel(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
        recent.forEach { c ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(castWhen(w, c.at), style = UType.Body, modifier = Modifier.weight(1f))
                ManaTag(c.mana, prefix = "+")
                Spacer(Modifier.width(10.dp))
                PixelButton(stringResource(R.string.undo), { asking = c }, kind = ButtonKind.SECONDARY, enabled = w.canUndo(c))
            }
        }
    }
    Gap(6.dp)
    Text(
        stringResource(if (recent.all { w.canUndo(it) }) R.string.spell_recent_note else R.string.spell_recent_spent),
        style = UType.Muted,
    )
    asking?.let { c ->
        ConfirmDialog(
            gs(R.string.spell_undo_title),
            gs(R.string.spell_undo_text, c.mana),
            stringResource(R.string.spell_undo_confirm),
            onConfirm = { container.write { undoCast(c.id) } },
            onDismiss = { asking = null },
        )
    }
}

private const val RECENT_CASTS = 5

/** "Today, 14:32", "Yesterday, 09:10" or "28 Oct, 18:05". */
@Composable
private fun castWhen(w: World, at: Long): String {
    val z = Instant.ofEpochMilli(at).atZone(w.zone)
    val time = z.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (w.today.toEpochDay() - z.toLocalDate().toEpochDay()) {
        0L -> stringResource(R.string.spell_today) + ", " + time
        1L -> stringResource(R.string.spell_yesterday) + ", " + time
        else -> z.format(DateTimeFormatter.ofPattern("d MMM, HH:mm", locale()))
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
        TopBar(t(k.title), go::back, Tip.HELPER) { app.hearthspell.ui.share.ShareTile { app.hearthspell.ui.share.helperCard(data, k) } }
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
