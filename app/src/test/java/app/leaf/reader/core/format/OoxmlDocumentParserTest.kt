package app.leaf.reader.core.format

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OoxmlDocumentParserTest {

    @Test
    fun docx_streaming_parser_keeps_headings_lists_tables_and_image_alt_text() {
        val file = packageZip(
            "word/document.xml" to """
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
                  <w:body>
                    <w:p><w:pPr><w:pStyle w:val="Title"/></w:pPr><w:r><w:t>Annual plan</w:t></w:r></w:p>
                    <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Goals</w:t></w:r></w:p>
                    <w:p><w:pPr><w:numPr/></w:pPr><w:r><w:t>Ship the reader</w:t></w:r></w:p>
                    <w:tbl><w:tr><w:tc><w:p><w:r><w:t>Quarter</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>Target</w:t></w:r></w:p></w:tc></w:tr></w:tbl>
                    <w:p><w:r><w:drawing><wp:docPr descr="Diagram description"/></w:drawing></w:r></w:p>
                  </w:body>
                </w:document>
            """.trimIndent()
        )
        try {
            val blocks = OoxmlDocumentParser.parseDocx(file)
            assertEquals(
                listOf("Annual plan", "Goals", "Ship the reader", "Quarter  |  Target", "[Image: Diagram description]"),
                blocks.map(TextBlock::text)
            )
            assertEquals(BlockStyle.TITLE, blocks[0].style)
            assertEquals(BlockStyle.HEADING, blocks[1].style)
            assertEquals(BlockStyle.BULLET, blocks[2].style)
            assertEquals(BlockStyle.TABLE_ROW, blocks[3].style)
            assertEquals(BlockStyle.IMAGE, blocks[4].style)
        } finally {
            file.delete()
        }
    }

    @Test
    fun xlsx_parser_follows_relationship_id_and_resolves_shared_strings_and_cell_addresses() {
        val file = packageZip(
            "xl/workbook.xml" to """
                <workbook xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="Revenue" sheetId="1" r:id="rIdRevenue"/></sheets>
                </workbook>
            """.trimIndent(),
            "xl/_rels/workbook.xml.rels" to """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rIdRevenue" Type="worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
            """.trimIndent(),
            "xl/sharedStrings.xml" to """
                <sst><si><t>Region</t></si><si><r><t>North</t></r><r><t> America</t></r></si></sst>
            """.trimIndent(),
            "xl/worksheets/sheet1.xml" to """
                <worksheet><sheetData>
                  <row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1"><v>Amount</v></c></row>
                  <row r="2"><c r="A2" t="s"><v>1</v></c><c r="B2"><v>1250</v></c></row>
                </sheetData></worksheet>
            """.trimIndent()
        )
        try {
            val workbook = OoxmlDocumentParser.parseXlsx(file)
            assertEquals(listOf("Revenue"), workbook.map { it.first })
            assertEquals(listOf(listOf("Region", "Amount"), listOf("North America", "1250")), workbook.single().second)
        } finally {
            file.delete()
        }
    }

    @Test
    fun pptx_parser_respects_presentation_order_and_uses_layout_text_when_slide_has_no_placeholder_text() {
        val file = packageZip(
            "ppt/presentation.xml" to """
                <p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <p:sldIdLst><p:sldId id="256" r:id="rIdSecond"/><p:sldId id="257" r:id="rIdFirst"/><p:sldId id="258" r:id="rIdBlank"/></p:sldIdLst>
                </p:presentation>
            """.trimIndent(),
            "ppt/_rels/presentation.xml.rels" to """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rIdFirst" Type="slide" Target="slides/slide1.xml"/>
                  <Relationship Id="rIdSecond" Type="slide" Target="slides/slide2.xml"/>
                  <Relationship Id="rIdBlank" Type="slide" Target="slides/slide3.xml"/>
                </Relationships>
            """.trimIndent(),
            "ppt/slides/slide1.xml" to """
                <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                  <p:spTree><p:sp><p:nvSpPr><p:nvPr><p:ph type="title"/></p:nvPr></p:nvSpPr><p:txBody><a:p><a:r><a:t>First slide</a:t></a:r></a:p></p:txBody></p:sp></p:spTree>
                </p:sld>
            """.trimIndent(),
            "ppt/slides/slide2.xml" to """
                <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <p:spTree><p:sp><p:nvSpPr><p:nvPr><p:ph type="title"/></p:nvPr></p:nvSpPr><p:txBody><a:p/></p:txBody></p:sp></p:spTree>
                </p:sld>
            """.trimIndent(),
            "ppt/slides/slide3.xml" to """
                <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                  <p:spTree/>
                </p:sld>
            """.trimIndent(),
            "ppt/slides/_rels/slide2.xml.rels" to """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rIdLayout" Type="slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
                </Relationships>
            """.trimIndent(),
            "ppt/slideLayouts/slideLayout1.xml" to """
                <p:sldLayout xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                  <p:cSld><p:spTree><p:sp><p:nvSpPr><p:nvPr><p:ph type="title"/></p:nvPr></p:nvSpPr><p:txBody><a:p><a:r><a:t>Layout title</a:t></a:r></a:p></p:txBody></p:sp></p:spTree></p:cSld>
                </p:sldLayout>
            """.trimIndent()
        )
        try {
            val slides = OoxmlDocumentParser.parsePptx(file)
            assertEquals(listOf("Layout title", "First slide", ""), slides.map(SlidePage::title))
            assertEquals(listOf(1, 2, 3), slides.map(SlidePage::slideNumber))
            assertEquals(3, slides.first().slideCount)
            assertTrue(slides.take(2).all { it.title.isNotBlank() })
        } finally {
            file.delete()
        }
    }

    private fun packageZip(vararg entries: Pair<String, String>): File =
        File.createTempFile("leaf-ooxml-test", ".zip").also { file ->
            ZipOutputStream(file.outputStream()).use { zip ->
                entries.forEach { (name, text) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(text.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
        }
}
