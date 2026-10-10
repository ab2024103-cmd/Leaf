package app.leaf.reader.feature.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.leaf.reader.R
import app.leaf.reader.core.data.prefs.OpenTabResult
import app.leaf.reader.core.data.prefs.ReaderTabsStore
import app.leaf.reader.core.data.repo.DocumentRepository
import app.leaf.reader.core.data.repo.ReaderRepository
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.model.ReaderTab
import app.leaf.reader.core.model.ReaderTabsState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Shell-facing tab actions and the rows needed by the Open documents sheet. */
class ReaderTabsViewModel(
    application: Application,
    private val tabs: ReaderTabsStore,
    private val documents: DocumentRepository,
    private val reader: ReaderRepository,
    private val settings: SettingsStore
) : AndroidViewModel(application) {
    val state: StateFlow<ReaderTabsUiState> = combine(
        tabs.state,
        documents.observeDocuments(),
        settings.settings
    ) { openTabs, allDocuments, userSettings ->
        ReaderTabsUiState(
            tabs = openTabs,
            documents = allDocuments.associateBy(Document::id),
            tabLimit = userSettings.tabLimit,
            isLoaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ReaderTabsUiState())

    private val mutableEvents = MutableSharedFlow<ReaderTabsEvent>(extraBufferCapacity = 8)
    val events = mutableEvents.asSharedFlow()

    fun openDocument(documentId: String, newTab: Boolean = false, page: Int? = null, findQuery: String? = null) {
        viewModelScope.launch {
            val before = tabs.current()
            val existingTab = before.tabs.firstOrNull { it.docId == documentId }
            val prefs = settings.current()
            val saved = reader.progress(documentId)
            val defaultTab = ReaderTab(
                docId = documentId,
                page = page ?: if (prefs.rememberPage) saved?.page ?: 0 else 0,
                zoom = if (prefs.rememberPage) saved?.zoom ?: prefs.zoom else prefs.zoom,
                scrollDir = if (prefs.rememberPage) saved?.scrollDir ?: prefs.scrollDir else prefs.scrollDir,
                findQuery = findQuery,
                scrollFraction = if (page == null && prefs.rememberPage) saved?.scrollFraction ?: 0f else 0f,
                readingTheme = if (prefs.rememberPage) saved?.readingTheme ?: prefs.readingTheme else prefs.readingTheme,
                panX = if (prefs.rememberPage) saved?.panX ?: 0f else 0f,
                panY = if (prefs.rememberPage) saved?.panY ?: 0f else 0f
            )
            when (val result = tabs.open(documentId, newTab, prefs.tabLimit, defaultTab)) {
                OpenTabResult.Opened -> {
                    val message = when {
                        newTab && existingTab == null -> getApplication<Application>().getString(R.string.reader_tabs_opened_new)
                        existingTab != null && before.activeDocId != documentId ->
                            switchedMessage(documentId, page ?: existingTab.page)
                        else -> null
                    }
                    mutableEvents.emit(ReaderTabsEvent.OpenReader(documentId, page, findQuery, message))
                }
                is OpenTabResult.LimitReached -> mutableEvents.emit(ReaderTabsEvent.TabLimitReached(result.limit))
            }
        }
    }

    fun activate(documentId: String) {
        viewModelScope.launch {
            val before = tabs.current()
            val tab = before.tabs.firstOrNull { it.docId == documentId } ?: return@launch
            if (tabs.activate(documentId)) {
                val message = if (before.activeDocId != documentId) switchedMessage(documentId, tab.page) else null
                mutableEvents.emit(ReaderTabsEvent.OpenReader(documentId, null, null, message))
            }
        }
    }

    private fun switchedMessage(documentId: String, page: Int): String? {
        val document = state.value.documents[documentId] ?: return null
        return getApplication<Application>().getString(
            R.string.reader_tabs_switched,
            document.name,
            page + 1,
            document.pageCount.coerceAtLeast(1)
        )
    }

    fun closeTab(documentId: String) {
        viewModelScope.launch {
            val closed = tabs.close(documentId) ?: return@launch
            val remaining = tabs.current()
            val name = state.value.documents[documentId]?.name.orEmpty()
            val message = if (remaining.tabs.isEmpty()) {
                getApplication<Application>().getString(R.string.reader_tabs_closed_last)
            } else {
                getApplication<Application>().getString(R.string.reader_tabs_closed, name)
            }
            mutableEvents.emit(ReaderTabsEvent.TabClosed(closed, message, remaining.tabs.isEmpty()))
        }
    }

    fun undoClose(closed: app.leaf.reader.core.data.prefs.ClosedTab) {
        viewModelScope.launch {
            val wasEmpty = tabs.current().tabs.isEmpty()
            tabs.restore(closed)
            mutableEvents.emit(ReaderTabsEvent.TabRestored)
            if (wasEmpty) {
                mutableEvents.emit(
                    ReaderTabsEvent.OpenReader(
                        closed.tab.docId,
                        closed.tab.page,
                        closed.tab.findQuery
                    )
                )
            }
        }
    }

    fun onReaderLeft() {
        viewModelScope.launch {
            val count = tabs.current().tabs.size
            if (count > 0 && tabs.markKeptOpenHintShown()) {
                mutableEvents.emit(ReaderTabsEvent.KeptOpen(count))
            }
        }
    }

    fun closeAll() {
        viewModelScope.launch {
            val previous = tabs.closeAll()
            if (previous.tabs.isNotEmpty()) mutableEvents.emit(ReaderTabsEvent.AllTabsClosed)
        }
    }

    fun removeDeletedDocument(documentId: String) {
        viewModelScope.launch { tabs.removeDocument(documentId) }
    }

    suspend fun ensureTab(documentId: String) {
        val prefs = settings.current()
        val saved = reader.progress(documentId)
        val defaultTab = ReaderTab(
            docId = documentId,
            page = if (prefs.rememberPage) saved?.page ?: 0 else 0,
            zoom = if (prefs.rememberPage) saved?.zoom ?: prefs.zoom else prefs.zoom,
            scrollDir = if (prefs.rememberPage) saved?.scrollDir ?: prefs.scrollDir else prefs.scrollDir,
            findQuery = null,
            scrollFraction = if (prefs.rememberPage) saved?.scrollFraction ?: 0f else 0f,
            readingTheme = if (prefs.rememberPage) saved?.readingTheme ?: prefs.readingTheme else prefs.readingTheme,
            panX = if (prefs.rememberPage) saved?.panX ?: 0f else 0f,
            panY = if (prefs.rememberPage) saved?.panY ?: 0f else 0f
        )
        tabs.ensureActiveTab(documentId, prefs.tabLimit, defaultTab)
    }
}

data class ReaderTabsUiState(
    val tabs: ReaderTabsState = ReaderTabsState(),
    val documents: Map<String, Document> = emptyMap(),
    val tabLimit: Int = LeafSettings().tabLimit,
    val isLoaded: Boolean = false
)

sealed interface ReaderTabsEvent {
    data class OpenReader(
        val documentId: String,
        val page: Int?,
        val findQuery: String?,
        val message: String? = null
    ) : ReaderTabsEvent

    data class TabLimitReached(val limit: Int) : ReaderTabsEvent

    data class TabClosed(
        val closed: app.leaf.reader.core.data.prefs.ClosedTab,
        val message: String,
        val noTabsRemaining: Boolean
    ) : ReaderTabsEvent

    data class KeptOpen(val count: Int) : ReaderTabsEvent

    data object TabRestored : ReaderTabsEvent

    data object AllTabsClosed : ReaderTabsEvent
}
