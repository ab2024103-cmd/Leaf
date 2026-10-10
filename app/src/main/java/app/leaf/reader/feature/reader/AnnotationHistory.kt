package app.leaf.reader.feature.reader

import app.leaf.reader.core.model.Bookmark
import app.leaf.reader.core.model.Highlight

/** One persisted annotation state transition. The history itself is intentionally session-only. */
data class AnnotationChange(
    val before: AnnotationSnapshot,
    val after: AnnotationSnapshot,
    /** A user-facing action verb, such as "Highlight removed". */
    val label: String
)

data class AnnotationSnapshot(
    val bookmarks: List<Bookmark>,
    val highlights: List<Highlight>
)

/** Bounded undo/redo history for one document, not written to Room or DataStore. */
class AnnotationHistory(private val capacity: Int = MAX_STEPS) {
    private val undo = ArrayDeque<AnnotationChange>()
    private val redo = ArrayDeque<AnnotationChange>()

    init {
        require(capacity > 0)
    }

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()
    val undoLabel: String? get() = undo.lastOrNull()?.label
    val redoLabel: String? get() = redo.lastOrNull()?.label

    fun record(change: AnnotationChange) {
        if (change.before == change.after) return
        undo.addLast(change)
        while (undo.size > capacity) undo.removeFirst()
        redo.clear()
    }

    fun takeUndo(): AnnotationChange? = undo.removeLastOrNull()?.also(redo::addLast)

    fun takeRedo(): AnnotationChange? = redo.removeLastOrNull()?.also(undo::addLast)

    companion object {
        const val MAX_STEPS = 50
    }
}
