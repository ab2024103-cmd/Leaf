package app.leaf.reader.screenshots

import android.app.Application
import app.leaf.reader.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * A Koin graph for the Library screenshot test without LeafApp's production seeder.
 * The test seeds synchronously at FIXED_NOW so both Roborazzi record and verify runs
 * see the exact same timestamps.
 */
class LibraryScreenshotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LibraryScreenshotApplication)
            modules(appModule)
        }
    }
}
