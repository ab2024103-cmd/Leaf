package app.leaf.reader

import android.app.Application
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.data.db.LeafDatabase
import app.leaf.reader.core.data.repo.ReaderRepository
import app.leaf.reader.core.model.Progress
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ReaderRepositoryTest {
    private lateinit var db: LeafDatabase
    private lateinit var repository: ReaderRepository

    @Before
    fun setUp() {
        db = testDatabase()
        repository = ReaderRepository(
            documents = db.documentDao(),
            documentTags = db.documentTagDao(),
            positions = db.progressDao(),
            recents = db.recentDao(),
            extractedText = db.extractedTextDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun page_stay_round_trips_position_zoom_direction_theme_and_pan() = runTest {
        DatabaseSeeder(db).seed(FIXED_NOW)
        val progress = Progress(
            docId = "d1",
            page = 4,
            scrollFraction = 0.375f,
            zoom = 1.65f,
            scrollDir = ScrollDir.HORIZONTAL,
            updatedAt = FIXED_NOW,
            readingTheme = ReadingTheme.SEPIA,
            panX = 0.12f,
            panY = -0.08f
        )

        repository.saveProgress(progress)

        assertEquals(progress, repository.progress("d1"))
        assertEquals(progress, repository.observeProgress("d1").first())
    }

    @Test
    fun incoming_pdf_registration_is_idempotent_and_adds_recent_history() = runTest {
        DatabaseSeeder(db).seed(FIXED_NOW)
        val uri = "content://provider.example/shared/report.pdf"

        val firstId = repository.registerPdf(uri, "report.pdf", 12_345L, 9, FIXED_NOW + 1)
        val secondId = repository.registerPdf(uri, "changed-name.pdf", 12_345L, 9, FIXED_NOW + 2)

        assertEquals(firstId, secondId)
        assertEquals("report.pdf", repository.document(firstId)?.name)
        assertEquals(uri, repository.document(firstId)?.uri)
        assertEquals(9, repository.document(firstId)?.pageCount)
        assertNotNull(db.recentDao().get(firstId))
        assertEquals(FIXED_NOW + 2, db.documentDao().get(firstId)?.lastOpened)
        assertNull(repository.progress(firstId))
    }
}
