package app.leaf.reader

import android.app.Application
import android.graphics.Bitmap
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.data.db.LeafDatabase
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.data.repo.ReaderRepository
import app.leaf.reader.core.format.DocumentEngine
import app.leaf.reader.core.format.DocumentEngineFactory
import app.leaf.reader.core.format.MatchRect
import app.leaf.reader.core.format.PageMetrics
import app.leaf.reader.core.format.RenderTile
import app.leaf.reader.core.format.TextRun
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.util.LeafClock
import app.leaf.reader.feature.reader.ReaderViewModel
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ReaderViewModelTest {
    private lateinit var application: Application
    private lateinit var database: LeafDatabase
    private lateinit var databaseName: String
    private lateinit var repository: ReaderRepository
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
        settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        settingsFile = File(application.cacheDir, "reader-view-model-test.preferences_pb")
        settingsFile.delete()
        val dataStore = PreferenceDataStoreFactory.create(scope = settingsScope) { settingsFile }
        settings = SettingsStore(dataStore)
    }

    @After
    fun tearDown() {
        database.close()
        application.deleteDatabase(databaseName)
        settingsScope.cancel()
        settingsFile.delete()
    }

    @Test
    fun saved_reader_state_is_restored_by_a_fresh_viewmodel() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            DatabaseSeeder(database).seed(FIXED_NOW)
            val factory = DocumentEngineFactory { TestReaderEngine() }
            val clock = LeafClock { FIXED_NOW + 1 }
            val firstViewModel = ReaderViewModel(application, repository, settings, factory, clock)

            firstViewModel.openDocument("d1")
            firstViewModel.state.first { it.document?.id == "d1" }
            firstViewModel.setZoom(1.7f)
            firstViewModel.updatePan(0.12f, -0.08f)
            firstViewModel.setScrollDirection(ScrollDir.HORIZONTAL)
            firstViewModel.setReadingTheme(ReadingTheme.SEPIA)
            firstViewModel.updatePosition(pageIndex = 4, scrollFraction = 0.375f)

            val firstClosed = CompletableDeferred<Unit>()
            firstViewModel.closeReader { firstClosed.complete(Unit) }
            withTimeout(10_000) { firstClosed.await() }

            // Reopen the on-disk database as a new process would, then construct a fresh ViewModel.
            database.close()
            database = Room.databaseBuilder(application, LeafDatabase::class.java, databaseName)
                .allowMainThreadQueries()
                .build()
            repository = readerRepository(database)
            val reopenedViewModel = ReaderViewModel(application, repository, settings, factory, clock)
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
            reopenedViewModel.closeReader { secondClosed.complete(Unit) }
            withTimeout(10_000) { secondClosed.await() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun damaged_pdf_open_becomes_a_reader_error_state() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            DatabaseSeeder(database).seed(FIXED_NOW)
            val factory = DocumentEngineFactory { throw IOException("damaged PDF fixture") }
            val viewModel = ReaderViewModel(application, repository, settings, factory, LeafClock { FIXED_NOW })

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
