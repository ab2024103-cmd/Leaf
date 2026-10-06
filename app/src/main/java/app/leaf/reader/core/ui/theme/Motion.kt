package app.leaf.reader.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

/**
 * Motion — LEAF-MASTER-PROMPT.md §4.5. Durations are in milliseconds; every animation
 * is skipped when the Animations setting is off or when
 * `Settings.Global.ANIMATOR_DURATION_SCALE` is 0.
 */
object LeafMotion {
    /** Screen switch, shared axis (fade + 14 dp slide). */
    const val screenSwitch = 250
    /** Sheets, emphasized. */
    const val sheet = 250
    const val pageTurn = 250
    const val ripple = 450
    /** Snackbar: 2 200 ms, or 4 200 ms when it offers an action. */
    const val snackbar = 2_200L
    const val snackbarWithAction = 4_200L

    /** Long-press threshold, with a 0.97 press scale. */
    const val longPress = 470L
    const val pressScale = 0.97f

    /** Shared-axis slide distance. */
    val slideDistance = 14.dp

    /** Haptics: 18 ms on long-press, 8–10 ms on highlight / bookmark. */
    const val hapticLongPress = 18L
    const val hapticLight = 9L

    val easeStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val easeEmphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}
