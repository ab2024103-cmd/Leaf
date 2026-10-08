package app.leaf.reader

import app.leaf.reader.core.format.PageMetrics
import app.leaf.reader.core.format.PdfTileGrid
import app.leaf.reader.core.format.RenderTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfTileGridTest {

    @Test
    fun visible_tiles_include_one_tile_of_overdraw_and_clip_to_page_edges() {
        val tiles = PdfTileGrid.visibleTiles(
            pageWidth = 1_600,
            pageHeight = 1_600,
            visible = RenderTile(left = 600, top = 600, width = 100, height = 100)
        )

        assertEquals(9, tiles.size)
        assertTrue(tiles.any { it.left == 0 && it.top == 0 })
        assertTrue(tiles.any { it.left == 1_024 && it.top == 1_024 && it.width == 512 && it.height == 512 })
        assertTrue(tiles.all { it.left + it.width <= 1_600 && it.top + it.height <= 1_600 })
    }

    @Test
    fun visible_bottom_right_edge_does_not_request_another_tile() {
        val tiles = PdfTileGrid.visibleTiles(
            pageWidth = 1_024,
            pageHeight = 1_024,
            visible = RenderTile(left = 512, top = 512, width = 512, height = 512)
        )

        assertEquals(listOf(RenderTile(0, 0, 512, 512), RenderTile(512, 0, 512, 512), RenderTile(0, 512, 512, 512), RenderTile(512, 512, 512, 512)), tiles)
    }

    @Test
    fun output_size_is_scaled_and_never_zero() {
        assertEquals(1_224 to 1_584, PdfTileGrid.pagePixels(PageMetrics(612, 792), 2f))
        assertEquals(1 to 1, PdfTileGrid.pagePixels(PageMetrics(1, 1), 0.01f))
    }
}
