package app.leaf.reader.feature.reader

import app.leaf.reader.core.format.DocumentEngine
import app.leaf.reader.core.model.Bookmark
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.Highlight
import app.leaf.reader.core.model.HighlightColor
import app.leaf.reader.core.model.NormalizedRect
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir

/** A sheet is present only while it has a live reader action. */
enum class ReaderSheet { NONE, JUMP_TO_PAGE, VIEW_LAYOUT, BOOKMARKS, HIGHLIGHTS, CONFIRM_CLEAR_HIGHLIGHTS }

data class ReaderSelection(
    val pageIndex: Int,
    val text: String,
    val bounds: List<NormalizedRect>,
    val startWord: Int,
    val endWord: Int
)

data class ReaderSnackbarState(
    val message: String,
    val canUndo: Boolean = false
)

data class ReaderFindMatch(
    /** Zero-based page index. */
    val pageIndex: Int,
    /** One or more normalized hit rectangles; empty when the renderer supplies no geometry. */
    val bounds: List<NormalizedRect>
)

data class ReaderContentState(
    val isLoading: Boolean = false,
    val document: Document? = null,
    val engine: DocumentEngine? = null,
    val pageCount: Int = 0,
    val pageIndex: Int = 0,
    /** Fractional position within the current page, normalized to 0..1. */
    val scrollFraction: Float = 0f,
    val zoom: Float = 1f,
    val scrollDir: ScrollDir = ScrollDir.VERTICAL,
    val readingTheme: ReadingTheme = ReadingTheme.PAPER,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val isFullScreen: Boolean = false,
    val sheet: ReaderSheet = ReaderSheet.NONE,
    val error: String? = null,
    val warning: String? = null,
    val textExtractionProgress: Float? = null,
    val findBarVisible: Boolean = false,
    val findQuery: String = "",
    val findMatches: List<ReaderFindMatch> = emptyList(),
    val findIndex: Int = -1,
    val bookmarks: List<Bookmark> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
    val highlightColor: HighlightColor = HighlightColor.YELLOW,
    val selection: ReaderSelection? = null,
    val selectedHighlightId: String? = null,
    val highlightMenuExpanded: Boolean = false,
    val canUndoAnnotation: Boolean = false,
    val canRedoAnnotation: Boolean = false,
    val snackbar: ReaderSnackbarState? = null,
    /** Changed only for explicit jumps so ordinary scrolling is never pulled back. */
    val positionRequestId: Int = 0
) {
    val percentRead: Int
        get() = if (pageCount <= 0) 0 else (((pageIndex + 1).coerceAtMost(pageCount) * 100f) / pageCount).toInt()

    val findMatchCount: Int get() = findMatches.size
    val isCurrentPageBookmarked: Boolean get() = bookmarks.any { it.page == pageIndex }
}
