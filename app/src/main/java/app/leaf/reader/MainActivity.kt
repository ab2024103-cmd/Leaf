package app.leaf.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                LeafShell(windowSizeClass = calculateWindowSizeClass(this))
            }
        }
    }
}
