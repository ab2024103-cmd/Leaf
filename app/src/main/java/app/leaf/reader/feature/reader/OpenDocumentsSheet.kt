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
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.ReaderTab
import app.leaf.reader.core.ui.components.FileTypeIcon
import app.leaf.reader.core.ui.components.LeafSheetDivider
import app.leaf.reader.core.ui.components.LeafSheetItem
import app.leaf.reader.core.ui.components.LeafSheetOverlay
import app.leaf.reader.core.ui.components.LeafSheetSurface
import app.leaf.reader.core.ui.theme.LeafMetrics
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.ui.util.shortDate
import app.leaf.reader.core.ui.util.timeAgoText
import app.leaf.reader.core.util.TimeAgo
import app.leaf.reader.core.util.timeAgo

/** Ordered tab manager; no tab strip is added to the reader viewport. */
@Composable
fun OpenDocumentsSheet(
    state: ReaderTabsUiState,
    onDismiss: () -> Unit,
    onActivate: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onPickNewDocument: () -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    LeafSheetOverlay(onDismiss = onDismiss, modifier = modifier) {
        LeafSheetSurface(
            title = stringResource(R.string.action_open_documents),
            subtitle = stringResource(R.string.reader_tabs_summary, state.tabs.tabs.size, state.tabLimit)
        ) {
            if (state.tabs.tabs.isEmpty()) {
                Text(
                    text = stringResource(R.string.reader_tabs_empty),
                    style = LeafType.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp)
                )
            } else {
                state.tabs.tabs.forEach { tab ->
                    val document = state.documents[tab.docId]
                    if (document != null) {
                        OpenTabRow(
                            tab = tab,
                            document = document,
                            active = tab.docId == state.tabs.activeDocId,
                            now = now,
                            onActivate = { onActivate(tab.docId) },
                            onClose = { onCloseTab(tab.docId) }
                        )
                    }
                }
            }
            LeafSheetItem(
                icon = R.drawable.ic_tab_new,
                title = stringResource(R.string.reader_tabs_open_new),
                subtitle = stringResource(R.string.reader_tabs_open_new_sub),
                onClick = onPickNewDocument
            )
            if (state.tabs.tabs.isNotEmpty()) {
                LeafSheetDivider()
                LeafSheetItem(
                    icon = R.drawable.ic_delete,
                    title = stringResource(
                        if (state.tabs.tabs.size == 1) R.string.reader_tabs_close_one
                        else R.string.reader_tabs_close_all
                    ),
                    subtitle = if (state.tabs.tabs.size == 1) {
                        stringResource(R.string.reader_tabs_close_all_count_one)
                    } else {
                        stringResource(R.string.reader_tabs_close_all_count, state.tabs.tabs.size)
                    },
                    danger = true,
                    onClick = onCloseAll
                )
            }
        }
    }
}

@Composable
private fun OpenTabRow(
    tab: ReaderTab,
    document: Document,
    active: Boolean,
    now: Long,
    onActivate: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .background(
                color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(LeafMetrics.cardRadius)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onActivate).padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileTypeIcon(type = document.type)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = LeafType.body.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val timeLabel = document.lastOpened?.let { timestamp ->
                    val age = timeAgo(timestamp, now)
                    if (age is TimeAgo.Date) shortDate(age.millis) else timeAgoText(age)
                } ?: stringResource(R.string.value_never)
                Text(
                    text = stringResource(
                        R.string.reader_tabs_page_line,
                        (tab.page + 1).coerceAtMost(document.pageCount.coerceAtLeast(1)),
                        document.pageCount,
                        timeLabel
                    ),
                    style = LeafType.listMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (active) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(9.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Text(
                        text = stringResource(R.string.reader_tabs_active),
                        style = LeafType.chipLabel,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.reader_tabs_close_accessibility, document.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
