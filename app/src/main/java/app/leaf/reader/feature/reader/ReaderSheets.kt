package app.leaf.reader.feature.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.leaf.reader.R
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.model.Typeface
import app.leaf.reader.core.ui.components.LeafSegmented
import app.leaf.reader.core.ui.components.LeafSheetOverlay
import app.leaf.reader.core.ui.components.LeafSheetSurface
import app.leaf.reader.core.ui.theme.LeafType
import kotlin.math.roundToInt

@Composable
fun ReaderSheetHost(
    state: ReaderContentState,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit,
    onDirection: (ScrollDir) -> Unit,
    onTheme: (ReadingTheme) -> Unit,
    onZoom: (Float) -> Unit,
    onReflow: (Boolean) -> Unit,
    onTypeface: (Typeface) -> Unit,
    onPageNumbers: (Boolean) -> Unit,
    onOrientation: () -> Unit,
    onFullScreen: () -> Unit,
    onShareDocument: () -> Unit,
    onSharePageImage: () -> Unit,
    onShareSelection: () -> Unit,
    onCopyFilePath: () -> Unit,
    onCopyPageText: () -> Unit,
    onAddCurrentBookmark: () -> Unit,
    onJumpBookmark: (app.leaf.reader.core.model.Bookmark) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onJumpHighlight: (app.leaf.reader.core.model.Highlight) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onRequestClearAllHighlights: () -> Unit,
    onClearAllHighlights: () -> Unit
) {
    when (state.sheet) {
        ReaderSheet.NONE -> Unit
        ReaderSheet.JUMP_TO_PAGE -> LeafSheetOverlay(onDismiss = onDismiss) {
            JumpToPageSheet(
                name = state.document?.name.orEmpty(),
                pageCount = state.pageCount,
                currentPage = state.pageIndex,
                onGo = onJump
            )
        }
        ReaderSheet.VIEW_LAYOUT -> LeafSheetOverlay(onDismiss = onDismiss) {
            ViewLayoutSheet(
                state = state,
                onDirection = onDirection,
                onTheme = onTheme,
                onZoom = onZoom,
                onReflow = onReflow,
                onTypeface = onTypeface,
                onPageNumbers = onPageNumbers,
                onOrientation = onOrientation,
                onFullScreen = onFullScreen
            )
        }
        ReaderSheet.BOOKMARKS -> LeafSheetOverlay(onDismiss = onDismiss) {
            BookmarksSheet(
                bookmarks = state.bookmarks,
                currentPage = state.pageIndex,
                pageCount = state.pageCount,
                onAddCurrentPage = onAddCurrentBookmark,
                onJump = onJumpBookmark,
                onDelete = onDeleteBookmark
            )
        }
        ReaderSheet.HIGHLIGHTS -> LeafSheetOverlay(onDismiss = onDismiss) {
            HighlightsSheet(
                highlights = state.highlights,
                onJump = onJumpHighlight,
                onDelete = onDeleteHighlight,
                onClearAll = onRequestClearAllHighlights
            )
        }
        ReaderSheet.SHARE -> LeafSheetOverlay(onDismiss = onDismiss) {
            ShareSheet(
                hasSelection = !state.selection?.text.isNullOrBlank(),
                onShareDocument = onShareDocument,
                onSharePageImage = onSharePageImage,
                onShareSelection = onShareSelection,
                onCopyFilePath = onCopyFilePath,
                onCopyPageText = onCopyPageText
            )
        }
        ReaderSheet.CONFIRM_CLEAR_HIGHLIGHTS -> app.leaf.reader.core.ui.components.LeafConfirmDialogOverlay(
            title = stringResource(R.string.reader_clear_highlights_title),
            body = stringResource(R.string.reader_clear_highlights_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { onClearAllHighlights(); onDismiss() },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun JumpToPageSheet(
    name: String,
    pageCount: Int,
    currentPage: Int,
    onGo: (Int) -> Unit
) {
    var pageInput by remember(currentPage) { mutableStateOf((currentPage + 1).toString()) }
    val maxPage = pageCount.coerceAtLeast(1)
    val safePage = (pageInput.toIntOrNull() ?: (currentPage + 1)).coerceIn(1, maxPage)
    LeafSheetSurface(
        title = stringResource(R.string.reader_jump_title),
        subtitle = name
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { pageInput = (safePage - 1).coerceAtLeast(1).toString() }) { Text("−") }
                Text(
                    text = stringResource(R.string.reader_jump_display, safePage, maxPage),
                    style = LeafType.sheetTitle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(top = 12.dp)
                )
                TextButton(onClick = { pageInput = (safePage + 1).coerceAtMost(maxPage).toString() }) { Text("+") }
            }
            if (maxPage > 1) {
                Slider(
                    value = safePage.toFloat(),
                    onValueChange = { pageInput = it.toInt().coerceIn(1, maxPage).toString() },
                    valueRange = 1f..maxPage.toFloat()
                )
            }
            OutlinedTextField(
                value = pageInput,
                onValueChange = { raw -> pageInput = raw.filter(Char::isDigit).take(9) },
                label = { Text(stringResource(R.string.reader_page_number)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.reader_quick_jumps), style = LeafType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val quickPages = listOf(
                    R.string.reader_first to 1,
                    R.string.reader_quarter to ((maxPage - 1) * 0.25f).roundToInt() + 1,
                    R.string.reader_half to ((maxPage - 1) * 0.5f).roundToInt() + 1,
                    R.string.reader_three_quarters to ((maxPage - 1) * 0.75f).roundToInt() + 1,
                    R.string.reader_last to maxPage
                ).distinctBy { it.second }
                quickPages.forEach { (label, target) ->
                    FilterChip(
                        selected = safePage == target,
                        onClick = { pageInput = target.toString() },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
            Button(
                onClick = { onGo(safePage - 1) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.reader_jump_go, safePage))
            }
        }
    }
}

@Composable
private fun ViewLayoutSheet(
    state: ReaderContentState,
    onDirection: (ScrollDir) -> Unit,
    onTheme: (ReadingTheme) -> Unit,
    onZoom: (Float) -> Unit,
    onReflow: (Boolean) -> Unit,
    onTypeface: (Typeface) -> Unit,
    onPageNumbers: (Boolean) -> Unit,
    onOrientation: () -> Unit,
    onFullScreen: () -> Unit
) {
    LeafSheetSurface(
        title = stringResource(R.string.reader_view_layout_title),
        subtitle = stringResource(R.string.reader_view_layout_subtitle)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.reader_theme_label), style = LeafType.body, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ReadingTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = state.readingTheme == theme,
                        onClick = { onTheme(theme) },
                        label = { Text(stringResource(theme.labelRes())) }
                    )
                }
            }
            Text(stringResource(R.string.reader_scroll_label), style = LeafType.body, fontWeight = FontWeight.SemiBold)
            LeafSegmented(
                labels = listOf(
                    stringResource(R.string.reader_vertical),
                    stringResource(R.string.reader_horizontal)
                ),
                selectedIndex = if (state.scrollDir == ScrollDir.VERTICAL) 0 else 1,
                onSelect = { onDirection(if (it == 0) ScrollDir.VERTICAL else ScrollDir.HORIZONTAL) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.reader_reflow), style = LeafType.body, fontWeight = FontWeight.SemiBold)
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Switch(checked = state.reflow, onCheckedChange = onReflow)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_page_numbers), style = LeafType.body, fontWeight = FontWeight.SemiBold)
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Switch(checked = state.showPageNumbers, onCheckedChange = onPageNumbers)
            }
            if (state.orientationLock) {
                // Stored per document; the lock setting decides whether it is applied (§6.6, §6.5).
                val orientation = state.document?.orientation ?: app.leaf.reader.core.model.Orientation.AUTO
                val orientationValue = stringResource(orientationLabel(orientation))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onOrientation)
                        .semantics { stateDescription = orientationValue }
                        .padding(vertical = 12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.reader_orientation_label), style = LeafType.body, fontWeight = FontWeight.SemiBold)
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    Text(orientationValue, style = LeafType.supporting, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Text(stringResource(R.string.reader_typeface), style = LeafType.body, fontWeight = FontWeight.SemiBold)
            LeafSegmented(
                labels = listOf(
                    stringResource(R.string.reader_typeface_serif),
                    stringResource(R.string.reader_typeface_sans)
                ),
                selectedIndex = if (state.typeface == Typeface.SERIF) 0 else 1,
                onSelect = { onTypeface(if (it == 0) Typeface.SERIF else Typeface.SANS) }
            )
            Text(stringResource(R.string.reader_zoom_label, (state.zoom * 100).toInt()), style = LeafType.body, fontWeight = FontWeight.SemiBold)
            Slider(
                value = state.zoom,
                onValueChange = onZoom,
                valueRange = 0.7f..2.5f
            )
            TextButton(onClick = { onZoom(1f) }) { Text(stringResource(R.string.reader_zoom_reset)) }
            Button(onClick = onFullScreen, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.isFullScreen) R.string.reader_exit_fullscreen else R.string.reader_fullscreen))
            }
        }
    }
}

@Composable
private fun ShareSheet(
    hasSelection: Boolean,
    onShareDocument: () -> Unit,
    onSharePageImage: () -> Unit,
    onShareSelection: () -> Unit,
    onCopyFilePath: () -> Unit,
    onCopyPageText: () -> Unit
) {
    LeafSheetSurface(title = stringResource(R.string.reader_share_title)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(onClick = onShareDocument, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.reader_share_document), modifier = Modifier.fillMaxWidth())
            }
            TextButton(onClick = onSharePageImage, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.reader_share_page_image), modifier = Modifier.fillMaxWidth())
            }
            TextButton(
                onClick = onShareSelection,
                enabled = hasSelection,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.reader_share_selected_text), modifier = Modifier.fillMaxWidth())
            }
            TextButton(onClick = onCopyFilePath, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.reader_copy_file_path), modifier = Modifier.fillMaxWidth())
            }
            TextButton(onClick = onCopyPageText, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.reader_copy_page_text), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun orientationLabel(orientation: app.leaf.reader.core.model.Orientation): Int = when (orientation) {
    app.leaf.reader.core.model.Orientation.AUTO -> R.string.reader_orientation_auto
    app.leaf.reader.core.model.Orientation.PORTRAIT -> R.string.reader_orientation_portrait
    app.leaf.reader.core.model.Orientation.LANDSCAPE -> R.string.reader_orientation_landscape
}

private fun ReadingTheme.labelRes(): Int = when (this) {
    ReadingTheme.PAPER -> R.string.reader_theme_paper
    ReadingTheme.SEPIA -> R.string.reader_theme_sepia
    ReadingTheme.NIGHT -> R.string.reader_theme_night
    ReadingTheme.OLED -> R.string.reader_theme_oled
    ReadingTheme.AUTO -> R.string.reader_theme_auto
}
