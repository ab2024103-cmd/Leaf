package app.leaf.reader

import app.leaf.reader.core.util.renameKeepingExtension
import org.junit.Assert.assertEquals
import org.junit.Test

/** §8.6: renaming keeps the extension, the way the mockup's `renamePrompt` does. */
class RenameTest {

    @Test
    fun typing_a_plain_name_keeps_the_extension() {
        assertEquals(
            "Annual report.pdf",
            renameKeepingExtension("Annual report 2024.pdf", "Annual report")
        )
    }

    @Test
    fun typing_your_own_extension_is_respected() {
        assertEquals(
            "Notes.txt",
            renameKeepingExtension("Notes.pdf", "Notes.txt")
        )
    }

    @Test
    fun typing_another_extension_keeps_the_one_you_typed() {
        // The mockup trusts an explicit extension: "report.docx" on a PDF stays DOCX.
        assertEquals(
            "report.docx",
            renameKeepingExtension("old.pdf", "report.docx")
        )
    }

    @Test
    fun a_document_without_an_extension_stays_without_one() {
        assertEquals("README", renameKeepingExtension("README", "CHANGELOG"))
    }

    @Test
    fun surrounding_space_is_trimmed() {
        assertEquals("  Q3.pdf".trim(), renameKeepingExtension("old.pdf", "  Q3  "))
    }

    @Test
    fun an_empty_name_keeps_the_current_one() {
        assertEquals("old.pdf", renameKeepingExtension("old.pdf", "   "))
    }
}
