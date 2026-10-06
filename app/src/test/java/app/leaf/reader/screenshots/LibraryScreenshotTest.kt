package app.leaf.reader.screenshots

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.ui.theme.LeafTheme
import app.leaf.reader.navigation.LeafDestination
import app.leaf.reader.navigation.LeafShell
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
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
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [33])
class LibraryScreenshotTest {

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

