package app.leaf.reader.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.leaf.reader.core.ui.theme.LeafMetrics
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.navigation.LeafDestination

/**
 * Bottom navigation for compact widths (< 600 dp): five items, icon plus an
 * 11.5 sp label, and a 62 × 30 dp active pill (§5).
 */
@Composable
fun LeafNavBar(
    selected: LeafDestination,
    onSelect: (LeafDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            LeafDestination.entries.forEach { destination ->
                LeafNavItem(
                    label = stringResource(destination.titleRes),
                    icon = destination.icon,
                    selected = destination == selected,
                    onSelect = { onSelect(destination) },
                    modifier = Modifier.weight(1f),
                    pillWidth = LeafMetrics.navPillWidth,
                    pillHeight = LeafMetrics.navPillHeight
                )
            }
        }
    }
}

/**
 * Navigation rail for medium and expanded widths (≥ 600 dp): 84 dp wide, icon above
 * an 11.5 sp label, 56 × 32 dp active pill (§5).
 */
@Composable
fun LeafNavRail(
    selected: LeafDestination,
    onSelect: (LeafDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .width(LeafMetrics.railWidth)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LeafDestination.entries.forEach { destination ->
                LeafNavItem(
                    label = stringResource(destination.titleRes),
                    icon = destination.icon,
                    selected = destination == selected,
                    onSelect = { onSelect(destination) },
                    modifier = Modifier.padding(vertical = 2.dp),
                    pillWidth = LeafMetrics.railPillWidth,
                    pillHeight = LeafMetrics.railPillHeight
                )
            }
        }
    }
}

@Composable
private fun LeafNavItem(
    label: String,
    icon: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    pillWidth: androidx.compose.ui.unit.Dp,
    pillHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
                contentDescription = label
            }
            .selectable(selected = selected, onClick = onSelect, role = Role.Tab)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = pillWidth, height = pillHeight)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    shape = RoundedCornerShape(percent = 50)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = LeafType.navLabel,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}
