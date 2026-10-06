package app.leaf.reader.screenshots

import app.leaf.reader.MainActivity
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * TEMPORARY diagnostic: capture the real MainActivity through the native view
 * hierarchy instead of Roborazzi's Compose bridge, which renders blank with this
 * toolchain.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class CanaryActivityTest {

    @After
    fun tearDown() {
        org.koin.core.context.stopKoin()
    }

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun canary_activity_360() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        activity.captureRoboImage("canary_activity_360.png")
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun canary_activity_700() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        activity.captureRoboImage("canary_activity_700.png")
    }
}
