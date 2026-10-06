package app.leaf.reader.core.ui.util

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * True once [scrollState] has moved past [threshold] — the mockup tints the app bar
 * after 4 dp of scroll (§6.1).
 */
@Composable
fun isScrolled(scrollState: ScrollState, threshold: Dp = 4.dp): Boolean {
    val density = LocalDensity.current
    return remember(scrollState, density, threshold) {
        derivedStateOf { scrollState.value > with(density) { threshold.toPx() } }
    }.value
}
