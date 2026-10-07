package app.leaf.reader.core.util

import java.util.Locale

/**
 * §8.6 / mockup `renamePrompt`: renaming keeps the extension. Typing "Annual report" on
 * `Annual report 2024.pdf` keeps `.pdf`; typing a new extension of your own is respected.
 */
private val EXTENSION = Regex("\\.[a-z0-9]{2,4}$", RegexOption.IGNORE_CASE)
private val ANY_SUFFIX = Regex("\\.[^.]+$")

fun renameKeepingExtension(currentName: String, typedName: String): String {
    val typed = typedName.trim()
    if (typed.isEmpty()) return currentName
    if (EXTENSION.containsMatchIn(typed)) return typed
    val stem = typed.replace(ANY_SUFFIX, "")
    val extension = ANY_SUFFIX.find(currentName)?.value.orEmpty()
    return stem + extension
}

/** The mockup renders sizes as MB with two decimals (`doc.size.toFixed(2)`). */
fun formatMegabytes(bytes: Long): String =
    String.format(Locale.US, "%.2f", bytes / 1_048_576.0)

/**
 * Tags are stored lower-case (mockup `newTagPrompt`), so "Work" and "work" are one tag.
 */
fun normalizeTagName(name: String): String = name.trim().lowercase(Locale.getDefault())
