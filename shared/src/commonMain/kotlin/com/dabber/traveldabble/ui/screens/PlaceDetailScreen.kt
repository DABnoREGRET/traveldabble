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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.model.Destination
import com.dabber.traveldabble.model.Place
import com.dabber.traveldabble.ui.components.CategoryBadge
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.components.GradientCover
import com.dabber.traveldabble.ui.mock.MockData
import com.dabber.traveldabble.ui.mock.icon
import com.dabber.traveldabble.ui.mock.tint
import com.dabber.traveldabble.ui.mock.toDomain
import com.dabber.traveldabble.ui.theme.AuroraGold
import com.dabber.traveldabble.ui.theme.AuroraTeal
import com.dabber.traveldabble.ui.theme.CoverOcean

@Composable
fun PlaceDetailScreen(
    placeId: String,
    onBack: () -> Unit,
    onPlaceClick: ((String) -> Unit)? = null,
    onNavigateToMap: ((lat: Double, lng: Double, placeId: String?) -> Unit)? = null,
    onNavigateToPlanTrip: ((String) -> Unit)? = null,
) {
    var place by remember { mutableStateOf<Place?>(null) }
    var destination by remember { mutableStateOf<Destination?>(null) }
    var relatedPlaces by remember { mutableStateOf<List<Place>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(placeId) {
        loading = true

        // 1. Try finding as a Place
        val foundPlace = Repository.getPlace(placeId)
        if (foundPlace != null) {
            place = foundPlace
            loading = false
            return@LaunchedEffect
        }

        // 2. Try finding as a Destination
        val foundDest = Repository.getDestination(placeId)
            ?: MockData.destinations.firstOrNull { it.id == placeId || it.name.equals(placeId, ignoreCase = true) }?.toDomain()

        if (foundDest != null) {
            destination = foundDest
            // Find places located in this destination area
            val destName = foundDest.name.lowercase()
            val allMockPlaces = MockData.hanoiPlaces + MockData.centralPlaces + MockData.haGiangPlaces + MockData.saigonPlaces + MockData.ninhBinhPlaces
            relatedPlaces = when {
                destName.contains("hanoi") || destName.contains("ha long") -> MockData.hanoiPlaces
                destName.contains("hoi an") || destName.contains("da nang") -> MockData.centralPlaces
                destName.contains("ha giang") -> MockData.haGiangPlaces
                destName.contains("ho chi minh") || destName.contains("saigon") -> MockData.saigonPlaces
                destName.contains("ninh binh") -> MockData.ninhBinhPlaces
                else -> allMockPlaces.take(6)
            }
            loading = false
            return@LaunchedEffect
        }

        loading = false
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val currentPlace = place
    val currentDest = destination

    if (currentPlace != null) {
        // Render Place Details
        PlaceDetailContent(
            place = currentPlace,
            onBack = onBack,
            onNavigateToMap = onNavigateToMap,
            onPlanTrip = onNavigateToPlanTrip,
        )
    } else if (currentDest != null) {
        // Render Destination Details
        DestinationDetailContent(
            destination = currentDest,
            relatedPlaces = relatedPlaces,
            onBack = onBack,
            onPlaceClick = onPlaceClick,
            onNavigateToMap = onNavigateToMap,
            onPlanTrip = onNavigateToPlanTrip,
        )
    } else {
        // Not found fallback
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Place or Destination not found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GlassButton(label = "Go Back", onClick = onBack)
            }
        }
    }
}

@Composable
private fun PlaceDetailContent(
    place: Place,
    onBack: () -> Unit,
    onNavigateToMap: ((lat: Double, lng: Double, placeId: String?) -> Unit)? = null,
    onPlanTrip: ((String) -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 60.dp),
    ) {
        item {
            Box {
                GradientCover(
                    gradient = listOf(place.category.tint, place.category.tint.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(230.dp),
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
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlassChip(label = place.category.label, tint = Color.White)
                    Text(place.name, style = MaterialTheme.typography.displaySmall, color = Color.White)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, tint = AuroraGold, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("${place.rating}", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(place.openHours, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CategoryBadge(icon = place.category.icon, tint = place.category.tint, size = 40)
                    Column {
                        Text(
                            "About this place",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            place.category.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    place.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassButton(
                        label = "View on Map",
                        icon = Icons.Filled.Map,
                        onClick = { onNavigateToMap?.invoke(place.lat, place.lng, place.id) },
                        accent = true,
                        modifier = Modifier.weight(1f),
                    )
                    GlassButton(
                        label = "Plan Trip",
                        icon = Icons.Filled.Add,
                        onClick = { onPlanTrip?.invoke(place.name) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationDetailContent(
    destination: Destination,
    relatedPlaces: List<Place>,
    onBack: () -> Unit,
    onPlaceClick: ((String) -> Unit)? = null,
    onNavigateToMap: ((lat: Double, lng: Double, placeId: String?) -> Unit)? = null,
    onPlanTrip: ((String) -> Unit)? = null,
) {
    val coverColors = if (destination.cover.isNotEmpty()) destination.cover.map { Color(it) } else CoverOcean

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Box {
                GradientCover(
                    gradient = coverColors,
                    modifier = Modifier.fillMaxWidth().height(250.dp),
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GlassChip(label = destination.country, tint = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, tint = AuroraGold, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("${destination.rating}", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                    Text(
                        destination.name,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Text(
                        destination.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }

        // Tags Pill Row
        if (destination.tags.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    destination.tags.forEach { tag ->
                        GlassChip(label = tag, tint = AuroraTeal)
                    }
                }
            }
        }

        // Top Attractions / Places in Destination
        if (relatedPlaces.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Top Sights & Experiences",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Tap any place to view details & map location",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(relatedPlaces) { itemPlace ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    onClick = {
                        onPlaceClick?.invoke(itemPlace.id)
                    },
                    contentPadding = 12.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CategoryBadge(icon = itemPlace.category.icon, tint = itemPlace.category.tint, size = 36)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                itemPlace.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                itemPlace.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, null, tint = AuroraGold, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("${itemPlace.rating}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View",
                                tint = AuroraTeal,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassButton(
                    label = "Explore on Map",
                    icon = Icons.Filled.Map,
                    onClick = {
                        val first = relatedPlaces.firstOrNull()
                        val lat = first?.lat ?: 21.0285
                        val lng = first?.lng ?: 105.8542
                        onNavigateToMap?.invoke(lat, lng, first?.id)
                    },
                    modifier = Modifier.weight(1f),
                )
                GlassButton(
                    label = "Plan Trip",
                    icon = Icons.Filled.Add,
                    onClick = { onPlanTrip?.invoke(destination.name) },
                    accent = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
