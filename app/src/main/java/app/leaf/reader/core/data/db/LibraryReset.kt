package app.leaf.reader.core.data.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Settings → Data & storage → Reset library (§6.5): removes every document, folder, tag,
 * collection and annotation so the user can start again with their own files. Leaf ships
 * no built-in documents. Settings-level keys in DataStore are not touched by this.
 */
class LibraryReset(private val db: LeafDatabase) {
    private val mutex = Mutex()

    suspend fun reset() = mutex.withLock {
        // Room's clearAllTables() is blocking and must stay off the main thread.
        withContext(Dispatchers.IO) { db.clearAllTables() }
    }
}
