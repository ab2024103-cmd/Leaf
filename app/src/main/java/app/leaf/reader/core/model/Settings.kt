package app.leaf.reader.core.model

/**
 * Settings (§2.6, §6.5) with the mockup's defaults.
 *
 * Two independent axes: [AppTheme] drives the chrome, [ReadingTheme] drives the
 * document canvas only. Neither may drive the other.
 */
enum class AppTheme { LIGHT, DARK, SYSTEM }

enum class ReadingTheme { PAPER, SEPIA, NIGHT, OLED, AUTO }

enum class ScrollDir { VERTICAL, HORIZONTAL }

enum class Typeface { SERIF, SANS }

/**
 * @param matchSystemColors Material You dynamic colour (§1.4). Off by default so brand
 * parity is preserved; exposed as an opt-in switch.
 * @param groupByType The persisted file-type grouping switch (§8.13).
 * @param searchHistory Last 10 distinct queries (§2.3).
 */
data class LeafSettings(
    val appTheme: AppTheme = AppTheme.LIGHT,
    val matchSystemColors: Boolean = false,
    val readingTheme: ReadingTheme = ReadingTheme.PAPER,
    val scrollDir: ScrollDir = ScrollDir.VERTICAL,
    val zoom: Float = 1f,
    val typeface: Typeface = Typeface.SERIF,
    val reflow: Boolean = false,
    val rememberPage: Boolean = true,
    val autoRotate: Boolean = true,
    val orientationLock: Boolean = false,
    val showPageNumbers: Boolean = true,
    val animations: Boolean = true,
    val haptics: Boolean = true,
    val tabLimit: Int = 6,
    val groupByType: Boolean = false,
    val language: String = "en",
    val sortField: SortField = SortField.NAME,
    val sortAscending: Boolean = true,
    val favSort: FavSort = FavSort.ADDED,
    val searchHistory: List<String> = emptyList()
) {
    companion object {
        const val TAB_LIMIT_MIN = 1
        const val TAB_LIMIT_MAX = 10
        const val SEARCH_HISTORY_MAX = 10
        const val RECENTS_MAX = 60
        const val ZOOM_MIN = 0.70f
        const val ZOOM_MAX = 2.50f
    }
}
