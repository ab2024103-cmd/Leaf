package app.leaf.reader

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.leaf.reader.core.data.db.LeafDatabase

/** An in-memory database for JVM unit tests (Robolectric supplies SQLite). */
internal fun testDatabase(): LeafDatabase {
    val context: Context = ApplicationProvider.getApplicationContext()
    return Room.inMemoryDatabaseBuilder(context, LeafDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}
