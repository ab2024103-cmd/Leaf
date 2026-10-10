package app.leaf.reader.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.leaf.reader.R
import app.leaf.reader.core.ui.theme.LeafType

@Composable
internal fun PickDocumentBanner(
    tabsInUse: Int,
    tabLimit: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.reader_pick_title),
                style = LeafType.body,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.reader_pick_subtitle, tabsInUse, tabLimit),
                style = LeafType.supporting,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        TextButton(onClick = onCancel) {
            Text(
                text = stringResource(R.string.reader_pick_cancel),
                style = LeafType.chipText,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
