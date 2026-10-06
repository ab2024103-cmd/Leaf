package app.leaf.reader.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.HighlightColor
import app.leaf.reader.core.model.Orientation
import app.leaf.reader.core.model.ScrollDir

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val uri: String,
    val type: DocType,
    val sizeBytes: Long,
    val pageCount: Int,
    val dateAdded: Long,
    val lastOpened: Long?,
    val favorite: Boolean,
    val favoritedAt: Long?,
    val folderId: String?,
    val orientation: Orientation
)

/**
 * Document → tag join (LEAF-SPEC.md §4: several tags per document, AND filtering).
 * Deleting a document removes its rows; deleting a tag never touches documents (§2.1).
 */
@Entity(
    tableName = "document_tags",
    primaryKeys = ["docId", "tagName"],
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["docId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("docId"), Index("tagName")]
)
data class DocumentTagEntity(
    val docId: String,
    val tagName: String
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val iconKey: String,
    val parentId: String?,
    val isSystem: Boolean
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val name: String,
    val colorHex: String
)

/**
 * Smart collections store their rule flattened (§2.1). [ruleKind] is one of
 * type · ageDays · inProgress · unfiled · hasHighlights · tag.
 */
@Entity(tableName = "smart_collections")
data class SmartCollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ruleKind: String,
    val ruleType: DocType?,
    val ruleDays: Int?,
    val ruleTag: String?
)

/** Recents: one row per document, unique, capped at 60 (§2.1). */
@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val docId: String,
    val openedAt: Long
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["docId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("docId")]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val docId: String,
    val page: Int,
    val label: String,
    val createdAt: Long
)

/**
 * Highlights (§7.4). [bounds] holds PDF-style 0..1 page rects, [rangeStart] /
 * [rangeEnd] hold reflow character offsets and [cfiRange] holds the EPUB range —
 * a highlight uses whichever representation its format supports.
 */
@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["docId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("docId")]
)
data class HighlightEntity(
    @PrimaryKey val id: String,
    val docId: String,
    val page: Int,
    val color: HighlightColor,
    val text: String,
    /** Serialised [app.leaf.reader.core.model.NormalizedRect] list; see Converters. */
    val bounds: String,
    val rangeStart: Int?,
    val rangeEnd: Int?,
    val cfiRange: String?,
    val createdAt: Long
)

/**
 * Extracted page text, cached per document so in-file search and reflow do the work
 * once per document instead of once per open (§7.2).
 */
@Entity(tableName = "extracted_text", primaryKeys = ["docId", "pageIndex"])
data class ExtractedTextEntity(
    val docId: String,
    val pageIndex: Int,
    /** Paragraphs of the page, joined with '\n'. */
    val text: String
)

/** Reading position — Page Stay (§2.2). */
@Entity(tableName = "reading_progress")
data class ProgressEntity(
    @PrimaryKey val docId: String,
    val page: Int,
    val scrollFraction: Float,
    val zoom: Float,
    val scrollDir: ScrollDir,
    val updatedAt: Long
)
