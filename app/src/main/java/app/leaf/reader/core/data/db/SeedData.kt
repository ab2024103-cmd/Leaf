package app.leaf.reader.core.data.db

import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.Highlight
import app.leaf.reader.core.model.HighlightColor
import app.leaf.reader.core.model.Orientation
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.model.SmartCollection
import app.leaf.reader.core.model.SmartRule
import app.leaf.reader.core.model.Tag

/**
 * The demo library: 10 documents (including the .xlsx and .pptx), 5 folders with one
 * nested subfolder, 7 tags and 6 smart collections — the mockup's SEED_FOLDERS,
 * SEED_TAGS, SEED_SMART and SEED_DOCS (§3, LEAF-SPEC.md §4).
 *
 * Documents are demo content: their text lives in `extracted_text`, so the reader and
 * in-file search work without a real file on disk. URIs therefore use the `leaf-demo`
 * scheme instead of a SAF `content://` URI.
 */
object SeedData {

    /** Mockup SEED_FOLDERS: one system folder, Work → Reports nested, University, Personal. */
    fun folders(): List<Folder> = listOf(
        Folder("f-all", "All documents", "#4D6357", "lib", null, isSystem = true),
        Folder("f-work", "Work", "#4A6FA5", "work", null, isSystem = false),
        Folder("f-rep", "Reports", "#3D6373", "rep", "f-work", isSystem = false),
        Folder("f-uni", "University", "#7A6BB5", "uni", null, isSystem = false),
        Folder("f-person", "Personal", "#2B7A5B", "per", null, isSystem = false)
    )

    /** Mockup SEED_TAGS. */
    fun tags(): List<Tag> = listOf(
        Tag("important", "#C94F3D"),
        Tag("reading", "#2B7A5B"),
        Tag("work", "#4A6FA5"),
        Tag("study", "#7A6BB5"),
        Tag("recipe", "#C2703B"),
        Tag("personal", "#3F8F8A"),
        Tag("travel", "#A04A6C")
    )

    /** Mockup SEED_SMART: the six auto-updating collections the prototype ships with. */
    fun smartCollections(): List<SmartCollection> = listOf(
        SmartCollection("s-pdf", "All PDFs", SmartRule.OfType(DocType.PDF)),
        SmartCollection("s-week", "Added this week", SmartRule.AgeDays(7)),
        SmartCollection("s-open", "Still reading", SmartRule.InProgress),
        SmartCollection("s-unfiled", "Not filed", SmartRule.Unfiled),
        SmartCollection("s-hl", "Has highlights", SmartRule.HasHighlights),
        SmartCollection("s-sheet", "Spreadsheets", SmartRule.OfType(DocType.XLSX))
    )

    /**
     * The mockup's sample highlights, so the highlights panel has content from the
     * first run. Offsets are resolved against the seeded page text, exactly like a
     * real highlight made in reflow mode (§7.4).
     */
    fun highlights(now: Long): List<Highlight> = listOf(
        Highlight(
            id = "h1", docId = "d1", page = 0, color = HighlightColor.YELLOW,
            text = "Simplicity is not about having less.",
            bounds = emptyList(), textRange = null, cfiRange = null,
            createdAt = now - 5 * DAY_MS
        ),
        Highlight(
            id = "h2", docId = "d1", page = 0, color = HighlightColor.GREEN,
            text = "The leaf does not hurry, yet everything is accomplished.",
            bounds = emptyList(), textRange = null, cfiRange = null,
            createdAt = now - 4 * DAY_MS
        ),
        Highlight(
            id = "h3", docId = "d1", page = 1, color = HighlightColor.BLUE,
            text = "Let the first hour belong to you and to the light coming through the window.",
            bounds = emptyList(), textRange = null, cfiRange = null,
            createdAt = now - 2 * DAY_MS
        ),
        Highlight(
            id = "h4", docId = "d5", page = 1, color = HighlightColor.PINK,
            text = "Nine of twelve participants described losing their place in long documents",
            bounds = emptyList(), textRange = null, cfiRange = null,
            createdAt = now - DAY_MS
        )
    )

    internal fun documents(now: Long) = seedDocuments(now)

    /** Demo documents never point at a real file; the reader reads their cached text. */
    fun uriFor(id: String) = "leaf-demo://document/$id"
}

/**
 * Writes the demo library into the database. Idempotent: [seedIfEmpty] is safe to call
 * on every launch, and [seed] repopulates from scratch for "Reset demo library" (§6.5).
 */
class DatabaseSeeder(private val db: LeafDatabase) {

    suspend fun seedIfEmpty(now: Long = System.currentTimeMillis()): Boolean {
        if (db.documentDao().count() > 0) return false
        seed(now)
        return true
    }

    suspend fun seed(now: Long = System.currentTimeMillis()) {
        val documents = SeedData.documents(now)

        db.folderDao().insertAll(SeedData.folders().map { it.toEntity() })
        db.tagDao().insertAll(SeedData.tags().map { it.toEntity() })
        db.smartCollectionDao().insertAll(SeedData.smartCollections().map { it.toEntity() })

        db.documentDao().insertAll(documents.map { it.toEntity() })
        db.documentTagDao().insertAll(documents.flatMap { doc -> doc.tags.map { tag -> DocumentTagEntity(doc.id, tag) } })

        db.extractedTextDao().insertAll(documents.flatMap { doc ->
            doc.pages.mapIndexed { index, paragraphs ->
                ExtractedTextEntity(doc.id, index, paragraphs.joinToString("\n"))
            }
        })

        db.progressDao().replaceAll(documents.map { doc ->
            ProgressEntity(
                docId = doc.id,
                page = doc.page,
                scrollFraction = 0f,
                zoom = 1f,
                scrollDir = ScrollDir.VERTICAL,
                updatedAt = doc.lastOpened
            )
        })

        db.bookmarkDao().insertAll(documents.flatMap { doc ->
            doc.bookmarks.mapIndexed { index, bookmark ->
                BookmarkEntity(
                    id = "${doc.id}-b$index",
                    docId = doc.id,
                    page = bookmark.page,
                    label = bookmark.label,
                    createdAt = bookmark.createdAt
                )
            }
        })

        val pageText = documents.associate { doc ->
            doc.id to doc.pages.map { it.joinToString("\n") }
        }
        db.highlightDao().insertAll(SeedData.highlights(now).map { highlight ->
            val text = pageText[highlight.docId]?.getOrNull(highlight.page)
            val start = text?.indexOf(highlight.text)?.takeIf { it >= 0 }
            highlight.toEntity(
                rangeStart = start,
                rangeEnd = start?.let { it + highlight.text.length }
            )
        })

        // Recents mirror the documents' last-opened times, newest first (§2.1).
        db.recentDao().clear()
        documents.forEach { doc -> db.recentDao().insert(RecentEntity(doc.id, doc.lastOpened)) }
        db.recentDao().keepNewest(60)
    }
}

private fun Folder.toEntity() = FolderEntity(id, name, colorHex, iconKey, parentId, isSystem)

private fun Tag.toEntity() = TagEntity(name, colorHex)

private fun SmartCollection.toEntity() = SmartCollectionEntity(
    id = id,
    name = name,
    ruleKind = when (rule) {
        is SmartRule.OfType -> "type"
        is SmartRule.AgeDays -> "ageDays"
        SmartRule.InProgress -> "inProgress"
        SmartRule.Unfiled -> "unfiled"
        SmartRule.HasHighlights -> "hasHighlights"
        is SmartRule.WithTag -> "tag"
    },
    ruleType = (rule as? SmartRule.OfType)?.type,
    ruleDays = (rule as? SmartRule.AgeDays)?.days,
    ruleTag = (rule as? SmartRule.WithTag)?.tag
)

private fun SeedDocument.toEntity() = DocumentEntity(
    id = id,
    name = name,
    uri = SeedData.uriFor(id),
    type = type,
    sizeBytes = (sizeMb * 1024.0 * 1024.0).toLong(),
    pageCount = pages.size,
    dateAdded = dateAdded,
    lastOpened = lastOpened,
    favorite = favorite,
    favoritedAt = favoritedAt,
    folderId = folderId,
    orientation = Orientation.AUTO
)

private fun Highlight.toEntity(rangeStart: Int?, rangeEnd: Int?) = HighlightEntity(
    id = id,
    docId = docId,
    page = page,
    color = color,
    text = text,
    bounds = "",
    rangeStart = rangeStart,
    rangeEnd = rangeEnd,
    cfiRange = cfiRange,
    createdAt = createdAt
)
