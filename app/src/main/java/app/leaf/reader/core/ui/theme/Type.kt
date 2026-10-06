package app.leaf.reader.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import app.leaf.reader.R

/**
 * Leaf type scale — LEAF-MASTER-PROMPT.md §4.4.
 *
 * UI text uses the platform sans (Roboto/system). Reading text uses the bundled
 * Noto Serif variable font (OFL licence in res/raw/ofl.txt), instantiated at Regular
 * and SemiBold through [FontVariation] so a single file covers both weights.
 * API 23–25 ignore variation settings and render the font's default (Regular) instance.
 */
val NotoSerif = FontFamily(
    Font(
        R.font.noto_serif,
        FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.noto_serif,
        FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    )
)

/** Weights the mockup uses that have no [FontWeight] constant. */
val FontWeight750 = FontWeight(750)
val FontWeight800 = FontWeight(800)

/** §4.4 — role-based scale. The M3 slot typography below maps the shared roles onto it. */
object LeafType {
    /** Screen title (app bar): 22 sp / 28, 600, UI sans. */
    val screenTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.W600
    )

    /** Section label ("8 DOCUMENTS"): 13 sp / 16, 700, tracking 0.6, uppercase. */
    val sectionLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.W700,
        letterSpacing = 0.6.sp
    )

    /** List item title: 14.5 sp / 19, 600. */
    val listTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.5.sp, lineHeight = 19.sp, fontWeight = FontWeight.W600
    )

    /** List item meta: 11 sp / 15, 400. */
    val listMeta = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.W400
    )

    /** Tag chip label: 10 sp / 13, 700. */
    val chipLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.W700
    )

    /** Body (sheets, settings): 14 sp / 20, 400. */
    val body = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.W400
    )

    /** Supporting text: 11.5 sp / 16, 400. */
    val supporting = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 11.5.sp, lineHeight = 16.sp, fontWeight = FontWeight.W400
    )

    /** Reading body: 17 sp, line-height 1.75 (≈29.75 sp), 400, Noto Serif. */
    val readingBody = TextStyle(
        fontFamily = NotoSerif,
        fontSize = 17.sp, lineHeight = 17.sp * 1.75f, fontWeight = FontWeight.W400
    )

    /** Reading body (reflow): 18 sp, line-height 1.85. */
    val readingReflow = TextStyle(
        fontFamily = NotoSerif,
        fontSize = 18.sp, lineHeight = 18.sp * 1.85f, fontWeight = FontWeight.W400
    )

    /** Reading heading inside a page: 1.25 × body, 600. */
    val readingHeading = TextStyle(
        fontFamily = NotoSerif,
        fontSize = (17 * 1.25f).sp, lineHeight = (17 * 1.25f).sp * 1.75f,
        fontWeight = FontWeight.W600
    )

    /** Reading sans option: same sizes, line-height 1.7. */
    val readingSans = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 17.sp, lineHeight = 17.sp * 1.7f, fontWeight = FontWeight.W400
    )

    /** Page label ("PAGE 4 OF 7"): 10 sp / 13, 800, tracking 1.4, uppercase. */
    val pageLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight800,
        letterSpacing = 1.4.sp
    )

    /** Navigation item label: 11.5 sp, 600. */
    val navLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 11.5.sp, lineHeight = 16.sp, fontWeight = FontWeight.W600
    )

    /** App-bar subtitle: 12 sp, 400. */
    val appBarSubtitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.W400
    )

    /** Empty-state title: 15.5 sp, 600. */
    val emptyTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 15.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.W600
    )

    /** Empty-state body: 13 sp, line-height 1.6. */
    val emptyBody = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp, lineHeight = 13.sp * 1.6f, fontWeight = FontWeight.W400
    )
}

/**
 * M3 slot typography. Keeps Material components on the Leaf scale; Compose code uses
 * [LeafType] for the roles that are not M3 slots.
 *
 * [LineHeightStyle.Trim.None] keeps the first and last lines on the mockup's exact
 * line heights instead of M3's default trimming.
 */
val LeafM3Typography = Typography(
    titleLarge = LeafType.screenTitle.copy(lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None
    )),
    titleMedium = LeafType.listTitle,
    titleSmall = LeafType.sectionLabel,
    bodyLarge = LeafType.body,
    bodyMedium = LeafType.supporting,
    bodySmall = LeafType.listMeta,
    labelLarge = LeafType.listTitle,
    labelMedium = LeafType.chipLabel,
    labelSmall = LeafType.pageLabel
)
