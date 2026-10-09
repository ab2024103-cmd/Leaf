package app.leaf.reader.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.leaf.reader.R
import app.leaf.reader.core.ui.components.LeafConfirmDialogOverlay
import app.leaf.reader.core.ui.components.LeafEmptyState
import app.leaf.reader.core.ui.components.LeafIconButton
import app.leaf.reader.core.ui.components.LeafSnackbar
import app.leaf.reader.core.ui.components.LeafTopAppBar
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.ui.util.annotated
import app.leaf.reader.core.ui.util.quantityText
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchScreen(
    onOpenDocument: (String) -> Unit,
    onOpenFileSearch: (documentId: String, pageIndex: Int, query: String) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showHistoryCleared by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SearchEvent.SearchHistoryCleared -> showHistoryCleared = true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LeafTopAppBar(
                title = stringResource(R.string.nav_search),
                actions = {
                    LeafIconButton(
                        icon = R.drawable.ic_clock,
                        contentDescription = stringResource(R.string.search_history_title),
                        onClick = viewModel::openHistory
                    )
                }
            )
            SearchInputBar(
                query = state.input.query,
                onQueryChange = viewModel::setQuery,
                onSearch = viewModel::commitQuery,
                onClear = viewModel::clearQuery,
                modifier = Modifier.padding(top = 2.dp)
            )
            SearchScopeBar(
                selected = state.input.scope,
                activeFilterCount = state.input.filters.activeCount,
                onSelect = viewModel::setScope,
                onFilters = viewModel::toggleFilterPanel
            )
            AnimatedVisibility(visible = state.filterPanelOpen) {
                SearchFilterPanel(
                    filters = state.draftFilters,
                    folders = state.folders,
                    tags = state.tags,
                    onChange = viewModel::updateDraftFilters,
                    onReset = viewModel::resetDraftFilters,
                    onApply = viewModel::applyDraftFilters,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.input.query.isBlank() && state.searchHistory.isNotEmpty()) {
                    item(key = "recent-searches") {
                        RecentSearches(
                            history = state.searchHistory,
                            onRun = viewModel::rerunQuery,
                            onRemove = viewModel::removeHistoryItem,
                            onClear = { confirmClearHistory = true }
                        )
                    }
                }
                if (state.input.query.isBlank()) {
                    item(key = "search-by") {
                        SearchByChips(viewModel::runQuickSearch)
                    }
                }

                when {
                    !state.hasSearchCriteria -> item(key = "search-empty") {
                        LeafEmptyState(
                            icon = R.drawable.ic_nav_search,
                            title = stringResource(R.string.empty_search_title),
                            message = annotated(stringResource(R.string.empty_search_message)),
                            modifier = Modifier.padding(top = 26.dp)
                        )
                    }
                    state.isSearching -> item(key = "search-progress") {
                        Box(Modifier.fillMaxWidth().padding(top = 28.dp), contentAlignment = Alignment.TopCenter) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    else -> {
                        val results = state.results
                        val query = state.input.query
                        if (query.isNotBlank()) {
                            item(key = "search-summary") {
                                SearchSummaryBanner(state)
                            }
                        }
                        if (results.documents.isNotEmpty()) {
                            item(key = "documents-heading") {
                                Text(
                                    stringResource(R.string.search_documents_heading, results.documents.size),
                                    style = LeafType.sectionLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
                                )
                            }
                            items(results.documents, key = { "doc-${it.id}" }) { document ->
                                SearchDocumentRow(
                                    document = document,
                                    now = state.now,
                                    tagColors = state.tags.associate { it.name to it.colorHex },
                                    canOpen = document.type == app.leaf.reader.core.model.DocType.PDF,
                                    onOpen = { onOpenDocument(document.id) },
                                    onFavorite = { viewModel.toggleFavorite(document.id) }
                                )
                            }
                        }
                        if (results.files.isNotEmpty()) {
                            item(key = "inside-files-heading") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.search_inside_heading, results.files.size),
                                        style = LeafType.sectionLabel,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        quantityText(R.plurals.search_match_count, results.totalFileHits, results.totalFileHits),
                                        style = LeafType.supporting,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(results.files, key = { "file-${it.document.id}" }) { file ->
                                SearchFileResultRow(
                                    result = file,
                                    now = state.now,
                                    onOpen = { onOpenFileSearch(file.document.id, file.firstPageIndex, query) },
                                    onFavorite = { viewModel.toggleFavorite(file.document.id) }
                                )
                            }
                        }
                        if (results.documents.isEmpty() && results.files.isEmpty()) {
                            item(key = "no-results") {
                                LeafEmptyState(
                                    icon = R.drawable.ic_nav_search,
                                    title = stringResource(R.string.search_no_results_title),
                                    message = if (query.isNotBlank()) {
                                        stringResource(
                                            R.string.search_no_results_message,
                                            query,
                                            state.input.filters.activeCount
                                        )
                                    } else {
                                        ""
                                    },
                                    modifier = Modifier.padding(top = 22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showHistoryCleared) {
            LeafSnackbar(
                message = stringResource(R.string.snack_search_history_cleared),
                modifier = Modifier.align(Alignment.BottomCenter),
                onDismiss = { showHistoryCleared = false }
            )
        }

        if (state.historyOpen) {
            SearchHistorySheet(
                history = state.searchHistory,
                onDismiss = viewModel::dismissHistory,
                onRun = viewModel::runHistoryItem,
                onRemove = viewModel::removeHistoryItem,
                onClear = { confirmClearHistory = true }
            )
        }
        if (confirmClearHistory) {
            LeafConfirmDialogOverlay(
                title = stringResource(R.string.dialog_clear_search_history_title),
                body = "",
                confirmLabel = stringResource(R.string.search_history_clear),
                onConfirm = {
                    confirmClearHistory = false
                    viewModel.clearHistory()
                    viewModel.dismissHistory()
                },
                onDismiss = { confirmClearHistory = false }
            )
        }
    }
}

@Composable
private fun SearchSummaryBanner(state: SearchContentState) {
    SurfaceSummary(
        text = SearchSummaryText(
            documentCount = state.results.documents.size,
            fileCount = state.results.fileCount,
            query = state.input.query.trim(),
            hits = state.results.totalFileHits
        )
    )
}

@Composable
private fun SurfaceSummary(text: String) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("✓", style = LeafType.listTitle, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(text, style = LeafType.listTitle, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}
