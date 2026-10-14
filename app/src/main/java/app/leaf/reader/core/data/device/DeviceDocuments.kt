package app.leaf.reader.core.data.device

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import app.leaf.reader.core.format.resolveDocType
import app.leaf.reader.core.model.DocType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One supported document found on the device by [scanDeviceDocuments]. */
data class DeviceDocument(
    /** A MediaStore content URI; the intake copies it into Leaf's private storage on open. */
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val dateModified: Long,
    val type: DocType
)

/**
 * Storage access for listing documents on the device. Android 11+ (API 30) uses the
 * All files access permission (MANAGE_EXTERNAL_STORAGE); earlier releases use the
 * READ_EXTERNAL_STORAGE runtime permission. Access is always requested by the user.
 */
object DeviceStorageAccess {

    fun isGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    /** The system page where the user turns on "Allow access to manage all files" for Leaf. */
    fun allFilesSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))

    /** Fallback page when the per-app All files access screen is not available on the device. */
    fun allFilesSettingsFallbackIntent(): Intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)

    /** The runtime permission used before API 30. */
    const val READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
}

private val SUPPORTED_EXTENSIONS = listOf("pdf", "epub", "docx", "xlsx", "pptx", "txt")

/**
 * Lists every supported document on shared storage, newest first. Requires
 * [DeviceStorageAccess.isGranted]; throws SecurityException otherwise.
 */
suspend fun scanDeviceDocuments(context: Context): List<DeviceDocument> = withContext(Dispatchers.IO) {
    val collection = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.DATE_MODIFIED,
        MediaStore.MediaColumns.MIME_TYPE
    )
    val mimes = DocType.entries.map { it.mime }
    // Some providers report generic MIME types, so the file name extension is matched too.
    val selection = "${MediaStore.MediaColumns.MIME_TYPE} IN (${mimes.joinToString { "?" }}) OR " +
        SUPPORTED_EXTENSIONS.joinToString(" OR ") { "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?" }
    val args = (mimes + SUPPORTED_EXTENSIONS.map { "%.$it" }).toTypedArray()
    val found = mutableListOf<DeviceDocument>()
    context.contentResolver.query(
        collection,
        projection,
        selection,
        args,
        "${MediaStore.MediaColumns.DISPLAY_NAME} COLLATE NOCASE"
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
        val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        while (cursor.moveToNext()) {
            val name = cursor.getString(nameColumn) ?: continue
            val type = resolveDocType(cursor.getString(mimeColumn), name) ?: continue
            found += DeviceDocument(
                uri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn)),
                name = name,
                sizeBytes = cursor.getLong(sizeColumn).coerceAtLeast(0L),
                dateModified = cursor.getLong(dateColumn) * 1000L,
                type = type
            )
        }
    }
    found
}
