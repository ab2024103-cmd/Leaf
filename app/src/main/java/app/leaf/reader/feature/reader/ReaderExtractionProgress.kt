package app.leaf.reader.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/** Exposes the background text extraction progress used by M4's cached document search. */
@Composable
internal fun ReaderExtractionProgress(progress: Float) {
    val fraction = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier.fillMaxWidth().height(2.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(fraction).height(2.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
