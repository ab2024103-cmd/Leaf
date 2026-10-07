package app.leaf.reader.core.ui.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import app.leaf.reader.core.ui.theme.LeafMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The mockup's row gestures: a 470 ms long-press with a press state that the row paints
 * (§4.5), and an optional tap. The timer is a real coroutine rather than
 * `ViewConfiguration.getLongPressTimeoutMillis()` because §4.5 pins the delay to 470 ms
 * and the platform default is 500 ms.
 *
 * After a long press fires, the eventual "up" is swallowed so the row never also taps.
 */
internal suspend fun PointerInputScope.detectLeafPress(
    onPressChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onTap: (() -> Unit)?
) {
    coroutineScope {
        awaitEachGesture {
            awaitFirstDown()
            onPressChange(true)
            var longPressed = false
            val timer = launch {
                delay(LeafMotion.longPress)
                longPressed = true
                onPressChange(false)
                onLongPress()
            }
            val up = waitForUpOrCancellation()
            timer.cancel()
            if (!longPressed) {
                onPressChange(false)
                if (up != null) onTap?.invoke()
            }
        }
    }
}

/**
 * Row press handling for the library (§6.1). [onTap] is null until the milestone that
 * gives the row something to open, so nothing is wired to a dead action.
 */
fun Modifier.leafPressable(
    onPressChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onTap: (() -> Unit)? = null
): Modifier = this.then(
    Modifier.pointerInput(onLongPress) {
        detectLeafPress(onPressChange = onPressChange, onLongPress = onLongPress, onTap = onTap)
    }
)
