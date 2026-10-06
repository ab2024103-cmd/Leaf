package app.leaf.reader.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Leaf Material 3 colour roles — LEAF-MASTER-PROMPT.md §4.1.
 * TonalSpot scheme generated from the brand seed. Values copied verbatim.
 */
private val LeafGreen = Color(0xFF2B7A5B)

/** Brand seed — also the app icon base. */
val Seed = LeafGreen

val LeafLight = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF206A4E), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8F2CE), onPrimaryContainer = Color(0xFF002114),
    inversePrimary = Color(0xFF8DD5B3),
    secondary = Color(0xFF4D6357), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE9D9), onSecondaryContainer = Color(0xFF0A1F16),
    tertiary = Color(0xFF3D6373), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E9FB), onTertiaryContainer = Color(0xFF001F29),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FBF5), onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF5FBF5), onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DD), onSurfaceVariant = Color(0xFF404943),
    surfaceDim = Color(0xFFD6DBD6), surfaceBright = Color(0xFFF5FBF5),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFEFF5EF),
    surfaceContainer = Color(0xFFEAEFE9), surfaceContainerHigh = Color(0xFFE4EAE4),
    surfaceContainerHighest = Color(0xFFDEE4DE),
    outline = Color(0xFF707973), outlineVariant = Color(0xFFBFC9C2),
    inverseSurface = Color(0xFF2C322E), inverseOnSurface = Color(0xFFEDF2EC),
    scrim = Color(0xFF000000)
)

val LeafDark = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFF8DD5B3), onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF005138), onPrimaryContainer = Color(0xFFA8F2CE),
    inversePrimary = Color(0xFF206A4E),
    secondary = Color(0xFFB3CCBE), onSecondary = Color(0xFF1F352A),
    secondaryContainer = Color(0xFF354B40), onSecondaryContainer = Color(0xFFCFE9D9),
    tertiary = Color(0xFFA5CCDF), onTertiary = Color(0xFF073543),
    tertiaryContainer = Color(0xFF244C5B), onTertiaryContainer = Color(0xFFC1E9FB),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1511), onBackground = Color(0xFFDEE4DE),
    surface = Color(0xFF0F1511), onSurface = Color(0xFFDEE4DE),
    surfaceVariant = Color(0xFF404943), onSurfaceVariant = Color(0xFFBFC9C2),
    surfaceDim = Color(0xFF0F1511), surfaceBright = Color(0xFF353B37),
    surfaceContainerLowest = Color(0xFF0A0F0C), surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211D), surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303632),
    outline = Color(0xFF8A938C), outlineVariant = Color(0xFF404943),
    inverseSurface = Color(0xFFDEE4DE), inverseOnSurface = Color(0xFF2C322E),
    scrim = Color(0xFF000000)
)
