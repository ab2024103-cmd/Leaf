package app.leaf.reader

import android.app.Application

import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.model.DocType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The demo library: 10 documents (including the .xlsx and the .pptx), 5 folders,
 * 7 tags, 6 smart collections, recents, highlights and bookmarks (§3).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SeedDataTest {

    private lateinit var db: app.leaf.reader.core.data.db.LeafDatabase

    /** A fixed "now" so the relative timestamps in the seed are deterministic. */
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        db = testDatabase()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seed_populates_the_whole_demo_library() = runTest {
        DatabaseSeeder(db).seed(now)

        assertEquals(10, db.documentDao().count())
        assertEquals(5, db.folderDao().count())
        assertEquals(7, db.tagDao().count())
        assertEquals(6, db.smartCollectionDao().count())
        assertEquals(40, db.extractedTextDao().count())
        assertEquals(4, db.highlightDao().count())
        assertEquals(15, db.documentTagDao().getAll().size)
        assertEquals(3, db.documentDao().getAll().count { it.favorite })
    }

    @Test
    fun seed_includes_the_spreadsheet_and_the_deck() = runTest {
        DatabaseSeeder(db).seed(now)

        val budget = db.documentDao().getAll().first { it.id == "d9" }
        assertEquals(DocType.XLSX, budget.type)
        assertEquals("f-rep", budget.folderId)
        assertEquals(3, budget.pageCount)

        val deck = db.documentDao().getAll().first { it.id == "d10" }
        assertEquals(DocType.PPTX, deck.type)
        assertEquals("f-work", deck.folderId)
        assertEquals(3, deck.pageCount)
    }

    @Test
    fun seed_resolves_highlight_offsets_against_the_page_text() = runTest {
        DatabaseSeeder(db).seed(now)

        val highlights = db.highlightDao().observeAll().first()
        assertEquals(4, highlights.size)
        highlights.forEach { highlight ->
            assertNotNull("expected a text range for '${highlight.text}'", highlight.rangeStart)
            assertNotNull(highlight.rangeEnd)
            val pageText = db.extractedTextDao()
                .pagesForDocument(highlight.docId)
                .first { it.pageIndex == highlight.page }
                .text
            val sliced = pageText.substring(highlight.rangeStart!!, highlight.rangeEnd!!)
            assertEquals(highlight.text, sliced)
        }
    }

    @Test
    fun seed_stores_one_recent_entry_per_document_newest_first() = runTest {
        DatabaseSeeder(db).seed(now)

        val recents = db.recentDao().observeAll().first()
        assertEquals(10, recents.size)
        assertEquals(recents.map { it.docId }.toSet().size, 10)
        // d5 was opened 45 minutes ago — the most recent document in the seed.
        assertEquals("d5", recents.first().docId)
        assertTrue(recents.zipWithNext().all { (a, b) -> a.openedAt >= b.openedAt })
    }

    @Test
    fun seed_writes_a_reading_position_for_every_document() = runTest {
        DatabaseSeeder(db).seed(now)

        val progress = db.progressDao().getAll()
        assertEquals(10, progress.size)
        assertTrue(progress.all { it.zoom == 1f })
    }

    @Test
    fun seeding_twice_does_not_duplicate_the_library() = runTest {
        val seeder = DatabaseSeeder(db)
        assertTrue(seeder.seedIfEmpty(now))
        assertEquals(false, seeder.seedIfEmpty(now))

        assertEquals(10, db.documentDao().count())
    }
}
