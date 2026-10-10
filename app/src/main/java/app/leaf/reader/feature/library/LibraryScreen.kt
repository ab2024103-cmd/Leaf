package app.leaf.reader.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.leaf.reader.R
import app.leaf.reader.core.domain.LibraryDoc
import app.leaf.reader.core.domain.TypeGroupCard
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.SmartCollection
import app.leaf.reader.core.model.SortField
import app.leaf.reader.core.model.Tag
import app.leaf.reader.core.ui.components.FolderDot
import app.leaf.reader.core.ui.components.LeafChipRail
import app.leaf.reader.core.ui.components.LeafConfirmDialogOverlay
import app.leaf.reader.core.ui.components.LeafDocumentRow
import app.leaf.reader.core.ui.components.LeafEmptyState
import app.leaf.reader.core.ui.components.LeafFab
import app.leaf.reader.core.ui.components.LeafFilterChip
import app.leaf.reader.core.ui.components.LeafGroupCard
import app.leaf.reader.core.ui.components.LeafIconButton
import app.leaf.reader.core.ui.components.LeafPromptSheet
import app.leaf.reader.core.ui.components.LeafSectionLabel
import app.leaf.reader.core.ui.components.LeafSheetItem
import app.leaf.reader.core.ui.components.LeafSheetOverlay
import app.leaf.reader.core.ui.components.LeafSheetSurface
import app.leaf.reader.core.ui.components.LeafSnackbar
import app.leaf.reader.core.ui.components.LeafTopAppBar
import app.leaf.reader.core.ui.components.TagDot
import app.leaf.reader.core.ui.components.swatchColor
import app.leaf.reader.core.ui.theme.LeafSpacing
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.feature.reader.OpenTabsButton
import app.leaf.reader.core.ui.util.groupTitle
import app.leaf.reader.core.ui.util.quantityText
import app.leaf.reader.core.ui.util.shortDate
import app.leaf.reader.core.ui.util.stringText
import app.leaf.reader.core.ui.util.timeAgoText
import app.leaf.reader.core.util.TimeAgo
import app.leaf.reader.core.util.formatMegabytes
import app.leaf.reader.core.util.timeAgo
import kotlinx.coroutines.flow.collect
import org.koin.androidx.compose.koinViewModel

/**
 * Library — the default destination (§6.1).
 *
 * The screen is split in two on purpose: [LibraryContent] is a pure function of
 * [LibraryContentState] plus [LibraryHandlers], so every sheet, group and empty state can
 * be rendered (and screenshot-tested) without a database, and every behaviour lives in
 * the ViewModel.
 *
 * PDF row taps open the M3 reader; formats without an engine remain untappable until M6.
 */
@Composable
fun LibraryContent(
    state: LibraryContentState,
    handlers: LibraryHandlers,
    modifier: Modifier = Modifier,
    /** 1 column on compact, 2 on medium, 3 on expanded (§5). Group cards cap at 2. */
    columns: Int = 1,
    openTabCount: Int = 0,
    onOpenDocuments: () -> Unit = {},
    pickingNewTab: Boolean = false,
    tabsInUse: Int = 0,
    tabLimit: Int = 6,
    onCancelPickingNewTab: () -> Unit = {},
    onOpenNewTab: (String) -> Unit = {}
) {
    val listState = rememberLazyListState()
    LaunchedEffect(pickingNewTab) {
        if (pickingNewTab) listState.animateScrollToItem(0)
    }
    val scrolled by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 8 }
    }
    val view = state.view
    val groupColumns = columns.coerceAtMost(2)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LeafTopAppBar(
                title = stringResource(R.string.nav_library),
                subtitle = state.subtitle,
                showBrand = true,
                scrolled = scrolled,
                actions = {
                    OpenTabsButton(count = openTabCount, onClick = onOpenDocuments)
                    LeafIconButton(
                        icon = R.drawable.ic_folder,
                        contentDescription = stringResource(R.string.action_folders),
                        onClick = handlers.onFoldersClicked
                    )
                    LeafIconButton(
                        icon = R.drawable.ic_tag,
                        contentDescription = stringResource(R.string.action_tags),
                        onClick = handlers.onTagsClicked
                    )
                }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LeafSpacing.listGap)
            ) {
                if (pickingNewTab) {
                    item(key = "pick-new-tab-banner") {
                        PickDocumentBanner(
                            tabsInUse = tabsInUse,
                            tabLimit = tabLimit,
                            onCancel = onCancelPickingNewTab,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                // The three rails describe the filter, so they have nothing to say on the
                // overview — there, the cards *are* the navigation (§6.1).
                if (!view.showGroups) {
                    item {
                        FolderRail(
                            folders = state.folders,
                            selectedId = state.filter.folderId,
                            totals = view.folderTotals,
                            onPick = handlers.onFolderChosen,
                            onAdd = handlers.onNewFolder
                        )
                    }
                    item {
                        CollectionRail(
                            collections = state.smart,
                            selectedId = state.filter.smartId,
                            totals = view.smartTotals,
                            onPick = handlers.onSmartChosen,
                            onAdd = handlers.onNewCollection
                        )
                    }
                    item {
                        TagRail(
                            tags = state.tags,
                            selected = state.filter.tags,
                            onToggle = handlers.onTagToggled,
                            onAdd = handlers.onNewTag
                        )
                    }
                }

                if (view.openType != null) {
                    item {
                        LeafGroupCrumb(
                            type = view.openType,
                            subtitle = stringResource(
                                R.string.crumb_sub,
                                quantityText(R.plurals.library_count, view.rows.size, view.rows.size),
                                state.sortLabel
                            ),
                            onBack = handlers.onGroupClosed
                        )
                    }
                }

                item {
                    SectionHeader(
                        count = if (view.showGroups) view.visible.size else view.rows.size,
                        types = if (view.showGroups) view.typeCount else 0,
                        sortLabel = state.sortLabel,
                        sortActive = state.sortField != SortField.NAME,
                        sortVisible = !view.showGroups,
                        grouped = state.groupByType,
                        onGroupToggle = handlers.onGroupToggled,
                        onSort = handlers.onSortClicked
                    )
                }

                if (view.showGroups) {
                    if (view.groups.isEmpty()) {
                        item { EmptyLibrary() }
                    } else {
                        // §6.1: one column when compact, two otherwise — never more.
                        view.groups.chunked(groupColumns).forEach { rowCards ->
                            item {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(LeafSpacing.listGap)
                                ) {
                                    rowCards.forEach { card ->
                                        GroupCardItem(
                                            card = card,
                                            modifier = Modifier.weight(1f)
                                        ) { handlers.onGroupOpened(card.type) }
                                    }
                                }
                            }
                        }
                    }
                } else if (view.rows.isEmpty()) {
                    item { EmptyLibrary() }
                } else {
                    items(view.rows, key = { "doc-${it.id}" }) { doc ->
                        LeafDocumentRow(
                            doc = doc,
                            meta = rowMeta(doc = doc, now = state.now),
                            tagColors = { name ->
                                swatchColor(state.tags.firstOrNull { it.name == name }?.colorHex.orEmpty())
                            },
                            onStar = { handlers.onStar(doc.id) },
                            onLongPress = { handlers.onRowLongPress(doc.id) },
                            onTap = if (doc.document.type == DocType.PDF) {
                                if (pickingNewTab) {
                                    { onOpenNewTab(doc.id) }
                                } else {
                                    { handlers.onOpenDocument(doc.id) }
                                }
                            } else {
                                null
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                            pickingNewTab = pickingNewTab
                        )
                    }
                }

                item {
                    // The list scrolls clear of the extended FAB (§6.1).
                    Spacer(modifier = Modifier.height(LeafSpacing.fabClearance))
                }
            }
        }

        if (!pickingNewTab) {
            LeafFab(
                label = stringResource(R.string.fab_new_folder),
                onClick = handlers.onNewFolder,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }

        state.message?.let { message ->
            // Keyed on the message id so posting the same text twice restarts the timer.
            key(message.key) {
                LeafSnackbar(
                    message = message.text,
                    actionLabel = message.actionLabel,
                    durationMs = message.durationMs,
                    onAction = handlers.onMessageAction,
                    onDismiss = handlers.onMessageShown,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                )
            }
        }

        LibrarySheetHost(state = state, handlers = handlers)
        LibraryDialogHost(state = state, handlers = handlers)
    }
}

/** The screen entry point: the ViewModel owns the data and the behaviour. */
@Composable
fun LibraryScreen(
    onOpenDocument: (String) -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
    /** 1 column on compact, 2 on medium, 3 on expanded (§5). */
    columns: Int = 1,
    openTabCount: Int = 0,
    onOpenDocuments: () -> Unit = {},
    pickingNewTab: Boolean = false,
    tabsInUse: Int = 0,
    tabLimit: Int = 6,
    onCancelPickingNewTab: () -> Unit = {},
    onOpenNewTab: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel, onOpenDocument, onOpenNewTab) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.OpenDocument -> onOpenDocument(event.documentId)
                is LibraryEvent.OpenNewTab -> onOpenNewTab(event.documentId)
            }
        }
    }
    LibraryContent(
        state = state,
        handlers = viewModel.handlers,
        columns = columns,
        openTabCount = openTabCount,
        onOpenDocuments = onOpenDocuments,
        pickingNewTab = pickingNewTab,
        tabsInUse = tabsInUse,
        tabLimit = tabLimit,
        onCancelPickingNewTab = onCancelPickingNewTab,
        onOpenNewTab = onOpenNewTab
    )
}

// ── the three filter rails ──────────────────────────────────────────────────────

/**
 * §6.1 / §8.4: "All documents" first, then every folder in tree order with subfolders
 * nested after their parent. Each chip carries the count it would show (§8.5).
 */
@Composable
private fun FolderRail(
    folders: List<Folder>,
    selectedId: String?,
    totals: Map<String?, Int>,
    onPick: (String?) -> Unit,
    onAdd: () -> Unit
) {
    LeafChipRail(top = 2.dp, bottom = 4.dp) {
        LeafFilterChip(
            label = stringResource(R.string.chip_all_documents),
            selected = selectedId == null,
            count = totals[null] ?: 0,
            onClick = { onPick(null) }
        )
        orderedFolders(folders).forEach { folder ->
            LeafFilterChip(
                label = folder.name,
                selected = selectedId == folder.id,
                count = totals[folder.id] ?: 0,
                onClick = { onPick(folder.id) },
                leading = { FolderDot(swatchColor(folder.colorHex)) }
            )
        }
        LeafFilterChip(
            label = stringResource(R.string.chip_add_folder),
            selected = false,
            onClick = onAdd,
            dashed = true
        )
    }
}

/** Depth-first: a parent immediately followed by its own subfolders. */
private fun orderedFolders(folders: List<Folder>): List<Folder> {
    val managed = folders.filter { !it.isSystem }
    val out = ArrayList<Folder>(managed.size)
    fun walk(parentId: String?) {
        managed.filter { it.parentId == parentId }.forEach { folder ->
            out += folder
            walk(folder.id)
        }
    }
    walk(null)
    return out
}

/** §6.1: the six seeded collections plus anything the composer has added. */
@Composable
private fun CollectionRail(
    collections: List<SmartCollection>,
    selectedId: String?,
    totals: Map<String, Int>,
    onPick: (String) -> Unit,
    onAdd: () -> Unit
) {
    LeafChipRail(top = 0.dp, bottom = 4.dp) {
        collections.forEach { collection ->
            LeafFilterChip(
                label = collection.name,
                selected = selectedId == collection.id,
                count = totals[collection.id] ?: 0,
                onClick = { onPick(collection.id) },
                leading = {
                    Icon(
                        painter = painterResource(R.drawable.ic_smart),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            )
        }
        LeafFilterChip(
            label = stringResource(R.string.chip_add_collection),
            selected = false,
            onClick = onAdd,
            dashed = true
        )
    }
}

/** §2.1: tag chips are multi-select with AND semantics. */
@Composable
private fun TagRail(
    tags: List<Tag>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onAdd: () -> Unit
) {
    if (tags.isEmpty() && selected.isEmpty()) return
    LeafChipRail(top = 0.dp, bottom = 10.dp) {
        tags.forEach { tag ->
            LeafFilterChip(
                label = tag.name,
                selected = tag.name in selected,
                mini = true,
                onClick = { onToggle(tag.name) },
                leading = { TagDot(swatchColor(tag.colorHex)) }
            )
        }
        LeafFilterChip(
            label = stringResource(R.string.chip_add_tag),
            selected = false,
            mini = true,
            onClick = onAdd,
            dashed = true
        )
    }
}

// ── header and cards ────────────────────────────────────────────────────────────

/**
 * `8 DOCUMENTS · 3 types` with two controls: the group-by-type toggle and the sort chip.
 * The sort chip is hidden on the overview (§6.1) — the cards have no sort order to show.
 */
@Composable
private fun SectionHeader(
    count: Int,
    types: Int,
    sortLabel: String,
    sortActive: Boolean,
    sortVisible: Boolean,
    grouped: Boolean,
    onGroupToggle: () -> Unit,
    onSort: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            val countText = quantityText(R.plurals.library_count, count, count)
            LeafSectionLabel(
                text = if (types > 0) {
                    countText + " · " + quantityText(R.plurals.library_types, types, types)
                } else {
                    countText
                }
            )
        }
        // §6.1 places the filter glyph before the group toggle. Its advanced-filter
        // panel is part of the later Search milestone, so it is kept decorative here —
        // no control is drawn that appears tappable but has no M2 behavior.
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_filter),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        LeafIconButton(
            icon = R.drawable.ic_group_type,
            contentDescription = if (grouped) {
                stringResource(R.string.cd_group_by_type_on)
            } else {
                stringResource(R.string.cd_group_by_type)
            },
            onClick = onGroupToggle,
            size = 36.dp,
            iconSize = 20.dp,
            selected = grouped,
            tint = if (grouped) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        if (sortVisible) {
            LeafFilterChip(
                label = sortLabel,
                selected = sortActive,
                mini = true,
                onClick = onSort,
                leading = {
                    Icon(
                        painter = painterResource(R.drawable.ic_sort),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun GroupCardItem(
    card: TypeGroupCard,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    val documents = quantityText(R.plurals.library_count, card.count, card.count)
    val size = stringText(R.string.value_megabytes, formatMegabytes(card.sizeBytes))
    val parts = mutableListOf(documents, size)
    if (card.docsWithHighlights > 0) {
        parts += quantityText(
            R.plurals.n_highlights_docs,
            card.docsWithHighlights,
            card.docsWithHighlights
        )
    }
    if (card.docsWithBookmarks > 0) {
        parts += quantityText(R.plurals.n_bookmarks, card.docsWithBookmarks, card.docsWithBookmarks)
    }
    LeafGroupCard(
        card = card,
        title = groupTitle(card.type),
        summaryParts = parts,
        onClick = onOpen,
        modifier = modifier
    )
}

@Composable
private fun EmptyLibrary() {
    LeafEmptyState(
        icon = R.drawable.ic_document,
        title = stringResource(R.string.empty_library_title),
        message = stringResource(R.string.empty_library_message)
    )
}

// ── the sheet host ──────────────────────────────────────────────────────────────

/** The sheet the Library is currently showing, if any. */
@Composable
fun LibrarySheetHost(state: LibraryContentState, handlers: LibraryHandlers) {
    val usage = remember(state.docs) {
        state.docs.flatMap { it.document.tags }.groupBy { it }.mapValues { it.value.size }
    }
    val subfolderCount = remember(state.folders) {
        state.folders.groupBy { it.parentId }.mapValues { it.value.size }
    }
    when (val sheet = state.sheet) {
        LibrarySheet.None -> Unit

        LibrarySheet.Sort -> LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
            SortSheetContent(
                field = state.sortField,
                ascending = state.sortAscending,
                onFieldChosen = handlers.onSortFieldChosen,
                onDirectionChosen = handlers.onSortDirectionChosen
            )
        }

        LibrarySheet.FolderManager -> LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
            FolderManagerSheetContent(
                folders = state.folders,
                documentCount = { id ->
                    state.docs.count { it.document.folderId == id }
                },
                subfolderCount = { id -> subfolderCount[id] ?: 0 },
                onAction = handlers.onFolderAction
            )
        }

        LibrarySheet.NewSubfolderPicker -> LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
            LibrarySheetsFolderPicker(folders = state.folders, onPick = handlers.onFolderAction)
        }

        LibrarySheet.TagManager -> LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
            TagManagerSheetContent(
                tags = state.tags,
                usage = { usage[it] ?: 0 },
                onAction = handlers.onTagAction
            )
        }

        LibrarySheet.NewCollection -> LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
            NewCollectionSheetContent(onRulePicked = handlers.onCollectionRulePicked)
        }

        LibrarySheet.PickCollectionType -> LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
            PickCollectionTypeContent(onPick = handlers.onCollectionTypePicked)
        }

        LibrarySheet.PickCollectionAge -> LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
            PickCollectionAgeContent(onPick = handlers.onCollectionAgePicked)
        }

        LibrarySheet.PickCollectionTag -> LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
            PickCollectionTagContent(
                tags = state.tags,
                onPick = handlers.onCollectionTagPicked
            )
        }

        is LibrarySheet.Actions -> {
            val doc = state.docs.firstOrNull { it.id == sheet.docId }
            if (doc != null) {
                LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
                    ActionsSheetContent(
                        doc = doc,
                        summary = actionSummary(doc, state),
                        onAction = handlers.onDocumentAction
                    )
                }
            }
        }

        is LibrarySheet.MoveToFolder -> {
            val doc = state.docs.firstOrNull { it.id == sheet.docId }
            if (doc != null) {
                LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
                    MoveToFolderSheetContent(
                        folders = state.folders,
                        currentFolderId = doc.document.folderId,
                        onPick = { handlers.onMovedToFolder(doc.id, it) }
                    )
                }
            }
        }

        is LibrarySheet.ManageTags -> {
            val doc = state.docs.firstOrNull { it.id == sheet.docId }
            if (doc != null) {
                LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
                    ManageTagsSheetContent(
                        doc = doc,
                        tags = state.tags,
                        usage = { usage[it] ?: 0 },
                        onToggle = { handlers.onTagAction(TagAction.Toggle(doc.id, it)) },
                        onNewTag = { handlers.onTagAction(TagAction.New) }
                    )
                }
            }
        }

        is LibrarySheet.FileInfo -> {
            val doc = state.docs.firstOrNull { it.id == sheet.docId }
            if (doc != null) {
                LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
                    FileInfoSheetContent(doc = doc, rows = fileInfoRows(doc, state))
                }
            }
        }

        is LibrarySheet.RenameDocument -> {
            val doc = state.docs.firstOrNull { it.id == sheet.docId }
            if (doc != null) {
                LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
                    LeafPromptSheet(
                        title = stringResource(R.string.prompt_rename_document),
                        hint = stringResource(R.string.prompt_currently, doc.document.name),
                        placeholder = stringResource(R.string.prompt_new_name),
                        initial = doc.document.name,
                        onSave = handlers.onPromptSave
                    )
                }
            }
        }

        is LibrarySheet.RenameFolder -> {
            val folder = state.folders.firstOrNull { it.id == sheet.folderId }
            if (folder != null) {
                LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
                    LeafPromptSheet(
                        title = stringResource(R.string.prompt_rename_folder),
                        hint = stringResource(R.string.prompt_currently, folder.name),
                        placeholder = stringResource(R.string.prompt_folder_name),
                        initial = folder.name,
                        onSave = handlers.onPromptSave
                    )
                }
            }
        }

        is LibrarySheet.FolderColour -> {
            val folder = state.folders.firstOrNull { it.id == sheet.folderId }
            if (folder != null) {
                LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
                    FolderColourSheetContent(
                        folder = folder,
                        onPick = handlers.onColourPicked
                    )
                }
            }
        }

        is LibrarySheet.NewFolder -> LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
            val parent = state.folders.firstOrNull { it.id == sheet.parentId }
            LeafPromptSheet(
                title = if (parent == null) {
                    stringResource(R.string.prompt_new_folder)
                } else {
                    stringResource(R.string.prompt_new_subfolder)
                },
                hint = if (parent == null) {
                    stringResource(R.string.prompt_folder_hint)
                } else {
                    stringResource(R.string.prompt_inside_folder, parent.name)
                },
                placeholder = stringResource(R.string.prompt_folder_name),
                initial = "",
                onSave = handlers.onPromptSave
            )
        }

        is LibrarySheet.RenameTag -> {
            val tag = state.tags.firstOrNull { it.name == sheet.tag }
            if (tag != null) {
                LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
                    LeafPromptSheet(
                        title = stringResource(R.string.prompt_rename_tag),
                        hint = stringResource(R.string.prompt_currently, tag.name),
                        placeholder = stringResource(R.string.prompt_tag_name),
                        initial = tag.name,
                        onSave = handlers.onPromptSave
                    )
                }
            }
        }

        is LibrarySheet.TagColour -> {
            val tag = state.tags.firstOrNull { it.name == sheet.tag }
            if (tag != null) {
                LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
                    TagColourSheetContent(tag = tag, onPick = handlers.onColourPicked)
                }
            }
        }

        LibrarySheet.NewTag -> LeafSheetOverlay(onDismiss = handlers.onDismissSheet) {
            LeafPromptSheet(
                title = stringResource(R.string.prompt_new_tag),
                hint = stringResource(R.string.prompt_tag_hint),
                placeholder = stringResource(R.string.prompt_tag_name),
                initial = "",
                onSave = handlers.onPromptSave
            )
        }
    }
}

/** Picking which top-level folder a new subfolder goes into (§6.1). */
@Composable
private fun LibrarySheetsFolderPicker(
    folders: List<Folder>,
    onPick: (FolderAction) -> Unit
) {
    LeafSheetSurface(
        title = stringResource(R.string.sheet_subfolder_picker_title),
        subtitle = stringResource(R.string.sheet_subfolder_picker_sub)
    ) {
        folders.filter { !it.isSystem }.forEach { folder ->
            LeafSheetItem(
                icon = R.drawable.ic_folder,
                title = folder.name,
                container = swatchColor(folder.colorHex).copy(alpha = 0.22f),
                content = swatchColor(folder.colorHex),
                onClick = { onPick(FolderAction.NewSubfolderIn(folder.id)) }
            )
        }
    }
}

/** `12 pages · 1.20 MB · Work · 40% read` under the actions sheet title. */
@Composable
private fun actionSummary(doc: LibraryDoc, state: LibraryContentState): String = stringResource(
    R.string.action_summary,
    doc.pageCount,
    stringText(R.string.value_megabytes, formatMegabytes(doc.document.sizeBytes)),
    state.folders.firstOrNull { it.id == doc.document.folderId }?.name
        ?: stringResource(R.string.value_not_filed),
    doc.pctRead
)

/** File info (§6.7): type · size · pages · added · last opened · position · … */
@Composable
private fun fileInfoRows(doc: LibraryDoc, state: LibraryContentState): List<Pair<String, String>> {
    val lastOpened = doc.document.lastOpened
    val position = if (doc.document.pageCount > 0) {
        stringResource(
            R.string.info_position_value,
            doc.page + 1,
            doc.document.pageCount,
            doc.pctRead
        )
    } else {
        stringResource(R.string.value_none)
    }
    return listOf(
        stringResource(R.string.info_type) to doc.document.type.label,
        stringResource(R.string.info_size) to stringText(
            R.string.value_megabytes,
            formatMegabytes(doc.document.sizeBytes)
        ),
        stringResource(R.string.info_pages) to quantityText(
            R.plurals.n_pages,
            doc.document.pageCount,
            doc.document.pageCount
        ),
        stringResource(R.string.info_added) to stampText(doc.document.dateAdded, state.now),
        stringResource(R.string.info_last_opened) to if (lastOpened == null) {
            stringResource(R.string.value_never)
        } else {
            val ago = timeAgo(lastOpened, state.now)
            if (ago is TimeAgo.Date) shortDate(ago.millis) else timeAgoText(ago)
        },
        stringResource(R.string.info_position) to position,
        stringResource(R.string.info_bookmarks) to if (doc.bookmarkCount > 0) {
            quantityText(R.plurals.n_bookmarks, doc.bookmarkCount, doc.bookmarkCount)
        } else {
            stringResource(R.string.value_none)
        },
        stringResource(R.string.info_highlights) to if (doc.highlightCount > 0) {
            quantityText(R.plurals.n_highlights, doc.highlightCount, doc.highlightCount)
        } else {
            stringResource(R.string.value_none)
        },
        stringResource(R.string.info_folder) to (
            state.folders.firstOrNull { it.id == doc.document.folderId }?.name
                ?: stringResource(R.string.value_not_filed)
            ),
        stringResource(R.string.info_tags) to doc.document.tags.joinToString(", ").ifEmpty {
            stringResource(R.string.value_none)
        }
    )
}

// ── dialogs ─────────────────────────────────────────────────────────────────────

/** Every destructive action confirms first (§6.7). */
@Composable
fun LibraryDialogHost(state: LibraryContentState, handlers: LibraryHandlers) {
    when (val dialog = state.dialog) {
        LibraryDialog.None -> Unit

        is LibraryDialog.DeleteDocument -> {
            val doc = state.docs.firstOrNull { it.id == dialog.docId } ?: return
            LeafConfirmDialogOverlay(
                title = stringResource(R.string.dialog_delete_document_title),
                body = if (doc.highlightCount > 0) {
                    stringResource(
                        R.string.dialog_delete_document_body,
                        doc.document.name,
                        quantityText(R.plurals.n_highlights, doc.highlightCount, doc.highlightCount),
                        doc.bookmarkCount
                    )
                } else {
                    stringResource(
                        R.string.dialog_delete_document_body_no_highlights,
                        doc.document.name,
                        doc.bookmarkCount
                    )
                },
                confirmLabel = stringResource(R.string.action_delete),
                onConfirm = handlers.onDialogConfirm,
                onDismiss = handlers.onDismissDialog
            )
        }

        is LibraryDialog.DeleteFolder -> {
            val folder = state.folders.firstOrNull { it.id == dialog.folderId } ?: return
            val inside = state.docs.count { it.document.folderId == folder.id }
            val destination = state.folders.firstOrNull { it.id == folder.parentId }?.name
                ?: stringResource(R.string.value_not_filed)
            LeafConfirmDialogOverlay(
                title = stringResource(R.string.dialog_delete_folder_title, folder.name),
                body = if (inside > 0) {
                    stringResource(R.string.dialog_delete_folder_body, inside, destination)
                } else {
                    stringResource(R.string.dialog_delete_folder_body_empty)
                },
                confirmLabel = stringResource(R.string.action_delete_folder),
                onConfirm = handlers.onDialogConfirm,
                onDismiss = handlers.onDismissDialog
            )
        }

        is LibraryDialog.DeleteTag -> LeafConfirmDialogOverlay(
            title = stringResource(R.string.dialog_delete_tag_title, dialog.tag),
            body = stringResource(R.string.dialog_delete_tag_body),
            confirmLabel = stringResource(R.string.action_delete_tag),
            onConfirm = handlers.onDialogConfirm,
            onDismiss = handlers.onDismissDialog
        )
    }
}
