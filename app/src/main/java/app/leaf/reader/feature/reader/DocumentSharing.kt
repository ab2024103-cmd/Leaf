package app.leaf.reader.feature.reader

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import app.leaf.reader.R
import app.leaf.reader.core.format.DocumentEngine
import app.leaf.reader.core.format.DocumentEngineFactory
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Document
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sends the original document to Android's share sheet, materializing seeded demo documents. */
internal suspend fun shareDocument(
    context: Context,
    document: Document,
    currentEngine: DocumentEngine? = null,
    engineFactory: DocumentEngineFactory? = null
) {
    val source = Uri.parse(document.uri)
    var exportEngine = currentEngine
    var ownsEngine = false
    try {
        if (source.scheme == "leaf-demo" && document.type != DocType.PDF && exportEngine == null) {
            exportEngine = engineFactory?.open(document.uri, document.type)
                ?: throw IllegalStateException("The sample document could not be opened")
            ownsEngine = true
        }
        val shared = withContext(Dispatchers.IO) {
            when (source.scheme) {
                "content" -> source
                "file" -> {
                    val file = source.path?.let(::File) ?: throw IllegalStateException("Missing file path")
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }
                "leaf-demo" -> {
                    val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
                    if (document.type == DocType.PDF) {
                        val id = source.lastPathSegment?.takeIf { it.matches(Regex("d[0-9]+")) }
                            ?: throw IllegalStateException("Unknown sample PDF")
                        val file = File(shareDir, "$id.pdf")
                        context.assets.open("demo-pdf/$id.pdf").use { input ->
                            file.outputStream().use(input::copyTo)
                        }
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    } else {
                        val file = File(shareDir, "${document.id}.txt")
                        val pages = exportEngine?.extractAllText { _, _ -> }
                            ?: throw IllegalStateException("The sample document has no text")
                        file.writeText(
                            pages.joinToString("\n\n") { page ->
                                page.joinToString("\n") { it.text }
                            },
                            Charsets.UTF_8
                        )
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    }
                }
                else -> throw IllegalStateException("Unsupported file location")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (source.scheme == "leaf-demo" && document.type != DocType.PDF) {
                "text/plain"
            } else {
                document.type.mime
            }
            putExtra(Intent.EXTRA_STREAM, shared)
            clipData = ClipData.newUri(context.contentResolver, document.name, shared)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.reader_share_chooser))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } finally {
        if (ownsEngine) withContext(Dispatchers.IO) { exportEngine?.close() }
    }
}
