package app.leaf.reader.feature.settings

import androidx.annotation.StringRes
import app.leaf.reader.R
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.util.LeafLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** The five destructive rows under Data & storage. Each one confirms before it runs (§6.7). */
enum class SettingsClear { RECENTS, SEARCH, POSITIONS, HIGHLIGHTS, RESET }

/** A snackbar request. [id] changes on every post so the same text restarts the timer (§6.7). */
data class SettingsSnack(val id: Int, @StringRes val textRes: Int)

/**
 * State and actions behind the Settings screen (§6.5). Every toggle persists immediately (§8.12).
 * Dependencies are optional so the screen can render without the app graph (the screenshot tests).
 * Nothing here touches Android views; the screen handles the locale and activity side effects.
 */
class SettingsViewModel(
    private val store: SettingsStore?,
    private val data: SettingsDataController?,
    private val scope: CoroutineScope
) {
    val settings: Flow<LeafSettings> = store?.settings ?: flowOf(LeafSettings())
    val documentCount: Flow<Int> = data?.observeDocumentCount() ?: flowOf(0)
    val recentCount: Flow<Int> = data?.observeRecentCount() ?: flowOf(0)
    val highlightTotal: Flow<Int> = data?.observeHighlightTotal() ?: flowOf(0)

    private val mutableSnack = MutableStateFlow<SettingsSnack?>(null)
    val snack: StateFlow<SettingsSnack?> = mutableSnack.asStateFlow()
    private var nextSnackId = 0

    /** Persists a settings change. Returns immediately; the write happens on [scope]. */
    fun update(transform: (LeafSettings) -> LeafSettings) {
        val current = store ?: return
        scope.launch { current.update(transform) }
    }

    /** Persists the language tag, normalised to a supported locale, and returns the tag that was saved. */
    fun setLanguage(tag: String): String {
        val normalized = LeafLocale.normalize(tag)
        update { it.copy(language = normalized) }
        return normalized
    }

    fun clear(kind: SettingsClear) {
        val controller = data ?: return
        scope.launch {
            val message = when (kind) {
                SettingsClear.RECENTS -> {
                    controller.clearRecents()
                    R.string.snack_recents_cleared
                }
                SettingsClear.SEARCH -> {
                    controller.clearSearchHistory()
                    R.string.snack_search_history_cleared
                }
                SettingsClear.POSITIONS -> {
                    controller.clearReadingPositions()
                    R.string.snack_reading_positions_cleared
                }
                SettingsClear.HIGHLIGHTS -> {
                    controller.clearAllHighlights()
                    R.string.snack_all_highlights_removed
                }
                SettingsClear.RESET -> {
                    controller.resetLibrary()
                    R.string.snack_library_reset
                }
            }
            post(message)
        }
    }

    fun dismissSnack() {
        mutableSnack.value = null
    }

    private fun post(@StringRes text: Int) {
        mutableSnack.value = SettingsSnack(id = ++nextSnackId, textRes = text)
    }
}
