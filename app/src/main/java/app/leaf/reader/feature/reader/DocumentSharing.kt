package app.leaf.reader.feature.reader

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import app.leaf.reader.R
import app.leaf.reader.core.model.Document
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sends the original document to Android's share sheet. */
internal suspend fun shareDocument(
    context: Context,
    document: Document
) {
    val source = android.net.Uri.parse(document.uri)
    val shared = withContext(Dispatchers.IO) {
        when (source.scheme) {
            "content" -> source
            "file" -> {
                val file = source.path?.let(::File) ?: throw IllegalStateException("Missing file path")
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            else -> throw IllegalStateException("Unsupported file location")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = document.type.mime
        putExtra(Intent.EXTRA_STREAM, shared)
        clipData = ClipData.newUri(context.contentResolver, document.name, shared)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.reader_share_chooser))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
