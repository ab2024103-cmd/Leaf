package app.leaf.reader.core.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.leaf.reader.R
import app.leaf.reader.core.ui.theme.LeafMetrics
import app.leaf.reader.core.ui.theme.LeafMotion
import app.leaf.reader.core.ui.theme.LeafShape
import app.leaf.reader.core.ui.theme.LeafType

/**
 * The sheet surface itself (§4.6): 28 dp top corners, `surfaceContainerLow`, a 32 × 4 dp
 * grabber at 40 %, a 16 dp / 600 title and a 12.5 dp supporting line. Kept separate from
 * [LeafSheetOverlay] so the screenshot tests can render a sheet without its scrim or its
 * enter animation.
 */
@Composable
fun LeafSheetSurface(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = LeafMetrics.sheetRadius, topEnd = LeafMetrics.sheetRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = LeafElevationLevel3
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp, bottom = 12.dp)
                    .size(width = LeafMetrics.grabberWidth, height = LeafMetrics.grabberHeight)
                    .align(Alignment.CenterHorizontally)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = LeafMetrics.grabberAlpha),
                        shape = CircleShape
                    )
            )
            Text(
                text = title,
                style = LeafType.sheetTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 22.dp)
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = LeafType.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
                )
            }
            Box(modifier = Modifier.height(6.dp))
            content()
            Box(modifier = Modifier.height(18.dp))
        }
    }
}

/**
 * Scrim + sheet, capped at 82 % of the height (§4.6) and dismissed by the scrim or by
 * back (§8.10: back closes the sheet before it navigates).
 */
@Composable
fun LeafSheetOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheet: @Composable () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible.targetState) LeafMetrics.scrimAlpha else 0f,
        animationSpec = tween(durationMillis = LeafMotion.sheet, easing = LeafMotion.easeEmphasized),
        label = "scrim"
    )
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(onClick = onDismiss, indication = null, interactionSource = remember { MutableInteractionSource() })
        )
        AnimatedVisibility(
            visibleState = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                animationSpec = tween(durationMillis = LeafMotion.sheet, easing = LeafMotion.easeEmphasized)
            ) { fullHeight -> fullHeight } + fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * LeafMetrics.sheetMaxHeightFraction)
            ) {
                sheet()
            }
        }
    }
}

/**
 * One row of a sheet (§4.6): a 38 dp icon circle, a 14 sp / 600 title, an 11.5 sp
 * supporting line and an optional trailing check.
 */
@Composable
fun LeafSheetItem(
    icon: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    container: Color? = null,
    content: Color? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
    indent: Boolean = false
) {
    val iconContainer = container
        ?: if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val iconColor = content
        ?: if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = if (indent) 38.dp else 22.dp, end = 22.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LeafType.body,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = LeafType.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/** The 1 dp divider between groups of sheet rows. */
@Composable
fun LeafSheetDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/** The sheet's ✓ marker (§4.6: primary, 16 sp, 800). */
@Composable
fun LeafCheck() {
    Text(
        text = "✓",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 16.sp,
        fontWeight = FontWeight.W800
    )
}

/**
 * Segmented control (§4.6): full radius, 1 dp `outline`, the active segment filled with
 * `secondaryContainer`, 7 × 14 dp segment padding.
 */
@Composable
fun LeafSegmented(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(LeafShape.full),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
        ) {
            Row {
                labels.forEachIndexed { index, label ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .size(width = 1.dp, height = 32.dp)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                    }
                    val selected = index == selectedIndex
                    TextButton(
                        onClick = { onSelect(index) },
                        shape = RoundedCornerShape(LeafShape.full),
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            },
                            contentColor = if (selected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        ),
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = label,
                            style = LeafType.chipText,
                            modifier = Modifier.padding(
                                horizontal = LeafMetrics.segmentPaddingH,
                                vertical = LeafMetrics.segmentPaddingV
                            )
                        )
                    }
                }
            }
        }
    }
}

/** The 10-swatch colour picker (§2.1): 34 dp circles, 9 dp gaps, ✓ on the current one. */
@Composable
fun LeafPalette(
    colors: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.padding(horizontal = 22.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        colors.forEach { hex ->
            val color = swatchColor(hex)
            val isSelected = hex.equals(selected, ignoreCase = true)
            Surface(
                onClick = { onSelect(hex) },
                shape = CircleShape,
                color = color,
                border = if (isSelected) {
                    BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface)
                } else {
                    null
                },
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W800,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * The mockup's prompt sheet: a hint, one text field and a filled Save pill. Used for
 * rename / new folder / new subfolder / new tag.
 */
@Composable
fun LeafPromptSheet(
    title: String,
    hint: String,
    placeholder: String,
    initial: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var value by remember(initial) { mutableStateOf(initial) }
    LeafSheetSurface(title = title, subtitle = hint, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(placeholder, style = LeafType.body) },
                textStyle = LeafType.body,
                singleLine = true,
                shape = RoundedCornerShape(LeafShape.xs),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { if (value.isNotBlank()) onSave(value.trim()) }
                )
            )
            androidx.compose.material3.Button(
                onClick = { if (value.isNotBlank()) onSave(value.trim()) },
                shape = RoundedCornerShape(LeafShape.full)
            ) {
                Text(text = stringResource(R.string.action_save), style = LeafType.chipText)
            }
        }
    }
}

private val LeafElevationLevel3 = 3.dp
