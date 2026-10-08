package app.leaf.reader.core.format

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.LruCache
import java.util.IdentityHashMap
import java.util.LinkedHashMap

/**
 * A byte-bounded LRU shared by open PDF engines. A rendered bitmap is leased by the
 * reader while it is composed, so cache eviction and trim-memory never recycle pixels
 * that Compose is still drawing. Full-resolution tiles are also limited to three pages.
 */
class PdfBitmapCache(maxBytes: Int = defaultCapacityBytes()) : ComponentCallbacks2 {
    private val leases = IdentityHashMap<Bitmap, Int>()
    private val pendingRecycle = IdentityHashMap<Bitmap, Boolean>()
    private val fullResolutionPages = LinkedHashMap<String, MutableSet<String>>()
    private val pageByKey = HashMap<String, String>()

    val cachedBytes: Int
        @Synchronized get() = cache.size()

    val capacityBytes: Int
        @Synchronized get() = cache.maxSize()

    val cachedFullResolutionPageCount: Int
        @Synchronized get() = fullResolutionPages.size

    private val cache = object : LruCache<String, Bitmap>(maxBytes.coerceAtLeast(1)) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount.coerceAtLeast(1)

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            forgetFullResolutionKey(key)
            recycleWhenSafe(oldValue)
        }
    }

    @Synchronized
    fun getRetained(key: String): Bitmap? = cache.get(key)?.also { bitmap ->
        retain(bitmap)
        touchFullResolutionPage(key)
    }

    /** Inserts and returns a retained bitmap; its caller must eventually [release]. */
    @Synchronized
    fun putRetained(key: String, bitmap: Bitmap): Bitmap {
        cache.get(key)?.let { existing ->
            if (existing !== bitmap && !bitmap.isRecycled) bitmap.recycle()
            retain(existing)
            touchFullResolutionPage(key)
            return existing
        }

        retain(bitmap)
        cache.put(key, bitmap)
        if (cache.get(key) === bitmap) {
            trackFullResolutionKey(key)
            trimFullResolutionPages()
        }
        return bitmap
    }

    @Synchronized
    fun retain(bitmap: Bitmap) {
        if (!bitmap.isRecycled) leases[bitmap] = (leases[bitmap] ?: 0) + 1
    }

    @Synchronized
    fun release(bitmap: Bitmap) {
        val count = leases[bitmap] ?: return
        if (count > 1) {
            leases[bitmap] = count - 1
            return
        }
        leases.remove(bitmap)
        if (pendingRecycle.remove(bitmap) != null && !bitmap.isRecycled) bitmap.recycle()
    }

    @Synchronized
    fun clear() {
        cache.evictAll()
        // LruCache reports each removal; this is a defensive reset for keys evicted earlier.
        fullResolutionPages.clear()
        pageByKey.clear()
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) clear()
    }

    override fun onLowMemory() = clear()

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    private fun recycleWhenSafe(bitmap: Bitmap) {
        if (leases.containsKey(bitmap)) pendingRecycle[bitmap] = true
        else if (!bitmap.isRecycled) bitmap.recycle()
    }

    private fun trackFullResolutionKey(key: String) {
        val page = pageIdentity(key) ?: return
        fullResolutionPages.getOrPut(page) { linkedSetOf() }.add(key)
        pageByKey[key] = page
    }

    private fun touchFullResolutionPage(key: String) {
        val page = pageByKey[key] ?: return
        val keys = fullResolutionPages.remove(page) ?: return
        fullResolutionPages[page] = keys
    }

    private fun forgetFullResolutionKey(key: String) {
        val page = pageByKey.remove(key) ?: return
        val keys = fullResolutionPages[page] ?: return
        keys.remove(key)
        if (keys.isEmpty()) fullResolutionPages.remove(page)
    }

    private fun trimFullResolutionPages() {
        while (fullResolutionPages.size > MAX_FULL_RESOLUTION_PAGES) {
            val oldest = fullResolutionPages.entries.firstOrNull() ?: return
            oldest.value.toList().forEach(cache::remove)
            fullResolutionPages.remove(oldest.key)
        }
    }

    private fun pageIdentity(key: String): String? {
        val parts = key.split('/', limit = 4)
        if (parts.size < 4) return null
        val scale = parts[2].toIntOrNull()?.let(Float::fromBits) ?: return null
        return if (scale > PREVIEW_SCALE_LIMIT) "${parts[0]}/${parts[1]}" else null
    }

    companion object {
        private const val MAX_FULL_RESOLUTION_PAGES = 3
        private const val PREVIEW_SCALE_LIMIT = 0.5f

        fun defaultCapacityBytes(maxMemoryBytes: Long = Runtime.getRuntime().maxMemory()): Int =
            (maxMemoryBytes / 8L).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
    }
}
