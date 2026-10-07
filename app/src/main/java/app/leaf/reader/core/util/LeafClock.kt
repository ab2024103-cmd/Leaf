package app.leaf.reader.core.util

/** The clock used to resolve relative dates and age-based collection rules. */
fun interface LeafClock {
    fun nowMillis(): Long
}

/** Production wall clock; screenshot tests replace this with a fixed instant. */
object SystemLeafClock : LeafClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
