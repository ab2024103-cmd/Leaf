package app.leaf.reader.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.leaf.reader.core.model.HighlightColor
import app.leaf.reader.core.model.ReadingTheme

/**
 * Reading-canvas palettes — LEAF-MASTER-PROMPT.md §4.2.
 * The document canvas only: the app chrome is driven by the M3 roles in [Color].
 */
data class ReadingPalette(
    val surface: Color,
    val surfaceAlt: Color,
    val text: Color,
    val textMuted: Color,
    val rule: Color,
    val link: Color,
    val isDark: Boolean
)

val Paper = ReadingPalette(
    Color(0xFFFFFFFF), Color(0xFFF7F5EF), Color(0xFF1B241F),
    Color(0xFF5C6A63), Color(0xFFE7E4DB), Color(0xFF206A4E), false
)
val Sepia = ReadingPalette(
    Color(0xFFF4ECD8), Color(0xFFEFE4C9), Color(0xFF43382A),
    Color(0xFF6D5D47), Color(0xFFE2D5BA), Color(0xFF8A5A2B), false
)
val Night = ReadingPalette(
    Color(0xFF12171A), Color(0xFF1A2124), Color(0xFFDDE5E0),
    Color(0xFF94A29B), Color(0xFF252D31), Color(0xFF8DD5B3), true
)
val Oled = ReadingPalette(
    Color(0xFF000000), Color(0xFF0C0F10), Color(0xFFCFD6D1),
    Color(0xFF8B9791), Color(0xFF1C2124), Color(0xFF8DD5B3), true
)

/** AUTO resolves at render time, never at save time. */
fun ReadingTheme.resolve(systemDarkMode: Boolean): ReadingPalette = when (this) {
    ReadingTheme.PAPER -> Paper
    ReadingTheme.SEPIA -> Sepia
    ReadingTheme.NIGHT -> Night
    ReadingTheme.OLED -> Oled
    ReadingTheme.AUTO -> if (systemDarkMode) Night else Paper
}

val LocalReadingPalette = staticCompositionLocalOf { Paper }

/** The reading palette in effect for the current composition. */
val currentReadingPalette: ReadingPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalReadingPalette.current

/**
 * Highlight swatches — §4.3. Fills are tone-matched per canvas: the light fill on
 * Paper/Sepia, the dark fill on Night/OLED.
 */
data class HighlightSwatch(
    val lightFill: Color,
    val darkFill: Color
) {
    fun fillFor(palette: ReadingPalette): Color = if (palette.isDark) darkFill else lightFill
}

val HighlightSwatches: Map<HighlightColor, HighlightSwatch> = mapOf(
    // Yellow #F2CD55 @ 55 % · #A08019 @ 65 %
    HighlightColor.YELLOW to HighlightSwatch(
        Color(0xFFF2CD55).copy(alpha = 0.55f),
        Color(0xFFA08019).copy(alpha = 0.65f)
    ),
    // Green #82D199 @ 50 % · #407E52 @ 62 %
    HighlightColor.GREEN to HighlightSwatch(
        Color(0xFF82D199).copy(alpha = 0.50f),
        Color(0xFF407E52).copy(alpha = 0.62f)
    ),
    // Blue #82B8F0 @ 50 % · #3A6896 @ 62 %
    HighlightColor.BLUE to HighlightSwatch(
        Color(0xFF82B8F0).copy(alpha = 0.50f),
        Color(0xFF3A6896).copy(alpha = 0.62f)
    ),
    // Pink #F099C0 @ 50 % · #96486C @ 62 %
    HighlightColor.PINK to HighlightSwatch(
        Color(0xFFF099C0).copy(alpha = 0.50f),
        Color(0xFF96486C).copy(alpha = 0.62f)
    ),
    // Orange #F5B478 @ 55 % · #9E6834 @ 62 %
    HighlightColor.ORANGE to HighlightSwatch(
        Color(0xFFF5B478).copy(alpha = 0.55f),
        Color(0xFF9E6834).copy(alpha = 0.62f)
    )
)

/** The active-highlight ring: primary 2 dp with a 24 % halo (§4.3). */
val HighlightRingAlpha = 0.24f

/**
 * Find-in-file marks (§4.3). All matches use the yellow swatch at 60 % with an amber
 * outline; the current match is solid #FF8A3C with white text. Highlight colours are
 * never reused for search marks.
 */
val FindMatchFill = Color(0xFFF2CD55).copy(alpha = 0.60f)
val FindMatchOutline = Color(0xFFFFB020)
val FindCurrentMatch = Color(0xFFFF8A3C)
val FindCurrentMatchContent = Color(0xFFFFFFFF)
