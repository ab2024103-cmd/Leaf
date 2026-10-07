package app.leaf.reader

import app.leaf.reader.core.util.TimeAgo
import app.leaf.reader.core.util.timeAgo
import org.junit.Assert.assertEquals
import org.junit.Test

/** The mockup's `timeAgo` boundaries, exactly. */
class TimeAgoTest {

    private val now = FIXED_NOW
    private fun ago(millis: Long) = timeAgo(now - millis, now)

    @Test
    fun a_missing_timestamp_is_never() {
        assertEquals(TimeAgo.Never, timeAgo(null, now))
        assertEquals(TimeAgo.Never, timeAgo(0L, now))
    }

    @Test
    fun under_a_minute_is_just_now() {
        assertEquals(TimeAgo.JustNow, ago(0))
        assertEquals(TimeAgo.JustNow, ago(59_000))
    }

    @Test
    fun minutes_start_at_sixty_seconds() {
        assertEquals(TimeAgo.Minutes(1), ago(60_000))
        assertEquals(TimeAgo.Minutes(59), ago(59 * 60_000))
    }

    @Test
    fun hours_start_at_sixty_minutes() {
        assertEquals(TimeAgo.Hours(1), ago(60 * 60_000))
        assertEquals(TimeAgo.Hours(23), ago(23 * 60 * 60_000))
    }

    @Test
    fun days_start_at_twenty_four_hours() {
        assertEquals(TimeAgo.Days(1), ago(24 * 60 * 60_000))
        assertEquals(TimeAgo.Days(6), ago(6 * 24 * 60 * 60_000))
    }

    @Test
    fun a_week_or_more_is_a_date() {
        assertEquals(TimeAgo.Date(now - 7 * 24 * 60 * 60_000), ago(7 * 24 * 60 * 60_000))
        assertEquals(TimeAgo.Date(now - 400L * 24 * 60 * 60_000), ago(400L * 24 * 60 * 60_000))
    }

    @Test
    fun a_timestamp_in_the_future_clamps_to_just_now() {
        assertEquals(TimeAgo.JustNow, timeAgo(now + 5_000, now))
    }
}
