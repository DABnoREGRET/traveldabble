package com.dabber.traveldabble.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.ui.theme.AuroraBlue
import com.dabber.traveldabble.ui.theme.AuroraTeal
import com.dabber.traveldabble.ui.theme.SpaceDeep
import com.dabber.traveldabble.ui.theme.SpaceNight

/**
 * Button style variants for glass-surfaced buttons.
 */
enum class GlassButtonStyle {
    /** Gradient accent (teal → blue) for primary actions. */
    Accent,
    /** Muted glass surface for secondary actions. */
    Default,
    /** Subtler glass for tertiary actions. */
    Secondary,
}

@Composable
fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Boolean = false,
    style: GlassButtonStyle = if (accent) GlassButtonStyle.Accent else GlassButtonStyle.Default,
) {
    val isDark = MaterialTheme.colorScheme.background == SpaceNight || MaterialTheme.colorScheme.surface == SpaceDeep
    val shape = RoundedCornerShape(18.dp)
    val bgModifier: Modifier
    val contentColor: Color

    when (style) {
        GlassButtonStyle.Accent -> {
            bgModifier = Modifier.background(
                Brush.linearGradient(
                    if (isDark) listOf(AuroraTeal, AuroraBlue)
                    else listOf(Color(0xFF059669), Color(0xFF0284C7))
                ),
                shape,
            )
            contentColor = Color.White
        }
        GlassButtonStyle.Default -> {
            bgModifier = Modifier
                .background(
                    if (isDark) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape,
                )
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.20f) else Color(0x28000000),
                    shape,
                )
            contentColor = MaterialTheme.colorScheme.onSurface
        }
        GlassButtonStyle.Secondary -> {
            bgModifier = Modifier
                .background(
                    if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.04f),
                    shape,
                )
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.12f) else Color(0x18000000),
                    shape,
                )
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    CompositionLocalProvider(LocalGlassButtonContentColor provides contentColor) {
        Row(
            modifier = modifier
                .bounceClick(pressedScale = 0.95f, onClick = onClick)
                .clip(shape)
                .then(bgModifier)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp),
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val isDark = MaterialTheme.colorScheme.background == SpaceNight || MaterialTheme.colorScheme.surface == SpaceDeep
    Box(
        modifier = modifier
            .bounceClick(pressedScale = 0.90f, onClick = onClick)
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.06f))
            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.10f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(19.dp))
    }
}

/**
 * CompositionLocal that mirrors M3's LocalContentColor for use inside
 * glass-surfaced composables.
 */
val LocalGlassButtonContentColor = staticCompositionLocalOf { Color.White }
