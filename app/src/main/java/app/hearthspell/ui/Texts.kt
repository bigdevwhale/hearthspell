package app.hearthspell.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.hearthspell.content.FloorKind
import app.hearthspell.content.L
import app.hearthspell.content.Spells
import app.hearthspell.data.Floor
import app.hearthspell.data.Spell
import app.hearthspell.domain.Gender
import app.hearthspell.domain.gendered
import java.util.Locale

/** Who the app is talking to; Russian strings are gendered by it. */
class Person(val name: String, val gender: Gender)

val LocalPerson = staticCompositionLocalOf { Person("", Gender.FEMALE) }

@Composable
@ReadOnlyComposable
fun locale(): Locale = LocalConfiguration.current.locales[0]

/** A catalog text in the app's language, with gender marks resolved. */
@Composable
fun t(l: L): String = gendered(l.of(locale()), LocalPerson.current.gender)

/** A string with gender marks resolved. */
@Composable
fun gs(@StringRes id: Int, vararg args: Any): String = gendered(stringResource(id, *args), LocalPerson.current.gender)

@Composable
fun gp(@PluralsRes id: Int, count: Int, vararg args: Any): String = gendered(pluralStringResource(id, count, *args), LocalPerson.current.gender)

/** The floor's tower name: the user's own, or its kind's ("Alchemy Lab"). */
@Composable
fun floorName(f: Floor?): String = if (f == null) "" else f.name.ifBlank { t(FloorKind.of(f.kind).magic) }

/** The real room behind a floor ("Kitchen"). */
@Composable
fun roomName(f: Floor?): String = if (f == null) "" else t(FloorKind.of(f.kind).room)

/** The spell's name: the user's own, or the preset's in the app's language. */
@Composable
fun spellName(s: Spell?): String {
    if (s == null) return ""
    if (s.name.isNotBlank()) return s.name
    return Spells.of(s.presetKey)?.let { t(it.name) }.orEmpty()
}

/** The same outside composition (notifications). */
fun spellName(s: Spell, locale: Locale): String =
    s.name.ifBlank { Spells.of(s.presetKey)?.name?.of(locale).orEmpty() }

fun floorName(f: Floor, locale: Locale): String = f.name.ifBlank { FloorKind.of(f.kind).magic.of(locale) }

/** A line in quotation marks of the language: «…» in Russian, “…” in English. */
fun quote(text: String, locale: Locale): String = if (locale.language == "ru") "«$text»" else "“$text”"

fun hhmm(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
