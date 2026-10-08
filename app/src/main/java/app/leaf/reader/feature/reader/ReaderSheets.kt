package app.leaf.reader.feature.reader

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.leaf.reader.R
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
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
    onFullScreen: () -> Unit
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
                onFullScreen = onFullScreen
            )
        }
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

private fun ReadingTheme.labelRes(): Int = when (this) {
    ReadingTheme.PAPER -> R.string.reader_theme_paper
    ReadingTheme.SEPIA -> R.string.reader_theme_sepia
    ReadingTheme.NIGHT -> R.string.reader_theme_night
    ReadingTheme.OLED -> R.string.reader_theme_oled
    ReadingTheme.AUTO -> R.string.reader_theme_auto
}
