package app.leaf.reader.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
fun LeafShell(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val useRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

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
                startDestination = LeafDestination.LIBRARY.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(LeafDestination.LIBRARY.route) { LibraryScreen() }
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
}
