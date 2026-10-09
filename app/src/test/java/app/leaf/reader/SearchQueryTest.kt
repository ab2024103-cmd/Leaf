package app.leaf.reader

import app.leaf.reader.core.domain.SearchAddedWithin
import app.leaf.reader.core.domain.SearchFilters
import app.leaf.reader.core.domain.SearchScope
import app.leaf.reader.core.domain.SearchableDocument
import app.leaf.reader.core.domain.countOccurrences
import app.leaf.reader.core.domain.pushSearchHistory
import app.leaf.reader.core.domain.removeSearchHistoryItem
import app.leaf.reader.core.domain.searchLibrary
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.Orientation
import app.leaf.reader.core.model.SortField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryTest {

    @Test
    fun name_tag_folder_and_everything_scopes_match_only_their_fields() {
        val records = searchableDocuments()
        val folders = folders()

        val byName = searchLibrary(records, folders, "quarterly", SearchScope.NAME, SearchFilters(), NOW)
        assertEquals(listOf("d1"), byName.documents.map { it.id })
        assertTrue(byName.files.isEmpty())

        val byTag = searchLibrary(records, folders, "IMPORTANT", SearchScope.TAG, SearchFilters(), NOW)
        assertEquals(listOf("d1", "d3"), byTag.documents.map { it.id }.sorted())
        assertTrue(byTag.files.isEmpty())

        val byFolder = searchLibrary(records, folders, "work", SearchScope.FOLDER, SearchFilters(), NOW)
        assertEquals(listOf("d1", "d2"), byFolder.documents.map { it.id }.sorted())
        assertTrue(byFolder.files.isEmpty())

        val everything = searchLibrary(records, folders, "Q3", SearchScope.EVERYTHING, SearchFilters(), NOW)
        assertEquals(listOf("d3"), everything.documents.map { it.id })
        assertEquals(listOf("d1", "d2"), everything.files.map { it.document.id }.sorted())
        assertEquals(5, everything.totalFileHits)
    }

    @Test
    fun inside_files_counts_every_occurrence_and_returns_the_first_page_excerpt() {
        val results = searchLibrary(
            searchable = searchableDocuments(),
            folders = folders(),
            query = " q3 ",
            scope = SearchScope.INSIDE_FILES,
            filters = SearchFilters(),
            now = NOW
        )

        assertTrue(results.documents.isEmpty())
        assertEquals(2, results.fileCount)
        assertEquals(5, results.totalFileHits)
        val first = results.files.first { it.document.id == "d1" }
        assertEquals(3, first.matchCount)
        assertEquals(0, first.firstPageIndex)
        assertEquals("Q3", first.excerpt.text.substring(first.excerpt.matchStart, first.excerpt.matchEnd))
    }

    @Test
    fun type_folder_tag_and_age_filters_compose_with_and_semantics() {
        val results = searchLibrary(
            searchable = searchableDocuments(),
            folders = folders(),
            query = "",
            scope = SearchScope.EVERYTHING,
            filters = SearchFilters(
                type = DocType.PDF,
                folderId = "work",
                tag = "important",
                addedWithin = SearchAddedWithin.MONTH
            ),
            now = NOW
        )

        assertEquals(listOf("d1"), results.documents.map { it.id })
        assertTrue(results.files.isEmpty())
    }

    @Test
    fun filter_folder_includes_documents_in_nested_subfolders() {
        val results = searchLibrary(
            searchable = searchableDocuments(),
            folders = folders(),
            query = "",
            scope = SearchScope.EVERYTHING,
            filters = SearchFilters(folderId = "work"),
            now = NOW
        )

        assertEquals(listOf("d1", "d2"), results.documents.map { it.id }.sorted())
    }

    @Test
    fun literal_match_count_is_case_insensitive_and_non_overlapping() {
        assertEquals(3, countOccurrences("Q3 q3 q3", "q3"))
        assertEquals(0, countOccurrences("anything", "  "))
        assertFalse(countOccurrences("banana", "ana") > 1)
    }

    @Test
    fun search_history_moves_duplicates_to_front_and_caps_at_ten() {
        val existing = (1..10).map { "query $it" }
        val updated = pushSearchHistory(existing, " QUERY 5 ")

        assertEquals("QUERY 5", updated.first())
        assertEquals(10, updated.size)
        assertEquals(1, updated.count { it.equals("query 5", ignoreCase = true) })
        assertEquals(listOf("next", "first"), pushSearchHistory(listOf("first"), "next"))
        assertEquals(listOf("first"), removeSearchHistoryItem(listOf("first", "second"), "SECOND"))
    }

    private fun searchableDocuments() = listOf(
        SearchableDocument(
            document = document(
                id = "d1", name = "Quarterly Notes.pdf", type = DocType.PDF,
                dateAdded = NOW - 2 * DAY, folderId = "work", tags = listOf("important", "personal")
            ),
            pages = listOf("The Q3 plan is near.", "Q3 and Q3 again.")
        ),
        SearchableDocument(
            document = document(
                id = "d2", name = "Budget.pdf", type = DocType.PDF,
                dateAdded = NOW - 14 * DAY, folderId = "reports", tags = listOf("work")
            ),
            pages = listOf("Q3 budget Q3")
        ),
        SearchableDocument(
            document = document(
                id = "d3", name = "Q3 Outline.docx", type = DocType.DOCX,
                dateAdded = NOW - 2 * DAY, folderId = null, tags = listOf("important")
            ),
            pages = emptyList()
        )
    )

    private fun folders() = listOf(
        Folder("work", "Work", "#2B7A5B", "folder", null, false),
        Folder("reports", "Reports", "#3D6373", "folder", "work", false)
    )

    private fun document(
        id: String,
        name: String,
        type: DocType,
        dateAdded: Long,
        folderId: String?,
        tags: List<String>
    ) = Document(
        id = id,
        name = name,
        type = type,
        sizeBytes = 1_024L,
        uri = "file:///$id",
        pageCount = 2,
        dateAdded = dateAdded,
        lastOpened = dateAdded,
        favorite = false,
        favoritedAt = null,
        folderId = folderId,
        orientation = Orientation.AUTO,
        tags = tags
    )

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
