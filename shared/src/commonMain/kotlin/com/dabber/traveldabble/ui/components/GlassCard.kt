package com.dabber.traveldabble.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.glass.glass

/**
 * A translucent glass card with tactile press micro-interactions, customizable shape, tint, and depth.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    intensity: GlassIntensity = GlassIntensity.Standard,
    tint: Color = Color.Transparent,
    depth: Float = 1f,
    contentPadding: Dp = 16.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val adjustedIntensity = when {
        depth < 0.7f -> GlassIntensity.Subtle
        depth > 1.3f -> GlassIntensity.Prominent
        else -> intensity
    }
    Column(
        modifier = modifier
            .glass(shape, adjustedIntensity, tint)
            .then(if (onClick != null) Modifier.bounceClick(pressedScale = 0.975f, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
