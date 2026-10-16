package app.hearthspell.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.FloorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.content.Spells
import app.hearthspell.data.setupTower
import app.hearthspell.domain.Gender
import app.hearthspell.domain.Mana
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.PixelIcon
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.ui.components.ButtonKind
import app.hearthspell.ui.components.Chip
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.ManaTag
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelButton
import app.hearthspell.ui.components.PixelField
import app.hearthspell.ui.components.SectionTitle
import app.hearthspell.ui.components.TimeDialog
import app.hearthspell.ui.components.ToggleRow
import app.hearthspell.ui.hhmm
import app.hearthspell.ui.locale
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

private const val PAGES = 5

/** The first minute: the master leaves, you give your name, pick your rooms and their spells, and a reminder time. */
@Composable
fun OnboardingScreen(data: WData, onDone: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val scope = rememberCoroutineScope()
    val ru = locale().language == "ru"

    var page by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(data.settings.name) }
    var gender by rememberSaveable { mutableStateOf(data.settings.gender) }
    var kinds by rememberSaveable {
        mutableStateOf(listOf(FloorKind.BEDROOM, FloorKind.LIVING, FloorKind.KITCHEN, FloorKind.BATH, FloorKind.WHOLE, FloorKind.HALL).map { it.name })
    }
    var picked by rememberSaveable { mutableStateOf(Spells.all.filter { it.common }.map { it.key }) }
    var remindOn by rememberSaveable { mutableStateOf(true) }
    var remindAt by rememberSaveable { mutableIntStateOf(19 * 60) }
    var timeDialog by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val chosenKinds = towerOrder.filter { it.name in kinds }

    fun finish() {
        if (busy) return
        busy = true
        scope.launch {
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val choice = chosenKinds.map { k -> k to Spells.forFloor(k).filter { it.key in picked } }
                container.dao.setupTower(choice, now)
                val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
                container.settings.update {
                    it.copy(
                        onboarded = true,
                        startDay = today,
                        name = name.trim(),
                        gender = gender,
                        remindOn = remindOn,
                        remindAt = remindAt,
                    )
                }
                container.changed()
            }
            onDone()
        }
    }

    BackHandler(enabled = page > 0) { page-- }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            when (page) {
                0 -> FarewellPage()
                1 -> NamePage(name, { name = it }, ru, gender) { gender = it }
                2 -> RoomsPage(kinds) { k -> kinds = if (k.name in kinds) kinds - k.name else kinds + k.name }
                3 -> SpellsPage(chosenKinds, picked) { key -> picked = if (key in picked) picked - key else picked + key }
                else -> ReminderPage(remindOn, { remindOn = it }, remindAt) { timeDialog = true }
            }
        }
        Dots(page)
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (page > 0) PixelButton(stringResource(R.string.setup_back), { page-- }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            val next = when (page) {
                0 -> stringResource(R.string.setup_take_keys)
                PAGES - 1 -> stringResource(R.string.setup_open_tower)
                else -> stringResource(R.string.next)
            }
            val canGo = when (page) {
                2 -> chosenKinds.isNotEmpty()
                else -> !busy
            }
            PixelButton(next, { if (page < PAGES - 1) page++ else finish() }, Modifier.weight(if (page > 0) 1.6f else 1f), enabled = canGo)
        }
    }

    if (timeDialog) TimeDialog(remindAt, { timeDialog = false }) { remindAt = it }
}

@Composable
private fun Dots(page: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(PAGES) { i ->
            Box(Modifier.padding(horizontal = 4.dp).size(if (i == page) 12.dp else 8.dp).background(if (i <= page) U.c.accent else U.c.surface2))
        }
    }
}

@Composable
private fun ColumnScope.FarewellPage() {
    Gap(24.dp)
    SpriteImage(Art.master(), 120.dp, Modifier.align(Alignment.CenterHorizontally))
    Gap(20.dp)
    Text(stringResource(R.string.setup_farewell_title), style = UType.Display, modifier = Modifier.align(Alignment.CenterHorizontally))
    Gap(18.dp)
    Panel(Modifier.fillMaxWidth(), fill = U.c.surface) {
        Text(stringResource(R.string.setup_farewell_text), style = UType.Body)
        Gap(10.dp)
        Text(stringResource(R.string.setup_farewell_sign), style = UType.Sub.copy(color = U.c.accent))
    }
    Gap(16.dp)
    Text(stringResource(R.string.setup_farewell_hint), style = UType.Muted)
}

@Composable
private fun NamePage(name: String, onName: (String) -> Unit, ru: Boolean, gender: Gender, onGender: (Gender) -> Unit) {
    Text(stringResource(R.string.setup_name_title), style = UType.Title)
    Gap(8.dp)
    Text(stringResource(R.string.setup_name_sub), style = UType.Sub)
    Gap(16.dp)
    PixelField(
        name, { onName(it.take(24)) }, Modifier.fillMaxWidth(),
        placeholder = stringResource(R.string.setup_name_hint),
        keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
    )
    if (ru) {
        SectionTitle(stringResource(R.string.setup_gender))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Chip(stringResource(R.string.setup_gender_male), gender == Gender.MALE, { onGender(Gender.MALE) })
            Chip(stringResource(R.string.setup_gender_female), gender == Gender.FEMALE, { onGender(Gender.FEMALE) })
        }
    }
}

@Composable
private fun RoomsPage(kinds: List<String>, onToggle: (FloorKind) -> Unit) {
    Text(stringResource(R.string.setup_rooms_title), style = UType.Title)
    Gap(8.dp)
    Text(stringResource(R.string.setup_rooms_sub), style = UType.Sub)
    Gap(14.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FloorKind.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k -> KindTile(k, k.name in kinds, { onToggle(k) }, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpellsPage(kinds: List<FloorKind>, picked: List<String>, onToggle: (String) -> Unit) {
    Text(stringResource(R.string.setup_spells_title), style = UType.Title)
    Gap(8.dp)
    Text(stringResource(R.string.setup_spells_sub), style = UType.Sub)
    kinds.forEach { k ->
        SectionTitle("${t(k.magic)} · ${t(k.room)}")
        Panel(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Spells.forFloor(k).forEach { p ->
                val on = p.key in picked
                Row(
                    Modifier.fillMaxWidth().clickable { onToggle(p.key) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CheckBox(on)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t(p.name), style = UType.Body, maxLines = 2, overflow = TextOverflow.Ellipsis, color = if (on) U.c.ink else U.c.muted)
                        Text(everyLabel(p.days), style = UType.Small)
                    }
                    ManaTag(Mana.base(p.effort))
                }
            }
        }
    }
    Gap(10.dp)
    Text(stringResource(R.string.setup_spells_later), style = UType.Muted)
}

@Composable
private fun ReminderPage(on: Boolean, onToggle: (Boolean) -> Unit, at: Int, onPickTime: () -> Unit) {
    Gap(8.dp)
    SpriteImage(Art.helper(HelperKind.BROOM), 96.dp, Modifier.fillMaxWidth())
    Gap(16.dp)
    Text(stringResource(R.string.setup_remind_title), style = UType.Title)
    Gap(8.dp)
    Text(stringResource(R.string.setup_remind_sub), style = UType.Sub)
    Gap(16.dp)
    Panel(Modifier.fillMaxWidth()) {
        ToggleRow(stringResource(R.string.setup_remind_toggle), on, onToggle)
        if (on) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onPickTime).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.setup_remind_time), style = UType.Body, modifier = Modifier.weight(1f))
                Text(hhmm(at), style = UType.Number.copy(color = U.c.accent))
            }
        }
    }
    Gap(14.dp)
    Text(stringResource(R.string.setup_remind_note), style = UType.Muted)
    Gap(8.dp)
    Row(verticalAlignment = Alignment.CenterVertically) {
        PixelIcon(Glyphs.imp, U.c.imp, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.setup_remind_imps), style = UType.Muted)
    }
    Spacer(Modifier.height(8.dp))
}
