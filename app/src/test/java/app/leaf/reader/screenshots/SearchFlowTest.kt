package app.leaf.reader.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.leaf.reader.FIXED_NOW
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.ui.theme.LeafTheme
import app.leaf.reader.navigation.LeafDestination
import app.leaf.reader.navigation.LeafShell
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

/** Exercises the Search screen through the real result callback into the Reader. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = ReaderScreenshotApplication::class, sdk = [33])
class SearchFlowTest : KoinComponent {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val seeder: DatabaseSeeder by inject()
    private val settings: SettingsStore by inject()

    @Before
    fun seedLibraryAndResetHistory() {
        runBlocking(Dispatchers.IO) {
            seeder.seed(FIXED_NOW)
            settings.update { it.copy(searchHistory = emptyList()) }
        }
    }

    @After
    fun stopKoinGraph() {
        if (GlobalContext.getOrNull() != null) stopKoin()
    }

    @Test
    fun cachedPdfResult_opensReader_atFirstHit_withFindPrefilled() {
        launchSearch()
        composeRule.onNode(hasSetTextAction()).performTextInput("Simplicity")
        waitForText("The Art of Simple Living.pdf")

        composeRule.onNodeWithText("The Art of Simple Living.pdf").performClick()
        waitForText("1 of 1")

        composeRule.onNodeWithTag("reader-find-query").assertTextEquals("Simplicity")
        composeRule.onNodeWithText("1 of 1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Page 1").assertIsDisplayed()
    }

    @Test
    fun committedHistory_canBeReRun_removed_andCleared() {
        launchSearch()
        composeRule.onNode(hasSetTextAction()).performTextInput("Simplicity")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        waitForText("The Art of Simple Living.pdf")

        composeRule.onNodeWithContentDescription("Search history").performClick()
        composeRule.onNodeWithText("Search history").assertIsDisplayed()
        composeRule.onNode(hasText("Simplicity") and hasClickAction()).performClick()
        waitForText("The Art of Simple Living.pdf")

        composeRule.onNodeWithContentDescription("Clear search").performClick()
        waitForText("Recent searches")
        composeRule.onNodeWithContentDescription("Remove “Simplicity” from search history").performClick()
        waitUntilTextAbsent("Recent searches")

        composeRule.onNode(hasSetTextAction()).performTextInput("chapter")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        waitForText("The Art of Simple Living.pdf")
        composeRule.onNodeWithContentDescription("Clear search").performClick()
        waitForText("Recent searches")
        composeRule.onNodeWithText("Clear all").performClick()
        composeRule.onNodeWithText("Clear search history?").assertIsDisplayed()
        composeRule.onNodeWithText("Clear search history").performClick()
        waitUntilTextAbsent("Recent searches")
    }

    private fun launchSearch() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.LIGHT) {
                LeafShell(
                    windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(360.dp, 760.dp)),
                    startDestination = LeafDestination.SEARCH
                )
            }
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntilTextAbsent(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }
}
