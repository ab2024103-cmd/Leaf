package app.leaf.reader.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.leaf.reader.R
import app.leaf.reader.core.domain.LibraryDoc
import app.leaf.reader.core.domain.TypeGroupCard
import app.leaf.reader.core.ui.theme.LeafMetrics
import app.leaf.reader.core.ui.theme.LeafMotion
import app.leaf.reader.core.ui.theme.LeafSpacing
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.ui.util.leafPressable
import app.leaf.reader.core.ui.util.rememberHaptics

/** The pre-resolved text of a row's meta line, so no sentence is built in composable code. */
data class RowMeta(
    val pages: String,
    val size: String,
    /** `null` when the document has no bookmarks — the part is then left out entirely. */
    val bookmarks: String?,
    val lastOpened: String
)

/**
 * A library document row (§4.6): 16 dp radius, `surfaceContainerLow`, 11 dp vertical
 * padding, a 40 × 48 dp file icon, then name · meta · tags · progress · star.
 *
 * Long-press (470 ms) opens the actions sheet with a pressed state and a haptic (§8.9).
 * The tap is deliberately not wired: §6.1 opens the reader, and the reader is M3 — a row
 * that pretends to open something would be a dead affordance.
 */
@Composable
fun LeafDocumentRow(
    doc: LibraryDoc,
    meta: RowMeta,
    tagColors: (String) -> Color,
    onStar: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    val progressDescription = if (doc.showProgress) {
        stringResource(R.string.cd_reading_progress, doc.pctRead, doc.page + 1, doc.pageCount)
    } else {
        ""
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = if (pressed) LeafMotion.pressScale else 1f
                scaleY = if (pressed) LeafMotion.pressScale else 1f
            }
            .semantics {
                contentDescription = doc.document.name
                if (progressDescription.isNotEmpty()) stateDescription = progressDescription
            }
            .leafPressable(
                onPressChange = { pressed = it },
                onLongPress = {
                    haptics.longPress()
                    onLongPress()
                }
            ),
        shape = RoundedCornerShape(LeafMetrics.cardRadius),
        color = if (pressed) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (pressed) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(
                start = LeafSpacing.cardPad,
                end = 8.dp,
                top = LeafMetrics.rowVerticalPadding,
                bottom = LeafMetrics.rowVerticalPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(LeafMetrics.rowIconGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileTypeIcon(type = doc.document.type)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.document.name,
                    style = LeafType.listTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetaPart(meta.pages)
                    Dot()
                    MetaPart(meta.size)
                    if (meta.bookmarks != null) {
                        Dot()
                        MetaPart(meta.bookmarks)
                    }
                    Dot()
                    MetaPart(meta.lastOpened)
                }
                if (doc.document.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        doc.document.tags.forEach { tag ->
                            LeafTagChip(name = tag, color = tagColors(tag))
                        }
                    }
                }
                if (doc.showProgress) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(LeafMetrics.progressHeight)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(doc.pctRead / 100f)
                                    .height(LeafMetrics.progressHeight)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Text(
                            text = stringResource(R.string.progress_label, doc.pctRead, doc.page + 1),
                            style = LeafType.chipLabel,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
            LeafStarButton(favorite = doc.document.favorite, onClick = onStar)
        }
    }
}

@Composable
private fun MetaPart(text: String) {
    Text(
        text = text,
        style = LeafType.listMeta,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}

@Composable
private fun Dot() {
    Text(
        text = stringResource(R.string.separator),
        style = LeafType.listMeta,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
}

/** The row star: 18 dp icon in a 40 dp target, `tertiary` and filled when favorite. */
@Composable
fun LeafStarButton(
    favorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(40.dp)) {
        Icon(
            painter = painterResource(if (favorite) R.drawable.ic_star_filled else R.drawable.ic_star),
            contentDescription = stringResource(if (favorite) R.string.cd_star_on else R.string.cd_star_off),
            tint = if (favorite) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * A file-type group card (§6.1): `surfaceContainerLow`, 1 dp `outlineVariant`, 16 dp
 * radius, 14 dp padding, the 40 × 48 dp icon, the type name and its summary line, then a
 * trailing chevron.
 */
@Composable
fun LeafGroupCard(
    card: TypeGroupCard,
    title: String,
    summaryParts: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LeafMetrics.cardRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileTypeIcon(type = card.type)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = LeafType.listTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    summaryParts.forEachIndexed { index, part ->
                        if (index > 0) Dot()
                        MetaPart(part)
                    }
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
