package app.leaf.reader

import android.content.pm.ActivityInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Crash sweep (M8), run on the API 23 and current emulators by the Real PDF job.
 *
 * Twenty rounds of: open the seeded demo PDF and back out, rotate, background then
 * foreground, and visit Search and Settings. An uncaught exception fails the test.
 *
 * Not covered yet: annotate and share. Both need a text-selection gesture or the system
 * share chooser, which are not automated here.
 */
@RunWith(AndroidJUnit4::class)
class AppCrashSweepInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun twenty_rounds_of_core_flows_do_not_crash() {
        composeRule.waitForIdle()
        // The demo library is seeded in the background at app start, so wait for its first row.
        try {
            composeRule.waitUntil(timeoutMillis = SEED_TIMEOUT_MS) {
                composeRule.onAllNodesWithText(DEMO_DOCUMENT, substring = true).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (timeout: androidx.compose.ui.test.ComposeTimeoutException) {
            // Put the visible tree in the failure message so the CI annotation shows what was on screen.
            val visible = composeRule.onAllNodes(hasText("", substring = true)).fetchSemanticsNodes()
                .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" | ") { it.text } }
            throw AssertionError(
                "Demo document '$DEMO_DOCUMENT' not on screen. Visible text (${visible.size}):\n" + visible.joinToString("\n"),
                timeout
            )
        }
        repeat(ROUNDS) {
            composeRule.onNodeWithText(DEMO_DOCUMENT, substring = true).performClick()
            composeRule.waitForIdle()
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
            composeRule.waitForIdle()

            composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            composeRule.waitForIdle()
            composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            composeRule.waitForIdle()

            composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            composeRule.waitForIdle()

            composeRule.onAllNodesWithText("Search").onFirst().performClick()
            composeRule.waitForIdle()
            composeRule.onAllNodesWithText("Settings").onFirst().performClick()
            composeRule.waitForIdle()
            composeRule.onAllNodesWithText("Library").onFirst().performClick()
            composeRule.waitForIdle()
        }
    }

    private companion object {
        const val ROUNDS = 20
        const val SEED_TIMEOUT_MS = 30_000L
        const val DEMO_DOCUMENT = "The Art of Simple Living"
    }
}
