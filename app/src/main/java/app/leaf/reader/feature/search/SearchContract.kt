package app.leaf.reader.feature.search

import app.leaf.reader.core.domain.SearchFilters
import app.leaf.reader.core.domain.SearchResults
import app.leaf.reader.core.domain.SearchScope
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.Tag

/** Inputs whose changes trigger one debounced library search. */
data class SearchInput(
    val query: String = "",
    val scope: SearchScope = SearchScope.EVERYTHING,
    val filters: SearchFilters = SearchFilters(),
    /** Invalidates a result snapshot after a document's visible metadata changes. */
    val refreshKey: Int = 0
)

data class SearchContentState(
    val input: SearchInput = SearchInput(),
    val results: SearchResults = SearchResults(),
    val isSearching: Boolean = false,
    val filterPanelOpen: Boolean = false,
    val draftFilters: SearchFilters = SearchFilters(),
    val historyOpen: Boolean = false,
    val searchHistory: List<String> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val now: Long = 0L
) {
    val hasSearchCriteria: Boolean get() = input.query.isNotBlank() || input.filters.activeCount > 0
}

sealed interface SearchEvent {
    data object SearchHistoryCleared : SearchEvent
}
