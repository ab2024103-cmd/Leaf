package app.leaf.reader.feature.reader

import app.leaf.reader.core.format.TextRun
import app.leaf.reader.core.model.NormalizedRect
import kotlin.math.max
import kotlin.math.min

/** Geometry-backed word used by the pragmatic PDF selection layer (§7.4). */
data class SelectableWord(
    val text: String,
    val bounds: List<NormalizedRect>
)

fun List<TextRun>.selectableWords(): List<SelectableWord> = buildList {
    this@selectableWords.forEach { run ->
        val ranges = wordRanges(run.text)
        ranges.forEach { range ->
            val glyphBounds = run.characterBounds
                .takeIf { it.size >= range.last + 1 }
                ?.subList(range.first, range.last + 1)
                ?.filter(NormalizedRect::isUsable)
                .orEmpty()
            val wordBounds = if (glyphBounds.isNotEmpty()) {
                mergeAdjacentRects(glyphBounds)
            } else {
                estimateWordBounds(run, range.first, range.last + 1)
            }
            if (wordBounds.isNotEmpty()) add(SelectableWord(run.text.substring(range), wordBounds))
        }
    }
}

fun nearestWordIndex(words: List<SelectableWord>, x: Float, y: Float): Int? =
    words.indices.minByOrNull { index ->
        words[index].bounds.minOfOrNull { it.distanceSquared(x, y) } ?: Float.MAX_VALUE
    }

fun selectionText(words: List<SelectableWord>, start: Int, end: Int): String {
    if (words.isEmpty()) return ""
    val first = min(start, end).coerceIn(words.indices)
    val last = max(start, end).coerceIn(words.indices)
    return words.subList(first, last + 1).map(SelectableWord::text).reduceOrNull { left, right ->
        val nextStartsWithPunctuation = right.firstOrNull() in setOf(',', '.', ';', ':', '?', '!', ')', ']', '}')
        val previousEndsWithOpening = left.lastOrNull() in setOf('(', '[', '{', '“', '‘')
        left + if (nextStartsWithPunctuation || previousEndsWithOpening) right else " $right"
    }.orEmpty()
}

fun selectionBounds(words: List<SelectableWord>, start: Int, end: Int): List<NormalizedRect> {
    if (words.isEmpty()) return emptyList()
    val first = min(start, end).coerceIn(words.indices)
    val last = max(start, end).coerceIn(words.indices)
    return mergeAdjacentRects(words.subList(first, last + 1).flatMap(SelectableWord::bounds))
}

private fun wordRanges(text: String): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var start = -1
    text.forEachIndexed { index, character ->
        val isWord = character.isLetterOrDigit() ||
            (character in setOf('\'', '’', '-') && index > 0 && index < text.lastIndex &&
                text[index - 1].isLetterOrDigit() && text[index + 1].isLetterOrDigit())
        if (isWord && start < 0) start = index
        if (!isWord && start >= 0) {
            ranges += start until index
            start = -1
        }
    }
    if (start >= 0) ranges += start until text.length
    return ranges
}

private fun estimateWordBounds(run: TextRun, start: Int, endExclusive: Int): List<NormalizedRect> {
    val line = run.bounds.firstOrNull(NormalizedRect::isUsable) ?: return emptyList()
    val length = run.text.length.coerceAtLeast(1)
    val rtl = run.text.firstOrNull(Char::isLetter)?.let { it in '\u0590'..'\u08ff' } == true
    val startFraction = start.toFloat() / length
    val endFraction = endExclusive.toFloat() / length
    val leftFraction = if (rtl) 1f - endFraction else startFraction
    val rightFraction = if (rtl) 1f - startFraction else endFraction
    val left = line.left + line.width * leftFraction
    val right = line.left + line.width * rightFraction
    return listOf(line.copy(left = left.coerceAtLeast(line.left), right = right.coerceAtMost(line.right)))
}

private fun mergeAdjacentRects(rectangles: List<NormalizedRect>): List<NormalizedRect> {
    val sorted = rectangles.filter(NormalizedRect::isUsable)
        .sortedWith(compareBy<NormalizedRect> { it.top }.thenBy { it.left })
    if (sorted.size < 2) return sorted
    val result = mutableListOf<NormalizedRect>()
    sorted.forEach { rect ->
        val previous = result.lastOrNull()
        if (previous != null && kotlin.math.abs(previous.top - rect.top) < 0.012f &&
            rect.left <= previous.right + 0.012f && previous.left <= rect.right + 0.012f
        ) {
            result[result.lastIndex] = NormalizedRect(
                left = min(previous.left, rect.left),
                top = min(previous.top, rect.top),
                right = max(previous.right, rect.right),
                bottom = max(previous.bottom, rect.bottom)
            )
        } else {
            result += rect
        }
    }
    return result
}

private fun NormalizedRect.isUsable(): Boolean =
    left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() && right > left && bottom > top

private fun NormalizedRect.distanceSquared(x: Float, y: Float): Float {
    val dx = when {
        x < left -> left - x
        x > right -> x - right
        else -> 0f
    }
    val dy = when {
        y < top -> top - y
        y > bottom -> y - bottom
        else -> 0f
    }
    return dx * dx + dy * dy
}

private val NormalizedRect.width: Float get() = (right - left).coerceAtLeast(0f)
