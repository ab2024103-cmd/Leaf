package app.leaf.reader.feature.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.leaf.reader.R
import app.leaf.reader.core.data.prefs.ReaderTabsStore
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.data.repo.AnnotationRepository
import app.leaf.reader.core.data.repo.ReaderRepository
import app.leaf.reader.core.domain.countOccurrences
import app.leaf.reader.core.format.DocumentEngine
import app.leaf.reader.core.format.DocumentEngineFactory
import app.leaf.reader.core.model.Bookmark
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Highlight
import app.leaf.reader.core.model.HighlightColor
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.model.NormalizedRect
import app.leaf.reader.core.model.Progress
import app.leaf.reader.core.model.ReaderTab
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.util.LeafClock
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Reader orchestration: PDF lifecycle, Page Stay, tabs and persistent annotations. */
class ReaderViewModel(
    application: Application,
    private val repository: ReaderRepository,
    private val annotations: AnnotationRepository,
    private val tabs: ReaderTabsStore,
    private val settings: SettingsStore,
    private val engines: DocumentEngineFactory,
    private val clock: LeafClock
) : AndroidViewModel(application) {
    private val mutableState = MutableStateFlow(ReaderContentState())
    val state: StateFlow<ReaderContentState> = mutableState.asStateFlow()

    private var activeEngine: DocumentEngine? = null
    private var activeDocumentId: String? = null
    private var progressJob: Job? = null
    private var extractionJob: Job? = null
    private var findJob: Job? = null
    private var annotationJob: Job? = null
    private var session = 0
    private val annotationMutationMutex = Mutex()
    private val annotationHistories = mutableMapOf<String, AnnotationHistory>()
    private val unresolvedHighlightIds = mutableSetOf<String>()
    private var selectionWordsPage: Int? = null
    private var selectionWords: List<SelectableWord> = emptyList()

    fun openDocument(documentId: String, initialPage: Int? = null, initialFindQuery: String? = null) {
        if (documentId == activeDocumentId && activeEngine != null) {
            if (initialPage != null) jumpToPage(initialPage)
            if (!initialFindQuery.isNullOrBlank()) setFindQuery(initialFindQuery)
            return
        }
        val request = ++session
        val previous = mutableState.value
        val previousEngine = activeEngine
        progressJob?.cancel()
        extractionJob?.cancel()
        findJob?.cancel()
        annotationJob?.cancel()
        activeEngine = null
        activeDocumentId = documentId
        selectionWordsPage = null
        selectionWords = emptyList()
        unresolvedHighlightIds.clear()
        mutableState.value = ReaderContentState(isLoading = true)

        viewModelScope.launch {
            try {
                if (previous.document != null) persist(previous)
                previousEngine?.let { withContext(Dispatchers.IO) { it.close() } }
                val document = repository.document(documentId)
                    ?: throw IllegalStateException(getApplication<Application>().getString(R.string.reader_error_missing_document))
                if (document.type != DocType.PDF) {
                    throw UnsupportedOperationException(getApplication<Application>().getString(R.string.reader_error_pdf_only))
                }
                val prefs = settings.current()
                val stored = repository.progress(documentId)
                val savedTab = tabs.current().tabs.firstOrNull { it.docId == documentId }
                val tab = if (savedTab == null) {
                    val initialTab = defaultTab(documentId, prefs, stored)
                    tabs.ensureActiveTab(documentId, prefs.tabLimit, initialTab)
                    tabs.current().tabs.firstOrNull { it.docId == documentId } ?: initialTab
                } else {
                    tabs.activate(documentId)
                    savedTab
                }
                val engine = engines.open(document.uri)
                if (request != session) {
                    withContext(Dispatchers.IO) { engine.close() }
                    return@launch
                }
                if (engine.pageCount <= 0) {
                    withContext(Dispatchers.IO) { engine.close() }
                    throw IllegalStateException(getApplication<Application>().getString(R.string.reader_error_empty_pdf))
                }
                activeEngine = engine
                val pageCount = engine.pageCount
                if (document.pageCount != pageCount) repository.updatePageCount(documentId, pageCount)
                val savedBookmarks = annotations.bookmarks(documentId)
                val savedHighlights = annotations.highlights(documentId)
                val page = initialPage?.coerceIn(0, pageCount - 1)
                    ?: tab.page.coerceIn(0, pageCount - 1)
                val initialQuery = initialFindQuery?.takeIf(String::isNotBlank)
                    ?: tab.findQuery?.takeIf(String::isNotBlank).orEmpty()
                val initial = ReaderContentState(
                    document = document.copy(pageCount = pageCount),
                    engine = engine,
                    pageCount = pageCount,
                    pageIndex = page,
                    scrollFraction = if (initialPage != null || initialQuery.isNotEmpty()) 0f
                        else tab.scrollFraction.coerceIn(0f, 1f),
                    zoom = tab.zoom.coerceIn(LeafSettings.ZOOM_MIN, LeafSettings.ZOOM_MAX),
                    scrollDir = tab.scrollDir,
                    readingTheme = tab.readingTheme,
                    panX = tab.panX,
                    panY = tab.panY,
                    bookmarks = savedBookmarks,
                    highlights = savedHighlights,
                    highlightColor = prefs.highlightColor,
                    warning = if (pageCount > MAX_PAGES) getApplication<Application>().getString(R.string.reader_warning_page_limit) else null,
                    findBarVisible = initialQuery.isNotEmpty(),
                    findQuery = initialQuery,
                    positionRequestId = 1
                )
                mutableState.value = initial
                refreshHistoryState(documentId)
                repository.recordOpen(documentId, clock.nowMillis())
                observeAnnotations(documentId, engine, request)
                extractTextIfNeeded(documentId, engine, pageCount, request)
                schedulePersist(immediate = true)
                if (initialQuery.isNotEmpty()) setFindQuery(initialQuery)
            } catch (error: Exception) {
                if (request == session) {
                    activeEngine?.let { withContext(Dispatchers.IO) { it.close() } }
                    activeEngine = null
                    mutableState.value = ReaderContentState(
                        error = error.message?.takeIf(String::isNotBlank)
                            ?: getApplication<Application>().getString(R.string.reader_page_render_error)
                    )
                }
            }
        }
    }

    private fun defaultTab(documentId: String, prefs: LeafSettings, stored: Progress?): ReaderTab = ReaderTab(
        docId = documentId,
        page = if (prefs.rememberPage) stored?.page ?: 0 else 0,
        zoom = if (prefs.rememberPage) stored?.zoom ?: prefs.zoom else prefs.zoom,
        scrollDir = if (prefs.rememberPage) stored?.scrollDir ?: prefs.scrollDir else prefs.scrollDir,
        findQuery = null,
        scrollFraction = if (prefs.rememberPage) stored?.scrollFraction ?: 0f else 0f,
        readingTheme = if (prefs.rememberPage) stored?.readingTheme ?: prefs.readingTheme else prefs.readingTheme,
        panX = if (prefs.rememberPage) stored?.panX ?: 0f else 0f,
        panY = if (prefs.rememberPage) stored?.panY ?: 0f else 0f
    )

    private fun observeAnnotations(documentId: String, engine: DocumentEngine, request: Int) {
        annotationJob = viewModelScope.launch {
            combine(
                annotations.observeBookmarks(documentId),
                annotations.observeHighlights(documentId)
            ) { bookmarks, highlights -> bookmarks to highlights }
                .collect { (bookmarks, highlights) ->
                    if (request != session) return@collect
                    mutableState.update { current ->
                        if (current.document?.id != documentId) current
                        else current.copy(bookmarks = bookmarks, highlights = highlights)
                    }
                    resolveMissingHighlightBounds(documentId, engine, highlights, request)
                }
        }
    }

    private suspend fun resolveMissingHighlightBounds(
        documentId: String,
        engine: DocumentEngine,
        highlights: List<Highlight>,
        request: Int
    ) {
        highlights.asSequence()
            .filter { it.bounds.isEmpty() && it.text.isNotBlank() && it.id !in unresolvedHighlightIds }
            .forEach { highlight ->
                if (request != session) return
                unresolvedHighlightIds += highlight.id
                val bounds = try {
                    withContext(Dispatchers.IO) {
                        engine.findInPage(highlight.page, highlight.text).firstOrNull()?.bounds.orEmpty()
                    }
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (_: Exception) {
                    emptyList()
                }
                if (bounds.isNotEmpty() && request == session) {
                    annotations.insert(highlight.copy(bounds = bounds))
                }
            }
    }

    private fun extractTextIfNeeded(documentId: String, engine: DocumentEngine, pageCount: Int, request: Int) {
        extractionJob = viewModelScope.launch(Dispatchers.IO) {
            val cached = repository.cachedText(documentId)
            if (cached.size >= pageCount || request != session) return@launch
            mutableState.update { if (request == session) it.copy(textExtractionProgress = 0f) else it }
            try {
                val pages = engine.extractAllText { completed, total ->
                    if (request == session && total > 0) {
                        mutableState.update { it.copy(textExtractionProgress = completed / total.toFloat()) }
                    }
                }
                if (request == session) {
                    repository.cacheText(documentId, pages.map { runs -> runs.joinToString("\n") { it.text } })
                    mutableState.update { it.copy(textExtractionProgress = null) }
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (_: Exception) {
                // Raster reading remains available for scanned, encrypted, or text-incompatible PDFs.
                if (request == session) mutableState.update { it.copy(textExtractionProgress = null) }
            }
        }
    }

    fun updatePosition(pageIndex: Int, scrollFraction: Float) {
        val current = mutableState.value
        if (current.document == null || current.pageCount <= 0) return
        val safePage = pageIndex.coerceIn(0, current.pageCount - 1)
        mutableState.update {
            it.copy(
                pageIndex = safePage,
                scrollFraction = scrollFraction.coerceIn(0f, 1f),
                selection = it.selection?.takeIf { selected -> selected.pageIndex == safePage },
                selectedHighlightId = it.selectedHighlightId?.takeIf { id ->
                    it.highlights.any { highlight -> highlight.id == id && highlight.page == safePage }
                }
            )
        }
        schedulePersist()
    }

    fun jumpToPage(pageIndex: Int) {
        val current = mutableState.value
        if (current.pageCount <= 0) return
        val page = pageIndex.coerceIn(0, current.pageCount - 1)
        mutableState.update {
            it.copy(
                pageIndex = page,
                scrollFraction = 0f,
                selection = null,
                selectedHighlightId = null,
                positionRequestId = it.positionRequestId + 1
            )
        }
        schedulePersist()
    }

    fun toggleFindBar() {
        if (mutableState.value.findBarVisible) closeFindBar()
        else mutableState.update { it.copy(findBarVisible = true) }
    }

    fun setFindQuery(query: String) {
        mutableState.update {
            it.copy(findBarVisible = true, findQuery = query, findMatches = emptyList(), findIndex = -1)
        }
        findJob?.cancel()
        if (query.isBlank()) return
        val request = session
        findJob = viewModelScope.launch {
            delay(FIND_DEBOUNCE_MILLIS)
            val documentId = activeDocumentId ?: return@launch
            val current = mutableState.value
            val engine = activeEngine ?: return@launch
            if (request != session || current.findQuery != query) return@launch

            var pages = repository.cachedText(documentId)
            if (pages.size < current.pageCount) {
                extractionJob?.join()
                if (request != session) return@launch
                pages = repository.cachedText(documentId)
            }
            val found = withContext(Dispatchers.IO) {
                val matchesPerPage = pages.sortedBy { it.pageIndex }.mapNotNull { page ->
                    val count = countOccurrences(page.text, query)
                    count.takeIf { it > 0 }?.let { page.pageIndex to it }
                }
                val geometryByPage = try {
                    engine.findInPages(matchesPerPage.map { it.first }, query)
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (_: Exception) {
                    emptyMap()
                }
                buildList {
                    matchesPerPage.forEach { (pageIndex, occurrenceCount) ->
                        val geometry = geometryByPage[pageIndex].orEmpty()
                        repeat(occurrenceCount) { index ->
                            add(ReaderFindMatch(pageIndex, geometry.getOrNull(index)?.bounds.orEmpty()))
                        }
                    }
                }
            }
            if (request != session || mutableState.value.findQuery != query) return@launch
            mutableState.update { latest ->
                if (latest.findQuery != query) latest else {
                    val first = found.firstOrNull()
                    latest.copy(
                        findMatches = found,
                        findIndex = if (first == null) -1 else 0,
                        pageIndex = first?.pageIndex ?: latest.pageIndex,
                        scrollFraction = first?.bounds?.minOfOrNull { it.top } ?: latest.scrollFraction,
                        selection = null,
                        selectedHighlightId = null,
                        positionRequestId = latest.positionRequestId + if (first == null) 0 else 1
                    )
                }
            }
            if (found.isNotEmpty()) schedulePersist(immediate = true)
        }
    }

    fun closeFindBar() {
        findJob?.cancel()
        mutableState.update {
            it.copy(findBarVisible = false, findQuery = "", findMatches = emptyList(), findIndex = -1)
        }
        schedulePersist(immediate = true)
    }

    fun nextFindMatch() = stepFindMatch(1)

    fun previousFindMatch() = stepFindMatch(-1)

    private fun stepFindMatch(direction: Int) {
        val current = mutableState.value
        if (current.findMatches.isEmpty()) return
        val start = if (current.findIndex < 0) 0 else current.findIndex
        val next = (start + direction + current.findMatches.size) % current.findMatches.size
        val match = current.findMatches[next]
        mutableState.update {
            it.copy(
                findIndex = next,
                pageIndex = match.pageIndex,
                scrollFraction = match.bounds.minOfOrNull { rect -> rect.top } ?: 0f,
                selection = null,
                selectedHighlightId = null,
                positionRequestId = it.positionRequestId + 1
            )
        }
        schedulePersist()
    }

    fun setZoom(zoom: Float) {
        mutableState.update { current ->
            val next = zoom.coerceIn(LeafSettings.ZOOM_MIN, LeafSettings.ZOOM_MAX)
            val limit = ((next - 1f) / 2f).coerceAtLeast(0f)
            current.copy(
                zoom = next,
                panX = current.panX.coerceIn(-limit, limit),
                panY = current.panY.coerceIn(-limit, limit)
            )
        }
        schedulePersist()
    }

    fun stepZoom(direction: Int) {
        if (direction == 0) return
        val current = mutableState.value.zoom
        setZoom((current + direction * ZOOM_STEP).coerceIn(LeafSettings.ZOOM_MIN, LeafSettings.ZOOM_MAX))
    }

    fun setScrollDirection(direction: ScrollDir) {
        mutableState.update { it.copy(scrollDir = direction, scrollFraction = 0f) }
        schedulePersist(immediate = true)
    }

    fun setReadingTheme(theme: ReadingTheme) {
        mutableState.update { it.copy(readingTheme = theme) }
        schedulePersist(immediate = true)
    }

    /** Pan fractions are relative to the fit page, so Page Stay survives density changes. */
    fun updatePan(panX: Float, panY: Float) {
        mutableState.update { current ->
            val limit = ((current.zoom - 1f) / 2f).coerceAtLeast(0f)
            current.copy(
                panX = panX.coerceIn(-limit, limit),
                panY = panY.coerceIn(-limit, limit)
            )
        }
        schedulePersist()
    }

    fun toggleBookmarkCurrentPage() {
        val page = mutableState.value.pageIndex
        val documentId = activeDocumentId ?: return
        viewModelScope.launch {
            annotationMutationMutex.withLock {
                val before = snapshot(documentId)
                val existing = before.bookmarks.firstOrNull { it.page == page }
                val afterBookmarks = if (existing == null) {
                    before.bookmarks + Bookmark(
                        id = UUID.randomUUID().toString(),
                        docId = documentId,
                        page = page,
                        label = getApplication<Application>().getString(R.string.reader_bookmark_page_label, page + 1),
                        createdAt = clock.nowMillis()
                    )
                } else before.bookmarks.filterNot { it.id == existing.id }
                val after = before.copy(bookmarks = afterBookmarks)
                commitSnapshot(
                    documentId = documentId,
                    before = before,
                    after = after,
                    label = getApplication<Application>().getString(
                        if (existing == null) R.string.snack_page_bookmarked else R.string.snack_bookmark_removed,
                        page + 1
                    ),
                    selectedHighlightId = null
                )
            }
        }
    }

    fun deleteBookmark(id: String) {
        val documentId = activeDocumentId ?: return
        viewModelScope.launch {
            annotationMutationMutex.withLock {
                val before = snapshot(documentId)
                val bookmark = before.bookmarks.firstOrNull { it.id == id } ?: return@withLock
                val after = before.copy(bookmarks = before.bookmarks.filterNot { it.id == id })
                commitSnapshot(
                    documentId,
                    before,
                    after,
                    getApplication<Application>().getString(R.string.snack_bookmark_removed, bookmark.page + 1),
                    null
                )
            }
        }
    }

    fun jumpToBookmark(bookmark: Bookmark) {
        setSheet(ReaderSheet.NONE)
        jumpToPage(bookmark.page)
    }

    fun toggleHighlightMenu() {
        mutableState.update { it.copy(highlightMenuExpanded = !it.highlightMenuExpanded) }
    }

    fun dismissHighlightMenu() {
        mutableState.update { it.copy(highlightMenuExpanded = false) }
    }

    fun showCopiedMessage() {
        showSnackbar(getApplication<Application>().getString(R.string.snack_copied))
    }

    fun selectHighlightColor(color: HighlightColor) {
        viewModelScope.launch {
            settings.update { it.copy(highlightColor = color) }
            mutableState.update { it.copy(highlightColor = color, highlightMenuExpanded = false) }
            val current = mutableState.value
            when {
                current.selectedHighlightId != null -> recolorHighlight(current.selectedHighlightId, color)
                current.selection != null -> addHighlight(current.selection, color)
                else -> showSnackbar(
                    getApplication<Application>().getString(R.string.snack_selected_colour, colorTitle(color))
                )
            }
        }
    }

    fun addHighlight() {
        val selection = mutableState.value.selection ?: return
        addHighlight(selection, mutableState.value.highlightColor)
    }

    fun addHighlight(selection: ReaderSelection, color: HighlightColor) {
        val documentId = activeDocumentId ?: return
        viewModelScope.launch {
            val engine = activeEngine
            val resolvedBounds = if (engine != null && selection.text.isNotBlank()) {
                try {
                    withContext(Dispatchers.IO) {
                        val candidates = engine.findInPage(selection.pageIndex, selection.text)
                        val centerX = selection.bounds.centerX()
                        val centerY = selection.bounds.centerY()
                        candidates.minByOrNull { match -> match.bounds.distanceSquared(centerX, centerY) }?.bounds
                    }
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
            } else null
            val highlight = Highlight(
                id = UUID.randomUUID().toString(),
                docId = documentId,
                page = selection.pageIndex,
                color = color,
                text = selection.text,
                bounds = resolvedBounds?.takeIf { it.isNotEmpty() } ?: selection.bounds,
                textRange = null,
                cfiRange = null,
                createdAt = clock.nowMillis()
            )
            annotationMutationMutex.withLock {
                val before = snapshot(documentId)
                val after = before.copy(highlights = before.highlights + highlight)
                commitSnapshot(
                    documentId,
                    before,
                    after,
                    getApplication<Application>().getString(R.string.snack_highlighted, colorLowercase(color)),
                    highlight.id,
                    clearSelection = true
                )
            }
        }
    }

    fun recolorHighlight(id: String, color: HighlightColor) {
        val documentId = activeDocumentId ?: return
        viewModelScope.launch {
            annotationMutationMutex.withLock {
                val before = snapshot(documentId)
                val target = before.highlights.firstOrNull { it.id == id } ?: return@withLock
                if (target.color == color) return@withLock
                val after = before.copy(highlights = before.highlights.map { if (it.id == id) it.copy(color = color) else it })
                commitSnapshot(
                    documentId,
                    before,
                    after,
                    getApplication<Application>().getString(R.string.snack_colour_changed, colorLowercase(color)),
                    selectedHighlightId = id
                )
            }
        }
    }

    fun removeHighlight(id: String) {
        val documentId = activeDocumentId ?: return
        viewModelScope.launch {
            annotationMutationMutex.withLock {
                val before = snapshot(documentId)
                if (before.highlights.none { it.id == id }) return@withLock
                val after = before.copy(highlights = before.highlights.filterNot { it.id == id })
                commitSnapshot(
                    documentId,
                    before,
                    after,
                    getApplication<Application>().getString(R.string.snack_highlight_removed),
                    selectedHighlightId = null
                )
            }
        }
    }

    fun clearPageHighlights() {
        val current = mutableState.value
        val documentId = activeDocumentId ?: return
        val page = current.pageIndex
        viewModelScope.launch {
            annotationMutationMutex.withLock {
                val before = snapshot(documentId)
                val onPage = before.highlights.filter { it.page == page }
                if (onPage.isEmpty()) {
                    showSnackbar(getApplication<Application>().getString(R.string.snack_no_page_highlights, page + 1))
                    return@withLock
                }
                val after = before.copy(highlights = before.highlights.filterNot { it.page == page })
                val message = getApplication<Application>().resources.getQuantityString(
                    R.plurals.snack_page_highlights_cleared,
                    onPage.size,
                    onPage.size
                )
                commitSnapshot(
                    documentId,
                    before,
                    after,
                    message,
                    selectedHighlightId = null,
                    clearSelection = true
                )
            }
        }
    }

    fun clearAllHighlights() {
        val documentId = activeDocumentId ?: return
        viewModelScope.launch {
            annotationMutationMutex.withLock {
                val before = snapshot(documentId)
                if (before.highlights.isEmpty()) return@withLock
                val after = before.copy(highlights = emptyList())
                commitSnapshot(
                    documentId,
                    before,
                    after,
                    getApplication<Application>().getString(R.string.snack_all_highlights_removed),
                    selectedHighlightId = null,
                    clearSelection = true
                )
            }
        }
    }

    fun jumpToHighlight(highlight: Highlight) {
        setSheet(ReaderSheet.NONE)
        mutableState.update {
            it.copy(
                pageIndex = highlight.page.coerceIn(0, (it.pageCount - 1).coerceAtLeast(0)),
                scrollFraction = highlight.bounds.minOfOrNull(NormalizedRect::top) ?: 0f,
                selection = null,
                selectedHighlightId = highlight.id,
                positionRequestId = it.positionRequestId + 1
            )
        }
        schedulePersist(immediate = true)
    }

    fun setSelectedHighlight(id: String?) {
        mutableState.update {
            it.copy(selectedHighlightId = id, selection = if (id == null) it.selection else null)
        }
    }

    fun clearSelection() {
        mutableState.update { it.copy(selection = null, selectedHighlightId = null) }
    }

    fun selectTextAt(pageIndex: Int, normalizedX: Float, normalizedY: Float) {
        val request = session
        viewModelScope.launch {
            val engine = activeEngine ?: return@launch
            val runs = try {
                withContext(Dispatchers.IO) { engine.pageText(pageIndex) }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (_: Exception) {
                emptyList()
            }
            if (request != session) return@launch
            selectionWordsPage = pageIndex
            selectionWords = runs.selectableWords()
            val index = nearestWordIndex(selectionWords, normalizedX, normalizedY)
            if (index == null) {
                mutableState.update { it.copy(selection = null, selectedHighlightId = null) }
                return@launch
            }
            val selection = ReaderSelection(
                pageIndex = pageIndex,
                text = selectionText(selectionWords, index, index),
                bounds = selectionBounds(selectionWords, index, index),
                startWord = index,
                endWord = index
            )
            mutableState.update {
                it.copy(selection = selection, selectedHighlightId = null, highlightMenuExpanded = false)
            }
        }
    }

    fun moveSelectionHandle(isStart: Boolean, normalizedX: Float, normalizedY: Float) {
        val current = mutableState.value.selection ?: return
        if (selectionWordsPage != current.pageIndex) return
        val index = nearestWordIndex(selectionWords, normalizedX, normalizedY) ?: return
        val start = if (isStart) index else current.startWord
        val end = if (isStart) current.endWord else index
        val selection = current.copy(
            text = selectionText(selectionWords, start, end),
            bounds = selectionBounds(selectionWords, start, end),
            startWord = start,
            endWord = end
        )
        mutableState.update { it.copy(selection = selection) }
    }

    fun tapPage(pageIndex: Int, normalizedX: Float, normalizedY: Float) {
        val tapped = mutableState.value.highlights.firstOrNull { highlight ->
            highlight.page == pageIndex && highlight.bounds.any { rect ->
                normalizedX >= rect.left - HIT_TEST_INSET && normalizedX <= rect.right + HIT_TEST_INSET &&
                    normalizedY >= rect.top - HIT_TEST_INSET && normalizedY <= rect.bottom + HIT_TEST_INSET
            }
        }
        if (tapped != null) {
            setSelectedHighlight(tapped.id)
        } else {
            clearSelection()
        }
    }

    fun undoAnnotation() {
        val documentId = activeDocumentId ?: return
        viewModelScope.launch {
            annotationMutationMutex.withLock {
                val change = history(documentId).takeUndo() ?: return@withLock
                annotations.replaceBookmarks(documentId, change.before.bookmarks)
                annotations.replaceHighlights(documentId, change.before.highlights)
                mutableState.update { current ->
                    if (current.document?.id != documentId) current
                    else current.copy(
                        bookmarks = change.before.bookmarks,
                        highlights = change.before.highlights,
                        selection = null,
                        selectedHighlightId = null,
                        snackbar = ReaderSnackbarState(getApplication<Application>().getString(R.string.snack_undone, change.label))
                    )
                }
                refreshHistoryState(documentId)
            }
        }
    }

    fun redoAnnotation() {
        val documentId = activeDocumentId ?: return
        viewModelScope.launch {
            annotationMutationMutex.withLock {
                val change = history(documentId).takeRedo() ?: return@withLock
                annotations.replaceBookmarks(documentId, change.after.bookmarks)
                annotations.replaceHighlights(documentId, change.after.highlights)
                mutableState.update { current ->
                    if (current.document?.id != documentId) current
                    else current.copy(
                        bookmarks = change.after.bookmarks,
                        highlights = change.after.highlights,
                        selection = null,
                        selectedHighlightId = null,
                        snackbar = ReaderSnackbarState(getApplication<Application>().getString(R.string.snack_redone, change.label))
                    )
                }
                refreshHistoryState(documentId)
            }
        }
    }

    fun dismissSnackbar() = mutableState.update { it.copy(snackbar = null) }

    private suspend fun snapshot(documentId: String) = AnnotationSnapshot(
        bookmarks = annotations.bookmarks(documentId),
        highlights = annotations.highlights(documentId)
    )

    private suspend fun commitSnapshot(
        documentId: String,
        before: AnnotationSnapshot,
        after: AnnotationSnapshot,
        label: String,
        selectedHighlightId: String? = mutableState.value.selectedHighlightId,
        clearSelection: Boolean = false
    ) {
        if (before == after) return
        annotations.replaceBookmarks(documentId, after.bookmarks)
        annotations.replaceHighlights(documentId, after.highlights)
        history(documentId).record(AnnotationChange(before, after, label))
        mutableState.update { current ->
            if (current.document?.id != documentId) current
            else current.copy(
                bookmarks = after.bookmarks,
                highlights = after.highlights,
                selectedHighlightId = selectedHighlightId,
                selection = if (clearSelection) null else current.selection,
                highlightMenuExpanded = false,
                snackbar = ReaderSnackbarState(label, canUndo = true)
            )
        }
        refreshHistoryState(documentId)
    }

    private fun showSnackbar(message: String, canUndo: Boolean = false) {
        mutableState.update { it.copy(snackbar = ReaderSnackbarState(message, canUndo)) }
    }

    private fun colorTitle(color: HighlightColor): String = getApplication<Application>().getString(
        when (color) {
            HighlightColor.YELLOW -> R.string.reader_colour_yellow
            HighlightColor.GREEN -> R.string.reader_colour_green
            HighlightColor.BLUE -> R.string.reader_colour_blue
            HighlightColor.PINK -> R.string.reader_colour_pink
            HighlightColor.ORANGE -> R.string.reader_colour_orange
        }
    )

    private fun colorLowercase(color: HighlightColor): String = getApplication<Application>().getString(
        when (color) {
            HighlightColor.YELLOW -> R.string.reader_colour_yellow_lower
            HighlightColor.GREEN -> R.string.reader_colour_green_lower
            HighlightColor.BLUE -> R.string.reader_colour_blue_lower
            HighlightColor.PINK -> R.string.reader_colour_pink_lower
            HighlightColor.ORANGE -> R.string.reader_colour_orange_lower
        }
    )

    private fun history(documentId: String) = annotationHistories.getOrPut(documentId) { AnnotationHistory() }

    private fun refreshHistoryState(documentId: String) {
        if (activeDocumentId != documentId) return
        val history = history(documentId)
        mutableState.update { it.copy(canUndoAnnotation = history.canUndo, canRedoAnnotation = history.canRedo) }
    }

    fun setSheet(sheet: ReaderSheet, clearSelectionMenu: Boolean = true) = mutableState.update {
        it.copy(sheet = sheet, highlightMenuExpanded = if (clearSelectionMenu) false else it.highlightMenuExpanded)
    }

    fun toggleFullScreen() = mutableState.update {
        it.copy(isFullScreen = !it.isFullScreen, highlightMenuExpanded = false)
    }

    /** Persist the active page, then release the renderer while the tab remains open. */
    fun leaveReader(onLeft: () -> Unit) {
        val request = ++session
        val snapshot = mutableState.value
        val engine = activeEngine
        val jobsToStop = listOfNotNull(progressJob, extractionJob, findJob, annotationJob)
        jobsToStop.forEach { it.cancel() }
        progressJob = null
        extractionJob = null
        findJob = null
        annotationJob = null
        activeEngine = null
        viewModelScope.launch {
            jobsToStop.joinAll()
            if (snapshot.document != null) persist(snapshot)
            engine?.let { withContext(Dispatchers.IO) { it.close() } }
            if (request == session) mutableState.update { it.copy(engine = null, isLoading = false) }
            onLeft()
        }
    }

    private fun schedulePersist(immediate: Boolean = false) {
        progressJob?.cancel()
        val request = session
        progressJob = viewModelScope.launch {
            if (!immediate) delay(SAVE_DEBOUNCE_MILLIS)
            if (request == session) persist(mutableState.value)
        }
    }

    private suspend fun persist(snapshot: ReaderContentState) {
        val documentId = snapshot.document?.id ?: return
        tabs.updateSnapshot(
            ReaderTab(
                docId = documentId,
                page = snapshot.pageIndex.coerceIn(0, (snapshot.pageCount - 1).coerceAtLeast(0)),
                zoom = snapshot.zoom.coerceIn(LeafSettings.ZOOM_MIN, LeafSettings.ZOOM_MAX),
                scrollDir = snapshot.scrollDir,
                findQuery = snapshot.findQuery.takeIf { snapshot.findBarVisible && it.isNotBlank() },
                scrollFraction = snapshot.scrollFraction.coerceIn(0f, 1f),
                readingTheme = snapshot.readingTheme,
                panX = snapshot.panX,
                panY = snapshot.panY
            )
        )
        if (!settings.current().rememberPage) return
        repository.saveProgress(
            Progress(
                docId = documentId,
                page = snapshot.pageIndex.coerceIn(0, (snapshot.pageCount - 1).coerceAtLeast(0)),
                scrollFraction = snapshot.scrollFraction.coerceIn(0f, 1f),
                zoom = snapshot.zoom.coerceIn(LeafSettings.ZOOM_MIN, LeafSettings.ZOOM_MAX),
                scrollDir = snapshot.scrollDir,
                updatedAt = clock.nowMillis(),
                readingTheme = snapshot.readingTheme,
                panX = snapshot.panX,
                panY = snapshot.panY
            )
        )
    }

    override fun onCleared() {
        progressJob?.cancel()
        extractionJob?.cancel()
        findJob?.cancel()
        annotationJob?.cancel()
        activeEngine?.close()
        activeEngine = null
        super.onCleared()
    }

    companion object {
        const val MAX_PAGES = 2_000
        private const val SAVE_DEBOUNCE_MILLIS = 350L
        private const val FIND_DEBOUNCE_MILLIS = 250L
        private const val ZOOM_STEP = 0.1f
        private const val HIT_TEST_INSET = 0.012f
    }
}

private fun List<NormalizedRect>.centerX(): Float = if (isEmpty()) 0f else (minOf { it.left } + maxOf { it.right }) / 2f
private fun List<NormalizedRect>.centerY(): Float = if (isEmpty()) 0f else (minOf { it.top } + maxOf { it.bottom }) / 2f

private fun List<NormalizedRect>.distanceSquared(x: Float, y: Float): Float {
    val dx = when {
        x < minOf { it.left } -> minOf { it.left } - x
        x > maxOf { it.right } -> x - maxOf { it.right }
        else -> 0f
    }
    val dy = when {
        y < minOf { it.top } -> minOf { it.top } - y
        y > maxOf { it.bottom } -> y - maxOf { it.bottom }
        else -> 0f
    }
    return dx * dx + dy * dy
}
