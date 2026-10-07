package app.leaf.reader.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import app.leaf.reader.FIXED_NOW
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.ui.theme.LeafTheme
import app.leaf.reader.navigation.LeafDestination
import app.leaf.reader.navigation.LeafShell
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Library at 360 / 412 / 700 dp, light and dark (§13 M1, §12).
 *
 * The whole shell is rendered — so the navigation bar below 600 dp and the rail at
 * and above it are part of every baseline — and captured through the native view
 * hierarchy (Roborazzi's Compose bridge renders blank with this toolchain).
 *
 * The Library screen reads its ViewModel from Koin, so the test application builds the
 * app's real [app.leaf.reader.di.appModule] graph but omits the production background
 * seeder. The demo library is seeded synchronously at [FIXED_NOW], so the rows, relative
 * dates and chip counts are identical in Roborazzi's record and verify runs.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = LibraryScreenshotApplication::class, sdk = [33])
class LibraryScreenshotTest : KoinComponent {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val seeder: DatabaseSeeder by inject()

    @Before
    fun seedDemoLibrary() {
        runBlocking(Dispatchers.IO) { seeder.seed(FIXED_NOW) }
    }

    @After
    fun stopKoinGraph() {
        // Koin is process-global; Robolectric creates a fresh screenshot Application
        // for the next capture but runs the tests in the same JVM.
        if (GlobalContext.getOrNull() != null) stopKoin()
    }

    private fun capture(name: String, dark: Boolean, widthDp: Int, heightDp: Int) {
        composeRule.setContent {
            LeafTheme(appTheme = if (dark) AppTheme.DARK else AppTheme.LIGHT) {
                LeafShell(
                    windowSizeClass = WindowSizeClass.calculateFromSize(
                        DpSize(widthDp.dp, heightDp.dp)
                    ),
                    startDestination = LeafDestination.LIBRARY
                )
            }
        }
        // The app uses Room Flow -> combine -> StateFlow. Wait until the seeded library
        // has reached the actual semantics tree; capturing the first frame records the
        // StateFlow's intentionally empty initial value instead of the screen (§12).
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("10 DOCUMENTS").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.activity.window.decorView.captureRoboImage("$name.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun library_360_light() = capture("library_360_light", dark = false, widthDp = 360, heightDp = 760)

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun library_360_dark() = capture("library_360_dark", dark = true, widthDp = 360, heightDp = 760)

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun library_412_light() = capture("library_412_light", dark = false, widthDp = 412, heightDp = 892)

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun library_412_dark() = capture("library_412_dark", dark = true, widthDp = 412, heightDp = 892)

    @Test
    @Config(qualifiers = "w700dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun library_700_light() = capture("library_700_light", dark = false, widthDp = 700, heightDp = 900)

    @Test
    @Config(qualifiers = "w700dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun library_700_dark() = capture("library_700_dark", dark = true, widthDp = 700, heightDp = 900)
}
