package app.leaf.reader.feature.settings

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.model.ReadingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Settings actions persist immediately, normalise languages, and post the §11 snackbar copy. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SettingsViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var store: SettingsStore

    @Before
    fun setUp() {
        val file = tmp.newFile("leaf_settings_vm.preferences_pb")
        store = SettingsStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(UnconfinedTestDispatcher() + Job()),
                produceFile = { file }
            )
        )
    }

    @Test
    fun toggles_and_choices_persist_immediately() = runTest {
        val vm = SettingsViewModel(store, null, this)
        vm.update { it.copy(appTheme = AppTheme.DARK, readingTheme = ReadingTheme.SEPIA, animations = false) }
        advanceUntilIdle()

        val saved = store.current()
        assertEquals(AppTheme.DARK, saved.appTheme)
        assertEquals(ReadingTheme.SEPIA, saved.readingTheme)
        assertEquals(false, saved.animations)
    }

    @Test
    fun unsupported_language_tags_are_saved_as_english() = runTest {
        val vm = SettingsViewModel(store, null, this)
        assertEquals("en", vm.setLanguage("xx"))
        assertEquals("ar", vm.setLanguage("ar"))
        advanceUntilIdle()
        assertEquals("ar", store.current().language)
    }

    @Test
    fun a_clear_without_data_access_does_nothing_and_posts_nothing() = runTest {
        val vm = SettingsViewModel(store, null, this)
        vm.clear(SettingsClear.RECENTS)
        advanceUntilIdle()
        assertNull(vm.snack.value)
    }
}
