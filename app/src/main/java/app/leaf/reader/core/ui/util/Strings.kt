package app.leaf.reader.core.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle

/**
 * Renders the `<i>…</i>` emphasis used in the mockup's copy deck as real italics.
 * Copy is authored in strings.xml, never concatenated in code (§1.10).
 */
fun annotated(text: CharSequence): AnnotatedString {
    val raw = text.toString()
    if (!raw.contains("<i>")) return AnnotatedString(raw)
    return buildAnnotatedString {
        val pattern = Regex("<i>(.*?)</i>", RegexOption.DOT_MATCHES_ALL)
        var cursor = 0
        pattern.findAll(raw).forEach { match ->
            append(raw.substring(cursor, match.range.first))
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(match.groupValues[1]) }
            cursor = match.range.last + 1
        }
        append(raw.substring(cursor))
    }
}
