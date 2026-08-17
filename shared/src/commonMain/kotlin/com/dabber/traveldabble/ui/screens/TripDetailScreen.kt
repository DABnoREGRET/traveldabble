package com.dabber.traveldabble.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.ui.components.CategoryBadge
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.components.GradientCover
import com.dabber.traveldabble.ui.components.ProgressTrack
import com.dabber.traveldabble.ui.mock.Trip
import com.dabber.traveldabble.ui.mock.toUi

@Composable
fun TripDetailScreen(
    tripId: String,
    onBack: () -> Unit,
    onOpenItinerary: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onOpenBudget: (String) -> Unit,
    onAskAi: (String) -> Unit,
    onOpenGroup: (String) -> Unit = {},
) {
    var trip by remember { mutableStateOf<Trip?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(tripId) {
        loading = true
        trip = Repository.getTrip(tripId)?.toUi()
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (loading) {
            item {
                Text(
                    "Loading trip details…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                )
            }
        } else {
            val loadedTrip = trip
            if (loadedTrip == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Trip not found",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "We couldn't find this trip. It may have been deleted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        GlassIconButton(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                        )
                    }
                }
            } else {
                item { TripHero(loadedTrip, onBack) }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DetailAction("Itinerary", Icons.Filled.CalendarMonth, { onOpenItinerary(loadedTrip.id) }, Modifier.weight(1f))
                        DetailAction("Map", Icons.Filled.Map, { onOpenMap(loadedTrip.id) }, Modifier.weight(1f))
                        DetailAction("Budget", Icons.Filled.AccountBalanceWallet, { onOpenBudget(loadedTrip.id) }, Modifier.weight(1f))
                        DetailAction("Group", Icons.Filled.People, { onOpenGroup(loadedTrip.id) }, Modifier.weight(1f))
                    }
                }
                if (loadedTrip.days.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Text(
                                "Itinerary preview",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(10.dp))
                            loadedTrip.days.take(3).forEach { day ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                    onClick = { onOpenItinerary(loadedTrip.id) },
                                    contentPadding = 12.dp,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        DayBadge(day.dayNumber)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                day.dateLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                day.activities.joinToString(" → ") { it.place.name },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { BudgetSnapshot(loadedTrip, onOpenBudget) }
            }
        }
    }
}

@Composable
private fun TripHero(trip: Trip, onBack: () -> Unit) {
    Box {
        GradientCover(
            gradient = trip.coverColors,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
        )
        GlassIconButton(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 10.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            trip.daysUntil?.let {
                GlassChip(label = "In $it days", icon = Icons.Filled.CalendarMonth, tint = Color.White)
            }
            Text(trip.title, style = MaterialTheme.typography.displaySmall, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${trip.destination}, ${trip.country}  •  ${trip.startDate} – ${trip.endDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun DetailAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, onClick = onClick, contentPadding = 12.dp) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CategoryBadge(icon = icon, tint = MaterialTheme.colorScheme.primary, size = 30)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DayBadge(day: Int) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        CategoryBadge(icon = Icons.Filled.CalendarMonth, tint = MaterialTheme.colorScheme.primary, size = 40)
        Text(
            "$day",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun BudgetSnapshot(trip: Trip, onOpenBudget: (String) -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        onClick = { onOpenBudget(trip.id) },
        contentPadding = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Budget",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${trip.budget.spent.toInt()} / ${trip.budget.total.toInt()} USD",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(10.dp))
        ProgressTrack(
            fraction = (trip.budget.spent / trip.budget.total).toFloat().coerceIn(0f, 1f),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${trip.budget.expenses.size} expenses logged",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
