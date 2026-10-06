package app.leaf.reader.core.ui.theme

import androidx.compose.ui.unit.dp

/** Shape scale — LEAF-MASTER-PROMPT.md §4.5. */
object LeafShape {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val full = 999.dp
}

/** Spacing scale — §4.5. Touch targets are never below 48 dp. */
object LeafSpacing {
    /** Screen side padding. */
    val screenH = 16.dp
    /** Gap between cards. */
    val listGap = 6.dp
    /** Inner padding of a card / list row. */
    val cardPad = 12.dp
    /** Icon-button visual size. */
    val control = 40.dp
    /** Minimum touch target. */
    val touch = 48.dp
    /** Extra bottom padding so lists scroll clear of the extended FAB. */
    val fabClearance = 92.dp
}

/** Component metrics — §4.5 and §4.6. */
object LeafMetrics {
    /** Cards / list rows / document tiles. */
    val cardRadius = 16.dp
    /** Sheets (top corners), dialogs. */
    val sheetRadius = 28.dp
    /** Filter chips. */
    val chipRadius = 8.dp
    /** Chip height (mini chips are 28 dp). */
    val chipHeight = 32.dp
    val chipHeightMini = 28.dp
    /** Search bar height, `surfaceContainerHigh`. */
    val searchBarHeight = 52.dp
    /** Navigation-bar active pill. */
    val navPillWidth = 62.dp
    val navPillHeight = 30.dp
    /** Navigation rail width. */
    val railWidth = 84.dp
    val railPillWidth = 56.dp
    val railPillHeight = 32.dp
    /** Dialog max width. */
    val dialogMaxWidth = 340.dp
    /** Snackbar: 4 dp radius, 16 dp side margins, 14 dp above the navigation bar. */
    val snackbarRadius = 4.dp
    val snackbarSide = 16.dp
    val snackbarGap = 14.dp
    /** Sheet grabber 32 × 4 dp at 40 % opacity; sheets cap at 82 % of the height. */
    val grabberWidth = 32.dp
    val grabberHeight = 4.dp
    val grabberAlpha = 0.40f
    val sheetMaxHeightFraction = 0.82f
    val scrimAlpha = 0.42f
    /** Document row: 11 dp vertical padding, 12 dp icon gap, 40 × 48 dp icon. */
    val rowVerticalPadding = 11.dp
    val rowIconGap = 12.dp
    val fileIconWidth = 40.dp
    val fileIconHeight = 48.dp
    /** File-icon dog-ear corner, 11 dp; label 9 sp / 800. */
    val dogEar = 11.dp
    /** Progress bar: 4 dp track, full radius. */
    val progressHeight = 4.dp
    /** File-type icon in a group crumb bar. */
    val crumbIconWidth = 30.dp
    val crumbIconHeight = 36.dp
    /** Highlight colour menu: 12 dp radius, 236 dp minimum width, 24 dp dots. */
    val highlightMenuRadius = 12.dp
    val highlightMenuMinWidth = 236.dp
    val highlightDotSize = 24.dp
    /** Hero card ("Jump back in"): 28 dp radius, 18 dp padding. */
    val heroRadius = 28.dp
    val heroPadding = 18.dp
    /** Segmented control: 7 × 14 dp segment padding. */
    val segmentPaddingV = 7.dp
    val segmentPaddingH = 14.dp
    /** Switch 52 × 32 dp, 24 dp thumb when on. */
    val switchWidth = 52.dp
    val switchHeight = 32.dp
    val switchThumb = 24.dp
    /** Reader tab: 34 dp tall, top corners 8 dp, name max 104 dp, × 19 dp. */
    val readerTabHeight = 34.dp
    val readerTabNameMax = 104.dp
    val readerTabClose = 19.dp
    /** Reader page column: max 720 dp, centred. */
    val pageMaxWidth = 720.dp
    /** Vertical page gap. */
    val pageGap = 12.dp
    val pagePaddingH = 20.dp
}

/**
 * Elevation — §4.5. Level 0 for list content, 1 raised cards, 2 the "Jump back in"
 * hero, 3 sheets/dialogs/FAB. On API 23–27 prefer [androidx.compose.ui.draw.shadow]
 * plus a tonal surface over real elevation to avoid grey smudges.
 */
object LeafElevation {
    val level0 = 0.dp
    val level1 = 1.dp
    val level2 = 2.dp
    val level3 = 3.dp
}
