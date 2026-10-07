package app.leaf.reader.screenshots

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.leaf.reader.FIXED_NOW
import app.leaf.reader.LeafApp
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.ui.theme.LeafTheme
import app.leaf.reader.navigation.LeafDestination
import app.leaf.reader.navigation.LeafShell
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.robolectric.Robolectric
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
 * The Library screen reads its ViewModel from Koin, so the run uses [LeafApp] rather
 * than a bare `Application`: that is the graph the app itself builds. The demo library
 * is then seeded synchronously — at [FIXED_NOW] — so the rows, the relative dates and
 * the chip counts are identical on every run.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = LeafApp::class, sdk = [33])
class LibraryScreenshotTest : KoinComponent {

    private val seeder: DatabaseSeeder by inject()

    @Before
    fun seedDemoLibrary() {
        runBlocking(Dispatchers.IO) { seeder.seedIfEmpty(FIXED_NOW) }
    }

    private fun capture(name: String, dark: Boolean, widthDp: Int, heightDp: Int) {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            LeafTheme(appTheme = if (dark) AppTheme.DARK else AppTheme.LIGHT) {
                LeafShell(
                    windowSizeClass = WindowSizeClass.calculateFromSize(
                        DpSize(widthDp.dp, heightDp.dp)
                    ),
                    startDestination = LeafDestination.LIBRARY
                )
            }
        }
        activity.window.decorView.captureRoboImage("$name.png")
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
