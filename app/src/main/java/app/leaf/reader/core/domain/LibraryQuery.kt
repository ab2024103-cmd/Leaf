package app.leaf.reader.core.domain

import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.SmartCollection
import app.leaf.reader.core.model.SmartRule
import app.leaf.reader.core.model.SortField

/**
 * The library's filter state. Every dimension composes (§8.5); a `null` dimension is off.
 *
 * @param folderId The selected folder; `null` means "All documents" (the mockup's system
 *   folder). Subfolders are included, so a parent shows everything filed beneath it.
 * @param smartId The selected smart collection.
 * @param tags Selected tags with **AND** semantics (§2.1).
 * @param type The open file-type group. This is a *view*, not a filter (§8.13), so chip
 *   counts ignore it and it only narrows the row list.
 */
data class LibraryFilter(
    val folderId: String? = null,
    val smartId: String? = null,
    val tags: Set<String> = emptySet(),
    val type: app.leaf.reader.core.model.DocType? = null
)

/**
 * Everything one render of the Library needs, computed in one pass from the raw data.
 *
 * @param visible Every document passing the folder × collection × tag filters, sorted.
 * @param rows [visible] narrowed to the open file-type group, or [visible] itself.
 * @param groups The overview cards, one per file type present in [visible].
 */
data class LibraryView(
    val visible: List<LibraryDoc> = emptyList(),
    val rows: List<LibraryDoc> = emptyList(),
    val groups: List<TypeGroupCard> = emptyList(),
    val folderTotals: Map<String?, Int> = emptyMap(),
    val smartTotals: Map<String, Int> = emptyMap(),
    val openType: app.leaf.reader.core.model.DocType? = null,
    val showGroups: Boolean = false,
    val typeCount: Int = 0
)

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/** The folder itself plus every folder nested beneath it, at any depth. */
fun subtreeIds(folders: List<Folder>, folderId: String?): Set<String> {
    if (folderId == null) return emptySet()
    val children = folders.filter { it.parentId == folderId }.map { it.id }.toSet()
    return setOf(folderId) + children + children.flatMap { subtreeIds(folders, it) }
}

fun LibraryDoc.matchesFolder(folderId: String?, folders: List<Folder>): Boolean {
    if (folderId == null) return true
    return document.folderId in subtreeIds(folders, folderId)
}

fun LibraryDoc.matchesTags(tags: Set<String>): Boolean =
    tags.isEmpty() || tags.all { it in document.tags }

/** Smart-collection rules (§2.1, LEAF-SPEC §2.1). */
fun SmartRule.matches(doc: LibraryDoc, now: Long): Boolean = when (this) {
    SmartRule.All -> true
    is SmartRule.OfType -> doc.document.type == type
    is SmartRule.AgeDays -> now - doc.document.dateAdded <= days * DAY_MILLIS
    SmartRule.InProgress -> doc.startedPct > 0 && doc.pctRead < 100
    SmartRule.Unfiled -> doc.document.folderId == null
    SmartRule.HasHighlights -> doc.highlightCount > 0
    is SmartRule.WithTag -> tag in doc.document.tags
}

fun List<LibraryDoc>.applyFilter(
    filter: LibraryFilter,
    folders: List<Folder>,
    smart: List<SmartCollection>,
    now: Long
): List<LibraryDoc> {
    val rule = filter.smartId?.let { id -> smart.firstOrNull { it.id == id }?.rule }
    return filter { doc ->
        doc.matchesFolder(filter.folderId, folders) &&
            doc.matchesTags(filter.tags) &&
            (rule == null || rule.matches(doc, now)) &&
            (filter.type == null || doc.document.type == filter.type)
    }
}

/** One card per file type present, in the fixed §6.1 order. */
fun groupCards(docs: List<LibraryDoc>): List<TypeGroupCard> =
    docs.groupBy { it.document.type }
        .map { (type, group) ->
            TypeGroupCard(
                type = type,
                count = group.size,
                sizeBytes = group.sumOf { it.document.sizeBytes },
                docsWithHighlights = group.count { it.highlightCount > 0 },
                docsWithBookmarks = group.count { it.bookmarkCount > 0 }
            )
        }
        .sortedBy { it.type.groupOrder }

/**
 * Builds one render of the Library. Pure: the same inputs always give the same view, so
 * it is unit-testable without a database (§12).
 *
 * Counts follow §8.5 — "the counts shown on chips always reflect the current filter
 * context" — by dropping the rail's own dimension: a folder chip answers "how many
 * documents would this folder show, given my collection and tag filters".
 */
fun composeLibrary(
    docs: List<LibraryDoc>,
    folders: List<Folder>,
    smart: List<SmartCollection>,
    filter: LibraryFilter,
    sortField: SortField,
    sortAscending: Boolean,
    groupByType: Boolean,
    now: Long
): LibraryView {
    // The open group is a view: it narrows the rows but never the chip counts.
    val effective = filter.copy(type = null)
    val baseForFolders = docs.applyFilter(effective.copy(folderId = null), folders, smart, now)
    val baseForSmart = docs.applyFilter(effective.copy(smartId = null), folders, smart, now)

    val filtered = docs.applyFilter(effective, folders, smart, now)

    // §8.13: when filters empty the open group's type, fall back to the overview.
    val wanted = if (groupByType) filter.type else null
    val openType = wanted?.takeIf { type -> filtered.any { it.document.type == type } }
    val showGroups = groupByType && openType == null

    val folderTotals = buildMap<String?, Int> {
        put(null, baseForFolders.size)
        folders.forEach { folder ->
            if (folder.isSystem) return@forEach
            put(folder.id, baseForFolders.count { it.matchesFolder(folder.id, folders) })
        }
    }
    val smartTotals = smart.associate { collection ->
        collection.id to baseForSmart.count { collection.rule.matches(it, now) }
    }

    val visible = LibrarySort.sorted(filtered, sortField, sortAscending)
    val rows = if (openType == null) visible else visible.filter { it.document.type == openType }

    return LibraryView(
        visible = visible,
        rows = rows,
        groups = if (showGroups) groupCards(visible) else emptyList(),
        folderTotals = folderTotals,
        smartTotals = smartTotals,
        openType = openType,
        showGroups = showGroups,
        typeCount = visible.map { it.document.type }.distinct().size
    )
}
