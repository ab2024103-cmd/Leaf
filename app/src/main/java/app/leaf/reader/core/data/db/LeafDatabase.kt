package app.leaf.reader.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The Leaf database. Room holds documents, folders, tags, collections, recents,
 * bookmarks, highlights, cached page text and reading positions; DataStore holds
 * settings, navigation state and open tabs (LEAF-SPEC.md §4).
 */
@Database(
    entities = [
        DocumentEntity::class,
        DocumentTagEntity::class,
        FolderEntity::class,
        TagEntity::class,
        SmartCollectionEntity::class,
        RecentEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
        ExtractedTextEntity::class,
        ProgressEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LeafDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun documentTagDao(): DocumentTagDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun smartCollectionDao(): SmartCollectionDao
    abstract fun recentDao(): RecentDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao
    abstract fun extractedTextDao(): ExtractedTextDao
    abstract fun progressDao(): ProgressDao

    companion object {
        /** Adds per-document reading theme and normalized pan offsets without discarding M1/M2 data. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE reading_progress ADD COLUMN readingTheme TEXT")
                database.execSQL("ALTER TABLE reading_progress ADD COLUMN panX REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE reading_progress ADD COLUMN panY REAL NOT NULL DEFAULT 0.0")
            }
        }

        /**
         * Removes the built-in demo library that earlier builds wrote into existing installs. The
         * schema does not change; this only deletes rows the seeder created (documents with a
         * `leaf-demo://` URI, and the sample folders, tags and smart collections). Documents the
         * user imported, and anything they created themselves, are kept.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val demoDocuments = "SELECT id FROM documents WHERE uri LIKE 'leaf-demo://%'"
                val hadDemoLibrary = database.query(
                    "SELECT COUNT(*) FROM documents WHERE uri LIKE 'leaf-demo://%'"
                ).use { cursor -> cursor.moveToFirst() && cursor.getInt(0) > 0 }
                database.execSQL("DELETE FROM document_tags WHERE docId IN ($demoDocuments)")
                database.execSQL("DELETE FROM bookmarks WHERE docId IN ($demoDocuments)")
                database.execSQL("DELETE FROM highlights WHERE docId IN ($demoDocuments)")
                database.execSQL("DELETE FROM extracted_text WHERE docId IN ($demoDocuments)")
                database.execSQL("DELETE FROM reading_progress WHERE docId IN ($demoDocuments)")
                database.execSQL("DELETE FROM recents WHERE docId IN ($demoDocuments)")
                database.execSQL("DELETE FROM documents WHERE uri LIKE 'leaf-demo://%'")

                // Sample folders, tags and collections are only removed when the sample library was present.
                if (!hadDemoLibrary) return

                // A sample folder or tag that a remaining document still uses is kept, so the user's own
                // filing is never lost.
                database.execSQL(
                    "DELETE FROM folders WHERE id IN ('f-rep','f-uni','f-person','f-work','f-all') " +
                        "AND id NOT IN (SELECT folderId FROM documents WHERE folderId IS NOT NULL)"
                )
                database.execSQL(
                    "DELETE FROM tags WHERE name IN ('important','reading','work','study','recipe','personal','travel') " +
                        "AND name NOT IN (SELECT tagName FROM document_tags)"
                )
                database.execSQL(
                    "DELETE FROM smart_collections WHERE id IN " +
                        "('s-pdf','s-week','s-open','s-unfiled','s-hl','s-sheet')"
                )
            }
        }
    }
}
