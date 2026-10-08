package app.leaf.reader

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.leaf.reader.core.format.PdfBitmapCache
import app.leaf.reader.core.format.PdfDocumentEngineFactory
import app.leaf.reader.core.format.RenderTile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against a real PdfRenderer and bundled demo PDF on API 23 and current Android. */
@RunWith(AndroidJUnit4::class)
class PdfDocumentEngineInstrumentedTest {

    @Test
    fun real_demo_pdf_renders_extracts_text_and_respects_cache_limits() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cache = PdfBitmapCache()
        val engine = PdfDocumentEngineFactory(context, cache).open("leaf-demo://document/d1")
        val leases = mutableListOf<android.graphics.Bitmap>()
        try {
            assertEquals(6, engine.pageCount)
            val metrics = engine.pageMetrics(0)
            assertTrue(metrics.widthPoints > 0)
            assertTrue(metrics.heightPoints > metrics.widthPoints)

            val previewStarted = android.os.SystemClock.elapsedRealtimeNanos()
            val preview = engine.renderPreview(0)
            leases += preview
            assertFalse(preview.isRecycled)
            assertTrue(maxOf(preview.width, preview.height) <= 960)

            val previewMillis = (android.os.SystemClock.elapsedRealtimeNanos() - previewStarted) / 1_000_000L
            var totalTileNanos = 0L
            repeat(50) { swipe ->
                val started = android.os.SystemClock.elapsedRealtimeNanos()
                val bitmap = engine.renderTile(swipe % engine.pageCount, 1.5f, RenderTile(0, 0, 512, 512))
                totalTileNanos += android.os.SystemClock.elapsedRealtimeNanos() - started
                assertFalse(bitmap.isRecycled)
                engine.releaseBitmap(bitmap)
                assertTrue(cache.cachedBytes <= cache.capacityBytes)
                assertTrue(cache.cachedFullResolutionPageCount <= 3)
            }
            assertTrue(cache.capacityBytes.toLong() <= Runtime.getRuntime().maxMemory() / 8L)
            val meanTileMillis = totalTileNanos / 50_000_000.0
            Log.i("LeafPdfPerf", "API ${Build.VERSION.SDK_INT}: preview=${previewMillis}ms, mean tile=${meanTileMillis}ms, cached=${cache.cachedBytes}/${cache.capacityBytes} bytes")

            val text = engine.pageText(0).joinToString(" ") { it.text }
            assertTrue("Seed text should be extracted by the selected platform/PDFBox path", text.contains("Simplicity", ignoreCase = true))
        } finally {
            leases.forEach(engine::releaseBitmap)
            engine.close()
            cache.clear()
        }
    }
}
