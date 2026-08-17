package com.dabber.traveldabble.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.SettingsState
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.TropicalLogo
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.theme.AuroraBlue
import com.dabber.traveldabble.ui.theme.AuroraTeal
import com.dabber.traveldabble.ui.theme.AuroraViolet
import kotlinx.coroutines.launch

private data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconGradient: List<Color>,
    val highlights: List<String>,
)

private val onboardingPages = listOf(
    OnboardingPageData(
        title = "Plan Trips Seamlessly",
        description = "Create tailored day-by-day itineraries, track expenses, and discover places with interactive maps.",
        icon = Icons.Filled.Explore,
        iconGradient = listOf(AuroraTeal, AuroraBlue),
        highlights = listOf("Interactive Itineraries", "Smart Budget Tracking", "Offline Maps & POIs"),
    ),
    OnboardingPageData(
        title = "AI Travel Copilot",
        description = "Get instant local suggestions, restaurant recommendations, and itinerary ideas powered by AI.",
        icon = Icons.Filled.AutoAwesome,
        iconGradient = listOf(AuroraTeal, AuroraViolet),
        highlights = listOf("Free Built-in AI Model", "BYOK OpenRouter Support", "Custom Travel Tools"),
    ),
    OnboardingPageData(
        title = "Private & Local-First",
        description = "Your trips stay safe on your device. Sign in anytime to sync across devices or invite friends.",
        icon = Icons.Filled.PhoneAndroid,
        iconGradient = listOf(AuroraBlue, AuroraViolet),
        highlights = listOf("Works Completely Offline", "Optional Cloud Sync", "Real-Time Collaboration"),
    ),
)

/**
 * Clean, crash-safe, and elegant 3-step onboarding flow.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onLogin: () -> Unit = onFinish,
    onRequestLocationPermission: () -> Unit = {},
) {
    val totalPages = onboardingPages.size
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} of $totalPages",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (pagerState.currentPage < totalPages - 1) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable {
                            SettingsState.completeOnboarding()
                            onFinish()
                        }
                        .padding(8.dp),
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
        }

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            val pageData = onboardingPages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Large Graphic / Logo / Icon
                if (page == 0) {
                    TropicalLogo(size = 80.dp, showBackground = true)
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(pageData.iconGradient)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = pageData.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = pageData.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = pageData.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                // Feature Highlights Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    intensity = GlassIntensity.Subtle,
                    contentPadding = 16.dp,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        pageData.highlights.forEach { highlight ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                                Text(
                                    text = highlight,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom section: Dots & Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Animated Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(totalPages) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        label = "dotWidth",
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            ),
                    )
                }
            }

            // Actions
            if (pagerState.currentPage < totalPages - 1) {
                GlassButton(
                    label = "Continue",
                    icon = Icons.AutoMirrored.Filled.NavigateNext,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    accent = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    GlassButton(
                        label = "Get Started",
                        onClick = {
                            SettingsState.completeOnboarding()
                            onFinish()
                        },
                        accent = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = "Sign In / Register",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                SettingsState.completeOnboarding()
                                onLogin()
                            }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                    )
                }
            }
        }
    }
}
