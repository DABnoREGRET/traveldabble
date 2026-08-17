package com.dabber.traveldabble.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.AuthState
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.data.SettingsState
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.StatTile
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.navigation.ScrollState

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToAppInfo: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
) {
    val user = AuthState.currentUser
    val isGuest = AuthState.isGuestMode
    var tripCount by remember { mutableIntStateOf(0) }
    var countryCount by remember { mutableIntStateOf(0) }
    var daysAway by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val trips = Repository.getTrips()
        tripCount = trips.size
        countryCount = trips.map { it.country }.distinct().size
        daysAway = trips.mapNotNull { it.daysUntil }.sum()
    }

    val initial = (user?.displayName?.firstOrNull()?.uppercaseChar() ?: 'T').toString()
    val displayName = if (isGuest) "Local Traveler" else (user?.displayName ?: "Traveler")
    val email = if (isGuest) "Local-first storage • Sign in to sync" else (user?.email ?: "")

    val listState = rememberLazyListState()

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        ScrollState.onScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "Profile",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }

        // Hero Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            initial,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(Modifier.size(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    value = tripCount.toString(),
                    label = "Trips",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = countryCount.toString(),
                    label = "Countries",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = if (daysAway > 0) "${daysAway}d" else "—",
                    label = "Upcoming",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Demo Mode Control Card
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("App Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

                GlassCard(intensity = GlassIntensity.Standard, contentPadding = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column {
                                Text(
                                    "Curated Vietnam Demo Data",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    if (SettingsState.demoMode) "Sample trips enabled (Hanoi, Da Nang, Ha Giang...)"
                                    else "Clean slate (User trips only)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Switch(
                            checked = SettingsState.demoMode,
                            onCheckedChange = { SettingsState.updateDemoMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            ),
                        )
                    }
                }
            }
        }

        // Settings Section
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

                SettingsRow(
                    icon = Icons.Filled.Palette,
                    title = "Appearance",
                    subtitle = SettingsState.themeMode.label,
                    onClick = onNavigateToAppearance,
                )
                SettingsRow(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    subtitle = if (SettingsState.tripReminders) "Reminders active" else "Disabled",
                    onClick = onNavigateToNotifications,
                )
                SettingsRow(
                    icon = Icons.Filled.Map,
                    title = "Map & Navigation",
                    subtitle = "${SettingsState.defaultMapStyle.label} style",
                    onClick = onNavigateToMap,
                )
                SettingsRow(
                    icon = Icons.Filled.AutoAwesome,
                    title = "AI Assistant",
                    subtitle = if (SettingsState.openRouterApiKey != null) "Using custom API key" else "Local / Server AI",
                    onClick = onNavigateToAccount,
                )
                SettingsRow(
                    icon = Icons.Filled.Person,
                    title = "Account",
                    subtitle = if (isGuest) "Local mode" else (user?.displayName ?: "Signed in"),
                    onClick = onNavigateToAccount,
                )
                SettingsRow(
                    icon = Icons.Filled.Security,
                    title = "Privacy",
                    subtitle = if (SettingsState.telemetryOptOut) "Telemetry off" else "Telemetry on",
                    onClick = onNavigateToPrivacy,
                )
                SettingsRow(
                    icon = Icons.Filled.Info,
                    title = "About",
                    subtitle = "Travel Dabble v1.0 • Vietnam Edition",
                    onClick = onNavigateToAppInfo,
                )
            }
        }

        // Account / Login Card
        item {
            if (isGuest) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Local-First Mode Active",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Your trips and plans are stored directly on your device. Sign in anytime to sync to the cloud.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        GlassButton(
                            label = "Sign In or Register (Optional)",
                            icon = Icons.AutoMirrored.Filled.Login,
                            onClick = onSignIn,
                            accent = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                GlassButton(
                    label = "Log out",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) {
    GlassCard(intensity = GlassIntensity.Subtle, contentPadding = 14.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
