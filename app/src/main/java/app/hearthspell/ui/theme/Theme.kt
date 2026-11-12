package app.hearthspell.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.hearthspell.R
import app.hearthspell.data.ThemeMode

/**
 * The UI colours: a storybook. The tower at night: deep indigo lit by candles and gold. By day: warm parchment and
 * ink. Cards are soft rounded panes, [outline] their shadow and [bevel] their light edge.
 */
@Immutable
data class Palette(
    val bg: Color,
    /** The sky at the top of the screen. */
    val bgTop: Color,
    val surface: Color,
    val surface2: Color,
    /** The dark pixel outline around panels and buttons. */
    val outline: Color,
    /** The 1-pixel bevel highlight on the top edge of a panel. */
    val bevel: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    /** Candlelight: primary buttons, selection. */
    val accent: Color,
    val accentDeep: Color,
    val onAccent: Color,
    /** Mana is cyan in both themes. */
    val mana: Color,
    val good: Color,
    val danger: Color,
    val imp: Color,
    val dark: Boolean,
)

val NightPalette = Palette(
    bg = Color(0xFF120F26),
    bgTop = Color(0xFF1C1640),
    surface = Color(0xFF1F1A40),
    surface2 = Color(0xFF2A2354),
    outline = Color(0xFF05040C),
    bevel = Color(0xFF51468F),
    ink = Color(0xFFFBF1DC),
    muted = Color(0xFFC2B6DE),
    faint = Color(0xFF857AAF),
    accent = Color(0xFFF6C25B),
    accentDeep = Color(0xFFD98A2B),
    onAccent = Color(0xFF2B1800),
    mana = Color(0xFF5FD6FF),
    good = Color(0xFF7DDB5A),
    danger = Color(0xFFFF6B5D),
    imp = Color(0xFFB062E0),
    dark = true,
)

val ParchmentPalette = Palette(
    bg = Color(0xFFF3E7CB),
    bgTop = Color(0xFFE9D7AE),
    surface = Color(0xFFFCF5E3),
    surface2 = Color(0xFFEFDFBC),
    outline = Color(0xFF5A4128),
    bevel = Color(0xFFD9C29A),
    ink = Color(0xFF2B1D12),
    muted = Color(0xFF6E5640),
    faint = Color(0xFFA08868),
    accent = Color(0xFFE07A1F),
    accentDeep = Color(0xFFA9500F),
    onAccent = Color(0xFFFFF6E0),
    mana = Color(0xFF1F7EC4),
    good = Color(0xFF3E9B2F),
    danger = Color(0xFFC83A3A),
    imp = Color(0xFF7B3BAA),
    dark = false,
)

/** The fixed 8-bit palette every sprite is drawn with, close to the NES one. */
object Nes {
    val black = Color(0xFF07060F)
    val night = Color(0xFF14112B)
    val indigo = Color(0xFF2E2660)
    val violet = Color(0xFF5B4BA8)
    val lavender = Color(0xFF9B8AE0)
    val stoneDark = Color(0xFF3C3A52)
    val stone = Color(0xFF5E5C78)
    val stoneLight = Color(0xFF8A88A3)
    val white = Color(0xFFFCFCFC)
    val cream = Color(0xFFFCEFD2)
    val sand = Color(0xFFE8C88A)
    val wood = Color(0xFF9C5A2C)
    val woodDark = Color(0xFF5C3018)
    val woodLight = Color(0xFFC88440)
    val red = Color(0xFFD82800)
    val brick = Color(0xFFA83C2C)
    val orange = Color(0xFFFC7400)
    val amber = Color(0xFFFFB938)
    val yellow = Color(0xFFFCE070)
    val green = Color(0xFF38A838)
    val greenDark = Color(0xFF1E6420)
    val leaf = Color(0xFF7DDB5A)
    val teal = Color(0xFF00A8A8)
    val sky = Color(0xFF5FD6FF)
    val blue = Color(0xFF2860D8)
    val navy = Color(0xFF183078)
    val pink = Color(0xFFF878B8)
    val purple = Color(0xFF8838C8)
    val plum = Color(0xFF581878)
    val dust = Color(0xFF8D7B68)
    val skin = Color(0xFFF8B888)
    val grey = Color(0xFFA8A8B8)
}

val LocalPalette = staticCompositionLocalOf { NightPalette }

/**
 * A spellbook's lettering, all SIL OFL with Cyrillic: Kurale for titles (a fairy-tale hand), Alegreya SC for the small
 * capital labels (an old grimoire's rubrics), Philosopher for everything else (soft, a little magical, easy to read).
 */
val Storybook = FontFamily(Font(R.font.kurale, FontWeight.Normal), Font(R.font.kurale, FontWeight.Bold))
val Rubric = FontFamily(Font(R.font.alegreya_sc_extrabold, FontWeight.Bold))
val Round = FontFamily(
    Font(R.font.philosopher, FontWeight.Normal),
    Font(R.font.philosopher, FontWeight.SemiBold),
    Font(R.font.philosopher_bold, FontWeight.Bold),
)

object U {
    val c: Palette @Composable get() = LocalPalette.current
}

object UType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Storybook, fontSize = 36.sp, lineHeight = 40.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Storybook, fontSize = 30.sp, lineHeight = 34.sp, color = U.c.ink)
    val Heading: TextStyle @Composable get() = TextStyle(fontFamily = Storybook, fontSize = 24.sp, lineHeight = 28.sp, color = U.c.ink)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Rubric, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.06.em, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Round, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = U.c.ink)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Round, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Round, fontSize = 16.sp, lineHeight = 22.sp, color = U.c.ink)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Round, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Round, fontSize = 15.sp, lineHeight = 21.sp, color = U.c.muted)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Round, fontSize = 14.sp, lineHeight = 19.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Round, fontSize = 12.sp, lineHeight = 16.sp, color = U.c.faint)
}

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun HearthTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) NightPalette else ParchmentPalette
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = p.accent,
                onPrimary = p.onAccent,
                primaryContainer = p.surface2,
                onPrimaryContainer = p.ink,
                secondaryContainer = p.surface2,
                onSecondaryContainer = p.ink,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface2,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.outline,
                outlineVariant = p.faint,
                error = p.danger,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body,
                bodySmall = UType.Muted,
                labelLarge = UType.Button,
                titleLarge = UType.Title,
                headlineSmall = UType.Title,
            ),
            content = content,
        )
    }
}
