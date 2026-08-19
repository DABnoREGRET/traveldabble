package com.dabber.traveldabble.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.model.ActivityItem
import com.dabber.traveldabble.model.Place
import com.dabber.traveldabble.model.PlaceCategory
import com.dabber.traveldabble.ui.components.CategoryBadge
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.mock.Trip
import com.dabber.traveldabble.ui.mock.icon
import com.dabber.traveldabble.ui.mock.tint
import com.dabber.traveldabble.ui.mock.toUi
import com.dabber.traveldabble.ui.theme.Danger
import kotlinx.coroutines.launch

@Composable
fun ItineraryScreen(
    tripId: String,
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
) {
    var trip by remember { mutableStateOf<Trip?>(null) }
    var availablePlaces by remember { mutableStateOf<List<Place>>(emptyList()) }
    var selectedDay by remember { mutableIntStateOf(0) }
    var showAddActivityDialog by remember { mutableStateOf(false) }
    var activityTitle by remember { mutableStateOf("") }
    var activityAddress by remember { mutableStateOf("") }
    var activityCategory by remember { mutableStateOf(PlaceCategory.SIGHT) }
    var selectedPlaceLat by remember { mutableStateOf(21.0285) }
    var selectedPlaceLng by remember { mutableStateOf(105.8542) }
    var startTimeInput by remember { mutableStateOf("09:00") }
    var endTimeInput by remember { mutableStateOf("11:00") }
    var activityNote by remember { mutableStateOf("") }
    var activityToDelete by remember { mutableStateOf<Pair<Int, ActivityItem>?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tripId) {
        trip = Repository.getTrip(tripId)?.toUi()
        availablePlaces = Repository.getPlaces()
    }

    val loadedTrip = trip
    val day = loadedTrip?.days?.getOrNull(selectedDay) ?: loadedTrip?.days?.firstOrNull()

    if (showAddActivityDialog && day != null) {
        val categories = listOf(
            PlaceCategory.SIGHT,
            PlaceCategory.FOOD,
            PlaceCategory.STAY,
            PlaceCategory.TRANSIT,
            PlaceCategory.ACTIVITY,
        )
        AlertDialog(
            onDismissRequest = { showAddActivityDialog = false },
            title = { Text("Add Activity to Day ${day.dayNumber}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = activityTitle,
                        onValueChange = { activityTitle = it },
                        label = { Text("Place or Activity Name") },
                        placeholder = { Text("e.g. Dong Xuan Market or Old Quarter") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activityAddress,
                        onValueChange = { activityAddress = it },
                        label = { Text("Address / Location") },
                        placeholder = { Text("e.g. Dong Xuan St, Hoan Kiem, Hanoi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Quick select from destination places & shops
                    if (availablePlaces.isNotEmpty()) {
                        Text(
                            "Or choose from popular spots & shops:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            availablePlaces.take(8).forEach { p ->
                                GlassChip(
                                    label = p.name,
                                    selected = activityTitle == p.name,
                                    onClick = {
                                        activityTitle = p.name
                                        activityCategory = p.category
                                        activityAddress = p.description
                                        selectedPlaceLat = p.lat
                                        selectedPlaceLng = p.lng
                                    },
                                )
                            }
                        }
                    }

                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        categories.take(3).forEach { cat ->
                            GlassChip(
                                label = cat.label,
                                selected = activityCategory == cat,
                                onClick = { activityCategory = cat },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        categories.drop(3).forEach { cat ->
                            GlassChip(
                                label = cat.label,
                                selected = activityCategory == cat,
                                onClick = { activityCategory = cat },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = startTimeInput,
                            onValueChange = { startTimeInput = it },
                            label = { Text("Start Time") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = endTimeInput,
                            onValueChange = { endTimeInput = it },
                            label = { Text("End Time") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = activityNote,
                        onValueChange = { activityNote = it },
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("e.g. Bring camera and local cash") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (activityTitle.isNotBlank()) {
                            val newPlace = Place(
                                id = "place_${System.currentTimeMillis()}",
                                name = activityTitle.trim(),
                                category = activityCategory,
                                lat = selectedPlaceLat,
                                lng = selectedPlaceLng,
                                rating = 4.8f,
                                description = activityAddress.ifBlank { activityNote.ifBlank { "${activityTitle.trim()} in ${loadedTrip?.destination ?: "Vietnam"}" } },
                            )
                            scope.launch {
                                Repository.addActivityToTrip(
                                    tripId = tripId,
                                    dayNumber = day.dayNumber,
                                    place = newPlace,
                                    startTime = startTimeInput.trim().ifBlank { "09:00" },
                                    endTime = endTimeInput.trim().ifBlank { "11:00" },
                                    note = activityNote.takeIf { it.isNotBlank() } ?: activityAddress.takeIf { it.isNotBlank() },
                                )
                                trip = Repository.getTrip(tripId)?.toUi()
                            }
                            activityTitle = ""
                            activityAddress = ""
                            activityNote = ""
                            showAddActivityDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddActivityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (activityToDelete != null) {
        val (dayNum, act) = activityToDelete!!
        AlertDialog(
            onDismissRequest = { activityToDelete = null },
            title = { Text("Remove Activity?") },
            text = {
                Text(
                    "Remove '${act.place.name}' from Day $dayNum?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            Repository.removeActivityFromTrip(tripId, dayNum, act.id)
                            trip = Repository.getTrip(tripId)?.toUi()
                            activityToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { activityToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
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
            Column(Modifier.weight(1f)) {
                Text(
                    loadedTrip?.title ?: "Itinerary",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${loadedTrip?.destination ?: ""}, ${loadedTrip?.country ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (loadedTrip?.days?.isNotEmpty() == true) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                loadedTrip.days.forEachIndexed { index, d ->
                    GlassChip(
                        label = "Day ${d.dayNumber}",
                        selected = selectedDay == index,
                        onClick = { selectedDay = index },
                    )
                }

                GlassChip(
                    label = "+ Add Day",
                    selected = false,
                    onClick = {
                        scope.launch {
                            val newDay = Repository.addDayToTrip(tripId)
                            val updatedTrip = Repository.getTrip(tripId)?.toUi()
                            trip = updatedTrip
                            if (updatedTrip != null) {
                                selectedDay = updatedTrip.days.size - 1
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            if (day != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Day ${day.dayNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            day.dateLabel.ifBlank { "Day ${day.dayNumber}" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        "${day.activities.size} ${if (day.activities.size == 1) "activity" else "activities"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (day == null || day.activities.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "No activities planned for this day yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Ask AI Copilot or tap 'Add activity' below to start building your day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                } else {
                    itemsIndexed(day.activities) { index, activity ->
                        TimelineRow(
                            activity = activity,
                            isLast = index == day.activities.lastIndex,
                            onClick = { onPlaceClick(activity.place.id) },
                            onDelete = {
                                activityToDelete = day.dayNumber to activity
                            },
                        )
                    }
                }
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, start = 34.dp),
                        onClick = { showAddActivityDialog = true },
                        contentPadding = 14.dp,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "Add activity",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No activities planned for this trip yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    activity: ActivityItem,
    isLast: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
) {
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(96.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp),
            onClick = onClick,
            contentPadding = 12.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryBadge(icon = activity.place.category.icon, tint = activity.place.category.tint)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        activity.place.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${activity.startTime} – ${activity.endTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    activity.note?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete Activity",
                        tint = Danger.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
