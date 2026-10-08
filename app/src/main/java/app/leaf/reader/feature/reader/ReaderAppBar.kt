package app.leaf.reader.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.leaf.reader.R
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.ui.theme.LeafType

@Composable
internal fun ReaderAppBar(state: ReaderContentState, onBack: () -> Unit, onLayout: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.reader_back_to_library))
        }
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                state.document?.name.orEmpty(),
                style = LeafType.listTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.reader_subtitle,
                    state.pageCount,
                    if (state.scrollDir == ScrollDir.VERTICAL) stringResource(R.string.reader_vertical_lower) else stringResource(R.string.reader_horizontal_lower)
                ),
                style = LeafType.listMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onLayout, modifier = Modifier.align(Alignment.CenterEnd)) {
            Icon(painterResource(R.drawable.ic_view_layout), contentDescription = stringResource(R.string.reader_view_layout_title))
        }
    }
}
