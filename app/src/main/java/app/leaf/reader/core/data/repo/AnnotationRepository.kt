package app.leaf.reader.core.data.repo

import app.leaf.reader.core.data.db.BookmarkDao
import app.leaf.reader.core.data.db.BookmarkEntity
import app.leaf.reader.core.data.db.HighlightDao
import app.leaf.reader.core.data.db.HighlightEntity
import app.leaf.reader.core.model.Bookmark
import app.leaf.reader.core.model.Highlight
import app.leaf.reader.core.model.NormalizedRect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persistence for the reader's PDF annotations; Room remains the source of truth. */
class AnnotationRepository(
    private val bookmarks: BookmarkDao,
    private val highlights: HighlightDao
) {
    fun observeBookmarks(docId: String): Flow<List<Bookmark>> =
        bookmarks.observeForDocument(docId).map { rows -> rows.map(BookmarkEntity::toModel) }

    fun observeHighlights(docId: String): Flow<List<Highlight>> =
        highlights.observeForDocument(docId).map { rows -> rows.map(HighlightEntity::toModel) }

    suspend fun bookmarks(docId: String): List<Bookmark> =
        bookmarks.getForDocument(docId).map(BookmarkEntity::toModel)

    suspend fun highlights(docId: String): List<Highlight> =
        highlights.getForDocument(docId).map(HighlightEntity::toModel)

    suspend fun insert(bookmark: Bookmark) = bookmarks.insert(bookmark.toEntity())

    suspend fun deleteBookmark(id: String) = bookmarks.delete(id)

    suspend fun replaceBookmarks(docId: String, rows: List<Bookmark>) =
        bookmarks.replaceForDocument(docId, rows.map(Bookmark::toEntity))

    suspend fun insert(highlight: Highlight) = highlights.insert(highlight.toEntity())

    suspend fun deleteHighlight(id: String) = highlights.delete(id)

    suspend fun replaceHighlights(docId: String, rows: List<Highlight>) =
        highlights.replaceForDocument(docId, rows.map(Highlight::toEntity))

    private fun BookmarkEntity.toModel() = Bookmark(
        id = id,
        docId = docId,
        page = page,
        label = label,
        createdAt = createdAt
    )

    private fun Bookmark.toEntity() = BookmarkEntity(
        id = id,
        docId = docId,
        page = page,
        label = label,
        createdAt = createdAt
    )

    private fun HighlightEntity.toModel() = Highlight(
        id = id,
        docId = docId,
        page = page,
        color = color,
        text = text,
        bounds = Converters.rects(bounds),
        textRange = if (rangeStart != null && rangeEnd != null && rangeEnd > rangeStart) {
            rangeStart until rangeEnd
        } else null,
        cfiRange = cfiRange,
        createdAt = createdAt
    )

    private fun Highlight.toEntity() = HighlightEntity(
        id = id,
        docId = docId,
        page = page,
        color = color,
        text = text,
        bounds = Converters.rects(bounds),
        rangeStart = textRange?.first,
        rangeEnd = textRange?.let { if (it.isEmpty()) it.first else it.last + 1 },
        cfiRange = cfiRange,
        createdAt = createdAt
    )

    private object Converters {
        fun rects(encoded: String): List<NormalizedRect> {
            if (encoded.isBlank()) return emptyList()
            return encoded.split(';').mapNotNull { value ->
                val fields = value.split(',')
                if (fields.size != 4) return@mapNotNull null
                val coords = fields.map(String::toFloatOrNull)
                if (coords.any { it == null }) return@mapNotNull null
                NormalizedRect(coords[0]!!, coords[1]!!, coords[2]!!, coords[3]!!)
            }
        }

        fun rects(values: List<NormalizedRect>): String = values.joinToString(";") { rect ->
            "${rect.left},${rect.top},${rect.right},${rect.bottom}"
        }
    }
}
