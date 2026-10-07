package app.leaf.reader

import android.app.Application
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.data.db.LeafDatabase
import app.leaf.reader.core.data.repo.DocumentRepository
import app.leaf.reader.core.data.repo.FolderRepository
import app.leaf.reader.core.data.repo.SmartCollectionRepository
import app.leaf.reader.core.data.repo.TagRepository
import app.leaf.reader.core.domain.SmartRule
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.util.LeafSwatches
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The manager sheets, at the repository level:
 * folders re-file, tags cascade, and neither ever throws a document away (§8.8, §8.9).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class OrganiseTest {

    private lateinit var db: LeafDatabase
    private lateinit var documents: DocumentRepository
    private lateinit var folders: FolderRepository
    private lateinit var tags: TagRepository
    private lateinit var smart: SmartCollectionRepository

    @Before
    fun setUp() {
        db = testDatabase()
        documents = DocumentRepository(
            documents = db.documentDao(),
            documentTags = db.documentTagDao(),
            bookmarks = db.bookmarkDao(),
            highlights = db.highlightDao(),
            progress = db.progressDao(),
            recents = db.recentDao()
        )
        folders = FolderRepository(db.folderDao(), db.documentDao())
        tags = TagRepository(db.tagDao(), db.documentTagDao())
        smart = SmartCollectionRepository(db.smartCollectionDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seed() {
        DatabaseSeeder(db).seed(FIXED_NOW)
    }

    private suspend fun rows() = documents.observeDocuments().first()

    // ── folders ───────────────────────────────────────────────────────────────

    @Test
    fun deleting_a_folder_reparents_its_documents_and_its_children() = runTest {
        seed()
        val inWork = rows().filter { it.folderId == "f-work" }
        assertTrue("the seed files something under Work", inWork.isNotEmpty())

        folders.delete("f-work")

        val left = folders.observeFolders().first()
        assertTrue(left.none { it.id == "f-work" })
        // Reports survives and moves up to the top level.
        assertEquals(null, left.first { it.id == "f-rep" }.parentId)

        val after = rows()
        assertEquals("no document is ever deleted with its folder", 10, after.size)
        inWork.forEach { moved ->
            assertEquals(null, after.first { it.id == moved.id }.folderId)
        }
    }

    @Test
    fun a_nested_folder_is_reparented_to_the_deleted_parents_parent() = runTest {
        seed()
        val parent = folders.create("Science", null)
        val child = folders.create("Chemistry", parent.id)
        val deeper = folders.create("Organic", child.id)

        folders.delete(child.id)

        val left = folders.observeFolders().first().associateBy { it.id }
        assertEquals(parent.id, left[deeper.id]?.parentId)
        assertTrue(left[child.id] == null)
    }

    @Test
    fun a_new_folder_takes_the_next_palette_swatch() = runTest {
        seed()
        val created = folders.create("Research", null)
        assertEquals("Research", created.name)
        assertEquals(LeafSwatches[5], created.colorHex)
        assertEquals(6, folders.observeFolders().first().size)
    }

    // ── tags ──────────────────────────────────────────────────────────────────

    @Test
    fun renaming_a_tag_cascades_to_every_document() = runTest {
        seed()
        val before = rows().filter { it.tags.contains("work") }.map { it.id }.sorted()
        assertTrue(before.isNotEmpty())

        tags.rename("work", "office")

        val after = rows()
        assertEquals(before, after.filter { it.tags.contains("office") }.map { it.id }.sorted())
        assertTrue(after.none { it.tags.contains("work") })
        assertEquals(1, tags.observeTags().first().count { it.name == "office" })
        assertEquals("the documents themselves are untouched", 10, after.size)
    }

    @Test
    fun deleting_a_tag_unfiles_every_document_but_deletes_none_of_them() = runTest {
        seed()
        tags.delete("work")

        val after = rows()
        assertEquals(10, after.size)
        assertTrue(after.none { it.tags.contains("work") })
        assertTrue(tags.observeTags().first().none { it.name == "work" })
    }

    @Test
    fun a_duplicate_tag_is_rejected_and_a_fresh_one_is_accepted() = runTest {
        seed()
        assertFalse(tags.create("Work"))
        assertTrue(tags.create("urgent"))
        assertEquals(1, tags.observeTags().first().count { it.name == "urgent" })
    }

    // ── smart collections ─────────────────────────────────────────────────────

    @Test
    fun a_duplicate_collection_is_rejected_and_a_fresh_one_is_accepted() = runTest {
        seed()
        assertFalse(smart.add("All PDFs", SmartRule.OfType(DocType.PDF)))
        assertTrue(smart.add("All documents", SmartRule.All))
        assertEquals(7, smart.observeCollections().first().size)
    }

    @Test
    fun a_collection_keeps_its_rule_after_a_round_trip() = runTest {
        seed()
        assertTrue(smart.add("Long reads", SmartRule.OfType(DocType.EPUB)))
        val stored = smart.observeCollections().first().first { it.name == "Long reads" }
        assertEquals(SmartRule.OfType(DocType.EPUB), stored.rule)
    }
}
