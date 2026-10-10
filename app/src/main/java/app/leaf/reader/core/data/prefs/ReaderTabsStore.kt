package app.leaf.reader.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.model.ReaderTab
import app.leaf.reader.core.model.ReaderTabsState
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Process-persistent, ordered reader tabs stored alongside user preferences. */
class ReaderTabsStore(private val dataStore: DataStore<Preferences>) {
    val state: Flow<ReaderTabsState> = dataStore.data
        .catch { cause -> if (cause is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw cause }
        .map { it[TABS]?.let(::decode) ?: ReaderTabsState() }

    suspend fun current(): ReaderTabsState = state.first()

    /** Open in the current tab, or append a new tab when [newTab] is true. */
    suspend fun open(
        docId: String,
        newTab: Boolean,
        tabLimit: Int,
        initialTab: ReaderTab
    ): OpenTabResult {
        val effectiveLimit = tabLimit.coerceIn(LeafSettings.TAB_LIMIT_MIN, LeafSettings.TAB_LIMIT_MAX)
        var result: OpenTabResult = OpenTabResult.Opened
        dataStore.edit { prefs ->
            val old = prefs[TABS]?.let(::decode) ?: ReaderTabsState()
            val existing = old.tabs.firstOrNull { it.docId == docId }
            val next = when {
                existing != null -> old.copy(activeDocId = docId)
                newTab && old.tabs.size >= effectiveLimit -> {
                    result = OpenTabResult.LimitReached(effectiveLimit)
                    old
                }
                newTab -> old.copy(
                    tabs = old.tabs + initialTab.copy(docId = docId),
                    activeDocId = docId
                )
                old.activeDocId == null || old.tabs.isEmpty() -> old.copy(
                    tabs = old.tabs + initialTab.copy(docId = docId),
                    activeDocId = docId
                )
                else -> {
                    val activeIndex = old.tabs.indexOfFirst { it.docId == old.activeDocId }
                    val replaced = old.tabs.toMutableList()
                    if (activeIndex >= 0) replaced[activeIndex] = initialTab.copy(docId = docId)
                    else replaced += initialTab.copy(docId = docId)
                    old.copy(tabs = replaced.distinctBy(ReaderTab::docId), activeDocId = docId)
                }
            }
            if (result == OpenTabResult.Opened) prefs[TABS] = encode(next)
        }
        return result
    }

    suspend fun activate(docId: String): Boolean {
        var activated = false
        dataStore.edit { prefs ->
            val current = prefs[TABS]?.let(::decode) ?: ReaderTabsState()
            if (current.tabs.any { it.docId == docId }) {
                prefs[TABS] = encode(current.copy(activeDocId = docId))
                activated = true
            }
        }
        return activated
    }

    /** Saves a tab snapshot without resurrecting a tab that the user has closed. */
    suspend fun updateSnapshot(tab: ReaderTab) {
        dataStore.edit { prefs ->
            val current = prefs[TABS]?.let(::decode) ?: ReaderTabsState()
            val index = current.tabs.indexOfFirst { it.docId == tab.docId }
            if (index >= 0) {
                val tabs = current.tabs.toMutableList()
                tabs[index] = tab
                prefs[TABS] = encode(current.copy(tabs = tabs))
            }
        }
    }

    suspend fun close(docId: String): ClosedTab? {
        var closed: ClosedTab? = null
        dataStore.edit { prefs ->
            val current = prefs[TABS]?.let(::decode) ?: ReaderTabsState()
            val index = current.tabs.indexOfFirst { it.docId == docId }
            if (index < 0) return@edit
            val tab = current.tabs[index]
            val remaining = current.tabs.toMutableList().also { it.removeAt(index) }
            val nextActive = if (current.activeDocId != docId) {
                current.activeDocId?.takeIf { id -> remaining.any { it.docId == id } }
            } else {
                remaining.getOrNull(index)?.docId ?: remaining.getOrNull(index - 1)?.docId
            }
            prefs[TABS] = encode(ReaderTabsState(remaining, nextActive))
            closed = ClosedTab(tab = tab, index = index, previousActiveDocId = current.activeDocId)
        }
        return closed
    }

    /** Restores the saved row in-place; visibility of the Reader is controlled by the shell. */
    suspend fun restore(closed: ClosedTab) {
        dataStore.edit { prefs ->
            val current = prefs[TABS]?.let(::decode) ?: ReaderTabsState()
            if (current.tabs.any { it.docId == closed.tab.docId }) return@edit
            val tabs = current.tabs.toMutableList()
            tabs.add(closed.index.coerceIn(0, tabs.size), closed.tab)
            val active = closed.previousActiveDocId
                ?.takeIf { id -> tabs.any { it.docId == id } }
                ?: current.activeDocId
                ?: closed.tab.docId
            prefs[TABS] = encode(ReaderTabsState(tabs, active))
        }
    }

    suspend fun removeDocument(docId: String) {
        dataStore.edit { prefs ->
            val current = prefs[TABS]?.let(::decode) ?: ReaderTabsState()
            if (current.tabs.none { it.docId == docId }) return@edit
            val index = current.tabs.indexOfFirst { it.docId == docId }
            val tabs = current.tabs.filterNot { it.docId == docId }
            val active = if (current.activeDocId == docId) {
                tabs.getOrNull(index)?.docId ?: tabs.getOrNull(index - 1)?.docId
            } else current.activeDocId?.takeIf { id -> tabs.any { it.docId == id } }
            prefs[TABS] = encode(ReaderTabsState(tabs, active))
        }
    }

    suspend fun closeAll(): ReaderTabsState {
        val old = current()
        dataStore.edit { it[TABS] = encode(ReaderTabsState()) }
        return old
    }

    /** Returns true only once per installation for the first-kept-tabs hint. */
    suspend fun markKeptOpenHintShown(): Boolean {
        var first = false
        dataStore.edit { prefs ->
            if (prefs[KEPT_OPEN_HINT_SHOWN] != true) {
                prefs[KEPT_OPEN_HINT_SHOWN] = true
                first = true
            }
        }
        return first
    }

    /** Used by a standalone ReaderScreen entry point as well as shell restore. */
    suspend fun ensureActiveTab(docId: String, tabLimit: Int, initialTab: ReaderTab): OpenTabResult =
        open(docId, newTab = false, tabLimit = tabLimit, initialTab = initialTab)

    private fun decode(encoded: String): ReaderTabsState = runCatching {
        val root = JSONObject(encoded)
        val array = root.optJSONArray("tabs") ?: JSONArray()
        val tabs = buildList {
            for (index in 0 until array.length()) {
                val row = array.optJSONObject(index) ?: continue
                val id = row.optString("docId").takeIf(String::isNotBlank) ?: continue
                add(
                    ReaderTab(
                        docId = id,
                        page = row.optInt("page", 0).coerceAtLeast(0),
                        zoom = row.optDouble("zoom", 1.0).toFloat().coerceIn(LeafSettings.ZOOM_MIN, LeafSettings.ZOOM_MAX),
                        scrollDir = enumValue(row.optString("scrollDir"), ScrollDir.VERTICAL),
                        findQuery = row.optString("findQuery").takeIf(String::isNotBlank),
                        scrollFraction = row.optDouble("scrollFraction", 0.0).toFloat().coerceIn(0f, 1f),
                        readingTheme = enumValue(row.optString("readingTheme"), ReadingTheme.PAPER),
                        panX = row.optDouble("panX", 0.0).toFloat(),
                        panY = row.optDouble("panY", 0.0).toFloat()
                    )
                )
            }
        }.distinctBy(ReaderTab::docId)
        val active = root.optString("activeDocId").takeIf { id -> tabs.any { it.docId == id } }
            ?: tabs.lastOrNull()?.docId
        ReaderTabsState(tabs, active)
    }.getOrDefault(ReaderTabsState())

    private fun encode(state: ReaderTabsState): String = JSONObject().apply {
        put("activeDocId", state.activeDocId)
        put("tabs", JSONArray().apply {
            state.tabs.forEach { tab ->
                put(JSONObject().apply {
                    put("docId", tab.docId)
                    put("page", tab.page)
                    put("zoom", tab.zoom.toDouble())
                    put("scrollDir", tab.scrollDir.name)
                    put("findQuery", tab.findQuery)
                    put("scrollFraction", tab.scrollFraction.toDouble())
                    put("readingTheme", tab.readingTheme.name)
                    put("panX", tab.panX.toDouble())
                    put("panY", tab.panY.toDouble())
                })
            }
        })
    }.toString()

    private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

    private companion object {
        val TABS = stringPreferencesKey("reader_tabs_v1")
        val KEPT_OPEN_HINT_SHOWN = booleanPreferencesKey("reader_tabs_kept_open_hint_shown")
    }
}

sealed interface OpenTabResult {
    data object Opened : OpenTabResult
    data class LimitReached(val limit: Int) : OpenTabResult
}

data class ClosedTab(
    val tab: ReaderTab,
    val index: Int,
    val previousActiveDocId: String?
)
