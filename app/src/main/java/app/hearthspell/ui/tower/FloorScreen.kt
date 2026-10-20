package app.hearthspell.ui.tower

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.FloorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.data.Spell
import app.hearthspell.data.cast
import app.hearthspell.data.undoCast
import app.hearthspell.domain.Glow
import app.hearthspell.domain.HelperState
import app.hearthspell.domain.World
import app.hearthspell.ui.Go
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Looks
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.content.ImpKind
import app.hearthspell.ui.Chime
import app.hearthspell.ui.LocalFeel
import app.hearthspell.ui.Tone
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.PixelIcon
import app.hearthspell.ui.art.PixelSprite
import app.hearthspell.ui.art.drawPixels
import app.hearthspell.ui.components.ButtonKind
import app.hearthspell.ui.components.IconTile
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
import app.hearthspell.ui.roomName
import app.hearthspell.ui.spellName
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.Nes
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import app.hearthspell.ui.tips.Tip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A cast that just happened, for the sparkle and "+mana" burst. [chain] counts casts in a row;
 * [grand] is the first cast after coming home.
 */
private data class Burst(val mana: Int, val poof: Boolean, val key: Long, val chain: Int = 0, val grand: Boolean = false)

/** The shared cast flow: feedback, undo and helpers waking up. */
@Stable
private class Caster {
    var burst by mutableStateOf<Burst?>(null)
    var undoId by mutableStateOf<Long?>(null)
    var undoMana by mutableStateOf(0)
    var waking by mutableStateOf<HelperKind?>(null)
    /** The imp just shown the door, for its parting words. */
    var farewell by mutableStateOf<Pair<ImpKind, Long>?>(null)
}

@Composable
private fun rememberCaster(data: WData): Pair<Caster, (Spell) -> Unit> {
    val context = LocalContext.current
    val container = context.container
    val feel = LocalFeel.current
    val scope = rememberCoroutineScope()
    val caster = remember { Caster() }
    val cast: (Spell) -> Unit = { s ->
        val w = data.world
        val mana = w.manaFor(s)
        val kind = HelperKind.of(s.helper)
        val helper = w.helper(kind)
        val step = Chime.nextStep()
        val imp = w.impOf(s)
        val grand = w.homecoming
        when {
            grand -> feel?.tone(Tone.FANFARE, 0.8f)
            imp != null -> { feel?.cast(step); feel?.tone(Tone.POOF, 0f) }
            else -> feel?.cast(step)
        }
        caster.burst = Burst(mana, imp != null, System.nanoTime(), step, grand)
        if (imp != null) caster.farewell = imp to System.nanoTime()
        // The helper wakes on the cast that brings its spells to the first level.
        val key = "helper:${kind.name}"
        if (!helper.awake && HelperState.levelOf(helper.casts + 1) > 0 && key !in data.settings.seen) {
            caster.waking = kind
            container.scope.launch { container.settings.update { it.copy(seen = it.seen + key) } }
        }
        container.scope.launch {
            val id = container.dao.cast(s, w)
            container.changed()
            scope.launch {
                caster.undoMana = mana
                caster.undoId = id
            }
        }
    }
    LaunchedEffect(caster.farewell) {
        if (caster.farewell != null) {
            delay(4500)
            caster.farewell = null
        }
    }
    LaunchedEffect(caster.undoId) {
        if (caster.undoId != null) {
            delay(5000)
            caster.undoId = null
        }
    }
    return caster to cast
}

@Composable
fun FloorScreen(id: Long, data: WData, go: Go) {
    val w = data.world
    val floor = w.floor(id)
    if (floor == null) {
        LaunchedEffect(Unit) { go.back() }
        return
    }
    val (caster, cast) = rememberCaster(data)
    val tick = rememberTicker()
    val sky = Sky(day = !U.c.dark)
    val spells = w.spellsOf(id).sortedWith(compareBy<Spell> { it.paused }.thenBy { w.freshness(it) })
    val kind = FloorKind.of(floor.kind)
    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                TopBar(floorName(floor), go::back, Tip.FLOOR) {
                    if (kind == FloorKind.KITCHEN) {
                        IconTile(Glyphs.flask, go::brew, size = 42.dp, tint = U.c.mana)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (kind == FloorKind.BEDROOM) {
                        IconTile(Glyphs.star, go::stars, size = 42.dp, tint = U.c.accent)
                        Spacer(Modifier.width(8.dp))
                    }
                    IconTile(Glyphs.chest, { go.decor(id) }, size = 42.dp)
                    Spacer(Modifier.width(8.dp))
                    IconTile(Glyphs.pencil, { go.floorForm(id) }, size = 42.dp)
                }
            }
            item {
                Box(Modifier.background(sky.low)) {
                    LiveFloor(data, floor, tick, sky)
                    ManaBurst(caster.burst, Modifier.matchParentSize())
                }
            }
            item {
                val imps = w.imps(id)
                val dust = w.dust(id)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(roomName(floor), style = UType.Sub, modifier = Modifier.weight(1f))
                    val impKinds = w.impsOn(id)
                    val (label, color) = when {
                        imps == 1 && impKinds.size == 1 -> gs(R.string.floor_imp_named, t(impKinds[0].title)) to U.c.imp
                        imps > 0 -> gp(R.plurals.floor_imps_here, imps, imps) to U.c.imp
                        dust > 0.5f -> gs(R.string.floor_dusty) to U.c.muted
                        else -> gs(R.string.floor_spotless) to U.c.good
                    }
                    Text(label, style = UType.Muted.copy(color = color))
                }
            }
            item {
                SectionTitle(gs(R.string.floor_spells), Modifier.padding(horizontal = 16.dp), action = "+ " + gs(R.string.floor_add_spell)) {
                    go.spellForm(0, id)
                }
            }
            if (spells.isEmpty()) {
                item {
                    Panel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Text(gs(R.string.floor_empty), style = UType.Body)
                        Spacer(Modifier.height(12.dp))
                        PixelButton(gs(R.string.floor_add_spell), { go.spellForm(0, id) }, icon = Glyphs.plus)
                    }
                }
            }
            items(spells, key = { it.id }) { s ->
                SpellRow(s, w, Modifier.padding(horizontal = 16.dp, vertical = 5.dp), onCast = { cast(s) }, onOpen = { go.spell(s.id) }, onEdit = { go.spellForm(s.id) })
            }
            if (spells.isNotEmpty()) {
                item { Text(gs(R.string.floor_hint), style = UType.Small, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter)) {
            Farewell(caster)
            UndoBar(caster)
        }
        WakeDialog(caster)
    }
}

@Composable
fun CantripsScreen(data: WData, go: Go) {
    val w = data.world
    val (caster, cast) = rememberCaster(data)
    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item { TopBar(gs(R.string.cantrips_title), go::back, Tip.CANTRIPS) }
            item {
                Box {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PixelSprite(Art.helper(HelperKind.CANDLE, 0), 2.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(gs(R.string.cantrips_sub), style = UType.Sub, modifier = Modifier.weight(1f))
                    }
                    ManaBurst(caster.burst, Modifier.matchParentSize())
                }
            }
            if (w.cantrips.isEmpty()) {
                item { Panel(Modifier.fillMaxWidth().padding(16.dp)) { Text(gs(R.string.cantrips_empty), style = UType.Body) } }
            }
            items(w.cantrips, key = { it.id }) { s ->
                SpellRow(
                    s, w, Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    floorLabel = floorName(w.floor(s.floorId)),
                    onCast = { cast(s) }, onOpen = { go.spell(s.id) }, onEdit = { go.spellForm(s.id) },
                )
            }
        }
        Column(Modifier.align(Alignment.BottomCenter)) {
            Farewell(caster)
            UndoBar(caster)
        }
        WakeDialog(caster)
    }
}

@Composable
private fun glowColor(g: Glow): Color = when (g) {
    Glow.BRIGHT -> U.c.good
    Glow.FADING -> U.c.accent
    Glow.DUSTY -> Nes.orange
    Glow.IMPS -> U.c.imp
}

@Composable
private fun status(s: Spell, w: World): String = when {
    s.paused -> gs(R.string.tower_spell_paused)
    w.overdueDays(s) > 0 -> gp(R.plurals.spell_late, w.overdueDays(s), w.overdueDays(s))
    w.glow(s) == Glow.IMPS -> gs(R.string.spell_overdue)
    w.daysLeft(s) == 0 -> gs(R.string.spell_due_today)
    else -> gp(R.plurals.spell_due_in, w.daysLeft(s), w.daysLeft(s))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpellRow(
    s: Spell,
    w: World,
    modifier: Modifier = Modifier,
    floorLabel: String? = null,
    onCast: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    val c = U.c
    val glow = w.glow(s)
    val helper = w.helper(HelperKind.of(s.helper))
    val request = w.isPendingRequest(s.id)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val name = spellName(s)
    Row(
        modifier
            .fillMaxWidth()
            .offset { IntOffset(0, if (pressed) PX.roundToPx() else 0) }
            .pixelBox(c.surface, c.outline, c.bevel, c.outline.copy(alpha = 0.35f), pressed)
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onCast, onLongClick = onOpen)
            .padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelSprite(if (helper.awake) Art.helper(helper.kind, 0) else Art.helperAsleep(helper.kind), 1.4.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = UType.Strong, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t(w.rank(s).title), style = UType.Small.copy(color = c.accent))
                if (floorLabel != null) Text(" · $floorLabel", style = UType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (request) {
                    Spacer(Modifier.width(8.dp))
                    PixelIcon(Glyphs.star, c.mana, size = 11.dp)
                    Spacer(Modifier.width(3.dp))
                    Text(gs(R.string.spell_request), style = UType.Small.copy(color = c.mana), maxLines = 1)
                }
            }
            Spacer(Modifier.height(6.dp))
            PixelBar(if (s.paused) 1f else w.freshness(s), Modifier.fillMaxWidth(), color = if (s.paused) c.faint else glowColor(glow), height = 11.dp)
            Spacer(Modifier.height(4.dp))
            Text(status(s, w), style = UType.Muted.copy(color = if (glow == Glow.IMPS && !s.paused) c.imp else c.muted))
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val desc = gs(R.string.spell_cast_a11y, name)
            Box(
                Modifier
                    .size(48.dp)
                    .pixelBox(if (request) c.mana else c.accent, c.outline, Color.White.copy(alpha = 0.35f), c.accentDeep)
                    .combinedClickable(onClick = onCast, onLongClick = onOpen)
                    .semantics { contentDescription = desc },
                contentAlignment = Alignment.Center,
            ) { PixelIcon(Glyphs.check, c.onAccent, size = 20.dp) }
            Spacer(Modifier.height(4.dp))
            Text(gs(R.string.cast_mana, w.manaFor(s)), style = UType.Small.copy(color = c.mana))
        }
        Spacer(Modifier.width(6.dp))
        IconTile(Glyphs.pencil, onEdit, size = 30.dp, fill = c.surface)
    }
}

/** Sparkles bursting out, a puff where the imp was, and "+N" rising and fading. */
@Composable
private fun ManaBurst(burst: Burst?, modifier: Modifier = Modifier) {
    if (burst == null) return
    val p = remember(burst.key) { Animatable(0f) }
    LaunchedEffect(burst.key) { p.animateTo(1f, tween(1100, easing = LinearEasing)) }
    val v = p.value
    if (v >= 1f) return
    val mana = U.c.mana
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val cell = PX.toPx() * 1.5f
            val cx = size.width / 2
            val cy = size.height * 0.55f
            val r = v * size.height * 0.45f
            val alpha = (1f - v).coerceIn(0f, 1f)
            val n = if (burst.grand) 28 else 12 + burst.chain.coerceAtMost(8)
            if (burst.grand) {
                // Confetti raining over the whole floor.
                for (i in 0 until 40) {
                    val h = (i * 7919 + 13) % 1000
                    val x = size.width * (h / 1000f)
                    val y = size.height * (v * 1.2f + (h % 97) / 300f) - size.height * 0.2f
                    val col = listOf(Nes.yellow, Nes.pink, Nes.sky, Nes.leaf, Nes.orange)[i % 5]
                    drawRect(col, Offset(x, y), Size(cell * 1.4f, cell * 1.4f), alpha = alpha)
                }
            }
            for (i in 0 until n) {
                val a = i * (Math.PI * 2 / n) + v
                val x = cx + (Math.cos(a) * r * (1 + (i % 3) * 0.25f)).toFloat()
                val y = cy + (Math.sin(a) * r * 0.7f).toFloat()
                val col = when (i % 3) { 0 -> Nes.yellow; 1 -> mana; else -> Nes.white }
                // A four-point pixel star.
                drawRect(col, Offset(x - cell / 2, y - cell * 1.5f), Size(cell, cell * 3), alpha = alpha)
                drawRect(col, Offset(x - cell * 1.5f, y - cell / 2), Size(cell * 3, cell), alpha = alpha)
            }
            if (burst.poof) {
                val px = size.width * 0.3f
                val py = size.height * 0.6f
                for (i in 0 until 8) {
                    val a = i * (Math.PI * 2 / 8)
                    val d = v * size.height * 0.18f
                    val s = cell * (2.5f - v * 1.5f)
                    drawRect(Nes.grey, Offset(px + (Math.cos(a) * d).toFloat() - s / 2, py + (Math.sin(a) * d).toFloat() - s / 2), Size(s, s), alpha = alpha * 0.9f)
                }
            }
        }
        Column(
            Modifier.graphicsLayer { translationY = -v * 60.dp.toPx(); this.alpha = (1.4f - v).coerceIn(0f, 1f) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (burst.grand) {
                Text(gs(R.string.home_grand), style = UType.Heading.copy(color = Nes.yellow), textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelIcon(Glyphs.mana, mana, size = 22.dp)
                Spacer(Modifier.width(6.dp))
                Text(gs(R.string.cast_mana, burst.mana), style = UType.Display.copy(color = mana, fontSize = 20.sp))
            }
            if (burst.chain >= 2) Text(gs(R.string.cast_chain, burst.chain + 1), style = UType.Eyebrow.copy(color = Nes.yellow))
        }
    }
}

@Composable
private fun Farewell(caster: Caster) {
    val (imp, _) = caster.farewell ?: return
    val tick = rememberTicker(300)
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .fillMaxWidth()
            .pixelBox(U.c.surface2, U.c.outline, U.c.bevel)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpriteImage(Looks.imp(imp, (tick.value % 2).toInt()), 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t(imp.title), style = UType.Eyebrow.copy(color = U.c.imp))
            Spacer(Modifier.height(4.dp))
            Text(t(imp.farewell), style = UType.Body)
        }
    }
}

@Composable
private fun UndoBar(caster: Caster, modifier: Modifier = Modifier) {
    val id = caster.undoId ?: return
    val container = LocalContext.current.container
    Row(
        modifier
            .navigationBarsPadding()
            .padding(16.dp)
            .fillMaxWidth()
            .pixelBox(U.c.surface2, U.c.outline, U.c.bevel)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelIcon(Glyphs.star, U.c.accent, size = 16.dp)
        Spacer(Modifier.width(10.dp))
        Text(gs(R.string.cast_done), style = UType.Body, modifier = Modifier.weight(1f))
        Text(gs(R.string.cast_mana, caster.undoMana), style = UType.Body.copy(color = U.c.mana))
        Spacer(Modifier.width(10.dp))
        PixelButton(gs(R.string.undo), {
            caster.undoId = null
            container.write { undoCast(id) }
        }, kind = ButtonKind.SECONDARY)
    }
}

@Composable
private fun WakeDialog(caster: Caster) {
    val kind = caster.waking ?: return
    val tick = rememberTicker(400)
    AlertDialog(
        onDismissRequest = { caster.waking = null },
        containerColor = U.c.surface,
        shape = RectangleShape,
        title = { Text(gs(R.string.helper_woke_title), style = UType.Heading, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                    // Redraws only this canvas as the helper bobs.
                    Canvas(Modifier.size(110.dp)) {
                        val s = Art.helper(kind, (tick.value % 2).toInt())
                        val px = kotlin.math.floor(minOf(size.width / s.w, size.height / s.h))
                        val ox = (size.width - s.w * px) / 2
                        val oy = (size.height - s.h * px) / 2
                        drawRect(Nes.amber, Offset(ox - px * 2, oy + s.h * px - px), Size((s.w + 4) * px, px), alpha = 0.4f)
                        drawPixels(s, ox, oy, px)
                    }
                }
                Text(t(kind.title), style = UType.Title, textAlign = TextAlign.Center)
                Text(gs(R.string.helper_woke_text, t(kind.title), t(kind.blurb)), style = UType.Body, textAlign = TextAlign.Center)
            }
        },
        confirmButton = { PixelButton(gs(R.string.helper_woke_ok), { caster.waking = null }, Modifier.fillMaxWidth()) },
    )
}
