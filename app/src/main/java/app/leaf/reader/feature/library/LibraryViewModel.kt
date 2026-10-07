package app.leaf.reader.feature.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.leaf.reader.R
import app.leaf.reader.core.data.db.DocumentSnapshot
import app.leaf.reader.core.data.repo.DocumentRepository
import app.leaf.reader.core.data.repo.FolderRepository
import app.leaf.reader.core.data.repo.SmartCollectionRepository
import app.leaf.reader.core.data.repo.TagRepository
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.domain.LibraryDoc
import app.leaf.reader.core.domain.LibraryFilter
import app.leaf.reader.core.domain.composeLibrary
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.model.SmartCollection
import app.leaf.reader.core.model.SmartRule
import app.leaf.reader.core.model.SortField
import app.leaf.reader.core.model.Tag
import app.leaf.reader.core.ui.theme.LeafMotion
import app.leaf.reader.core.util.normalizeTagName
import app.leaf.reader.core.util.renameKeepingExtension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Library state and behaviour (§6.1, §6.7, §8).
 *
 * The screen renders [state]; every user action is a function here. Nothing in the UI
 * talks to the database, so the rules — filters compose, counts follow the filter
 * context, deletes never touch documents they should not — live in one testable place.
 */
class LibraryViewModel(
    private val app: Application,
    private val documents: DocumentRepository,
    private val folders: FolderRepository,
    private val tags: TagRepository,
    private val smart: SmartCollectionRepository,
    private val settings: SettingsStore
) : AndroidViewModel(app) {

    private data class Bundle(
        val docs: List<LibraryDoc>,
        val folders: List<Folder>,
        val tags: List<Tag>,
        val smart: List<SmartCollection>,
        val settings: LeafSettings
    )

    private data class Chrome(
        val filter: LibraryFilter,
        val sheet: LibrarySheet,
        val dialog: LibraryDialog,
        val message: LeafMessage?,
        val now: Long
    )

    /** A sheet remembers where it was opened from, so back returns to the manager. */
    private data class SheetState(
        val current: LibrarySheet = LibrarySheet.None,
        val parent: LibrarySheet = LibrarySheet.None
    )

    private val filter = MutableStateFlow(LibraryFilter())
    private val sheet = MutableStateFlow(SheetState())
    private val dialog = MutableStateFlow<LibraryDialog>(LibraryDialog.None)
    private val message = MutableStateFlow<LeafMessage?>(null)
    private val now = MutableStateFlow(System.currentTimeMillis())

    /** The snapshot behind the current "Undo" (§8.7). */
    private var pendingUndo: DocumentSnapshot? = null

    /** Increments on every snackbar so a repeated message still restarts its timer. */
    private var messageKey = 0

    private val bundle = combine(
        documents.observeLibrary(),
        folders.observeFolders(),
        tags.observeTags(),
        smart.observeCollections(),
        settings.settings
    ) { docs, folderList, tagList, collectionList, prefs ->
        Bundle(docs, folderList, tagList, collectionList, prefs)
    }

    private val chrome = combine(filter, sheet, dialog, message, now) { f, s, d, m, n ->
        Chrome(f, s.current, d, m, n)
    }

    val state: StateFlow<LibraryContentState> = combine(bundle, chrome) { data, ui ->
        build(data, ui)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryContentState())

    val handlers = LibraryHandlers(
        onFolderChosen = { chooseFolder(it) },
        onSmartChosen = { chooseSmart(it) },
        onTagToggled = { toggleTag(it) },
        onSortClicked = { openSheet(LibrarySheet.Sort) },
        onSortFieldChosen = { chooseSortField(it) },
        onSortDirectionChosen = { chooseSortDirection(it) },
        onGroupToggled = { toggleGrouping() },
        onGroupOpened = { openGroup(it) },
        onGroupClosed = { closeGroup() },
        onFoldersClicked = { openSheet(LibrarySheet.FolderManager) },
        onTagsClicked = { openSheet(LibrarySheet.TagManager) },
        onNewFolder = { openSheet(LibrarySheet.NewFolder(null)) },
        onNewCollection = { openSheet(LibrarySheet.NewCollection) },
        onNewTag = { openSheet(LibrarySheet.NewTag) },
        onStar = { toggleFavorite(it) },
        onRowLongPress = { openSheet(LibrarySheet.Actions(it)) },
        onFolderAction = { perform(it) },
        onTagAction = { perform(it) },
        onDocumentAction = { perform(it) },
        onMovedToFolder = { docId, folderId -> moveDocument(docId, folderId) },
        onPromptSave = { savePrompt(it) },
        onColourPicked = { applyColour(it) },
        onCollectionRulePicked = { chooseCollectionRule(it) },
        onCollectionTypePicked = { createCollection(SmartRule.OfType(it)) },
        onCollectionAgePicked = { createCollection(SmartRule.AgeDays(it)) },
        onCollectionTagPicked = { createCollection(SmartRule.WithTag(it)) },
        onDismissSheet = { dismissSheet() },
        onDialogConfirm = { confirmDialog() },
        onDismissDialog = { dismissDialog() },
        onMessageAction = { undo() },
        onMessageShown = { message.value = null }
    )

    // ── state ────────────────────────────────────────────────────────────────────

    private fun build(data: Bundle, ui: Chrome): LibraryContentState {
        val view = composeLibrary(
            docs = data.docs,
            folders = data.folders,
            smart = data.smart,
            filter = ui.filter,
            sortField = data.settings.sortField,
            sortAscending = data.settings.sortAscending,
            groupByType = data.settings.groupByType,
            now = ui.now
        )
        return LibraryContentState(
            subtitle = subtitleFor(data, ui),
            docs = data.docs,
            folders = data.folders,
            tags = data.tags,
            smart = data.smart,
            view = view,
            filter = ui.filter,
            sortLabel = sortLabel(data.settings.sortField, data.settings.sortAscending),
            sortField = data.settings.sortField,
            sortAscending = data.settings.sortAscending,
            groupByType = data.settings.groupByType,
            now = ui.now,
            sheet = ui.sheet,
            dialog = ui.dialog,
            message = ui.message
        )
    }

    /** `All 10 documents` · `Folder · Work` · `Collection · All PDFs` (§6.1). */
    private fun subtitleFor(data: Bundle, ui: Chrome): String {
        val collection = ui.filter.smartId?.let { id -> data.smart.firstOrNull { it.id == id } }
        if (collection != null) {
            return app.getString(R.string.library_subtitle_collection, collection.name)
        }
        val folder = ui.filter.folderId?.let { id -> data.folders.firstOrNull { it.id == id } }
        if (folder != null) {
            return app.getString(R.string.library_subtitle_folder, folder.name)
        }
        return app.resources.getQuantityString(
            R.plurals.library_subtitle_all,
            data.docs.size,
            data.docs.size
        )
    }

    private fun sortLabel(field: SortField, ascending: Boolean): String = app.getString(
        R.string.sort_chip,
        app.getString(
            when (field) {
                SortField.NAME -> R.string.sort_name
                SortField.DATE_ADDED -> R.string.sort_added
                SortField.LAST_OPENED -> R.string.sort_opened
                SortField.FILE_SIZE -> R.string.sort_size
            }
        ),
        app.getString(directionLabel(field, ascending))
    )

    private fun directionLabel(field: SortField, ascending: Boolean): Int = when (field) {
        SortField.NAME -> if (ascending) R.string.sort_name_asc else R.string.sort_name_desc
        SortField.DATE_ADDED -> if (ascending) R.string.sort_added_asc else R.string.sort_added_desc
        SortField.LAST_OPENED -> if (ascending) R.string.sort_opened_asc else R.string.sort_opened_desc
        SortField.FILE_SIZE -> if (ascending) R.string.sort_size_asc else R.string.sort_size_desc
    }

    // ── filters, sorting and grouping ────────────────────────────────────────────

    private fun chooseFolder(folderId: String?) {
        filter.value = filter.value.copy(folderId = folderId, smartId = null, type = null)
        now.value = System.currentTimeMillis()
    }

    private fun chooseSmart(collectionId: String?) {
        val current = filter.value.smartId
        filter.value = filter.value.copy(
            smartId = if (current == collectionId) null else collectionId,
            folderId = null,
            type = null
        )
        now.value = System.currentTimeMillis()
    }

    private fun toggleTag(name: String) {
        val selected = filter.value.tags
        filter.value = filter.value.copy(
            tags = if (name in selected) selected - name else selected + name
        )
        now.value = System.currentTimeMillis()
    }

    private fun chooseSortField(field: SortField) {
        viewModelScope.launch {
            settings.update { it.copy(sortField = field) }
            dismissSheet()
            post(app.getString(R.string.snack_sorted_by, app.getString(sortFieldLabel(field))))
        }
    }

    private fun sortFieldLabel(field: SortField): Int = when (field) {
        SortField.NAME -> R.string.sort_name
        SortField.DATE_ADDED -> R.string.sort_added
        SortField.LAST_OPENED -> R.string.sort_opened
        SortField.FILE_SIZE -> R.string.sort_size
    }

    private fun chooseSortDirection(ascending: Boolean) {
        viewModelScope.launch {
            settings.update { it.copy(sortAscending = ascending) }
            post(sortLabel(state.value.sortField, ascending))
        }
    }

    private fun toggleGrouping() {
        viewModelScope.launch {
            val next = !state.value.groupByType
            settings.update { it.copy(groupByType = next) }
            filter.value = filter.value.copy(type = null)
            now.value = System.currentTimeMillis()
            post(
                app.getString(
                    if (next) R.string.snack_grouped_on else R.string.snack_grouped_off
                )
            )
        }
    }

    private fun openGroup(type: DocType) {
        filter.value = filter.value.copy(type = type)
        now.value = System.currentTimeMillis()
        val count = state.value.view.visible.count { it.document.type == type }
        post(
            app.getString(
                R.string.snack_group_entered,
                groupTitleOf(type),
                app.resources.getQuantityString(R.plurals.library_count, count, count),
                sortLabel(state.value.sortField, state.value.sortAscending)
            )
        )
    }

    private fun closeGroup() {
        filter.value = filter.value.copy(type = null)
        now.value = System.currentTimeMillis()
    }

    private fun groupTitleOf(type: DocType): String = app.getString(
        when (type) {
            DocType.PDF -> R.string.group_pdf
            DocType.DOCX -> R.string.group_docx
            DocType.XLSX -> R.string.group_xlsx
            DocType.PPTX -> R.string.group_pptx
            DocType.TXT -> R.string.group_txt
            DocType.EPUB -> R.string.group_epub
        }
    )

    // ── document actions ─────────────────────────────────────────────────────────

    private fun toggleFavorite(id: String) {
        val doc = state.value.docs.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            documents.setFavorite(id, !doc.document.favorite)
            post(
                app.getString(
                    if (doc.document.favorite) {
                        R.string.snack_removed_from_favorites
                    } else {
                        R.string.snack_added_to_favorites
                    }
                )
            )
        }
    }

    private fun moveDocument(docId: String, folderId: String?) {
        viewModelScope.launch {
            documents.moveToFolder(docId, folderId)
            dismissSheet()
            val name = if (folderId == null) {
                null
            } else {
                state.value.folders.firstOrNull { it.id == folderId }?.name
            }
            post(
                if (name == null) {
                    app.getString(R.string.snack_removed_from_folder)
                } else {
                    app.getString(R.string.snack_moved_to, name)
                }
            )
        }
    }

    private fun perform(action: FolderAction) {
        when (action) {
            is FolderAction.Open -> chooseFolder(action.id)
            is FolderAction.Rename -> openSheet(LibrarySheet.RenameFolder(action.id), LibrarySheet.FolderManager)
            is FolderAction.Recolour -> openSheet(LibrarySheet.FolderColour(action.id), LibrarySheet.FolderManager)
            is FolderAction.Delete -> dialog.value = LibraryDialog.DeleteFolder(action.id)
            FolderAction.NewTop -> openSheet(LibrarySheet.NewFolder(null), LibrarySheet.FolderManager)
            FolderAction.NewNested -> openSheet(LibrarySheet.NewSubfolderPicker, LibrarySheet.FolderManager)
            is FolderAction.NewSubfolderIn -> openSheet(
                LibrarySheet.NewFolder(action.parentId),
                LibrarySheet.FolderManager
            )
        }
    }

    private fun perform(action: TagAction) {
        when (action) {
            is TagAction.Open -> {
                filter.value = filter.value.copy(tags = setOf(action.name))
                now.value = System.currentTimeMillis()
                dismissSheet()
            }
            is TagAction.Rename -> openSheet(LibrarySheet.RenameTag(action.name), LibrarySheet.TagManager)
            is TagAction.Recolour -> openSheet(LibrarySheet.TagColour(action.name), LibrarySheet.TagManager)
            is TagAction.Delete -> dialog.value = LibraryDialog.DeleteTag(action.name)
            TagAction.New -> openSheet(LibrarySheet.NewTag, LibrarySheet.TagManager)
            is TagAction.Toggle -> toggleDocumentTag(action.docId, action.name)
        }
    }

    private fun toggleDocumentTag(docId: String, name: String) {
        val doc = state.value.docs.firstOrNull { it.id == docId } ?: return
        val next = if (name in doc.document.tags) doc.document.tags - name else doc.document.tags + name
        viewModelScope.launch { documents.replaceTags(docId, next.sorted()) }
    }

    private fun perform(action: DocumentAction) {
        when (action) {
            is DocumentAction.Rename -> openSheet(LibrarySheet.RenameDocument(action.id))
            is DocumentAction.Favorite -> {
                toggleFavorite(action.id)
                dismissSheet()
            }
            is DocumentAction.ManageTags -> openSheet(LibrarySheet.ManageTags(action.id))
            is DocumentAction.Move -> openSheet(LibrarySheet.MoveToFolder(action.id))
            is DocumentAction.Info -> openSheet(LibrarySheet.FileInfo(action.id))
            is DocumentAction.Delete -> dialog.value = LibraryDialog.DeleteDocument(action.id)
        }
    }

    // ── smart collections ────────────────────────────────────────────────────────

    private fun chooseCollectionRule(choice: RuleChoice) {
        when (choice) {
            RuleChoice.All -> createCollection(SmartRule.All)
            RuleChoice.InProgress -> createCollection(SmartRule.InProgress)
            RuleChoice.Unfiled -> createCollection(SmartRule.Unfiled)
            RuleChoice.HasHighlights -> createCollection(SmartRule.HasHighlights)
            RuleChoice.OfOneType -> openSheet(LibrarySheet.PickCollectionType, LibrarySheet.NewCollection)
            RuleChoice.Age -> openSheet(LibrarySheet.PickCollectionAge, LibrarySheet.NewCollection)
            RuleChoice.WithTag -> openSheet(LibrarySheet.PickCollectionTag, LibrarySheet.NewCollection)
        }
    }

    private fun createCollection(rule: SmartRule) {
        viewModelScope.launch {
            val name = collectionNameFor(rule)
            val added = smart.add(name, rule)
            dismissSheet()
            post(
                if (added) {
                    app.getString(R.string.snack_collection_added, name)
                } else {
                    app.getString(R.string.snack_collection_exists)
                }
            )
        }
    }

    private fun collectionNameFor(rule: SmartRule): String = when (rule) {
        SmartRule.All -> app.getString(R.string.rule_all)
        SmartRule.InProgress -> app.getString(R.string.rule_in_progress)
        SmartRule.Unfiled -> app.getString(R.string.rule_unfiled)
        SmartRule.HasHighlights -> app.getString(R.string.rule_highlights)
        is SmartRule.OfType -> app.getString(
            when (rule.type) {
                DocType.PDF -> R.string.collection_pdfs
                DocType.DOCX -> R.string.collection_docx
                DocType.XLSX -> R.string.collection_xlsx
                DocType.PPTX -> R.string.collection_pptx
                DocType.TXT -> R.string.collection_txt
                DocType.EPUB -> R.string.collection_epub
            }
        )
        is SmartRule.AgeDays -> when (rule.days) {
            7 -> app.getString(R.string.collection_week)
            30 -> app.getString(R.string.collection_month)
            else -> app.resources.getQuantityString(R.plurals.last_days, rule.days, rule.days)
        }
        is SmartRule.WithTag -> app.getString(R.string.collection_tagged, rule.tag)
    }

    // ── prompts (rename / create) ─────────────────────────────────────────────────

    private fun savePrompt(value: String) {
        when (val current = sheet.value.current) {
            is LibrarySheet.RenameDocument -> renameDocument(current.docId, value)
            is LibrarySheet.NewFolder -> createFolder(value, current.parentId)
            is LibrarySheet.RenameFolder -> renameFolder(current.folderId, value)
            is LibrarySheet.RenameTag -> renameTag(current.tag, value)
            LibrarySheet.NewTag -> createTag(value)
            else -> Unit
        }
    }

    private fun renameDocument(docId: String, typed: String) {
        val doc = state.value.docs.firstOrNull { it.id == docId } ?: return
        val name = renameKeepingExtension(doc.document.name, typed)
        viewModelScope.launch {
            documents.rename(docId, name)
            toParent()
            post(app.getString(R.string.snack_renamed_to, name))
        }
    }

    private fun createFolder(name: String, parentId: String?) {
        viewModelScope.launch {
            folders.create(name, parentId)
            toParent()
            post(
                app.getString(
                    if (parentId == null) R.string.snack_folder_created else R.string.snack_subfolder_created
                )
            )
        }
    }

    private fun renameFolder(folderId: String, name: String) {
        viewModelScope.launch {
            folders.rename(folderId, name)
            toParent()
            post(app.getString(R.string.snack_folder_renamed))
        }
    }

    private fun renameTag(oldName: String, typed: String) {
        viewModelScope.launch {
            val renamed = tags.rename(oldName, typed)
            if (renamed && oldName in filter.value.tags) {
                filter.value = filter.value.copy(
                    tags = filter.value.tags - oldName + normalizeTagName(typed)
                )
            }
            toParent()
            post(
                app.getString(
                    if (renamed) R.string.snack_tag_renamed else R.string.snack_tag_exists
                )
            )
        }
    }

    private fun createTag(typed: String) {
        viewModelScope.launch {
            val created = tags.create(typed)
            toParent()
            post(
                app.getString(
                    if (created) R.string.snack_tag_created else R.string.snack_tag_exists
                )
            )
        }
    }

    private fun applyColour(hex: String) {
        when (val current = sheet.value.current) {
            is LibrarySheet.FolderColour -> viewModelScope.launch {
                folders.recolour(current.folderId, hex)
                toParent()
                post(app.getString(R.string.snack_colour_updated))
            }
            is LibrarySheet.TagColour -> viewModelScope.launch {
                tags.recolour(current.tag, hex)
                toParent()
                post(app.getString(R.string.snack_colour_updated))
            }
            else -> Unit
        }
    }

    // ── sheets and dialogs ───────────────────────────────────────────────────────

    private fun openSheet(next: LibrarySheet, parent: LibrarySheet = LibrarySheet.None) {
        sheet.value = SheetState(current = next, parent = parent)
    }

    private fun toParent() {
        sheet.value = SheetState(current = sheet.value.parent)
    }

    private fun dismissSheet() {
        sheet.value = SheetState()
    }

    private fun dismissDialog() {
        dialog.value = LibraryDialog.None
    }

    private fun confirmDialog() {
        when (val current = dialog.value) {
            LibraryDialog.None -> Unit
            is LibraryDialog.DeleteDocument -> deleteDocument(current.docId)
            is LibraryDialog.DeleteFolder -> deleteFolder(current.folderId)
            is LibraryDialog.DeleteTag -> deleteTag(current.tag)
        }
    }

    /** §8.7: confirm → remove from library, tabs and recents → snackbar with Undo. */
    private fun deleteDocument(docId: String) {
        viewModelScope.launch {
            pendingUndo = documents.snapshot(docId)
            documents.delete(docId)
            dialog.value = LibraryDialog.None
            sheet.value = SheetState()
            postUndo(app.getString(R.string.snack_document_deleted))
        }
    }

    private fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            folders.delete(folderId)
            if (filter.value.folderId == folderId) {
                filter.value = filter.value.copy(folderId = null)
            }
            dialog.value = LibraryDialog.None
            now.value = System.currentTimeMillis()
            post(app.getString(R.string.snack_folder_deleted))
        }
    }

    private fun deleteTag(name: String) {
        viewModelScope.launch {
            tags.delete(name)
            filter.value = filter.value.copy(tags = filter.value.tags - name)
            dialog.value = LibraryDialog.None
            now.value = System.currentTimeMillis()
            post(app.getString(R.string.snack_tag_deleted))
        }
    }

    private fun undo() {
        val snapshot = pendingUndo ?: return
        pendingUndo = null
        viewModelScope.launch {
            documents.restore(snapshot)
            post(app.getString(R.string.snack_document_restored))
        }
    }

    // ── snackbars ────────────────────────────────────────────────────────────────

    private fun post(text: String) {
        message.value = LeafMessage(text = text, key = ++messageKey)
    }

    private fun postUndo(text: String) {
        message.value = LeafMessage(
            text = text,
            key = ++messageKey,
            actionLabel = app.getString(R.string.action_undo),
            durationMs = LeafMotion.snackbarWithAction
        )
    }
}
