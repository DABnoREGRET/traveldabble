package com.dabber.traveldabble.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.ui.theme.GlassWhite10
import com.dabber.traveldabble.ui.theme.GlassWhite14
import com.dabber.traveldabble.ui.theme.GlassWhiteBorder
import com.dabber.traveldabble.ui.theme.SpaceDeep
import com.dabber.traveldabble.ui.theme.SpaceNight

enum class GlassIntensity {
    Subtle,
    Standard,
    Prominent,
}

/**
 * Apply a translucent glass surface with optional color [tint].
 * Adapts intelligently to Dark and Light theme modes.
 */
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(20.dp),
    intensity: GlassIntensity = GlassIntensity.Standard,
    tint: Color = Color.Transparent,
): Modifier {
    val isDark = MaterialTheme.colorScheme.background == SpaceNight || MaterialTheme.colorScheme.surface == SpaceDeep

    val (fill, borderAlpha) = if (isDark) {
        when (intensity) {
            GlassIntensity.Subtle -> GlassWhite10 to GlassWhite14
            GlassIntensity.Standard -> GlassWhite14 to GlassWhiteBorder
            GlassIntensity.Prominent -> Color(0x2EFFFFFF) to GlassWhiteBorder
        }
    } else {
        when (intensity) {
            GlassIntensity.Subtle -> Color(0xE6FFFFFF) to Color(0x1F0F172A)
            GlassIntensity.Standard -> Color(0xF2FFFFFF) to Color(0x280F172A)
            GlassIntensity.Prominent -> Color(0xFAFFFFFF) to Color(0x380F172A)
        }
    }

    val base = this
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    fill,
                    fill.copy(alpha = (fill.alpha * 0.90f).coerceAtMost(1f)),
                )
            )
        )
        .border(1.dp, borderAlpha, shape)

    return if (tint != Color.Transparent) {
        base.background(tint.copy(alpha = if (isDark) 0.12f else 0.08f), shape)
    } else {
        base
    }
}
