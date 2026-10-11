package app.leaf.reader.core.format

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.NormalizedRect
import java.io.Closeable
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Formatting information retained by the simple DOCX text renderer. */
internal enum class BlockStyle { BODY, HEADING, TITLE, BULLET, TABLE_ROW, IMAGE }

internal data class TextBlock(
    val text: String,
    val style: BlockStyle = BlockStyle.BODY
)

internal data class GridPage(
    val sheetName: String,
    val rows: List<List<String>>,
    val firstRow: Int,
    val firstColumn: Int,
    val sheetPage: Int,
    val sheetPageCount: Int
)

internal data class SlidePage(
    val title: String,
    val bullets: List<String>,
    val slideNumber: Int,
    val slideCount: Int
)

/**
 * One native Office/text page. Both the image renderer and text index are built from the same
 * source layout, so find, selection, annotations and page sharing agree about page coordinates.
 */
internal sealed interface NativePage {
    val label: String?
    val metrics: PageMetrics
    val runs: List<TextRun>
    fun draw(canvas: Canvas, colors: NativeCanvasColors)
}

internal data class NativeCanvasColors(
    val background: Int = Color.WHITE,
    val surfaceAlt: Int = Color.rgb(247, 245, 239),
    val text: Int = Color.rgb(27, 36, 31),
    val outlineVariant: Int = Color.rgb(191, 201, 194)
)

internal data class DrawLine(
    val text: String,
    val x: Float,
    val baseline: Float,
    val size: Float,
    val color: Int,
    val bold: Boolean,
    val width: Float,
    val lineHeight: Float
) {
    fun draw(canvas: Canvas, textColor: Int) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = textColor
            typeface = Typeface.create(Typeface.SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        // StaticLayout supplies platform bidi shaping when a line contains RTL text.
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, max(1, width.toInt()))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setMaxLines(1)
            .build()
        canvas.save()
        canvas.translate(x, baseline - layout.getLineBaseline(0))
        layout.draw(canvas)
        canvas.restore()
    }
}

internal data class TextPage(
    val lines: List<DrawLine>,
    override val runs: List<TextRun>,
    override val label: String? = null,
    override val metrics: PageMetrics = PageMetrics(TEXT_PAGE_WIDTH, TEXT_PAGE_HEIGHT)
) : NativePage {
    override fun draw(canvas: Canvas, colors: NativeCanvasColors) {
        canvas.drawColor(colors.background)
        lines.forEach { it.draw(canvas, colors.text) }
    }
}

internal data class GridNativePage(
    val grid: GridPage,
    override val label: String,
    override val metrics: PageMetrics = PageMetrics(GRID_PAGE_WIDTH, GRID_PAGE_HEIGHT)
) : NativePage {
    override val runs: List<TextRun> by lazy { gridRuns(grid, metrics) }

    override fun draw(canvas: Canvas, colors: NativeCanvasColors) {
        canvas.drawColor(colors.background)
        val left = GRID_MARGIN
        val top = GRID_MARGIN
        val width = metrics.widthPoints - GRID_MARGIN * 2f
        val headerHeight = GRID_HEADER_HEIGHT
        val rowHeight = ((metrics.heightPoints - GRID_MARGIN * 2f - headerHeight) /
            grid.rows.size.coerceAtLeast(1)).coerceIn(GRID_ROW_MIN, GRID_ROW_MAX)
        val columnCount = grid.rows.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
        val columnWidth = width / columnCount
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.outlineVariant
            strokeWidth = GRID_LINE_WIDTH
            style = Paint.Style.STROKE
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.surfaceAlt
            style = Paint.Style.FILL
        }
        canvas.drawRect(left, top, left + width, top + headerHeight, headerPaint)
        for (rowIndex in grid.rows.indices) {
            val y = top + headerHeight + rowIndex * rowHeight
            for (columnIndex in 0 until columnCount) {
                val x = left + columnIndex * columnWidth
                canvas.drawRect(x, y, x + columnWidth, y + rowHeight, gridPaint)
                val value = grid.rows[rowIndex].getOrNull(columnIndex).orEmpty()
                if (value.isNotBlank()) {
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = colors.text
                        textSize = if (rowIndex == 0) GRID_TEXT_SIZE + 1 else GRID_TEXT_SIZE
                        typeface = Typeface.create(
                            Typeface.MONOSPACE,
                            if (rowIndex == 0) Typeface.BOLD else Typeface.NORMAL
                        )
                    }
                    val safeText = TextUtils.ellipsize(
                        value,
                        paint,
                        (columnWidth - GRID_CELL_PADDING * 2).coerceAtLeast(1f),
                        TextUtils.TruncateAt.END
                    ).toString()
                    val baseline = y + (rowHeight - paint.ascent() - paint.descent()) / 2f
                    canvas.drawText(safeText, x + GRID_CELL_PADDING, baseline, paint)
                }
            }
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.text
            textSize = GRID_TITLE_SIZE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(grid.sheetName, left, top - 16f, titlePaint)
    }
}

internal data class SlideNativePage(
    val slide: SlidePage,
    override val label: String,
    override val metrics: PageMetrics = PageMetrics(SLIDE_PAGE_WIDTH, SLIDE_PAGE_HEIGHT)
) : NativePage {
    override val runs: List<TextRun> by lazy { slideRuns(slide, metrics) }

    override fun draw(canvas: Canvas, colors: NativeCanvasColors) {
        canvas.drawColor(colors.background)
        val pageWidth = metrics.widthPoints.toFloat()
        val pageHeight = metrics.heightPoints.toFloat()
        val marginX = pageWidth * 0.075f
        val marginY = pageHeight * 0.11f
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.outlineVariant
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(
            marginX / 2f,
            marginY / 2f,
            pageWidth - marginX / 2f,
            pageHeight - marginY / 2f,
            18f,
            18f,
            border
        )
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.text
            textSize = SLIDE_TITLE_SIZE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        drawParagraph(canvas, slide.title, marginX, marginY, pageWidth - marginX * 2, titlePaint)
        var y = marginY + SLIDE_TITLE_GAP
        slide.bullets.forEach { bullet ->
            val bulletPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.text
                textSize = SLIDE_BODY_SIZE
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val bulletText = "•  $bullet"
            val layout = StaticLayout.Builder.obtain(
                bulletText,
                0,
                bulletText.length,
                bulletPaint,
                (pageWidth - marginX * 2).toInt()
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setLineSpacing(8f, 1f).build()
            canvas.save()
            canvas.translate(marginX, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + SLIDE_BULLET_GAP
        }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.text
            textSize = SLIDE_BADGE_SIZE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("${slide.slideNumber} / ${slide.slideCount}", pageWidth - marginX, pageHeight - marginY / 2f, badgePaint)
    }
}

/**
 * A measured, page-anchored text engine used for TXT, DOCX and EPUB indexing. TXT pagination is
 * computed from Android's measured StaticLayout line height rather than character-count guesses.
 */
internal class NativeDocumentEngine(
    private val type: DocType,
    private val pages: List<NativePage>,
    private val onClose: () -> Unit = {}
) : DocumentEngine {
    @Volatile private var canvasColors = NativeCanvasColors()

    override val pageCount: Int = pages.size

    override fun setReaderColors(background: Int, surfaceAlt: Int, text: Int, outlineVariant: Int) {
        canvasColors = NativeCanvasColors(background, surfaceAlt, text, outlineVariant)
    }

    override suspend fun pageMetrics(pageIndex: Int): PageMetrics = withContext(Dispatchers.Default) {
        pages.getOrNull(pageIndex)?.metrics ?: throw IndexOutOfBoundsException("Document page is outside the file")
    }

    override suspend fun pageLabel(pageIndex: Int): String? =
        pages.getOrNull(pageIndex)?.label

    override suspend fun renderPreview(pageIndex: Int, maxEdgePixels: Int): Bitmap = withContext(Dispatchers.Default) {
        require(maxEdgePixels > 0)
        val page = pages.getOrNull(pageIndex) ?: throw IndexOutOfBoundsException("Document page is outside the file")
        val scale = (maxEdgePixels.toFloat() / max(page.metrics.widthPoints, page.metrics.heightPoints)).coerceAtMost(1f)
        render(page, scale, RenderTile(0, 0, ceil(page.metrics.widthPoints * scale).toInt().coerceAtLeast(1), ceil(page.metrics.heightPoints * scale).toInt().coerceAtLeast(1)))
    }

    override suspend fun renderTile(pageIndex: Int, scale: Float, tile: RenderTile): Bitmap = withContext(Dispatchers.Default) {
        require(scale.isFinite() && scale > 0f)
        val page = pages.getOrNull(pageIndex) ?: throw IndexOutOfBoundsException("Document page is outside the file")
        render(page, scale, tile)
    }

    private fun render(page: NativePage, scale: Float, tile: RenderTile): Bitmap {
        val pageWidth = ceil(page.metrics.widthPoints * scale).toInt().coerceAtLeast(1)
        val pageHeight = ceil(page.metrics.heightPoints * scale).toInt().coerceAtLeast(1)
        val left = tile.left.coerceIn(0, pageWidth - 1)
        val top = tile.top.coerceIn(0, pageHeight - 1)
        val width = min(tile.width, pageWidth - left).coerceAtLeast(1)
        val height = min(tile.height, pageHeight - top).coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            canvas.translate(-left / scale, -top / scale)
            page.draw(canvas, canvasColors)
            return bitmap
        } catch (error: Throwable) {
            if (!bitmap.isRecycled) bitmap.recycle()
            throw error
        }
    }

    override suspend fun pageText(pageIndex: Int): List<TextRun> =
        pages.getOrNull(pageIndex)?.runs ?: throw IndexOutOfBoundsException("Document page is outside the file")

    override suspend fun findInPage(pageIndex: Int, query: String): List<MatchRect> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        return pageText(pageIndex).flatMap { run ->
            val result = mutableListOf<MatchRect>()
            var from = 0
            while (from <= run.text.length - needle.length) {
                val match = run.text.indexOf(needle, from, ignoreCase = true)
                if (match < 0) break
                val end = match + needle.length
                val glyphs = run.characterBounds
                    .takeIf { it.size >= end }
                    ?.subList(match, end)
                    ?.filter { it.right > it.left && it.bottom > it.top }
                    .orEmpty()
                result += MatchRect(needle, glyphs.ifEmpty { run.bounds })
                from = end.coerceAtLeast(match + 1)
            }
            result
        }
    }

    override suspend fun extractAllText(onProgress: suspend (completed: Int, total: Int) -> Unit): List<List<TextRun>> =
        buildList {
            for (page in pages.indices) {
                add(pageText(page))
                onProgress(page + 1, pages.size)
            }
        }

    override fun close() = onClose()

    companion object {
        fun text(
            type: DocType,
            source: String,
            labelPrefix: String? = null,
            onClose: () -> Unit = {}
        ): NativeDocumentEngine = textPages(type, listOf(source.split('\n').map { TextBlock(it) }), labelPrefix, onClose)

        fun textPages(
            type: DocType,
            blocksBySourcePage: List<List<TextBlock>>,
            labelPrefix: String? = null,
            onClose: () -> Unit = {}
        ): NativeDocumentEngine {
            val pages = if (blocksBySourcePage.isEmpty()) {
                measuredTextPages(emptyList(), labelPrefix)
            } else {
                blocksBySourcePage.flatMapIndexed { sourcePage, blocks ->
                    measuredTextPages(
                        blocks,
                        labelPrefix,
                        sourcePageIndex = sourcePage.takeIf { blocksBySourcePage.size > 1 }
                    )
                }
            }
            return NativeDocumentEngine(type, pages.ifEmpty { measuredTextPages(listOf(TextBlock(" ")), null) }, onClose)
        }

        fun docx(blocks: List<TextBlock>, onClose: () -> Unit = {}): NativeDocumentEngine =
            NativeDocumentEngine(DocType.DOCX, measuredTextPages(blocks, "Page"), onClose)

        fun xlsx(
            sheets: List<Pair<String, List<List<String>>>>,
            onClose: () -> Unit = {}
        ): NativeDocumentEngine {
            val pages = buildList {
                sheets.forEach { (sheetName, rows) ->
                    val safeRows = rows.ifEmpty { listOf(listOf("")) }
                    val maximumColumns = safeRows.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
                    val rowWindows = safeRows.chunked(XLSX_ROWS_PER_PAGE)
                    val columnStarts = (0 until maximumColumns step XLSX_COLUMNS_PER_PAGE).toList().ifEmpty { listOf(0) }
                    columnStarts.forEachIndexed { columnPage, firstColumn ->
                        rowWindows.forEachIndexed { rowPage, rowChunk ->
                            val clippedRows = rowChunk.map { row ->
                                row.drop(firstColumn).take(XLSX_COLUMNS_PER_PAGE)
                            }
                            val page = GridPage(
                                sheetName = sheetName,
                                rows = clippedRows,
                                firstRow = rowPage * XLSX_ROWS_PER_PAGE,
                                firstColumn = firstColumn,
                                sheetPage = columnPage * rowWindows.size + rowPage + 1,
                                sheetPageCount = columnStarts.size * rowWindows.size
                            )
                            add(GridNativePage(page, "$sheetName · ${page.sheetPage}/${page.sheetPageCount}"))
                        }
                    }
                }
            }
            return NativeDocumentEngine(DocType.XLSX, pages.ifEmpty {
                listOf(GridNativePage(GridPage("Sheet 1", listOf(listOf("")), 0, 0, 1, 1), "Sheet 1 · 1/1"))
            }, onClose)
        }

        fun pptx(slides: List<SlidePage>, onClose: () -> Unit = {}): NativeDocumentEngine = NativeDocumentEngine(
            DocType.PPTX,
            slides.map { slide ->
                SlideNativePage(slide, "Slide ${slide.slideNumber} / ${slide.slideCount}")
            }.ifEmpty {
                listOf(SlideNativePage(SlidePage("", emptyList(), 1, 1), "Slide 1 / 1"))
            },
            onClose
        )

        fun epubPageCounts(pages: List<List<TextBlock>>): List<Int> = pages.map { blocks ->
            measuredTextPages(blocks, null).size.coerceAtLeast(1)
        }

        fun epub(
            pages: List<List<TextBlock>>,
            labels: List<String?>,
            onClose: () -> Unit
        ): NativeDocumentEngine {
            val nativePages = pages.flatMapIndexed { index, blocks ->
                val measured = measuredTextPages(blocks, null).ifEmpty {
                    measuredTextPages(listOf(TextBlock(" ")), null)
                }
                measured.mapIndexed { subPage, page ->
                    val sourceLabel = labels.getOrNull(index)
                    val label = if (measured.size > 1) {
                        sourceLabel?.let { "$it · ${subPage + 1}/${measured.size}" }
                    } else sourceLabel
                    page.copy(label = label)
                }
            }
            return NativeDocumentEngine(DocType.EPUB, nativePages.ifEmpty {
                measuredTextPages(listOf(TextBlock(" ")), null)
            }, onClose)
        }
    }
}

private fun measuredTextPages(
    blocks: List<TextBlock>,
    labelPrefix: String?,
    sourcePageIndex: Int? = null
): List<TextPage> {
    val pageWidth = TEXT_PAGE_WIDTH
    val pageHeight = TEXT_PAGE_HEIGHT
    val marginX = TEXT_MARGIN_X
    val marginY = TEXT_MARGIN_Y
    val textWidth = pageWidth - marginX * 2
    val pageLineLists = mutableListOf<MutableList<DrawLine>>()
    val pageRuns = mutableListOf<MutableList<TextRun>>()
    var pageIndex = 0
    var y = marginY.toFloat()

    fun startPage() {
        pageLineLists.add(mutableListOf())
        pageRuns.add(mutableListOf())
        pageIndex += 1
        y = marginY.toFloat()
    }
    startPage()

    blocks.forEach { block ->
        if (block.text.isBlank()) {
            y += TEXT_PARAGRAPH_GAP
            if (y >= pageHeight - marginY) startPage()
            return@forEach
        }
        val style = block.style
        val size = when (style) {
            BlockStyle.TITLE -> TEXT_TITLE_SIZE
            BlockStyle.HEADING -> TEXT_HEADING_SIZE
            BlockStyle.BULLET -> TEXT_BODY_SIZE
            BlockStyle.TABLE_ROW -> TEXT_TABLE_SIZE
            BlockStyle.IMAGE -> TEXT_BODY_SIZE
            BlockStyle.BODY -> TEXT_BODY_SIZE
        }
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            color = TEXT_COLOR
            typeface = Typeface.create(
                if (style == BlockStyle.TABLE_ROW) Typeface.MONOSPACE else Typeface.SERIF,
                if (style == BlockStyle.TITLE || style == BlockStyle.HEADING) Typeface.BOLD else Typeface.NORMAL
            )
        }
        val paragraph = if (style == BlockStyle.BULLET && !block.text.startsWith("•")) "•  ${block.text}" else block.text
        val layout = StaticLayout.Builder.obtain(paragraph, 0, paragraph.length, paint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(0f, TEXT_LINE_SPACING)
            .setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE)
            .build()
        for (lineIndex in 0 until layout.lineCount) {
            val rawStart = layout.getLineStart(lineIndex)
            val rawEnd = layout.getLineEnd(lineIndex).coerceAtMost(paragraph.length)
            val end = (rawStart until rawEnd).lastOrNull { paragraph[it] != '\n' }?.plus(1) ?: rawStart
            val lineText = paragraph.substring(rawStart, end)
            val lineTopInLayout = layout.getLineTop(lineIndex).toFloat()
            val lineHeight = (layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)).toFloat().coerceAtLeast(size)
            val needed = lineHeight + if (lineIndex == 0) TEXT_PARAGRAPH_GAP else 0f
            if (y + needed + TEXT_MARGIN_Y > pageHeight && pageLineLists.last().isNotEmpty()) startPage()
            if (lineIndex == 0) y += TEXT_PARAGRAPH_GAP
            val top = y
            val baseline = y + (layout.getLineBaseline(lineIndex) - layout.getLineTop(lineIndex))
            val lineLeft = marginX + layout.getLineLeft(lineIndex)
            val lineRight = (marginX + layout.getLineRight(lineIndex)).coerceAtLeast(lineLeft + 1f)
            val bounds = NormalizedRect(
                left = (lineLeft / pageWidth).coerceIn(0f, 1f),
                top = (top / pageHeight).coerceIn(0f, 1f),
                right = (lineRight / pageWidth).coerceIn(0f, 1f),
                bottom = ((top + lineHeight) / pageHeight).coerceIn(0f, 1f)
            )
            val charBounds = lineText.indices.map { charIndex ->
                val sourceOffset = rawStart + charIndex
                val firstX = marginX + layout.getPrimaryHorizontal(sourceOffset)
                val secondX = marginX + layout.getPrimaryHorizontal(sourceOffset + 1)
                val left = min(firstX, secondX)
                val right = max(firstX, secondX)
                if (right - left < 0.25f) NormalizedRect(0f, 0f, 0f, 0f)
                else NormalizedRect(
                    left = (left / pageWidth).coerceIn(0f, 1f),
                    top = (top / pageHeight).coerceIn(0f, 1f),
                    right = (right / pageWidth).coerceIn(0f, 1f),
                    bottom = ((top + lineHeight) / pageHeight).coerceIn(0f, 1f)
                )
            }
            if (lineText.isNotEmpty()) {
                pageLineLists.last() += DrawLine(
                    text = lineText,
                    x = marginX.toFloat(),
                    baseline = baseline,
                    size = size,
                    color = TEXT_COLOR,
                    bold = style == BlockStyle.TITLE || style == BlockStyle.HEADING,
                    width = textWidth.toFloat(),
                    lineHeight = lineHeight
                )
                pageRuns.last() += TextRun(lineText, listOf(bounds), charBounds)
            }
            y += lineHeight
        }
        y += when (style) {
            BlockStyle.TITLE -> TEXT_TITLE_GAP
            BlockStyle.HEADING -> TEXT_HEADING_GAP
            else -> TEXT_PARAGRAPH_GAP
        }
        if (y > pageHeight - marginY && pageLineLists.last().isNotEmpty()) startPage()
    }
    val pages = pageLineLists.indices.map { index ->
        val label = when {
            sourcePageIndex != null -> "$labelPrefix ${sourcePageIndex + 1}"
            labelPrefix != null -> "$labelPrefix ${index + 1}"
            else -> null
        }
        TextPage(pageLineLists[index].toList(), pageRuns[index].toList(), label)
    }
    return pages.ifEmpty { listOf(TextPage(emptyList(), emptyList(), labelPrefix?.let { "$it 1" })) }
}

private fun gridRuns(grid: GridPage, metrics: PageMetrics): List<TextRun> {
    val left = GRID_MARGIN
    val top = GRID_MARGIN
    val width = metrics.widthPoints - GRID_MARGIN * 2f
    val headerHeight = GRID_HEADER_HEIGHT
    val rowHeight = ((metrics.heightPoints - GRID_MARGIN * 2f - headerHeight) /
        grid.rows.size.coerceAtLeast(1)).coerceIn(GRID_ROW_MIN, GRID_ROW_MAX)
    val columns = grid.rows.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
    val columnWidth = width / columns
    return buildList {
        grid.rows.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, value ->
                if (value.isBlank()) return@forEachIndexed
                val x = left + columnIndex * columnWidth + GRID_CELL_PADDING
                val y = top + headerHeight + rowIndex * rowHeight
                val visibleWidth = (columnWidth - GRID_CELL_PADDING * 2).coerceAtLeast(1f)
                val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = GRID_TEXT_SIZE }
                val visibleLength = takeWhileWithWidth(value, textPaint, visibleWidth)
                val glyphWidth = visibleWidth / visibleLength.coerceAtLeast(1)
                val chars = value.indices.map { charIndex ->
                    if (charIndex >= visibleLength) NormalizedRect(0f, 0f, 0f, 0f)
                    else NormalizedRect(
                        left = ((x + charIndex * glyphWidth) / metrics.widthPoints).coerceIn(0f, 1f),
                        top = ((y + 3f) / metrics.heightPoints).coerceIn(0f, 1f),
                        right = ((x + (charIndex + 1) * glyphWidth) / metrics.widthPoints).coerceIn(0f, 1f),
                        bottom = ((y + rowHeight - 3f) / metrics.heightPoints).coerceIn(0f, 1f)
                    )
                }
                val bounds = NormalizedRect(
                    (x / metrics.widthPoints).coerceIn(0f, 1f),
                    ((y + 3f) / metrics.heightPoints).coerceIn(0f, 1f),
                    ((x + visibleWidth) / metrics.widthPoints).coerceIn(0f, 1f),
                    ((y + rowHeight - 3f) / metrics.heightPoints).coerceIn(0f, 1f)
                )
                add(TextRun(value, listOf(bounds), chars))
            }
        }
    }
}

private fun slideRuns(slide: SlidePage, metrics: PageMetrics): List<TextRun> {
    val width = metrics.widthPoints.toFloat()
    val height = metrics.heightPoints.toFloat()
    val marginX = width * 0.075f
    val marginY = height * 0.11f
    val result = mutableListOf<TextRun>()
    fun addRun(text: String, top: Float, heightPx: Float) {
        if (text.isBlank()) return
        val bounds = NormalizedRect(
            (marginX / width).coerceIn(0f, 1f),
            (top / height).coerceIn(0f, 1f),
            ((width - marginX) / width).coerceIn(0f, 1f),
            ((top + heightPx) / height).coerceIn(0f, 1f)
        )
        val chars = text.indices.map { index ->
            val fraction = index.toFloat() / text.length.coerceAtLeast(1)
            val next = (index + 1f) / text.length.coerceAtLeast(1)
            NormalizedRect(
                bounds.left + bounds.right.minus(bounds.left) * fraction,
                bounds.top,
                bounds.left + bounds.right.minus(bounds.left) * next,
                bounds.bottom
            )
        }
        result += TextRun(text, listOf(bounds), chars)
    }
    addRun(slide.title, marginY, height * 0.20f)
    val availableBody = (height - marginY * 2 - height * 0.25f).coerceAtLeast(1f)
    val lineHeight = availableBody / slide.bullets.size.coerceAtLeast(1)
    slide.bullets.forEachIndexed { index, text ->
        addRun(text, marginY + height * 0.25f + index * lineHeight, lineHeight * 0.8f)
    }
    return result
}

private fun drawParagraph(canvas: Canvas, text: String, x: Float, y: Float, width: Float, paint: TextPaint) {
    if (text.isBlank()) return
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .setLineSpacing(4f, 1f)
        .build()
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
}

private fun takeWhileWithWidth(value: String, paint: TextPaint, width: Float): Int {
    var end = value.length
    while (end > 1 && paint.measureText(value, 0, end) > width) end--
    return end.coerceAtLeast(1)
}

private const val TEXT_PAGE_WIDTH = 900
private const val TEXT_PAGE_HEIGHT = 1200
private const val TEXT_MARGIN_X = 68
private const val TEXT_MARGIN_Y = 70
private const val TEXT_BODY_SIZE = 28f
private const val TEXT_HEADING_SIZE = 38f
private const val TEXT_TITLE_SIZE = 48f
private const val TEXT_TABLE_SIZE = 22f
private const val TEXT_LINE_SPACING = 1.14f
private const val TEXT_PARAGRAPH_GAP = 15f
private const val TEXT_HEADING_GAP = 18f
private const val TEXT_TITLE_GAP = 22f
private val TEXT_COLOR = Color.rgb(27, 36, 31)
private const val GRID_PAGE_WIDTH = 1000
private const val GRID_PAGE_HEIGHT = 1320
private const val GRID_MARGIN = 52f
private const val GRID_HEADER_HEIGHT = 48f
private const val GRID_ROW_MIN = 28f
private const val GRID_ROW_MAX = 42f
private const val GRID_CELL_PADDING = 8f
private const val GRID_LINE_WIDTH = 2f
private const val GRID_TEXT_SIZE = 24f
private const val GRID_TITLE_SIZE = 34f
private const val XLSX_ROWS_PER_PAGE = 30
private const val XLSX_COLUMNS_PER_PAGE = 8
private const val SLIDE_PAGE_WIDTH = 1600
private const val SLIDE_PAGE_HEIGHT = 900
private const val SLIDE_TITLE_SIZE = 64f
private const val SLIDE_BODY_SIZE = 36f
private const val SLIDE_BADGE_SIZE = 24f
private const val SLIDE_TITLE_GAP = 160f
private const val SLIDE_BULLET_GAP = 28f
