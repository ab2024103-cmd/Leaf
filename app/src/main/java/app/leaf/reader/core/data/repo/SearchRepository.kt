package app.leaf.reader.core.data.repo

import app.leaf.reader.core.data.db.DocumentDao
import app.leaf.reader.core.data.db.DocumentEntity
import app.leaf.reader.core.data.db.DocumentTagDao
import app.leaf.reader.core.data.db.ExtractedTextDao
import app.leaf.reader.core.data.db.FolderDao
import app.leaf.reader.core.data.db.FolderEntity
import app.leaf.reader.core.data.db.TagDao
import app.leaf.reader.core.domain.SearchFilters
import app.leaf.reader.core.domain.SearchResults
import app.leaf.reader.core.domain.SearchScope
import app.leaf.reader.core.domain.SearchableDocument
import app.leaf.reader.core.domain.searchLibrary
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.SortField
import app.leaf.reader.core.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Room-backed metadata and cached-PDF-text search for M4 (§6.4, §7.2). */
class SearchRepository(
    private val documents: DocumentDao,
    private val documentTags: DocumentTagDao,
    private val folders: FolderDao,
    private val tags: TagDao,
    private val extractedText: ExtractedTextDao
) {
    fun observeFolders(): Flow<List<Folder>> = folders.observeAll()
        .map { rows -> rows.filterNot { it.isSystem }.map { it.toModel() } }

    fun observeTags(): Flow<List<Tag>> = tags.observeAll().map { rows -> rows.map { Tag(it.name, it.colorHex) } }

    suspend fun setFavorite(documentId: String, favorite: Boolean, now: Long) {
        documents.setFavorite(documentId, favorite, if (favorite) now else null)
    }

    suspend fun search(
        query: String,
        scope: SearchScope,
        filters: SearchFilters,
        now: Long,
        sortField: SortField,
        sortAscending: Boolean
    ): SearchResults {
        val documentRows = documents.getAll()
        val tagsByDocument = documentTags.getAll().groupBy({ it.docId }, { it.tagName })
        val folderRows = folders.getAll().map(FolderEntity::toModel)
        val shouldReadText = query.isNotBlank() && scope in setOf(SearchScope.EVERYTHING, SearchScope.INSIDE_FILES)
        val pagesByDocument = if (shouldReadText) {
            extractedText.pagesForLibrary()
                .groupBy({ it.docId }, { it.pageIndex to it.text })
                .mapValues { (_, pages) -> pages.sortedBy { it.first }.map { it.second } }
        } else {
            emptyMap()
        }
        val searchable = documentRows.map { row ->
            val document = row.toModel(tagsByDocument[row.id].orEmpty().sorted())
            // The M4 reader opens PDFs only. Non-PDF demo records retain their seeded text
            // for future format support, but are not offered as unopenable in-file hits.
            val pages = if (document.type == DocType.PDF) pagesByDocument[row.id].orEmpty() else emptyList()
            SearchableDocument(document, pages)
        }
        return withContext(Dispatchers.Default) {
            searchLibrary(
                searchable = searchable,
                folders = folderRows,
                query = query,
                scope = scope,
                filters = filters,
                now = now,
                sortField = sortField,
                sortAscending = sortAscending
            )
        }
    }
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

private fun FolderEntity.toModel() = Folder(
    id = id,
    name = name,
    colorHex = colorHex,
    iconKey = iconKey,
    parentId = parentId,
    isSystem = isSystem
)
