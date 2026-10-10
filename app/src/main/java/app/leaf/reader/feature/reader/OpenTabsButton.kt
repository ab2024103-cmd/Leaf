package app.leaf.reader.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.leaf.reader.R

/** Open-documents action with a compact, accessible count badge. */
@Composable
fun OpenTabsButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_tabs),
                contentDescription = stringResource(R.string.cd_open_documents_count, count),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-1).dp, y = 1.dp)
                .size(if (count > 9) 19.dp else 17.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = if (count > 9) 8.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
