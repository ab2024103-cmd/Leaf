package app.leaf.reader.feature.device

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.leaf.reader.R
import app.leaf.reader.core.data.device.DeviceDocument
import app.leaf.reader.core.data.device.DeviceStorageAccess
import app.leaf.reader.core.data.device.scanDeviceDocuments
import app.leaf.reader.core.ui.components.LeafEmptyState
import app.leaf.reader.core.ui.components.LeafIconButton
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.CancellationException

/**
 * "On this device": asks the user for storage access, then lists every supported document
 * on shared storage. Tapping a row imports the file (copied into Leaf's private storage) and
 * opens it. The system picker stays available for files the list does not show.
 */
@Composable
fun DeviceDocumentsScreen(
    onBack: () -> Unit,
    onOpenDocument: (DeviceDocument) -> Unit,
    onPickWithSystemPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    var granted by remember { mutableStateOf(DeviceStorageAccess.isGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // The All files access page is a separate screen: re-check whenever Leaf comes back to the foreground.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = DeviceStorageAccess.isGranted(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val runtimePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = DeviceStorageAccess.isGranted(context)
    }
    val allFilesSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        granted = DeviceStorageAccess.isGranted(context)
    }
    val requestAccess: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { allFilesSettings.launch(DeviceStorageAccess.allFilesSettingsIntent(context)) }
                .recoverCatching { allFilesSettings.launch(DeviceStorageAccess.allFilesSettingsFallbackIntent()) }
        } else {
            runtimePermission.launch(DeviceStorageAccess.READ_PERMISSION)
        }
    }

    var documents by remember { mutableStateOf<List<DeviceDocument>?>(null) }
    var scanFailed by remember { mutableStateOf(false) }
    LaunchedEffect(granted) {
        documents = null
        scanFailed = false
        if (granted) {
            documents = try {
                scanDeviceDocuments(context)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                scanFailed = true
                emptyList()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // Keeps chrome clear of the status bar and the gesture/navigation bar (§5).
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LeafIconButton(
                icon = R.drawable.ic_back,
                contentDescription = stringResource(R.string.cd_back),
                onClick = onBack
            )
            Text(
                text = stringResource(R.string.device_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        val list = documents
        when {
            !granted -> AccessPrompt(onAllow = requestAccess, onPicker = onPickWithSystemPicker)
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            list.isEmpty() -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                LeafEmptyState(
                    icon = R.drawable.ic_document,
                    title = stringResource(
                        if (scanFailed) R.string.device_scan_failed_title else R.string.device_empty_title
                    ),
                    message = stringResource(
                        if (scanFailed) R.string.device_scan_failed_message else R.string.device_empty_message
                    )
                )
                TextButton(onClick = onPickWithSystemPicker, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.device_picker))
                }
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(list, key = { it.uri.toString() }) { document ->
                    DeviceDocumentRow(document = document, onClick = { onOpenDocument(document) })
                }
                item {
                    TextButton(onClick = onPickWithSystemPicker, modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.device_picker))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessPrompt(onAllow: () -> Unit, onPicker: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.device_permission_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.device_permission_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.device_permission_settings_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAllow) { Text(stringResource(R.string.device_permission_allow)) }
        TextButton(onClick = onPicker, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.device_picker))
        }
    }
}

@Composable
private fun DeviceDocumentRow(document: DeviceDocument, onClick: () -> Unit) {
    val date = remember(document.dateModified) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(document.dateModified))
    }
    val megabytes = document.sizeBytes / (1024.0 * 1024.0)
    val size = if (megabytes < 1.0) {
        stringResource(R.string.device_size_kb, (document.sizeBytes / 1024L).coerceAtLeast(1L).toInt())
    } else {
        stringResource(R.string.value_megabytes, "%.1f".format(megabytes))
    }
    val meta = stringResource(R.string.device_document_meta, document.type.label, size, date)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = document.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = meta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
