package app.leaf.reader.core.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.leaf.reader.R
import app.leaf.reader.core.ui.theme.LeafMetrics
import app.leaf.reader.core.ui.theme.LeafMotion
import app.leaf.reader.core.ui.theme.LeafSpacing
import app.leaf.reader.core.ui.theme.LeafType
import kotlinx.coroutines.delay

/**
 * The confirm dialog (§4.6): 28 dp radius, max 340 dp wide, a 44 dp `errorContainer`
 * icon, a left-aligned 17 sp / 600 title and right-aligned actions. Used for every
 * destructive action (§6.7).
 */
@Composable
fun LeafConfirmDialogSurface(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = true
) {
    Surface(
        modifier = modifier
            .widthIn(max = LeafMetrics.dialogMaxWidth)
            .fillMaxWidth(),
        shape = RoundedCornerShape(LeafMetrics.sheetRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = LeafType.dialogTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
            )
            Text(
                text = body,
                style = LeafType.dialogBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), style = LeafType.chipText)
                }
                TextButton(
                    onClick = onConfirm,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = if (danger) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(confirmLabel, style = LeafType.chipText)
                }
            }
        }
    }
}

/** Dialog + scrim, centred, dismissed by back (§8.10). */
@Composable
fun LeafConfirmDialogOverlay(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = true
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = LeafMetrics.scrimAlpha))
            .clickable(onClick = onDismiss, indication = null, interactionSource = remember { MutableInteractionSource() }),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.padding(horizontal = LeafSpacing.screenH)) {
            LeafConfirmDialogSurface(
                title = title,
                body = body,
                confirmLabel = confirmLabel,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                danger = danger
            )
        }
    }
}

/**
 * Snackbar (§4.6): inverse surface, 4 dp radius, 16 dp side margins. It sits above the
 * navigation bar because the screen ends where the bar begins. Plain messages last
 * 2 200 ms; one that offers an action lasts 4 200 ms (§6.7). One at a time — the newest
 * message replaces whatever was showing (§8.11).
 */
@Composable
fun LeafSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: () -> Unit = {},
    durationMs: Long = LeafMotion.snackbar
) {
    val currentMessage by rememberUpdatedState(message)
    val currentDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(currentMessage, actionLabel, durationMs) {
        delay(durationMs)
        currentDismiss()
    }
    Surface(
        modifier = modifier
            .padding(horizontal = LeafMetrics.snackbarSide)
            .padding(bottom = LeafMetrics.snackbarGap),
        shape = RoundedCornerShape(LeafMetrics.snackbarRadius),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = LeafType.body,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.inversePrimary
                    )
                ) {
                    Text(
                        text = actionLabel,
                        style = LeafType.chipText,
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
        }
    }
}
