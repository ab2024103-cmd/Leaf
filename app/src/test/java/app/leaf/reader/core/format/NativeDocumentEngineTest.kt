package app.leaf.reader.core.format

import android.graphics.Color
import app.leaf.reader.core.model.DocType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NativeDocumentEngineTest {

    @Test
    fun measured_txt_pages_keep_search_text_and_page_anchors_in_sync() = runBlocking {
        val source = List(420) { "Leaf measures every line before it chooses a stable page anchor." }
            .joinToString("\n")
        val engine = NativeDocumentEngine.text(DocType.TXT, source, "Page")
        try {
            assertTrue(engine.pageCount > 1)
            assertEquals("Page 1", engine.pageLabel(0))
            assertEquals("Page ${engine.pageCount}", engine.pageLabel(engine.pageCount - 1))
            assertTrue(engine.pageText(0).joinToString(" ") { it.text }.contains("Leaf measures"))
            assertFalse(engine.findInPage(0, "stable").isEmpty())
            assertTrue(engine.renderPreview(0, 320).let { bitmap ->
                val valid = bitmap.width > 0 && bitmap.height > 0
                bitmap.recycle()
                valid
            })
        } finally {
            engine.close()
        }
    }

    @Test
    fun workbook_grid_and_slide_pages_have_labels_text_and_search_geometry() = runBlocking {
        val workbook = NativeDocumentEngine.xlsx(
            listOf("Revenue" to listOf(listOf("Region", "Amount"), listOf("North", "1250")))
        )
        val slides = NativeDocumentEngine.pptx(
            listOf(SlidePage("Quarterly review", listOf("Revenue grew"), 1, 1))
        )
        try {
            assertEquals("Revenue · 1/1", workbook.pageLabel(0))
            assertTrue(workbook.pageText(0).any { it.text == "North" })
            assertTrue(workbook.findInPage(0, "1250").single().bounds.isNotEmpty())
            assertEquals("Slide 1 / 1", slides.pageLabel(0))
            assertTrue(slides.pageText(0).any { it.text == "Quarterly review" })
            assertFalse(slides.findInPage(0, "Revenue").isEmpty())

            workbook.setReaderColors(Color.BLACK, Color.DKGRAY, Color.WHITE, 0xFFBFC9C2.toInt())
            val themedGrid = workbook.renderPreview(0, 300)
            try {
                assertTrue(themedGrid.width > 0)
            } finally {
                themedGrid.recycle()
            }
        } finally {
            workbook.close()
            slides.close()
        }
    }
}
