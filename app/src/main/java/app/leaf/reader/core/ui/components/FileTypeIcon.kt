package app.leaf.reader.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.ui.theme.FileTypeTones
import app.leaf.reader.core.ui.theme.LeafMetrics
import app.leaf.reader.core.ui.theme.LeafShape
import app.leaf.reader.core.ui.util.dogEar

/**
 * The tonal file-type icon (§4.6): 40 × 48 dp with an 11 dp dog-ear, an 11 dp top-right
 * radius, a 9 sp / 800 label and the type's own container / label colours. The label is
 * inside the icon so the type never depends on colour alone (§10).
 */
@Composable
fun FileTypeIcon(
    type: DocType,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val dark = isSystemInDarkTheme()
    val tone = FileTypeTones.getValue(type)
    val container = if (dark) tone.containerDark else tone.containerLight
    val label = if (dark) tone.labelDark else tone.labelLight
    val width = if (compact) LeafMetrics.crumbIconWidth else LeafMetrics.fileIconWidth
    val height = if (compact) LeafMetrics.crumbIconHeight else LeafMetrics.fileIconHeight
    val fontSize: TextUnit = if (compact) 8.sp else 9.sp
    val ear = if (compact) 8.dp else LeafMetrics.dogEar

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .dogEar(color = label.copy(alpha = 0.30f), corner = ear)
            .background(
                color = container,
                shape = RoundedCornerShape(
                    topStart = LeafShape.s,
                    topEnd = LeafShape.m,
                    bottomEnd = LeafShape.s,
                    bottomStart = LeafShape.s
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = type.label,
            color = label,
            fontSize = fontSize,
            fontWeight = FontWeight.W800,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
    }
}

/** Parses a `#RRGGBB` seed colour (folder and tag swatches). */
fun swatchColor(hex: String, fallback: Color = Color(0xFF2B7A5B)): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
