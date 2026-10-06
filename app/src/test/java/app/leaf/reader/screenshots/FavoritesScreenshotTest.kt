package app.leaf.reader.screenshots

import android.app.Application

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.ui.theme.LeafTheme
import app.leaf.reader.feature.favorites.FavoritesScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Favorites at 360 / 412 / 700 dp, light and dark (§13 M1, §12). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FavoritesScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun favorites_360_light() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.LIGHT) {
                FavoritesScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("favorites_360_light.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun favorites_360_dark() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.DARK) {
                FavoritesScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("favorites_360_dark.png")
    }

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun favorites_412_light() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.LIGHT) {
                FavoritesScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("favorites_412_light.png")
    }

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun favorites_412_dark() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.DARK) {
                FavoritesScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("favorites_412_dark.png")
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun favorites_700_light() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.LIGHT) {
                FavoritesScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("favorites_700_light.png")
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun favorites_700_dark() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.DARK) {
                FavoritesScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("favorites_700_dark.png")
    }
}
