package app.leaf.reader

import app.leaf.reader.core.model.Bookmark
import app.leaf.reader.core.model.Highlight
import app.leaf.reader.feature.reader.AnnotationChange
import app.leaf.reader.feature.reader.AnnotationHistory
import app.leaf.reader.feature.reader.AnnotationSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotationHistoryTest {
    @Test
    fun history_is_bounded_to_fifty_steps_and_redo_tracks_undo_order() {
        val history = AnnotationHistory()
        repeat(60) { step ->
            history.record(
                AnnotationChange(
                    before = snapshot(step),
                    after = snapshot(step + 1),
                    label = "step-$step"
                )
            )
        }

        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
        repeat(50) { offset ->
            val expected = 59 - offset
            assertEquals("step-$expected", history.takeUndo()?.label)
        }
        assertNull(history.takeUndo())
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)

        repeat(50) { offset ->
            val expected = 10 + offset
            assertEquals("step-$expected", history.takeRedo()?.label)
        }
        assertNull(history.takeRedo())
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun recording_after_undo_discards_the_redo_branch() {
        val history = AnnotationHistory()
        history.record(AnnotationChange(snapshot(0), snapshot(1), "first"))
        history.takeUndo()
        history.record(AnnotationChange(snapshot(0), snapshot(2), "replacement"))

        assertFalse(history.canRedo)
        assertEquals("replacement", history.takeUndo()?.label)
    }

    private fun snapshot(value: Int): AnnotationSnapshot = AnnotationSnapshot(
        bookmarks = if (value == 0) emptyList() else listOf(
            Bookmark("b$value", "doc", value, "Page $value", value.toLong())
        ),
        highlights = if (value == 0) emptyList() else listOf(
            Highlight("h$value", "doc", value, app.leaf.reader.core.model.HighlightColor.YELLOW, "text $value", emptyList(), null, null, value.toLong())
        )
    )
}
