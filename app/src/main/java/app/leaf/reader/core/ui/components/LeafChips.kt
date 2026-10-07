package app.leaf.reader.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.leaf.reader.core.ui.theme.LeafMetrics
import app.leaf.reader.core.ui.theme.LeafShape
import app.leaf.reader.core.ui.theme.LeafSpacing
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.ui.util.dashedOutline
import app.leaf.reader.core.ui.util.rightFade

/**
 * One filter rail: a horizontally scrolling row with 16 dp gutters and the mockup's
 * right-edge fade (§6.1). The three rails stack tightly, so the vertical padding is a
 * parameter rather than a constant.
 */
@Composable
fun LeafChipRail(
    modifier: Modifier = Modifier,
    top: Dp = 2.dp,
    bottom: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    Row(
        modifier = modifier
            .rightFade(surface)
            .horizontalScroll(rememberScrollState())
            .padding(start = LeafSpacing.screenH, end = LeafSpacing.screenH, top = top, bottom = bottom),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

/**
 * A filter chip (§4.6): 32 dp tall (28 dp `mini`), 8 dp radius, 1 dp `outlineVariant`,
 * active = `secondaryContainer`.
 */
@Composable
fun LeafFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    mini: Boolean = false,
    dashed: Boolean = false,
    leading: @Composable (() -> Unit)? = null
) {
    val height: Dp = if (mini) LeafMetrics.chipHeightMini else LeafMetrics.chipHeight
    val shape: Shape = RoundedCornerShape(LeafMetrics.chipRadius)
    val outline = MaterialTheme.colorScheme.outlineVariant
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .height(height)
            .then(if (dashed) Modifier.dashedOutline(outline, radius = LeafMetrics.chipRadius) else Modifier),
        onClick = onClick,
        shape = shape,
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (mini) 10.dp else LeafSpacing.cardPad),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) leading()
            Text(
                text = label,
                style = if (mini) LeafType.supporting else LeafType.chipText,
                maxLines = 1
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = if (mini) LeafType.supporting else LeafType.chipText,
                    color = content.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}

/** The mockup's 16 dp folder swatch inside a folder chip. */
@Composable
fun FolderDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .background(color, RoundedCornerShape(LeafShape.xs))
    )
}

/** The mockup's 8 dp colour dot inside a tag chip. */
@Composable
fun TagDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(color, RoundedCornerShape(LeafShape.full))
    )
}

/** A label chip on a document row: 10 sp / 700, the tag colour at 16 % behind it. */
@Composable
fun LeafTagChip(name: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LeafShape.full),
        color = color.copy(alpha = 0.16f),
        contentColor = color
    ) {
        Text(
            text = name,
            style = LeafType.chipLabel,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
            maxLines = 1
        )
    }
}

/** Uppercase section label ("8 DOCUMENTS"). */
@Composable
fun LeafSectionLabel(text: String, modifier: Modifier = Modifier, style: TextStyle = LeafType.sectionLabel) {
    Text(
        text = text.uppercase(),
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
        maxLines = 1
    )
}
