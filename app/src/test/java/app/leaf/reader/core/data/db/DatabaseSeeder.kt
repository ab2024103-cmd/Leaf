package app.leaf.reader.core.data.db

import app.leaf.reader.core.model.ScrollDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Test-only: writes the demo fixture library into the database. Production never seeds
 * demo content; tests call this explicitly so their fixtures stay deterministic.
 */
class DatabaseSeeder(private val db: LeafDatabase) {
    private val seedMutex = Mutex()

    suspend fun seedIfEmpty(now: Long = System.currentTimeMillis()): Boolean = seedMutex.withLock {
        if (db.documentDao().count() > 0) return@withLock false
        seed(now)
        true
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
