package app.leaf.reader.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.model.ReadingTheme

/**
 * The Leaf theme: M3 colour roles for the chrome, a reading palette for the document
 * canvas. The two axes are independent (§1.6).
 *
 * @param appTheme App chrome theme.
 * @param dynamicColor Material You dynamic colour (§1.4). Off by default, Android 12+.
 *   Guarded: dynamic colour does not exist below API 31.
 * @param readingTheme Document canvas theme. AUTO resolves at render time (§4.2).
 */
@Composable
fun LeafTheme(
    appTheme: AppTheme = AppTheme.LIGHT,
    dynamicColor: Boolean = false,
    readingTheme: ReadingTheme = ReadingTheme.PAPER,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colorScheme = remember(darkTheme, dynamicColor, context) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> LeafDark
            else -> LeafLight
        }
    }

    // Status-bar icon contrast on API 23+ (§9): go through androidx, never the raw
    // Window.setDecorFitsSystemWindows / systemUiVisibility APIs.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val palette = remember(readingTheme, darkTheme) { readingTheme.resolve(darkTheme) }
    CompositionLocalProvider(LocalReadingPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LeafM3Typography,
            content = content
        )
    }
}
