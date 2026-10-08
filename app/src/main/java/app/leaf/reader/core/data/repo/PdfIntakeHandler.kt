package app.leaf.reader.core.data.repo

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import app.leaf.reader.core.data.db.DatabaseSeeder
import app.leaf.reader.core.format.DocumentEngineFactory
import app.leaf.reader.core.util.LeafClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/** Handles only real incoming PDFs from Android's Open with / Share intents in M3. */
class PdfIntakeHandler(
    context: Context,
    private val seeder: DatabaseSeeder,
    private val repository: ReaderRepository,
    private val engines: DocumentEngineFactory,
    private val clock: LeafClock
) {
    private val appContext = context.applicationContext

    suspend fun receive(intent: Intent): String? = withContext(Dispatchers.IO) {
        val uri = intent.pdfUri() ?: return@withContext null
        val mimeType = intent.type ?: appContext.contentResolver.getType(uri)
        if (mimeType != PDF_MIME && appContext.contentResolver.getType(uri) != PDF_MIME) {
            throw IOException("Leaf M3 accepts PDF documents only")
        }

        // Finish the seed transaction first; an incoming file must not prevent demo data from seeding.
        seeder.seedIfEmpty(clock.nowMillis())
        val displayName = displayName(uri).let { name ->
            if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
        }
        val originalSize = size(uri)
        val persistedUri = makeDurableUri(uri, intent)
        val engine = engines.open(persistedUri)
        val pages = try {
            engine.pageCount
        } finally {
            engine.close()
        }
        if (pages <= 0) throw IOException("The selected PDF has no readable pages")
        val sizeBytes = if (persistedUri.startsWith("file:")) {
            Uri.parse(persistedUri).path?.let(::File)?.length() ?: originalSize
        } else {
            originalSize
        }
        repository.registerPdf(persistedUri, displayName, sizeBytes, pages, clock.nowMillis())
    }

    private fun Intent.pdfUri(): Uri? = when (action) {
        Intent.ACTION_VIEW -> data
        Intent.ACTION_SEND -> parcelableStream()
        else -> null
    }

    @Suppress("DEPRECATION")
    private fun Intent.parcelableStream(): Uri? {
        val stream = if (android.os.Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        }
        return stream ?: clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    }

    private suspend fun makeDurableUri(uri: Uri, intent: Intent): String {
        if (uri.scheme == "content") {
            val accessFlags = intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val hasPersistableGrant = intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0
            if (hasPersistableGrant && accessFlags != 0) {
                try {
                    appContext.contentResolver.takePersistableUriPermission(uri, accessFlags)
                    return uri.toString()
                } catch (_: SecurityException) {
                    // A share provider may advertise a persistable grant it cannot retain; copy only then.
                }
            }
            val input = appContext.contentResolver.openInputStream(uri)
                ?: throw IOException("The shared PDF could not be opened")
            return copyToPrivateStorage(input)
        }
        if (uri.scheme == "file") {
            val source = uri.path?.let(::File) ?: throw IOException("The shared PDF path is missing")
            return copyToPrivateStorage(source.inputStream())
        }
        throw IOException("Unsupported PDF URI")
    }

    private fun copyToPrivateStorage(input: InputStream): String {
        val directory = File(appContext.filesDir, "incoming-pdf").apply { mkdirs() }
        val destination = File(directory, "${UUID.randomUUID()}.pdf")
        val temp = File(directory, "${destination.name}.tmp")
        try {
            input.use { source ->
                temp.outputStream().use { output -> source.copyTo(output) }
            }
        } catch (error: Throwable) {
            temp.delete()
            throw error
        }
        if (!temp.renameTo(destination)) {
            temp.delete()
            throw IOException("The shared PDF could not be saved for later reading")
        }
        return Uri.fromFile(destination).toString()
    }

    private fun displayName(uri: Uri): String {
        val fromProvider = query(uri) { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
        return fromProvider?.takeIf(String::isNotBlank)
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
            ?: "Document.pdf"
    }

    private fun size(uri: Uri): Long = query(uri) { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (index >= 0 && cursor.moveToFirst()) cursor.getLong(index).coerceAtLeast(0L) else 0L
    } ?: 0L

    private inline fun <T> query(uri: Uri, read: (Cursor) -> T): T? =
        runCatching {
            appContext.contentResolver.query(uri, null, null, null, null)?.use(read)
        }.getOrNull()

    private companion object {
        const val PDF_MIME = "application/pdf"
    }
}
