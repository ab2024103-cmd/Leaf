package app.leaf.reader.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.ui.theme.LeafTheme
import app.leaf.reader.feature.library.LibraryScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Library at 360 / 412 / 700 dp, light and dark (§13 M1, §12). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LibraryScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun library_360_light() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.LIGHT) {
                LibraryScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("library_360_light.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun library_360_dark() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.DARK) {
                LibraryScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("library_360_dark.png")
    }

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun library_412_light() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.LIGHT) {
                LibraryScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("library_412_light.png")
    }

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun library_412_dark() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.DARK) {
                LibraryScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("library_412_dark.png")
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun library_700_light() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.LIGHT) {
                LibraryScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("library_700_light.png")
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun library_700_dark() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.DARK) {
                LibraryScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("library_700_dark.png")
    }
}
