package app.leaf.reader.core.domain

import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.DocType
import kotlin.math.roundToInt

/**
 * A document plus the derived numbers the library rows, group cards and smart
 * collections need. Built once per library emission so the UI never recomputes.
 *
 * @param page The saved reading page, zero-based (Page Stay, §2.2). A document that has
 *   never been opened sits on page 0 with [pctRead] of `100 / pageCount`, matching the
 *   mockup's `pctOf`.
 */
data class LibraryDoc(
    val document: Document,
    val page: Int = 0,
    val bookmarkCount: Int = 0,
    val highlightCount: Int = 0
) {
    val id: String get() = document.id
    val pageCount: Int get() = document.pageCount

    /** Percentage read, counting the current page as read (mockup `pctOf`). */
    val pctRead: Int
        get() = if (pageCount <= 0) 0 else ((page + 1).coerceAtMost(pageCount) * 100f / pageCount).roundToInt()

    /** Percentage read before the current page (mockup `startedOf`). */
    val startedPct: Int
        get() = if (pageCount <= 0) 0 else (page.coerceAtMost(pageCount) * 100f / pageCount).roundToInt()

    /** §6.1: the progress row appears only when the document is started but not finished. */
    val showProgress: Boolean get() = startedPct > 0 && pctRead < 100
}

/** The fixed file-type order of the grouped view (§6.1): PDF → DOCX → XLSX → PPTX → TXT → EPUB. */
val DocType.groupOrder: Int
    get() = when (this) {
        DocType.PDF -> 1
        DocType.DOCX -> 2
        DocType.XLSX -> 3
        DocType.PPTX -> 4
        DocType.TXT -> 5
        DocType.EPUB -> 6
    }

/** One card of the file-type overview (§6.1). */
data class TypeGroupCard(
    val type: DocType,
    val count: Int,
    val sizeBytes: Long,
    val docsWithHighlights: Int,
    val docsWithBookmarks: Int
)
