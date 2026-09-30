package com.onerouteandroid.oneroute.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/*
 * The light scheme is the literal `colors` block from specs/ui/DESIGN.md.
 *
 * The dark scheme is not specified by the design system, so it is derived from
 * the same tonal family: the `inverse-*` / `*-fixed*` roles take over as the
 * light-facing roles, and the surface containers step down through the
 * `on-surface` → `inverse-surface` ramp.
 */
private val LightColorScheme = lightColorScheme(
    primary = OneRoutePrimary,
    onPrimary = OneRouteOnPrimary,
    primaryContainer = OneRoutePrimaryContainer,
    onPrimaryContainer = OneRouteOnPrimaryContainer,
    inversePrimary = OneRouteInversePrimary,
    secondary = OneRouteSecondary,
    onSecondary = OneRouteOnSecondary,
    secondaryContainer = OneRouteSecondaryContainer,
    onSecondaryContainer = OneRouteOnSecondaryContainer,
    tertiary = OneRouteTertiary,
    onTertiary = OneRouteOnTertiary,
    tertiaryContainer = OneRouteTertiaryContainer,
    onTertiaryContainer = OneRouteOnTertiaryContainer,
    error = OneRouteError,
    onError = OneRouteOnError,
    errorContainer = OneRouteErrorContainer,
    onErrorContainer = OneRouteOnErrorContainer,
    background = OneRouteBackground,
    onBackground = OneRouteOnBackground,
    surface = OneRouteSurface,
    onSurface = OneRouteOnSurface,
    surfaceVariant = OneRouteSurfaceVariant,
    onSurfaceVariant = OneRouteOnSurfaceVariant,
    surfaceDim = OneRouteSurfaceDim,
    surfaceBright = OneRouteSurfaceBright,
    surfaceContainerLowest = OneRouteSurfaceContainerLowest,
    surfaceContainerLow = OneRouteSurfaceContainerLow,
    surfaceContainer = OneRouteSurfaceContainer,
    surfaceContainerHigh = OneRouteSurfaceContainerHigh,
    surfaceContainerHighest = OneRouteSurfaceContainerHighest,
    inverseSurface = OneRouteInverseSurface,
    inverseOnSurface = OneRouteInverseOnSurface,
    outline = OneRouteOutline,
    outlineVariant = OneRouteOutlineVariant,
    surfaceTint = OneRouteSurfaceTint,
    scrim = OneRouteInverseSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = OneRoutePrimaryFixedDim,
    onPrimary = OneRouteOnPrimaryFixed,
    primaryContainer = OneRouteOnPrimaryFixedVariant,
    onPrimaryContainer = OneRoutePrimaryFixed,
    inversePrimary = OneRoutePrimary,
    secondary = OneRouteSecondaryFixedDim,
    onSecondary = OneRouteOnSecondaryFixed,
    secondaryContainer = OneRouteOnSecondaryFixedVariant,
    onSecondaryContainer = OneRouteSecondaryFixed,
    tertiary = OneRouteTertiaryFixedDim,
    onTertiary = OneRouteOnTertiaryFixed,
    tertiaryContainer = OneRouteOnTertiaryFixedVariant,
    onTertiaryContainer = OneRouteTertiaryFixed,
    error = OneRouteTertiaryFixedDim,
    onError = OneRouteOnTertiaryFixed,
    errorContainer = OneRouteOnTertiaryFixedVariant,
    onErrorContainer = OneRouteTertiaryFixed,
    background = OneRouteOnSurface,
    onBackground = OneRouteInverseOnSurface,
    surface = OneRouteOnSurface,
    onSurface = OneRouteInverseOnSurface,
    surfaceVariant = OneRouteOutlineVariant,
    onSurfaceVariant = OneRouteOutlineVariant,
    surfaceDim = OneRouteOnSurface,
    surfaceBright = OneRouteInverseSurface,
    surfaceContainerLowest = Color(0xFF0C1322),
    surfaceContainerLow = OneRouteOnSurface,
    surfaceContainer = Color(0xFF1B2338),
    surfaceContainerHigh = OneRouteInverseSurface,
    surfaceContainerHighest = Color(0xFF333C52),
    inverseSurface = OneRouteInverseOnSurface,
    inverseOnSurface = OneRouteOnSurface,
    outline = OneRouteOutline,
    outlineVariant = OneRouteOnSurfaceVariant,
    surfaceTint = OneRoutePrimaryFixedDim,
    scrim = Color(0xFF000000),
)

@Composable
fun OneRouteAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default: Material You would repaint the OneRoute brand indigo, and
    // the approved mock-ups are pinned to the DESIGN.md palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = OneRouteShapes,
        content = content,
    )
}
