package app.leaf.reader.feature.settings

import app.leaf.reader.core.data.prefs.ReaderTabsStore
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.data.db.LibraryReset
import app.leaf.reader.core.data.repo.AnnotationRepository
import app.leaf.reader.core.data.repo.DocumentRepository
import app.leaf.reader.core.data.repo.ReaderRepository
import app.leaf.reader.core.data.repo.RecentRepository
import kotlinx.coroutines.flow.Flow

/**
 * The Settings → Data & storage actions and live counts (§6.5). Every destructive call here
 * is reached only through a confirmation dialog in the screen.
 */
class SettingsDataController(
    private val documents: DocumentRepository,
    private val recents: RecentRepository,
    private val annotations: AnnotationRepository,
    private val reader: ReaderRepository,
    private val tabs: ReaderTabsStore,
    private val libraryReset: LibraryReset,
    private val settings: SettingsStore
) {
    fun observeDocumentCount(): Flow<Int> = documents.observeCount()

    fun observeRecentCount(): Flow<Int> = recents.observeCount()

    fun observeHighlightTotal(): Flow<Int> = annotations.observeHighlightTotal()

    suspend fun clearRecents() = recents.clear()

    suspend fun clearSearchHistory() = settings.update { it.copy(searchHistory = emptyList()) }

    /**
     * Positions live in two places: Room (per-document Page Stay) and the open-tab snapshots
     * (DataStore). Both are reset so no document reopens at its old page.
     */
    suspend fun clearReadingPositions() {
        reader.clearPositions()
        tabs.current().tabs.forEach { tab ->
            tabs.updateSnapshot(tab.copy(page = 0, scrollFraction = 0f, panX = 0f, panY = 0f))
        }
    }

    suspend fun clearAllHighlights() = annotations.clearAllHighlights()

    /** Removes every document and annotation; open tabs are closed first (their documents may be gone). */
    suspend fun resetLibrary() {
        tabs.closeAll()
        libraryReset.reset()
    }
}
