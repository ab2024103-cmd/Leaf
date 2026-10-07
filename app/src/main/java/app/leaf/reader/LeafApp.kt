package app.leaf.reader

import android.app.Application
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

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

    override fun onCreate() {
        super.onCreate()
        // Robolectric builds a fresh Application per test in the same JVM, so the graph
        // left behind by the previous one has to be dropped before this instance binds
        // its own context. In the app itself there is only ever one process and one call.
        if (GlobalContext.getOrNull() != null) stopKoin()
        startKoin {
            androidLogger()
            androidContext(this@LeafApp)
            modules(appModule)
        }
        appScope.launch {
            // Idempotent: the seeder no-ops once the library is populated.
            settings.migrateLegacySortField()
            seeder.seedIfEmpty()
        }
    }
}
