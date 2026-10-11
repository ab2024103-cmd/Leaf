package app.leaf.reader.core.format

import android.content.Context
import android.net.Uri
import android.os.Build
import android.text.Html
import android.view.ActionMode
import androidx.fragment.app.FragmentFactory
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.model.Typeface as ReadingTypeface
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/** Opens EPUB packages through Readium and retains Readium's publication locators as Leaf anchors. */
class EpubDocumentEngineFactory(context: Context) : DocumentEngineFactory {
    private val appContext = context.applicationContext

    override suspend fun open(uri: String): DocumentEngine = withContext(Dispatchers.IO) {
        val prepared = materializeEpub(Uri.parse(uri))
        val file = prepared.file
        try {
            val httpClient = DefaultHttpClient()
            val assetRetriever = AssetRetriever(appContext.contentResolver, httpClient)
            val asset = assetRetriever.retrieve(file.toUrl()).getOrElse { error ->
                throw IOException(error.message)
            }
            val opener = PublicationOpener(
                DefaultPublicationParser(
                    context = appContext,
                    httpClient = httpClient,
                    assetRetriever = assetRetriever,
                    pdfFactory = null
                )
            )
            val publication = opener.open(asset, allowUserInteraction = false).getOrElse { error ->
                throw IOException(error.message)
            }
            try {
                val document = publicationTextPages(publication, publication.readingOrderAnchors())
                val pageBlocks = document.pages.map { it.blocks }
                val pageCounts = NativeDocumentEngine.epubPageCounts(pageBlocks)
                val expandedAnchors = document.anchors.flatMapIndexed { index, anchor ->
                    val count = pageCounts.getOrNull(index) ?: 1
                    List(count) { subPage ->
                        if (count <= 1) anchor else {
                            val baseProgression = anchor.locator.locations.progression ?: 0.0
                            anchor.copy(
                                locator = anchor.locator.copy(
                                    locations = anchor.locator.locations.copy(
                                        position = null,
                                        progression = (baseProgression + subPage * 0.0001).coerceAtMost(1.0)
                                    )
                                )
                            )
                        }
                    }
                }
                EpubDocumentEngine(
                    publication = publication,
                    anchors = expandedAnchors,
                    native = NativeDocumentEngine.epub(
                        pages = pageBlocks,
                        labels = document.pages.map { it.label },
                        onClose = { if (prepared.deleteOnClose) file.delete() }
                    )
                )
            } catch (error: Throwable) {
                publication.close()
                throw error
            }
        } catch (error: Throwable) {
            if (prepared.deleteOnClose) file.delete()
            throw error
        }
    }

    private data class PreparedEpub(val file: File, val deleteOnClose: Boolean)

    private fun materializeEpub(uri: Uri): PreparedEpub {
        if (uri.scheme == "file") {
            return PreparedEpub(uri.path?.let(::File) ?: throw IOException("The EPUB path is missing"), false)
        }
        if (uri.scheme == "content") {
            val cache = File(appContext.cacheDir, "epub-open").apply { mkdirs() }
            val destination = File(cache, "${UUID.randomUUID()}.epub")
            val temporary = File(cache, "${destination.name}.tmp")
            val input = appContext.contentResolver.openInputStream(uri)
                ?: throw IOException("The EPUB document could not be opened")
            try {
                input.use { source -> temporary.outputStream().use(source::copyTo) }
            } catch (error: Throwable) {
                temporary.delete()
                throw error
            }
            if (!temporary.renameTo(destination)) {
                temporary.delete()
                throw IOException("The EPUB could not be prepared for reading")
            }
            return PreparedEpub(destination, true)
        }
        throw IOException("Unsupported EPUB URI")
    }
}

/**
 * The common ReaderScreen hosts [EpubNavigatorFragment] for actual EPUB rendering. This text index
 * is a parallel, stable chapter/position map used by Leaf Find, annotations, clipboard and Page Stay.
 */
class EpubDocumentEngine internal constructor(
    val publication: Publication,
    private val anchors: List<EpubAnchor>,
    private val native: NativeDocumentEngine
) : DocumentEngine by native {

    override val pageCount: Int get() = anchors.size

    override suspend fun pageLabel(pageIndex: Int): String? = anchors.getOrNull(pageIndex)?.label

    fun locatorAt(pageIndex: Int): Locator = anchors[
        pageIndex.coerceIn(0, (anchors.size - 1).coerceAtLeast(0))
    ].locator

    fun pageIndexFor(locator: Locator): Int {
        val related = anchors.withIndex().filter { it.value.locator.href.toString() == locator.href.toString() }
        val progression = locator.locations.progression
        if (related.isNotEmpty()) {
            if (progression != null) {
                return related.minByOrNull { indexed ->
                    kotlin.math.abs((indexed.value.locator.locations.progression ?: 0.0) - progression)
                }?.index ?: related.first().index
            }
            return related.first().index
        }
        locator.locations.position?.let { position ->
            if (position in 1..anchors.size) return position - 1
        }
        return related.firstOrNull()?.index ?: 0
    }

    @OptIn(ExperimentalReadiumApi::class)
    fun createFragmentFactory(
        pageIndex: Int,
        readingTheme: ReadingTheme,
        typeface: ReadingTypeface,
        zoom: Float,
        reflow: Boolean,
        scrollDir: ScrollDir,
        systemDark: Boolean,
        onPageChanged: (Locator) -> Unit = {},
        selectionActionModeCallback: ActionMode.Callback? = null
    ): FragmentFactory {
        val factory = EpubNavigatorFactory(publication)
        return factory.createFragmentFactory(
            initialLocator = locatorAt(pageIndex),
            initialPreferences = preferences(readingTheme, typeface, zoom, reflow, scrollDir, systemDark),
            paginationListener = object : EpubNavigatorFragment.PaginationListener {
                override fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) {
                    onPageChanged(locator)
                }
            },
            configuration = EpubNavigatorFragment.Configuration(
                selectionActionModeCallback = selectionActionModeCallback
            )
        )
    }

    @OptIn(ExperimentalReadiumApi::class)
    fun submitPreferences(
        navigator: EpubNavigatorFragment,
        readingTheme: ReadingTheme,
        typeface: ReadingTypeface,
        zoom: Float,
        reflow: Boolean,
        scrollDir: ScrollDir,
        systemDark: Boolean
    ) {
        navigator.submitPreferences(preferences(readingTheme, typeface, zoom, reflow, scrollDir, systemDark))
    }

    @OptIn(ExperimentalReadiumApi::class)
    private fun preferences(
        readingTheme: ReadingTheme,
        typeface: ReadingTypeface,
        zoom: Float,
        reflow: Boolean,
        scrollDir: ScrollDir,
        systemDark: Boolean
    ) = EpubPreferences(
        theme = when (readingTheme) {
            ReadingTheme.PAPER -> Theme.LIGHT
            ReadingTheme.SEPIA -> Theme.SEPIA
            ReadingTheme.NIGHT, ReadingTheme.OLED -> Theme.DARK
            ReadingTheme.AUTO -> if (systemDark) Theme.DARK else Theme.LIGHT
        },
        fontFamily = if (typeface == ReadingTypeface.SERIF) FontFamily.SERIF else FontFamily.SANS_SERIF,
        fontSize = zoom.toDouble(),
        publisherStyles = false,
        scroll = reflow || scrollDir == ScrollDir.VERTICAL
    )

    override fun close() {
        try {
            publication.close()
        } finally {
            native.close()
        }
    }
}

internal data class EpubAnchor(val locator: Locator, val label: String)
private data class EpubPageText(val blocks: List<TextBlock>, val label: String)
private data class EpubIndexedText(val anchors: List<EpubAnchor>, val pages: List<EpubPageText>)

private suspend fun Publication.readingOrderAnchors(): List<EpubAnchor> {
    val grouped = positionsByReadingOrder()
    val anchors = buildList {
        readingOrder.forEachIndexed { index, link ->
            val positions = grouped.getOrNull(index).orEmpty()
            if (positions.isNotEmpty()) {
                positions.forEach { locator ->
                    val title = locator.title?.takeIf(String::isNotBlank)
                        ?: link.title?.takeIf(String::isNotBlank)
                        ?: "Chapter ${size + 1}"
                    add(EpubAnchor(locator, title))
                }
            } else {
                locatorFromLink(link)?.let { locator ->
                    add(EpubAnchor(locator, link.title?.takeIf(String::isNotBlank) ?: "Chapter ${size + 1}"))
                }
            }
        }
    }
    if (anchors.isNotEmpty()) return anchors
    val first = readingOrder.firstOrNull()?.let(::locatorFromLink)
        ?: throw IOException("The EPUB has no readable reading order")
    return listOf(EpubAnchor(first, first.title ?: "Chapter 1"))
}

private suspend fun publicationTextPages(
    publication: Publication,
    sourceAnchors: List<EpubAnchor>
): EpubIndexedText {
    val linkIndexByHref = publication.readingOrder.withIndex().associate { it.value.url().toString() to it.index }
    val textByHref = mutableMapOf<String, String>()
    sourceAnchors.forEach { anchor ->
        val href = anchor.locator.href.toString()
        if (href in textByHref) return@forEach
        val link = publication.readingOrder.getOrNull(linkIndexByHref[href] ?: -1)
            ?: publication.readingOrder.firstOrNull { it.url().toString() == href }
            ?: return@forEach
        val resource = publication.get(link) ?: return@forEach
        val bytes = resource.read().getOrElse { error -> throw IOException(error.message) }
        val html = bytes.toString(Charsets.UTF_8)
        val plain = if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html).toString()
        }
        textByHref[href] = plain
            .replace('\u00a0', ' ')
            .replace(Regex("[\\t\\r ]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    val anchors = mutableListOf<EpubAnchor>()
    val pages = mutableListOf<EpubPageText>()
    sourceAnchors.groupBy { it.locator.href.toString() }.forEach { (href, group) ->
        val source = textByHref[href].orEmpty()
        val minimumSegments = ceil(source.length / EPUB_SEGMENT_CHARACTERS.toDouble()).toInt().coerceAtLeast(1)
        val segmentCount = maxOf(group.size, minimumSegments)
        val segments = splitAtWordBoundaries(source, segmentCount)
        segments.forEachIndexed { index, text ->
            val base = group.getOrNull(index) ?: group.last()
            val anchor = if (index < group.size) base else {
                val progression = index.toDouble() / segmentCount.toDouble()
                base.copy(locator = base.locator.copy(
                    locations = base.locator.locations.copy(position = null, progression = progression)
                ))
            }
            anchors += anchor
            val displayLabel = if (segmentCount == 1) base.label else "${base.label} · ${index + 1}/$segmentCount"
            pages += EpubPageText(listOf(TextBlock(text.ifBlank { " " })), displayLabel)
        }
    }
    if (pages.isEmpty()) {
        val label = sourceAnchors.firstOrNull()?.label ?: "Chapter 1"
        return EpubIndexedText(
            anchors = listOf(sourceAnchors.first()),
            pages = listOf(EpubPageText(listOf(TextBlock(" ")), label))
        )
    }
    return EpubIndexedText(anchors, pages)
}

private fun splitAtWordBoundaries(text: String, requestedSegments: Int): List<String> {
    if (requestedSegments <= 1 || text.isEmpty()) return listOf(text)
    val segments = mutableListOf<String>()
    var start = 0
    repeat(requestedSegments - 1) { index ->
        if (start >= text.length) return@repeat
        val remainingSegments = requestedSegments - index
        val desired = (text.length - start) / remainingSegments
        var end = (start + desired).coerceAtMost(text.length)
        if (end < text.length) {
            val boundary = text.indexOf(' ', end).takeIf { it in (start + 1)..(end + EPUB_WORD_LOOKAHEAD) }
                ?: text.lastIndexOf(' ', end).takeIf { it > start }
            if (boundary != null) end = boundary
        }
        if (end <= start) end = minOf(text.length, start + desired.coerceAtLeast(1))
        segments += text.substring(start, end).trim()
        start = end
        while (start < text.length && text[start].isWhitespace()) start++
    }
    if (start < text.length) segments += text.substring(start).trim()
    while (segments.size < requestedSegments) segments += ""
    return segments.take(requestedSegments)
}

private const val EPUB_SEGMENT_CHARACTERS = 600
private const val EPUB_WORD_LOOKAHEAD = 240
