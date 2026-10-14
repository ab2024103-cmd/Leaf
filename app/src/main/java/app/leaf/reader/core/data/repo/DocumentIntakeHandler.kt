package app.leaf.reader.core.data.repo

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import app.leaf.reader.core.data.device.DeviceDocument
import app.leaf.reader.core.format.DocumentEngineFactory
import app.leaf.reader.core.format.resolveDocType
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.util.LeafClock
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.CodingErrorAction
import java.util.UUID
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Retains supported open-with/share documents in the library; this is not an import-screen flow. */
class DocumentIntakeHandler(
    context: Context,
    private val repository: ReaderRepository,
    private val engines: DocumentEngineFactory,
    private val clock: LeafClock
) {
    private val appContext = context.applicationContext

    suspend fun receive(intent: Intent): String? = withContext(Dispatchers.IO) {
        val uri = intent.documentUri() ?: return@withContext null
        val displayName = displayName(uri)
        val mimeType = intent.type ?: appContext.contentResolver.getType(uri)
        val type = resolveDocType(mimeType, displayName) ?: detectType(uri)
            ?: throw IOException("Leaf could not determine the document format")
        val name = displayName.withExtension(type)

        val sourceSize = size(uri)
        val persistedUri = makeDurableUri(uri, intent, type)
        val engine = engines.open(persistedUri, type)
        val pages = try {
            engine.pageCount
        } finally {
            engine.close()
        }
        if (pages <= 0) throw IOException("The selected document has no readable pages")
        val persistedFileSize = Uri.parse(persistedUri).takeIf { it.scheme == "file" }
            ?.path?.let(::File)?.length()
        val sizeBytes = persistedFileSize?.takeIf { it > 0L } ?: sourceSize
        repository.registerDocument(persistedUri, name, type, sizeBytes, pages, clock.nowMillis())
    }

    /**
     * Imports a file the user picked from the device list. A file already in the library
     * (same name and size) is reopened instead of being copied again.
     */
    suspend fun receiveDevice(document: DeviceDocument): String? {
        repository.findDocumentIdByNameAndSize(document.name, document.sizeBytes)?.let { return it }
        val intent = Intent(Intent.ACTION_VIEW, document.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return receive(intent)
    }

    private fun Intent.documentUri(): Uri? = when (action) {
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

    private fun makeDurableUri(uri: Uri, intent: Intent, type: DocType): String {
        if (uri.scheme == "content") {
            val hasReadGrant = intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
            val hasPersistableGrant = intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0
            if (hasPersistableGrant && hasReadGrant) {
                try {
                    appContext.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    return uri.toString()
                } catch (_: SecurityException) {
                    // Some share providers cannot retain grants; copy those documents privately.
                }
            }
            val input = appContext.contentResolver.openInputStream(uri)
                ?: throw IOException("The shared document could not be opened")
            return copyToPrivateStorage(input, type)
        }
        if (uri.scheme == "file") {
            val source = uri.path?.let(::File) ?: throw IOException("The shared file path is missing")
            return copyToPrivateStorage(source.inputStream(), type)
        }
        throw IOException("Unsupported document URI")
    }

    private fun copyToPrivateStorage(input: InputStream, type: DocType): String {
        val directory = File(appContext.filesDir, "incoming-documents").apply { mkdirs() }
        val destination = File(directory, "${UUID.randomUUID()}.${type.extension}")
        val temporary = File(directory, "${destination.name}.tmp")
        try {
            input.use { source -> temporary.outputStream().use { output -> source.copyTo(output) } }
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete()
            throw IOException("The shared document could not be saved for later reading")
        }
        return Uri.fromFile(destination).toString()
    }

    private fun detectType(uri: Uri): DocType? {
        if (uri.scheme == "file") {
            val file = uri.path?.let(::File) ?: return null
            return detectFileType(file)
        }
        if (uri.scheme != "content") return null
        val sample = ByteArray(8192)
        val count = appContext.contentResolver.openInputStream(uri)?.use { it.read(sample) } ?: return null
        if (count >= PDF_MAGIC.size && sample.copyOfRange(0, PDF_MAGIC.size).contentEquals(PDF_MAGIC)) {
            return DocType.PDF
        }
        if (count >= ZIP_MAGIC.size && sample.copyOfRange(0, ZIP_MAGIC.size).contentEquals(ZIP_MAGIC)) {
            val probe = File(appContext.cacheDir, "format-probe-${UUID.randomUUID()}.zip")
            try {
                appContext.contentResolver.openInputStream(uri)?.use { input -> probe.outputStream().use(input::copyTo) }
                return detectFileType(probe)
            } finally {
                probe.delete()
            }
        }
        val decoded = runCatching {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(sample, 0, count)).toString()
        }.getOrNull()
        return decoded?.takeIf { it.isReadableTextSample() }?.let { DocType.TXT }
    }

    private fun detectFileType(file: File): DocType? {
        if (!file.isFile) return null
        if (file.inputStream().use { input ->
                val magic = ByteArray(PDF_MAGIC.size)
                input.read(magic) == magic.size && magic.contentEquals(PDF_MAGIC)
            }) return DocType.PDF
        return runCatching {
            ZipFile(file).use { zip ->
                val entries = zip.entries().asSequence().map { it.name }.toSet()
                val mimetypeEntry = zip.getEntry("mimetype")
                when {
                    "word/document.xml" in entries -> DocType.DOCX
                    "xl/workbook.xml" in entries -> DocType.XLSX
                    "ppt/presentation.xml" in entries -> DocType.PPTX
                    mimetypeEntry != null && zip.getInputStream(mimetypeEntry).bufferedReader().use { it.readText().trim() } == EPUB_MIME -> DocType.EPUB
                    else -> null
                }
            }
        }.getOrNull() ?: runCatching {
            file.inputStream().use { input ->
                val sample = ByteArray(8192)
                val count = input.read(sample).coerceAtLeast(0)
                val text = Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(sample, 0, count)).toString()
                text.takeIf { it.isReadableTextSample() }?.let { DocType.TXT }
            }
        }.getOrNull()
    }

    private fun String.isReadableTextSample(): Boolean = all { character ->
        !character.isISOControl() || character == '\n' || character == '\r' || character == '\t'
    }

    private fun displayName(uri: Uri): String {
        val fromProvider = query(uri) { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
        return fromProvider?.takeIf(String::isNotBlank)
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
            ?: "Document"
    }

    private fun size(uri: Uri): Long = query(uri) { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (index >= 0 && cursor.moveToFirst()) cursor.getLong(index).coerceAtLeast(0L) else 0L
    } ?: 0L

    private inline fun <T> query(uri: Uri, read: (Cursor) -> T): T? =
        runCatching { appContext.contentResolver.query(uri, null, null, null, null)?.use(read) }.getOrNull()

    private fun String.withExtension(type: DocType): String {
        val extension = substringAfterLast('.', "").lowercase()
        if (extension == type.extension) return this
        val stem = if (extension.isNotEmpty()) substringBeforeLast('.') else this
        return "${stem.ifBlank { "Document" }}.${type.extension}"
    }

    private val DocType.extension: String
        get() = when (this) {
            DocType.PDF -> "pdf"
            DocType.DOCX -> "docx"
            DocType.XLSX -> "xlsx"
            DocType.PPTX -> "pptx"
            DocType.TXT -> "txt"
            DocType.EPUB -> "epub"
        }

    private companion object {
        val PDF_MAGIC = "%PDF-".toByteArray(Charsets.US_ASCII)
        val ZIP_MAGIC = byteArrayOf(0x50, 0x4b, 0x03, 0x04)
        const val EPUB_MIME = "application/epub+zip"
    }
}
