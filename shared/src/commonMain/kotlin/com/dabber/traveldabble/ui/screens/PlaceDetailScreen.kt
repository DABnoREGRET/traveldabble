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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.model.Place
import com.dabber.traveldabble.ui.components.CategoryBadge
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.components.GradientCover
import com.dabber.traveldabble.ui.mock.icon
import com.dabber.traveldabble.ui.mock.tint
import com.dabber.traveldabble.ui.theme.AuroraGold

@Composable
fun PlaceDetailScreen(placeId: String, onBack: () -> Unit) {
    var place by remember { mutableStateOf<Place?>(null) }

    LaunchedEffect(placeId) {
        val trips = Repository.getTrips()
        place = trips.flatMap { it.days }
            .flatMap { it.activities }
            .map { it.place }
            .firstOrNull { it.id == placeId }
    }

    val loadedPlace = place

    if (loadedPlace == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Loading destination…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            Box {
                GradientCover(
                    gradient = listOf(loadedPlace.category.tint, loadedPlace.category.tint.copy(alpha = 0.5f)),
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
                    GlassChip(label = loadedPlace.category.label, tint = Color.White)
                    Text(loadedPlace.name, style = MaterialTheme.typography.displaySmall, color = Color.White)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, tint = AuroraGold, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("${loadedPlace.rating}", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(loadedPlace.openHours, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
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
                    CategoryBadge(icon = loadedPlace.category.icon, tint = loadedPlace.category.tint, size = 40)
                    Column {
                        Text(
                            "About this place",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            loadedPlace.category.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    loadedPlace.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(label = "Add to trip", icon = Icons.Filled.Add, onClick = {}, accent = true)
                    GlassButton(label = "Bookmark", icon = Icons.Filled.FavoriteBorder, onClick = {})
                    GlassButton(label = "AI Tips", icon = Icons.Filled.AutoAwesome, onClick = {})
                }
            }
        }
    }
}
