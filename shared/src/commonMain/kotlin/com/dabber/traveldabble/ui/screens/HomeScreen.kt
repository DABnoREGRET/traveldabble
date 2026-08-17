package com.dabber.traveldabble.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.dabber.traveldabble.ui.components.bounceClick
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.components.GradientCover
import com.dabber.traveldabble.ui.components.ProgressTrack
import com.dabber.traveldabble.ui.components.SectionHeader
import com.dabber.traveldabble.data.AuthState
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.ui.mock.Destination
import com.dabber.traveldabble.model.Place
import com.dabber.traveldabble.ui.mock.Trip
import com.dabber.traveldabble.ui.mock.toUi
import com.dabber.traveldabble.ui.map.MapStyle
import com.dabber.traveldabble.ui.map.TravelMap
import com.dabber.traveldabble.ui.navigation.ScrollState
import com.dabber.traveldabble.ui.theme.AuroraGold

@Composable
fun HomeScreen(
    onTripClick: (String) -> Unit,
    onCreateTrip: () -> Unit,
    onAskAi: () -> Unit,
    onOpenMap: () -> Unit,
    onExplore: () -> Unit,
    onSeeAllTrips: () -> Unit,
    onDestinationClick: (String) -> Unit,
) {
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var destinations by remember { mutableStateOf<List<Destination>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        trips = Repository.getTrips().map { it.toUi() }
        destinations = Repository.getDestinations().map { it.toUi() }
        loading = false
    }

    val nextTrip = trips.firstOrNull { it.daysUntil != null } ?: trips.firstOrNull()
    val upcomingTrips = if (nextTrip != null) trips.filterNot { it == nextTrip } else trips

    val listState = rememberLazyListState()

    // Detect scroll events for bottom bar visibility
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        ScrollState.onScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HomeHeader(onAskAi = onAskAi, onOpenMap = onOpenMap)
        }
        item {
            QuickActionsGrid(
                onCreateTrip = onCreateTrip,
                onAskAi = onAskAi,
                onOpenMap = onOpenMap,
                onExplore = onExplore,
            )
        }
        if (loading && trips.isEmpty()) {
            item {
                Text(
                    "Loading trips...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        if (!loading && trips.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "No trips yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Create your first adventure or explore destinations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        com.dabber.traveldabble.ui.components.GlassButton(
                            label = "Create trip",
                            icon = Icons.Filled.Add,
                            onClick = onCreateTrip,
                        )
                    }
                }
            }
        }
        nextTrip?.let { trip ->
            item {
                NextTripCard(trip = trip, onTripClick = { onTripClick(trip.id) })
            }
        }
        if (upcomingTrips.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(
                        title = "Upcoming trips",
                        actionLabel = "See all",
                        onAction = onSeeAllTrips,
                    )
                    Spacer(Modifier.height(12.dp))
                    UpcomingTripsList(trips = upcomingTrips, onTripClick = onTripClick)
                }
            }
        }
        if (destinations.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(title = "Explore destinations")
                    Spacer(Modifier.height(12.dp))
                    ExploreCarousel(destinations = destinations, onDestinationClick = onDestinationClick)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onAskAi: () -> Unit, onOpenMap: () -> Unit) {
    val user = AuthState.currentUser
    val displayName = user?.displayName ?: "Traveler"
    val initial = (user?.displayName?.firstOrNull()?.uppercaseChar() ?: 'A').toString()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                timeGreeting(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                displayName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassIconButton(
                Icons.Filled.Search,
                contentDescription = "Search",
                onClick = {},
            )
            GlassIconButton(
                Icons.Filled.Map,
                contentDescription = "Map",
                onClick = onOpenMap,
            )
            GlassIconButton(
                Icons.Filled.AutoAwesome,
                contentDescription = "Ask AI",
                onClick = onAskAi,
            )
            // Profile avatar placeholder — circle with initials.
            Box(
                modifier = Modifier
                    .size(42.dp)
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
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onCreateTrip: () -> Unit,
    onAskAi: () -> Unit,
    onOpenMap: () -> Unit,
    onExplore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickAction("New trip", Icons.Filled.Add, onCreateTrip, Modifier.weight(1f))
        QuickAction("Ask AI", Icons.Filled.AutoAwesome, onAskAi, Modifier.weight(1f))
        QuickAction("Map", Icons.Filled.Map, onOpenMap, Modifier.weight(1f))
        QuickAction("Explore", Icons.Filled.Explore, onExplore, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .bounceClick(pressedScale = 0.94f, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun NextTripCard(trip: Trip, onTripClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        onClick = onTripClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mini map showing the trip's places
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                val places = trip.days.flatMap { it.activities }.map { it.place }.distinctBy { it.id }
                TravelMap(
                    places = places,
                    style = MapStyle.Liberty,
                    modifier = Modifier.fillMaxSize(),
                    tilt3d = false,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    trip.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    "${trip.destination}, ${trip.country}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    "${trip.startDate} – ${trip.endDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
                trip.daysUntil?.let { daysUntil ->
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                                    )
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(
                            "In $daysUntil days",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingTripsList(trips: List<Trip>, onTripClick: (String) -> Unit) {
    Column {
        trips.forEach { trip ->
            TripCard(trip = trip, onTripClick = { onTripClick(trip.id) })
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TripCard(trip: Trip, onTripClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        onClick = onTripClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GradientCover(
                gradient = trip.coverColors,
                modifier = Modifier.size(70.dp),
            ) {
                // Trip count badge (only shown for trips with multiple days).
                if (trip.days.isNotEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Box(
                            Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "${trip.days.size} days",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    trip.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    "${trip.destination}, ${trip.country}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    "${trip.startDate} – ${trip.endDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun ExploreCarousel(destinations: List<Destination>, onDestinationClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        destinations.take(5).forEach { dest ->
            InspirationCard(dest, onClick = { onDestinationClick(dest.id) })
        }
    }
}

@Composable
private fun InspirationCard(destination: Destination, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.width(250.dp),
        onClick = onClick,
        contentPadding = 0.dp,
    ) {
        GradientCover(
            gradient = destination.coverColors,
            modifier = Modifier.fillMaxWidth().height(130.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.TopEnd) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = AuroraGold, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${destination.rating}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }
            }
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${destination.name}, ${destination.country}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                destination.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                destination.tags.forEach { GlassChip(label = it) }
            }
        }
    }
}

private fun timeGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}