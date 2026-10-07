package app.leaf.reader.feature.library

import app.leaf.reader.core.domain.LibraryDoc
import app.leaf.reader.core.domain.LibraryFilter
import app.leaf.reader.core.domain.LibraryView
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Folder
import app.leaf.reader.core.model.SmartCollection
import app.leaf.reader.core.model.SortField
import app.leaf.reader.core.model.Tag
import app.leaf.reader.core.ui.theme.LeafMotion

/**
 * Everything one frame of the Library needs (§6.1).
 *
 * The screen is dumb on purpose: it renders [LibraryContentState] and calls
 * [LibraryHandlers]. Counting, grouping and filtering live in `core/domain`, where they
 * are unit-tested without a device (§12, §13 row 2). Every field defaults, so a test or
 * a preview can build a state one piece at a time.
 */
data class LibraryContentState(
    /** `All 10 documents` · `Folder · Work` · `Collection · All PDFs` (§6.1). */
    val subtitle: String = "",
    /** Every document, unfiltered — sheets and counts read from here. */
    val docs: List<LibraryDoc> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val smart: List<SmartCollection> = emptyList(),
    /** The composed view: rows, group cards and chip counts (§8.5). */
    val view: LibraryView = LibraryView(),
    val filter: LibraryFilter = LibraryFilter(),
    /** The sort chip's label, e.g. `Date added · Newest`. */
    val sortLabel: String = "",
    val sortField: SortField = SortField.NAME,
    val sortAscending: Boolean = true,
    /** The persisted file-type grouping switch (§8.13). */
    val groupByType: Boolean = false,
    /** One clock for the whole render, so relative dates agree. */
    val now: Long = System.currentTimeMillis(),
    val sheet: LibrarySheet = LibrarySheet.None,
    val dialog: LibraryDialog = LibraryDialog.None,
    val message: LeafMessage? = null
)

/**
 * A snackbar waiting to be shown (§11). [key] changes on every post, so posting the
 * same text twice restarts the timer instead of being swallowed.
 */
data class LeafMessage(
    val text: String,
    val key: Int = 0,
    val actionLabel: String? = null,
    val durationMs: Long = LeafMotion.snackbar
)

/**
 * Every sheet the Library can open, as data.
 *
 * Sheets opened *on top of* a manager are pushed with that manager as their parent, so
 * saving or going back returns to the manager rather than closing the whole stack.
 */
sealed interface LibrarySheet {

    data object None : LibrarySheet

    // ── sorting ───────────────────────────────────────────────────────────────

    /** §6.1: direction, then Name · Date added · Last opened · File size. */
    data object Sort : LibrarySheet

    // ── documents ─────────────────────────────────────────────────────────────

    /** §6.7 long-press actions. */
    data class Actions(val docId: String) : LibrarySheet

    /** §8.8 move to folder. */
    data class MoveToFolder(val docId: String) : LibrarySheet

    /** §6.7 manage tags. */
    data class ManageTags(val docId: String) : LibrarySheet

    /** File info — the mockup's `infoSheet`. */
    data class FileInfo(val docId: String) : LibrarySheet

    /** §8.6: renaming keeps the extension. */
    data class RenameDocument(val docId: String) : LibrarySheet

    // ── folders ───────────────────────────────────────────────────────────────

    /** §6.1 folder manager: rename, recolour, new subfolder, delete. */
    data object FolderManager : LibrarySheet

    data class RenameFolder(val folderId: String) : LibrarySheet

    /** The 10-swatch colour sheet for one folder (§8.8). */
    data class FolderColour(val folderId: String) : LibrarySheet

    /** New folder; [parentId] non-null makes it a subfolder. */
    data class NewFolder(val parentId: String?) : LibrarySheet

    /** Picks which top-level folder a new subfolder goes into. */
    data object NewSubfolderPicker : LibrarySheet

    // ── tags ──────────────────────────────────────────────────────────────────

    /** §6.1 tag manager: usage counts, rename, recolour, delete. */
    data object TagManager : LibrarySheet

    data class RenameTag(val tag: String) : LibrarySheet

    /** The 10-swatch colour sheet for one tag (§8.9). */
    data class TagColour(val tag: String) : LibrarySheet

    data object NewTag : LibrarySheet

    // ── smart collections ─────────────────────────────────────────────────────

    /** §6.1 new-collection sheet: pick a rule, name it, done. */
    data object NewCollection : LibrarySheet

    data object PickCollectionType : LibrarySheet

    data object PickCollectionAge : LibrarySheet

    data object PickCollectionTag : LibrarySheet
}

/** The three destructives (§8.7, §8.8, §8.9), each behind a confirmation. */
sealed interface LibraryDialog {

    data object None : LibraryDialog

    data class DeleteDocument(val docId: String) : LibraryDialog

    data class DeleteFolder(val folderId: String) : LibraryDialog

    data class DeleteTag(val tag: String) : LibraryDialog
}

/** What the folder manager can ask for. */
sealed interface FolderAction {
    /** Filter the library down to this folder (and everything filed beneath it). */
    data class Open(val id: String) : FolderAction

    data class Rename(val id: String) : FolderAction

    data class Recolour(val id: String) : FolderAction

    data class Delete(val id: String) : FolderAction

    /** New folder at the top level. */
    data object NewTop : FolderAction

    /** New subfolder — shows the parent picker first. */
    data object NewNested : FolderAction

    data class NewSubfolderIn(val parentId: String) : FolderAction
}

/** What the tag manager can ask for. */
sealed interface TagAction {
    /** Filter the library down to this tag. */
    data class Open(val name: String) : TagAction

    data class Rename(val name: String) : TagAction

    data class Recolour(val name: String) : TagAction

    data class Delete(val name: String) : TagAction

    data object New : TagAction

    /** Add or remove one tag on one document (the manage-tags sheet). */
    data class Toggle(val docId: String, val name: String) : TagAction
}

/** The nine long-press actions (§6.7), minus the row-level ones. */
sealed interface DocumentAction {
    data class Rename(val id: String) : DocumentAction

    data class Favorite(val id: String) : DocumentAction

    data class ManageTags(val id: String) : DocumentAction

    data class Move(val id: String) : DocumentAction

    data class Info(val id: String) : DocumentAction

    data class Delete(val id: String) : DocumentAction
}

/**
 * The seven smart-collection rules as the user chooses them (§2.1). Three of them need
 * a second answer — a type, a number of days or a tag — so picking one of those opens
 * its own sheet and the rule is finished there.
 */
sealed interface RuleChoice {
    data object All : RuleChoice
    data object OfOneType : RuleChoice
    data object Age : RuleChoice
    data object InProgress : RuleChoice
    data object Unfiled : RuleChoice
    data object HasHighlights : RuleChoice
    data object WithTag : RuleChoice
}

/** The screen's whole event surface: one lambda per user action, none optional. */
data class LibraryHandlers(
    // ── rails, sort, grouping ─────────────────────────────────────────────────
    val onFolderChosen: (String?) -> Unit,
    val onSmartChosen: (String?) -> Unit,
    val onTagToggled: (String) -> Unit,
    val onSortClicked: () -> Unit,
    val onSortFieldChosen: (SortField) -> Unit,
    val onSortDirectionChosen: (Boolean) -> Unit,
    val onGroupToggled: () -> Unit,
    val onGroupOpened: (DocType) -> Unit,
    val onGroupClosed: () -> Unit,
    val onFoldersClicked: () -> Unit,
    val onTagsClicked: () -> Unit,
    val onNewFolder: () -> Unit,
    val onNewCollection: () -> Unit,
    val onNewTag: () -> Unit,
    // ── rows ──────────────────────────────────────────────────────────────────
    val onStar: (String) -> Unit,
    val onRowLongPress: (String) -> Unit,
    // ── managers ──────────────────────────────────────────────────────────────
    val onFolderAction: (FolderAction) -> Unit,
    val onTagAction: (TagAction) -> Unit,
    val onDocumentAction: (DocumentAction) -> Unit,
    val onMovedToFolder: (String, String?) -> Unit,
    val onPromptSave: (String) -> Unit,
    val onColourPicked: (String) -> Unit,
    // ── the collection composer ───────────────────────────────────────────────
    val onCollectionRulePicked: (RuleChoice) -> Unit,
    val onCollectionTypePicked: (DocType) -> Unit,
    val onCollectionAgePicked: (Int) -> Unit,
    val onCollectionTagPicked: (String) -> Unit,
    // ── chrome ────────────────────────────────────────────────────────────────
    val onDismissSheet: () -> Unit,
    val onDialogConfirm: () -> Unit,
    val onDismissDialog: () -> Unit,
    val onMessageAction: () -> Unit,
    val onMessageShown: () -> Unit
)
