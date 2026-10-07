package app.leaf.reader.core.util

/**
 * The 10-swatch palette shared by folders and tags (LEAF-SPEC §2.1). New folders and
 * tags take the next swatch in sequence, exactly like the mockup's
 * `palette[count % palette.length]`.
 */
val LeafSwatches: List<String> = listOf(
    "#2B7A5B", "#3D6373", "#4A6FA5", "#7A6BB5", "#A04A6C",
    "#C2703B", "#B5793A", "#C94F3D", "#3F8F8A", "#587A2B"
)

fun swatchFor(index: Int): String = LeafSwatches[index.mod(LeafSwatches.size)]
