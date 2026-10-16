package app.hearthspell.ui.market

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.Aisle
import app.hearthspell.content.AisleGuess
import app.hearthspell.content.HelperKind
import app.hearthspell.content.Spells
import app.hearthspell.data.Reagent
import app.hearthspell.data.finishTrip
import app.hearthspell.data.need
import app.hearthspell.ui.Go
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.PixelIcon
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.ui.components.ButtonKind
import app.hearthspell.ui.components.Chip
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.IconTile
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelBar
import app.hearthspell.ui.components.PixelButton
import app.hearthspell.ui.components.PixelField
import app.hearthspell.ui.components.SectionTitle
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.gs
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// The shopping list as reagents for the tower, and a one-handed checklist for the trip itself.

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarketTab(data: WData, go: Go) {
    val w = data.world
    val c = LocalContext.current.container
    var text by rememberSaveable { mutableStateOf("") }
    // Null follows the guess; set when the user picks a stall by hand.
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    val aisle = picked?.let { Aisle.of(it) } ?: AisleGuess.guess(text)
    var choosing by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Reagent?>(null) }

    fun add() {
        val name = text.trim()
        if (name.isEmpty()) return
        val a = aisle
        c.write { need(name, a) }
        text = ""
        picked = null
    }

    val list = w.shoppingList
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.market_title), null)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.market_sub), style = UType.Sub)
            Gap(12.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelField(
                    text, { text = it }, Modifier.weight(1f),
                    placeholder = stringResource(R.string.market_add_hint),
                    keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.width(8.dp))
                IconTile(Glyphs.plus, { add() }, tint = U.c.onAccent, fill = U.c.accent, size = 50.dp, enabled = text.isNotBlank())
            }
            if (text.isNotBlank()) {
                Gap(8.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.market_choose_aisle) + " ", style = UType.Muted)
                    Chip(t(aisle.title), true, { choosing = true })
                }
            }

            if (list.isEmpty()) {
                Gap(18.dp)
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        SpriteImage(Art.helper(HelperKind.SATCHEL), 96.dp)
                        Gap(10.dp)
                        Text(stringResource(R.string.market_empty_title).uppercase(), style = UType.Heading, textAlign = TextAlign.Center)
                        Gap(6.dp)
                        Text(gs(R.string.market_empty_text), style = UType.Sub, textAlign = TextAlign.Center)
                    }
                }
            } else {
                SectionTitle(stringResource(R.string.market_list) + " · " + pluralStringResource(R.plurals.market_count, list.size, list.size))
                byAisle(list).forEach { (a, items) ->
                    AisleHeader(a)
                    items.forEach { r ->
                        ListRow(r, onEdit = { editing = r }) { c.write { upsertReagent(r.copy(needed = false, inBasket = false)) } }
                        Gap(6.dp)
                    }
                }
                Gap(12.dp)
                PixelButton(stringResource(R.string.market_go), go::trip, Modifier.fillMaxWidth(), icon = Glyphs.basket)
            }

            val low = w.runningLow
            if (low.isNotEmpty()) {
                SectionTitle(stringResource(R.string.market_low))
                low.take(8).forEach { r ->
                    Panel(Modifier.fillMaxWidth(), onClick = { c.write { need(r.name, Aisle.of(r.aisle)) } }, padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.name, style = UType.Body)
                                val d = r.avgGapDays.roundToInt().coerceAtLeast(1)
                                Text(pluralStringResource(R.plurals.market_low_sub, d, d), style = UType.Small)
                            }
                            PixelIcon(Glyphs.plus, U.c.accent, size = 18.dp)
                        }
                    }
                    Gap(6.dp)
                }
            }

            val lowIds = low.map { it.id }.toSet()
            val usual = w.reagents.filter { !it.needed && it.id !in lowIds && it.boughtCount > 0 }
                .sortedWith(compareByDescending<Reagent> { it.boughtCount }.thenBy { it.name.lowercase() })
                .take(16)
            if (usual.isNotEmpty()) {
                SectionTitle(stringResource(R.string.market_usual))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    usual.forEach { r -> Chip("+ " + r.name, false, { c.write { need(r.name, Aisle.of(r.aisle)) } }) }
                }
            }
            Gap(28.dp)
        }
    }

    if (choosing) AisleDialog(aisle, onDismiss = { choosing = false }) { picked = it.name; choosing = false }
    editing?.let { r ->
        ReagentDialog(
            r,
            onDismiss = { editing = null },
            onAisle = { a -> c.write { upsertReagent(r.copy(aisle = a.name)) }; editing = null },
            onForget = { c.write { deleteReagent(r.id) }; editing = null },
        )
    }
}

private fun byAisle(items: List<Reagent>): List<Pair<Aisle, List<Reagent>>> =
    items.groupBy { Aisle.of(it.aisle) }.toSortedMap(compareBy { it.ordinal }).map { (a, rs) -> a to rs.sortedBy { it.name.lowercase() } }

@Composable
private fun AisleHeader(a: Aisle) {
    Row(Modifier.padding(top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(t(a.title).uppercase(), style = UType.Eyebrow.copy(color = U.c.accent))
        Text("  " + t(a.shop), style = UType.Small)
    }
}

@Composable
private fun ListRow(r: Reagent, onEdit: () -> Unit, onRemove: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), padding = PaddingValues(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.name, style = UType.Body, modifier = Modifier.weight(1f).clickable(onClick = onEdit).padding(vertical = 6.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            IconTile(Glyphs.close, onRemove, size = 38.dp, tint = U.c.muted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AisleDialog(current: Aisle, onDismiss: () -> Unit, onPick: (Aisle) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface,
        shape = RectangleShape,
        title = { Text(stringResource(R.string.market_choose_aisle), style = UType.Heading) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Aisle.entries.forEach { a -> Chip(t(a.title), a == current, { onPick(a) }) }
            }
        },
        confirmButton = { PixelButton(stringResource(R.string.cancel), onDismiss, kind = ButtonKind.SECONDARY) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReagentDialog(r: Reagent, onDismiss: () -> Unit, onAisle: (Aisle) -> Unit, onForget: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface,
        shape = RectangleShape,
        title = { Text(r.name, style = UType.Strong) },
        text = {
            Column {
                Text(stringResource(R.string.market_choose_aisle), style = UType.Muted)
                Gap(8.dp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Aisle.entries.forEach { a -> Chip(t(a.title), a.name == r.aisle, { onAisle(a) }) }
                }
            }
        },
        confirmButton = { PixelButton(stringResource(R.string.done), onDismiss, kind = ButtonKind.SECONDARY) },
        dismissButton = { PixelButton(stringResource(R.string.market_forget), onForget, kind = ButtonKind.DANGER, icon = Glyphs.trash) },
    )
}

// The trip.

/** What a finished trip brought home. */
private data class TripResult(val bought: Int, val mana: Int)

@Composable
fun TripScreen(data: WData, go: Go) {
    val w = data.world
    val container = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<TripResult?>(null) }
    val list = w.shoppingList.sortedWith(compareBy<Reagent> { Aisle.of(it.aisle).ordinal }.thenBy { it.name.lowercase() })
    val inBasket = list.count { it.inBasket }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.trip_title), go::back)
        val done = result
        if (done != null) {
            Column(
                Modifier.fillMaxSize().padding(24.dp).navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                SpriteImage(Art.helper(HelperKind.SATCHEL, 1), 140.dp)
                Gap(16.dp)
                if (done.bought > 0) {
                    Text(gs(R.string.trip_welcome), style = UType.Heading, textAlign = TextAlign.Center)
                    Gap(10.dp)
                    Text(stringResource(R.string.trip_bought, done.bought), style = UType.Sub)
                    if (done.mana > 0) {
                        Gap(10.dp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PixelIcon(Glyphs.mana, U.c.mana, size = 16.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.trip_mana, done.mana), style = UType.Sub.copy(color = U.c.mana))
                        }
                    }
                } else {
                    Text(gs(R.string.trip_none_bought), style = UType.Sub, textAlign = TextAlign.Center)
                }
                Gap(28.dp)
                PixelButton(stringResource(R.string.trip_back), go::back, Modifier.fillMaxWidth())
            }
            return@Column
        }
        if (list.isEmpty()) {
            Text(gs(R.string.trip_empty), style = UType.Sub, modifier = Modifier.padding(16.dp))
            return@Column
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.trip_progress, inBasket, list.size), style = UType.Strong)
            Gap(8.dp)
            PixelBar(inBasket.toFloat() / list.size, Modifier.fillMaxWidth(), color = U.c.good, segments = list.size.coerceIn(4, 16), height = 16.dp)
            Gap(6.dp)
            Text(gs(R.string.trip_hint), style = UType.Small)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            var last: Aisle? = null
            list.forEach { r ->
                val a = Aisle.of(r.aisle)
                if (a != last) {
                    AisleHeader(a)
                    last = a
                }
                TripRow(r) { container.write { upsertReagent(r.copy(inBasket = !r.inBasket)) } }
                Gap(8.dp)
            }
            Gap(12.dp)
        }
        Box(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
            PixelButton(
                stringResource(R.string.trip_done),
                {
                    val market = w.spells.firstOrNull { it.presetKey == Spells.MARKET }
                    val mana = if (market != null && inBasket > 0) w.manaFor(market) else 0
                    scope.launch {
                        val n = container.dao.finishTrip(w)
                        container.changed()
                        result = TripResult(n, mana)
                    }
                },
                Modifier.fillMaxWidth(),
                kind = if (inBasket == list.size) ButtonKind.PRIMARY else ButtonKind.SECONDARY,
                icon = Glyphs.check,
            )
        }
    }
}

@Composable
private fun TripRow(r: Reagent, onToggle: () -> Unit) {
    val c = U.c
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .pixelBox(if (r.inBasket) c.surface else c.surface2, c.outline, c.bevel)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).pixelBox(if (r.inBasket) c.good else c.bg, c.outline), contentAlignment = Alignment.Center) {
            if (r.inBasket) PixelIcon(Glyphs.check, c.outline, size = 20.dp)
        }
        Spacer(Modifier.width(16.dp))
        Text(
            r.name,
            style = UType.Strong.copy(color = if (r.inBasket) c.faint else c.ink),
            modifier = Modifier.weight(1f),
        )
    }
}
