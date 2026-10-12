package app.leaf.reader

import android.content.pm.ActivityInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
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
 * Twenty rounds of: open a seeded PDF from the first screen of the library and back out, rotate, background then
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
            throw AssertionError("Demo document '$DEMO_DOCUMENT' not on screen. Visible text:\n" + visibleText(), timeout)
        }
        repeat(ROUNDS) {
            // Tap the merged document row (content description = name, with a click action),
            // not its text child, so the press reaches the row's own clickable.
            composeRule.onNode(hasContentDescription(DEMO_DOCUMENT, substring = true) and hasClickAction()).performClick()
            // Only back out after confirming the reader opened; otherwise back would close the app.
            try {
                composeRule.waitUntil(timeoutMillis = READER_TIMEOUT_MS) { readerVisible() }
            } catch (timeout: androidx.compose.ui.test.ComposeTimeoutException) {
                throw AssertionError("Tapping '$DEMO_DOCUMENT' did not open the reader. Visible text:\n" + visibleText(), timeout)
            }
            composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeRule.waitUntil(timeoutMillis = READER_TIMEOUT_MS) { libraryVisible() }

            composeRule.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            composeRule.waitForIdle()
            composeRule.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
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

    private fun libraryVisible(): Boolean =
        composeRule.onAllNodesWithText("DOCUMENTS", substring = true).fetchSemanticsNodes().isNotEmpty()

    /** The reader's first page carries the content description "Page 1" (see SearchFlowTest). */
    private fun readerVisible(): Boolean =
        composeRule.onAllNodes(hasContentDescription("Page 1")).fetchSemanticsNodes().isNotEmpty()

    /** Every text node on screen, so a failure message shows what the test actually saw. */
    private fun visibleText(): String {
        val lines = composeRule.onAllNodes(hasText("", substring = true)).fetchSemanticsNodes()
            .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" | ") { it.text } }
        return "(${lines.size} nodes)\n" + lines.joinToString("\n")
    }

    private companion object {
        const val ROUNDS = 20
        const val SEED_TIMEOUT_MS = 30_000L
        const val READER_TIMEOUT_MS = 15_000L
        const val DEMO_DOCUMENT = "Designing Data-Intensive Applications"
    }
}
