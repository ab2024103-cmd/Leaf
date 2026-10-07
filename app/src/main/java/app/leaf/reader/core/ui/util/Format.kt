package app.leaf.reader.core.ui.util

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import app.leaf.reader.R
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.util.TimeAgo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reads a plural with format arguments (§10: "1 document" / "8 documents" come from
 * `<plurals>`, never from concatenation).
 */
@Composable
@ReadOnlyComposable
fun quantityText(@PluralsRes id: Int, quantity: Int, vararg args: Any): String =
    LocalContext.current.resources.getQuantityString(id, quantity, *args)

@Composable
@ReadOnlyComposable
fun stringText(@StringRes id: Int, vararg args: Any): String =
    LocalContext.current.getString(id, *args)

/** The short date the mockup falls back to once a timestamp is older than a week. */
@Composable
@ReadOnlyComposable
fun shortDate(millis: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))

@Composable
@ReadOnlyComposable
fun timeAgoText(ago: TimeAgo): String = when (ago) {
    TimeAgo.Never -> stringText(R.string.time_never)
    TimeAgo.JustNow -> stringText(R.string.time_just_now)
    is TimeAgo.Minutes -> quantityText(R.plurals.time_minutes, ago.value, ago.value)
    is TimeAgo.Hours -> quantityText(R.plurals.time_hours, ago.value, ago.value)
    is TimeAgo.Days -> quantityText(R.plurals.time_days, ago.value, ago.value)
    is TimeAgo.Date -> shortDate(ago.millis)
}

/** The §6.1 group titles: `PDF documents`, `Word · DOCX`, `Excel · XLSX` … */
@Composable
@ReadOnlyComposable
fun groupTitle(type: DocType): String = stringText(
    when (type) {
        DocType.PDF -> R.string.group_pdf
        DocType.DOCX -> R.string.group_docx
        DocType.XLSX -> R.string.group_xlsx
        DocType.PPTX -> R.string.group_pptx
        DocType.TXT -> R.string.group_txt
        DocType.EPUB -> R.string.group_epub
    }
)
