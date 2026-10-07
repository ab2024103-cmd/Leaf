package app.leaf.reader

import android.app.Application
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.testTag
import androidx.compose.ui.unit.dp
import app.leaf.reader.core.ui.theme.LeafMotion
import app.leaf.reader.core.ui.util.leafPressable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The row's actual pointer modifier, not a duplicate timer: a release before 470 ms is a
 * tap; reaching 470 ms opens the actions flow once, and the eventual release is swallowed
 * (§4.5, §6.7).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [33])
class LeafPressTest {

    @get:Rule
    val compose = createComposeRule()

    private val longPresses = AtomicInteger()
    private val taps = AtomicInteger()
    private val pressedTransitions = AtomicInteger()
    private val releasedTransitions = AtomicInteger()

    private fun showPressTarget() {
        compose.setContent {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .testTag("press-target")
                    .leafPressable(
                        onPressChange = { pressed ->
                            if (pressed) pressedTransitions.incrementAndGet()
                            else releasedTransitions.incrementAndGet()
                        },
                        onLongPress = { longPresses.incrementAndGet() },
                        onTap = { taps.incrementAndGet() }
                    )
            )
        }
        compose.waitForIdle()
    }

    /** Hold a real injected pointer while Robolectric advances the main looper's clock. */
    private fun holdFor(durationMs: Long) {
        compose.onNodeWithTag("press-target").performTouchInput { down(center) }
        compose.waitForIdle()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(durationMs, TimeUnit.MILLISECONDS)
        compose.waitForIdle()
        compose.onNodeWithTag("press-target").performTouchInput { up() }
        compose.waitForIdle()
    }

    @Test
    fun release_before_470_ms_is_a_tap_and_never_opens_actions() {
        showPressTarget()

        holdFor(LeafMotion.longPress - 1)

        assertEquals(0, longPresses.get())
        assertEquals(1, taps.get())
        assertEquals(1, pressedTransitions.get())
        assertEquals(1, releasedTransitions.get())
    }

    @Test
    fun reaching_470_ms_opens_actions_once_and_swallows_the_release_tap() {
        showPressTarget()

        holdFor(LeafMotion.longPress)

        assertEquals(1, longPresses.get())
        assertEquals(0, taps.get())
        // A long press clears the visual pressed state before opening the sheet.
        assertEquals(1, pressedTransitions.get())
        assertEquals(1, releasedTransitions.get())
    }
}
