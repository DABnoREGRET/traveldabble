package com.dabber.traveldabble.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.components.GradientCover
import com.dabber.traveldabble.ui.mock.Trip
import com.dabber.traveldabble.ui.mock.toUi
import com.dabber.traveldabble.ui.navigation.ScrollState

@Composable
fun TripsScreen(
    onTripClick: (String) -> Unit,
    onCreateTrip: () -> Unit,
    onRequireLogin: () -> Unit = {},
) {
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        trips = Repository.getTrips().map { it.toUi() }
        loading = false
    }

    val upcoming = trips.filter { it.daysUntil != null || it.days.isNotEmpty() }
    val past = trips.filter { it.daysUntil == null && it.days.isEmpty() }

    val listState = rememberLazyListState()

    // Detect scroll events for bottom bar visibility
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        ScrollState.onScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "My trips",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassIconButton(
                        Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        onClick = { refreshKey++ },
                    )
                    GlassIconButton(
                        Icons.Filled.Add,
                        contentDescription = "Create trip",
                        onClick = onCreateTrip,
                    )
                }
            }
        }

        if (loading && trips.isEmpty()) {
            item {
                Text(
                    "Loading trips…",
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
                        .padding(horizontal = 20.dp, vertical = 20.dp),
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
                            "Plan your first Vietnam adventure or ask AI to sketch a route.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        GlassButton(
                            label = "Create trip",
                            icon = Icons.Filled.Add,
                            onClick = onCreateTrip,
                            accent = true,
                        )
                    }
                }
            }
        }

        if (trips.isNotEmpty()) {
            if (upcoming.isNotEmpty()) {
                item {
                    Text(
                        "Upcoming & Active",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                items(upcoming) { trip ->
                    TripListCard(trip, onClick = { onTripClick(trip.id) })
                }
            }

            if (past.isNotEmpty()) {
                item {
                    Text(
                        "Past Trips",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(top = 8.dp),
                    )
                }
                items(past) { trip ->
                    TripListCard(trip, onClick = { onTripClick(trip.id) }, muted = true)
                }
            }
        }
    }
}

@Composable
private fun TripListCard(trip: Trip, onClick: () -> Unit, muted: Boolean = false) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        onClick = onClick,
        contentPadding = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GradientCover(
                gradient = trip.coverColors,
                modifier = Modifier.size(76.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    trip.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${trip.destination}, ${trip.country}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${trip.startDate} – ${trip.endDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
                if (!muted) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        trip.daysUntil?.let { GlassChip(label = "In $it days") }
                        GlassChip(label = "${trip.travelers} travelers", icon = Icons.Filled.People)
                    }
                }
            }
        }
    }
}
