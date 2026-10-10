package app.leaf.reader.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.leaf.reader.R
import app.leaf.reader.core.data.prefs.ClosedTab
import app.leaf.reader.core.ui.components.LeafConfirmDialogOverlay
import app.leaf.reader.core.ui.components.LeafNavBar
import app.leaf.reader.core.ui.components.LeafNavRail
import app.leaf.reader.core.ui.components.LeafSnackbar
import app.leaf.reader.feature.favorites.FavoritesScreen
import app.leaf.reader.feature.library.LibraryScreen
import app.leaf.reader.feature.reader.OpenDocumentsSheet
import app.leaf.reader.feature.reader.ReaderScreen
import app.leaf.reader.feature.reader.ReaderTabsEvent
import app.leaf.reader.feature.reader.ReaderTabsUiState
import app.leaf.reader.feature.reader.ReaderTabsViewModel
import app.leaf.reader.feature.recents.RecentsScreen
import app.leaf.reader.feature.search.SearchScreen
import app.leaf.reader.feature.settings.SettingsScreen
import kotlinx.coroutines.flow.flowOf
import org.koin.androidx.compose.koinViewModel

/**
 * The shell: one top-level graph for the five destinations (§3). Compact widths get a
 * bottom navigation bar; medium and expanded widths get an 84 dp rail (§5).
 *
 * The reader is deliberately not part of this graph — it overlays the shell as a
 * full-screen surface so each open document keeps its own state (§3).
 */
@Composable
fun LeafShell(
    windowSizeClass: WindowSizeClass,
    /** Library is the default destination; the screenshot tests start on each one. */
    startDestination: LeafDestination = LeafDestination.LIBRARY,
    externalDocumentId: String? = null,
    onExternalDocumentConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val context = LocalContext.current

    // Some destination screenshot tests intentionally use a plain Application with no Koin.
    // Those destinations do not need shell tab state, so do not resolve a ViewModel there.
    val needsShellTabs = startDestination != LeafDestination.FAVORITES &&
        startDestination != LeafDestination.RECENTS &&
        startDestination != LeafDestination.SETTINGS
    val tabsViewModel: ReaderTabsViewModel? = if (needsShellTabs) koinViewModel() else null
    val emptyTabsFlow = remember { flowOf(ReaderTabsUiState(isLoaded = true)) }
    val tabsFlow = remember(tabsViewModel) { tabsViewModel?.state ?: emptyTabsFlow }
    val tabsState by tabsFlow.collectAsStateWithLifecycle(initialValue = ReaderTabsUiState())

    var readerVisible by rememberSaveable { mutableStateOf(false) }
    var readerStartDocumentId by rememberSaveable { mutableStateOf<String?>(null) }
    var readerStartPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var readerInitialFindQuery by rememberSaveable { mutableStateOf<String?>(null) }
    var tabsRestored by rememberSaveable { mutableStateOf(false) }
    var openDocumentsSheet by rememberSaveable { mutableStateOf(false) }
    var confirmCloseAll by rememberSaveable { mutableStateOf(false) }
    var pickingNewDocument by rememberSaveable { mutableStateOf(false) }
    var returnToReaderAfterPick by rememberSaveable { mutableStateOf(false) }
    var shellMessageId by remember { mutableIntStateOf(0) }
    var shellMessage by remember { mutableStateOf<ShellMessage?>(null) }

    fun showShellMessage(text: String, closedTab: ClosedTab? = null) {
        shellMessageId += 1
        shellMessage = ShellMessage(shellMessageId, text, closedTab)
    }

    fun navigateTo(destination: LeafDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun cancelPickingNewDocument() {
        pickingNewDocument = false
        if (returnToReaderAfterPick && tabsState.tabs.activeDocId != null) {
            readerVisible = true
        }
        returnToReaderAfterPick = false
    }

    LaunchedEffect(tabsState.isLoaded, tabsState.tabs.activeDocId) {
        if (!tabsState.isLoaded) return@LaunchedEffect
        if (!tabsRestored) {
            tabsRestored = true
            readerVisible = tabsState.tabs.activeDocId != null
        } else if (tabsState.tabs.activeDocId == null) {
            readerVisible = false
        }
    }

    LaunchedEffect(tabsViewModel) {
        val viewModel = tabsViewModel ?: return@LaunchedEffect
        viewModel.events.collect { event ->
            when (event) {
                is ReaderTabsEvent.OpenReader -> {
                    readerStartDocumentId = event.documentId
                    readerStartPage = event.page
                    readerInitialFindQuery = event.findQuery
                    readerVisible = true
                    event.message?.let { showShellMessage(it) }
                    openDocumentsSheet = false
                    pickingNewDocument = false
                    returnToReaderAfterPick = false
                }
                is ReaderTabsEvent.TabLimitReached -> {
                    showShellMessage(
                        context.getString(R.string.reader_tabs_limit_pick, event.limit)
                    )
                }
                is ReaderTabsEvent.TabClosed -> {
                    showShellMessage(event.message, event.closed)
                    if (event.noTabsRemaining) {
                        readerVisible = false
                        navigateTo(LeafDestination.LIBRARY)
                    }
                }
                is ReaderTabsEvent.KeptOpen -> {
                    val message = context.resources.getQuantityString(
                        R.plurals.reader_tabs_kept_open,
                        event.count,
                        event.count
                    )
                    showShellMessage(message)
                }
                ReaderTabsEvent.TabRestored -> {
                    showShellMessage(context.getString(R.string.reader_tabs_restored))
                }
                ReaderTabsEvent.AllTabsClosed -> {
                    readerVisible = false
                    navigateTo(LeafDestination.LIBRARY)
                    readerStartDocumentId = null
                    readerStartPage = null
                    readerInitialFindQuery = null
                    showShellMessage(context.getString(R.string.reader_tabs_all_closed))
                }
            }
        }
    }

    LaunchedEffect(externalDocumentId, tabsViewModel) {
        externalDocumentId?.let { documentId ->
            tabsViewModel?.openDocument(documentId)
            onExternalDocumentConsumed()
        }
    }

    BackHandler(enabled = pickingNewDocument) { cancelPickingNewDocument() }

    val useRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    // §5: one column on compact, two on medium, three on expanded.
    val libraryColumns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 1
        WindowWidthSizeClass.Medium -> 2
        else -> 3
    }
    val activeDocumentId = tabsState.tabs.activeDocId
    val activeTabCount = tabsState.tabs.tabs.size

    fun beginNewTabPick() {
        returnToReaderAfterPick = readerVisible
        readerVisible = false
        openDocumentsSheet = false
        pickingNewDocument = true
        navigateTo(LeafDestination.LIBRARY)
    }

    fun pickNewTabFromReader() {
        if (activeTabCount >= tabsState.tabLimit) {
            showShellMessage(context.getString(R.string.reader_tabs_limit_pick, tabsState.tabLimit))
        } else {
            beginNewTabPick()
        }
    }

    fun pickNewTabFromSheet() {
        if (activeTabCount >= tabsState.tabLimit) {
            showShellMessage(context.getString(R.string.reader_tabs_limit_sheet, tabsState.tabLimit))
        } else {
            beginNewTabPick()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                // Keeps chrome clear of the status bar and the gesture/navigation bar (§5).
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Row(modifier = Modifier.weight(1f)) {
                if (useRail) {
                    LeafNavRail(
                        selected = LeafDestination.fromRoute(currentRoute),
                        onSelect = ::navigateTo
                    )
                }
                NavHost(
                    navController = navController,
                    startDestination = startDestination.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(LeafDestination.LIBRARY.route) {
                        LibraryScreen(
                            onOpenDocument = { documentId ->
                                readerStartPage = null
                                readerInitialFindQuery = null
                                tabsViewModel?.openDocument(documentId)
                            },
                            columns = libraryColumns,
                            openTabCount = activeTabCount,
                            onOpenDocuments = { openDocumentsSheet = true },
                            pickingNewTab = pickingNewDocument,
                            tabsInUse = activeTabCount,
                            tabLimit = tabsState.tabLimit,
                            onCancelPickingNewTab = ::cancelPickingNewDocument,
                            onOpenNewTab = { documentId ->
                                readerStartPage = null
                                readerInitialFindQuery = null
                                tabsViewModel?.openDocument(documentId, newTab = true)
                            }
                        )
                    }
                    composable(LeafDestination.RECENTS.route) { RecentsScreen() }
                    composable(LeafDestination.FAVORITES.route) { FavoritesScreen() }
                    composable(LeafDestination.SEARCH.route) {
                        SearchScreen(
                            onOpenDocument = { documentId ->
                                readerStartPage = null
                                readerInitialFindQuery = null
                                tabsViewModel?.openDocument(documentId)
                            },
                            onOpenFileSearch = { documentId, pageIndex, query ->
                                readerStartPage = pageIndex
                                readerInitialFindQuery = query
                                tabsViewModel?.openDocument(documentId, page = pageIndex, findQuery = query)
                            }
                        )
                    }
                    composable(LeafDestination.SETTINGS.route) { SettingsScreen() }
                }
            }
            if (!useRail) {
                LeafNavBar(
                    selected = LeafDestination.fromRoute(currentRoute),
                    onSelect = ::navigateTo
                )
            }
        }

        val startPageForActive = readerStartPage.takeIf { readerStartDocumentId == activeDocumentId }
        val queryForActive = readerInitialFindQuery.takeIf { readerStartDocumentId == activeDocumentId }
        if (readerVisible && activeDocumentId != null) {
            ReaderScreen(
                documentId = activeDocumentId,
                initialPage = startPageForActive,
                initialFindQuery = queryForActive,
                onClose = {
                    readerVisible = false
                    tabsViewModel?.onReaderLeft()
                },
                openTabCount = activeTabCount,
                onOpenTabs = { openDocumentsSheet = true },
                onPickNewTab = ::pickNewTabFromReader,
                modifier = Modifier.fillMaxSize().zIndex(1f)
            )
        }

        if (openDocumentsSheet) {
            OpenDocumentsSheet(
                state = tabsState,
                onDismiss = { openDocumentsSheet = false },
                onActivate = { documentId -> tabsViewModel?.activate(documentId) },
                onCloseTab = { documentId -> tabsViewModel?.closeTab(documentId) },
                onPickNewDocument = ::pickNewTabFromSheet,
                onCloseAll = { confirmCloseAll = true },
                modifier = Modifier.fillMaxSize().zIndex(2f)
            )
        }

        if (confirmCloseAll) {
            Box(Modifier.fillMaxSize().zIndex(3f)) {
                LeafConfirmDialogOverlay(
                    title = stringResource(R.string.reader_tabs_close_all_title),
                    body = stringResource(R.string.reader_tabs_close_all_body),
                    confirmLabel = stringResource(R.string.reader_tabs_close_all_confirm),
                    onConfirm = {
                        confirmCloseAll = false
                        openDocumentsSheet = false
                        readerVisible = false
                        tabsViewModel?.closeAll()
                    },
                    onDismiss = { confirmCloseAll = false }
                )
            }
        }

        if (!confirmCloseAll) shellMessage?.let { message ->
            LeafSnackbar(
                message = message.text,
                actionLabel = if (message.closedTab != null) stringResource(R.string.action_undo) else null,
                onAction = {
                    message.closedTab?.let { tabsViewModel?.undoClose(it) }
                    shellMessage = null
                },
                onDismiss = { if (shellMessage?.id == message.id) shellMessage = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (useRail) 8.dp else 70.dp)
                    .zIndex(4f)
            )
        }
    }
}

private data class ShellMessage(
    val id: Int,
    val text: String,
    val closedTab: ClosedTab? = null
)
