package app.leaf.reader.core.data.repo

import app.leaf.reader.core.data.db.BookmarkDao
import app.leaf.reader.core.data.db.BookmarkEntity
import app.leaf.reader.core.data.db.DocumentDao
import app.leaf.reader.core.data.db.DocumentEntity
import app.leaf.reader.core.data.db.DocumentSnapshot
import app.leaf.reader.core.data.db.DocumentTagDao
import app.leaf.reader.core.data.db.DocumentTagEntity
import app.leaf.reader.core.data.db.FolderDao
import app.leaf.reader.core.data.db.FolderEntity
import app.leaf.reader.core.data.db.HighlightDao
import app.leaf.reader.core.data.db.ProgressDao
import app.leaf.reader.core.data.db.RecentDao
import app.leaf.reader.core.data.db.SmartCollectionDao
import app.leaf.reader.core.data.db.SmartCollectionEntity
import app.leaf.reader.core.data.db.TagDao
import app.leaf.reader.core.data.db.TagEntity
import app.leaf.reader.core.data.prefs.ReaderTabsStore
import app.leaf.reader.core.domain.LibraryDoc
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.RecentEntry
import app.leaf.reader.core.model.SmartCollection
import app.leaf.reader.core.model.SmartRule
import app.leaf.reader.core.model.Tag
import app.leaf.reader.core.util.normalizeTagName
import app.leaf.reader.core.util.swatchFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Documents and their tags (LEAF-SPEC.md §4, §3). */
class DocumentRepository(
    private val documents: DocumentDao,
    private val documentTags: DocumentTagDao,
    private val bookmarks: BookmarkDao,
    private val highlights: HighlightDao,
    private val progress: ProgressDao,
    private val recents: RecentDao,
    private val tabs: ReaderTabsStore? = null
) {

    /** Every document with its tags, newest added first. */
    fun observeDocuments(): Flow<List<Document>> = combine(
        documents.observeAll(),
        documentTags.observeAll()
    ) { docs, tags ->
        val byDoc = tags.groupBy({ it.docId }, { it.tagName })
        docs.map { it.toModel(byDoc[it.id].orEmpty().sorted()) }
    }

    /**
     * The library in one flow: each document with its reading page, bookmark count and
     * highlight count, so a row can be drawn without further queries (§12 performance).
     */
    fun observeLibrary(): Flow<List<LibraryDoc>> = combine(
        documents.observeAll(),
        documentTags.observeAll(),
        bookmarks.observeCounts(),
        highlights.observeCounts(),
        progress.observeAll()
    ) { docs, tags, bookmarkCounts, highlightCounts, positions ->
        val tagsByDoc = tags.groupBy({ it.docId }, { it.tagName })
        val bookmarkByDoc = bookmarkCounts.associate { it.docId to it.total }
        val highlightByDoc = highlightCounts.associate { it.docId to it.total }
        val pageByDoc = positions.associate { it.docId to it.page }
        docs.map { doc ->
            LibraryDoc(
                document = doc.toModel(tagsByDoc[doc.id].orEmpty().sorted()),
                page = pageByDoc[doc.id] ?: 0,
                bookmarkCount = bookmarkByDoc[doc.id] ?: 0,
                highlightCount = highlightByDoc[doc.id] ?: 0
            )
        }
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

    suspend fun setFavorite(id: String, favorite: Boolean) {
        documents.setFavorite(id, favorite, if (favorite) System.currentTimeMillis() else null)
    }

    suspend fun rename(id: String, name: String) = documents.rename(id, name)

    suspend fun moveToFolder(id: String, folderId: String?) = documents.moveToFolder(id, folderId)

    suspend fun replaceTags(id: String, tags: List<String>) {
        documentTags.deleteForDocument(id)
        documentTags.insertAll(tags.map { DocumentTagEntity(id, it) })
    }

    /**
     * Everything [delete] is about to cascade away, so an Undo can put it back (§8.7).
     * The reading position is kept too, so a restored document resumes where it was.
     */
    suspend fun snapshot(id: String): DocumentSnapshot? {
        val document = documents.get(id) ?: return null
        return DocumentSnapshot(
            document = document,
            tags = documentTags.getForDocument(id),
            bookmarks = bookmarks.getForDocument(id),
            highlights = highlights.getForDocument(id),
            recent = recents.get(id),
            progress = progress.get(id)
        )
    }

    /** Removes the document from the library, the recents list and every tab (§8.7). */
    suspend fun delete(id: String) {
        tabs?.removeDocument(id)
        recents.delete(id)
        documents.delete(id)
    }

    suspend fun restore(snapshot: DocumentSnapshot) {
        documents.insert(snapshot.document)
        if (snapshot.tags.isNotEmpty()) documentTags.insertAll(snapshot.tags)
        snapshot.bookmarks.forEach { bookmarks.insert(it) }
        snapshot.highlights.forEach { highlights.insert(it) }
        snapshot.progress?.let { progress.insert(it) }
        snapshot.recent?.let { recents.insert(it) }
    }
}

/** Folders, including the system "All documents" entry and nested subfolders. */
class FolderRepository(
    private val folders: FolderDao,
    private val documents: DocumentDao
) {
    fun observeFolders(): Flow<List<Folder>> = folders.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun all(): List<Folder> = folders.getAll().map { it.toModel() }

    suspend fun get(id: String): Folder? = folders.get(id)?.toModel()

    /** New folders take the next palette swatch, as the mockup does. */
    suspend fun create(name: String, parentId: String?): Folder {
        val folder = FolderEntity(
            id = "f-" + System.currentTimeMillis().toString(36) + "-" + name.hashCode().toString(36),
            name = name,
            colorHex = swatchFor(all().size),
            iconKey = "folder",
            parentId = parentId,
            isSystem = false
        )
        folders.insert(folder)
        return folder.toModel()
    }

    suspend fun rename(id: String, name: String) = folders.rename(id, name)

    suspend fun recolour(id: String, colorHex: String) = folders.recolour(id, colorHex)

    suspend fun documentCount(id: String): Int = folders.documentCount(id)

    suspend fun childCount(id: String): Int = folders.childCount(id)

    /**
     * Deleting a folder never deletes a document (§2.1): documents move to the parent
     * folder, or to "Not filed" when there is none. Subfolders move up one level.
     */
    suspend fun delete(id: String) {
        val parent = folders.get(id)?.parentId
        documents.reparent(id, parent)
        folders.reparentChildren(id, parent)
        folders.delete(id)
    }
}

/** Tags (§2.1). */
class TagRepository(
    private val tags: TagDao,
    private val documentTags: DocumentTagDao
) {
    fun observeTags(): Flow<List<Tag>> = tags.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun all(): List<Tag> = tags.getAll().map { it.toModel() }

    /** @return false when a tag with that name already exists. */
    suspend fun create(rawName: String): Boolean {
        val name = normalizeTagName(rawName)
        if (name.isEmpty() || all().any { it.name == name }) return false
        tags.insert(TagEntity(name, swatchFor(all().size)))
        return true
    }

    /** Renaming cascades to every document that carries the tag (§2.1). */
    suspend fun rename(oldName: String, rawNewName: String): Boolean {
        val newName = normalizeTagName(rawNewName)
        if (newName.isEmpty()) return false
        if (newName != oldName && all().any { it.name == newName }) return false
        val colour = all().firstOrNull { it.name == oldName }?.colorHex ?: swatchFor(0)
        tags.insert(TagEntity(newName, colour))
        documentTags.renameTag(oldName, newName)
        tags.delete(oldName)
        return true
    }

    suspend fun recolour(name: String, colorHex: String) = tags.recolour(name, colorHex)

    /** Deletes the tag and removes it from every document; documents are untouched. */
    suspend fun delete(name: String) {
        documentTags.deleteForTag(name)
        tags.delete(name)
    }
}

/** Smart collections (§2.1). */
class SmartCollectionRepository(private val collections: SmartCollectionDao) {
    fun observeCollections(): Flow<List<SmartCollection>> =
        collections.observeAll().map { list -> list.map { it.toModel() } }

    /** @return false when a collection with that name already exists. */
    suspend fun add(name: String, rule: SmartRule): Boolean {
        if (collectionsList().any { it.name == name }) return false
        collections.insert(
            SmartCollectionEntity(
                id = "s-" + System.currentTimeMillis().toString(36) + "-" + name.hashCode().toString(36),
                name = name,
                ruleKind = rule.kind,
                ruleType = (rule as? SmartRule.OfType)?.type,
                ruleDays = (rule as? SmartRule.AgeDays)?.days,
                ruleTag = (rule as? SmartRule.WithTag)?.tag
            )
        )
        return true
    }

    suspend fun delete(id: String) = collections.delete(id)

    private suspend fun collectionsList(): List<SmartCollection> = collections.getAll().map { it.toModel() }
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

private fun FolderEntity.toModel() = Folder(id, name, colorHex, iconKey, parentId, isSystem)

private fun TagEntity.toModel() = Tag(name, colorHex)

private fun SmartCollectionEntity.toModel(): SmartCollection {
    val rule = when (ruleKind) {
        "all" -> SmartRule.All
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

/** The Room column that stores a rule kind (§2.1 flat representation). */
private val SmartRule.kind: String
    get() = when (this) {
        SmartRule.All -> "all"
        is SmartRule.OfType -> "type"
        is SmartRule.AgeDays -> "ageDays"
        SmartRule.InProgress -> "inProgress"
        SmartRule.Unfiled -> "unfiled"
        SmartRule.HasHighlights -> "hasHighlights"
        is SmartRule.WithTag -> "tag"
    }
