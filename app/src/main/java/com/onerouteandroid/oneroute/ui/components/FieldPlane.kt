package com.onerouteandroid.oneroute.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onerouteandroid.oneroute.ui.theme.OneRouteElevation

/** Duration shared by every field state change. */
const val FieldAnimationMillis = 180

/**
 * The rounded plane behind a form field.
 *
 * Shared because every auth screen wants the same animated shadow, rest/focus
 * colours, error ring and focus reporting; only the numbers differ. Label
 * strategy stays per-screen (registration floats it, sign-in sets it above).
 *
 * Fields are borderless, so the resting shadow is Level 1 and steps to Level 2
 * on focus.
 */
@Composable
fun FieldPlane(
    focused: Boolean,
    isError: Boolean,
    shape: Shape,
    height: Dp,
    restContainerColor: Color,
    focusContainerColor: Color,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shadowElevation by animateDpAsState(
        targetValue = if (focused && !isError) {
            OneRouteElevation.Active
        } else {
            OneRouteElevation.Resting
        },
        animationSpec = tween(FieldAnimationMillis),
        label = "fieldShadowElevation",
    )
    val shadowAmbient by animateColorAsState(
        targetValue = if (focused) {
            OneRouteElevation.ActiveAmbient
        } else {
            OneRouteElevation.RestingAmbient
        },
        label = "fieldShadowAmbient",
    )
    val shadowSpot by animateColorAsState(
        targetValue = if (focused) {
            OneRouteElevation.ActiveSpot
        } else {
            OneRouteElevation.RestingSpot
        },
        label = "fieldShadowSpot",
    )
    val containerColor by animateColorAsState(
        targetValue = if (focused) focusContainerColor else restContainerColor,
        label = "fieldContainerColor",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                clip = false,
                ambientColor = shadowAmbient,
                spotColor = shadowSpot,
            )
            .clip(shape)
            .background(containerColor)
            .then(
                if (isError) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.error, shape)
                } else {
                    Modifier
                },
            )
            .onFocusChanged { onFocusChange(it.isFocused) },
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}

/** The 22dp leading glyph shared by every field. */
@Composable
fun FieldLeadingIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color? = null,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(size),
    )
}

/** Helper / error line under a field: 6dp below the plane, inset 12dp. */
@Composable
fun FieldHelperLine(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(Modifier.width(4.dp)) // gap-1
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
