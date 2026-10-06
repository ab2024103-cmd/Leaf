package app.leaf.reader.feature.search

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.leaf.reader.R
import app.leaf.reader.core.ui.components.LeafEmptyState
import app.leaf.reader.core.ui.components.LeafScreen
import app.leaf.reader.core.ui.util.annotated
import app.leaf.reader.core.ui.util.isScrolled

/**
 * Search (§6.4). The mockup gives this screen no subtitle.
 *
 * M1 renders the scaffold with the blank-query empty state. The search bar, the scope
 * row, the filter panel and the results land in M4.
 */
@Composable
fun SearchScreen() {
    val scrollState = rememberScrollState()
    LeafScreen(
        title = stringResource(R.string.nav_search),
        scrollState = scrollState,
        scrolled = isScrolled(scrollState)
    ) {
        LeafEmptyState(
            icon = R.drawable.ic_nav_search,
            title = stringResource(R.string.empty_search_title),
            message = annotated(stringResource(R.string.empty_search_message))
        )
    }
}
