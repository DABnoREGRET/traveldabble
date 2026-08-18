package com.dabber.traveldabble.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.ui.components.bounceClick
import com.dabber.traveldabble.ui.theme.SpaceDeep
import com.dabber.traveldabble.ui.theme.SpaceNight

@Composable
fun GlassBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val isDark = MaterialTheme.colorScheme.background == SpaceNight || MaterialTheme.colorScheme.surface == SpaceDeep
    val dockBgColor = if (isDark) Color(0xF40F172A) else Color(0xF8FFFFFF)
    val dockBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0x220F172A)
    val dockShape = RoundedCornerShape(32.dp)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) +
                slideInVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    initialOffsetY = { it },
                ),
        exit = fadeOut(animationSpec = tween(180)) +
               slideOutVertically(
                   animationSpec = tween(250, easing = FastOutSlowInEasing),
                   targetOffsetY = { it },
               ),
        modifier = modifier.navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .fillMaxWidth()
                .shadow(16.dp, dockShape)
                .clip(dockShape)
                .background(dockBgColor)
                .border(1.dp, dockBorderColor, dockShape)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomTabs.forEach { tab ->
                val selected = currentRoute == tab.route

                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "tabIconScale",
                )

                val contentColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    animationSpec = tween(durationMillis = 200),
                    label = "tabColor",
                )

                val pillBackground by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                  else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "tabPillBg",
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(pillBackground)
                        .bounceClick(pressedScale = 0.94f) { onTabSelected(tab.route) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(iconScale),
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = contentColor,
                        )
                    }
                }
            }
        }
    }
}
