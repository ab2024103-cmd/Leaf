package app.leaf.reader.feature.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.data.repo.ReaderRepository
import app.leaf.reader.core.format.DocumentEngine
import app.leaf.reader.core.format.DocumentEngineFactory
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Progress
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.util.LeafClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Reader orchestration: PdfRenderer lifecycle, recent history and debounced Page Stay. */
class ReaderViewModel(
    application: Application,
    private val repository: ReaderRepository,
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
    private var session = 0

    fun openDocument(documentId: String) {
        if (documentId == activeDocumentId && activeEngine != null) return
        val request = ++session
        val previous = mutableState.value
        if (previous.document != null) viewModelScope.launch { persist(previous) }
        progressJob?.cancel()
        extractionJob?.cancel()
        val previousEngine = activeEngine
        activeEngine = null
        activeDocumentId = documentId
        mutableState.value = ReaderContentState(isLoading = true)

        viewModelScope.launch {
            try {
                previousEngine?.let { withContext(Dispatchers.IO) { it.close() } }
                val document = repository.document(documentId)
                    ?: throw IllegalStateException("This document is no longer in the library")
                if (document.type != DocType.PDF) {
                    throw UnsupportedOperationException("Only PDF reading is included in M3")
                }
                val engine = engines.open(document.uri)
                if (request != session) {
                    engine.close()
                    return@launch
                }
                if (engine.pageCount <= 0) {
                    engine.close()
                    throw IllegalStateException("The PDF contains no readable pages")
                }
                activeEngine = engine
                val prefs = settings.current()
                val stored = repository.progress(documentId)
                val pageCount = engine.pageCount
                if (document.pageCount != pageCount) repository.updatePageCount(documentId, pageCount)
                val page = if (prefs.rememberPage && stored != null) {
                    stored.page.coerceIn(0, pageCount - 1)
                } else {
                    0
                }
                val initial = ReaderContentState(
                    document = document.copy(pageCount = pageCount),
                    engine = engine,
                    pageCount = pageCount,
                    pageIndex = page,
                    scrollFraction = if (prefs.rememberPage) stored?.scrollFraction?.coerceIn(0f, 1f) ?: 0f else 0f,
                    zoom = (if (prefs.rememberPage) stored?.zoom ?: prefs.zoom else prefs.zoom)
                        .coerceIn(LeafSettings.ZOOM_MIN, LeafSettings.ZOOM_MAX),
                    scrollDir = if (prefs.rememberPage) stored?.scrollDir ?: prefs.scrollDir else prefs.scrollDir,
                    readingTheme = if (prefs.rememberPage) stored?.readingTheme ?: prefs.readingTheme else prefs.readingTheme,
                    panX = if (prefs.rememberPage) stored?.panX ?: 0f else 0f,
                    panY = if (prefs.rememberPage) stored?.panY ?: 0f else 0f,
                    warning = if (pageCount > MAX_PAGES) "This PDF has over 2,000 pages; only visible pages are rendered." else null
                )
                mutableState.value = initial
                repository.recordOpen(documentId, clock.nowMillis())
                restorePosition(initial)
                extractTextIfNeeded(documentId, engine, pageCount, request)
            } catch (error: Exception) {
                if (request == session) {
                    activeEngine?.let { withContext(Dispatchers.IO) { it.close() } }
                    activeEngine = null
                    mutableState.value = ReaderContentState(
                        error = error.message?.takeIf(String::isNotBlank)
                            ?: "This page could not be rendered"
                    )
                }
            }
        }
    }

    private suspend fun restorePosition(initial: ReaderContentState) {
        val stored = repository.progress(initial.document?.id ?: return)
        if (stored == null) persist(initial)
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
            } catch (_: Exception) {
                // Raster reading remains available for scanned, encrypted, or text-incompatible PDFs.
                if (request == session) mutableState.update { it.copy(textExtractionProgress = null) }
            }
        }
    }

    fun updatePosition(pageIndex: Int, scrollFraction: Float) {
        val current = mutableState.value
        if (current.document == null || current.pageCount <= 0) return
        mutableState.update {
            it.copy(
                pageIndex = pageIndex.coerceIn(0, current.pageCount - 1),
                scrollFraction = scrollFraction.coerceIn(0f, 1f)
            )
        }
        schedulePersist()
    }

    fun jumpToPage(pageIndex: Int) {
        val current = mutableState.value
        if (current.pageCount <= 0) return
        mutableState.update { it.copy(pageIndex = pageIndex.coerceIn(0, current.pageCount - 1), scrollFraction = 0f) }
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
        val next = (current + direction * ZOOM_STEP).coerceIn(LeafSettings.ZOOM_MIN, LeafSettings.ZOOM_MAX)
        setZoom(next)
    }

    fun setScrollDirection(direction: ScrollDir) {
        mutableState.update { it.copy(scrollDir = direction, scrollFraction = 0f) }
        schedulePersist(immediate = true)
    }

    fun setReadingTheme(theme: ReadingTheme) {
        mutableState.update { it.copy(readingTheme = theme) }
        schedulePersist(immediate = true)
    }

    /** Pan fractions are relative to the fit page, so Page Stay survives screen-density changes. */
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

    fun setSheet(sheet: ReaderSheet) = mutableState.update { it.copy(sheet = sheet) }

    fun toggleFullScreen() = mutableState.update { it.copy(isFullScreen = !it.isFullScreen) }

    fun closeReader(onClosed: () -> Unit) {
        // Invalidate an in-flight open so Back during loading cannot resurrect the reader.
        val request = ++session
        val snapshot = mutableState.value
        progressJob?.cancel()
        extractionJob?.cancel()
        viewModelScope.launch {
            if (snapshot.document != null && settings.current().rememberPage) persist(snapshot)
            if (request == session) {
                activeEngine?.let { withContext(Dispatchers.IO) { it.close() } }
                activeEngine = null
                activeDocumentId = null
                mutableState.value = ReaderContentState()
            }
            onClosed()
        }
    }

    private fun schedulePersist(immediate: Boolean = false) {
        progressJob?.cancel()
        val request = session
        progressJob = viewModelScope.launch {
            if (!immediate) delay(SAVE_DEBOUNCE_MS)
            if (request == session) persist(mutableState.value)
        }
    }

    private suspend fun persist(snapshot: ReaderContentState) {
        val documentId = snapshot.document?.id ?: return
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
        activeEngine?.close()
        activeEngine = null
        super.onCleared()
    }

    companion object {
        const val MAX_PAGES = 2_000
        private const val SAVE_DEBOUNCE_MS = 350L
        private const val ZOOM_STEP = 0.1f
    }
}
