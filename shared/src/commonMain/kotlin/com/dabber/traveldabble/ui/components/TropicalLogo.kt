package com.dabber.traveldabble.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.ui.theme.AuroraGold
import com.dabber.traveldabble.ui.theme.AuroraTeal
import com.dabber.traveldabble.ui.theme.JadeGreen

/**
 * Tropical Vector Logo for Travel Dabble.
 * Rendered using pure Compose Multiplatform graphics for 100% crash-proof,
 * zero-latency rendering across all Android and KMP targets.
 */
@Composable
fun TropicalLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showBackground: Boolean = true,
) {
    val cornerRadius = size * 0.26f

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showBackground) {
                    Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(cornerRadius),
                            spotColor = AuroraTeal.copy(alpha = 0.35f),
                        )
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF064E3B), // Tropical Jungle Green
                                    JadeGreen,         // Emerald
                                    Color(0xFF0F172A), // Ocean Navy
                                )
                            )
                        )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.TravelExplore,
            contentDescription = "Travel Dabble Tropical Logo",
            tint = AuroraGold,
            modifier = Modifier.size(size * 0.62f),
        )
    }
}
