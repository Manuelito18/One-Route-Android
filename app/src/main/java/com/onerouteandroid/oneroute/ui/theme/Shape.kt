package com.onerouteandroid.oneroute.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Corner radii, spacing steps and elevation tokens. Source: `specs/ui/DESIGN.md`. */
val OneRouteShapes = Shapes(
    // Small component: badges, vehicle markers, counters.
    extraSmall = RoundedCornerShape(OneRouteRadius.Small),
    small = RoundedCornerShape(OneRouteRadius.Small),
    // Medium container: text fields, dialogs, dropdowns, vehicle cards.
    medium = RoundedCornerShape(OneRouteRadius.Medium),
    // Large container: ride cards, bottom sheets, route panels.
    large = RoundedCornerShape(OneRouteRadius.Large),
    // Card / banner container.
    extraLarge = RoundedCornerShape(OneRouteRadius.Card),
)

/** 8dp baseline grid with 4dp sub-increments. */
object OneRouteSpacing {
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 16.dp
    val SpaceLg = 24.dp
    val SpaceXl = 32.dp

    /** Interior gutter between split card attributes (mobile). */
    val GutterMobile = 12.dp

    /** Outer screen margin (mobile). */
    val Margin = 16.dp

    /** Outer screen margin (tablet / large foldable). */
    val MarginDesktop = 24.dp
}

/** Named corner radii. Full-pill shapes use `CircleShape` directly. */
object OneRouteRadius {
    /** Small component — 8dp. */
    val Small = 8.dp

    /** Medium container — 16dp. */
    val Medium = 16.dp

    /** Large container — 24dp. */
    val Large = 24.dp

    /** Card / banner container — 32dp. */
    val Card = 32.dp

    /** Insight panel / sheet container — 48dp. */
    val Sheet = 48.dp

    /** Form field container — 12dp. */
    val Field = 12.dp

    /** Checkbox box — 6dp. */
    val Checkbox = 6.dp
}

/**
 * Material 3 tints ambient layers instead of dropping shadows, so the shadow
 * colours are the token — not the elevation alone.
 */
object OneRouteElevation {
    /** Level 0 — flat canvas. */
    val Flat = 0.dp

    /** Level 1 — cards and bars at rest. */
    val Resting = 1.dp

    /** Level 2 — active elements and extended FAB. */
    val Active = 3.dp

    /** Level 3 — modal bottom sheets. */
    val Modal = 6.dp

    internal val RestingAmbient = Color(0x0A0F172A) // 4%
    internal val RestingSpot = Color(0x1A0F172A) // 10%
    internal val ActiveAmbient = Color(0x140F172A) // 8%
    internal val ActiveSpot = Color(0x2E0F172A) // 18%
    internal val TopBarAmbient = Color(0x0A000000) // 4%
    internal val TopBarSpot = Color(0x0A000000) // 4%
}
