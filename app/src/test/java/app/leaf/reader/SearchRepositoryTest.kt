package app.leaf.reader

import android.app.Application
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.data.db.LeafDatabase
import app.leaf.reader.core.data.repo.SearchRepository
import app.leaf.reader.core.domain.SearchAddedWithin
import app.leaf.reader.core.domain.SearchFilters
import app.leaf.reader.core.domain.SearchScope
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.SortField
import kotlinx.coroutines.test.runTest
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
class SearchRepositoryTest {
    private lateinit var database: LeafDatabase
    private lateinit var repository: SearchRepository

    @Before
    fun setUp() {
        database = testDatabase()
        repository = SearchRepository(
            documents = database.documentDao(),
            documentTags = database.documentTagDao(),
            folders = database.folderDao(),
            tags = database.tagDao(),
            extractedText = database.extractedTextDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun inside_file_search_uses_seeded_room_text_and_returns_exact_page_and_count() = runTest {
        DatabaseSeeder(database).seed(FIXED_NOW)

        val results = repository.search(
            query = "Simplicity",
            scope = SearchScope.INSIDE_FILES,
            filters = SearchFilters(),
            now = FIXED_NOW,
            sortField = SortField.NAME,
            sortAscending = true
        )

        assertTrue(results.documents.isEmpty())
        assertEquals(listOf("d1"), results.files.map { it.document.id })
        assertEquals(1, results.totalFileHits)
        assertEquals(1, results.files.single().matchCount)
        assertEquals(0, results.files.single().firstPageIndex)
        val excerpt = results.files.single().excerpt
        assertEquals("Simplicity", excerpt.text.substring(excerpt.matchStart, excerpt.matchEnd))
    }

    @Test
    fun combined_filters_apply_to_full_text_results_including_nested_folder_and_age() = runTest {
        DatabaseSeeder(database).seed(FIXED_NOW)

        val results = repository.search(
            query = "Q3",
            scope = SearchScope.INSIDE_FILES,
            filters = SearchFilters(
                type = DocType.PDF,
                folderId = "f-work",
                tag = "important",
                addedWithin = SearchAddedWithin.WEEK
            ),
            now = FIXED_NOW,
            sortField = SortField.NAME,
            sortAscending = true
        )

        assertEquals(listOf("d2"), results.files.map { it.document.id })
        assertTrue(results.files.all { it.document.type == DocType.PDF })
        assertTrue(results.totalFileHits >= 2)
    }

    @Test
    fun non_pdf_seed_text_is_not_offered_as_an_unopenable_reader_result() = runTest {
        DatabaseSeeder(database).seed(FIXED_NOW)

        val results = repository.search(
            query = "chapter",
            scope = SearchScope.INSIDE_FILES,
            filters = SearchFilters(),
            now = FIXED_NOW,
            sortField = SortField.NAME,
            sortAscending = true
        )

        assertTrue(results.files.isNotEmpty())
        assertTrue(results.files.all { it.document.type == DocType.PDF })
        assertTrue(results.files.any { it.document.id == "d1" })
    }

    private companion object {
        const val FIXED_NOW = 1_700_000_000_000L
    }
}
