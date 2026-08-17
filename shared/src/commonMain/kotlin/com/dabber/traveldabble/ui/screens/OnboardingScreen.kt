package com.dabber.traveldabble.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.SettingsState
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.TropicalLogo
import com.dabber.traveldabble.ui.components.bounceClick
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.glass.glass
import com.dabber.traveldabble.ui.theme.AuroraBlue
import com.dabber.traveldabble.ui.theme.AuroraGold
import com.dabber.traveldabble.ui.theme.AuroraTeal
import com.dabber.traveldabble.ui.theme.AuroraViolet
import kotlinx.coroutines.launch

/**
 * Interactive 4-step Onboarding Experience showcasing real interactive mock UIs:
 * 1. AI Travel Copilot chat mockup with tap-to-test prompts
 * 2. Day-by-Day route & itinerary planner mockup
 * 3. Google Maps style exploration mockup with custom pin markers
 * 4. Choose account mode: Sign In/Register vs Continue in Local Mode + Demo sample data switch
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onLogin: () -> Unit = onFinish,
    onRequestLocationPermission: () -> Unit = {},
) {
    val totalPages = 4
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top Header with Step Badge and Skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
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
                    text = "Step ${pagerState.currentPage + 1} of $totalPages",
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
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> AiCopilotSlide()
                1 -> ItineraryPlannerSlide()
                2 -> MapExplorationSlide()
                3 -> ChooseModeSlide(onLogin = onLogin, onContinueLocal = onFinish)
            }
        }

        // Progress Dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
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

        // Bottom Action Button (Steps 1-3)
        if (pagerState.currentPage < totalPages - 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
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
            }
        }
    }
}

/** Slide 1: Interactive AI Copilot Mockup */
@Composable
private fun AiCopilotSlide() {
    var selectedPrompt by remember { mutableStateOf("Plan a 3-day trip to Da Nang") }

    val sampleResponses = mapOf(
        "Plan a 3-day trip to Da Nang" to "Day 1: An Bang Beach & Hoi An lanterns.\nDay 2: Golden Bridge Ba Na Hills & My Khe.\nDay 3: Marble Mountains & Dragon Bridge!",
        "Best cafes in Hanoi Old Quarter" to "1. Cafe Giang (Famous Egg Coffee since 1946)\n2. Cafe Dinh (Balcony view of Hoan Kiem)\n3. The Note Coffee (Whimsical multi-story note cafe)",
        "Budget for Ha Giang Motorbike loop" to "Estimated $850 for 2 travelers:\n• Motorbike rental: $120\n• Homestays & dinners: $200\n• Nho Que river cruise: $36",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Smart AI Travel Copilot",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Ask anything about destinations, local food, or day-by-day schedules.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        // Interactive Mock Chat Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            intensity = GlassIntensity.Prominent,
            contentPadding = 16.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Assistant Bubble
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(AuroraTeal, AuroraViolet))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .glass(RoundedCornerShape(16.dp), GlassIntensity.Standard)
                            .padding(12.dp),
                    ) {
                        AnimatedContent(
                            targetState = sampleResponses[selectedPrompt] ?: "",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "aiResponse",
                        ) { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Text(
                    text = "Tap a prompt to test:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                // Interactive Prompt Pills
                sampleResponses.keys.forEach { prompt ->
                    val isSelected = selectedPrompt == prompt
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(pressedScale = 0.96f) { selectedPrompt = prompt }
                            .glass(RoundedCornerShape(12.dp), if (isSelected) GlassIntensity.Prominent else GlassIntensity.Subtle)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            if (isSelected) {
                                Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

/** Slide 2: Interactive Itinerary Planner Mockup */
@Composable
private fun ItineraryPlannerSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Day-by-Day Visual Itineraries",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Effortlessly organize activities, times, and budgets for every day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        // Mock Itinerary Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            intensity = GlassIntensity.Prominent,
            contentPadding = 16.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Day 1 • Hanoi Heritage",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    GlassChip(label = "$180 / $600 spent", tint = AuroraGold)
                }

                // Stops
                MockStopItem(time = "09:00", title = "Sofitel Legend Metropole", category = "Stay", color = Color(0xFF2563EB))
                MockStopItem(time = "11:30", title = "Temple of Literature", category = "Sight", color = Color(0xFF059669))
                MockStopItem(time = "14:00", title = "Cafe Giang (Egg Coffee)", category = "Food", color = Color(0xFFEA580C))
                MockStopItem(time = "18:30", title = "Bun Cha Huong Lien Dinner", category = "Food", color = Color(0xFFEA580C))
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun MockStopItem(time: String, title: String, category: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(12.dp), GlassIntensity.Standard)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

/** Slide 3: Interactive Google Maps Exploration Mockup */
@Composable
private fun MapExplorationSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Google Maps Style Navigation",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Interactive vector maps with custom teardrop pins and step-by-step route lines.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        // Mock Map Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            intensity = GlassIntensity.Prominent,
            contentPadding = 16.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Mock Map Surface with pins
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MockMapPin(label = "1", title = "Hoi An Town", color = Color(0xFF059669))
                            MockMapPin(label = "2", title = "Golden Bridge", color = Color(0xFF2563EB))
                            MockMapPin(label = "3", title = "Dragon Bridge", color = Color(0xFFEA580C))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Route: Da Nang – Hoi An Coastal Link (32 km)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    Text("3D Perspective", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("GPS Centering", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Offline Vector Tiles", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun MockMapPin(label: String, title: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

/** Slide 4: Getting Started Choice: Cloud vs Local Mode */
@Composable
private fun ChooseModeSlide(
    onLogin: () -> Unit,
    onContinueLocal: () -> Unit,
) {
    var isDemoMode by remember { mutableStateOf(SettingsState.demoMode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TropicalLogo(size = 56.dp, showBackground = true)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Choose How You Explore",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Travel Dabble is private & local-first by default. Sign in whenever you want cloud sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Option 1: Continue in Local Mode
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    SettingsState.updateDemoMode(isDemoMode)
                    SettingsState.completeOnboarding()
                    onContinueLocal()
                },
                intensity = GlassIntensity.Prominent,
                contentPadding = 16.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PhoneAndroid, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Continue in Local Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "No account required. Trips stay 100% on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Option 2: Sign In / Register
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    SettingsState.updateDemoMode(isDemoMode)
                    SettingsState.completeOnboarding()
                    onLogin()
                },
                intensity = GlassIntensity.Standard,
                contentPadding = 16.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(AuroraBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, null, tint = AuroraBlue, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sign In / Create Account",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Sync itineraries across devices and collaborate in real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Demo Mode Switch
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                intensity = GlassIntensity.Subtle,
                contentPadding = 14.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Load Curated Vietnam Demo Trips",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (isDemoMode) "Includes Hanoi, Da Nang, Ha Giang & Saigon sample trips"
                                   else "Start fresh with a clean empty slate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = isDemoMode,
                        onCheckedChange = { isDemoMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}
