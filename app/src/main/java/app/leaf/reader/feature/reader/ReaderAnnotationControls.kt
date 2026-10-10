package app.leaf.reader.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.leaf.reader.R
import app.leaf.reader.core.model.Highlight
import app.leaf.reader.core.model.HighlightColor
import app.leaf.reader.core.ui.theme.HighlightSwatches
import app.leaf.reader.core.ui.theme.LeafType

@Composable
internal fun ReaderToolbar(
    state: ReaderContentState,
    onLayout: () -> Unit,
    onFind: () -> Unit,
    onZoom: (Int) -> Unit,
    onFullScreen: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onHighlightMenu: () -> Unit,
    onHighlightColor: (HighlightColor) -> Unit,
    onHighlights: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        val showZoom = maxWidth >= 392.dp
        val showZoomValue = maxWidth >= 460.dp
        val showHighlightsList = maxWidth >= 580.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            HighlightTrigger(
                state = state,
                wide = maxWidth >= 600.dp,
                onToggle = onHighlightMenu,
                onClose = onHighlightMenu,
                onSelect = onHighlightColor
            )
            ReaderControl(
                icon = R.drawable.ic_undo,
                description = stringResource(R.string.reader_undo),
                enabled = state.canUndoAnnotation,
                onClick = onUndo
            )
            ReaderControl(
                icon = R.drawable.ic_redo,
                description = stringResource(R.string.reader_redo),
                enabled = state.canRedoAnnotation,
                onClick = onRedo
            )
            ReaderControl(R.drawable.ic_nav_search, stringResource(R.string.reader_find_hint), onFind)
            ReaderControl(R.drawable.ic_view_layout, stringResource(R.string.reader_view_layout_title), onLayout)
            Spacer(Modifier.weight(1f))
            if (showZoom) {
                ReaderControl(R.drawable.ic_zoom_out, stringResource(R.string.reader_zoom_out)) { onZoom(-1) }
                if (showZoomValue) {
                    Text(
                        stringResource(R.string.reader_zoom_value, (state.zoom * 100).toInt()),
                        style = LeafType.supporting,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
                ReaderControl(R.drawable.ic_zoom_in, stringResource(R.string.reader_zoom_in)) { onZoom(1) }
            }
            if (showHighlightsList) {
                ReaderControl(R.drawable.ic_document, stringResource(R.string.reader_highlights_title), onHighlights)
            }
            ReaderControl(
                if (state.isFullScreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen,
                stringResource(if (state.isFullScreen) R.string.reader_exit_fullscreen else R.string.reader_fullscreen),
                onFullScreen
            )
        }
    }
}

@Composable
private fun HighlightTrigger(
    state: ReaderContentState,
    wide: Boolean,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    onSelect: (HighlightColor) -> Unit
) {
    Box {
        Surface(
            onClick = onToggle,
            modifier = Modifier.height(48.dp).widthIn(min = if (wide) 100.dp else 44.dp),
            shape = RoundedCornerShape(14.dp),
            color = if (state.highlightMenuExpanded) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (wide) 10.dp else 7.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_highlight),
                    contentDescription = stringResource(R.string.reader_highlight_colour),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    Modifier.size(13.dp).background(
                        HighlightSwatches.getValue(state.highlightColor).fillFor(app.leaf.reader.core.ui.theme.currentReadingPalette),
                        CircleShape
                    )
                )
                if (wide) {
                    Text(
                        stringResource(R.string.reader_highlight_label),
                        style = LeafType.chipText,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        DropdownMenu(
            expanded = state.highlightMenuExpanded,
            onDismissRequest = onClose,
            modifier = Modifier.widthIn(min = 236.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.reader_highlight_colour_heading),
                        style = LeafType.chipLabel.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.reader_close_highlight_menu),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HighlightColor.entries.forEach { color ->
                        HighlightMenuColor(
                            color = color,
                            selected = color == state.highlightColor,
                            onClick = { onSelect(color) }
                        )
                    }
                }
                Text(
                    text = stringResource(
                        if (state.selection != null) R.string.reader_highlight_selected_hint
                        else R.string.reader_highlight_empty_hint
                    ),
                    style = LeafType.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HighlightMenuColor(color: HighlightColor, selected: Boolean, onClick: () -> Unit) {
    val label = stringResource(color.labelRes())
    Column(
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier.size(22.dp).background(
                HighlightSwatches.getValue(color).fillFor(app.leaf.reader.core.ui.theme.currentReadingPalette),
                CircleShape
            )
        )
        Text(
            text = label,
            style = LeafType.chipLabel.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
internal fun HighlightContextBar(
    highlight: Highlight,
    onColor: (HighlightColor) -> Unit,
    onRemove: () -> Unit,
    onCopy: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HighlightColor.entries.forEach { color ->
            val selected = color == highlight.color
            Surface(
                onClick = { onColor(color) },
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(22.dp).background(
                            HighlightSwatches.getValue(color).fillFor(app.leaf.reader.core.ui.theme.currentReadingPalette),
                            CircleShape
                        )
                    )
                }
            }
        }
        ContextIconButton(R.drawable.ic_delete, stringResource(R.string.reader_remove_highlight), onRemove)
        ContextIconButton(R.drawable.ic_copy, stringResource(R.string.reader_copy_highlight), onCopy)
        ContextIconButton(R.drawable.ic_close, stringResource(R.string.reader_done), onDone)
    }
}

@Composable
private fun ReaderControl(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp)) {
        Icon(
            painterResource(icon),
            contentDescription = description,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun ContextIconButton(icon: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Icon(
            painterResource(icon),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun HighlightColor.labelRes(): Int = when (this) {
    HighlightColor.YELLOW -> R.string.reader_colour_yellow
    HighlightColor.GREEN -> R.string.reader_colour_green
    HighlightColor.BLUE -> R.string.reader_colour_blue
    HighlightColor.PINK -> R.string.reader_colour_pink
    HighlightColor.ORANGE -> R.string.reader_colour_orange
}
