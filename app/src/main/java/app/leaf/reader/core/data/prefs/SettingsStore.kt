package app.leaf.reader.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.model.FavSort
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.model.SortField
import app.leaf.reader.core.model.Typeface
import java.io.IOException

/**
 * Settings in DataStore Preferences (LEAF-SPEC.md §4). Every toggle persists
 * immediately and re-renders dependent surfaces (§8.12).
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<LeafSettings> = dataStore.data
        .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
        .map { prefs -> prefs.toSettings() }

    suspend fun current(): LeafSettings = settings.first()

    suspend fun update(transform: (LeafSettings) -> LeafSettings) {
        val next = transform(current())
        dataStore.edit { prefs -> prefs.writeAll(next) }
    }

    /**
     * §8.13: a build that persisted "File type" as a sort field moves to the grouped
     * view (groupByType = true, sortField = Name). Run once at startup.
     */
    suspend fun migrateLegacySortField() {
        dataStore.edit { prefs ->
            if (prefs[SORT_FIELD] == LEGACY_FILE_TYPE) {
                prefs[SORT_FIELD] = SortField.NAME.name
                prefs[GROUP_BY_TYPE] = true
            }
        }
    }

    private fun Preferences.toSettings(): LeafSettings = LeafSettings(
        appTheme = enumOrDefault(this[APP_THEME], AppTheme.LIGHT),
        matchSystemColors = this[MATCH_SYSTEM_COLORS] ?: false,
        readingTheme = enumOrDefault(this[READING_THEME], ReadingTheme.PAPER),
        scrollDir = enumOrDefault(this[SCROLL_DIR], ScrollDir.VERTICAL),
        zoom = this[ZOOM] ?: 1f,
        typeface = enumOrDefault(this[TYPEFACE], Typeface.SERIF),
        reflow = this[REFLOW] ?: false,
        rememberPage = this[REMEMBER_PAGE] ?: true,
        autoRotate = this[AUTO_ROTATE] ?: true,
        orientationLock = this[ORIENTATION_LOCK] ?: false,
        showPageNumbers = this[SHOW_PAGE_NUMBERS] ?: true,
        animations = this[ANIMATIONS] ?: true,
        haptics = this[HAPTICS] ?: true,
        tabLimit = this[TAB_LIMIT] ?: 6,
        groupByType = this[GROUP_BY_TYPE] ?: false,
        language = this[LANGUAGE] ?: "en",
        sortField = if (this[SORT_FIELD] == LEGACY_FILE_TYPE) {
            SortField.NAME
        } else {
            enumOrDefault(this[SORT_FIELD], SortField.NAME)
        },
        sortAscending = this[SORT_ASCENDING] ?: true,
        favSort = enumOrDefault(this[FAV_SORT], FavSort.ADDED),
        searchHistory = this[SEARCH_HISTORY]?.toList() ?: emptyList()
    )

    private fun MutablePreferences.writeAll(settings: LeafSettings) {
        this[APP_THEME] = settings.appTheme.name
        this[MATCH_SYSTEM_COLORS] = settings.matchSystemColors
        this[READING_THEME] = settings.readingTheme.name
        this[SCROLL_DIR] = settings.scrollDir.name
        this[ZOOM] = settings.zoom
        this[TYPEFACE] = settings.typeface.name
        this[REFLOW] = settings.reflow
        this[REMEMBER_PAGE] = settings.rememberPage
        this[AUTO_ROTATE] = settings.autoRotate
        this[ORIENTATION_LOCK] = settings.orientationLock
        this[SHOW_PAGE_NUMBERS] = settings.showPageNumbers
        this[ANIMATIONS] = settings.animations
        this[HAPTICS] = settings.haptics
        this[TAB_LIMIT] = settings.tabLimit
        this[GROUP_BY_TYPE] = settings.groupByType
        this[LANGUAGE] = settings.language
        this[SORT_FIELD] = settings.sortField.name
        this[SORT_ASCENDING] = settings.sortAscending
        this[FAV_SORT] = settings.favSort.name
        this[SEARCH_HISTORY] = settings.searchHistory.toSet()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, fallback: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    private companion object {
        const val LEGACY_FILE_TYPE = "FILE_TYPE"

        val APP_THEME = stringPreferencesKey("app_theme")
        val MATCH_SYSTEM_COLORS = booleanPreferencesKey("match_system_colors")
        val READING_THEME = stringPreferencesKey("reading_theme")
        val SCROLL_DIR = stringPreferencesKey("scroll_dir")
        val ZOOM = floatPreferencesKey("zoom")
        val TYPEFACE = stringPreferencesKey("typeface")
        val REFLOW = booleanPreferencesKey("reflow")
        val REMEMBER_PAGE = booleanPreferencesKey("remember_page")
        val AUTO_ROTATE = booleanPreferencesKey("auto_rotate")
        val ORIENTATION_LOCK = booleanPreferencesKey("orientation_lock")
        val SHOW_PAGE_NUMBERS = booleanPreferencesKey("show_page_numbers")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val HAPTICS = booleanPreferencesKey("haptics")
        val TAB_LIMIT = intPreferencesKey("tab_limit")
        val GROUP_BY_TYPE = booleanPreferencesKey("group_by_type")
        val LANGUAGE = stringPreferencesKey("language")
        val SORT_FIELD = stringPreferencesKey("sort_field")
        val SORT_ASCENDING = booleanPreferencesKey("sort_ascending")
        val FAV_SORT = stringPreferencesKey("fav_sort")
        val SEARCH_HISTORY = stringSetPreferencesKey("search_history")
    }
}
