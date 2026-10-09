package app.leaf.reader.core.format

import android.graphics.Bitmap
import app.leaf.reader.core.model.NormalizedRect

/** One line or contiguous text run extracted from a document page. */
data class TextRun(
    val text: String,
    val bounds: List<NormalizedRect> = emptyList()
)

/** Geometry and source text for a search match; all bounds are in normalized page space. */
data class MatchRect(
    val text: String,
    val bounds: List<NormalizedRect>
)

data class PageMetrics(
    val widthPoints: Int,
    val heightPoints: Int
) {
    init {
        require(widthPoints > 0)
        require(heightPoints > 0)
    }
}

/** A rectangle in the rendered page's output-pixel coordinate space. */
data class RenderTile(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
) {
    init {
        require(left >= 0 && top >= 0)
        require(width > 0 && height > 0)
    }
}

/**
 * Format-neutral contract from §7. PDF is the only production implementation in M3;
 * the other formats are deliberately added in M6, not represented by placeholders.
 */
interface DocumentEngine : AutoCloseable {
    val pageCount: Int

    suspend fun pageMetrics(pageIndex: Int): PageMetrics

    /** A fast, full-page preview rendered at no more than half scale. */
    suspend fun renderPreview(pageIndex: Int, maxEdgePixels: Int = 960): Bitmap

    /** Render only [tile] at the requested PDF-point-to-output-pixel scale. */
    suspend fun renderTile(pageIndex: Int, scale: Float, tile: RenderTile): Bitmap

    /** Hold a rendered bitmap while a Compose surface is drawing it. */
    fun retainBitmap(bitmap: Bitmap) = Unit

    /** Release a bitmap previously retained by the reader. */
    fun releaseBitmap(bitmap: Bitmap) = Unit

    suspend fun pageText(pageIndex: Int): List<TextRun>

    suspend fun findInPage(pageIndex: Int, query: String): List<MatchRect>

    /** Batch geometry extraction lets PDF engines parse one source once per Find query. */
    suspend fun findInPages(pageIndices: List<Int>, query: String): Map<Int, List<MatchRect>> {
        val matches = LinkedHashMap<Int, List<MatchRect>>()
        for (pageIndex in pageIndices.distinct()) {
            matches[pageIndex] = findInPage(pageIndex, query)
        }
        return matches
    }

    /** Extract every page once in the background so M4 search uses the Room cache. */
    suspend fun extractAllText(onProgress: suspend (completed: Int, total: Int) -> Unit): List<List<TextRun>>
}

fun interface DocumentEngineFactory {
    suspend fun open(uri: String): DocumentEngine
}
