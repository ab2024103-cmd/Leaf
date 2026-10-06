package app.leaf.reader.feature.library

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.leaf.reader.R
import app.leaf.reader.core.ui.components.LeafEmptyState
import app.leaf.reader.core.ui.components.LeafScreen
import app.leaf.reader.core.ui.util.isScrolled

/**
 * Library — the default destination (§6.1).
 *
 * M1 renders the structural scaffold: brand app bar and the empty state. Filter rails,
 * document rows, the sort sheet and the FAB land in M2 with their features.
 */
@Composable
fun LibraryScreen() {
    val scrollState = rememberScrollState()
    LeafScreen(
        title = stringResource(R.string.app_name),
        subtitle = stringResource(R.string.library_subtitle_empty),
        showBrand = true,
        scrollState = scrollState,
        scrolled = isScrolled(scrollState)
    ) {
        LeafEmptyState(
            icon = R.drawable.ic_nav_library,
            title = stringResource(R.string.empty_library_title),
            message = stringResource(R.string.empty_library_message)
        )
    }
}
