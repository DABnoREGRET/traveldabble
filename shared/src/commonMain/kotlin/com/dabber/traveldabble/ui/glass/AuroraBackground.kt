package com.dabber.traveldabble.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.dabber.traveldabble.ui.theme.JadeGreen
import com.dabber.traveldabble.ui.theme.LightBackground
import com.dabber.traveldabble.ui.theme.MekongOrange
import com.dabber.traveldabble.ui.theme.SpaceDeep
import com.dabber.traveldabble.ui.theme.SpaceNight

/**
 * Atmospheric background canvas adapting between Dark and Light mode.
 * Dark mode: deep obsidian night with subtle aurora ambient glow.
 * Light mode: crisp light canvas with soft ambient warmth.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background == SpaceNight || MaterialTheme.colorScheme.surface == SpaceDeep

    if (isDark) {
        Box(
            modifier
                .fillMaxSize()
                .background(SpaceNight)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                JadeGreen.copy(alpha = 0.08f),
                                MekongOrange.copy(alpha = 0.04f),
                                Color.Transparent,
                            ),
                        )
                    )
            )
        }
    } else {
        Box(
            modifier
                .fillMaxSize()
                .background(LightBackground)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                JadeGreen.copy(alpha = 0.05f),
                                MekongOrange.copy(alpha = 0.03f),
                                Color.Transparent,
                            ),
                        )
                    )
            )
        }
    }
}
