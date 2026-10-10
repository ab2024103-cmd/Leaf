package app.leaf.reader.screenshots

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.model.Bookmark
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Highlight
import app.leaf.reader.core.model.HighlightColor
import app.leaf.reader.core.model.NormalizedRect
import app.leaf.reader.core.model.Orientation
import app.leaf.reader.core.model.ReaderTab
import app.leaf.reader.core.model.ReaderTabsState
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.ui.components.LeafSheetOverlay
import app.leaf.reader.core.ui.theme.LeafTheme
import app.leaf.reader.feature.reader.BookmarksSheet
import app.leaf.reader.feature.reader.HighlightsSheet
import app.leaf.reader.feature.reader.OpenDocumentsSheet
import app.leaf.reader.feature.reader.ReaderTabsUiState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** M5 visual coverage for the tab manager and persistent annotation summaries. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [33])
class M5ScreenshotsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val now = System.currentTimeMillis()

    private fun capture(name: String, dark: Boolean = false, sheet: @Composable () -> Unit) {
        composeRule.setContent {
            LeafTheme(appTheme = if (dark) AppTheme.DARK else AppTheme.LIGHT) {
                Box(Modifier.fillMaxSize()) { sheet() }
            }
        }
        composeRule.waitForIdle()
        composeRule.activity.window.decorView.captureRoboImage("$name.png")
    }

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun open_documents_sheet_shows_order_active_position_and_close_controls() {
        val first = Document(
            id = "d1", name = "Q3 Product Roadmap.pdf", type = DocType.PDF,
            sizeBytes = 2_400_000, uri = "leaf-demo://d1", pageCount = 8,
            dateAdded = now - 86_400_000, lastOpened = now - 7_200_000,
            favorite = false, favoritedAt = null, folderId = null,
            orientation = Orientation.AUTO, tags = emptyList()
        )
        val second = first.copy(
            id = "d2", name = "Research Notes.pdf", uri = "leaf-demo://d2",
            pageCount = 5, lastOpened = now - 86_400_000
        )
        val state = ReaderTabsUiState(
            tabs = ReaderTabsState(
                tabs = listOf(
                    ReaderTab("d1", 2, 1.4f, ScrollDir.VERTICAL, null, readingTheme = ReadingTheme.SEPIA),
                    ReaderTab("d2", 0, 1f, ScrollDir.HORIZONTAL, null)
                ),
                activeDocId = "d1"
            ),
            documents = mapOf("d1" to first, "d2" to second),
            tabLimit = 6,
            isLoaded = true
        )

        capture("reader_open_documents_412") {
            OpenDocumentsSheet(
                state = state,
                onDismiss = {},
                onActivate = {},
                onCloseTab = {},
                onPickNewDocument = {},
                onCloseAll = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun bookmarks_sheet_shows_saved_rows_and_current_page_action() {
        val bookmarks = listOf(
            Bookmark("b1", "d1", 1, "Page 2", now - 3_600_000),
            Bookmark("b2", "d1", 4, "Page 5", now - 86_400_000)
        )

        capture("reader_bookmarks_sheet_412") {
            LeafSheetOverlay(onDismiss = {}) {
                BookmarksSheet(
                    bookmarks = bookmarks,
                    currentPage = 2,
                    pageCount = 8,
                    onAddCurrentPage = {},
                    onJump = {},
                    onDelete = {}
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h892dp-normal-long-notround-any-320dpi-keyshidden-nonav")
    fun highlights_sheet_shows_quotes_and_clear_action() {
        val highlights = listOf(
            Highlight(
                id = "h1", docId = "d1", page = 2, color = HighlightColor.YELLOW,
                text = "A quoted passage remains legible across themes.",
                bounds = listOf(NormalizedRect(0.12f, 0.2f, 0.78f, 0.24f)),
                textRange = null, cfiRange = null, createdAt = now - 600_000
            ),
            Highlight(
                id = "h2", docId = "d1", page = 4, color = HighlightColor.BLUE,
                text = "A second saved excerpt.", bounds = emptyList(),
                textRange = null, cfiRange = null, createdAt = now - 172_800_000
            )
        )

        capture("reader_highlights_sheet_412", dark = true) {
            LeafSheetOverlay(onDismiss = {}) {
                HighlightsSheet(
                    highlights = highlights,
                    onJump = {},
                    onDelete = {},
                    onClearAll = {}
                )
            }
        }
    }
}
