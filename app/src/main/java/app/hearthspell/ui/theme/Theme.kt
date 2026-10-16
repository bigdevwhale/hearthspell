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
 * The UI colours. The tower at night: deep indigo stone lit by candles. By day: parchment and ink.
 * Pixel art itself uses the fixed [Nes] palette in both themes.
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
    bg = Color(0xFF14112B),
    bgTop = Color(0xFF1E1846),
    surface = Color(0xFF221C45),
    surface2 = Color(0xFF2E2660),
    outline = Color(0xFF07060F),
    bevel = Color(0xFF4A3F8C),
    ink = Color(0xFFFCEFD2),
    muted = Color(0xFFBCAEDB),
    faint = Color(0xFF7A6BA8),
    accent = Color(0xFFFFB938),
    accentDeep = Color(0xFFD9701E),
    onAccent = Color(0xFF2B1600),
    mana = Color(0xFF5FD6FF),
    good = Color(0xFF7DDB5A),
    danger = Color(0xFFFF6B5D),
    imp = Color(0xFFB062E0),
    dark = true,
)

val ParchmentPalette = Palette(
    bg = Color(0xFFF1E3C2),
    bgTop = Color(0xFFE6D2A6),
    surface = Color(0xFFFBF2DC),
    surface2 = Color(0xFFEBD9B0),
    outline = Color(0xFF3A2A1A),
    bevel = Color(0xFFFFFBEF),
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

/** Headings, numbers and buttons: the classic 8x8 console font. Keep it short, it's wide. */
val PressStart = FontFamily(Font(R.font.press_start, FontWeight.Normal))

/** Everything else: a compact pixel font that reads well in long lines. */
val Tiny = FontFamily(Font(R.font.tiny5, FontWeight.Normal))

object U {
    val c: Palette @Composable get() = LocalPalette.current
}

object UType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = PressStart, fontSize = 18.sp, lineHeight = 26.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = PressStart, fontSize = 14.sp, lineHeight = 21.sp, color = U.c.ink)
    val Heading: TextStyle @Composable get() = TextStyle(fontFamily = PressStart, fontSize = 11.sp, lineHeight = 17.sp, color = U.c.ink)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = PressStart, fontSize = 8.sp, lineHeight = 12.sp, letterSpacing = 0.05.em, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = PressStart, fontSize = 13.sp, color = U.c.ink)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = PressStart, fontSize = 10.sp, lineHeight = 14.sp)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Tiny, fontSize = 19.sp, lineHeight = 24.sp, color = U.c.ink)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Tiny, fontSize = 21.sp, lineHeight = 25.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Tiny, fontSize = 18.sp, lineHeight = 23.sp, color = U.c.muted)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Tiny, fontSize = 16.sp, lineHeight = 20.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Tiny, fontSize = 14.sp, lineHeight = 17.sp, color = U.c.faint)
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
