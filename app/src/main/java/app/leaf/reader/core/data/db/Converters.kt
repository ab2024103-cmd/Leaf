package app.leaf.reader.core.data.db

import androidx.room.TypeConverter
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.HighlightColor
import app.leaf.reader.core.model.NormalizedRect
import app.leaf.reader.core.model.Orientation
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir

/**
 * Room converters. Enums are stored by name; normalized rects are stored as
 * "left,top,right,bottom" groups joined by ';' so no JSON dependency is needed.
 */
class Converters {

    @TypeConverter
    fun docType(value: String?): DocType? = value?.let { DocType.valueOf(it) }

    @TypeConverter
    fun docType(value: DocType?): String? = value?.name

    @TypeConverter
    fun orientation(value: String?): Orientation? = value?.let { Orientation.valueOf(it) }

    @TypeConverter
    fun orientation(value: Orientation?): String? = value?.name

    @TypeConverter
    fun readingTheme(value: String?): ReadingTheme? = value?.let { ReadingTheme.valueOf(it) }

    @TypeConverter
    fun readingTheme(value: ReadingTheme?): String? = value?.name

    @TypeConverter
    fun scrollDir(value: String?): ScrollDir? = value?.let { ScrollDir.valueOf(it) }

    @TypeConverter
    fun scrollDir(value: ScrollDir?): String? = value?.name

    @TypeConverter
    fun highlightColor(value: String?): HighlightColor? = value?.let { HighlightColor.valueOf(it) }

    @TypeConverter
    fun highlightColor(value: HighlightColor?): String? = value?.name

    @TypeConverter
    fun rects(value: String?): List<NormalizedRect> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(RECT_SEPARATOR).mapNotNull { rect ->
            val parts = rect.split(FIELD_SEPARATOR)
            if (parts.size == 4) {
                NormalizedRect(
                    left = parts[0].toFloatOrNull() ?: return@mapNotNull null,
                    top = parts[1].toFloatOrNull() ?: return@mapNotNull null,
                    right = parts[2].toFloatOrNull() ?: return@mapNotNull null,
                    bottom = parts[3].toFloatOrNull() ?: return@mapNotNull null
                )
            } else {
                null
            }
        }
    }

    @TypeConverter
    fun rects(value: List<NormalizedRect>?): String =
        value.orEmpty().joinToString(RECT_SEPARATOR) {
            listOf(it.left, it.top, it.right, it.bottom).joinToString(FIELD_SEPARATOR)
        }

    private companion object {
        const val RECT_SEPARATOR = ";"
        const val FIELD_SEPARATOR = ","
    }
}
