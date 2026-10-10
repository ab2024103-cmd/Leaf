package app.leaf.reader

import app.leaf.reader.core.format.TextRun
import app.leaf.reader.core.model.NormalizedRect
import app.leaf.reader.feature.reader.nearestWordIndex
import app.leaf.reader.feature.reader.selectableWords
import app.leaf.reader.feature.reader.selectionBounds
import app.leaf.reader.feature.reader.selectionText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSelectionTest {
    @Test
    fun pdf_text_runs_select_the_nearest_word_and_merge_character_bounds() {
        val text = "Leaf rules"
        val characterBounds = text.mapIndexed { index, char ->
            if (char.isWhitespace()) {
                NormalizedRect(0f, 0f, 0f, 0f)
            } else {
                val left = 0.1f + index * 0.035f
                NormalizedRect(left, 0.2f, left + 0.03f, 0.23f)
            }
        }
        val words = listOf(
            TextRun(
                text = text,
                bounds = listOf(NormalizedRect(0.1f, 0.2f, 0.46f, 0.23f)),
                characterBounds = characterBounds
            )
        ).selectableWords()

        assertEquals(listOf("Leaf", "rules"), words.map { it.text })
        assertEquals(0, nearestWordIndex(words, 0.18f, 0.21f))
        assertEquals(1, nearestWordIndex(words, 0.39f, 0.21f))
        assertEquals("Leaf rules", selectionText(words, 0, 1))

        val bounds = selectionBounds(words, 0, 1)
        assertEquals(2, bounds.size)
        assertTrue(bounds.first().left <= 0.1f)
        assertTrue(bounds.last().right > bounds.first().right)
    }

    @Test
    fun text_without_character_geometry_uses_a_normalized_line_estimate() {
        val words = listOf(
            TextRun("chapter notes", listOf(NormalizedRect(0.2f, 0.4f, 0.8f, 0.44f)))
        ).selectableWords()

        assertEquals(listOf("chapter", "notes"), words.map { it.text })
        assertEquals(0, nearestWordIndex(words, 0.32f, 0.42f))
        assertEquals(1, nearestWordIndex(words, 0.7f, 0.42f))
    }
}
