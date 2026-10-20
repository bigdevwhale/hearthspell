package app.hearthspell.ui.extras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.Garment
import app.hearthspell.data.Settings
import app.hearthspell.data.buy
import app.hearthspell.domain.World
import app.hearthspell.ui.Go
import app.hearthspell.ui.LocalFeel
import app.hearthspell.ui.Tone
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Looks
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.ui.art.drawPixels
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.ManaTag
import app.hearthspell.ui.components.SectionTitle
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.gs
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import app.hearthspell.ui.tips.Tip
import app.hearthspell.ui.tower.rememberTicker
import kotlinx.coroutines.launch
import kotlin.math.floor

/** The apprentice's wardrobe: robes and hats bought with mana, worn on the tower and on every floor. */
@Composable
fun WardrobeScreen(data: WData, go: Go) {
    val w = data.world
    val s = data.settings
    val container = LocalContext.current.container
    val feel = LocalFeel.current
    val tick = rememberTicker(450)
    fun wear(g: Garment) {
        container.scope.launch {
            container.settings.update { if (g.slot == Garment.Slot.ROBE) it.copy(robe = g.name) else it.copy(hat = g.name) }
        }
    }
    val onPick: (Garment) -> Unit = { g ->
        when {
            w.owns(g) -> { wear(g); feel?.tone(Tone.PAT, 0.2f) }
            w.canAfford(g) -> {
                container.write { buy(g, w) }
                wear(g)
                feel?.tone(Tone.GIFT)
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar(gs(R.string.wardrobe_title), go::back, Tip.WARDROBE) { ManaTag(w.mana, Modifier.padding(end = 4.dp)) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(170.dp).pixelBox(Color(0xFF211A4A), U.c.outline), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(120.dp, 160.dp)) {
                    val sp = Looks.apprentice(s.robe, s.hat, (tick.value % 2).toInt())
                    val px = floor(minOf(size.width / sp.w, size.height / sp.h))
                    drawPixels(sp, (size.width - sp.w * px) / 2, (size.height - sp.h * px) / 2, px)
                }
            }
            Gap(8.dp)
            Text(gs(R.string.wardrobe_sub), style = UType.Muted)
            SectionTitle(gs(R.string.wardrobe_robes))
            Grid(Garment.entries.filter { it.slot == Garment.Slot.ROBE }, w, s, onPick)
            SectionTitle(gs(R.string.wardrobe_hats))
            Grid(Garment.entries.filter { it.slot == Garment.Slot.HAT }, w, s, onPick)
            Gap(24.dp)
        }
    }
}

@Composable
private fun Grid(items: List<Garment>, w: World, s: Settings, onPick: (Garment) -> Unit) {
    items.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { g -> Cell(g, w, s, Modifier.weight(1f)) { onPick(g) } }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Gap(8.dp)
    }
}

@Composable
private fun Cell(g: Garment, w: World, s: Settings, modifier: Modifier, onClick: () -> Unit) {
    val c = U.c
    val worn = s.robe == g.name || s.hat == g.name
    val owned = w.owns(g)
    val afford = owned || w.canAfford(g)
    // Preview: the robe with the current hat, or the hat with the current robe.
    val sprite = if (g.slot == Garment.Slot.ROBE) Looks.apprentice(g.name, s.hat, 0) else Looks.apprentice(s.robe, g.name, 0)
    Column(
        modifier
            .pixelBox(if (worn) c.surface2 else c.surface, if (worn) c.accent else c.outline, c.bevel)
            .clickable(enabled = afford, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpriteImage(sprite, 60.dp, alpha = if (afford) 1f else 0.45f)
        Gap(6.dp)
        Text(t(g.title), style = UType.Small.copy(color = if (afford) c.ink else c.faint), maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Gap(4.dp)
        when {
            worn -> Text(gs(R.string.wardrobe_worn), style = UType.Small.copy(color = c.accent))
            owned -> Text(gs(R.string.wardrobe_wear), style = UType.Small.copy(color = c.good))
            else -> ManaTag(g.cost)
        }
    }
}
