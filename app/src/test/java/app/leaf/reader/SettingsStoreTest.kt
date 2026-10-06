package app.leaf.reader

import android.app.Application

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.model.FavSort
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.model.SortField
import app.leaf.reader.core.model.Typeface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Settings defaults and the §8.13 sort-field migration. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SettingsStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var store: SettingsStore

    @Before
    fun setUp() {
        val file = tmp.newFile("leaf_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher() + Job()),
            produceFile = { file }
        )
        store = SettingsStore(dataStore)
    }

    @Test
    fun defaults_match_the_mockup() = runTest {
        val settings = store.current()

        assertEquals(LeafSettings(), settings)
        assertEquals(AppTheme.LIGHT, settings.appTheme)
        assertFalse(settings.matchSystemColors)
        assertEquals(ReadingTheme.PAPER, settings.readingTheme)
        assertEquals(ScrollDir.VERTICAL, settings.scrollDir)
        assertEquals(1f, settings.zoom)
        assertEquals(Typeface.SERIF, settings.typeface)
        assertFalse(settings.reflow)
        assertTrue(settings.rememberPage)
        assertTrue(settings.autoRotate)
        assertFalse(settings.orientationLock)
        assertTrue(settings.showPageNumbers)
        assertTrue(settings.animations)
        assertTrue(settings.haptics)
        assertEquals(6, settings.tabLimit)
        assertFalse(settings.groupByType)
        assertEquals("en", settings.language)
        assertEquals(SortField.NAME, settings.sortField)
        assertTrue(settings.sortAscending)
        assertEquals(FavSort.ADDED, settings.favSort)
        assertEquals(emptyList<String>(), settings.searchHistory)
    }

    @Test
    fun a_legacy_file_type_sort_becomes_the_grouped_view() = runTest {
        val key = stringPreferencesKey("sort_field")

        // A fresh store is untouched by the migration.
        store.migrateLegacySortField()
        assertEquals(SortField.NAME, store.current().sortField)
        assertFalse(store.current().groupByType)

        // Simulate legacy persisted state directly, then migrate.
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher() + Job()),
            produceFile = { tmp.newFile("legacy.preferences_pb") }
        )
        dataStore.edit { it[key] = "FILE_TYPE" }
        val legacy = SettingsStore(dataStore)
        assertEquals(SortField.NAME, legacy.current().sortField)
        assertFalse("stored value is still legacy until migration runs", legacy.current().groupByType)

        legacy.migrateLegacySortField()
        assertEquals(SortField.NAME, legacy.current().sortField)
        assertTrue(legacy.current().groupByType)
    }

    @Test
    fun updates_persist() = runTest {
        store.update { it.copy(appTheme = AppTheme.DARK, haptics = false, tabLimit = 3) }

        val settings = store.current()
        assertEquals(AppTheme.DARK, settings.appTheme)
        assertFalse(settings.haptics)
        assertEquals(3, settings.tabLimit)
    }

    @Test
    fun unknown_values_fall_back_to_the_default() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher() + Job()),
            produceFile = { tmp.newFile("junk.preferences_pb") }
        )
        dataStore.edit {
            it[stringPreferencesKey("app_theme")] = "NOT_A_THEME"
            it[stringPreferencesKey("reading_theme")] = "PAPER"
        }

        assertEquals(AppTheme.LIGHT, SettingsStore(dataStore).settings.first().appTheme)
    }
}
