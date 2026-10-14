package app.leaf.reader.core.format

import android.content.Context
import android.net.Uri
import app.leaf.reader.core.model.DocType
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Selects one real format engine from the library's persisted type, MIME type or filename. */
class MultiFormatDocumentEngineFactory(
    context: Context,
    bitmaps: PdfBitmapCache
) : DocumentEngineFactory {
    private val appContext = context.applicationContext
    private val pdf = PdfDocumentEngineFactory(appContext, bitmaps)
    private val epub = EpubDocumentEngineFactory(appContext)

    override suspend fun open(uri: String): DocumentEngine = open(uri, resolveType(uri))

    override suspend fun open(uri: String, type: DocType): DocumentEngine {
        if (type == DocType.PDF) return pdf.open(uri)
        if (type == DocType.EPUB) return epub.open(uri)

        return withContext(Dispatchers.IO) {
            val prepared = materialize(uri, type)
            try {
                when (type) {
                    DocType.TXT -> {
                        val source = prepared.file.readText(Charsets.UTF_8)
                        NativeDocumentEngine.text(type, source, "Page", prepared.onClose)
                    }
                    DocType.DOCX -> NativeDocumentEngine.docx(
                        OoxmlDocumentParser.parseDocx(prepared.file),
                        prepared.onClose
                    )
                    DocType.XLSX -> NativeDocumentEngine.xlsx(
                        OoxmlDocumentParser.parseXlsx(prepared.file),
                        prepared.onClose
                    )
                    DocType.PPTX -> NativeDocumentEngine.pptx(
                        OoxmlDocumentParser.parsePptx(prepared.file),
                        prepared.onClose
                    )
                    DocType.PDF, DocType.EPUB -> error("Handled before native OOXML dispatch")
                }
            } catch (error: Throwable) {
                prepared.onClose()
                throw error
            }
        }
    }

    private suspend fun resolveType(uri: String): DocType {
        val parsed = Uri.parse(uri)
        val mimeType = if (parsed.scheme == "content") appContext.contentResolver.getType(parsed) else null
        val displayName = parsed.lastPathSegment?.substringAfterLast('/')
        return resolveDocType(mimeType, displayName)
            ?: throw IOException("Leaf could not determine the document format")
    }

    private fun materialize(uri: String, type: DocType): PreparedFormatFile {
        val parsed = Uri.parse(uri)
        if (parsed.scheme == "file") {
            val file = parsed.path?.let(::File) ?: throw IOException("The document path is missing")
            return PreparedFormatFile(file) {}
        }
        if (parsed.scheme == "content") {
            val directory = File(appContext.cacheDir, "format-open").apply { mkdirs() }
            val target = File(directory, "${UUID.randomUUID()}.${type.extension}")
            val input = appContext.contentResolver.openInputStream(parsed)
                ?: throw IOException("The document could not be opened")
            try {
                input.use { source -> target.outputStream().use(source::copyTo) }
            } catch (error: Throwable) {
                target.delete()
                throw error
            }
            return PreparedFormatFile(target) { target.delete() }
        }
        throw IOException("Unsupported document URI")
    }

    private data class PreparedFormatFile(val file: File, val onClose: () -> Unit)
}

/** Returns the six formats Leaf renders, using MIME first and a suffix as the fallback. */
internal fun resolveDocType(mimeType: String?, displayName: String?): DocType? {
    val mime = mimeType?.substringBefore(';')?.trim()?.lowercase()
    val byMime = DocType.entries.firstOrNull { it.mime.equals(mime, ignoreCase = true) }
    if (byMime != null) return byMime
    val extension = displayName?.substringAfterLast('.', "")?.lowercase()
    return when (extension) {
        "pdf" -> DocType.PDF
        "docx" -> DocType.DOCX
        "xlsx" -> DocType.XLSX
        "pptx" -> DocType.PPTX
        "txt" -> DocType.TXT
        "epub" -> DocType.EPUB
        else -> null
    }
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
