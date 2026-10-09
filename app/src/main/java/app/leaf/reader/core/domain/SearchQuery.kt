package app.leaf.reader.core.domain

import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.SortField
import java.util.Locale

/** The five scopes defined for Search (§6.4). */
enum class SearchScope {
    EVERYTHING,
    NAME,
    TAG,
    FOLDER,
    INSIDE_FILES
}

/** Added-within filter values from the Search filter panel (§6.4). */
enum class SearchAddedWithin(val days: Int?) {
    ANY(null),
    WEEK(7),
    MONTH(30),
    LAST_THREE_MONTHS(90)
}

/** Independent filter dimensions; every selected dimension composes with AND. */
data class SearchFilters(
    val type: DocType? = null,
    val folderId: String? = null,
    val tag: String? = null,
    val addedWithin: SearchAddedWithin = SearchAddedWithin.ANY
) {
    val activeCount: Int
        get() = listOf(type != null, folderId != null, tag != null, addedWithin != SearchAddedWithin.ANY).count { it }
}

/** A document and its cached page text. Only cached PDF text is searched in M4. */
data class SearchableDocument(
    val document: Document,
    val pages: List<String> = emptyList()
)

/** A bounded excerpt whose match range is emphasized by the Search UI. */
data class SearchExcerpt(
    val text: String,
    val matchStart: Int,
    val matchEnd: Int
)

data class SearchFileResult(
    val document: Document,
    val matchCount: Int,
    /** Zero-based page index. */
    val firstPageIndex: Int,
    val excerpt: SearchExcerpt
)

data class SearchResults(
    val query: String = "",
    val scope: SearchScope = SearchScope.EVERYTHING,
    val filters: SearchFilters = SearchFilters(),
    val documents: List<Document> = emptyList(),
    val files: List<SearchFileResult> = emptyList(),
    /** Total literal query occurrences across all matching files. */
    val totalFileHits: Int = 0
) {
    val fileCount: Int get() = files.size
}

/** Pure, deterministic search rules; [now] is injected so age filters are testable. */
fun searchLibrary(
    searchable: List<SearchableDocument>,
    folders: List<Folder>,
    query: String,
    scope: SearchScope,
    filters: SearchFilters,
    now: Long,
    sortField: SortField = SortField.NAME,
    sortAscending: Boolean = true
): SearchResults {
    val normalizedQuery = query.trim()
    val folderById = folders.associateBy(Folder::id)
    val matchingDocs = ArrayList<Document>()
    val fileResults = ArrayList<SearchFileResult>()

    searchable.forEach { searchableDocument ->
        val document = searchableDocument.document
        if (!matchesFilters(document, folderById, filters, now)) return@forEach

        val folderNames = folderNamesFor(document.folderId, folderById)
        val hasMetadataMatch = if (normalizedQuery.isEmpty()) {
            scope != SearchScope.INSIDE_FILES
        } else {
            when (scope) {
                SearchScope.EVERYTHING ->
                    document.name.contains(normalizedQuery, ignoreCase = true) ||
                        document.tags.any { it.contains(normalizedQuery, ignoreCase = true) } ||
                        folderNames.any { it.contains(normalizedQuery, ignoreCase = true) }
                SearchScope.NAME -> document.name.contains(normalizedQuery, ignoreCase = true)
                SearchScope.TAG -> document.tags.any { it.contains(normalizedQuery, ignoreCase = true) }
                SearchScope.FOLDER -> folderNames.any { it.contains(normalizedQuery, ignoreCase = true) }
                SearchScope.INSIDE_FILES -> false
            }
        }
        if (hasMetadataMatch) matchingDocs += document

        if (normalizedQuery.isNotEmpty() && scope in setOf(SearchScope.EVERYTHING, SearchScope.INSIDE_FILES)) {
            val fileMatch = findFileMatches(searchableDocument, normalizedQuery)
            if (fileMatch != null) fileResults += fileMatch
        }
    }

    val sortedDocuments = sortDocuments(matchingDocs, sortField, sortAscending)
    val sortedFiles = sortDocuments(fileResults.map(SearchFileResult::document), sortField, sortAscending)
        .mapNotNull { doc -> fileResults.firstOrNull { it.document.id == doc.id } }

    return SearchResults(
        query = normalizedQuery,
        scope = scope,
        filters = filters,
        documents = sortedDocuments,
        files = sortedFiles,
        totalFileHits = fileResults.sumOf(SearchFileResult::matchCount)
    )
}

/** Case-insensitive, literal, non-overlapping occurrence count used by Search and Find. */
fun countOccurrences(text: String, query: String): Int {
    val needle = query.trim()
    if (needle.isEmpty()) return 0
    var count = 0
    var start = 0
    while (start <= text.length - needle.length) {
        val found = text.indexOf(needle, start, ignoreCase = true)
        if (found < 0) break
        count++
        start = found + needle.length
    }
    return count
}

/** Most-recent-first history, deduped case-insensitively and capped at ten (§2.3). */
fun pushSearchHistory(history: List<String>, rawQuery: String): List<String> {
    val query = rawQuery.trim()
    if (query.isEmpty()) return normalizeSearchHistory(history)
    return (listOf(query) + history.filterNot { it.equals(query, ignoreCase = true) })
        .let(::normalizeSearchHistory)
}

fun removeSearchHistoryItem(history: List<String>, query: String): List<String> =
    normalizeSearchHistory(history.filterNot { it.equals(query, ignoreCase = true) })

fun normalizeSearchHistory(history: List<String>): List<String> {
    val seen = HashSet<String>()
    return history.asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .filter { seen.add(it.lowercase(Locale.ROOT)) }
        .take(10)
        .toList()
}

private fun findFileMatches(
    searchable: SearchableDocument,
    query: String
): SearchFileResult? {
    var total = 0
    var firstPage = -1
    var firstExcerpt: SearchExcerpt? = null
    searchable.pages.forEachIndexed { pageIndex, pageText ->
        var start = 0
        while (start <= pageText.length - query.length) {
            val matchStart = pageText.indexOf(query, start, ignoreCase = true)
            if (matchStart < 0) break
            total++
            if (firstPage < 0) {
                firstPage = pageIndex
                firstExcerpt = makeExcerpt(pageText, matchStart, query.length)
            }
            start = matchStart + query.length
        }
    }
    if (total == 0) return null
    return SearchFileResult(
        document = searchable.document,
        matchCount = total,
        firstPageIndex = firstPage,
        excerpt = checkNotNull(firstExcerpt)
    )
}

private fun makeExcerpt(text: String, matchStart: Int, matchLength: Int): SearchExcerpt {
    val before = 46
    val after = 74
    val start = (matchStart - before).coerceAtLeast(0)
    val end = (matchStart + matchLength + after).coerceAtMost(text.length)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < text.length) "…" else ""
    val excerpt = prefix + text.substring(start, end) + suffix
    val emphasizedStart = prefix.length + matchStart - start
    return SearchExcerpt(
        text = excerpt,
        matchStart = emphasizedStart,
        matchEnd = emphasizedStart + matchLength
    )
}

private fun matchesFilters(
    document: Document,
    folderById: Map<String, Folder>,
    filters: SearchFilters,
    now: Long
): Boolean {
    if (filters.type != null && document.type != filters.type) return false
    if (filters.tag != null && filters.tag !in document.tags) return false
    if (filters.folderId != null && filters.folderId !in folderChainIds(document.folderId, folderById)) return false
    val ageDays = filters.addedWithin.days
    if (ageDays != null && now - document.dateAdded > ageDays * DAY_MILLIS) return false
    return true
}

private fun folderNamesFor(folderId: String?, folderById: Map<String, Folder>): List<String> =
    folderChainIds(folderId, folderById).mapNotNull(folderById::get).map(Folder::name)

private fun folderChainIds(folderId: String?, folderById: Map<String, Folder>): Set<String> {
    val result = linkedSetOf<String>()
    var current = folderId
    while (current != null && result.add(current)) {
        current = folderById[current]?.parentId
    }
    return result
}

private fun sortDocuments(
    documents: List<Document>,
    field: SortField,
    ascending: Boolean
): List<Document> {
    val comparator = when (field) {
        SortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        SortField.DATE_ADDED -> compareByDescending<Document> { it.dateAdded }
        SortField.LAST_OPENED -> compareByDescending<Document> { it.lastOpened ?: 0L }
        SortField.FILE_SIZE -> compareByDescending<Document> { it.sizeBytes }
    }
    val direction = if (ascending) comparator else comparator.reversed()
    return documents.sortedWith(direction.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
}

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
