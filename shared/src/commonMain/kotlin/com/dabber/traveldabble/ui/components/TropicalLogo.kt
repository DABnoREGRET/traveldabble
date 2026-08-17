package com.dabber.traveldabble.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import traveldabble.shared.generated.resources.Res
import traveldabble.shared.generated.resources.ic_travel_dabble_logo
import com.dabber.traveldabble.ui.theme.AuroraTeal
import org.jetbrains.compose.resources.painterResource

/**
 * Tropical Vector Logo for Travel Dabble.
 * Loads the actual vector SVG / XML logo asset (Res.drawable.ic_travel_dabble_logo).
 */
@Composable
fun TropicalLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showBackground: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showBackground) {
                    Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(size * 0.26f),
                            spotColor = AuroraTeal.copy(alpha = 0.35f),
                        )
                        .clip(RoundedCornerShape(size * 0.26f))
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_travel_dabble_logo),
            contentDescription = "Travel Dabble Tropical Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size),
        )
    }
}
