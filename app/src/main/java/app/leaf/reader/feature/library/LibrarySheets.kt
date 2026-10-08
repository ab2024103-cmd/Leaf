package app.leaf.reader.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.leaf.reader.R
import app.leaf.reader.core.domain.LibraryDoc
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.SortField
import app.leaf.reader.core.model.Tag
import app.leaf.reader.core.ui.components.FileTypeIcon
import app.leaf.reader.core.ui.components.LeafCheck
import app.leaf.reader.core.ui.components.LeafIconButton
import app.leaf.reader.core.ui.components.LeafPalette
import app.leaf.reader.core.ui.components.LeafSegmented
import app.leaf.reader.core.ui.components.LeafSheetDivider
import app.leaf.reader.core.ui.components.LeafSheetItem
import app.leaf.reader.core.ui.components.LeafSheetSurface
import app.leaf.reader.core.ui.components.RowMeta
import app.leaf.reader.core.ui.components.swatchColor
import app.leaf.reader.core.ui.theme.FileTypeTones
import app.leaf.reader.core.ui.theme.LeafSpacing
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.ui.util.groupTitle
import app.leaf.reader.core.ui.util.quantityText
import app.leaf.reader.core.ui.util.shortDate
import app.leaf.reader.core.ui.util.stringText
import app.leaf.reader.core.ui.util.timeAgoText
import app.leaf.reader.core.util.LeafSwatches
import app.leaf.reader.core.util.TimeAgo
import app.leaf.reader.core.util.formatMegabytes
import app.leaf.reader.core.util.timeAgo

/**
 * The Library's sheets (§6.1, §6.7, §8).
 *
 * Every sheet is a **pure composable**: it takes data and callbacks and owns nothing, so
 * the ViewModel decides what a tap means and a screenshot test can render any sheet with
 * no database behind it.
 */

// ── sort ─────────────────────────────────────────────────────────────────────────

/**
 * §6.1: a direction segment on top, then the four sort fields. File type is *not* here —
 * it is the grouped view's own toggle (§8.13).
 */
@Composable
fun SortSheetContent(
    field: SortField,
    ascending: Boolean,
    onFieldChosen: (SortField) -> Unit,
    onDirectionChosen: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LeafSheetSurface(
        title = stringResource(R.string.sort_sheet_title),
        subtitle = stringResource(R.string.sort_sheet_sub),
        modifier = modifier
    ) {
        Row(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 10.dp)) {
            LeafSegmented(
                labels = listOf(
                    stringResource(R.string.sort_dir_ascending),
                    stringResource(R.string.sort_dir_descending)
                ),
                selectedIndex = if (ascending) 0 else 1,
                onSelect = { onDirectionChosen(it == 0) }
            )
        }
        SortFieldRow(
            field = SortField.NAME,
            current = field,
            ascending = ascending,
            title = stringResource(R.string.sort_name),
            ascendingLabel = stringResource(R.string.sort_name_asc),
            descendingLabel = stringResource(R.string.sort_name_desc),
            onClick = { onFieldChosen(SortField.NAME) }
        )
        SortFieldRow(
            field = SortField.DATE_ADDED,
            current = field,
            ascending = ascending,
            title = stringResource(R.string.sort_added),
            ascendingLabel = stringResource(R.string.sort_added_asc),
            descendingLabel = stringResource(R.string.sort_added_desc),
            onClick = { onFieldChosen(SortField.DATE_ADDED) }
        )
        SortFieldRow(
            field = SortField.LAST_OPENED,
            current = field,
            ascending = ascending,
            title = stringResource(R.string.sort_opened),
            ascendingLabel = stringResource(R.string.sort_opened_asc),
            descendingLabel = stringResource(R.string.sort_opened_desc),
            onClick = { onFieldChosen(SortField.LAST_OPENED) }
        )
        SortFieldRow(
            field = SortField.FILE_SIZE,
            current = field,
            ascending = ascending,
            title = stringResource(R.string.sort_size),
            ascendingLabel = stringResource(R.string.sort_size_asc),
            descendingLabel = stringResource(R.string.sort_size_desc),
            onClick = { onFieldChosen(SortField.FILE_SIZE) }
        )
    }
}

/**
 * One sort field. Every row previews what the **current** direction would do, the way the
 * mockup keeps `A → Z` / `Z → A` live under whichever field is selected.
 */
@Composable
private fun SortFieldRow(
    field: SortField,
    current: SortField,
    ascending: Boolean,
    title: String,
    ascendingLabel: String,
    descendingLabel: String,
    onClick: () -> Unit
) {
    LeafSheetItem(
        icon = R.drawable.ic_sort,
        title = title,
        subtitle = if (ascending) ascendingLabel else descendingLabel,
        onClick = onClick,
        trailing = { if (field == current) LeafCheck() }
    )
}

// ── long-press actions ───────────────────────────────────────────────────────────

/**
 * §6.7 long-press actions.
 *
 * M3 adds the live PDF Open action. Open-in-new-tab and Share stay out of the sheet until
 * their planned milestones; non-PDF Open is omitted until its M6 engine exists.
 */
@Composable
fun ActionsSheetContent(
    doc: LibraryDoc,
    summary: String,
    onAction: (DocumentAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LeafSheetSurface(title = doc.document.name, subtitle = summary, modifier = modifier) {
        if (doc.document.type == DocType.PDF) {
            LeafSheetItem(
                icon = R.drawable.ic_document,
                title = stringResource(R.string.action_open),
                subtitle = stringResource(R.string.action_open_sub, doc.page + 1),
                onClick = { onAction(DocumentAction.Open(doc.id)) }
            )
        }
        LeafSheetItem(
            icon = R.drawable.ic_rename,
            title = stringResource(R.string.action_rename),
            subtitle = stringResource(R.string.prompt_currently, doc.document.name),
            onClick = { onAction(DocumentAction.Rename(doc.id)) }
        )
        LeafSheetItem(
            icon = R.drawable.ic_star,
            title = if (doc.document.favorite) {
                stringResource(R.string.action_remove_favorites)
            } else {
                stringResource(R.string.action_add_favorites)
            },
            subtitle = stringResource(R.string.action_favorites_sub),
            onClick = { onAction(DocumentAction.Favorite(doc.id)) },
            trailing = { if (doc.document.favorite) LeafCheck() }
        )
        LeafSheetItem(
            icon = R.drawable.ic_tag,
            title = stringResource(R.string.action_manage_tags),
            subtitle = if (doc.document.tags.isEmpty()) {
                stringResource(R.string.action_no_tags)
            } else {
                doc.document.tags.joinToString(", ")
            },
            onClick = { onAction(DocumentAction.ManageTags(doc.id)) }
        )
        LeafSheetItem(
            icon = R.drawable.ic_folder,
            title = stringResource(R.string.action_move_to_folder),
            onClick = { onAction(DocumentAction.Move(doc.id)) }
        )
        LeafSheetItem(
            icon = R.drawable.ic_info,
            title = stringResource(R.string.action_file_info),
            subtitle = stringResource(R.string.action_file_info_sub),
            onClick = { onAction(DocumentAction.Info(doc.id)) }
        )
        LeafSheetDivider()
        LeafSheetItem(
            icon = R.drawable.ic_delete,
            title = stringResource(R.string.action_delete_document),
            subtitle = stringResource(R.string.action_delete_document_sub),
            danger = true,
            onClick = { onAction(DocumentAction.Delete(doc.id)) }
        )
    }
}

// ── move to folder ───────────────────────────────────────────────────────────────

/** §8.8: moving re-files the document. No row here ever deletes one. */
@Composable
fun MoveToFolderSheetContent(
    folders: List<Folder>,
    currentFolderId: String?,
    onPick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LeafSheetSurface(
        title = stringResource(R.string.sheet_move_title),
        subtitle = stringResource(R.string.sheet_move_sub),
        modifier = modifier
    ) {
        LeafSheetItem(
            icon = R.drawable.ic_folder,
            title = stringResource(R.string.sheet_move_none),
            subtitle = stringResource(R.string.sheet_move_none_sub),
            container = MaterialTheme.colorScheme.surfaceContainerHighest,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { onPick(null) },
            trailing = { if (currentFolderId == null) LeafCheck() }
        )
        LeafSheetDivider()
        FolderTree(
            folders = folders,
            parentId = null,
            depth = 0,
            onPick = onPick,
            trailingFor = { folder -> folder.id == currentFolderId }
        )
    }
}

// ── this document's tags ─────────────────────────────────────────────────────────

@Composable
fun ManageTagsSheetContent(
    doc: LibraryDoc,
    tags: List<Tag>,
    usage: (String) -> Int,
    onToggle: (String) -> Unit,
    onNewTag: () -> Unit,
    modifier: Modifier = Modifier
) {
    LeafSheetSurface(
        title = stringResource(R.string.sheet_doc_tags_title),
        subtitle = stringResource(R.string.sheet_doc_tags_sub, doc.document.name),
        modifier = modifier
    ) {
        if (tags.isEmpty()) SheetNote(stringResource(R.string.action_no_tags))
        tags.forEach { tag ->
            val color = swatchColor(tag.colorHex)
            val on = tag.name in doc.document.tags
            LeafSheetItem(
                icon = R.drawable.ic_tag,
                title = tag.name,
                subtitle = quantityText(R.plurals.tag_document_count, usage(tag.name), usage(tag.name)),
                container = color.copy(alpha = if (on) 0.34f else 0.16f),
                content = color,
                onClick = { onToggle(tag.name) },
                trailing = { if (on) LeafCheck() }
            )
        }
        LeafSheetDivider()
        LeafSheetItem(
            icon = R.drawable.ic_plus,
            title = stringResource(R.string.sheet_new_tag_inline),
            onClick = onNewTag
        )
    }
}

// ── file info ────────────────────────────────────────────────────────────────────

/** The mockup's `infoSheet`: the file name as the subtitle, then the fact rows. */
@Composable
fun FileInfoSheetContent(
    doc: LibraryDoc,
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    LeafSheetSurface(
        title = stringResource(R.string.sheet_info_title),
        subtitle = doc.document.name,
        modifier = modifier
    ) {
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = label,
                    style = LeafType.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.42f)
                )
                Text(
                    text = value,
                    style = LeafType.body,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.58f)
                )
            }
        }
    }
}

// ── folder manager ───────────────────────────────────────────────────────────────

/**
 * §6.1 folder manager: every non-system folder with its document and subfolder counts,
 * rename / recolour / delete inline, plus new folder and new subfolder.
 */
@Composable
fun FolderManagerSheetContent(
    folders: List<Folder>,
    documentCount: (String) -> Int,
    subfolderCount: (String) -> Int,
    onAction: (FolderAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LeafSheetSurface(
        title = stringResource(R.string.sheet_folders_title),
        subtitle = stringResource(R.string.sheet_folders_sub),
        modifier = modifier
    ) {
        val managed = folders.filter { !it.isSystem }
        if (managed.isEmpty()) SheetNote(stringResource(R.string.sheet_no_folders))
        FolderTree(
            folders = managed,
            parentId = null,
            depth = 0,
            onPick = { folderId -> folderId?.let { onAction(FolderAction.Open(it)) } },
            subtitleFor = { folder ->
                folderSummary(
                    documents = documentCount(folder.id),
                    subfolders = subfolderCount(folder.id)
                )
            },
            actionsFor = { folder ->
                ManagerActions(
                    onRename = { onAction(FolderAction.Rename(folder.id)) },
                    onRecolour = { onAction(FolderAction.Recolour(folder.id)) },
                    onDelete = { onAction(FolderAction.Delete(folder.id)) }
                )
            }
        )
        LeafSheetDivider()
        LeafSheetItem(
            icon = R.drawable.ic_folder_nested,
            title = stringResource(R.string.sheet_new_folder_row),
            subtitle = stringResource(R.string.sheet_new_folder_row_sub),
            onClick = { onAction(FolderAction.NewTop) }
        )
        LeafSheetItem(
            icon = R.drawable.ic_folder_nested,
            title = stringResource(R.string.sheet_new_subfolder_row),
            subtitle = stringResource(R.string.sheet_new_subfolder_row_sub),
            onClick = { onAction(FolderAction.NewNested) }
        )
    }
}

/**
 * The folder rows. Nesting is stored to any depth (§2.1); the UI indents one level, so
 * anything deeper is listed flat beneath its nearest ancestor.
 */
@Composable
private fun FolderTree(
    folders: List<Folder>,
    parentId: String?,
    depth: Int,
    onPick: (String?) -> Unit,
    subtitleFor: (@Composable (Folder) -> String)? = null,
    actionsFor: (@Composable (Folder) -> Unit)? = null,
    /** When set, a ✓ marks the folders it answers true for. */
    trailingFor: ((Folder) -> Boolean)? = null
) {
    folders.filter { it.parentId == parentId }.forEach { folder ->
        val color = swatchColor(folder.colorHex)
        LeafSheetItem(
            icon = R.drawable.ic_folder,
            title = folder.name,
            subtitle = subtitleFor?.invoke(folder),
            container = color.copy(alpha = 0.22f),
            content = color,
            indent = depth > 0,
            onClick = { onPick(folder.id) },
            trailing = when {
                actionsFor != null -> { { actionsFor.invoke(folder) } }
                trailingFor != null && trailingFor.invoke(folder) -> { { LeafCheck() } }
                else -> null
            }
        )
        FolderTree(
            folders = folders,
            parentId = folder.id,
            depth = depth + 1,
            onPick = onPick,
            subtitleFor = subtitleFor,
            actionsFor = actionsFor,
            trailingFor = trailingFor
        )
    }
}

/** The mockup's three small buttons on the right of a manager row (34 dp). */
@Composable
private fun ManagerActions(
    onRename: () -> Unit,
    onRecolour: () -> Unit,
    onDelete: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        LeafIconButton(
            icon = R.drawable.ic_rename,
            contentDescription = stringResource(R.string.cd_rename),
            onClick = onRename,
            size = 34.dp,
            iconSize = 17.dp
        )
        LeafIconButton(
            icon = R.drawable.ic_palette,
            contentDescription = stringResource(R.string.cd_recolour),
            onClick = onRecolour,
            size = 34.dp,
            iconSize = 17.dp
        )
        LeafIconButton(
            icon = R.drawable.ic_delete,
            contentDescription = stringResource(R.string.cd_delete),
            onClick = onDelete,
            size = 34.dp,
            iconSize = 17.dp
        )
    }
}

@Composable
private fun folderSummary(documents: Int, subfolders: Int): String = stringResource(
    R.string.folder_summary,
    quantityText(R.plurals.folder_documents, documents, documents),
    quantityText(R.plurals.folder_subfolders, subfolders, subfolders)
)

// ── tag manager ──────────────────────────────────────────────────────────────────

/** §8.9: renaming cascades to every document; deleting only un-files them. */
@Composable
fun TagManagerSheetContent(
    tags: List<Tag>,
    usage: (String) -> Int,
    onAction: (TagAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LeafSheetSurface(
        title = stringResource(R.string.sheet_tags_title),
        subtitle = stringResource(R.string.sheet_tags_sub),
        modifier = modifier
    ) {
        if (tags.isEmpty()) SheetNote(stringResource(R.string.action_no_tags))
        tags.forEach { tag ->
            val color = swatchColor(tag.colorHex)
            LeafSheetItem(
                icon = R.drawable.ic_tag,
                title = tag.name,
                subtitle = quantityText(R.plurals.tag_document_count, usage(tag.name), usage(tag.name)),
                container = color.copy(alpha = 0.20f),
                content = color,
                onClick = { onAction(TagAction.Open(tag.name)) },
                trailing = {
                    ManagerActions(
                        onRename = { onAction(TagAction.Rename(tag.name)) },
                        onRecolour = { onAction(TagAction.Recolour(tag.name)) },
                        onDelete = { onAction(TagAction.Delete(tag.name)) }
                    )
                }
            )
        }
        LeafSheetDivider()
        LeafSheetItem(
            icon = R.drawable.ic_plus,
            title = stringResource(R.string.sheet_new_tag_row),
            subtitle = stringResource(R.string.sheet_new_tag_row_sub),
            onClick = { onAction(TagAction.New) }
        )
    }
}

// ── new smart collection ─────────────────────────────────────────────────────────

/** §2.1: the seven rules. Three of them ask a follow-up question on the next sheet. */
@Composable
fun NewCollectionSheetContent(
    onRulePicked: (RuleChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    LeafSheetSurface(
        title = stringResource(R.string.sheet_new_collection_title),
        subtitle = stringResource(R.string.sheet_new_collection_sub),
        modifier = modifier
    ) {
        LeafSheetItem(
            icon = R.drawable.ic_smart,
            title = stringResource(R.string.rule_all),
            subtitle = stringResource(R.string.rule_all_sub),
            onClick = { onRulePicked(RuleChoice.All) }
        )
        LeafSheetItem(
            icon = R.drawable.ic_document,
            title = stringResource(R.string.rule_type),
            subtitle = stringResource(R.string.rule_type_sub),
            onClick = { onRulePicked(RuleChoice.OfOneType) }
        )
        LeafSheetItem(
            icon = R.drawable.ic_clock,
            title = stringResource(R.string.rule_age),
            subtitle = stringResource(R.string.rule_age_sub),
            onClick = { onRulePicked(RuleChoice.Age) }
        )
        LeafSheetItem(
            icon = R.drawable.ic_progress,
            title = stringResource(R.string.rule_in_progress),
            subtitle = stringResource(R.string.rule_in_progress_sub),
            onClick = { onRulePicked(RuleChoice.InProgress) }
        )
        LeafSheetItem(
            icon = R.drawable.ic_folder,
            title = stringResource(R.string.rule_unfiled),
            subtitle = stringResource(R.string.rule_unfiled_sub),
            onClick = { onRulePicked(RuleChoice.Unfiled) }
        )
        LeafSheetItem(
            icon = R.drawable.ic_star,
            title = stringResource(R.string.rule_highlights),
            subtitle = stringResource(R.string.rule_highlights_sub),
            onClick = { onRulePicked(RuleChoice.HasHighlights) }
        )
        LeafSheetItem(
            icon = R.drawable.ic_tag,
            title = stringResource(R.string.rule_tag),
            subtitle = stringResource(R.string.rule_tag_sub),
            onClick = { onRulePicked(RuleChoice.WithTag) }
        )
    }
}

/** The follow-up to "of one type": the six supported types, in the §6.1 order. */
@Composable
fun PickCollectionTypeContent(
    onPick: (DocType) -> Unit,
    modifier: Modifier = Modifier
) {
    LeafSheetSurface(
        title = stringResource(R.string.sheet_pick_type_title),
        subtitle = stringResource(R.string.sheet_pick_type_sub),
        modifier = modifier
    ) {
        DocType.entries.forEach { type ->
            val tone = FileTypeTones.getValue(type)
            LeafSheetItem(
                icon = R.drawable.ic_document,
                title = type.label,
                subtitle = groupTitle(type),
                container = tone.containerLight,
                content = tone.labelLight,
                onClick = { onPick(type) }
            )
        }
    }
}

/** The follow-up to "added recently": 7, 30 or 90 days. */
@Composable
fun PickCollectionAgeContent(onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    LeafSheetSurface(
        title = stringResource(R.string.sheet_pick_age_title),
        subtitle = stringResource(R.string.sheet_pick_age_sub),
        modifier = modifier
    ) {
        listOf(7, 30, 90).forEach { days ->
            LeafSheetItem(
                icon = R.drawable.ic_clock,
                title = quantityText(R.plurals.last_days, days, days),
                onClick = { onPick(days) }
            )
        }
    }
}

/** The follow-up to "tagged with": one of the existing tags. */
@Composable
fun PickCollectionTagContent(
    tags: List<Tag>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LeafSheetSurface(
        title = stringResource(R.string.sheet_pick_tag_title),
        subtitle = stringResource(R.string.sheet_pick_tag_sub),
        modifier = modifier
    ) {
        if (tags.isEmpty()) SheetNote(stringResource(R.string.action_no_tags))
        tags.forEach { tag ->
            val color = swatchColor(tag.colorHex)
            LeafSheetItem(
                icon = R.drawable.ic_tag,
                title = tag.name,
                container = color.copy(alpha = 0.20f),
                content = color,
                onClick = { onPick(tag.name) }
            )
        }
    }
}

// ── colour sheets ────────────────────────────────────────────────────────────────

/** §4.6: the 10-swatch palette, 34 dp circles with a ✓ on the current one. */
@Composable
fun FolderColourSheetContent(
    folder: Folder,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LeafSheetSurface(
        title = stringResource(R.string.sheet_folder_colour_title, folder.name),
        subtitle = stringResource(R.string.sheet_folder_colour_sub),
        modifier = modifier
    ) {
        LeafPalette(colors = LeafSwatches, selected = folder.colorHex, onSelect = onPick)
    }
}

@Composable
fun TagColourSheetContent(
    tag: Tag,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LeafSheetSurface(
        title = stringResource(R.string.sheet_tag_colour_title, tag.name),
        modifier = modifier
    ) {
        LeafPalette(colors = LeafSwatches, selected = tag.colorHex, onSelect = onPick)
    }
}

// ── the group crumb ──────────────────────────────────────────────────────────────

/**
 * §6.1: inside a file-type group the back arrow and the compact file icon take the place
 * of the chip rails, with `N documents · Sorted by …` underneath.
 */
@Composable
fun LeafGroupCrumb(
    type: DocType,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeafIconButton(
            icon = R.drawable.ic_back,
            contentDescription = stringResource(R.string.cd_back_to_groups),
            onClick = onBack,
            size = LeafSpacing.control,
            iconSize = 20.dp
        )
        FileTypeIcon(type = type, compact = true)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = groupTitle(type),
                style = LeafType.listTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = LeafType.supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── shared bits ──────────────────────────────────────────────────────────────────

/** A supporting line where a sheet would otherwise be empty. */
@Composable
private fun SheetNote(text: String) {
    Text(
        text = text,
        style = LeafType.supporting,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
    )
}

/**
 * The row meta line (§6.1): `12 pages · 1.20 MB · 3 bookmarks · 4 days ago`.
 *
 * The mockup falls back to `dateAdded` for a document that has never been opened, so the
 * line is never empty.
 */
@Composable
@ReadOnlyComposable
internal fun rowMeta(doc: LibraryDoc, now: Long): RowMeta = RowMeta(
    pages = quantityText(R.plurals.n_pages, doc.pageCount, doc.pageCount),
    size = stringText(R.string.value_megabytes, formatMegabytes(doc.document.sizeBytes)),
    bookmarks = if (doc.bookmarkCount > 0) {
        quantityText(R.plurals.n_bookmarks, doc.bookmarkCount, doc.bookmarkCount)
    } else {
        null
    },
    lastOpened = stampText(doc.document.lastOpened ?: doc.document.dateAdded, now)
)

/** A timestamp the way the mockup prints one: relative while fresh, a short date after. */
@Composable
@ReadOnlyComposable
internal fun stampText(timestamp: Long?, now: Long): String = when (val ago = timeAgo(timestamp, now)) {
    TimeAgo.Never -> stringResource(R.string.value_never)
    is TimeAgo.Date -> shortDate(ago.millis)
    else -> timeAgoText(ago)
}
