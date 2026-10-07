package app.leaf.reader.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.data.db.LeafDatabase
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.data.repo.DocumentRepository
import app.leaf.reader.core.data.repo.FolderRepository
import app.leaf.reader.core.data.repo.RecentRepository
import app.leaf.reader.core.data.repo.SmartCollectionRepository
import app.leaf.reader.core.data.repo.TagRepository
import app.leaf.reader.core.util.LeafClock
import app.leaf.reader.core.util.SystemLeafClock
import app.leaf.reader.feature.library.LibraryViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/** Leaf's graph: database → DAOs → repositories, plus settings (§3). */
val appModule = module {
    single {
        Room.databaseBuilder(androidContext(), LeafDatabase::class.java, "leaf.db")
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    single { get<LeafDatabase>().documentDao() }
    single { get<LeafDatabase>().documentTagDao() }
    single { get<LeafDatabase>().folderDao() }
    single { get<LeafDatabase>().tagDao() }
    single { get<LeafDatabase>().smartCollectionDao() }
    single { get<LeafDatabase>().recentDao() }
    single { get<LeafDatabase>().bookmarkDao() }
    single { get<LeafDatabase>().highlightDao() }
    single { get<LeafDatabase>().extractedTextDao() }
    single { get<LeafDatabase>().progressDao() }

    singleOf(::DocumentRepository)
    singleOf(::FolderRepository)
    singleOf(::TagRepository)
    singleOf(::SmartCollectionRepository)
    singleOf(::RecentRepository)

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create {
            androidContext().preferencesDataStoreFile("leaf_settings.preferences_pb")
        }
    }
    singleOf(::SettingsStore)
    singleOf(::DatabaseSeeder)
    single<LeafClock> { SystemLeafClock }

    // AndroidViewModel: the application is needed for the resource strings the
    // snackbars, subtitles and headers are built from (§1.10).
    viewModel { LibraryViewModel(androidApplication(), get(), get(), get(), get(), get(), get()) }
}
