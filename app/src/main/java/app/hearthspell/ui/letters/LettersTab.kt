package app.hearthspell.ui.letters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.LetterText
import app.hearthspell.content.LetterTexts
import app.hearthspell.ui.Go
import app.hearthspell.ui.LocalPerson
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.PixelIcon
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.ManaTag
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.SectionTitle
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.floorName
import app.hearthspell.ui.gp
import app.hearthspell.ui.gs
import app.hearthspell.ui.spellName
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import app.hearthspell.ui.tips.Tip
import kotlinx.coroutines.delay

/** The master's letters: one on the first day, then one a week. */
object Letters {
    /** How many letters are written: [LetterTexts.all] has exactly this many (checked by a test). */
    const val COUNT = 12

    /** Letters that have arrived but haven't been opened. */
    fun unread(data: WData): Int =
        (0 until data.world.lettersArrived(COUNT)).count { it.toString() !in data.settings.lettersRead }
}

private val Paper = Color(0xFFF4E4BE)
private val PaperEdge = Color(0xFFD9C08E)
private val Ink = Color(0xFF2B1D12)
private val InkMuted = Color(0xFF6E5640)
private val Wax = Color(0xFFC83A3A)

@Composable
fun LettersTab(data: WData, go: Go) {
    val w = data.world
    val arrived = w.lettersArrived(Letters.COUNT)
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.letters_title), null, Tip.LETTERS)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MasterSprite(56.dp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.letters_sub), style = UType.Sub, modifier = Modifier.weight(1f))
            }
            Gap(14.dp)
            // Newest on top; the next one still on its way, shown as a silhouette.
            if (arrived < Letters.COUNT) {
                val n = arrived
                val days = (data.settings.startDay + 7L * n - w.day).toInt().coerceAtLeast(1)
                Panel(Modifier.fillMaxWidth(), fill = U.c.bg, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Envelope(sealed = true, dim = true)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.letters_on_way), style = UType.Body.copy(color = U.c.faint))
                            Text(gp(R.plurals.letters_arrives, days, days), style = UType.Small)
                        }
                    }
                }
                Gap(8.dp)
            }
            (arrived - 1 downTo 0).forEach { n ->
                val letter = LetterTexts.all[n]
                val sealed = n.toString() !in data.settings.lettersRead
                Panel(
                    Modifier.fillMaxWidth(),
                    fill = if (sealed) U.c.surface2 else U.c.surface,
                    onClick = { go.letter(n) },
                    padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Envelope(sealed = sealed)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.letter_number, n + 1).uppercase() + if (sealed) "  ·  " + stringResource(R.string.letters_sealed).uppercase() else "",
                                style = UType.Eyebrow.copy(color = if (sealed) U.c.accent else U.c.faint),
                            )
                            Gap(4.dp)
                            Text(t(letter.title), style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t(letter.from), style = UType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Gap(8.dp)
            }
            Gap(24.dp)
        }
    }
}

/** An 8-bit envelope; sealed ones carry a red wax seal. */
@Composable
private fun Envelope(sealed: Boolean, dim: Boolean = false) {
    val c = U.c
    Box(
        Modifier.size(width = 44.dp, height = 32.dp).pixelBox(if (dim) c.surface else Paper, c.outline, if (dim) null else Color.White.copy(alpha = 0.5f), PaperEdge.takeIf { !dim }),
        contentAlignment = Alignment.Center,
    ) {
        if (dim) PixelIcon(Glyphs.letter, c.faint, size = 22.dp)
        else if (sealed) Box(Modifier.size(12.dp).background(Wax))
        else PixelIcon(Glyphs.letter, InkMuted, size = 22.dp)
    }
}

@Composable
private fun MasterSprite(size: Dp) {
    var f by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(700)
            f = 1 - f
        }
    }
    SpriteImage(Art.master(f), size)
}

@Composable
fun LetterScreen(n: Int, data: WData, go: Go) {
    val w = data.world
    val container = LocalContext.current.container
    val arrived = w.lettersArrived(Letters.COUNT)
    val letter: LetterText? = LetterTexts.all.getOrNull(n)?.takeIf { n < arrived }
    LaunchedEffect(n, letter != null) {
        if (letter != null && n.toString() !in data.settings.lettersRead) {
            container.settings.update { it.copy(lettersRead = it.lettersRead + n.toString()) }
        }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.letter_number, n + 1), go::back)
        if (letter == null) {
            Text(stringResource(R.string.letter_not_yet), style = UType.Sub, modifier = Modifier.padding(16.dp))
            return@Column
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Column(Modifier.fillMaxWidth().pixelBox(Paper, U.c.outline, Color.White.copy(alpha = 0.5f), PaperEdge).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MasterSprite(64.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t(letter.title).uppercase(), style = UType.Heading.copy(color = Ink))
                        Gap(4.dp)
                        Text(t(letter.from), style = UType.Small.copy(color = InkMuted))
                    }
                }
                Gap(18.dp)
                val name = LocalPerson.current.name
                Text(
                    if (name.isNotBlank()) gs(R.string.letter_greeting, name) else gs(R.string.letter_greeting_anon),
                    style = UType.Strong.copy(color = Ink),
                )
                Gap(10.dp)
                Text(t(letter.body), style = UType.Body.copy(color = Ink))
                Gap(18.dp)
                Text(stringResource(R.string.letter_sign), style = UType.Body.copy(color = InkMuted), modifier = Modifier.align(Alignment.End))
                Text(stringResource(R.string.letter_master), style = UType.Heading.copy(color = Ink), modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
            }

            // The requests belong to this week, so they ride along with the newest letter.
            if (n == arrived - 1) {
                SectionTitle(stringResource(R.string.letter_requests))
                if (w.requests.isEmpty()) {
                    Text(gs(R.string.letter_requests_none), style = UType.Sub)
                } else {
                    Text(stringResource(R.string.letter_requests_sub), style = UType.Muted)
                    Gap(8.dp)
                    w.requests.forEach { s ->
                        val done = w.requestDone(s.id)
                        Panel(Modifier.fillMaxWidth(), onClick = { go.floor(s.floorId) }, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(28.dp).pixelBox(if (done) U.c.good else U.c.bg, U.c.outline), contentAlignment = Alignment.Center) {
                                    if (done) PixelIcon(Glyphs.check, U.c.outline, size = 16.dp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(spellName(s), style = UType.Body.copy(color = if (done) U.c.faint else U.c.ink))
                                    Text(w.floor(s.floorId)?.let { floorName(it) }.orEmpty(), style = UType.Small)
                                }
                                if (!done) ManaTag(w.manaFor(s), prefix = "+")
                            }
                        }
                        Gap(8.dp)
                    }
                }
            }
            Gap(24.dp)
        }
    }
}
