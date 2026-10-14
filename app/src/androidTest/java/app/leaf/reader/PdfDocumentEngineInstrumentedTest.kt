package app.leaf.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.leaf.reader.core.format.PdfBitmapCache
import app.leaf.reader.core.format.PdfDocumentEngineFactory
import app.leaf.reader.core.format.RenderTile
import android.net.Uri
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against a real PdfRenderer and a test-only fixture PDF on API 23 and current Android. */
@RunWith(AndroidJUnit4::class)
class PdfDocumentEngineInstrumentedTest {

    @Test
    fun real_fixture_pdf_renders_extracts_text_and_respects_cache_limits() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cache = PdfBitmapCache()
        val fixture = File(context.cacheDir, "fixtures/d1.pdf").apply {
            parentFile?.mkdirs()
            InstrumentationRegistry.getInstrumentation().context.assets.open("fixtures/d1.pdf")
                .use { input -> outputStream().use(input::copyTo) }
        }
        val engine = PdfDocumentEngineFactory(context, cache).open(Uri.fromFile(fixture).toString())
        val leases = mutableListOf<android.graphics.Bitmap>()
        try {
            assertEquals(6, engine.pageCount)
            val metrics = engine.pageMetrics(0)
            assertTrue(metrics.widthPoints > 0)
            assertTrue(metrics.heightPoints > metrics.widthPoints)

            val preview = engine.renderPreview(0)
            leases += preview
            assertFalse(preview.isRecycled)
            assertTrue(maxOf(preview.width, preview.height) <= 960)

            repeat(50) { swipe ->
                val bitmap = engine.renderTile(swipe % engine.pageCount, 1.5f, RenderTile(0, 0, 512, 512))
                assertFalse(bitmap.isRecycled)
                engine.releaseBitmap(bitmap)
                assertTrue(cache.cachedBytes <= cache.capacityBytes)
                assertTrue(cache.cachedFullResolutionPageCount <= 3)
            }
            assertTrue(cache.capacityBytes.toLong() <= Runtime.getRuntime().maxMemory() / 8L)

            val text = engine.pageText(0).joinToString(" ") { it.text }
            assertTrue("Seed text should be extracted by the selected platform/PDFBox path", text.contains("Simplicity", ignoreCase = true))

            val findMatches = engine.findInPage(0, "Simplicity")
            assertEquals(1, findMatches.size)
            assertTrue("Find should return normalized match geometry", findMatches.single().bounds.isNotEmpty())
        } finally {
            leases.forEach(engine::releaseBitmap)
            engine.close()
            cache.clear()
        }
    }
}
