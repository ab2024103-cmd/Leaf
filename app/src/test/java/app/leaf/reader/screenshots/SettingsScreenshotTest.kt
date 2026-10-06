package app.leaf.reader.screenshots

import android.app.Application

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.ui.theme.LeafTheme
import app.leaf.reader.feature.settings.SettingsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings at 360 / 412 / 700 dp, light and dark (§13 M1, §12). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun settings_360_light() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.LIGHT) {
                SettingsScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("settings_360_light.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun settings_360_dark() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.DARK) {
                SettingsScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("settings_360_dark.png")
    }

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun settings_412_light() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.LIGHT) {
                SettingsScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("settings_412_light.png")
    }

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun settings_412_dark() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.DARK) {
                SettingsScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("settings_412_dark.png")
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun settings_700_light() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.LIGHT) {
                SettingsScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("settings_700_light.png")
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun settings_700_dark() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.DARK) {
                SettingsScreen()
            }
        }
        composeRule.onRoot().captureRoboImage("settings_700_dark.png")
    }
}
