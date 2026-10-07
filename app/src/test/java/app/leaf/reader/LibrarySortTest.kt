package app.leaf.reader

import app.leaf.reader.core.domain.LibraryDoc
import app.leaf.reader.core.domain.LibrarySort
import app.leaf.reader.core.model.SortField
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * §12: sort comparators, every field in both directions, plus the name tie-break.
 * "Ascending" is the direction the sort chip names first for the field (§6.1):
 * A–Z · Newest · Recent · Largest.
 */
class LibrarySortTest {

    private val library = listOf(
        doc("d1", name = "Banana.pdf", sizeMb = 3.0, addedDaysAgo = 10, openedDaysAgo = 1),
        doc("d2", name = "apple.pdf", sizeMb = 1.0, addedDaysAgo = 2, openedDaysAgo = 9),
        doc("d3", name = "Cherry.pdf", sizeMb = 2.0, addedDaysAgo = 5, openedDaysAgo = 5),
        // Same size as d3: the tie must fall back to the name, not to insertion order.
        doc("d4", name = "Apricot.pdf", sizeMb = 2.0, addedDaysAgo = 1, openedDaysAgo = 2)
    )

    private fun sorted(field: SortField, ascending: Boolean): List<String> =
        LibrarySort.sorted(library, field, ascending).map { it.id }

    @Test
    fun name_sorting_is_case_insensitive_and_ties_break_on_the_name() {
        assertEquals(listOf("d2", "d4", "d1", "d3"), sorted(SortField.NAME, ascending = true))
        assertEquals(listOf("d3", "d1", "d4", "d2"), sorted(SortField.NAME, ascending = false))
    }

    @Test
    fun date_added_ascending_puts_the_newest_first() {
        assertEquals(listOf("d4", "d2", "d3", "d1"), sorted(SortField.DATE_ADDED, ascending = true))
        assertEquals(listOf("d1", "d3", "d2", "d4"), sorted(SortField.DATE_ADDED, ascending = false))
    }

    @Test
    fun last_opened_ascending_puts_the_most_recent_first() {
        assertEquals(listOf("d1", "d4", "d3", "d2"), sorted(SortField.LAST_OPENED, ascending = true))
        assertEquals(listOf("d2", "d3", "d4", "d1"), sorted(SortField.LAST_OPENED, ascending = false))
    }

    @Test
    fun file_size_ascending_puts_the_largest_first() {
        assertEquals(listOf("d1", "d4", "d3", "d2"), sorted(SortField.FILE_SIZE, ascending = true))
        assertEquals(listOf("d2", "d4", "d3", "d1"), sorted(SortField.FILE_SIZE, ascending = false))
    }

    @Test
    fun a_document_that_was_never_opened_sorts_as_the_oldest() {
        val mixed = listOf(
            doc("opened", openedDaysAgo = 1),
            doc("never", openedDaysAgo = null)
        )
        // No timestamp at all reads as the oldest there is, so it trails "Recent" and
        // leads "Oldest" — never disappearing from either end of the list.
        assertEquals(
            listOf("opened", "never"),
            LibrarySort.sorted(mixed, SortField.LAST_OPENED, ascending = true).map { it.id }
        )
        assertEquals(
            listOf("never", "opened"),
            LibrarySort.sorted(mixed, SortField.LAST_OPENED, ascending = false).map { it.id }
        )
    }

    @Test
    fun descending_reverses_the_field_but_not_the_tie_break() {
        val ties = listOf(
            doc("z", name = "Same.pdf", sizeMb = 1.0),
            doc("a", name = "same.pdf", sizeMb = 1.0)
        )
        // The primary key is tied, so the name breaks it — identically in both directions.
        assertEquals(listOf("z", "a"), LibrarySort.sorted(ties, SortField.FILE_SIZE, true).map { it.id })
        assertEquals(listOf("z", "a"), LibrarySort.sorted(ties, SortField.FILE_SIZE, false).map { it.id })
    }

    @Test
    fun sorting_does_not_mutate_the_input() {
        val input: List<LibraryDoc> = library
        LibrarySort.sorted(input, SortField.NAME, true)
        assertEquals(listOf("d1", "d2", "d3", "d4"), input.map { it.id })
    }
}
