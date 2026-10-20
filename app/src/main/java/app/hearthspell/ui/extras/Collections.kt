package app.hearthspell.ui.extras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.content.ImpKind
import app.hearthspell.content.Trinket
import app.hearthspell.ui.Go
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Looks
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelBar
import app.hearthspell.ui.components.SectionTitle
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.gp
import app.hearthspell.ui.gs
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import app.hearthspell.ui.tips.Tip
import app.hearthspell.ui.tower.rememberTicker

/** Every imp ever shown the door, and the ones still to meet (as shadows with a hint of where they live). */
@Composable
fun BestiaryScreen(data: WData, go: Go) {
    val w = data.world
    val met = w.impsMet
    val tick = rememberTicker(400)
    Column(Modifier.fillMaxSize()) {
        TopBar(gs(R.string.bestiary_title), go::back, Tip.BESTIARY)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(gs(R.string.bestiary_sub), style = UType.Muted)
            Gap(10.dp)
            Text(gs(R.string.bestiary_count, met.size, ImpKind.entries.size), style = UType.Sub)
            Gap(6.dp)
            PixelBar(met.size / ImpKind.entries.size.toFloat(), Modifier.fillMaxWidth(), color = U.c.imp, segments = ImpKind.entries.size)
            Gap(12.dp)
            ImpKind.entries.forEach { k ->
                val n = met[k] ?: 0
                Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp), fill = if (n > 0) U.c.surface else U.c.bg) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val s = Looks.imp(k, if (n > 0) (tick.value % 2).toInt() else 0)
                        SpriteImage(if (n > 0) s else s.silhouette(), 52.dp, alpha = if (n > 0) 1f else 0.6f)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            if (n > 0) {
                                Text(t(k.title), style = UType.Strong)
                                Text(t(k.habit), style = UType.Muted)
                                Gap(4.dp)
                                Text(gp(R.plurals.bestiary_seen, n, n), style = UType.Small.copy(color = U.c.imp))
                            } else {
                                Text("???", style = UType.Strong.copy(color = U.c.faint))
                                Text(hint(k), style = UType.Small)
                            }
                        }
                    }
                }
            }
            Gap(24.dp)
        }
    }
}

@Composable
private fun hint(k: ImpKind): String = when {
    k.season != null -> gs(R.string.bestiary_hint_season, t(k.season.title))
    k.floor != null -> gs(R.string.bestiary_hint_floor, t(k.floor.magic))
    else -> gs(R.string.bestiary_hint_any)
}

/** A flat dark shape of the sprite, for imps not yet met. */
private fun app.hearthspell.ui.art.Sprite.silhouette(): app.hearthspell.ui.art.Sprite = recolorAll(androidx.compose.ui.graphics.Color(0xFF2E2660))

/** The trinkets the helpers have brought, on a shelf. */
@Composable
fun TreasuresScreen(data: WData, go: Go) {
    val w = data.world
    val found = w.trinketsFound
    Column(Modifier.fillMaxSize()) {
        TopBar(gs(R.string.treasures_title), go::back, Tip.TREASURES)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(gs(R.string.treasures_sub), style = UType.Muted)
            Gap(10.dp)
            Text(gs(R.string.treasures_count, found.size, Trinket.entries.size), style = UType.Sub)
            Gap(6.dp)
            PixelBar(found.size / Trinket.entries.size.toFloat(), Modifier.fillMaxWidth(), color = U.c.accent, segments = Trinket.entries.size)
            SectionTitle(gs(R.string.treasures_shelf))
            Trinket.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { tr ->
                        val n = found[tr] ?: 0
                        Column(
                            Modifier.weight(1f).pixelBox(if (n > 0) U.c.surface else U.c.bg, U.c.outline, U.c.bevel).padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val s = Art.trinket(tr.name)
                            SpriteImage(if (n > 0) s else s.silhouette(), 44.dp)
                            Gap(6.dp)
                            Text(
                                if (n > 0) t(tr.title) else "???", style = UType.Small.copy(color = if (n > 0) U.c.ink else U.c.faint),
                                maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                            )
                            if (n > 1) Text("×$n", style = UType.Small.copy(color = U.c.accent))
                            else if (tr.rare) Text(gs(R.string.treasures_rare), style = UType.Small.copy(color = U.c.mana))
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Gap(8.dp)
            }
            Gap(6.dp)
            Text(gs(R.string.treasures_how), style = UType.Muted)
            Gap(24.dp)
        }
    }
}
