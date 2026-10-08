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
    version = 2,
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
    }
}
