package app.hearthspell.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.hearthspell.BuildConfig
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.data.Backup
import app.hearthspell.data.Settings
import app.hearthspell.data.ThemeMode
import app.hearthspell.domain.Gender
import app.hearthspell.ui.Go
import app.hearthspell.ui.WData
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.PixelIcon
import app.hearthspell.ui.components.ButtonKind
import app.hearthspell.ui.components.Chip
import app.hearthspell.ui.components.ConfirmDialog
import app.hearthspell.ui.components.Gap
import app.hearthspell.ui.components.Panel
import app.hearthspell.ui.components.PixelButton
import app.hearthspell.ui.components.PixelField
import app.hearthspell.ui.components.SectionTitle
import app.hearthspell.ui.components.TimeDialog
import app.hearthspell.ui.components.ToggleRow
import app.hearthspell.ui.components.TopBar
import app.hearthspell.ui.gs
import app.hearthspell.ui.hhmm
import app.hearthspell.ui.locale
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(data: WData, go: Go) {
    val context = LocalContext.current
    val container = context.container
    val scope = rememberCoroutineScope()
    val s = data.settings
    val ru = locale().language == "ru"

    fun update(change: (Settings) -> Settings) {
        container.scope.launch {
            container.settings.update(change)
            container.changed()
        }
    }

    var name by remember { mutableStateOf(s.name) }
    LaunchedEffect(name) { if (name.trim() != s.name) update { it.copy(name = name.trim()) } }
    var timeDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var language by remember { mutableStateOf(Language.current()) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val json = Backup.export(container.db, container.settings.now())
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } ?: error("no stream")
                }.isSuccess
            }
            Toast.makeText(context, if (ok) R.string.settings_export_ok else R.string.settings_export_fail, Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    Column(Modifier.fillMaxSize().background(U.c.bg).imePadding()) {
        TopBar(stringResource(R.string.settings_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            SectionTitle(gs(R.string.settings_apprentice))
            PixelField(
                name, { name = it.take(24) }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.setup_name_hint),
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            if (ru) {
                Gap(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Chip(stringResource(R.string.setup_gender_male), s.gender == Gender.MALE, { update { it.copy(gender = Gender.MALE) } })
                    Chip(stringResource(R.string.setup_gender_female), s.gender == Gender.FEMALE, { update { it.copy(gender = Gender.FEMALE) } })
                }
            }

            SectionTitle(stringResource(R.string.settings_tower))
            Panel(Modifier.fillMaxWidth(), onClick = go::floors, padding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelIcon(Glyphs.tower, U.c.accent, size = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_floors), style = UType.Body)
                        Text(stringResource(R.string.settings_floors_sub), style = UType.Muted)
                    }
                }
            }

            SectionTitle(stringResource(R.string.settings_reminder))
            Panel(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                ToggleRow(stringResource(R.string.setup_remind_toggle), s.remindOn, { on -> update { it.copy(remindOn = on) } }, sub = stringResource(R.string.settings_reminder_sub))
                if (s.remindOn) {
                    Row(Modifier.fillMaxWidth().clickable { timeDialog = true }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.setup_remind_time), style = UType.Body, modifier = Modifier.weight(1f))
                        Text(hhmm(s.remindAt), style = UType.Number.copy(color = U.c.accent))
                    }
                }
            }

            SectionTitle(stringResource(R.string.settings_look))
            Text(stringResource(R.string.settings_theme), style = UType.Muted)
            Gap(8.dp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ThemeMode.DARK to R.string.settings_theme_night, ThemeMode.LIGHT to R.string.settings_theme_parchment, ThemeMode.SYSTEM to R.string.settings_theme_system)
                    .forEach { (mode, label) -> Chip(stringResource(label), s.theme == mode, { update { it.copy(theme = mode) } }) }
            }
            Gap(14.dp)
            Text(stringResource(R.string.settings_language), style = UType.Muted)
            Gap(8.dp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Language.SYSTEM to stringResource(R.string.settings_language_system), Language.EN to "English", Language.RU to "Русский")
                    .forEach { (tag, label) -> Chip(label, language == tag, { language = tag; Language.set(tag) }) }
            }
            Gap(10.dp)
            Panel(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                ToggleRow(stringResource(R.string.settings_haptics), s.haptics, { on -> update { it.copy(haptics = on) } })
                ToggleRow(stringResource(R.string.settings_sound), s.sound, { on -> update { it.copy(sound = on) } })
            }
            Gap(10.dp)
            PixelButton(
                stringResource(R.string.settings_tips_again),
                { update { st -> st.copy(seen = st.seen.filterNot { it.startsWith("tip:") }.toSet()) }; go.home() },
                Modifier.fillMaxWidth(),
                kind = ButtonKind.SECONDARY,
                icon = Glyphs.question,
            )

            SectionTitle(stringResource(R.string.settings_backup))
            Text(stringResource(R.string.settings_backup_sub), style = UType.Muted)
            Gap(10.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PixelButton(stringResource(R.string.settings_export), {
                    exportLauncher.launch("hearthspell-${LocalDate.now()}.json")
                }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
                PixelButton(stringResource(R.string.settings_import), {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            }

            Gap(28.dp)
            About()
            Gap(24.dp)
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (timeDialog) TimeDialog(s.remindAt, { timeDialog = false }) { m -> update { it.copy(remindAt = m) } }

    pendingImport?.let { uri ->
        ConfirmDialog(
            stringResource(R.string.settings_import_title),
            stringResource(R.string.settings_import_text),
            stringResource(R.string.settings_import),
            onConfirm = {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("no stream")
                            Backup.import(container.db, container.settings, text)
                            container.changed()
                        }.isSuccess
                    }
                    Toast.makeText(context, if (ok) R.string.settings_import_ok else R.string.settings_import_fail, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { pendingImport = null },
            danger = true,
        )
    }
}

@Composable
private fun ColumnScope.About() {
    Text("HEARTHSPELL", style = UType.Heading.copy(color = U.c.accent), modifier = Modifier.align(Alignment.CenterHorizontally))
    Gap(6.dp)
    Text(stringResource(R.string.settings_about, BuildConfig.VERSION_NAME), style = UType.Small, modifier = Modifier.align(Alignment.CenterHorizontally))
}
