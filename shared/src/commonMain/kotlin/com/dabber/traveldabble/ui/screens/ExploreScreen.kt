package com.dabber.traveldabble.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.components.GradientCover
import com.dabber.traveldabble.ui.mock.Destination
import com.dabber.traveldabble.ui.mock.toUi
import com.dabber.traveldabble.ui.theme.AuroraGold

@Composable
fun ExploreScreen(onBack: () -> Unit, onDestinationClick: (String) -> Unit) {
    val exploreTags = listOf("All", "Beach", "City", "Mountains", "Food", "Culture", "Adventure", "Nature")
    var selectedTag by remember { mutableStateOf("All") }
    var destinations by remember { mutableStateOf<List<Destination>>(emptyList()) }

    LaunchedEffect(Unit) {
        destinations = Repository.getDestinations().map { it.toUi() }
    }

    val filtered = remember(selectedTag, destinations) {
        if (selectedTag == "All") destinations
        else destinations.filter { it.tags.contains(selectedTag) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
            Text(
                "Explore Vietnam",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            exploreTags.forEach { tag ->
                GlassChip(
                    label = tag,
                    selected = tag == selectedTag,
                    onClick = { selectedTag = tag },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(filtered) { destination ->
                ExploreCard(destination, onClick = { onDestinationClick(destination.id) })
            }
        }
    }
}

@Composable
private fun ExploreCard(destination: Destination, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, contentPadding = 0.dp) {
        GradientCover(gradient = destination.coverColors, modifier = Modifier.fillMaxWidth().height(140.dp)) {
            Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.TopEnd) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Star, null, tint = AuroraGold, modifier = Modifier.width(14.dp).height(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${destination.rating}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${destination.name}, ${destination.country}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                destination.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                destination.tags.forEach { GlassChip(label = it) }
            }
        }
    }
}
