package app.leaf.reader

import app.leaf.reader.core.domain.LibraryDoc
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.Orientation
import app.leaf.reader.core.model.SmartCollection
import app.leaf.reader.core.model.SmartRule
import app.leaf.reader.core.model.Tag

/** A fixed "now" so every relative assertion is deterministic. */
const val FIXED_NOW = 1_700_000_000_000L
internal const val DAY_MS = 24 * 60 * 60 * 1000L

internal fun doc(
    id: String,
    type: DocType = DocType.PDF,
    name: String = "$id.pdf",
    sizeMb: Double = 1.0,
    pageCount: Int = 5,
    addedDaysAgo: Int = 0,
    openedDaysAgo: Int? = null,
    folderId: String? = null,
    tags: List<String> = emptyList(),
    favorite: Boolean = false,
    page: Int = 0,
    bookmarks: Int = 0,
    highlights: Int = 0,
    now: Long = FIXED_NOW
): LibraryDoc = LibraryDoc(
    document = Document(
        id = id,
        name = name,
        type = type,
        sizeBytes = (sizeMb * 1024.0 * 1024.0).toLong(),
        uri = "leaf-demo://document/$id",
        pageCount = pageCount,
        dateAdded = now - addedDaysAgo * DAY_MS,
        lastOpened = openedDaysAgo?.let { now - it * DAY_MS },
        favorite = favorite,
        favoritedAt = if (favorite) now else null,
        folderId = folderId,
        orientation = Orientation.AUTO,
        tags = tags
    ),
    page = page,
    bookmarkCount = bookmarks,
    highlightCount = highlights
)

internal fun folder(id: String, parentId: String? = null, name: String = id) = Folder(
    id = id, name = name, colorHex = "#2B7A5B", iconKey = "folder",
    parentId = parentId, isSystem = false
)

internal fun tag(name: String) = Tag(name = name, colorHex = "#C94F3D")

internal fun collection(id: String, rule: SmartRule, name: String = id) =
    SmartCollection(id = id, name = name, rule = rule)
