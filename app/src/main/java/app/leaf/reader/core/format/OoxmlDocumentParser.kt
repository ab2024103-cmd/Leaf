package app.leaf.reader.core.format

import android.util.Xml
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile
import org.xmlpull.v1.XmlPullParser

/** Streaming readers for the OOXML subset promised by §7.3. No complete XML document is retained. */
internal object OoxmlDocumentParser {

    fun parseDocx(file: File): List<TextBlock> = ZipFile(file).use { zip ->
        val entry = zip.getEntry("word/document.xml")
            ?: throw IOException("The DOCX package has no word/document.xml")
        zip.getInputStream(entry).use(::parseDocxXml)
    }

    fun parseXlsx(file: File): List<Pair<String, List<List<String>>>> = ZipFile(file).use { zip ->
        val sharedStrings = zip.getEntry("xl/sharedStrings.xml")?.let { entry ->
            zip.getInputStream(entry).use(::parseSharedStrings)
        }.orEmpty()
        val relationships = zip.getEntry("xl/_rels/workbook.xml.rels")?.let { entry ->
            zip.getInputStream(entry).use(::parseRelationships)
        }.orEmpty()
        val workbookEntry = zip.getEntry("xl/workbook.xml")
            ?: throw IOException("The XLSX package has no xl/workbook.xml")
        val sheets = zip.getInputStream(workbookEntry).use(::parseWorkbookSheets)
        sheets.mapIndexedNotNull { index, sheet ->
            val target = relationships[sheet.relationshipId] ?: return@mapIndexedNotNull null
            val path = resolvePartPath("xl/workbook.xml", target)
            val entry = zip.getEntry(path) ?: return@mapIndexedNotNull null
            val rows = zip.getInputStream(entry).use { parseWorksheet(it, sharedStrings) }
            (sheet.name.ifBlank { "Sheet ${index + 1}" }) to rows
        }.ifEmpty { throw IOException("The XLSX workbook has no readable worksheets") }
    }

    fun parsePptx(file: File): List<SlidePage> = ZipFile(file).use { zip ->
        val presentation = zip.getEntry("ppt/presentation.xml")
        val relationships = zip.getEntry("ppt/_rels/presentation.xml.rels")?.let { entry ->
            zip.getInputStream(entry).use(::parseRelationships)
        }.orEmpty()
        val orderedIds = presentation?.let { entry ->
            zip.getInputStream(entry).use(::parsePresentationSlideIds)
        }.orEmpty()
        val slidePaths = if (orderedIds.isNotEmpty()) {
            orderedIds.mapNotNull { id -> relationships[id]?.let { resolvePartPath("ppt/presentation.xml", it) } }
        } else {
            zip.entries().asSequence()
                .map { it.name }
                .filter { it.matches(Regex("ppt/slides/slide[0-9]+\\.xml")) }
                .sortedBy { it.substringAfter("slide").substringBefore('.').toIntOrNull() ?: Int.MAX_VALUE }
                .toList()
        }
        if (slidePaths.isEmpty()) throw IOException("The PPTX presentation has no readable slides")
        val slides = slidePaths.map { path ->
            val entry = zip.getEntry(path) ?: throw IOException("The PPTX package is missing $path")
            val slide = zip.getInputStream(entry).use(::parseSlideXml)
            val relationshipsPath = "${path.substringBeforeLast('/')}/_rels/${path.substringAfterLast('/')}.rels"
            val slideRelationships = zip.getEntry(relationshipsPath)?.let { rels ->
                zip.getInputStream(rels).use(::parseRelationships)
            }.orEmpty()
            val layoutPath = slideRelationships.values.firstOrNull { it.contains("slideLayout") }
                ?.let { resolvePartPath(path, it) }
            val layout = layoutPath?.let(zip::getEntry)?.let { layoutEntry ->
                zip.getInputStream(layoutEntry).use(::parseSlideXml)
            }
            when {
                slide == null && layout == null -> SlidePage("", emptyList(), 1, 1)
                slide == null -> layout!!
                layout == null -> slide
                else -> slide.copy(
                    title = slide.title.ifBlank { layout.title },
                    bullets = slide.bullets.ifEmpty { layout.bullets }
                )
            }
        }
        if (slides.isEmpty()) throw IOException("The PPTX presentation has no readable slides")
        slides.mapIndexed { index, slide ->
            slide.copy(slideNumber = index + 1, slideCount = slides.size)
        }
    }

    internal fun parseDocxXml(input: InputStream): List<TextBlock> {
        val parser = newParser(input)
        val blocks = mutableListOf<TextBlock>()
        var inTable = false
        var row: MutableList<String>? = null
        var cell: StringBuilder? = null
        var inParagraph = false
        var paragraph = StringBuilder()
        var paragraphStyle: BlockStyle = BlockStyle.BODY
        var listParagraph = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name.localTag()) {
                    "tbl" -> inTable = true
                    "tr" -> row = mutableListOf()
                    "tc" -> cell = StringBuilder()
                    "p" -> {
                        inParagraph = true
                        paragraph = StringBuilder()
                        paragraphStyle = BlockStyle.BODY
                        listParagraph = false
                    }
                    "pStyle" -> if (inParagraph) {
                        paragraphStyle = docxParagraphStyle(attribute(parser, "val"))
                    }
                    "numPr" -> if (inParagraph) listParagraph = true
                    "t" -> if (inParagraph) paragraph.append(parser.nextText())
                    "tab" -> if (inParagraph) paragraph.append('\t')
                    "br" -> if (inParagraph) paragraph.append('\n')
                    "docPr" -> if (inParagraph) {
                        val alt = attribute(parser, "descr")?.takeIf(String::isNotBlank)
                            ?: attribute(parser, "title")?.takeIf(String::isNotBlank)
                        paragraph.append(if (alt == null) "[Image]" else "[Image: $alt]")
                        if (paragraphStyle == BlockStyle.BODY) paragraphStyle = BlockStyle.IMAGE
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name.localTag()) {
                    "p" -> if (inParagraph) {
                        val text = paragraph.toString().trim()
                        if (text.isNotEmpty()) {
                            val style = if (listParagraph && paragraphStyle == BlockStyle.BODY) BlockStyle.BULLET else paragraphStyle
                            if (cell != null) {
                                if (cell!!.isNotEmpty()) cell!!.append(' ')
                                cell!!.append(text)
                            } else {
                                blocks += TextBlock(text, style)
                            }
                        }
                        inParagraph = false
                    }
                    "tc" -> {
                        row?.add(cell?.toString().orEmpty().trim())
                        cell = null
                    }
                    "tr" -> {
                        val values = row.orEmpty()
                        if (values.any(String::isNotBlank)) {
                            blocks += TextBlock(values.joinToString("  |  "), BlockStyle.TABLE_ROW)
                        }
                        row = null
                    }
                    "tbl" -> inTable = false
                }
            }
            parser.next()
        }
        // Keep the table state explicit: paragraphs in a cell are consumed into the row instead of
        // being emitted a second time as body text.
        if (inTable && row?.isNotEmpty() == true) {
            blocks += TextBlock(row!!.joinToString("  |  "), BlockStyle.TABLE_ROW)
        }
        return blocks
    }

    private data class SheetRef(val name: String, val relationshipId: String)
    private data class SlideShape(val placeholder: String?, val paragraphs: List<String>)

    private fun parseSharedStrings(input: InputStream): List<String> {
        val parser = newParser(input)
        val values = mutableListOf<String>()
        var inString = false
        var value = StringBuilder()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name.localTag()) {
                    "si" -> { inString = true; value = StringBuilder() }
                    "t" -> if (inString) value.append(parser.nextText())
                }
                XmlPullParser.END_TAG -> if (parser.name.localTag() == "si" && inString) {
                    values += value.toString()
                    inString = false
                }
            }
            parser.next()
        }
        return values
    }

    private fun parseRelationships(input: InputStream): Map<String, String> {
        val parser = newParser(input)
        val relationships = linkedMapOf<String, String>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name.localTag() == "Relationship") {
                val id = attribute(parser, "Id")
                val target = attribute(parser, "Target")
                if (!id.isNullOrBlank() && !target.isNullOrBlank()) relationships[id] = target
            }
            parser.next()
        }
        return relationships
    }

    private fun parseWorkbookSheets(input: InputStream): List<SheetRef> {
        val parser = newParser(input)
        val sheets = mutableListOf<SheetRef>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name.localTag() == "sheet") {
                val name = attribute(parser, "name").orEmpty()
                val relationshipId = relationshipId(parser).orEmpty()
                if (relationshipId.isNotBlank()) sheets += SheetRef(name, relationshipId)
            }
            parser.next()
        }
        return sheets
    }

    private fun parseWorksheet(input: InputStream, sharedStrings: List<String>): List<List<String>> {
        val parser = newParser(input)
        val rows = mutableListOf<MutableList<String>>()
        var currentRow: MutableList<String>? = null
        var currentRowNumber = rows.size + 1
        var cellColumn = 0
        var cellType = ""
        var cellValue = StringBuilder()

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name.localTag()) {
                    "row" -> {
                        currentRowNumber = attribute(parser, "r")?.toIntOrNull() ?: (rows.size + 1)
                        while (rows.size < currentRowNumber) rows.add(mutableListOf())
                        currentRow = rows[currentRowNumber - 1]
                    }
                    "c" -> {
                        cellColumn = cellColumnIndex(attribute(parser, "r").orEmpty())
                        cellType = attribute(parser, "t").orEmpty()
                        cellValue = StringBuilder()
                        val row = currentRow ?: run {
                            rows.add(mutableListOf())
                            rows.last().also { currentRow = it }
                        }
                        while (row.size <= cellColumn) row += ""
                    }
                    "v", "t" -> if (currentRow != null) cellValue.append(parser.nextText())
                }
                XmlPullParser.END_TAG -> if (parser.name.localTag() == "c") {
                    val row = currentRow
                    if (row != null) {
                        val raw = cellValue.toString()
                        val resolved = if (cellType == "s") {
                            sharedStrings.getOrNull(raw.toIntOrNull() ?: -1).orEmpty()
                        } else raw
                        while (row.size <= cellColumn) row += ""
                        row[cellColumn] = resolved
                    }
                }
            }
            parser.next()
        }
        return rows.map(List<String>::toList)
    }

    private fun parsePresentationSlideIds(input: InputStream): List<String> {
        val parser = newParser(input)
        val ids = mutableListOf<String>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name.localTag() == "sldId") {
                relationshipId(parser)?.let(ids::add)
            }
            parser.next()
        }
        return ids
    }

    private fun parseSlideXml(input: InputStream): SlidePage? {
        val parser = newParser(input)
        val shapes = mutableListOf<SlideShape>()
        var inShape = false
        var placeholder: String? = null
        var shapeParagraphs = mutableListOf<String>()
        var paragraphText = StringBuilder()

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name.localTag()) {
                    "sp" -> {
                        inShape = true
                        placeholder = null
                        shapeParagraphs = mutableListOf()
                    }
                    "ph" -> if (inShape) placeholder = attribute(parser, "type")
                    "p" -> if (inShape) paragraphText = StringBuilder()
                    "t" -> if (inShape) paragraphText.append(parser.nextText())
                }
                XmlPullParser.END_TAG -> when (parser.name.localTag()) {
                    "p" -> if (inShape) {
                        val paragraph = paragraphText.toString().trim()
                        if (paragraph.isNotEmpty()) shapeParagraphs += paragraph
                    }
                    "sp" -> if (inShape) {
                        if (shapeParagraphs.isNotEmpty()) shapes += SlideShape(placeholder, shapeParagraphs.toList())
                        inShape = false
                    }
                }
            }
            parser.next()
        }
        if (shapes.isEmpty()) return null
        val titleIndex = shapes.indexOfFirst { it.placeholder == "title" || it.placeholder == "ctrTitle" }
            .takeIf { it >= 0 } ?: 0
        val titleShape = shapes[titleIndex]
        val title = titleShape.paragraphs.joinToString(" ")
        val bullets = shapes.withIndex()
            .filter { it.index != titleIndex }
            .flatMap { it.value.paragraphs }
        return SlidePage(title, bullets, 1, 1)
    }

    private fun newParser(input: InputStream): XmlPullParser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        setInput(input, null)
    }

    private fun docxParagraphStyle(style: String?): BlockStyle = when {
        style.isNullOrBlank() -> BlockStyle.BODY
        style.equals("Title", ignoreCase = true) || style.equals("Subtitle", ignoreCase = true) -> BlockStyle.TITLE
        style.startsWith("Heading", ignoreCase = true) -> BlockStyle.HEADING
        style.contains("List", ignoreCase = true) -> BlockStyle.BULLET
        else -> BlockStyle.BODY
    }

    private fun relationshipId(parser: XmlPullParser): String? {
        for (index in 0 until parser.attributeCount) {
            if (parser.getAttributeName(index) == "id" &&
                parser.getAttributeNamespace(index) == RELATIONSHIP_NAMESPACE
            ) return parser.getAttributeValue(index)
        }
        // Some producers omit the namespace URI but retain the conventional r:id prefix.
        for (index in 0 until parser.attributeCount) {
            if (parser.getAttributeName(index) == "r:id") return parser.getAttributeValue(index)
        }
        return null
    }

    private fun attribute(parser: XmlPullParser, localName: String): String? {
        for (index in 0 until parser.attributeCount) {
            if (parser.getAttributeName(index).substringAfter(':') == localName) {
                return parser.getAttributeValue(index)
            }
        }
        return null
    }

    private fun String.localTag(): String = substringAfter(':')

    private fun cellColumnIndex(reference: String): Int {
        val letters = reference.takeWhile(Char::isLetter).uppercase()
        var column = 0
        letters.forEach { character -> column = column * 26 + (character - 'A' + 1) }
        return (column - 1).coerceAtLeast(0)
    }

    private const val RELATIONSHIP_NAMESPACE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    private fun resolvePartPath(basePart: String, target: String): String {
        if (target.startsWith('/')) return target.trimStart('/')
        val combined = (basePart.substringBeforeLast('/') + "/" + target).split('/')
        val clean = mutableListOf<String>()
        combined.forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (clean.isNotEmpty()) clean.removeAt(clean.lastIndex)
                else -> clean += part
            }
        }
        return clean.joinToString("/")
    }
}
