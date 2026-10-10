package app.leaf.reader.feature.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import app.leaf.reader.core.model.Highlight
import app.leaf.reader.core.model.HighlightColor
import app.leaf.reader.core.model.NormalizedRect
import app.leaf.reader.core.ui.theme.HighlightRingAlpha
import app.leaf.reader.core.ui.theme.HighlightSwatches
import app.leaf.reader.core.ui.theme.currentReadingPalette

@Composable
internal fun AnnotationOverlay(
    pageIndex: Int,
    highlights: List<Highlight>,
    selection: ReaderSelection?,
    selectedHighlightId: String?,
    highlightColor: HighlightColor,
    onHandleDrag: (Boolean, Float, Float) -> Unit
) {
    val palette = currentReadingPalette
    val pageHighlights = highlights.filter { it.page == pageIndex && it.bounds.isNotEmpty() }
    val pageSelection = selection?.takeIf { it.pageIndex == pageIndex }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            pageHighlights.forEach { highlight ->
                val fill = HighlightSwatches[highlight.color]?.fillFor(palette) ?: Color.Yellow.copy(alpha = 0.5f)
                highlight.bounds.forEach { rect ->
                    drawNormalizedRect(rect, fill)
                    if (highlight.id == selectedHighlightId) {
                        drawNormalizedRect(
                            rect,
                            Color.Transparent,
                            stroke = androidx.compose.ui.graphics.Color(0xFF206A4E).copy(alpha = HighlightRingAlpha)
                        )
                        drawNormalizedRect(
                            rect,
                            Color.Transparent,
                            stroke = androidx.compose.ui.graphics.Color(0xFF206A4E),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
            pageSelection?.bounds?.forEach { rect ->
                drawNormalizedRect(
                    rect,
                    HighlightSwatches[highlightColor]?.fillFor(palette) ?: Color.Yellow.copy(alpha = 0.5f)
                )
                drawNormalizedRect(rect, Color.Transparent, stroke = androidx.compose.ui.graphics.Color(0xFF206A4E), strokeWidth = 1.dp.toPx())
            }
        }
        if (pageSelection != null && pageSelection.bounds.isNotEmpty()) {
            val ordered = pageSelection.bounds.sortedWith(compareBy<NormalizedRect> { it.top }.thenBy { it.left })
            val start = ordered.first()
            val end = ordered.last()
            SelectionHandle(
                pageIndex = pageIndex,
                isStart = true,
                anchor = Offset(start.left, start.bottom),
                pageWidth = maxWidth,
                pageHeight = maxHeight,
                onDrag = onHandleDrag
            )
            SelectionHandle(
                pageIndex = pageIndex,
                isStart = false,
                anchor = Offset(end.right, end.bottom),
                pageWidth = maxWidth,
                pageHeight = maxHeight,
                onDrag = onHandleDrag
            )
        }
    }
}

@Composable
private fun SelectionHandle(
    pageIndex: Int,
    isStart: Boolean,
    anchor: Offset,
    pageWidth: Dp,
    pageHeight: Dp,
    onDrag: (Boolean, Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val widthPx = with(density) { pageWidth.toPx() }.coerceAtLeast(1f)
    val heightPx = with(density) { pageHeight.toPx() }.coerceAtLeast(1f)
    var position by remember(pageIndex, isStart) { mutableStateOf(anchor) }
    LaunchedEffect(anchor) { position = anchor }
    val x = with(density) { (position.x * widthPx).toDp() - 12.dp }
    val y = with(density) { (position.y * heightPx).toDp() - 12.dp }
    Canvas(
        Modifier
            .offset(x = x, y = y)
            .size(24.dp)
            .pointerInput(pageIndex, isStart, widthPx, heightPx) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val next = Offset(
                        (position.x + dragAmount.x / widthPx).coerceIn(0f, 1f),
                        (position.y + dragAmount.y / heightPx).coerceIn(0f, 1f)
                    )
                    position = next
                    onDrag(isStart, next.x, next.y)
                }
            }
    ) {
        val centerX = size.width / 2f
        val radius = 5.dp.toPx()
        val color = androidx.compose.ui.graphics.Color(0xFF206A4E)
        drawLine(color, Offset(centerX, 0f), Offset(centerX, size.height / 2f), strokeWidth = 2.dp.toPx())
        drawCircle(color, radius, Offset(centerX, size.height / 2f + radius))
        drawCircle(Color.White, radius = radius * 0.45f, center = Offset(centerX, size.height / 2f + radius))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNormalizedRect(
    rect: NormalizedRect,
    color: Color,
    stroke: Color? = null,
    strokeWidth: Float = 1.dp.toPx()
) {
    val topLeft = Offset(rect.left * size.width, rect.top * size.height)
    val rectSize = Size((rect.right - rect.left) * size.width, (rect.bottom - rect.top) * size.height)
    if (rectSize.width <= 0f || rectSize.height <= 0f) return
    if (color.alpha > 0f) drawRect(color = color, topLeft = topLeft, size = rectSize)
    if (stroke != null) drawRect(color = stroke, topLeft = topLeft, size = rectSize, style = Stroke(strokeWidth))
}
