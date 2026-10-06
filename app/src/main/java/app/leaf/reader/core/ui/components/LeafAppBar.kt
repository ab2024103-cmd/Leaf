package app.leaf.reader.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.leaf.reader.R
import app.leaf.reader.core.ui.theme.LeafMetrics
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.ui.util.annotated

/**
 * Leaf top app bar (small, §6.1). Padding matches the mockup: 4 dp top, 10 dp bottom,
 * 16 dp start, 8 dp end. It gains a `surfaceContainer` tint and a 1 dp hairline once
 * the content has scrolled past 4 dp — scroll-linked elevation (§6.1).
 */
@Composable
fun LeafTopAppBar(
    title: String,
    subtitle: String? = null,
    showBrand: Boolean = false,
    scrolled: Boolean = false,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null
) {
    Surface(
        color = if (scrolled) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBrand) {
                    Icon(
                        painter = painterResource(R.drawable.ic_leaf_mark),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(26.dp)
                            .padding(end = 10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = LeafType.screenTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrEmpty()) {
                        Text(
                            text = subtitle,
                            style = LeafType.appBarSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (actions != null) {
                    Row(horizontalArrangement = Arrangement.End) { actions() }
                }
            }
            if (scrolled) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/**
 * One content screen: app bar over a scrolling body with 16 dp side padding (§5).
 * The body keeps its own scroll position for the session (§8.4).
 */
@Composable
fun LeafScreen(
    title: String,
    subtitle: String? = null,
    showBrand: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    scrolled: Boolean = false,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LeafTopAppBar(
            title = title,
            subtitle = subtitle,
            showBrand = showBrand,
            scrolled = scrolled,
            actions = actions
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            content()
        }
    }
}

/**
 * Empty state (§6.1): a 64 dp `secondaryContainer` circle holding a 30 dp icon, a
 * 15.5 sp title and 13 sp body copy — 48 dp top padding, 30 dp side padding.
 */
@Composable
fun LeafEmptyState(
    icon: Int,
    title: String,
    message: CharSequence,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 30.dp, end = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(LeafMetrics.heroRadius),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Text(
            text = title,
            style = LeafType.emptyTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = annotated(message),
            style = LeafType.emptyBody,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 7.dp)
                .widthIn(max = 280.dp)
        )
    }
}
