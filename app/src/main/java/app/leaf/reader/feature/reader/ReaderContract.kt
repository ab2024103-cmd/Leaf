package app.leaf.reader.feature.reader

import app.leaf.reader.core.format.DocumentEngine
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir

/** A sheet is present only while it has a live M3 action. */
enum class ReaderSheet { NONE, JUMP_TO_PAGE, VIEW_LAYOUT }

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
    val textExtractionProgress: Float? = null
) {
    val percentRead: Int
        get() = if (pageCount <= 0) 0 else (((pageIndex + 1).coerceAtMost(pageCount) * 100f) / pageCount).toInt()
}
