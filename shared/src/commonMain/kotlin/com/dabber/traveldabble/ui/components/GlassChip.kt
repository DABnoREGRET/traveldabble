package com.dabber.traveldabble.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.glass.glass

/**
 * Semantic chip variants.
 */
enum class GlassChipStyle {
    /** Generic tag / label chip. */
    Assist,
    /** Toggleable filter chip. */
    Filter,
}

@Composable
fun GlassChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    tint: Color = Color.Unspecified,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    style: GlassChipStyle = GlassChipStyle.Assist,
) {
    val shape = RoundedCornerShape(50)

    val glassIntensity = when {
        style == GlassChipStyle.Filter && selected -> GlassIntensity.Prominent
        selected -> GlassIntensity.Prominent
        else -> GlassIntensity.Subtle
    }

    val effectiveTint = if (tint != Color.Unspecified) {
        tint
    } else if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val backgroundTint = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent

    Row(
        modifier = modifier
            .bounceClick(pressedScale = 0.94f, onClick = onClick)
            .glass(shape, glassIntensity, backgroundTint)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = effectiveTint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = effectiveTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(6.dp))
            Icon(trailingIcon, contentDescription = null, tint = effectiveTint, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun GlassChipRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = horizontalArrangement, content = content)
}
