package app.leaf.reader.core.format

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.content.PdfPageTextContent
import android.graphics.pdf.models.PageMatchBounds
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import app.leaf.reader.core.model.NormalizedRect
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock as withReentrantLock
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.min

/** Opens a URI-backed PDF. The PdfRenderer and its descriptor have one clear owner. */
class PdfDocumentEngineFactory(
    context: Context,
    private val bitmaps: PdfBitmapCache
) : DocumentEngineFactory {
    private val appContext = context.applicationContext
    private val demoMutex = Mutex()

    override suspend fun open(uri: String): DocumentEngine = withContext(Dispatchers.IO) {
        val parsed = Uri.parse(uri)
        val descriptor: ParcelFileDescriptor
        val textInput: () -> InputStream
        when (parsed.scheme) {
            "leaf-demo" -> {
                val demoFile = demoMutex.withLock { materializeDemoPdf(parsed) }
                descriptor = ParcelFileDescriptor.open(demoFile, ParcelFileDescriptor.MODE_READ_ONLY)
                textInput = { demoFile.inputStream() }
            }
            "content" -> {
                descriptor = appContext.contentResolver.openFileDescriptor(parsed, "r")
                    ?: throw IOException("The document provider did not return a file descriptor")
                textInput = {
                    appContext.contentResolver.openInputStream(parsed)
                        ?: throw IOException("The document provider could not open this PDF")
                }
            }
            "file" -> {
                val file = parsed.path?.let(::File) ?: throw IOException("The PDF path is missing")
                descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                textInput = { file.inputStream() }
            }
            else -> throw IOException("Unsupported PDF URI")
        }
        try {
            val renderer = PdfRenderer(descriptor)
            PdfDocumentEngine(appContext, parsed, descriptor, renderer, textInput, bitmaps)
        } catch (error: Throwable) {
            descriptor.close()
            throw error
        }
    }

    private fun materializeDemoPdf(uri: Uri): File {
        val id = uri.lastPathSegment?.takeIf { it.matches(Regex("d[0-9]+")) }
            ?: throw IOException("Unknown demo PDF")
        val targetDir = File(appContext.cacheDir, "demo-pdf").apply { mkdirs() }
        val target = File(targetDir, "$id.pdf")
        if (!target.exists() || target.length() == 0L) {
            val temp = File(targetDir, "$id.pdf.tmp")
            appContext.assets.open("demo-pdf/$id.pdf").use { input -> temp.outputStream().use(input::copyTo) }
            if (!temp.renameTo(target)) {
                temp.delete()
                throw IOException("Could not prepare the demo PDF")
            }
        }
        return target
    }
}

/**
 * Android's API-23-safe PdfRenderer implementation. All access to one renderer is
 * serialized; rendering only allocates the requested visible tile.
 */
class PdfDocumentEngine internal constructor(
    private val context: Context,
    private val uri: Uri,
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    private val textInput: () -> InputStream,
    private val bitmaps: PdfBitmapCache
) : DocumentEngine {
    private val rendererLock = ReentrantLock()
    private val cachePrefix = UUID.nameUUIDFromBytes(uri.toString().toByteArray(Charsets.UTF_8)).toString()
    @Volatile private var closed = false

    override val pageCount: Int = renderer.pageCount

    override suspend fun pageMetrics(pageIndex: Int): PageMetrics = withContext(Dispatchers.IO) {
        rendererLock.withReentrantLock {
            ensurePage(pageIndex)
            renderer.openPage(pageIndex).use { page -> PageMetrics(page.width, page.height) }
        }
    }

    override suspend fun renderPreview(pageIndex: Int, maxEdgePixels: Int): Bitmap {
        require(maxEdgePixels > 0)
        val metrics = pageMetrics(pageIndex)
        val scale = min(0.5f, maxEdgePixels.toFloat() / maxOf(metrics.widthPoints, metrics.heightPoints))
            .coerceAtLeast(0.01f)
        val (width, height) = PdfTileGrid.pagePixels(metrics, scale)
        return renderTile(pageIndex, scale, RenderTile(0, 0, width, height))
    }

    override suspend fun renderTile(pageIndex: Int, scale: Float, tile: RenderTile): Bitmap =
        withContext(Dispatchers.IO) {
            require(scale.isFinite() && scale > 0f)
            rendererLock.withReentrantLock {
                ensurePage(pageIndex)
                renderer.openPage(pageIndex).use { page ->
                    val pageWidth = maxOf(1, ceil(page.width * scale).toInt())
                    val pageHeight = maxOf(1, ceil(page.height * scale).toInt())
                    val left = tile.left.coerceIn(0, pageWidth - 1)
                    val top = tile.top.coerceIn(0, pageHeight - 1)
                    val width = min(tile.width, pageWidth - left).coerceAtLeast(1)
                    val height = min(tile.height, pageHeight - top).coerceAtLeast(1)
                    val key = "$cachePrefix/$pageIndex/${scale.toBits()}/$left/$top/$width/$height"
                    bitmaps.getRetained(key)?.let { return@withReentrantLock it }
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    try {
                        val matrix = Matrix().apply {
                            setValues(
                                floatArrayOf(
                                    scale, 0f, -left.toFloat(),
                                    0f, scale, -top.toFloat(),
                                    0f, 0f, 1f
                                )
                            )
                        }
                        page.render(bitmap, Rect(0, 0, width, height), matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.putRetained(key, bitmap)
                    } catch (error: Throwable) {
                        if (!bitmap.isRecycled) bitmap.recycle()
                        throw error
                    }
                }
            }
        }

    override fun retainBitmap(bitmap: Bitmap) = bitmaps.retain(bitmap)

    override fun releaseBitmap(bitmap: Bitmap) = bitmaps.release(bitmap)

    override suspend fun pageText(pageIndex: Int): List<TextRun> = withContext(Dispatchers.IO) {
        ensurePage(pageIndex)
        if (Build.VERSION.SDK_INT >= 35) {
            readPlatformText(pageIndex)
        } else {
            readPdfBoxPage(pageIndex)
        }
    }

    override suspend fun findInPage(pageIndex: Int, query: String): List<MatchRect> =
        findInPages(listOf(pageIndex), query)[pageIndex].orEmpty()

    override suspend fun findInPages(pageIndices: List<Int>, query: String): Map<Int, List<MatchRect>> =
        withContext(Dispatchers.IO) {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isEmpty()) return@withContext emptyMap()
            val pages = pageIndices.distinct()
            pages.forEach(::ensurePage)
            if (Build.VERSION.SDK_INT >= 35) {
                val result = LinkedHashMap<Int, List<MatchRect>>()
                for (pageIndex in pages) {
                    result[pageIndex] = searchPlatformText(pageIndex, normalizedQuery)
                }
                result
            } else {
                searchPdfBoxPages(pages, normalizedQuery)
            }
        }

    /** PDFBox supplies glyph positions on API 23–34 so Find can paint actual hit bounds. */
    private fun searchPdfBoxPages(pageIndices: List<Int>, query: String): Map<Int, List<MatchRect>> {
        PDFBoxResourceLoader.init(context)
        if (pageIndices.isEmpty() || query.isEmpty()) return emptyMap()
        return textInput().use { input ->
            PDDocument.load(input).use { document ->
                buildMap {
                    pageIndices.forEach { pageIndex ->
                        if (pageIndex !in 0 until document.numberOfPages) {
                            throw IndexOutOfBoundsException("PDF page is outside the document")
                        }
                        val page = document.getPage(pageIndex)
                        val cropBox = page.cropBox
                        val rotation = ((page.rotation % 360) + 360) % 360
                        val pageWidth = if (rotation == 90 || rotation == 270) cropBox.height else cropBox.width
                        val pageHeight = if (rotation == 90 || rotation == 270) cropBox.width else cropBox.height
                        val stripper = PdfBoxMatchStripper(query, pageWidth, pageHeight).apply {
                            sortByPosition = true
                            startPage = pageIndex + 1
                            endPage = pageIndex + 1
                        }
                        stripper.getText(document)
                        put(pageIndex, stripper.matches.toList())
                    }
                }
            }
        }
    }

    override suspend fun extractAllText(onProgress: suspend (completed: Int, total: Int) -> Unit): List<List<TextRun>> =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= 35) {
                buildList {
                    for (page in 0 until pageCount) {
                        add(pageText(page))
                        onProgress(page + 1, pageCount)
                    }
                }
            } else {
                PDFBoxResourceLoader.init(context)
                textInput().use { input ->
                    PDDocument.load(input).use { document ->
                        val stripper = PDFTextStripper().apply { sortByPosition = true }
                        buildList {
                            for (page in 0 until pageCount) {
                                stripper.startPage = page + 1
                                stripper.endPage = page + 1
                                val text = stripper.getText(document)
                                add(text.lineSequence().filter(String::isNotBlank).map { TextRun(it) }.toList())
                                onProgress(page + 1, pageCount)
                            }
                        }
                    }
                }
            }
        }

    @RequiresApi(35)
    private suspend fun readPlatformText(pageIndex: Int): List<TextRun> = rendererLock.withReentrantLock {
        ensurePage(pageIndex)
        renderer.openPage(pageIndex).use { page ->
            page.textContents.map { item: PdfPageTextContent ->
                TextRun(item.text, item.bounds.map { it.normalized(page.width, page.height) })
            }
        }
    }

    @RequiresApi(35)
    private suspend fun searchPlatformText(pageIndex: Int, query: String): List<MatchRect> =
        rendererLock.withReentrantLock {
            ensurePage(pageIndex)
            renderer.openPage(pageIndex).use { page ->
                page.searchText(query).map { match: PageMatchBounds ->
                    MatchRect(
                        text = query,
                        bounds = match.bounds.map { it.normalized(page.width, page.height) }
                    )
                }
            }
        }

    private suspend fun readPdfBoxPage(pageIndex: Int): List<TextRun> {
        PDFBoxResourceLoader.init(context)
        return textInput().use { input ->
            PDDocument.load(input).use { document ->
                if (pageIndex !in 0 until document.numberOfPages) throw IndexOutOfBoundsException("PDF page is outside the document")
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    startPage = pageIndex + 1
                    endPage = pageIndex + 1
                }
                stripper.getText(document).lineSequence().filter(String::isNotBlank).map { TextRun(it) }.toList()
            }
        }
    }

    private fun ensurePage(pageIndex: Int) {
        check(!closed) { "PDF document is closed" }
        if (pageIndex !in 0 until pageCount) throw IndexOutOfBoundsException("PDF page is outside the document")
    }

    override fun close() = rendererLock.withReentrantLock {
        if (closed) return@withReentrantLock
        closed = true
        try {
            renderer.close()
        } finally {
            descriptor.close()
        }
    }

    private fun RectF.normalized(pageWidth: Int, pageHeight: Int): NormalizedRect = NormalizedRect(
        left = (left / pageWidth).coerceIn(0f, 1f),
        top = (top / pageHeight).coerceIn(0f, 1f),
        right = (right / pageWidth).coerceIn(0f, 1f),
        bottom = (bottom / pageHeight).coerceIn(0f, 1f)
    )
}

/** API 23–34 fallback: search PDFBox text lines and return normalized glyph geometry. */
private class PdfBoxMatchStripper(
    private val query: String,
    private val pageWidth: Float,
    private val pageHeight: Float
) : PDFTextStripper() {
    val matches = mutableListOf<MatchRect>()

    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        super.writeString(text, textPositions)
        if (text.isEmpty() || query.isEmpty()) return

        val positionsByCharacter = mapPositions(text, textPositions)
        var start = 0
        while (start <= text.length - query.length) {
            val matchStart = text.indexOf(query, start, ignoreCase = true)
            if (matchStart < 0) break
            val matchEnd = matchStart + query.length
            val matchedPositions = positionsByCharacter.subList(matchStart, matchEnd).filterNotNull()
            val geometry = matchedPositions.ifEmpty { textPositions }
            matches += MatchRect(
                text = text.substring(matchStart, matchEnd),
                bounds = mergeAdjacent(geometry.map(::normalizedBounds))
            )
            start = matchEnd
        }
    }

    private fun mapPositions(text: String, positions: List<TextPosition>): List<TextPosition?> {
        if (positions.size == text.length) return positions
        val positionCharacters = positions.flatMap { position ->
            position.unicode.orEmpty().map { character -> character to position }
        }
        val mapped = MutableList<TextPosition?>(text.length) { null }
        var nextCharacter = 0
        text.forEachIndexed { index, character ->
            if (character.isWhitespace()) {
                val next = positionCharacters.getOrNull(nextCharacter)
                if (next?.first?.isWhitespace() == true) {
                    mapped[index] = next.second
                    nextCharacter++
                }
            } else {
                val match = (nextCharacter until positionCharacters.size).firstOrNull { candidate ->
                    positionCharacters[candidate].first.equals(character, ignoreCase = true)
                }
                if (match != null) {
                    mapped[index] = positionCharacters[match].second
                    nextCharacter = match + 1
                }
            }
        }
        return mapped
    }

    private fun normalizedBounds(position: TextPosition): NormalizedRect {
        val width = pageWidth.coerceAtLeast(1f)
        val height = pageHeight.coerceAtLeast(1f)
        val left = position.xDirAdj
        val top = position.yDirAdj - position.heightDir
        return NormalizedRect(
            left = (left / width).coerceIn(0f, 1f),
            top = (top / height).coerceIn(0f, 1f),
            right = ((left + position.widthDirAdj) / width).coerceIn(0f, 1f),
            bottom = ((top + position.heightDir) / height).coerceIn(0f, 1f)
        )
    }

    private fun mergeAdjacent(rectangles: List<NormalizedRect>): List<NormalizedRect> {
        if (rectangles.size < 2) return rectangles
        val sorted = rectangles.sortedWith(compareBy<NormalizedRect> { it.top }.thenBy { it.left })
        val merged = mutableListOf<NormalizedRect>()
        for (rectangle in sorted) {
            val previous = merged.lastOrNull()
            if (previous != null &&
                kotlin.math.abs(previous.top - rectangle.top) < 0.015f &&
                rectangle.left <= previous.right + 0.015f
            ) {
                merged[merged.lastIndex] = NormalizedRect(
                    left = minOf(previous.left, rectangle.left),
                    top = minOf(previous.top, rectangle.top),
                    right = maxOf(previous.right, rectangle.right),
                    bottom = maxOf(previous.bottom, rectangle.bottom)
                )
            } else {
                merged += rectangle
            }
        }
        return merged
    }
}
