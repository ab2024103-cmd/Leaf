package app.leaf.reader.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<DocumentEntity>)

    @Query("SELECT * FROM documents ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY dateAdded DESC")
    suspend fun getAll(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observe(id: String): Flow<DocumentEntity?>

    @Query("SELECT COUNT(*) FROM documents")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun get(id: String): DocumentEntity?

    @Query("UPDATE documents SET favorite = :favorite, favoritedAt = :favoritedAt WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, favoritedAt: Long?)

    @Query("UPDATE documents SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE documents SET folderId = :folderId WHERE id = :id")
    suspend fun moveToFolder(id: String, folderId: String?)

    @Query("UPDATE documents SET lastOpened = :openedAt WHERE id = :id")
    suspend fun updateLastOpened(id: String, openedAt: Long)

    @Query("UPDATE documents SET pageCount = :pageCount WHERE id = :id")
    suspend fun updatePageCount(id: String, pageCount: Int)

    /** Deleting a folder moves its documents to the parent, never to the bin (§2.1). */
    @Query("UPDATE documents SET folderId = :folderId WHERE folderId = :id")
    suspend fun reparent(id: String, folderId: String?)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface DocumentTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<DocumentTagEntity>)

    @Query("SELECT * FROM document_tags")
    fun observeAll(): Flow<List<DocumentTagEntity>>

    @Query("SELECT * FROM document_tags")
    suspend fun getAll(): List<DocumentTagEntity>

    @Query("DELETE FROM document_tags WHERE docId = :docId")
    suspend fun deleteForDocument(docId: String)

    @Query("SELECT * FROM document_tags WHERE docId = :docId")
    suspend fun getForDocument(docId: String): List<DocumentTagEntity>

    @Query("DELETE FROM document_tags WHERE tagName = :tagName")
    suspend fun deleteForTag(tagName: String)

    /** Renaming a tag cascades to every document that carries it (§2.1). */
    @Query("UPDATE document_tags SET tagName = :newName WHERE tagName = :oldName")
    suspend fun renameTag(oldName: String, newName: String)
}

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<FolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity)

    @Query("SELECT * FROM folders ORDER BY isSystem DESC, name ASC")
    suspend fun getAll(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun get(id: String): FolderEntity?

    @Query("SELECT * FROM folders ORDER BY isSystem DESC, name ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT COUNT(*) FROM folders")
    suspend fun count(): Int

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE folders SET colorHex = :colorHex WHERE id = :id")
    suspend fun recolour(id: String, colorHex: String)

    @Query("SELECT COUNT(*) FROM documents WHERE folderId = :id")
    suspend fun documentCount(id: String): Int

    @Query("SELECT COUNT(*) FROM folders WHERE parentId = :id")
    suspend fun childCount(id: String): Int

    /** Deleting a folder re-parents its children one level up (§2.1). */
    @Query("UPDATE folders SET parentId = :newParentId WHERE parentId = :id")
    suspend fun reparentChildren(id: String, newParentId: String?)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: TagEntity)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun count(): Int

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAll(): List<TagEntity>

    @Query("UPDATE tags SET name = :name WHERE name = :oldName")
    suspend fun rename(oldName: String, name: String)

    @Query("UPDATE tags SET colorHex = :colorHex WHERE name = :name")
    suspend fun recolour(name: String, colorHex: String)

    @Query("DELETE FROM tags WHERE name = :name")
    suspend fun delete(name: String)
}

@Dao
interface SmartCollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collections: List<SmartCollectionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(collection: SmartCollectionEntity)

    @Query("SELECT * FROM smart_collections ORDER BY name ASC")
    fun observeAll(): Flow<List<SmartCollectionEntity>>

    @Query("SELECT COUNT(*) FROM smart_collections")
    suspend fun count(): Int

    @Query("SELECT * FROM smart_collections ORDER BY name ASC")
    suspend fun getAll(): List<SmartCollectionEntity>

    @Query("DELETE FROM smart_collections WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RecentDao {

    /** Recents are unique per document: re-opening replaces the timestamp (§2.1). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: RecentEntity)

    @Query("SELECT * FROM recents ORDER BY openedAt DESC")
    fun observeAll(): Flow<List<RecentEntity>>

    @Query("SELECT COUNT(*) FROM recents")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recents WHERE openedAt >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RecentEntity>)

    @Query("SELECT * FROM recents WHERE docId = :docId")
    suspend fun get(docId: String): RecentEntity?

    @Query("DELETE FROM recents WHERE docId = :docId")
    suspend fun delete(docId: String)

    @Query("DELETE FROM recents")
    suspend fun clear()

    /** Keeps only the newest [limit] entries (§2.1: capped at 60). */
    @Query(
        "DELETE FROM recents WHERE docId NOT IN " +
            "(SELECT docId FROM recents ORDER BY openedAt DESC LIMIT :limit)"
    )
    suspend fun keepNewest(limit: Int)
}

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookmarks: List<BookmarkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks WHERE docId = :docId ORDER BY page ASC")
    suspend fun getForDocument(docId: String): List<BookmarkEntity>

    /** One row per document that has bookmarks — the library needs only the count. */
    @Query("SELECT docId, COUNT(*) AS total FROM bookmarks GROUP BY docId")
    fun observeCounts(): Flow<List<DocCount>>

    @Query("SELECT * FROM bookmarks WHERE docId = :docId ORDER BY page ASC")
    fun observeForDocument(docId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE docId = :docId")
    fun observeCountForDocument(docId: String): Flow<Int>

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface HighlightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(highlights: List<HighlightEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highlight: HighlightEntity)

    @Query("SELECT * FROM highlights WHERE docId = :docId ORDER BY page ASC, createdAt ASC")
    suspend fun getForDocument(docId: String): List<HighlightEntity>

    /** One row per document that has highlights — the library needs only the count. */
    @Query("SELECT docId, COUNT(*) AS total FROM highlights GROUP BY docId")
    fun observeCounts(): Flow<List<DocCount>>

    @Query("SELECT * FROM highlights WHERE docId = :docId ORDER BY page ASC, createdAt ASC")
    fun observeForDocument(docId: String): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HighlightEntity>>

    @Query("SELECT COUNT(*) FROM highlights")
    suspend fun count(): Int

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM highlights WHERE docId = :docId")
    suspend fun clearForDocument(docId: String)

    @Query("DELETE FROM highlights")
    suspend fun clearAll()
}

@Dao
interface ExtractedTextDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<ExtractedTextEntity>)

    @Query("SELECT * FROM extracted_text WHERE docId = :docId ORDER BY pageIndex ASC")
    suspend fun pagesForDocument(docId: String): List<ExtractedTextEntity>

    @Query("SELECT * FROM extracted_text ORDER BY docId ASC, pageIndex ASC")
    suspend fun pagesForLibrary(): List<ExtractedTextEntity>

    @Query("SELECT * FROM extracted_text WHERE docId = :docId AND pageIndex = :pageIndex LIMIT 1")
    suspend fun page(docId: String, pageIndex: Int): ExtractedTextEntity?

    @Query("SELECT COUNT(*) FROM extracted_text")
    suspend fun count(): Int

    @Query("DELETE FROM extracted_text WHERE docId = :docId")
    suspend fun clearForDocument(docId: String)
}

@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: ProgressEntity)

    @Query("SELECT * FROM reading_progress WHERE docId = :docId")
    fun observe(docId: String): Flow<ProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE docId = :docId")
    suspend fun get(docId: String): ProgressEntity?

    @Query("SELECT * FROM reading_progress")
    suspend fun getAll(): List<ProgressEntity>

    /** Every position at once, so the library rows can show progress in one pass. */
    @Query("SELECT * FROM reading_progress")
    fun observeAll(): Flow<List<ProgressEntity>>

    @Query("DELETE FROM reading_progress WHERE docId = :docId")
    suspend fun delete(docId: String)

    @Query("DELETE FROM reading_progress")
    suspend fun clear()

    /** Replaces the row set in one transaction, used when seeding (§8.3). */
    @Transaction
    suspend fun replaceAll(entries: List<ProgressEntity>) {
        clear()
        entries.forEach { insert(it) }
    }
}
