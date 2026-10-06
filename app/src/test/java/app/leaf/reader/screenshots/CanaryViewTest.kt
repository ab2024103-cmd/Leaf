package app.leaf.reader.screenshots

import android.app.Application
import android.graphics.Color
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * TEMPORARY diagnostic: can Robolectric render a plain Android View at all, and
 * does the graphics mode matter? Rules out Compose entirely.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class CanaryViewTest {

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun canary_view_native() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = TextView(activity).apply {
            setBackgroundColor(Color.RED)
            text = "canary"
        }
        activity.setContentView(view)
        view.captureRoboImage("canary_view_native.png")
    }

    @Test
    fun canary_view_default() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = TextView(activity).apply {
            setBackgroundColor(Color.RED)
            text = "canary"
        }
        activity.setContentView(view)
        view.captureRoboImage("canary_view_default.png")
    }
}
