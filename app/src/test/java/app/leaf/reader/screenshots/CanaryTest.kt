package app.leaf.reader.screenshots

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * TEMPORARY diagnostic: which SDK / test rule actually renders under Robolectric?
 * A full-screen red box with blue text is unmistakable in the output.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class)
class CanaryTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val androidComposeRule = createAndroidComposeRule<ComponentActivity>()

    @Composable
    private fun content() {
        Box(Modifier.fillMaxSize().background(Color.Red)) {
            Text("canary", color = Color.Blue)
        }
    }

    @Test
    @Config(sdk = [30], qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun canary_sdk30() {
        composeRule.setContent { content() }
        composeRule.onRoot().captureRoboImage("canary_sdk30.png")
    }

    @Test
    @Config(sdk = [33], qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun canary_sdk33() {
        composeRule.setContent { content() }
        composeRule.onRoot().captureRoboImage("canary_sdk33.png")
    }

    @Test
    @Config(sdk = [35], qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun canary_sdk35() {
        composeRule.setContent { content() }
        composeRule.onRoot().captureRoboImage("canary_sdk35.png")
    }

    @Test
    @Config(sdk = [35], qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun canary_sdk35_androidRule() {
        androidComposeRule.setContent { content() }
        androidComposeRule.onRoot().captureRoboImage("canary_sdk35_androidRule.png")
    }
}
