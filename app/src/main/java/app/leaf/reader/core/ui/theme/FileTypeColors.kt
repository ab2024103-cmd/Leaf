package app.leaf.reader.core.ui.theme

import androidx.compose.ui.graphics.Color
import app.leaf.reader.core.model.DocType

/**
 * Tonal file-type icon containers — LEAF-MASTER-PROMPT.md §4.6.
 *
 * Light canvas values are the §4.6 table copied verbatim. Dark canvas values are the
 * mockup's own dark tokens (§4.6 states the rule as "containers at 30 % tone, labels
 * at 90 %" without hexes, so the mockup's explicit dark set is used).
 */
data class FileTypeTone(
    /** Container in the light app theme. */
    val containerLight: Color,
    /** Label in the light app theme. */
    val labelLight: Color,
    /** Container in the dark app theme. */
    val containerDark: Color,
    /** Label in the dark app theme. */
    val labelDark: Color
)

val FileTypeTones: Map<DocType, FileTypeTone> = mapOf(
    DocType.PDF to FileTypeTone(
        containerLight = Color(0xFFFFDAD6), labelLight = Color(0xFF93000A),
        containerDark = Color(0xFF5A1F1C), labelDark = Color(0xFFFFDAD6)
    ),
    DocType.DOCX to FileTypeTone(
        containerLight = Color(0xFFC1E9FB), labelLight = Color(0xFF0F4A5C),
        containerDark = Color(0xFF1D3F4C), labelDark = Color(0xFFC1E9FB)
    ),
    DocType.XLSX to FileTypeTone(
        containerLight = Color(0xFFCDE7C4), labelLight = Color(0xFF2C4A22),
        containerDark = Color(0xFF1E4A2C), labelDark = Color(0xFFC7ECC7)
    ),
    DocType.PPTX to FileTypeTone(
        containerLight = Color(0xFFFFD9C2), labelLight = Color(0xFF7A3A15),
        containerDark = Color(0xFF4D2C12), labelDark = Color(0xFFFFDCC2)
    ),
    DocType.TXT to FileTypeTone(
        containerLight = Color(0xFFE0E6E1), labelLight = Color(0xFF3C4640),
        containerDark = Color(0xFF3A4440), labelDark = Color(0xFFDBE5DD)
    ),
    DocType.EPUB to FileTypeTone(
        containerLight = Color(0xFFA8F2CE), labelLight = Color(0xFF005138),
        containerDark = Color(0xFF1C4C37), labelDark = Color(0xFFA8F2CE)
    )
)
