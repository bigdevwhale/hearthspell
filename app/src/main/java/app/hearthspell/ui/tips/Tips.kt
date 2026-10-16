package app.hearthspell.ui.tips

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hearthspell.BuildConfig
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.DecorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.Sprite
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.ui.components.ButtonKind
import app.hearthspell.ui.components.IconTile
import app.hearthspell.ui.components.PixelButton
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.gs
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Who explains a screen: the master himself, a helper, or a piece of decor. */
sealed interface Speaker {
    data object Master : Speaker
    data class Helper(val kind: HelperKind) : Speaker
    data class Thing(val kind: DecorKind) : Speaker
}

private fun Speaker.sprite(frame: Int): Sprite = when (this) {
    Speaker.Master -> Art.master(frame)
    is Speaker.Helper -> Art.helper(kind, frame)
    is Speaker.Thing -> Art.decor(kind)
}

/** One page of a hint: a short title and a few lines. */
class Page(@StringRes val title: Int, @StringRes val text: Int)

/**
 * The hints shown the first time a screen opens. [TOWER] is the tour right after onboarding.
 * Seen hints are stored in [app.hearthspell.data.Settings.seen] as "tip:<name>".
 */
enum class Tip(val speaker: Speaker, val pages: List<Page>) {
    TOWER(
        Speaker.Master,
        listOf(
            Page(R.string.tip_tower_1_t, R.string.tip_tower_1),
            Page(R.string.tip_tower_2_t, R.string.tip_tower_2),
            Page(R.string.tip_tower_3_t, R.string.tip_tower_3),
            Page(R.string.tip_tower_4_t, R.string.tip_tower_4),
            Page(R.string.tip_tower_5_t, R.string.tip_tower_5),
        ),
    ),
    FLOOR(Speaker.Helper(HelperKind.BROOM), listOf(Page(R.string.tip_floor_1_t, R.string.tip_floor_1), Page(R.string.tip_floor_2_t, R.string.tip_floor_2))),
    CANTRIPS(Speaker.Helper(HelperKind.PILLOW), listOf(Page(R.string.tip_cantrips_t, R.string.tip_cantrips))),
    GRIMOIRE(Speaker.Helper(HelperKind.CANDLE), listOf(Page(R.string.tip_grimoire_1_t, R.string.tip_grimoire_1), Page(R.string.tip_grimoire_2_t, R.string.tip_grimoire_2))),
    SPELL(Speaker.Helper(HelperKind.BOOK), listOf(Page(R.string.tip_spell_t, R.string.tip_spell))),
    HELPER(Speaker.Helper(HelperKind.SPONGE), listOf(Page(R.string.tip_helper_t, R.string.tip_helper))),
    MARKET(Speaker.Helper(HelperKind.SATCHEL), listOf(Page(R.string.tip_market_1_t, R.string.tip_market_1), Page(R.string.tip_market_2_t, R.string.tip_market_2))),
    TRIP(Speaker.Helper(HelperKind.SATCHEL), listOf(Page(R.string.tip_trip_t, R.string.tip_trip))),
    LETTERS(Speaker.Master, listOf(Page(R.string.tip_letters_t, R.string.tip_letters))),
    DECOR(Speaker.Thing(DecorKind.CAT), listOf(Page(R.string.tip_decor_t, R.string.tip_decor))),
    SPELL_FORM(Speaker.Helper(HelperKind.LADLE), listOf(Page(R.string.tip_form_t, R.string.tip_form))),
    FLOORS(Speaker.Helper(HelperKind.KEY), listOf(Page(R.string.tip_floors_t, R.string.tip_floors)));

    val key: String get() = "tip:$name"

    companion object {
        /** A hint reopened by its "?" button; observed by [TipHost]. */
        internal var reopened by mutableStateOf<Tip?>(null)
    }
}

/**
 * Shows [tip] the first time its screen opens (never in the demo build, which is for screenshots),
 * and again whenever its "?" is tapped.
 */
@Composable
fun TipHost(tip: Tip) {
    if (BuildConfig.DEMO && Tip.reopened != tip) return
    val container = LocalContext.current.container
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return
    val firstTime = s.onboarded && tip.key !in s.seen && !BuildConfig.DEMO
    if (!firstTime && Tip.reopened != tip) return
    // Let the screen settle in before the hint pops up.
    var ready by remember { mutableStateOf(Tip.reopened == tip) }
    LaunchedEffect(Unit) { if (!ready) { delay(350); ready = true } }
    if (!ready) return
    fun close(all: Boolean) {
        Tip.reopened = null
        container.scope.launch {
            container.settings.update { st ->
                st.copy(seen = st.seen + if (all) Tip.entries.map { it.key } else listOf(tip.key))
            }
        }
    }
    TipDialog(tip, onDone = { close(false) }, onSkipAll = { close(true) })
}

/** The "?" that brings a screen's hint back. */
@Composable
fun HelpTile(tip: Tip) {
    IconTile(Glyphs.question, { Tip.reopened = tip }, size = 42.dp, tint = U.c.muted)
}

@Composable
private fun TipDialog(tip: Tip, onDone: () -> Unit, onSkipAll: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val last = page == tip.pages.lastIndex
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(420)
            frame = (frame + 1) % 4
        }
    }
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val c = U.c
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 18.dp)
                    // Swallow taps on the card itself.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .pixelBox(c.surface, c.outline, c.bevel, c.outline.copy(alpha = 0.35f))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).pixelBox(c.surface2, c.outline), contentAlignment = Alignment.Center) {
                        SpriteImage(tip.speaker.sprite(frame % 2), 54.dp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(speakerName(tip.speaker).uppercase(), style = UType.Eyebrow.copy(color = c.accent))
                        Spacer(Modifier.height(6.dp))
                        Text(gs(tip.pages[page].title), style = UType.Heading)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(gs(tip.pages[page].text), style = UType.Body)
                Spacer(Modifier.height(16.dp))
                if (tip.pages.size > 1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        tip.pages.indices.forEach { i ->
                            Box(Modifier.padding(horizontal = 4.dp).size(if (i == page) 10.dp else 7.dp).background(if (i == page) c.accent else c.faint))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (page > 0) {
                        PixelButton(stringResource(R.string.tip_back), { page-- }, kind = ButtonKind.SECONDARY)
                    } else {
                        PixelButton(stringResource(R.string.tip_skip_all), onSkipAll, kind = ButtonKind.SECONDARY)
                    }
                    Spacer(Modifier.width(10.dp))
                    PixelButton(
                        stringResource(if (last) R.string.tip_got_it else R.string.next),
                        { if (last) onDone() else page++ },
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun speakerName(s: Speaker): String = when (s) {
    Speaker.Master -> stringResource(R.string.tip_master)
    is Speaker.Helper -> app.hearthspell.ui.t(s.kind.title)
    is Speaker.Thing -> app.hearthspell.ui.t(s.kind.title)
}
