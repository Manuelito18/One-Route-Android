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
 * The rounded plane that sits behind a form field.
 *
 * Both auth screens need the same behaviour and differ only in a handful of
 * numbers, so the animated shadow, the rest/focus container colours, the error
 * ring and the focus reporting all live here. Each screen keeps its own label
 * strategy — the registration form floats the label over the top edge, the login
 * form sets it above the field — which is why only the plane is shared.
 *
 * The mock's fields are borderless, so the resting shadow is always Level 1 and
 * steps to Level 2 on focus (`focus-within:shadow-md`).
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

/**
 * Helper / error line under a field, matching the mock's `mt-1.5 px-3`:
 * 6dp below the plane, 12dp in from its edge, set in `label-small`.
 */
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
