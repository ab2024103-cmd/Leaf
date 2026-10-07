package app.leaf.reader.core.ui.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

/**
 * Haptics (§4.5): an 18 ms tick on long-press and a 8–10 ms tick on light actions.
 * Android's own `LONG_PRESS` / `KEYBOARD_TAP` constants are exactly those durations on
 * the platform, so no deprecated `Vibrator.vibrate(milliseconds)` call is needed and the
 * feedback follows the user's system haptics setting (§9).
 */
@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return Haptics(view)
}

class Haptics(private val view: View) {
    fun longPress() = fire(HapticFeedbackConstants.LONG_PRESS)
    fun light() = fire(HapticFeedbackConstants.KEYBOARD_TAP)

    private fun fire(constant: Int) {
        view.performHapticFeedback(constant)
    }
}
