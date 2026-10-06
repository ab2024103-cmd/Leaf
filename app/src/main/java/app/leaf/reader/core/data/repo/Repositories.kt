package app.leaf.reader.core.data.repo

import app.leaf.reader.core.data.db.DocumentDao
import app.leaf.reader.core.data.db.DocumentEntity
import app.leaf.reader.core.data.db.DocumentTagDao
import app.leaf.reader.core.data.db.FolderDao
import app.leaf.reader.core.data.db.RecentDao
import app.leaf.reader.core.data.db.SmartCollectionDao
import app.leaf.reader.core.data.db.SmartCollectionEntity
import app.leaf.reader.core.data.db.TagDao
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.RecentEntry
import app.leaf.reader.core.model.SmartCollection
import app.leaf.reader.core.model.SmartRule
import app.leaf.reader.core.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Documents and their tags (LEAF-SPEC.md §4, §3). */
class DocumentRepository(
    private val documents: DocumentDao,
    private val documentTags: DocumentTagDao
) {

    /** Every document with its tags, newest added first. */
    fun observeDocuments(): Flow<List<Document>> = combine(
        documents.observeAll(),
        documentTags.observeAll()
    ) { docs, tags ->
        val byDoc = tags.groupBy({ it.docId }, { it.tagName })
        docs.map { it.toModel(byDoc[it.id].orEmpty().sorted()) }
    }

    fun observeDocument(id: String): Flow<Document?> = combine(
        documents.observe(id),
        documentTags.observeAll()
    ) { doc, tags ->
        doc?.toModel(tags.filter { it.docId == doc.id }.map { it.tagName }.sorted())
    }

    fun observeCount(): Flow<Int> = documents.observeCount()

    /** Documents that carry every one of [tags] — AND semantics (§2.1). */
    fun observeWithAllTags(tags: List<String>): Flow<List<Document>> =
        observeDocuments().map { docs ->
            if (tags.isEmpty()) docs else docs.filter { doc -> tags.all { it in doc.tags } }
        }
}

/** Folders, including the system "All documents" entry and nested subfolders. */
class FolderRepository(private val folders: FolderDao) {
    fun observeFolders(): Flow<List<Folder>> = folders.observeAll().map { list -> list.map { it.toModel() } }
}

/** Tags (§2.1). */
class TagRepository(private val tags: TagDao) {
    fun observeTags(): Flow<List<Tag>> = tags.observeAll().map { list -> list.map { it.toModel() } }
}

/** Smart collections (§2.1). */
class SmartCollectionRepository(private val collections: SmartCollectionDao) {
    fun observeCollections(): Flow<List<SmartCollection>> =
        collections.observeAll().map { list -> list.map { it.toModel() } }
}

/** Recents (§2.1): unique per document, newest first, capped at 60. */
class RecentRepository(private val recents: RecentDao) {
    fun observeRecents(): Flow<List<RecentEntry>> =
        recents.observeAll().map { list -> list.map { RecentEntry(it.docId, it.openedAt) } }

    fun observeCount(): Flow<Int> = recents.observeCount()

    /** Entries from the last 24 h — the count shown on the Recents navigation item. */
    fun observeFreshCount(now: Long): Flow<Int> = recents.observeCountSince(now - DAY_MILLIS)

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
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

private fun Folder.toEntity() = app.leaf.reader.core.data.db.FolderEntity(
    id, name, colorHex, iconKey, parentId, isSystem
)

private fun app.leaf.reader.core.data.db.FolderEntity.toModel() =
    Folder(id, name, colorHex, iconKey, parentId, isSystem)

private fun app.leaf.reader.core.data.db.TagEntity.toModel() = Tag(name, colorHex)

private fun SmartCollectionEntity.toModel(): SmartCollection {
    val rule = when (ruleKind) {
        "type" -> ruleType?.let { SmartRule.OfType(it) } ?: SmartRule.Unfiled
        "ageDays" -> SmartRule.AgeDays(ruleDays ?: 7)
        "inProgress" -> SmartRule.InProgress
        "unfiled" -> SmartRule.Unfiled
        "hasHighlights" -> SmartRule.HasHighlights
        "tag" -> ruleTag?.let { SmartRule.WithTag(it) } ?: SmartRule.Unfiled
        else -> SmartRule.Unfiled
    }
    return SmartCollection(id, name, rule)
}
