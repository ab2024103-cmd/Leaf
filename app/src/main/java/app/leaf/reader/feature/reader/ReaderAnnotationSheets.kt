package app.leaf.reader.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.leaf.reader.R
import app.leaf.reader.core.model.Bookmark
import app.leaf.reader.core.model.Highlight
import app.leaf.reader.core.model.HighlightColor
import app.leaf.reader.core.ui.components.LeafSheetDivider
import app.leaf.reader.core.ui.components.LeafSheetItem
import app.leaf.reader.core.ui.components.LeafSheetSurface
import app.leaf.reader.core.ui.theme.HighlightSwatches
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.ui.theme.currentReadingPalette
import app.leaf.reader.core.ui.util.shortDate
import app.leaf.reader.core.ui.util.timeAgoText
import app.leaf.reader.core.util.TimeAgo
import app.leaf.reader.core.util.timeAgo

@Composable
internal fun BookmarksSheet(
    bookmarks: List<Bookmark>,
    currentPage: Int,
    pageCount: Int,
    onAddCurrentPage: () -> Unit,
    onJump: (Bookmark) -> Unit,
    onDelete: (String) -> Unit
) {
    val now = System.currentTimeMillis()
    LeafSheetSurface(
        title = stringResource(R.string.reader_bookmarks_title),
        subtitle = stringResource(R.string.reader_bookmarks_subtitle, bookmarks.size)
    ) {
        LeafSheetItem(
            icon = if (bookmarks.any { it.page == currentPage }) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark,
            title = stringResource(
                if (bookmarks.any { it.page == currentPage }) R.string.reader_bookmark_remove_current_page
                else R.string.reader_bookmark_current_page
            ),
            subtitle = stringResource(R.string.reader_page_of, currentPage + 1, pageCount),
            onClick = onAddCurrentPage
        )
        LeafSheetDivider()
        if (bookmarks.isEmpty()) {
            Text(
                text = stringResource(R.string.reader_no_bookmarks),
                style = LeafType.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp)
            )
        } else {
            bookmarks.sortedBy { it.page }.forEach { bookmark ->
                val age = timeAgo(bookmark.createdAt, now)
                BookmarkRow(
                    bookmark = bookmark,
                    age = if (age is TimeAgo.Date) shortDate(age.millis) else timeAgoText(age),
                    onJump = { onJump(bookmark) },
                    onDelete = { onDelete(bookmark.id) }
                )
            }
        }
    }
}

@Composable
private fun BookmarkRow(bookmark: Bookmark, age: String, onJump: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onJump).padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_bookmark_filled), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(bookmark.label, style = LeafType.body.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.reader_bookmark_page_age, bookmark.page + 1, age), style = LeafType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.reader_delete_bookmark, bookmark.page + 1), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun HighlightsSheet(
    highlights: List<Highlight>,
    onJump: (Highlight) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val now = System.currentTimeMillis()
    LeafSheetSurface(
        title = stringResource(R.string.reader_highlights_title),
        subtitle = stringResource(R.string.reader_highlights_subtitle, highlights.size)
    ) {
        if (highlights.isEmpty()) {
            Text(
                text = stringResource(R.string.reader_no_highlights),
                style = LeafType.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp)
            )
        } else {
            highlights.sortedWith(compareBy<Highlight> { it.page }.thenBy { it.createdAt }).forEach { highlight ->
                val age = timeAgo(highlight.createdAt, now)
                HighlightRow(
                    highlight = highlight,
                    age = if (age is TimeAgo.Date) shortDate(age.millis) else timeAgoText(age),
                    onJump = { onJump(highlight) },
                    onDelete = { onDelete(highlight.id) }
                )
            }
            LeafSheetDivider()
            LeafSheetItem(
                icon = R.drawable.ic_delete,
                title = stringResource(R.string.reader_clear_all_highlights),
                danger = true,
                onClick = onClearAll
            )
        }
    }
}

@Composable
private fun HighlightRow(highlight: Highlight, age: String, onJump: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onJump).padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val swatch = HighlightSwatches.getValue(highlight.color).fillFor(currentReadingPalette)
            Box(Modifier.size(12.dp).background(swatch, CircleShape))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reader_highlight_page_line, highlight.page + 1, age),
                    style = LeafType.supporting,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.reader_highlight_quote, highlight.text),
                    style = LeafType.body,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.reader_delete_highlight), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}
