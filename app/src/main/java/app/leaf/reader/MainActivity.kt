package app.leaf.reader

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.leaf.reader.core.data.repo.PdfIntakeHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.ui.theme.LeafTheme
import app.leaf.reader.navigation.LeafShell
import org.koin.android.ext.android.inject

/**
 * The single activity: edge-to-edge, never re-created by the framework on rotation or
 * resizing (`configChanges` in the manifest) because the shell re-flows in Compose (§2).
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {

    private val settingsStore: SettingsStore by inject()
    private val pdfIntakeHandler: PdfIntakeHandler by inject()
    private var externalDocumentId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsStore.settings.collectAsStateWithLifecycle(
                initialValue = LeafSettings()
            )
            LeafTheme(
                appTheme = settings.appTheme,
                dynamicColor = settings.matchSystemColors,
                readingTheme = settings.readingTheme
            ) {
                LeafShell(
                    windowSizeClass = calculateWindowSizeClass(this),
                    externalDocumentId = externalDocumentId,
                    onExternalDocumentConsumed = { externalDocumentId = null }
                )
            }
        }
        handleExternalIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalIntent(intent)
    }

    private fun handleExternalIntent(incoming: Intent?) {
        if (incoming?.action != Intent.ACTION_VIEW && incoming?.action != Intent.ACTION_SEND) return
        lifecycleScope.launch {
            try {
                val documentId = pdfIntakeHandler.receive(incoming)
                if (documentId != null) externalDocumentId = documentId
                else Toast.makeText(this@MainActivity, R.string.pdf_intake_missing, Toast.LENGTH_SHORT).show()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val message = error.message?.takeIf(String::isNotBlank)
                    ?: getString(R.string.pdf_intake_failed)
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
