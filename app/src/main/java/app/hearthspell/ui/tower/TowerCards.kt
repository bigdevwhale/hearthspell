package app.hearthspell.ui.tower

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hearthspell.BuildConfig
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.Stars
import app.hearthspell.data.claim
import app.hearthspell.domain.Gift
import app.hearthspell.ui.Go
import app.hearthspell.ui.LocalFeel
import app.hearthspell.ui.Tone
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelButton
import app.hearthspell.ui.components.PixelField
import app.hearthspell.ui.extras.ConstellationArt
import app.hearthspell.ui.floorName
import app.hearthspell.ui.gp
import app.hearthspell.ui.gs
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import app.hearthspell.ui.tips.Tip
import kotlinx.coroutines.launch

/** Cards under the master's requests: welcome home, gifts on the windowsill, Sunday tea. */
@Composable
internal fun TowerCards(data: WData, go: Go) {
    val w = data.world
    val container = LocalContext.current.container
    val feel = LocalFeel.current
    var revealed by remember { mutableStateOf<Gift?>(null) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (w.homecoming) {
            Card({ SpriteImage(Art.master(0), 44.dp) }, gs(R.string.home_title), gp(R.plurals.home_text, w.daysAway, w.daysAway))
        }
        w.gifts.forEach { g ->
            val floor = w.floor(g.floorId)
            Card(
                { SpriteImage(Art.helper(g.helper, 0), 44.dp) },
                gs(R.string.gift_title, t(g.helper.title)),
                gs(R.string.gift_text, floorName(floor)),
                onClick = {
                    container.write { claim(g) }
                    feel?.tone(Tone.GIFT)
                    revealed = g
                },
            )
        }
        val week = w.teaWeek
        if (week != null && "tea:${week.toEpochDay()}" !in data.settings.seen && w.teaReport(week).casts > 0) {
            Card({ SpriteImage(Art.teaTable, 44.dp) }, gs(R.string.tea_card_title), gs(R.string.tea_card_text), onClick = go::tea)
        }
    }
    revealed?.let { g -> GiftDialog(g) { revealed = null } }
}

@Composable
private fun Card(sprite: @Composable () -> Unit, title: String, text: String, onClick: (() -> Unit)? = null) {
    Panel(Modifier.fillMaxWidth().padding(vertical = 2.dp), onClick = onClick, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            sprite()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = UType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(text, style = UType.Muted)
            }
        }
    }
}

@Composable
private fun GiftDialog(g: Gift, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = U.c.surface,
        shape = RectangleShape,
        title = { Text(t(g.trinket.title), style = UType.Heading, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                SpriteImage(Art.trinket(g.trinket.name), 84.dp)
                Spacer(Modifier.height(12.dp))
                Text(t(g.trinket.blurb), style = UType.Body, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(gs(R.string.gift_found_by, t(g.helper.title)), style = UType.Muted, textAlign = TextAlign.Center)
                if (g.trinket.rare) Text(gs(R.string.gift_rare), style = UType.Muted.copy(color = U.c.mana), textAlign = TextAlign.Center)
            }
        },
        confirmButton = { PixelButton(gs(R.string.gift_keep), onDone, Modifier.fillMaxWidth()) },
    )
}

/** Moments that pop up over the tower once: the cat moving in, a constellation completed. */
@Composable
internal fun TowerMoments(data: WData) {
    val w = data.world
    val s = data.settings
    val container = LocalContext.current.container
    val feel = LocalFeel.current
    // Wait until the first tour is over.
    if (!s.onboarded || Tip.TOWER.key !in s.seen && !BuildConfig.DEMO) return
    if (s.catName.isBlank() && w.totalCasts >= 3) {
        CatDialog { name ->
            feel?.purr()
            container.scope.launch { container.settings.update { it.copy(catName = name) } }
        }
        return
    }
    val pending = Stars.complete(w.starsLit).filter { "star:${it.key}" !in s.seen }
    if (pending.isEmpty()) return
    // After an update many may be complete at once: celebrate only the newest.
    LaunchedEffect(pending.size) {
        if (pending.size > 1) container.settings.update { st -> st.copy(seen = st.seen + pending.dropLast(1).map { "star:${it.key}" }) }
    }
    val c = pending.last()
    LaunchedEffect(c.key) { feel?.tone(Tone.FANFARE, 0.6f) }
    AlertDialog(
        onDismissRequest = {},
        containerColor = U.c.surface,
        shape = RectangleShape,
        title = { Text(gs(R.string.stars_new_title), style = UType.Heading, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                ConstellationArt(c, c.stars.size, Modifier.size(180.dp, 120.dp))
                Spacer(Modifier.height(10.dp))
                Text(t(c.title), style = UType.Title.copy(color = U.c.accent), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(t(c.line), style = UType.Body, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            PixelButton(gs(R.string.stars_new_ok), {
                container.scope.launch { container.settings.update { st -> st.copy(seen = st.seen + "star:${c.key}") } }
            }, Modifier.fillMaxWidth())
        },
    )
}

@Composable
private fun CatDialog(onName: (String) -> Unit) {
    val default = gs(R.string.cat_default_name)
    var name by remember { mutableStateOf(default) }
    val tick = rememberTicker(500)
    AlertDialog(
        onDismissRequest = {},
        containerColor = U.c.surface,
        shape = RectangleShape,
        title = { Text(gs(R.string.cat_title), style = UType.Heading, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                SpriteImage(Art.catAwake(if (tick.value % 6 == 0L) 1 else 0), 96.dp)
                Spacer(Modifier.height(10.dp))
                Text(gs(R.string.cat_text), style = UType.Body, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                PixelField(name, { name = it.take(20) }, Modifier.fillMaxWidth(), placeholder = default)
            }
        },
        confirmButton = { PixelButton(gs(R.string.cat_ok), { onName(name.trim().ifBlank { default }) }, Modifier.fillMaxWidth()) },
    )
}
