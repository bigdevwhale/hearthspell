package app.hearthspell.ui.tower

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hearthspell.R
import app.hearthspell.content.FloorKind
import app.hearthspell.data.Floor
import app.hearthspell.domain.World
import app.hearthspell.ui.Go
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.PixelIcon
import app.hearthspell.ui.art.PixelSprite
import app.hearthspell.ui.components.ButtonKind
import app.hearthspell.ui.components.IconTile
import app.hearthspell.ui.components.ManaTag
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelButton
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.floorName
import app.hearthspell.ui.gp
import app.hearthspell.ui.gs
import app.hearthspell.ui.roomName
import app.hearthspell.ui.spellName
import app.hearthspell.ui.theme.Nes
import app.hearthspell.ui.theme.PressStart
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import kotlin.math.floor

/** What a floor looks like right now, from the world. */
internal fun World.look(f: Floor): FloorLook = FloorLook(
    id = f.id,
    kind = FloorKind.of(f.kind),
    dust = dust(f.id),
    imps = imps(f.id),
    helpers = helpersOn(f.id).map { it.kind },
    decor = decorOn(f.id),
)

@Composable
fun TowerTab(data: WData, go: Go) {
    val w = data.world
    val tick = rememberTicker()
    val sky = Sky(day = !U.c.dark)
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().background(sky.low), contentPadding = PaddingValues(bottom = 8.dp)) {
        item { TowerHeader(data, go) }
        item { Requests(data, go) }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                PixelButton(gs(R.string.tower_low_energy), go::cantrips, Modifier.weight(1f), kind = ButtonKind.SECONDARY, icon = Glyphs.moon)
                Spacer(Modifier.width(10.dp))
                Text(gs(R.string.tower_today, w.castsToday.size), style = UType.Muted.copy(color = if (sky.day) Nes.night else U.c.muted))
            }
        }
        item {
            val roof = ImageBitmap.imageResource(R.drawable.hs_roof)
            SceneCanvas(ROOF_ROWS, Modifier.padding(top = 6.dp)) { drawRoof(roof, tick.value, sky) }
        }
        if (w.floors.isEmpty()) {
            item { EmptyTower(go) }
        }
        items(w.floors, key = { it.id }) { f -> FloorCard(f, w, tick, sky) { go.floor(f.id) } }
        item { AddFloor(tick, sky) { go.floorForm(0) } }
        item {
            val base = ImageBitmap.imageResource(R.drawable.hs_base)
            SceneCanvas(BASE_ROWS) { drawBase(base, tick.value, sky) }
        }
    }
    // Keeps the scrolled tower from showing through the status bar icons.
    Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(sky.low.copy(alpha = 0.94f)))
    }
}

@Composable
private fun TowerHeader(data: WData, go: Go) {
    val w = data.world
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelSprite(Art.apprentice(0), 1.5.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            val name = data.settings.name
            Text(
                if (name.isBlank()) gs(R.string.tower_apprentice_plain) else gs(R.string.tower_apprentice, name),
                style = UType.Heading, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(gs(R.string.tower_circle, w.circle).uppercase(), style = UType.Eyebrow.copy(color = U.c.accent))
                Spacer(Modifier.width(12.dp))
                ManaTag(w.mana)
                Spacer(Modifier.width(12.dp))
                PixelIcon(Glyphs.imp, if (w.totalImps > 0) U.c.imp else U.c.faint, size = 14.dp)
                Spacer(Modifier.width(5.dp))
                Text("${w.totalImps}", style = UType.Number.copy(fontSize = 10.sp, color = if (w.totalImps > 0) U.c.imp else U.c.faint))
            }
        }
        IconTile(Glyphs.gear, go::settings, size = 42.dp)
    }
}

@Composable
private fun Requests(data: WData, go: Go) {
    val w = data.world
    Panel(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelSprite(Art.master(0), 0.75.dp)
            Spacer(Modifier.width(10.dp))
            Text(gs(R.string.tower_requests).uppercase(), style = UType.Eyebrow.copy(color = U.c.accent), modifier = Modifier.weight(1f))
            Text(gs(R.string.tower_double), style = UType.Eyebrow.copy(color = U.c.mana))
        }
        Spacer(Modifier.size(6.dp))
        val reqs = w.requests
        if (reqs.isEmpty()) {
            Text(gs(R.string.tower_requests_none), style = UType.Muted)
        } else {
            reqs.forEach { s ->
                val done = w.requestDone(s.id)
                Row(
                    Modifier.fillMaxWidth().clickable { go.floor(s.floorId) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(20.dp).pixelBox(if (done) U.c.good else U.c.surface2, U.c.outline),
                        contentAlignment = Alignment.Center,
                    ) { if (done) PixelIcon(Glyphs.check, U.c.outline, size = 11.dp) }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        spellName(s),
                        style = UType.Body.copy(color = if (done) U.c.faint else U.c.ink),
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(floorName(w.floor(s.floorId)), style = UType.Small, maxLines = 1)
                }
            }
            if (reqs.all { w.requestDone(it.id) }) {
                Spacer(Modifier.size(4.dp))
                Text(gs(R.string.tower_requests_done), style = UType.Muted.copy(color = U.c.good))
            }
        }
    }
}

/** One floor of the tower with its name on a plaque. */
@Composable
private fun FloorCard(f: Floor, w: World, tick: State<Long>, sky: Sky, onClick: () -> Unit) {
    val look = w.look(f)
    BoxWithConstraints(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        val density = LocalDensity.current
        val px = cellPx(constraints.maxWidth.toFloat())
        val ox = floor((constraints.maxWidth - GW * px) / 2)
        val bg = ImageBitmap.imageResource(SCENES.getValue(look.kind).res)
        SceneCanvas(SCENE_H) { drawFloor(look, bg, tick.value, sky) }
        val x = with(density) { (ox + (TX + 10) * px).toDp() }
        val y = with(density) { (8 * px).toDp() }
        Column(
            Modifier
                .offset(x, y)
                .widthIn(max = with(density) { (46 * px).toDp() })
                .background(Color(0xB307060F))
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(floorName(f), style = UType.Eyebrow.copy(color = Nes.cream, fontSize = 8.sp), maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(roomName(f), style = UType.Small.copy(color = Nes.sand), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (look.imps > 0) {
                    Spacer(Modifier.width(6.dp))
                    PixelIcon(Glyphs.imp, Nes.pink, size = 10.dp)
                    Text("${look.imps}", style = UType.Small.copy(color = Nes.pink))
                }
            }
        }
    }
}

@Composable
private fun AddFloor(tick: State<Long>, sky: Sky, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        SceneCanvas(SCENE_H / 2) { drawScaffold(tick.value, sky) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelIcon(Glyphs.plus, Nes.amber, size = 14.dp)
            Spacer(Modifier.width(8.dp))
            Text(gs(R.string.tower_add_floor).uppercase(), style = UType.Button.copy(color = Nes.amber, fontFamily = PressStart))
        }
    }
}

@Composable
private fun EmptyTower(go: Go) {
    Column(
        Modifier.fillMaxWidth().background(Nes.night).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PixelSprite(Art.helper(app.hearthspell.content.HelperKind.BROOM, 0), 3.dp)
        Text(gs(R.string.tower_empty_title), style = UType.Title.copy(color = Nes.cream))
        Text(gs(R.string.tower_empty_text), style = UType.Sub.copy(color = Nes.lavender))
        PixelButton(gs(R.string.tower_add_floor), { go.floorForm(0) }, icon = Glyphs.plus)
    }
}

/** Plural helper kept here so the tower strings stay in one place. */
@Composable
internal fun impsLabel(n: Int): String = if (n == 0) gs(R.string.tower_no_imps) else gp(R.plurals.tower_imps, n, n)
