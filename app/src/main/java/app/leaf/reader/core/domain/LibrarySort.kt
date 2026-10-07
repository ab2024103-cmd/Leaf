package app.leaf.reader.core.domain

import app.leaf.reader.core.model.SortField

/**
 * Library sorting (§6.1, §14 row 9). Four fields, two directions; **file type is not a
 * sort field** — it is its own grouped view (§8.13).
 *
 * Ties always fall back to the name, ascending, so every list feels stable even when the
 * primary field collides — the mockup's `sortDocs` does exactly this
 * (`(asc ? r : -r) || x.name.localeCompare(y.name)`).
 */
object LibrarySort {

    fun sorted(
        docs: List<LibraryDoc>,
        field: SortField,
        ascending: Boolean
    ): List<LibraryDoc> = docs.sortedWith(comparator(field, ascending))

    fun comparator(field: SortField, ascending: Boolean): Comparator<LibraryDoc> {
        val primary: Comparator<LibraryDoc> = when (field) {
            SortField.NAME -> caseInsensitiveName
            SortField.DATE_ADDED -> compareBy { it.document.dateAdded }
            SortField.LAST_OPENED -> compareBy { it.document.lastOpened ?: 0L }
            SortField.FILE_SIZE -> compareBy { it.document.sizeBytes }
        }
        val direction = if (ascending) primary else primary.reversed()
        // Natural order breaks ties that differ only in case, so the list is stable.
        return direction.thenBy { it.document.name }
    }

    /** Case-insensitive like `localeCompare`, with a case-sensitive tie-break. */
    private val caseInsensitiveName: Comparator<LibraryDoc> =
        compareBy(String.CASE_INSENSITIVE_ORDER) { it.document.name }
}
