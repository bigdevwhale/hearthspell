package app.hearthspell.ui.decor

import app.hearthspell.data.DECOR_SLOTS
import app.hearthspell.ui.tips.Tip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.DecorKind
import app.hearthspell.data.Decor
import app.hearthspell.data.Floor
import app.hearthspell.domain.World
import app.hearthspell.ui.Go
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.ui.components.ButtonKind
import app.hearthspell.ui.components.Chip
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.ManaTag
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelButton
import app.hearthspell.ui.components.SectionTitle
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.floorName
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import kotlinx.coroutines.delay

/** How many things fit on one floor of the tower. */
private const val SLOTS = DECOR_SLOTS

private fun World.freeSlot(floorId: Long): Int? {
    val used = decorOn(floorId).map { it.slot }.toSet()
    return (0 until SLOTS).firstOrNull { it !in used }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DecorScreen(floorId: Long, data: WData, go: Go) {
    val w = data.world
    val container = LocalContext.current.container
    // Where new purchases go: a floor, or 0 for the chest.
    var target by rememberSaveable { mutableLongStateOf(if (w.floor(floorId) != null) floorId else 0L) }
    var bought by remember { mutableStateOf<DecorKind?>(null) }
    var placing by remember { mutableStateOf<Decor?>(null) }
    LaunchedEffect(bought) {
        if (bought != null) {
            delay(2200)
            bought = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.decor_title), go::back, Tip.DECOR) { ManaTag(w.mana, Modifier.padding(end = 4.dp)) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            bought?.let {
                Panel(Modifier.fillMaxWidth(), fill = U.c.surface2) {
                    Text(stringResource(R.string.decor_bought, t(it.title)), style = UType.Body.copy(color = U.c.good))
                }
                Gap(8.dp)
            }

            SectionTitle(stringResource(R.string.decor_where))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                w.floors.forEach { f ->
                    val n = w.decorOn(f.id).size
                    Chip(floorName(f) + if (n >= SLOTS) " · " + stringResource(R.string.decor_full) else " · $n/$SLOTS", target == f.id, { target = f.id })
                }
                Chip(stringResource(R.string.decor_chest), target == 0L, { target = 0L })
            }

            SectionTitle(stringResource(R.string.decor_title))
            DecorKind.entries.filter { it.forSale(w.seasons) }.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { kind ->
                        ShopCell(kind, w, Modifier.weight(1f)) {
                            val into = if (target != 0L && w.freeSlot(target) != null) target else 0L
                            val slot = if (into == 0L) 0 else w.freeSlot(into) ?: 0
                            if (w.canAfford(kind)) {
                                container.write {
                                    upsertDecor(Decor(kind = kind.name, floorId = into, slot = slot, cost = kind.cost, boughtAt = System.currentTimeMillis()))
                                }
                                bought = kind
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Gap(8.dp)
            }

            SectionTitle(stringResource(R.string.decor_chest) + " · " + stringResource(R.string.decor_chest_sub))
            val chest = w.decor.filter { it.floorId == 0L || w.floor(it.floorId) == null }
            if (chest.isEmpty()) Text(stringResource(R.string.decor_chest_empty), style = UType.Sub)
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chest.forEach { d -> Item(d) { placing = d } }
            }

            val placedFloors = w.floors.filter { w.decorOn(it.id).isNotEmpty() }
            if (placedFloors.isNotEmpty()) {
                SectionTitle(stringResource(R.string.decor_placed))
                placedFloors.forEach { f ->
                    Text(floorName(f), style = UType.Eyebrow.copy(color = U.c.accent), modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        w.decorOn(f.id).forEach { d -> Item(d) { placing = d } }
                    }
                }
            }
            Gap(28.dp)
        }
    }

    placing?.let { d ->
        PlaceDialog(
            d, w,
            onDismiss = { placing = null },
            onPlace = { f ->
                val slot = if (f == null) 0 else w.freeSlot(f.id) ?: 0
                container.write { upsertDecor(d.copy(floorId = f?.id ?: 0L, slot = slot)) }
                placing = null
            },
        )
    }
}

@Composable
private fun ShopCell(kind: DecorKind, w: World, modifier: Modifier, onBuy: () -> Unit) {
    val c = U.c
    val afford = w.canAfford(kind)
    val owned = w.decor.count { it.kind == kind.name }
    Column(
        modifier
            .pixelBox(c.surface, c.outline, c.bevel)
            .clickable(enabled = afford, onClick = onBuy)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpriteImage(Art.decor(kind), 52.dp, alpha = if (afford) 1f else 0.45f)
        if (kind.season != null) Text(t(kind.season.title), style = UType.Small.copy(color = c.mana))
        Gap(6.dp)
        Text(t(kind.title), style = UType.Small.copy(color = if (afford) c.ink else c.faint), maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Gap(4.dp)
        if (afford) ManaTag(kind.cost)
        else Text(stringResource(R.string.decor_need, kind.cost - w.mana), style = UType.Small.copy(color = c.faint), textAlign = TextAlign.Center)
        if (owned > 0) Text(stringResource(R.string.decor_owned, owned), style = UType.Small.copy(color = c.muted))
    }
}

@Composable
private fun Item(d: Decor, onClick: () -> Unit) {
    val kind = DecorKind.of(d.kind) ?: return
    Column(
        Modifier.width(76.dp).pixelBox(U.c.surface2, U.c.outline, U.c.bevel).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpriteImage(Art.decor(kind), 40.dp)
        Gap(4.dp)
        Text(t(kind.title), style = UType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceDialog(d: Decor, w: World, onDismiss: () -> Unit, onPlace: (Floor?) -> Unit) {
    val kind = DecorKind.of(d.kind)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface,
        shape = RectangleShape,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (kind != null) SpriteImage(Art.decor(kind), 36.dp)
                Spacer(Modifier.width(10.dp))
                Text(kind?.let { t(it.title) }.orEmpty(), style = UType.Heading)
            }
        },
        text = {
            Column {
                Text(stringResource(R.string.decor_where), style = UType.Muted)
                Gap(8.dp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    w.floors.forEach { f ->
                        val here = d.floorId == f.id
                        val free = here || w.freeSlot(f.id) != null
                        Chip(floorName(f) + if (!free) " · " + stringResource(R.string.decor_full) else "", here, { if (free && !here) onPlace(f) })
                    }
                }
            }
        },
        confirmButton = { PixelButton(stringResource(R.string.cancel), onDismiss, kind = ButtonKind.SECONDARY) },
        dismissButton = if (d.floorId != 0L) {
            { PixelButton(stringResource(R.string.decor_to_chest), { onPlace(null) }, kind = ButtonKind.SECONDARY) }
        } else null,
    )
}
