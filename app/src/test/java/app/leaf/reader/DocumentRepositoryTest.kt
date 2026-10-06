package app.leaf.reader

import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.data.db.LeafDatabase
import app.leaf.reader.core.data.repo.DocumentRepository
import app.leaf.reader.core.data.repo.SmartCollectionRepository
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.SmartRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Documents, their tags and the smart-collection rules (LEAF-SPEC.md §4). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DocumentRepositoryTest {

    private lateinit var db: LeafDatabase
    private lateinit var documents: DocumentRepository

    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        db = testDatabase()
        documents = DocumentRepository(db.documentDao(), db.documentTagDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun documents_carry_their_tags() = runTest {
        DatabaseSeeder(db).seed(now)

        val all = documents.observeDocuments().first()
        assertEquals(10, all.size)
        assertEquals(listOf("personal", "reading"), all.first { it.id == "d1" }.tags)
        assertEquals(emptyList<String>(), all.first { it.id == "d8" }.tags)
    }

    @Test
    fun tag_filter_uses_and_semantics() = runTest {
        DatabaseSeeder(db).seed(now)

        val both = documents.observeWithAllTags(listOf("work", "important")).first()
        assertEquals(listOf("d2", "d9"), both.map { it.id }.sorted())

        val one = documents.observeWithAllTags(listOf("work")).first()
        assertEquals(listOf("d10", "d2", "d5", "d9"), one.map { it.id }.sorted())
    }

    @Test
    fun seed_sizes_and_page_counts_match_the_mockup() = runTest {
        DatabaseSeeder(db).seed(now)

        val all = documents.observeDocuments().first()
        val pdfs = all.filter { it.type == DocType.PDF }
        assertEquals(5, pdfs.size)
        assertEquals(6, all.first { it.id == "d1" }.pageCount)
        assertEquals(7, all.first { it.id == "d4" }.pageCount)
    }

    @Test
    fun smart_collections_round_trip_their_rules() = runTest {
        DatabaseSeeder(db).seed(now)

        val collections = SmartCollectionRepository(db.smartCollectionDao()).observeCollections().first()
        assertEquals(6, collections.size)
        val byId = collections.associateBy { it.id }
        assertEquals(SmartRule.OfType(DocType.PDF), byId["s-pdf"]?.rule)
        assertEquals(SmartRule.AgeDays(7), byId["s-week"]?.rule)
        assertEquals(SmartRule.InProgress, byId["s-open"]?.rule)
        assertEquals(SmartRule.Unfiled, byId["s-unfiled"]?.rule)
        assertEquals(SmartRule.HasHighlights, byId["s-hl"]?.rule)
        assertEquals(SmartRule.OfType(DocType.XLSX), byId["s-sheet"]?.rule)
    }
}
