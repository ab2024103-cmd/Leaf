package app.leaf.reader.core.format

import app.leaf.reader.core.model.DocType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatResolverTest {
    @Test
    fun resolves_by_mime_before_suffix_and_uses_suffix_when_mime_is_generic() {
        assertEquals(
            DocType.DOCX,
            resolveDocType("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "report.txt")
        )
        assertEquals(DocType.XLSX, resolveDocType("application/octet-stream", "forecast.XLSX"))
        assertEquals(DocType.EPUB, resolveDocType("application/epub+zip; charset=binary", "book.bin"))
        assertNull(resolveDocType("application/octet-stream", "archive.zip"))
    }
}
