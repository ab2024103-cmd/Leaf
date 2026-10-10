package app.leaf.reader

import android.app.Application
import android.graphics.Bitmap
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.data.db.LeafDatabase
import app.leaf.reader.core.data.prefs.ReaderTabsStore
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.data.repo.AnnotationRepository
import app.leaf.reader.core.data.repo.ReaderRepository
import app.leaf.reader.core.format.DocumentEngine
import app.leaf.reader.core.format.DocumentEngineFactory
import app.leaf.reader.core.format.MatchRect
import app.leaf.reader.core.format.PageMetrics
import app.leaf.reader.core.format.RenderTile
import app.leaf.reader.core.format.TextRun
import app.leaf.reader.core.model.Bookmark
import app.leaf.reader.core.model.Highlight
import app.leaf.reader.core.model.HighlightColor
import app.leaf.reader.core.model.NormalizedRect
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.util.LeafClock
import app.leaf.reader.feature.reader.ReaderSelection
import app.leaf.reader.feature.reader.ReaderViewModel
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    private lateinit var application: Application
    private lateinit var database: LeafDatabase
    private lateinit var databaseName: String
    private lateinit var repository: ReaderRepository
    private lateinit var annotationRepository: AnnotationRepository
    private lateinit var tabsStore: ReaderTabsStore
    private lateinit var settingsScope: CoroutineScope
    private lateinit var settings: SettingsStore
    private lateinit var settingsFile: File

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        databaseName = "reader-view-model-${System.nanoTime()}.db"
        database = Room.databaseBuilder(application, LeafDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        repository = readerRepository(database)
        annotationRepository = AnnotationRepository(database.bookmarkDao(), database.highlightDao())
        settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        settingsFile = File(application.cacheDir, "reader-view-model-test.preferences_pb")
        settingsFile.delete()
        val dataStore = PreferenceDataStoreFactory.create(scope = settingsScope) { settingsFile }
        settings = SettingsStore(dataStore)
        tabsStore = ReaderTabsStore(dataStore)
    }

    @After
    fun tearDown() {
        database.close()
        application.deleteDatabase(databaseName)
        settingsScope.cancel()
        settingsFile.delete()
    }

    // Room and Preferences DataStore use real I/O; runBlocking keeps the timeout wall-clock based.
    @Test
    fun saved_reader_state_is_restored_by_a_fresh_viewmodel() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            DatabaseSeeder(database).seed(FIXED_NOW)
            val factory = DocumentEngineFactory { TestReaderEngine() }
            val clock = LeafClock { FIXED_NOW + 1 }
            val firstViewModel = ReaderViewModel(application, repository, annotationRepository, tabsStore, settings, factory, clock)

            firstViewModel.openDocument("d1")
            firstViewModel.state.first { it.document?.id == "d1" }
            firstViewModel.setZoom(1.7f)
            firstViewModel.updatePan(0.12f, -0.08f)
            firstViewModel.setScrollDirection(ScrollDir.HORIZONTAL)
            firstViewModel.setReadingTheme(ReadingTheme.SEPIA)
            firstViewModel.updatePosition(pageIndex = 4, scrollFraction = 0.375f)

            val firstClosed = CompletableDeferred<Unit>()
            firstViewModel.leaveReader { firstClosed.complete(Unit) }
            withTimeout(10_000) { firstClosed.await() }

            // Reopen the on-disk database as a new process would, then construct a fresh ViewModel.
            database.close()
            database = Room.databaseBuilder(application, LeafDatabase::class.java, databaseName)
                .allowMainThreadQueries()
                .build()
            repository = readerRepository(database)
            val reopenedViewModel = ReaderViewModel(application, repository, annotationRepository, tabsStore, settings, factory, clock)
            reopenedViewModel.openDocument("d1")
            val restored = withTimeout(10_000) { reopenedViewModel.state.first { it.document?.id == "d1" } }

            assertEquals(4, restored.pageIndex)
            assertEquals(0.375f, restored.scrollFraction)
            assertEquals(1.7f, restored.zoom)
            assertEquals(ScrollDir.HORIZONTAL, restored.scrollDir)
            assertEquals(ReadingTheme.SEPIA, restored.readingTheme)
            assertEquals(0.12f, restored.panX)
            assertEquals(-0.08f, restored.panY)

            val secondClosed = CompletableDeferred<Unit>()
            reopenedViewModel.leaveReader { secondClosed.complete(Unit) }
            withTimeout(10_000) { secondClosed.await() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun annotations_and_reader_position_survive_theme_zoom_changes_and_database_reopen() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            DatabaseSeeder(database).seed(FIXED_NOW)
            val bookmark = Bookmark("bookmark-persist", "d1", 3, "Page 4", FIXED_NOW)
            val highlight = Highlight(
                id = "highlight-persist",
                docId = "d1",
                page = 3,
                color = HighlightColor.ORANGE,
                text = "Keep this highlighted text",
                bounds = listOf(NormalizedRect(0.12f, 0.20f, 0.76f, 0.24f)),
                textRange = null,
                cfiRange = null,
                createdAt = FIXED_NOW
            )
            annotationRepository.insert(bookmark)
            annotationRepository.insert(highlight)

            val firstViewModel = ReaderViewModel(
                application,
                repository,
                annotationRepository,
                tabsStore,
                settings,
                DocumentEngineFactory { TestReaderEngine() },
                LeafClock { FIXED_NOW + 1 }
            )
            firstViewModel.openDocument("d1")
            firstViewModel.state.first { it.document?.id == "d1" }
            firstViewModel.setZoom(1.8f)
            firstViewModel.setReadingTheme(ReadingTheme.NIGHT)
            firstViewModel.updatePosition(pageIndex = 3, scrollFraction = 0.3f)
            val firstClosed = CompletableDeferred<Unit>()
            firstViewModel.leaveReader { firstClosed.complete(Unit) }
            withTimeout(10_000) { firstClosed.await() }

            database.close()
            database = Room.databaseBuilder(application, LeafDatabase::class.java, databaseName)
                .allowMainThreadQueries()
                .build()
            repository = readerRepository(database)
            annotationRepository = AnnotationRepository(database.bookmarkDao(), database.highlightDao())
            val reopenedViewModel = ReaderViewModel(
                application,
                repository,
                annotationRepository,
                tabsStore,
                settings,
                DocumentEngineFactory { TestReaderEngine() },
                LeafClock { FIXED_NOW + 2 }
            )
            reopenedViewModel.openDocument("d1")
            val restored = withTimeout(10_000) {
                reopenedViewModel.state.first {
                    it.document?.id == "d1" && it.bookmarks.size == 1 && it.highlights.size == 1
                }
            }

            assertEquals(3, restored.pageIndex)
            assertEquals(0.3f, restored.scrollFraction)
            assertEquals(1.8f, restored.zoom)
            assertEquals(ReadingTheme.NIGHT, restored.readingTheme)
            assertEquals(bookmark, restored.bookmarks.single())
            assertEquals(highlight, restored.highlights.single())

            val reopenedClosed = CompletableDeferred<Unit>()
            reopenedViewModel.leaveReader { reopenedClosed.complete(Unit) }
            withTimeout(10_000) { reopenedClosed.await() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun reader_annotations_support_recolor_page_clear_full_clear_and_undo_redo() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            DatabaseSeeder(database).seed(FIXED_NOW)
            val viewModel = ReaderViewModel(
                application,
                repository,
                annotationRepository,
                tabsStore,
                settings,
                DocumentEngineFactory { TestReaderEngine() },
                LeafClock { FIXED_NOW }
            )
            viewModel.openDocument("d1")
            viewModel.state.first { it.document?.id == "d1" }

            val selectionBounds = listOf(NormalizedRect(0.2f, 0.3f, 0.52f, 0.34f))
            viewModel.addHighlight(
                ReaderSelection(0, "selected words", selectionBounds, startWord = 0, endWord = 1),
                HighlightColor.BLUE
            )
            val added = withTimeout(10_000) {
                viewModel.state.first { it.highlights.size == 1 }
            }
            assertEquals("selected words", added.highlights.single().text)
            assertEquals(HighlightColor.BLUE, added.highlights.single().color)
            assertEquals(selectionBounds, added.highlights.single().bounds)

            viewModel.recolorHighlight(added.highlights.single().id, HighlightColor.ORANGE)
            assertEquals(
                HighlightColor.ORANGE,
                withTimeout(10_000) { viewModel.state.first { it.highlights.singleOrNull()?.color == HighlightColor.ORANGE } }
                    .highlights.single().color
            )
            viewModel.undoAnnotation()
            assertEquals(
                HighlightColor.BLUE,
                withTimeout(10_000) { viewModel.state.first { it.highlights.singleOrNull()?.color == HighlightColor.BLUE } }
                    .highlights.single().color
            )
            viewModel.redoAnnotation()
            withTimeout(10_000) { viewModel.state.first { it.highlights.singleOrNull()?.color == HighlightColor.ORANGE } }

            viewModel.clearPageHighlights()
            assertTrue(withTimeout(10_000) { viewModel.state.first { it.highlights.isEmpty() } }.highlights.isEmpty())
            viewModel.undoAnnotation()
            assertEquals(
                HighlightColor.ORANGE,
                withTimeout(10_000) { viewModel.state.first { it.highlights.size == 1 } }.highlights.single().color
            )

            viewModel.clearAllHighlights()
            assertTrue(withTimeout(10_000) { viewModel.state.first { it.highlights.isEmpty() } }.highlights.isEmpty())
            viewModel.undoAnnotation()
            assertEquals(1, withTimeout(10_000) { viewModel.state.first { it.highlights.size == 1 } }.highlights.size)
            viewModel.redoAnnotation()
            assertTrue(withTimeout(10_000) { viewModel.state.first { it.highlights.isEmpty() } }.highlights.isEmpty())

            val closed = CompletableDeferred<Unit>()
            viewModel.leaveReader { closed.complete(Unit) }
            withTimeout(10_000) { closed.await() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun search_handoff_prefills_find_selects_first_hit_and_cycles_matches() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            DatabaseSeeder(database).seed(FIXED_NOW)
            val viewModel = ReaderViewModel(
                application,
                repository,
                annotationRepository,
                tabsStore,
                settings,
                DocumentEngineFactory { TestReaderEngine() },
                LeafClock { FIXED_NOW }
            )

            viewModel.openDocument("d1", initialPage = 4, initialFindQuery = "chapter")
            val firstHit = withTimeout(10_000) {
                viewModel.state.first { it.findMatches.isNotEmpty() }
            }

            assertEquals(true, firstHit.findBarVisible)
            assertEquals("chapter", firstHit.findQuery)
            assertEquals(0, firstHit.findIndex)
            assertEquals(0, firstHit.pageIndex)
            assertEquals(6, firstHit.findMatchCount)

            viewModel.nextFindMatch()
            val nextHit = viewModel.state.value
            assertEquals(1, nextHit.findIndex)
            assertEquals(1, nextHit.pageIndex)

            val closed = CompletableDeferred<Unit>()
            viewModel.leaveReader { closed.complete(Unit) }
            withTimeout(10_000) { closed.await() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun damaged_pdf_open_becomes_a_reader_error_state() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            DatabaseSeeder(database).seed(FIXED_NOW)
            val factory = DocumentEngineFactory { throw IOException("damaged PDF fixture") }
            val viewModel = ReaderViewModel(application, repository, annotationRepository, tabsStore, settings, factory, LeafClock { FIXED_NOW })

            viewModel.openDocument("d1")
            val failed = withTimeout(10_000) { viewModel.state.first { it.error != null } }

            assertEquals("damaged PDF fixture", failed.error)
            assertEquals(null, failed.engine)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun readerRepository(database: LeafDatabase) = ReaderRepository(
        documents = database.documentDao(),
        documentTags = database.documentTagDao(),
        positions = database.progressDao(),
        recents = database.recentDao(),
        extractedText = database.extractedTextDao()
    )

    private class TestReaderEngine : DocumentEngine {
        override val pageCount: Int = 6
        override suspend fun pageMetrics(pageIndex: Int) = PageMetrics(612, 792)
        override suspend fun renderPreview(pageIndex: Int, maxEdgePixels: Int): Bitmap = error("Not rendered in this test")
        override suspend fun renderTile(pageIndex: Int, scale: Float, tile: RenderTile): Bitmap = error("Not rendered in this test")
        override suspend fun pageText(pageIndex: Int): List<TextRun> = emptyList()
        override suspend fun findInPage(pageIndex: Int, query: String): List<MatchRect> = emptyList()
        override suspend fun extractAllText(onProgress: suspend (Int, Int) -> Unit): List<List<TextRun>> = emptyList()
        override fun close() = Unit
    }
}
