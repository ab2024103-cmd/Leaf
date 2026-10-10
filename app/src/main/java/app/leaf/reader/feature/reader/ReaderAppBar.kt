package app.leaf.reader.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.leaf.reader.R
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.ui.theme.LeafType

@Composable
internal fun ReaderAppBar(
    state: ReaderContentState,
    openTabCount: Int,
    onBack: () -> Unit,
    onOpenTabs: () -> Unit,
    onToggleBookmark: () -> Unit,
    onLayout: () -> Unit,
    onBookmarks: () -> Unit,
    onHighlights: () -> Unit,
    onJump: () -> Unit,
    onPickNewDocument: () -> Unit,
    onClearPageHighlights: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.reader_back_to_library))
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                state.document?.name.orEmpty(),
                style = LeafType.listTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.reader_subtitle,
                    state.pageCount,
                    if (state.scrollDir == ScrollDir.VERTICAL) stringResource(R.string.reader_vertical_lower)
                    else stringResource(R.string.reader_horizontal_lower)
                ),
                style = LeafType.listMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
        OpenTabsButton(count = openTabCount, onClick = onOpenTabs)
        IconButton(onClick = onToggleBookmark) {
            Icon(
                painter = painterResource(if (state.isCurrentPageBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                contentDescription = stringResource(
                    if (state.isCurrentPageBookmarked) R.string.reader_bookmark_remove
                    else R.string.reader_bookmark_add
                ),
                tint = if (state.isCurrentPageBookmarked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        androidx.compose.foundation.layout.Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.reader_more_actions))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_open_new_tab)) },
                    onClick = { menuExpanded = false; onPickNewDocument() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_view_layout_title)) },
                    onClick = { menuExpanded = false; onLayout() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_jump_title)) },
                    onClick = { menuExpanded = false; onJump() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_bookmarks_title)) },
                    onClick = { menuExpanded = false; onBookmarks() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_highlights_title)) },
                    onClick = { menuExpanded = false; onHighlights() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_clear_page_highlights)) },
                    onClick = { menuExpanded = false; onClearPageHighlights() }
                )
            }
        }
    }
}
