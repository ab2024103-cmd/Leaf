package app.leaf.reader.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.leaf.reader.core.ui.components.LeafNavBar
import app.leaf.reader.core.ui.components.LeafNavRail
import app.leaf.reader.feature.favorites.FavoritesScreen
import app.leaf.reader.feature.library.LibraryScreen
import app.leaf.reader.feature.recents.RecentsScreen
import app.leaf.reader.feature.reader.ReaderScreen
import app.leaf.reader.feature.search.SearchScreen
import app.leaf.reader.feature.settings.SettingsScreen

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
    var readerDocumentId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(externalDocumentId) {
        externalDocumentId?.let {
            readerDocumentId = it
            onExternalDocumentConsumed()
        }
    }
    val useRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    // §5: one column on compact, two on medium, three on expanded.
    val libraryColumns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 1
        WindowWidthSizeClass.Medium -> 2
        else -> 3
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
                    onSelect = { destination ->
                        navController.navigate(destination.route) {
                            // One entry per destination; state is kept per screen (§8.4).
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            NavHost(
                navController = navController,
                startDestination = startDestination.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(LeafDestination.LIBRARY.route) {
                    LibraryScreen(
                        onOpenDocument = { readerDocumentId = it },
                        columns = libraryColumns
                    )
                }
                composable(LeafDestination.RECENTS.route) { RecentsScreen() }
                composable(LeafDestination.FAVORITES.route) { FavoritesScreen() }
                composable(LeafDestination.SEARCH.route) { SearchScreen() }
                composable(LeafDestination.SETTINGS.route) { SettingsScreen() }
            }
        }
        if (!useRail) {
            LeafNavBar(
                selected = LeafDestination.fromRoute(currentRoute),
                onSelect = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
      }
      readerDocumentId?.let { documentId ->
          ReaderScreen(
              documentId = documentId,
              onClose = { readerDocumentId = null },
              modifier = Modifier.fillMaxSize().zIndex(1f)
          )
      }
    }
}
