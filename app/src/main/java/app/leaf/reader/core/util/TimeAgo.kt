package app.leaf.reader.core.util

/**
 * A relative timestamp, resolved but not yet turned into text. The UI maps each case to
 * its plural resource so nothing is concatenated in code (§1.10, §10).
 *
 * Boundaries are the mockup's `timeAgo`: < 60 s · "just now", < 1 h · minutes,
 * < 24 h · hours, < 7 days · days, older · a short date.
 */
sealed interface TimeAgo {
    /** No timestamp at all (never opened). */
    data object Never : TimeAgo
    data object JustNow : TimeAgo
    data class Minutes(val value: Int) : TimeAgo
    data class Hours(val value: Int) : TimeAgo
    data class Days(val value: Int) : TimeAgo
    /** Older than a week: the UI formats [millis] as a short date. */
    data class Date(val millis: Long) : TimeAgo
}

private const val SECOND = 1_000L
private const val MINUTE = 60 * SECOND
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR
private const val WEEK = 7 * DAY

fun timeAgo(timestamp: Long?, now: Long): TimeAgo {
    if (timestamp == null || timestamp <= 0L) return TimeAgo.Never
    val elapsed = (now - timestamp).coerceAtLeast(0L)
    return when {
        elapsed < MINUTE -> TimeAgo.JustNow
        elapsed < HOUR -> TimeAgo.Minutes((elapsed / MINUTE).toInt())
        elapsed < DAY -> TimeAgo.Hours((elapsed / HOUR).toInt())
        elapsed < WEEK -> TimeAgo.Days((elapsed / DAY).toInt())
        else -> TimeAgo.Date(timestamp)
    }
}
