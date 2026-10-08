package app.leaf.reader.core.format

import kotlin.math.ceil

/** Pure geometry for selecting visible tiles and adding the required overdraw ring. */
object PdfTileGrid {
    const val DEFAULT_TILE_SIZE = 512

    fun visibleTiles(
        pageWidth: Int,
        pageHeight: Int,
        visible: RenderTile,
        tileSize: Int = DEFAULT_TILE_SIZE,
        overdraw: Int = 1
    ): List<RenderTile> {
        require(pageWidth > 0 && pageHeight > 0)
        require(tileSize > 0)
        require(overdraw >= 0)
        val left = visible.left.coerceIn(0, pageWidth - 1)
        val top = visible.top.coerceIn(0, pageHeight - 1)
        val right = (visible.left.toLong() + visible.width).coerceIn(1L, pageWidth.toLong()).toInt()
        val bottom = (visible.top.toLong() + visible.height).coerceIn(1L, pageHeight.toLong()).toInt()
        val firstColumn = left / tileSize
        val lastColumn = (right - 1) / tileSize
        val firstRow = top / tileSize
        val lastRow = (bottom - 1) / tileSize
        val columns = (pageWidth + tileSize - 1) / tileSize
        val rows = (pageHeight + tileSize - 1) / tileSize
        val result = LinkedHashMap<Pair<Int, Int>, RenderTile>()

        for (row in (firstRow - overdraw).coerceAtLeast(0)..(lastRow + overdraw).coerceAtMost(rows - 1)) {
            for (column in (firstColumn - overdraw).coerceAtLeast(0)..(lastColumn + overdraw).coerceAtMost(columns - 1)) {
                val tileLeft = column * tileSize
                val tileTop = row * tileSize
                result[column to row] = RenderTile(
                    left = tileLeft,
                    top = tileTop,
                    width = minOf(tileSize, pageWidth - tileLeft),
                    height = minOf(tileSize, pageHeight - tileTop)
                )
            }
        }
        return result.values.toList()
    }

    fun pagePixels(metrics: PageMetrics, scale: Float): Pair<Int, Int> {
        require(scale.isFinite() && scale > 0f)
        return maxOf(1, ceil(metrics.widthPoints * scale).toInt()) to
            maxOf(1, ceil(metrics.heightPoints * scale).toInt())
    }
}
