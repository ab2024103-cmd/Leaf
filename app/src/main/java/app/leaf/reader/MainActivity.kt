package app.leaf.reader

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.leaf.reader.core.data.repo.DocumentIntakeHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.ui.theme.LeafPreferences
import app.leaf.reader.core.ui.theme.LeafTheme
import app.leaf.reader.core.util.LeafLocale
import android.content.Context
import app.leaf.reader.navigation.LeafShell
import org.koin.android.ext.android.inject

/**
 * The single activity: edge-to-edge, never re-created by the framework on rotation or
 * resizing (`configChanges` in the manifest) because the shell re-flows in Compose (§2).
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : FragmentActivity() {

    private val settingsStore: SettingsStore by inject()
    private val documentIntakeHandler: DocumentIntakeHandler by inject()
    private var externalDocumentId by mutableStateOf<String?>(null)

    /** The saved interface language is applied before any string is resolved (§10). */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LeafLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsStore.settings.collectAsStateWithLifecycle(
                initialValue = LeafSettings()
            )
            LeafPreferences(settings = settings) {
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
        }
        handleExternalIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalIntent(intent)
    }

    private fun handleExternalIntent(incoming: Intent?) {
        val received = incoming ?: return
        if (received.action != Intent.ACTION_VIEW && received.action != Intent.ACTION_SEND) return
        if (received.action == Intent.ACTION_VIEW && received.data == null) return
        if (received.action == Intent.ACTION_SEND && !received.hasExtra(Intent.EXTRA_STREAM) && received.clipData == null) return
        lifecycleScope.launch {
            try {
                val documentId = documentIntakeHandler.receive(received)
                if (documentId != null) externalDocumentId = documentId
                else Toast.makeText(this@MainActivity, R.string.document_intake_missing, Toast.LENGTH_SHORT).show()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val message = error.message?.takeIf(String::isNotBlank)
                    ?: getString(R.string.document_intake_failed)
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
