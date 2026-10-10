package app.leaf.reader

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.leaf.reader.core.data.prefs.OpenTabResult
import app.leaf.reader.core.data.prefs.ReaderTabsStore
import app.leaf.reader.core.model.ReaderTab
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.rules.TemporaryFolder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ReaderTabsStoreTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun tabs_keep_order_active_state_and_full_reader_position_across_store_recreation() = runTest {
        val file = temporary.newFile("tabs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher() + Job()),
            produceFile = { file }
        )
        val store = ReaderTabsStore(dataStore)
        val first = tab(
            "first",
            page = 4,
            zoom = 1.6f,
            direction = ScrollDir.VERTICAL,
            theme = ReadingTheme.SEPIA,
            query = "chapter",
            scrollFraction = 0.42f,
            panX = 0.12f,
            panY = -0.08f
        )
        val second = tab(
            "second",
            page = 2,
            zoom = 1.35f,
            direction = ScrollDir.HORIZONTAL,
            theme = ReadingTheme.OLED,
            query = "notes",
            scrollFraction = 0.2f,
            panX = -0.1f,
            panY = 0.08f
        )

        assertEquals(OpenTabResult.Opened, store.open("first", newTab = false, tabLimit = 3, initialTab = first))
        assertEquals(OpenTabResult.Opened, store.open("second", newTab = true, tabLimit = 3, initialTab = second))

        val restoredStore = ReaderTabsStore(dataStore)
        val restored = restoredStore.state.first()
        assertEquals(listOf("first", "second"), restored.tabs.map { it.docId })
        assertEquals("second", restored.activeDocId)
        assertEquals(first, restored.tabs[0])
        assertEquals(second, restored.tabs[1])
    }

    @Test
    fun normal_open_reuses_active_tab_new_tab_respects_limit_and_close_selects_neighbor() = runTest {
        val store = createStore(temporary.newFile("tabs-operations.preferences_pb"))
        store.open("one", newTab = false, tabLimit = 2, initialTab = tab("one"))
        store.open("two", newTab = true, tabLimit = 2, initialTab = tab("two"))

        assertEquals(
            OpenTabResult.LimitReached(2),
            store.open("three", newTab = true, tabLimit = 2, initialTab = tab("three"))
        )
        assertEquals(listOf("one", "two"), store.current().tabs.map { it.docId })
        store.open("replacement", newTab = false, tabLimit = 2, initialTab = tab("replacement"))
        assertEquals(listOf("one", "replacement"), store.current().tabs.map { it.docId })
        assertEquals("replacement", store.current().activeDocId)

        val closed = store.close("replacement")!!
        assertEquals("one", store.current().activeDocId)
        store.restore(closed)
        assertEquals(listOf("one", "replacement"), store.current().tabs.map { it.docId })
        assertEquals("replacement", store.current().activeDocId)

        store.close("one")
        assertEquals("replacement", store.current().activeDocId)
        store.close("replacement")
        assertTrue(store.current().tabs.isEmpty())
        assertEquals(null, store.current().activeDocId)
    }

    @Test
    fun first_kept_open_hint_is_recorded_once_in_preferences() = runTest {
        val store = createStore(temporary.newFile("kept-open-hint.preferences_pb"))

        assertTrue(store.markKeptOpenHintShown())
        assertFalse(store.markKeptOpenHintShown())
    }

    @Test
    fun snapshot_updates_do_not_resurrect_a_closed_tab() = runTest {
        val store = createStore(temporary.newFile("closed-tabs.preferences_pb"))
        store.open("one", newTab = false, tabLimit = 6, initialTab = tab("one"))
        store.close("one")
        store.updateSnapshot(tab("one", page = 5))

        assertFalse(store.current().tabs.any { it.docId == "one" })
    }

    private fun createStore(file: File): ReaderTabsStore {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher() + Job()),
            produceFile = { file }
        )
        return ReaderTabsStore(dataStore)
    }

    private fun tab(
        id: String,
        page: Int = 0,
        zoom: Float = 1f,
        direction: ScrollDir = ScrollDir.VERTICAL,
        theme: ReadingTheme = ReadingTheme.PAPER,
        query: String? = null,
        scrollFraction: Float = 0f,
        panX: Float = 0f,
        panY: Float = 0f
    ) = ReaderTab(
        docId = id,
        page = page,
        zoom = zoom,
        scrollDir = direction,
        findQuery = query,
        scrollFraction = scrollFraction,
        readingTheme = theme,
        panX = panX,
        panY = panY
    )
}
