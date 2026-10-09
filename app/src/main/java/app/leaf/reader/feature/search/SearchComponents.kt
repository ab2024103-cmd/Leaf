package app.leaf.reader.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.leaf.reader.R
import app.leaf.reader.core.domain.SearchAddedWithin
import app.leaf.reader.core.domain.SearchFilters
import app.leaf.reader.core.domain.SearchFileResult
import app.leaf.reader.core.domain.SearchScope
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Document
import app.leaf.reader.core.model.Tag
import app.leaf.reader.core.ui.components.FileTypeIcon
import app.leaf.reader.core.ui.components.LeafFilterChip
import app.leaf.reader.core.ui.components.LeafIconButton
import app.leaf.reader.core.ui.components.LeafStarButton
import app.leaf.reader.core.ui.components.LeafTagChip
import app.leaf.reader.core.ui.theme.FileTypeTones
import app.leaf.reader.core.ui.theme.LeafMetrics
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.util.formatMegabytes
import app.leaf.reader.core.ui.util.quantityText
import app.leaf.reader.core.ui.util.timeAgoText
import app.leaf.reader.core.util.timeAgo

@Composable
internal fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(52.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = LeafType.body.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = LeafType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (query.isNotEmpty()) {
                LeafIconButton(
                    icon = R.drawable.ic_close,
                    contentDescription = stringResource(R.string.cd_clear_search),
                    onClick = onClear,
                    size = 42.dp,
                    iconSize = 18.dp
                )
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
internal fun SearchScopeBar(
    selected: SearchScope,
    activeFilterCount: Int,
    onSelect: (SearchScope) -> Unit,
    onFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val scopes = SearchScope.entries
        scopes.forEachIndexed { index, scope ->
            val shape = when (index) {
                0 -> RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 0.dp, bottomEnd = 0.dp)
                scopes.lastIndex -> RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp, topStart = 0.dp, bottomStart = 0.dp)
                else -> RoundedCornerShape(0.dp)
            }
            Surface(
                onClick = { onSelect(scope) },
                modifier = Modifier.height(38.dp),
                shape = shape,
                color = if (scope == selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                contentColor = if (scope == selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier.defaultMinSize(minWidth = if (scope == SearchScope.INSIDE_FILES) 116.dp else 76.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(scopeLabel(scope), style = LeafType.chipText, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        val filterLabel = if (activeFilterCount == 0) {
            stringResource(R.string.search_filters)
        } else {
            stringResource(R.string.search_filters_with_count, activeFilterCount)
        }
        FilterChip(
            selected = activeFilterCount > 0,
            onClick = onFilters,
            label = { Text(filterLabel, style = LeafType.chipText, maxLines = 1) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_filter),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
            }
        )
    }
}

@Composable
private fun scopeLabel(scope: SearchScope): String = stringResource(
    when (scope) {
        SearchScope.EVERYTHING -> R.string.search_scope_everything
        SearchScope.NAME -> R.string.search_scope_name
        SearchScope.TAG -> R.string.search_scope_tag
        SearchScope.FOLDER -> R.string.search_scope_folder
        SearchScope.INSIDE_FILES -> R.string.search_scope_inside_files
    }
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun SearchFilterPanel(
    filters: SearchFilters,
    folders: List<app.leaf.reader.core.model.Folder>,
    tags: List<Tag>,
    onChange: (SearchFilters) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterOptionRow(label = stringResource(R.string.search_filter_type)) {
                FilterChoice(stringResource(R.string.search_filter_any), filters.type == null) {
                    onChange(filters.copy(type = null))
                }
                DocType.entries.forEach { type ->
                    FilterChoice(type.label, filters.type == type) { onChange(filters.copy(type = type)) }
                }
            }
            FilterOptionRow(label = stringResource(R.string.search_filter_folder)) {
                FilterChoice(stringResource(R.string.search_filter_any), filters.folderId == null) {
                    onChange(filters.copy(folderId = null))
                }
                folders.forEach { folder ->
                    FilterChoice(folder.name, filters.folderId == folder.id) {
                        onChange(filters.copy(folderId = folder.id))
                    }
                }
            }
            FilterOptionRow(label = stringResource(R.string.search_filter_tag)) {
                FilterChoice(stringResource(R.string.search_filter_any), filters.tag == null) {
                    onChange(filters.copy(tag = null))
                }
                tags.forEach { tag ->
                    FilterChoice(tag.name, filters.tag == tag.name) { onChange(filters.copy(tag = tag.name)) }
                }
            }
            FilterOptionRow(label = stringResource(R.string.search_filter_added)) {
                SearchAddedWithin.entries.forEach { added ->
                    FilterChoice(addedLabel(added), filters.addedWithin == added) {
                        onChange(filters.copy(addedWithin = added))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.search_filter_reset), style = LeafType.chipText)
                }
                Button(
                    onClick = onApply,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.search_filter_apply), style = LeafType.chipText)
                }
            }
        }
    }
}

@Composable
private fun FilterOptionRow(label: String, content: @Composable FlowRowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = LeafType.chipText, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(58.dp))
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

@Composable
private fun FilterChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    LeafFilterChip(label = label, selected = selected, onClick = onClick, mini = true)
}

@Composable
private fun addedLabel(value: SearchAddedWithin): String = stringResource(
    when (value) {
        SearchAddedWithin.ANY -> R.string.search_added_any
        SearchAddedWithin.WEEK -> R.string.search_added_week
        SearchAddedWithin.MONTH -> R.string.search_added_month
        SearchAddedWithin.LAST_THREE_MONTHS -> R.string.search_added_last_three_months
    }
)

@Composable
internal fun RecentSearches(
    history: List<String>,
    onRun: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.search_recent), style = LeafType.sectionLabel, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.search_clear_all), style = LeafType.chipText)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            history.forEach { query ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = query,
                            modifier = Modifier.clickable(onClick = { onRun(query) }).padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                            style = LeafType.supporting,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        LeafIconButton(
                            icon = R.drawable.ic_close,
                            contentDescription = stringResource(R.string.cd_remove_search_history, query),
                            onClick = { onRemove(query) },
                            size = 40.dp,
                            iconSize = 16.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SearchByChips(onSelect: (SearchQuickSearch) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.search_by),
            style = LeafType.sectionLabel,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 5.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickSearchChip(stringResource(R.string.search_quick_all_pdfs), onClick = { onSelect(SearchQuickSearch.ALL_PDFS) }, pdfDot = true)
            QuickSearchChip(stringResource(R.string.search_quick_week), onClick = { onSelect(SearchQuickSearch.ADDED_THIS_WEEK) })
            QuickSearchChip(stringResource(R.string.search_quick_folder_work), onClick = { onSelect(SearchQuickSearch.FOLDER_WORK) })
            QuickSearchChip(stringResource(R.string.search_quick_tag_important), onClick = { onSelect(SearchQuickSearch.TAG_IMPORTANT) })
            QuickSearchChip(stringResource(R.string.search_quick_word_chapter), onClick = { onSelect(SearchQuickSearch.WORD_CHAPTER) })
        }
    }
}

@Composable
private fun QuickSearchChip(label: String, onClick: () -> Unit, pdfDot: Boolean = false) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pdfDot) {
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                val tone = FileTypeTones.getValue(DocType.PDF)
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(if (isDark) tone.labelDark else tone.labelLight)
                )
            }
            Text(label, style = LeafType.chipText, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
internal fun SearchDocumentRow(
    document: Document,
    now: Long,
    tagColors: Map<String, String>,
    canOpen: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().then(if (canOpen) Modifier.clickable(onClick = onOpen) else Modifier),
        shape = RoundedCornerShape(LeafMetrics.cardRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileTypeIcon(document.type)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(document.name, style = LeafType.listTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                val pageCount = quantityText(R.plurals.n_pages, document.pageCount, document.pageCount)
                val lastOpened = timeAgoText(timeAgo(document.lastOpened ?: document.dateAdded, now))
                Text(
                    stringResource(R.string.search_document_metadata, pageCount, formatMegabytes(document.sizeBytes), lastOpened),
                    style = LeafType.listMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (document.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        document.tags.take(3).forEach { tag ->
                            val color = tagColors[tag]?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
                                ?: MaterialTheme.colorScheme.primary
                            LeafTagChip(tag, color)
                        }
                    }
                }
            }
            LeafStarButton(favorite = document.favorite, onClick = onFavorite)
        }
    }
}

@Composable
internal fun SearchFileResultRow(
    result: SearchFileResult,
    now: Long,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(LeafMetrics.cardRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileTypeIcon(result.document.type)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(result.document.name, style = LeafType.listTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                val matches = quantityText(R.plurals.search_match_count, result.matchCount, result.matchCount)
                val lastOpened = timeAgoText(timeAgo(result.document.lastOpened ?: result.document.dateAdded, now))
                Text(
                    stringResource(R.string.search_file_metadata, matches, result.firstPageIndex + 1, lastOpened),
                    style = LeafType.listMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    emphasizedExcerpt(result.excerpt.text, result.excerpt.matchStart, result.excerpt.matchEnd),
                    style = LeafType.readingBody.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            LeafStarButton(favorite = result.document.favorite, onClick = onFavorite)
        }
    }
}

@Composable
private fun emphasizedExcerpt(text: String, start: Int, end: Int): AnnotatedString = buildAnnotatedString {
    val safeStart = start.coerceIn(0, text.length)
    val safeEnd = end.coerceIn(safeStart, text.length)
    append(text.substring(0, safeStart))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
        append(text.substring(safeStart, safeEnd))
    }
    append(text.substring(safeEnd))
}

@Composable
internal fun SearchHistorySheet(
    history: List<String>,
    onDismiss: () -> Unit,
    onRun: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                stringResource(R.string.search_history_title),
                style = LeafType.sheetTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            history.forEach { query ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = { onRun(query) })
                        .padding(start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_clock),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        query,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        style = LeafType.body,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    LeafIconButton(
                        icon = R.drawable.ic_close,
                        contentDescription = stringResource(R.string.cd_remove_search_history, query),
                        onClick = { onRemove(query) },
                        size = 44.dp,
                        iconSize = 17.dp
                    )
                }
            }
            if (history.isNotEmpty()) {
                TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.search_history_clear), style = LeafType.chipText)
                }
            }
        }
    }
}

@Composable
internal fun SearchSummaryText(documentCount: Int, fileCount: Int, query: String, hits: Int): String {
    val documents = quantityText(R.plurals.search_document_count, documentCount, documentCount)
    val files = quantityText(R.plurals.search_file_count, fileCount, fileCount)
    val matchCount = quantityText(R.plurals.search_hit_count, hits, hits)
    return stringResource(R.string.search_summary, documents, files, query, matchCount)
}
