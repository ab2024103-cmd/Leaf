package app.leaf.reader

import android.app.Application
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.format.PdfBitmapCache
import app.leaf.reader.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

/**
 * Leaf application: builds the graph, migrates settings and seeds the demo library.
 *
 * No WorkManager and no network permission — indexing runs on IO coroutines scoped to
 * the process and simply re-runs on the next launch if it is interrupted (§9).
 */
class LeafApp : Application(), KoinComponent {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val seeder: DatabaseSeeder by inject()
    private val settings: SettingsStore by inject()
    private val pdfBitmapCache: PdfBitmapCache by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@LeafApp)
            modules(appModule)
        }
        registerComponentCallbacks(pdfBitmapCache)
        appScope.launch {
            // Idempotent: the seeder no-ops once the library is populated.
            settings.migrateLegacySortField()
            seeder.seedIfEmpty()
        }
    }
}
