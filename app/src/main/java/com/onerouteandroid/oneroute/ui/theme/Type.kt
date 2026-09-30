package com.onerouteandroid.oneroute.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/*
 * OneRoute type scale — taken verbatim from the `typography` block of
 * `specs/ui/DESIGN.md`.
 *
 * The spec asks for "Roboto Flex". That family is not bundled with the OS, so
 * the app ships [FontFamily.SansSerif], which resolves to the platform Roboto
 * on every Android device. The metrics below are what actually drive the
 * layout, so swapping the family in later is a one-line change here.
 */
private val OneRouteFontFamily: FontFamily = FontFamily.SansSerif

/** Trim leading so that Material's 4dp baseline grid stays exact. */
private val TightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun oneRouteStyle(
    fontSize: Int,
    lineHeight: Int,
    fontWeight: FontWeight,
    letterSpacing: Double,
) = TextStyle(
    fontFamily = OneRouteFontFamily,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    fontWeight = fontWeight,
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = TightLineHeight,
)

/**
 * Standalone styles for the scale steps that Material 3 has no dedicated slot
 * for, so they cannot be reached through [Typography].
 */
object OneRouteType {
    /** `headline-lg` — 32sp / 40sp / 700. Screen entry titles. */
    val HeadlineLarge = oneRouteStyle(32, 40, FontWeight.Bold, 0.0)

    /** `headline-lg-mobile` — 28sp / 36sp / 700. Mobile entry titles. */
    val HeadlineLargeMobile = oneRouteStyle(28, 36, FontWeight.Bold, 0.0)

    /** `headline-md` — 24sp / 32sp / 700. Route summaries. */
    val HeadlineMedium = oneRouteStyle(24, 32, FontWeight.Bold, 0.0)

    /** `title-md` — 16sp / 24sp / 500. Structural anchor for card headers. */
    val TitleMedium = oneRouteStyle(16, 24, FontWeight.Medium, 0.15)

    /** `label-sm` — 11sp / 16sp / 500. Technical metadata and counters. */
    val LabelSmall = oneRouteStyle(11, 16, FontWeight.Medium, 0.5)
}

val Typography = Typography(
    headlineLarge = oneRouteStyle(32, 40, FontWeight.Bold, 0.0),
    headlineMedium = oneRouteStyle(24, 32, FontWeight.Bold, 0.0),
    headlineSmall = oneRouteStyle(20, 26, FontWeight.SemiBold, 0.0),
    titleLarge = oneRouteStyle(20, 26, FontWeight.SemiBold, 0.0),
    titleMedium = oneRouteStyle(16, 24, FontWeight.Medium, 0.15),
    bodyLarge = oneRouteStyle(16, 24, FontWeight.Normal, 0.5),
    bodyMedium = oneRouteStyle(14, 20, FontWeight.Normal, 0.25),
    labelLarge = oneRouteStyle(14, 20, FontWeight.Medium, 0.1),
    labelSmall = oneRouteStyle(11, 16, FontWeight.Medium, 0.5),
)
