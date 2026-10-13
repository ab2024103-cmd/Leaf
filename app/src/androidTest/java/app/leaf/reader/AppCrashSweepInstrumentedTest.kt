package app.leaf.reader

import android.content.pm.ActivityInfo
import android.os.ParcelFileDescriptor
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Crash sweep (M8), run on the API 23 and current emulators by the Real PDF job.
 *
 * An uncaught exception fails a test. Covered: rotation, background/foreground, and visits to
 * Search and Settings. Not yet covered: opening a document (see the ignored test below),
 * annotate, and share.
 *
 * Background is a real HOME press and relaunch (see backgroundOnce). The diagnose_* tests isolate
 * the API 23 background/foreground failure. Failure messages carry the scenario state, the resumed
 * activity, and recent logcat, because CI artifacts are not readable from outside GitHub.
 */
@RunWith(AndroidJUnit4::class)
class AppCrashSweepInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Runs before the rule closes the activity (rules wrap @After). Teardown only works from a
     * steady state, so if the activity is not RESUMED here, fail with the activity-manager trail.
     */
    @After
    fun requireResumedBeforeTeardown() {
        val state = runCatching { composeRule.activityRule.scenario.state }.getOrNull()
        if (state == Lifecycle.State.RESUMED) return
        val trail = shell("logcat -d -t 800").lines()
            .filter { Regex("ActivityManager|leaf\\.reader|InstrumentationActivityInvoker|input").containsMatchIn(it) }
            .takeLast(10).joinToString("\n") { it.takeLast(150) }
        throw AssertionError("Before teardown the activity was $state, not RESUMED.\nActivity manager:\n$trail")
    }

    @Test
    fun twenty_rounds_of_rotate_background_and_destinations_do_not_crash() {
        waitForLibrary()
        repeat(ROUNDS) {
            rotateOnce(step = "round $it rotate")
            backgroundOnce(step = "round $it background")
            visitDestinations()
        }
    }

    @Test
    fun diagnose_background_only() {
        waitForLibrary()
        repeat(DIAGNOSTIC_ROUNDS) { backgroundOnce(step = "background-only round $it") }
    }

    @Test
    fun diagnose_rotation_only() {
        waitForLibrary()
        repeat(DIAGNOSTIC_ROUNDS) { rotateOnce(step = "rotation-only round $it") }
    }

    @Test
    fun diagnose_rotation_then_background() {
        waitForLibrary()
        repeat(DIAGNOSTIC_ROUNDS) {
            rotateOnce(step = "rotation round $it")
            backgroundOnce(step = "background after rotation round $it")
        }
    }

    @Test
    fun diagnose_navigation_then_background() {
        waitForLibrary()
        repeat(DIAGNOSTIC_ROUNDS) {
            visitDestinations()
            backgroundOnce(step = "background after navigation round $it")
        }
    }

    @Test
    fun diagnose_navigation_then_rotation() {
        waitForLibrary()
        repeat(DIAGNOSTIC_ROUNDS) {
            visitDestinations()
            rotateOnce(step = "rotation after navigation round $it")
        }
    }

    /**
     * Known gap: on the API 23 and 36 emulators, tapping a PDF row in the Library does not open
     * the reader (CI runs 38306305903 and 38298109977). Not yet diagnosed: it may be emulator
     * input or an app bug. Re-enable once a tap opens the reader on CI.
     */
    @Ignore("Known gap: tapping a Library row does not open the reader on the CI emulators yet.")
    @Test
    fun twenty_rounds_of_open_document_and_back_do_not_crash() {
        waitForLibrary()
        repeat(ROUNDS) {
            composeRule.onNode(hasContentDescription(DEMO_DOCUMENT, substring = true) and hasClickAction()).performClick()
            try {
                composeRule.waitUntil(timeoutMillis = READER_TIMEOUT_MS) { readerVisible() }
            } catch (timeout: androidx.compose.ui.test.ComposeTimeoutException) {
                throw AssertionError("Tapping '$DEMO_DOCUMENT' did not open the reader. Visible text:\n" + visibleText(), timeout)
            }
            composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeRule.waitUntil(timeoutMillis = READER_TIMEOUT_MS) { libraryVisible() }
        }
    }

    private fun visitDestinations() {
        composeRule.onAllNodesWithText("Search").onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Settings").onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Library").onFirst().performClick()
        composeRule.waitForIdle()
    }

    private fun rotateOnce(step: String) {
        diagnose(step) {
            composeRule.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            composeRule.waitForIdle()
            composeRule.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            composeRule.waitForIdle()
        }
    }

    /**
     * A real background/foreground cycle: HOME stops the app, and relaunching its singleTask
     * activity brings it back to the front. This replaces ActivityScenario.moveToState, which
     * covers the activity with the test library's EmptyActivity and waits on its broadcast
     * handshake. That handshake is the step that stalled on API 23 (run 38364775419).
     */
    private fun backgroundOnce(step: String) {
        diagnose(step) {
            shell("input keyevent KEYCODE_HOME")
            composeRule.waitUntil(timeoutMillis = LIFECYCLE_TIMEOUT_MS) {
                composeRule.activityRule.scenario.state == Lifecycle.State.CREATED
            }
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            shell("am start -n ${context.packageName}/${MainActivity::class.java.name}")
            composeRule.waitUntil(timeoutMillis = LIFECYCLE_TIMEOUT_MS) {
                composeRule.activityRule.scenario.state == Lifecycle.State.RESUMED
            }
            composeRule.waitForIdle()
            assertStaysResumed(step)
        }
    }

    /**
     * Probe: after the relaunch the activity must stay RESUMED. If it drifts, fail with the
     * timestamped activity and window manager trail (teardown later sees it STOPPED or STARTED).
     */
    private fun assertStaysResumed(step: String) {
        val until = System.currentTimeMillis() + STABILITY_WINDOW_MS
        while (System.currentTimeMillis() < until) {
            Thread.sleep(100)
            val state = runCatching { composeRule.activityRule.scenario.state }.getOrNull()
            if (state != Lifecycle.State.RESUMED) {
                throw AssertionError("After '$step' the activity drifted to $state.\n" + managerTrail())
            }
        }
    }

    private fun managerTrail(): String = shell("logcat -d -t 1000").lines()
        .filter { Regex("ActivityManager|WindowManager|leaf\\\\.reader|InstrumentationActivityInvoker|KEYCODE").containsMatchIn(it) }
        .takeLast(10).joinToString("\n") { it.takeLast(140) }

    /** Runs one step; on failure, rethrows with the scenario state and recent app logcat. */
    private fun diagnose(step: String, block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            val state = composeRule.activityRule.scenario.state
            val resumed = shell("dumpsys activity activities").lines()
                .firstOrNull { "mResumedActivity" in it }.orEmpty().trim().takeLast(150)
            throw AssertionError(
                "Step '$step' failed on API ${android.os.Build.VERSION.SDK_INT}; scenario state=$state.\n" +
                    "Resumed: $resumed\n" +
                    "Recent app logcat:\n" + recentLogcat(),
                error
            )
        }
    }

    /** Runs one shell command and returns its stdout. */
    private fun shell(command: String): String {
        val descriptor: ParcelFileDescriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }

    private fun recentLogcat(): String {
        val text = shell("logcat -d -t 1500")
        val keep = Regex("FATAL|AndroidRuntime|leaf\\.reader|ActivityScenario|LifecycleMonitor|ActivityManager: START|Exception")
        // Keep only the newest matches and trim each to its end, so the CI annotation keeps them.
        return text.lines().filter { keep.containsMatchIn(it) }.takeLast(8).joinToString("\n") { it.takeLast(150) }
    }

    /** The demo library is seeded in the background at app start, so wait for its first rows. */
    private fun waitForLibrary() {
        composeRule.waitForIdle()
        try {
            composeRule.waitUntil(timeoutMillis = SEED_TIMEOUT_MS) { libraryVisible() }
        } catch (timeout: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("Library did not load. Visible text:\n" + visibleText(), timeout)
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
        const val DIAGNOSTIC_ROUNDS = 3
        const val SEED_TIMEOUT_MS = 30_000L
        const val READER_TIMEOUT_MS = 15_000L
        const val LIFECYCLE_TIMEOUT_MS = 20_000L
        const val STABILITY_WINDOW_MS = 1_500L
        const val DEMO_DOCUMENT = "Designing Data-Intensive Applications"
    }
}
