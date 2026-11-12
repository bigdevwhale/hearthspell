package app.hearthspell.ui.share

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.hearthspell.R
import app.hearthspell.content.HelperKind
import app.hearthspell.content.ImpKind
import app.hearthspell.content.Trinket
import app.hearthspell.data.FIND_IMP
import app.hearthspell.data.Floor
import app.hearthspell.data.Spell
import app.hearthspell.domain.Rank
import app.hearthspell.ui.Go
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.Looks
import app.hearthspell.ui.components.ButtonKind
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.IconTile
import app.hearthspell.ui.components.PixelButton
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.floorName
import app.hearthspell.ui.gp
import app.hearthspell.ui.gs
import app.hearthspell.ui.locale
import app.hearthspell.ui.quote
import app.hearthspell.ui.roomName
import app.hearthspell.ui.spellName
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import app.hearthspell.ui.tower.look
import app.hearthspell.ui.tower.towerSky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.roundToInt

private const val GOLD = 0xFFF4C66E.toInt()
private const val LILAC = 0xFFB7A2FF.toInt()
private const val ROSE = 0xFFFF8FB4.toInt()

/** The words every card ends with. */
@Composable
private fun card(key: String, eyebrow: String, title: String, sub: String, figures: List<Pair<String, String>>, picture: Picture) =
    Postcard(key, eyebrow, title, sub, figures, picture, gs(R.string.card_tagline), gs(R.string.card_store))

/** This week's casts, day by day, beside the apprentice. */
@Composable
internal fun weekCard(data: WData): Postcard {
    val w = data.world
    val loc = locale()
    val start = w.weekStart
    val days = (0 until 7).map { start.plusDays(it.toLong()) }
    val dayOf = { at: Long -> java.time.Instant.ofEpochMilli(at).atZone(w.zone).toLocalDate() }
    val castDays = w.casts.map { dayOf(it.at) to it.mana }
    val counts = days.map { d -> castDays.count { it.first == d } }
    val total = counts.sum()
    val mana = castDays.filter { it.first in days }.sumOf { it.second }
    val imps = w.finds.count { it.type == FIND_IMP && LocalDate.ofEpochDay(it.day) in days }
    val best = counts.indices.maxByOrNull { counts[it] }?.takeIf { counts[it] > 0 }
    return card(
        "week",
        gs(R.string.card_week_eyebrow),
        gp(R.plurals.card_week_title, total, total),
        if (best == null) gs(R.string.card_week_quiet) else gs(R.string.card_week_best, days[best].dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, loc).lowercase(loc)),
        listOf("$total" to gs(R.string.stats_casts), "$mana" to gs(R.string.stats_mana), "$imps" to gs(R.string.stats_imps)),
        Picture.Week(counts, days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT_STANDALONE, loc).replaceFirstChar { c -> c.titlecase(loc) } }, Looks.apprentice(data.settings.robe, data.settings.hat, 0)),
    )
}

/** The whole tower. */
@Composable
internal fun towerCard(data: WData): Postcard {
    val w = data.world
    val n = w.floors.size
    return card(
        "tower",
        gs(R.string.card_tower_eyebrow),
        gs(R.string.card_tower_title_plain),
        gp(R.plurals.card_tower_sub, n, n),
        listOf(
            "$n" to gs(R.string.card_f_floors),
            "${w.helpers.count { it.awake }}" to gs(R.string.card_f_awake),
            "${(w.day - w.startDay + 1).coerceAtLeast(1)}" to gs(R.string.stats_days_tower),
        ),
        Picture.Tower(w.floors.take(8).map { data.look(it) }, w.towerSky()),
    )
}

/** The apprentice in their robe and hat, and how far the apprenticeship has come. */
@Composable
internal fun apprenticeCard(data: WData): Postcard {
    val w = data.world
    return card(
        "apprentice",
        gs(R.string.card_me_eyebrow),
        data.settings.name.ifBlank { gs(R.string.card_me_title) },
        gs(R.string.grimoire_circle, w.circle) + " · " + gp(R.plurals.grimoire_total, w.totalCasts, w.totalCasts),
        listOf(
            "${w.circle}" to gs(R.string.card_f_circle),
            "${w.manaEarned}" to gs(R.string.stats_mana),
            "${(w.day - w.startDay + 1).coerceAtLeast(1)}" to gs(R.string.stats_days_tower),
        ),
        Picture.Figure(Looks.apprentice(data.settings.robe, data.settings.hat, 0), GOLD),
    )
}

/** One floor, the apprentice in it. */
@Composable
internal fun roomCard(data: WData, f: Floor): Postcard {
    val w = data.world
    val imps = w.imps(f.id)
    return card(
        "room-${f.id}",
        roomName(f),
        floorName(f),
        if (imps > 0) gp(R.plurals.card_room_imps, imps, imps) else gs(R.string.card_room_clean),
        listOf(
            "${((1f - w.dust(f.id)) * 100).roundToInt()}%" to gs(R.string.card_f_tidy),
            "${w.spellsOf(f.id).size}" to gs(R.string.card_f_spells),
        ),
        Picture.Room(data.look(f, withApprentice = true)),
    )
}

@Composable
internal fun helperCard(data: WData, kind: HelperKind): Postcard {
    val h = data.world.helper(kind)
    return card(
        "helper-${kind.name.lowercase()}",
        gs(if (h.awake) R.string.card_helper_eyebrow else R.string.card_helper_asleep),
        t(kind.title),
        t(kind.blurb),
        listOf("${h.level}" to gs(R.string.card_f_level), "${h.casts}" to gs(R.string.stats_casts)),
        Picture.Figure(if (h.awake) Art.helper(kind, 0) else Art.helper(kind).muted(), LILAC),
    )
}

@Composable
internal fun helpersCard(data: WData): Postcard {
    val hs = data.world.helpers.sortedWith(compareByDescending<app.hearthspell.domain.HelperState> { it.awake }.thenByDescending { it.casts })
    val awake = hs.count { it.awake }
    return card(
        "helpers",
        gs(R.string.card_helpers_eyebrow),
        gs(R.string.card_helpers_title, awake, hs.size),
        "",
        emptyList(),
        Picture.Shelf(hs.map { Art.helper(it.kind) to it.awake }),
    )
}

@Composable
internal fun impCard(data: WData, kind: ImpKind): Postcard {
    val n = data.world.impsMet[kind] ?: 0
    return card(
        "imp-${kind.name.lowercase()}",
        gs(R.string.card_imp_eyebrow),
        t(kind.title),
        t(kind.habit) + " " + quote(t(kind.farewell), locale()),
        listOf("$n" to gs(R.string.card_f_banished)),
        Picture.Figure(Looks.imp(kind, 0), ROSE),
    )
}

@Composable
internal fun bestiaryCard(data: WData): Postcard {
    val met = data.world.impsMet
    return card(
        "bestiary",
        gs(R.string.card_bestiary_eyebrow),
        gs(R.string.card_bestiary_title, met.size, ImpKind.entries.size),
        gs(R.string.card_bestiary_sub),
        listOf("${met.values.sum()}" to gs(R.string.stats_imps)),
        Picture.Shelf(ImpKind.entries.map { Looks.imp(it, 0) to (it in met) }),
    )
}

@Composable
internal fun treasuresCard(data: WData): Postcard {
    val found = data.world.trinketsFound
    return card(
        "treasures",
        gs(R.string.card_treasures_eyebrow),
        gs(R.string.card_treasures_title, found.size, Trinket.entries.size),
        gs(R.string.card_treasures_sub),
        emptyList(),
        Picture.Shelf(Trinket.entries.map { Art.trinket(it.name) to (it in found) }),
    )
}

/** A spell and its rank: the chore the apprentice is proudest of. */
@Composable
internal fun spellCard(data: WData, s: Spell): Postcard {
    val w = data.world
    val n = w.castCount(s.id)
    val rank = Rank.of(n)
    return card(
        "spell-${s.id}",
        t(rank.title),
        spellName(s),
        gs(R.string.card_spell_sub, floorName(w.floor(s.floorId)), t(rank.title).lowercase(locale())),
        listOf("$n" to gs(R.string.card_f_cast)),
        Picture.Figure(Art.helper(HelperKind.of(s.helper), 0), GOLD),
    )
}

/** Every card the tower can make right now, in the gallery's order. */
@Composable
internal fun allCards(data: WData): List<Postcard> {
    val w = data.world
    val out = mutableListOf(weekCard(data))
    if (w.floors.isNotEmpty()) out += towerCard(data)
    out += apprenticeCard(data)
    w.floors.forEach { out += roomCard(data, it) }
    out += helpersCard(data)
    w.helpers.filter { it.awake }.sortedByDescending { it.casts }.forEach { out += helperCard(data, it.kind) }
    out += bestiaryCard(data)
    ImpKind.entries.filter { it in w.impsMet }.forEach { out += impCard(data, it) }
    out += treasuresCard(data)
    w.spells.map { it to w.castCount(it.id) }.filter { it.second > 0 }.sortedByDescending { it.second }.take(3)
        .forEach { out += spellCard(data, it.first) }
    return out
}

/** What makes a card look different: its preview is drawn again only when this changes. */
private val Postcard.signature: String get() = "$key|$eyebrow|$title|$sub|$figures"

private val Night = Color(0xFF120E2E)

/** A share button for a top bar: opens [card] in the postcard preview. */
@Composable
internal fun ShareTile(card: @Composable () -> Postcard) {
    var open by remember { mutableStateOf(false) }
    IconTile(Glyphs.share, { open = true }, size = 42.dp)
    if (open) PostcardDialog(card()) { open = false }
}

/** The postcard, big, with buttons to share it or keep it in the gallery. */
@Composable
internal fun PostcardDialog(card: Postcard, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val text = gs(R.string.card_text, Postcards.PLAY)
    val saved = stringResource(R.string.card_saved)
    val failed = stringResource(R.string.card_save_fail)
    val preview by produceState<ImageBitmap?>(null, card.signature) {
        value = withContext(Dispatchers.Default) { Postcards.render(ctx, card, 0.75f).asImageBitmap() }
    }
    var busy by remember { mutableStateOf(false) }
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(Postcards.W / Postcards.H.toFloat()).clip(RoundedCornerShape(18.dp)).background(Night),
                contentAlignment = Alignment.Center,
            ) {
                val img = preview
                if (img != null) Image(img, null, Modifier.fillMaxSize()) else CircularProgressIndicator(color = U.c.accent)
            }
            Gap(14.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelButton(
                    stringResource(R.string.card_share),
                    {
                        busy = true
                        scope.launch {
                            val intent = withContext(Dispatchers.IO) { runCatching { Postcards.shareIntent(ctx, card, text) }.getOrNull() }
                            busy = false
                            if (intent != null) ctx.startActivity(intent)
                        }
                    },
                    Modifier.weight(1f), icon = Glyphs.share, enabled = !busy && preview != null,
                )
                if (Postcards.canSave) {
                    Spacer(Modifier.width(10.dp))
                    PixelButton(
                        stringResource(R.string.card_save),
                        {
                            busy = true
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { Postcards.save(ctx, card) }
                                busy = false
                                Toast.makeText(ctx, if (ok) saved else failed, Toast.LENGTH_SHORT).show()
                            }
                        },
                        kind = ButtonKind.SECONDARY, enabled = !busy && preview != null,
                    )
                }
                Spacer(Modifier.width(10.dp))
                IconTile(Glyphs.close, onDismiss, size = 50.dp)
            }
        }
    }
}

/** Every postcard the tower can make, as little previews. */
@Composable
fun PostcardsScreen(data: WData, go: Go) {
    val cards = allCards(data)
    var open by remember { mutableStateOf<Postcard?>(null) }
    Column(Modifier.fillMaxSize()) {
        TopBar(gs(R.string.cards_title), go::back)
        LazyVerticalGrid(
            GridCells.Fixed(2), Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(2) }) { Text(gs(R.string.cards_sub), style = UType.Muted) }
            items(cards, key = { it.key }) { card -> Thumb(card) { open = card } }
        }
    }
    open?.let { PostcardDialog(it) { open = null } }
}

@Composable
private fun Thumb(card: Postcard, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val img by produceState<ImageBitmap?>(null, card.signature) {
        value = withContext(Dispatchers.Default) { Postcards.render(ctx, card, 0.32f).asImageBitmap() }
    }
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(Postcards.W / Postcards.H.toFloat()).clip(RoundedCornerShape(12.dp)).background(Night),
            contentAlignment = Alignment.Center,
        ) {
            img?.let { Image(it, null, Modifier.fillMaxSize()) }
        }
        Gap(6.dp)
        Text(card.title, style = UType.Small.copy(color = U.c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
