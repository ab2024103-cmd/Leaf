package app.leaf.reader

import app.leaf.reader.core.domain.LibraryFilter
import app.leaf.reader.core.domain.composeLibrary
import app.leaf.reader.core.domain.groupCards
import app.leaf.reader.core.domain.subtreeIds
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.SmartRule
import app.leaf.reader.core.model.SortField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §12: filter composition (folder × collection × tag × type), the seven smart rules, the
 * chip counts, the group cards and the §8.13 rule that an emptied group falls back to the
 * overview.
 */
class LibraryQueryTest {

    private val folders = listOf(
        app.leaf.reader.folder("f-work"),
        app.leaf.reader.folder("f-rep", parentId = "f-work"),
        app.leaf.reader.folder("f-uni")
    )
    private val tags = listOf("work", "reading", "study").map { app.leaf.reader.tag(it) }
    private val smart = listOf(
        app.leaf.reader.collection("s-pdf", SmartRule.OfType(DocType.PDF)),
        app.leaf.reader.collection("s-progress", SmartRule.InProgress),
        app.leaf.reader.collection("s-hl", SmartRule.HasHighlights),
        app.leaf.reader.collection("s-unfiled", SmartRule.Unfiled),
        app.leaf.reader.collection("s-week", SmartRule.AgeDays(7)),
        app.leaf.reader.collection("s-all", SmartRule.All),
        app.leaf.reader.collection("s-tag", SmartRule.WithTag("study"))
    )
    private val docs = listOf(
        doc("d1", type = DocType.PDF, folderId = "f-work", tags = listOf("work"), addedDaysAgo = 1,
            page = 1, pageCount = 5, highlights = 2, bookmarks = 1),
        doc("d2", type = DocType.PDF, folderId = "f-rep", tags = listOf("work", "reading"),
            addedDaysAgo = 3, pageCount = 5, sizeMb = 4.0),
        doc("d3", type = DocType.DOCX, folderId = "f-uni", tags = listOf("study"), addedDaysAgo = 20),
        doc("d4", type = DocType.XLSX, folderId = null, tags = listOf("reading"), addedDaysAgo = 40),
        doc("d5", type = DocType.PPTX, folderId = "f-work", tags = emptyList(), addedDaysAgo = 2,
            sizeMb = 8.0)
    )

    private fun view(
        filter: LibraryFilter = LibraryFilter(),
        groupByType: Boolean = false,
        field: SortField = SortField.NAME,
        ascending: Boolean = true
    ) = composeLibrary(
        docs = docs,
        folders = folders,
        smart = smart,
        filter = filter,
        sortField = field,
        sortAscending = ascending,
        groupByType = groupByType,
        now = FIXED_NOW
    )

    // ── filters compose ──────────────────────────────────────────────────────────

    @Test
    fun a_folder_counts_its_whole_subtree() {
        assertEquals(setOf("f-work", "f-rep"), subtreeIds(folders, "f-work"))
        assertEquals(setOf("f-rep"), subtreeIds(folders, "f-rep"))
        assertEquals(
            listOf("d1", "d2", "d5"),
            view(LibraryFilter(folderId = "f-work")).rows.map { it.id }
        )
    }

    @Test
    fun tags_use_and_semantics() {
        assertEquals(
            listOf("d2"),
            view(LibraryFilter(tags = setOf("work", "reading"))).rows.map { it.id }
        )
        assertEquals(
            listOf("d1", "d2"),
            view(LibraryFilter(tags = setOf("work"))).rows.map { it.id }
        )
    }

    @Test
    fun folder_collection_and_tags_compose() {
        val filtered = view(
            LibraryFilter(folderId = "f-work", smartId = "s-pdf", tags = setOf("work"))
        )
        assertEquals(listOf("d1", "d2"), filtered.rows.map { it.id })
    }

    @Test
    fun the_open_group_narrows_the_rows_without_touching_the_counts() {
        val grouped = view(LibraryFilter(type = DocType.PDF), groupByType = true)
        assertEquals(listOf("d1", "d2"), grouped.rows.map { it.id })
        assertEquals(DocType.PDF, grouped.openType)
        // The chips still answer for the whole filter context, not for the open group.
        assertEquals(3, grouped.folderTotals["f-work"])
    }

    @Test
    fun sorting_applies_inside_the_open_group() {
        val grouped = view(
            LibraryFilter(type = DocType.PDF),
            groupByType = true,
            field = SortField.FILE_SIZE,
            ascending = false
        )
        assertEquals(listOf("d2", "d1"), grouped.rows.map { it.id })
    }

    // ── the seven smart rules ────────────────────────────────────────────────────

    @Test
    fun every_smart_rule_matches_what_it_says() {
        assertEquals(listOf("d1", "d2", "d3", "d4", "d5"), view(LibraryFilter(smartId = "s-all")).rows.map { it.id })
        assertEquals(listOf("d1", "d2"), view(LibraryFilter(smartId = "s-pdf")).rows.map { it.id })
        // d1 is 40 % through a 5 page document: started, not finished.
        assertEquals(listOf("d1"), view(LibraryFilter(smartId = "s-progress")).rows.map { it.id })
        assertEquals(listOf("d1"), view(LibraryFilter(smartId = "s-hl")).rows.map { it.id })
        assertEquals(listOf("d4"), view(LibraryFilter(smartId = "s-unfiled")).rows.map { it.id })
        assertEquals(listOf("d1", "d2", "d5"), view(LibraryFilter(smartId = "s-week")).rows.map { it.id })
        assertEquals(listOf("d3"), view(LibraryFilter(smartId = "s-tag")).rows.map { it.id })
    }

    // ── counts follow the filter context (§8.5) ──────────────────────────────────

    @Test
    fun folder_counts_ignore_the_selected_folder_but_keep_the_tags() {
        val filtered = view(LibraryFilter(folderId = "f-work", tags = setOf("work")))
        // Only the folder dimension is dropped: the tag filter still applies.
        assertEquals(2, filtered.folderTotals[null])
        assertEquals(2, filtered.folderTotals["f-work"])   // d1 + d2 (the subtree)
        assertEquals(0, filtered.folderTotals["f-uni"])    // d3 is tagged "study"
    }

    @Test
    fun tag_filters_narrow_every_other_dimension() {
        val filtered = view(LibraryFilter(folderId = "f-work", tags = setOf("work")))
        assertEquals(2, filtered.folderTotals["f-work"])
        assertEquals(2, filtered.smartTotals["s-pdf"])
    }

    @Test
    fun smart_counts_reflect_the_other_filters() {
        val filtered = view(LibraryFilter(folderId = "f-work", smartId = "s-pdf"))
        assertEquals(2, filtered.smartTotals["s-pdf"])
        assertEquals(3, filtered.smartTotals["s-all"])
        assertEquals(2, filtered.smartTotals["s-week"])
    }

    // ── the grouped view ─────────────────────────────────────────────────────────

    @Test
    fun group_cards_follow_the_fixed_type_order_and_sum_their_documents() {
        val cards = groupCards(docs)
        assertEquals(
            listOf(DocType.PDF, DocType.DOCX, DocType.XLSX, DocType.PPTX),
            cards.map { it.type }
        )
        val pdf = cards.first { it.type == DocType.PDF }
        assertEquals(2, pdf.count)
        assertEquals(2, pdf.docsWithHighlights)
        assertEquals(1, pdf.docsWithBookmarks)
    }

    @Test
    fun the_overview_shows_when_grouping_is_on_and_no_group_is_open() {
        val view = view(groupByType = true)
        assertTrue(view.showGroups)
        assertEquals(4, view.groups.size)
        assertEquals(4, view.typeCount)
        assertEquals(docs.size, view.visible.size)
    }

    @Test
    fun a_group_that_the_filters_emptied_falls_back_to_the_overview() {
        // "s-pdf" leaves no PPTX behind, so the open group cannot stand (§8.13).
        val view = view(LibraryFilter(smartId = "s-pdf", type = DocType.PPTX), groupByType = true)
        assertEquals(null, view.openType)
        assertTrue(view.showGroups)
        assertEquals(listOf(DocType.PDF), view.groups.map { it.type })
    }

    @Test
    fun grouping_off_means_one_flat_list() {
        val view = view(LibraryFilter(type = DocType.PDF), groupByType = false)
        assertEquals(null, view.openType)
        assertEquals(docs.size, view.rows.size)
    }
}
