package app.leaf.reader.core.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import app.leaf.reader.core.model.LeafSettings

/**
 * Whether motion is allowed: the in-app Animations switch and the system animator scale.
 * A system scale of 0 (Developer options → Remove animations) switches motion off too (§4.5).
 */
val LocalLeafMotion = staticCompositionLocalOf { true }

/** Whether haptic ticks are allowed: the in-app Haptic feedback switch (§4.5). */
val LocalLeafHaptics = staticCompositionLocalOf { true }

/** Provides the Settings-driven preferences to everything below the shell. */
@Composable
fun LeafPreferences(settings: LeafSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemScale = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
    CompositionLocalProvider(
        LocalLeafMotion provides (settings.animations && systemScale > 0f),
        LocalLeafHaptics provides settings.haptics,
        content = content
    )
}
