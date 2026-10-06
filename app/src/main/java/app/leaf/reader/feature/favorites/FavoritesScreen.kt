package app.leaf.reader.feature.favorites

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.leaf.reader.R
import app.leaf.reader.core.ui.components.LeafEmptyState
import app.leaf.reader.core.ui.components.LeafScreen
import app.leaf.reader.core.ui.util.isScrolled

/**
 * Favorites — starred documents (§6.3).
 *
 * M1 renders the scaffold with the empty state. The rows and the screen's own sort
 * control (Name · Date added · Last opened) land in M2.
 */
@Composable
fun FavoritesScreen() {
    val scrollState = rememberScrollState()
    LeafScreen(
        title = stringResource(R.string.nav_favorites),
        subtitle = stringResource(R.string.favorites_subtitle_empty),
        scrollState = scrollState,
        scrolled = isScrolled(scrollState)
    ) {
        LeafEmptyState(
            icon = R.drawable.ic_nav_favorites,
            title = stringResource(R.string.empty_favorites_title),
            message = stringResource(R.string.empty_favorites_message)
        )
    }
}
