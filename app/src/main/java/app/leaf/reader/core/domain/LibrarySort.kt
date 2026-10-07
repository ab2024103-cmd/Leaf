package app.leaf.reader.core.domain

import app.leaf.reader.core.model.SortField

/**
 * Library sorting (§6.1, §14 row 9). Four fields, two directions; **file type is not a
 * sort field** — it is its own grouped view (§8.13).
 *
 * "Ascending" is the direction the sort chip names first for that field — `A–Z`,
 * `Newest`, `Recent`, `Largest` (mockup `SORTS[f].dirs[0]`, which is what
 * `sortLabelOf` prints for `sortAsc === true`). The mockup's own demo comparator sorts
 * the three non-name fields the other way, which would print `Date added · Newest` above
 * an oldest-first list; Leaf keeps the copy and makes the order agree with it.
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
        val natural: Comparator<LibraryDoc> = when (field) {
            SortField.NAME -> caseInsensitiveName
            SortField.DATE_ADDED -> compareByDescending { it.document.dateAdded }
            // A document that was never opened has no timestamp at all, so it reads as
            // the oldest one there is and trails the "Recent" end of the list.
            SortField.LAST_OPENED -> compareByDescending { it.document.lastOpened ?: 0L }
            SortField.FILE_SIZE -> compareByDescending { it.document.sizeBytes }
        }
        val direction = if (ascending) natural else natural.reversed()
        // Natural order breaks ties that differ only in case, so the list is stable.
        return direction.thenBy { it.document.name }
    }

    /** Case-insensitive like `localeCompare`, with a case-sensitive tie-break. */
    private val caseInsensitiveName: Comparator<LibraryDoc> =
        compareBy(String.CASE_INSENSITIVE_ORDER) { it.document.name }
}
