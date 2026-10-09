package app.leaf.reader.feature.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.data.repo.SearchRepository
import app.leaf.reader.core.domain.SearchAddedWithin
import app.leaf.reader.core.domain.SearchFilters
import app.leaf.reader.core.domain.SearchResults
import app.leaf.reader.core.domain.SearchScope
import app.leaf.reader.core.domain.pushSearchHistory
import app.leaf.reader.core.domain.removeSearchHistoryItem
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.model.SortField
import app.leaf.reader.core.model.Tag
import app.leaf.reader.core.util.LeafClock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Search input, history and filter-panel behaviour for §6.4. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel(
    application: Application,
    private val search: SearchRepository,
    private val settings: SettingsStore,
    private val clock: LeafClock
) : AndroidViewModel(application) {
    private data class SearchWork(
        val input: SearchInput,
        val sortField: SortField,
        val sortAscending: Boolean
    )

    private data class SearchComputation(
        val input: SearchInput,
        val results: SearchResults
    )

    private data class Catalog(val folders: List<Folder>, val tags: List<Tag>)
    private data class PanelState(
        val filterOpen: Boolean,
        val draft: SearchFilters,
        val historyOpen: Boolean
    )
    private data class SettingsAndPanel(
        val settings: LeafSettings,
        val panel: PanelState
    )

    private val input = MutableStateFlow(SearchInput())
    private val filterPanelOpen = MutableStateFlow(false)
    private val draftFilters = MutableStateFlow(SearchFilters())
    private val historyOpen = MutableStateFlow(false)
    private val eventChannel = Channel<SearchEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val work = combine(input, settings.settings) { current, prefs ->
        SearchWork(current, prefs.sortField, prefs.sortAscending)
    }.distinctUntilChanged()

    private val computed = work
        .debounce(SEARCH_DEBOUNCE_MILLIS)
        .distinctUntilChanged()
        .mapLatest { request ->
            val current = request.input
            val results = if (current.query.isBlank() && current.filters.activeCount == 0) {
                SearchResults(query = current.query, scope = current.scope, filters = current.filters)
            } else {
                search.search(
                    query = current.query,
                    scope = current.scope,
                    filters = current.filters,
                    now = clock.nowMillis(),
                    sortField = request.sortField,
                    sortAscending = request.sortAscending
                )
            }
            SearchComputation(current, results)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SearchComputation(SearchInput(), SearchResults())
        )

    private val catalog = combine(search.observeFolders(), search.observeTags(), ::Catalog)
    private val panel = combine(filterPanelOpen, draftFilters, historyOpen, ::PanelState)
    private val settingsAndPanel = combine(settings.settings, panel, ::SettingsAndPanel)
    private val inputAndResults = combine(input, computed) { current, calculation -> current to calculation }

    val state: StateFlow<SearchContentState> = combine(
        inputAndResults,
        catalog,
        settingsAndPanel
    ) { currentAndResult, choices, prefsAndPanel ->
        val (current, calculation) = currentAndResult
        val (prefs, panelState) = prefsAndPanel
        val matchesCurrentInput = calculation.input == current
        SearchContentState(
            input = current,
            results = if (matchesCurrentInput) calculation.results else SearchResults(
                query = current.query,
                scope = current.scope,
                filters = current.filters
            ),
            isSearching = !matchesCurrentInput && (current.query.isNotBlank() || current.filters.activeCount > 0),
            filterPanelOpen = panelState.filterOpen,
            draftFilters = panelState.draft,
            historyOpen = panelState.historyOpen,
            searchHistory = prefs.searchHistory,
            folders = choices.folders,
            tags = choices.tags,
            now = clock.nowMillis()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchContentState())

    fun setQuery(query: String) {
        input.value = input.value.copy(query = query)
    }

    fun clearQuery() {
        input.value = input.value.copy(query = "")
    }

    fun setScope(scope: SearchScope) {
        input.value = input.value.copy(scope = scope)
    }

    /** IME Search is the only action that commits a typed query to history (§6.4). */
    fun commitQuery() {
        val query = input.value.query
        viewModelScope.launch {
            settings.update { current ->
                current.copy(searchHistory = pushSearchHistory(current.searchHistory, query))
            }
        }
    }

    fun rerunQuery(query: String) {
        input.value = input.value.copy(query = query)
    }

    fun toggleFilterPanel() {
        if (filterPanelOpen.value) {
            filterPanelOpen.value = false
        } else {
            draftFilters.value = input.value.filters
            filterPanelOpen.value = true
        }
    }

    fun updateDraftFilters(filters: SearchFilters) {
        draftFilters.value = filters
    }

    fun resetDraftFilters() {
        draftFilters.value = SearchFilters()
    }

    fun applyDraftFilters() {
        input.value = input.value.copy(filters = draftFilters.value)
        filterPanelOpen.value = false
    }

    fun openHistory() {
        historyOpen.value = true
    }

    fun dismissHistory() {
        historyOpen.value = false
    }

    fun runHistoryItem(query: String) {
        input.value = input.value.copy(query = query)
        historyOpen.value = false
    }

    fun removeHistoryItem(query: String) {
        viewModelScope.launch {
            settings.update { current ->
                current.copy(searchHistory = removeSearchHistoryItem(current.searchHistory, query))
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            settings.update { current -> current.copy(searchHistory = emptyList()) }
            eventChannel.send(SearchEvent.SearchHistoryCleared)
        }
    }

    /** Search-by shortcuts use the intent stated by their labels (§6.4). */
    fun runQuickSearch(action: SearchQuickSearch) {
        viewModelScope.launch {
            val current = input.value
            input.value = when (action) {
                SearchQuickSearch.ALL_PDFS -> current.copy(
                    query = "",
                    scope = SearchScope.EVERYTHING,
                    filters = current.filters.copy(type = DocType.PDF)
                )
                SearchQuickSearch.ADDED_THIS_WEEK -> current.copy(
                    query = "",
                    scope = SearchScope.EVERYTHING,
                    filters = current.filters.copy(addedWithin = SearchAddedWithin.WEEK)
                )
                SearchQuickSearch.FOLDER_WORK -> {
                    val folder = search.observeFolders().first().firstOrNull { it.name.equals("Work", ignoreCase = true) }
                    if (folder == null) current else current.copy(
                        query = "",
                        scope = SearchScope.EVERYTHING,
                        filters = current.filters.copy(folderId = folder.id)
                    )
                }
                SearchQuickSearch.TAG_IMPORTANT -> {
                    val tag = search.observeTags().first().firstOrNull { it.name.equals("important", ignoreCase = true) }
                    if (tag == null) current else current.copy(
                        query = "",
                        scope = SearchScope.EVERYTHING,
                        filters = current.filters.copy(tag = tag.name)
                    )
                }
                SearchQuickSearch.WORD_CHAPTER -> current.copy(
                    query = "chapter",
                    scope = SearchScope.INSIDE_FILES
                )
            }
            filterPanelOpen.value = false
        }
    }

    fun toggleFavorite(documentId: String) {
        val document = state.value.results.documents.firstOrNull { it.id == documentId }
            ?: state.value.results.files.firstOrNull { it.document.id == documentId }?.document
            ?: return
        viewModelScope.launch {
            search.setFavorite(documentId, favorite = !document.favorite, now = clock.nowMillis())
            input.update { it.copy(refreshKey = it.refreshKey + 1) }
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 250L
    }
}

enum class SearchQuickSearch {
    ALL_PDFS,
    ADDED_THIS_WEEK,
    FOLDER_WORK,
    TAG_IMPORTANT,
    WORD_CHAPTER
}
