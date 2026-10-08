package app.leaf.reader

import android.app.Application
import android.graphics.Bitmap
import app.leaf.reader.core.format.PdfBitmapCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PdfBitmapCacheTest {

    @Test
    fun cache_caps_resolution_pages_and_keeps_leases_drawable_until_release() {
        val cache = PdfBitmapCache(maxBytes = 32_768)
        val retained = mutableListOf<Bitmap>()

        for (page in 0..3) {
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val key = "doc/$page/${1.5f.toBits()}/0/0/32/32"
            assertTrue(cache.putRetained(key, bitmap) === bitmap)
            retained += bitmap
        }

        assertEquals(3, cache.cachedFullResolutionPageCount)
        assertTrue(cache.cachedBytes <= cache.capacityBytes)
        assertFalse("evicted page is still leased by the UI", retained.first().isRecycled)

        cache.onTrimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
        assertEquals(0, cache.cachedBytes)
        assertFalse("trim must not recycle a bitmap Compose still draws", retained.last().isRecycled)
        retained.forEach(cache::release)
        assertTrue(retained.all { it.isRecycled })
    }
}
