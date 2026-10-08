package app.leaf.reader.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import app.leaf.reader.FIXED_NOW
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.data.repo.ReaderRepository
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.model.Progress
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.ui.theme.LeafTheme
import app.leaf.reader.feature.reader.ReaderScreen
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = ReaderScreenshotApplication::class, sdk = [33])
class ReaderScreenshotTest : KoinComponent {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val seeder: DatabaseSeeder by inject()
    private val repository: ReaderRepository by inject()

    @Before
    fun seedPdf() {
        runBlocking(Dispatchers.IO) {
            seeder.seed(FIXED_NOW)
        }
    }

    @After
    fun stopKoinGraph() {
        if (GlobalContext.getOrNull() != null) stopKoin()
    }

    private fun capture(name: String, theme: ReadingTheme, widthDp: Int, heightDp: Int) {
        runBlocking(Dispatchers.IO) {
            repository.saveProgress(
                Progress(
                    docId = "d1",
                    page = 0,
                    scrollFraction = 0f,
                    zoom = 1f,
                    scrollDir = ScrollDir.VERTICAL,
                    updatedAt = FIXED_NOW,
                    readingTheme = theme
                )
            )
        }
        composeRule.setContent {
            LeafTheme(
                appTheme = if (theme == ReadingTheme.PAPER || theme == ReadingTheme.SEPIA) AppTheme.LIGHT else AppTheme.DARK,
                readingTheme = theme
            ) {
                ReaderScreen(documentId = "d1", onClose = {})
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithContentDescription("Page 1").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.activity.window.decorView.captureRoboImage("$name.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun paper_360() = capture("reader_paper_360", ReadingTheme.PAPER, 360, 760)

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun paper_412() = capture("reader_paper_412", ReadingTheme.PAPER, 412, 892)

    @Test
    @Config(qualifiers = "w800dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun paper_800() = capture("reader_paper_800", ReadingTheme.PAPER, 800, 900)

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun sepia_360() = capture("reader_sepia_360", ReadingTheme.SEPIA, 360, 760)

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun sepia_412() = capture("reader_sepia_412", ReadingTheme.SEPIA, 412, 892)

    @Test
    @Config(qualifiers = "w800dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun sepia_800() = capture("reader_sepia_800", ReadingTheme.SEPIA, 800, 900)

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun night_360() = capture("reader_night_360", ReadingTheme.NIGHT, 360, 760)

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun night_412() = capture("reader_night_412", ReadingTheme.NIGHT, 412, 892)

    @Test
    @Config(qualifiers = "w800dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun night_800() = capture("reader_night_800", ReadingTheme.NIGHT, 800, 900)

    @Test
    @Config(qualifiers = "w360dp-h760dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun oled_360() = capture("reader_oled_360", ReadingTheme.OLED, 360, 760)

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun oled_412() = capture("reader_oled_412", ReadingTheme.OLED, 412, 892)

    @Test
    @Config(qualifiers = "w800dp-h900dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun oled_800() = capture("reader_oled_800", ReadingTheme.OLED, 800, 900)
}
