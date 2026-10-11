package app.leaf.reader.core.ui.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import app.leaf.reader.core.ui.theme.LocalLeafHaptics

/**
 * Haptics (§4.5): an 18 ms tick on long-press and a 8–10 ms tick on light actions.
 * Android's own `LONG_PRESS` / `KEYBOARD_TAP` constants are exactly those durations on
 * the platform, so no deprecated `Vibrator.vibrate(milliseconds)` call is needed. The
 * in-app Haptic feedback switch gates both ticks; the system haptics setting still applies.
 */
@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val enabled = LocalLeafHaptics.current
    return Haptics(view, enabled)
}

class Haptics(private val view: View, private val enabled: Boolean = true) {
    fun longPress() = fire(HapticFeedbackConstants.LONG_PRESS)
    fun light() = fire(HapticFeedbackConstants.KEYBOARD_TAP)

    private fun fire(constant: Int) {
        if (!enabled) return
        view.performHapticFeedback(constant)
    }
}
