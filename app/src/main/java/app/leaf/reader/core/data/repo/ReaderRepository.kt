package app.leaf.reader.core.data.repo

import app.leaf.reader.core.data.db.DocumentDao
import app.leaf.reader.core.data.db.DocumentEntity
import app.leaf.reader.core.data.db.DocumentTagDao
import app.leaf.reader.core.data.db.ExtractedTextDao
import app.leaf.reader.core.data.db.ExtractedTextEntity
import app.leaf.reader.core.data.db.ProgressDao
import app.leaf.reader.core.data.db.ProgressEntity
import app.leaf.reader.core.data.db.RecentDao
import app.leaf.reader.core.data.db.RecentEntity
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.Orientation
import app.leaf.reader.core.model.Progress
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Reader-only persistence: document metadata, recents, Page Stay and extracted page text. */
class ReaderRepository(
    private val documents: DocumentDao,
    private val documentTags: DocumentTagDao,
    private val positions: ProgressDao,
    private val recents: RecentDao,
    private val extractedText: ExtractedTextDao
) {
    fun observeDocument(id: String): Flow<Document?> = combine(
        documents.observe(id),
        documentTags.observeAll()
    ) { document, tags ->
        document?.toModel(tags.filter { it.docId == id }.map { it.tagName }.sorted())
    }

    suspend fun document(id: String): Document? {
        val row = documents.get(id) ?: return null
        return row.toModel(documentTags.getForDocument(id).map { it.tagName }.sorted())
    }

    fun observeProgress(id: String): Flow<Progress?> = positions.observe(id).map { it?.toModel() }

    suspend fun progress(id: String): Progress? = positions.get(id)?.toModel()

    suspend fun saveProgress(progress: Progress) {
        positions.insert(progress.toEntity())
    }

    suspend fun recordOpen(id: String, openedAt: Long) {
        documents.updateLastOpened(id, openedAt)
        recents.insert(RecentEntity(id, openedAt))
        recents.keepNewest(60)
    }

    suspend fun registerPdf(uri: String, name: String, sizeBytes: Long, pageCount: Int, now: Long): String {
        val id = "pdf-${UUID.nameUUIDFromBytes(uri.toByteArray(Charsets.UTF_8))}"
        val existing = documents.get(id)
        if (existing == null) {
            documents.insert(
                DocumentEntity(
                    id = id,
                    name = name,
                    uri = uri,
                    type = DocType.PDF,
                    sizeBytes = sizeBytes.coerceAtLeast(0L),
                    pageCount = pageCount,
                    dateAdded = now,
                    lastOpened = now,
                    favorite = false,
                    favoritedAt = null,
                    folderId = null,
                    orientation = Orientation.AUTO
                )
            )
        } else {
            documents.updatePageCount(id, pageCount)
            documents.updateLastOpened(id, now)
        }
        recordOpen(id, now)
        return id
    }

    suspend fun updatePageCount(id: String, pageCount: Int) = documents.updatePageCount(id, pageCount)

    suspend fun cachedText(id: String): List<ExtractedTextEntity> = extractedText.pagesForDocument(id)

    suspend fun cacheText(id: String, pages: List<String>) {
        extractedText.insertAll(pages.mapIndexed { index, text -> ExtractedTextEntity(id, index, text) })
    }

    private fun DocumentEntity.toModel(tags: List<String>) = Document(
        id = id,
        name = name,
        type = type,
        sizeBytes = sizeBytes,
        uri = uri,
        pageCount = pageCount,
        dateAdded = dateAdded,
        lastOpened = lastOpened,
        favorite = favorite,
        favoritedAt = favoritedAt,
        folderId = folderId,
        orientation = orientation,
        tags = tags
    )

    private fun ProgressEntity.toModel() = Progress(
        docId = docId,
        page = page,
        scrollFraction = scrollFraction,
        zoom = zoom,
        scrollDir = scrollDir,
        updatedAt = updatedAt,
        readingTheme = readingTheme,
        panX = panX,
        panY = panY
    )

    private fun Progress.toEntity() = ProgressEntity(
        docId = docId,
        page = page,
        scrollFraction = scrollFraction,
        zoom = zoom,
        scrollDir = scrollDir,
        updatedAt = updatedAt,
        readingTheme = readingTheme,
        panX = panX,
        panY = panY
    )
}
