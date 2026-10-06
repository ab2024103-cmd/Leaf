package app.leaf.reader.feature.recents

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.leaf.reader.R
import app.leaf.reader.core.ui.components.LeafEmptyState
import app.leaf.reader.core.ui.components.LeafScreen
import app.leaf.reader.core.ui.util.isScrolled

/**
 * Recents — reading history (§6.2).
 *
 * M1 renders the scaffold with the empty state. The hero card, the continue grid and
 * the today/earlier groups land in M2 alongside the library rows.
 */
@Composable
fun RecentsScreen() {
    val scrollState = rememberScrollState()
    LeafScreen(
        title = stringResource(R.string.nav_recents),
        subtitle = stringResource(R.string.recents_subtitle_empty),
        scrollState = scrollState,
        scrolled = isScrolled(scrollState)
    ) {
        LeafEmptyState(
            icon = R.drawable.ic_nav_recents,
            title = stringResource(R.string.empty_recents_title),
            message = stringResource(R.string.empty_recents_message)
        )
    }
}
