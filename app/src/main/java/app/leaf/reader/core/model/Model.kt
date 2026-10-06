package app.leaf.reader.core.model

/**
 * Supported document formats (§7.3). [label] is the text drawn inside the file-type
 * icon (§4.6) — file types always carry text so meaning is never colour-only (§10).
 */
enum class DocType(val label: String, val mime: String) {
    PDF("PDF", "application/pdf"),
    DOCX("DOCX", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLSX("XLSX", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PPTX("PPTX", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    TXT("TXT", "text/plain"),
    EPUB("EPUB", "application/epub+zip")
}

/** Highlight colours (§4.3). */
enum class HighlightColor { YELLOW, GREEN, BLUE, PINK, ORANGE }

/** A rectangle in 0..1 page space, independent of zoom and screen size (§7.4). */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/** Per-document orientation lock (§5). */
enum class Orientation { AUTO, PORTRAIT, LANDSCAPE }

/** Sort fields (§6.1). FILE_TYPE is not a sort field — it is its own grouped view (§8.13). */
enum class SortField { NAME, DATE_ADDED, LAST_OPENED, FILE_SIZE }

/** Favorites sort (§6.3) — independent of the library sort. */
enum class FavSort { NAME, ADDED, OPENED }

/** A document in the library (LEAF-SPEC.md §4). */
data class Document(
    val id: String,
    val name: String,
    val type: DocType,
    /** Size in bytes. */
    val sizeBytes: Long,
    val uri: String,
    val pageCount: Int,
    val dateAdded: Long,
    val lastOpened: Long?,
    val favorite: Boolean,
    val favoritedAt: Long?,
    val folderId: String?,
    val orientation: Orientation,
    val tags: List<String>
)

/** Reading position (LEAF-SPEC.md §4, §2.2 Page Stay). */
data class Progress(
    val docId: String,
    val page: Int,
    val scrollFraction: Float,
    val zoom: Float,
    val scrollDir: ScrollDir,
    val updatedAt: Long
)

/** A bookmarked page (§2.2). */
data class Bookmark(
    val id: String,
    val docId: String,
    val page: Int,
    val label: String,
    val createdAt: Long
)

/** A highlight (§7.4). */
data class Highlight(
    val id: String,
    val docId: String,
    val page: Int,
    val color: HighlightColor,
    val text: String,
    /** PDF and the paged Office formats: one rect per line, in 0..1 page space. */
    val bounds: List<NormalizedRect>,
    /** Reflow / TXT / DOCX / XLSX / PPTX / EPUB: character offsets in the extracted text. */
    val textRange: IntRange?,
    /** EPUB only. */
    val cfiRange: String?,
    val createdAt: Long
)

/** A folder (§2.1). Nesting is supported to any depth; the UI indents one level. */
data class Folder(
    val id: String,
    val name: String,
    val colorHex: String,
    val iconKey: String,
    val parentId: String?,
    val isSystem: Boolean
)

/** A tag (§2.1). */
data class Tag(
    val name: String,
    val colorHex: String
)

/** Auto-updating collection rules (§2.1). */
sealed interface SmartRule {
    /** Every document of one type. */
    data class OfType(val type: DocType) : SmartRule
    /** Added within the last [days] days. */
    data class AgeDays(val days: Int) : SmartRule
    /** Started but not finished. */
    data object InProgress : SmartRule
    /** Not in any folder. */
    data object Unfiled : SmartRule
    /** Has at least one highlight. */
    data object HasHighlights : SmartRule
    /** Tagged with [tag]. */
    data class WithTag(val tag: String) : SmartRule
}

/** A smart collection (§2.1). */
data class SmartCollection(
    val id: String,
    val name: String,
    val rule: SmartRule
)

/** A recents entry (§2.1). Capped at 60, unique per document. */
data class RecentEntry(
    val docId: String,
    val openedAt: Long
)

/** An open document (§2.4). State restored when the tab is activated again. */
data class ReaderTab(
    val docId: String,
    val page: Int,
    val zoom: Float,
    val scrollDir: ScrollDir,
    val findQuery: String?
)
