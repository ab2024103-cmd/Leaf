package app.leaf.reader.core.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The right-edge fade the mockup masks onto every horizontally scrolling rail
 * (`mask-image: linear-gradient(90deg, #000 0, #000 calc(100% - 26px), transparent 100%)`).
 * Painting the surface colour over the content is API 23-safe; a real mask is not (§9).
 */
fun Modifier.rightFade(color: Color, width: Dp = 26.dp): Modifier =
    this.then(
        Modifier.drawWithContent {
            drawContent()
            val fade = width.toPx()
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, color),
                    start = Offset(x = size.width - fade, y = 0f),
                    end = Offset(x = size.width, y = 0f)
                ),
                topLeft = Offset(x = size.width - fade, y = 0f),
                size = Size(width = fade, height = size.height)
            )
        }
    )

/** A 1 dp dashed outline — the mockup's `+ Collection` / `+ Tag` chips. */
fun Modifier.dashedOutline(color: Color, width: Dp = 1.dp, radius: Dp = 8.dp): Modifier =
    this.then(
        Modifier.drawWithContent {
            drawContent()
            inset(0.5f) {
                drawRoundRect(
                    color = color,
                    style = Stroke(
                        width = width.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    ),
                    cornerRadius = CornerRadius(radius.toPx())
                )
            }
        }
    )

/**
 * The file icon's dog-ear: an 11 dp triangle at the top-right, filled with the label
 * colour at 30 % (§4.6).
 */
fun Modifier.dogEar(color: Color, corner: Dp = 11.dp): Modifier =
    this.then(
        Modifier.drawWithContent {
            drawContent()
            val ear = corner.toPx()
            val right = size.width
            drawPath(
                path = Path().apply {
                    moveTo(right - ear, 0f)
                    lineTo(right, 0f)
                    lineTo(right, ear)
                    close()
                },
                color = color
            )
        }
    )

/** A clickable slot whose talk-back label is the action it performs (§10). */
fun Modifier.leafClickable(onClick: () -> Unit, label: String): Modifier =
    this.clickable(onClickLabel = label, onClick = onClick)

