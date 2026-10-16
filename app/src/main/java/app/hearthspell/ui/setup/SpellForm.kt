package app.hearthspell.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.FloorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.content.Spells
import app.hearthspell.data.addFloor
import app.hearthspell.data.addSpell
import app.hearthspell.data.deleteFloor
import app.hearthspell.data.deleteSpell
import app.hearthspell.data.moveFloor
import app.hearthspell.domain.DAY_MS
import app.hearthspell.domain.Mana
import app.hearthspell.ui.Go
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.PixelIcon
import app.hearthspell.ui.components.ButtonKind
import app.hearthspell.ui.components.Chip
import app.hearthspell.ui.components.ConfirmDialog
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.IconTile
import app.hearthspell.ui.components.ManaTag
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelButton
import app.hearthspell.ui.components.PixelField
import app.hearthspell.ui.components.SectionTitle
import app.hearthspell.ui.components.Stepper
import app.hearthspell.ui.components.ToggleRow
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.floorName
import app.hearthspell.ui.gp
import app.hearthspell.ui.roomName
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val quickDays = listOf(1, 2, 3, 7, 14, 30, 90)

/** Creates a spell on [floorId] (id 0) or edits spell [id]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellFormScreen(id: Long, floorId: Long, data: WData, go: Go) {
    val world = data.world
    val container = LocalContext.current.container
    val existing = if (id != 0L) world.spell(id) else null
    if (id != 0L && existing == null) return
    val preset = Spells.of(existing?.presetKey)

    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var floor by rememberSaveable { mutableLongStateOf(existing?.floorId ?: floorId.takeIf { world.floor(it) != null } ?: world.floors.firstOrNull()?.id ?: 0L) }
    var days by rememberSaveable { mutableIntStateOf(existing?.intervalDays ?: 7) }
    var effort by rememberSaveable { mutableIntStateOf(existing?.effort ?: 2) }
    var helper by rememberSaveable { mutableStateOf(existing?.helper ?: HelperKind.BROOM.name) }
    var paused by rememberSaveable { mutableStateOf(existing?.paused ?: false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val canSave = floor != 0L && (name.isNotBlank() || preset != null)

    fun save() {
        if (!canSave) return
        val now = System.currentTimeMillis()
        val e = existing
        container.write {
            if (e != null) {
                upsertSpell(
                    e.copy(
                        name = name.trim(), floorId = floor, intervalDays = days, effort = effort, helper = helper, paused = paused,
                        // Resuming starts the interval afresh rather than waking up to a pile of imps.
                        lastCastAt = if (e.paused && !paused) maxOf(e.lastCastAt, now - (e.intervalDays * DAY_MS) / 2) else e.lastCastAt,
                    )
                )
            } else {
                addSpell(floor, null, now, name = name.trim(), intervalDays = days, effort = effort, helper = helper, freshness = 0.6f)
            }
        }
        go.back()
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).imePadding()) {
        TopBar(stringResource(if (existing == null) R.string.spell_new else R.string.spell_edit), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            SectionTitle(stringResource(R.string.spell_name))
            PixelField(
                name, { name = it.take(60) }, Modifier.fillMaxWidth(),
                placeholder = preset?.let { t(it.name) } ?: stringResource(R.string.spell_name_hint),
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            SectionTitle(stringResource(R.string.spell_floor))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                world.floors.forEach { f -> Chip(floorName(f), f.id == floor, { floor = f.id }) }
            }

            SectionTitle(stringResource(R.string.spell_interval))
            Stepper(days, { days = it }, everyLabel(days), Modifier.fillMaxWidth())
            Gap(10.dp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quickDays.forEach { d -> Chip(gp(R.plurals.spell_days_short, d, d), d == days, { days = d }) }
            }

            SectionTitle(stringResource(R.string.spell_effort))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to R.string.spell_effort_1, 2 to R.string.spell_effort_2, 3 to R.string.spell_effort_3).forEach { (e, label) ->
                    Panel(
                        Modifier.fillMaxWidth(),
                        fill = if (e == effort) U.c.surface2 else U.c.surface,
                        onClick = { effort = e },
                        padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CheckBox(e == effort)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(label), style = UType.Body, modifier = Modifier.weight(1f))
                            ManaTag(Mana.base(e), prefix = "+")
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.spell_helper))
            HelperGrid(world, HelperKind.of(helper)) { helper = it.name }
            Gap(6.dp)
            Text(stringResource(R.string.spell_helper_note), style = UType.Muted)

            if (existing != null) {
                SectionTitle(stringResource(R.string.spell_more))
                Panel(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    ToggleRow(stringResource(R.string.spell_pause), paused, { paused = it }, sub = stringResource(R.string.spell_pause_sub))
                }
            }

            if (existing == null) FromGrimoire(floor, data)

            Gap(18.dp)
            PixelButton(stringResource(R.string.save), ::save, Modifier.fillMaxWidth(), enabled = canSave)
            if (existing != null) {
                Gap(12.dp)
                PixelButton(stringResource(R.string.spell_delete), { confirmDelete = true }, Modifier.fillMaxWidth(), kind = ButtonKind.DANGER, icon = Glyphs.trash)
            }
            Gap(24.dp)
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            stringResource(R.string.spell_delete_title),
            stringResource(R.string.spell_delete_text),
            stringResource(R.string.delete),
            onConfirm = {
                container.write { deleteSpell(existing.id) }
                go.back()
            },
            onDismiss = { confirmDelete = false },
            danger = true,
        )
    }
}

/** Catalog spells of the floor's kind that aren't on it yet: one tap adds each. */
@Composable
private fun FromGrimoire(floorId: Long, data: WData) {
    val world = data.world
    val f = world.floor(floorId) ?: return
    val container = LocalContext.current.container
    val have = world.spellsOf(floorId).mapNotNull { it.presetKey }.toSet()
    val left = Spells.forFloor(FloorKind.of(f.kind)).filter { it.key !in have }
    if (left.isEmpty()) return
    SectionTitle(stringResource(R.string.spell_from_grimoire))
    Panel(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
        left.forEach { p ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    val now = System.currentTimeMillis()
                    container.write { addSpell(floorId, p, now) }
                }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelIcon(Glyphs.plus, U.c.accent, size = 14.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t(p.name), style = UType.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(everyLabel(p.days), style = UType.Small)
                }
                ManaTag(Mana.base(p.effort))
            }
        }
    }
    Gap(4.dp)
    Text(stringResource(R.string.spell_from_grimoire_note), style = UType.Muted)
}

/** Creates a floor (id 0) or edits one: its kind of room and an optional name of its own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloorFormScreen(id: Long, data: WData, go: Go) {
    val world = data.world
    val container = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val existing = if (id != 0L) world.floor(id) else null
    if (id != 0L && existing == null) return

    var kind by rememberSaveable { mutableStateOf(existing?.kind ?: FloorKind.KITCHEN.name) }
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val k = FloorKind.of(kind)

    fun save() {
        val e = existing
        if (e != null) {
            container.write { upsertFloor(e.copy(kind = kind, name = name.trim())) }
            go.back()
        } else if (!busy) {
            busy = true
            scope.launch {
                val newId = withContext(Dispatchers.IO) { container.dao.addFloor(k, name.trim()) }
                go.back()
                go.floor(newId)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).imePadding()) {
        TopBar(stringResource(if (existing == null) R.string.floor_new else R.string.floor_edit), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            SectionTitle(stringResource(R.string.floor_kind))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FloorKind.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { fk -> KindTile(fk, fk.name == kind, { kind = fk.name }, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Gap(10.dp)
            Text(t(k.blurb), style = UType.Muted)

            SectionTitle(stringResource(R.string.floor_name))
            PixelField(
                name, { name = it.take(40) }, Modifier.fillMaxWidth(), placeholder = t(k.magic),
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            Gap(6.dp)
            Text(stringResource(R.string.floor_name_note), style = UType.Muted)

            if (existing == null) {
                Gap(10.dp)
                Text(stringResource(R.string.floor_new_note), style = UType.Muted)
            }

            Gap(18.dp)
            PixelButton(stringResource(R.string.save), ::save, Modifier.fillMaxWidth(), enabled = !busy)
            if (existing != null) {
                Gap(12.dp)
                PixelButton(stringResource(R.string.floor_delete), { confirmDelete = true }, Modifier.fillMaxWidth(), kind = ButtonKind.DANGER, icon = Glyphs.trash)
            }
            Gap(24.dp)
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (confirmDelete && existing != null) {
        val count = world.spellsOf(existing.id).size
        ConfirmDialog(
            stringResource(R.string.floor_delete_title),
            gp(R.plurals.floor_delete_text, count, count),
            stringResource(R.string.delete),
            onConfirm = {
                container.write { deleteFloor(existing.id) }
                go.home()
            },
            onDismiss = { confirmDelete = false },
            danger = true,
        )
    }
}

/** The floors of the tower, top to bottom: reorder, rename, add. */
@Composable
fun FloorsScreen(data: WData, go: Go) {
    val world = data.world
    val container = LocalContext.current.container
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        TopBar(stringResource(R.string.floors_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Text(stringResource(R.string.floors_sub), style = UType.Sub)
            Gap(12.dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                world.floors.forEachIndexed { i, f ->
                    Panel(Modifier.fillMaxWidth(), onClick = { go.floor(f.id) }, padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${world.floors.size - i}", style = UType.Number.copy(color = U.c.faint), modifier = Modifier.width(34.dp))
                            Column(Modifier.weight(1f)) {
                                Text(floorName(f), style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    roomName(f) + " · " + gp(R.plurals.floors_spells, world.spellsOf(f.id).size, world.spellsOf(f.id).size),
                                    style = UType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconTile(Glyphs.up, { container.write { moveFloor(f.id, -1) } }, size = 38.dp, enabled = i > 0)
                            Spacer(Modifier.width(6.dp))
                            IconTile(Glyphs.down, { container.write { moveFloor(f.id, 1) } }, size = 38.dp, enabled = i < world.floors.lastIndex)
                            Spacer(Modifier.width(6.dp))
                            IconTile(Glyphs.pencil, { go.floorForm(f.id) }, size = 38.dp)
                        }
                    }
                }
            }
            Gap(16.dp)
            PixelButton(stringResource(R.string.floors_add), { go.floorForm(0) }, Modifier.fillMaxWidth(), icon = Glyphs.plus)
            Gap(24.dp)
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
