package com.dabber.traveldabble.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.model.Destination
import com.dabber.traveldabble.model.Place
import com.dabber.traveldabble.model.Trip
import com.dabber.traveldabble.ui.components.CategoryBadge
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.components.GradientCover
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.mock.MockData
import com.dabber.traveldabble.ui.mock.icon
import com.dabber.traveldabble.ui.mock.tint
import com.dabber.traveldabble.ui.mock.toDomain
import com.dabber.traveldabble.ui.theme.AuroraGold
import com.dabber.traveldabble.ui.theme.AuroraTeal
import com.dabber.traveldabble.ui.theme.CoverOcean
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import com.dabber.traveldabble.ui.glass.glass
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private fun formatDateFromMillis(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val localDate = instant.toLocalDateTime(TimeZone.UTC).date
    val monthName = when (localDate.monthNumber) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }
    return "$monthName ${localDate.dayOfMonth}, ${localDate.year}"
}

@Composable
fun PlaceDetailScreen(
    placeId: String,
    onBack: () -> Unit,
    onPlaceClick: ((String) -> Unit)? = null,
    onNavigateToMap: ((lat: Double, lng: Double, placeId: String?) -> Unit)? = null,
    onNavigateToPlanTrip: ((String) -> Unit)? = null,
    onNavigateToTripDetail: ((String) -> Unit)? = null,
) {
    var place by remember { mutableStateOf<Place?>(null) }
    var destination by remember { mutableStateOf<Destination?>(null) }
    var relatedPlaces by remember { mutableStateOf<List<Place>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var userTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var showAddToTripDialog by remember { mutableStateOf(false) }
    var showAdoptTripDialog by remember { mutableStateOf(false) }
    var selectedTargetPlace by remember { mutableStateOf<Place?>(null) }
    var addedSuccessMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(placeId) {
        loading = true
        userTrips = Repository.getTrips()

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

    // Add to Itinerary Dialog
    if (showAddToTripDialog && selectedTargetPlace != null) {
        val targetPlace = selectedTargetPlace!!
        AddToTripModal(
            place = targetPlace,
            trips = userTrips,
            onDismiss = { showAddToTripDialog = false },
            onCreateNewTrip = {
                showAddToTripDialog = false
                onNavigateToPlanTrip?.invoke(targetPlace.name)
            },
            onAddToTrip = { tripId, dayNumber ->
                scope.launch {
                    Repository.addActivityToTrip(
                        tripId = tripId,
                        dayNumber = dayNumber,
                        place = targetPlace,
                        startTime = "09:00",
                        endTime = "11:00",
                        note = "Added from Place Details",
                    )
                    val tripTitle = userTrips.firstOrNull { it.id == tripId }?.title ?: "Trip"
                    addedSuccessMessage = "Added ${targetPlace.name} to $tripTitle (Day $dayNumber)!"
                    showAddToTripDialog = false
                }
            },
        )
    }

    // Adopt Recommended Destination Trip Dialog (Select Date Only)
    val destToAdopt = destination
    if (showAdoptTripDialog && destToAdopt != null) {
        AdoptRecommendedTripModal(
            destination = destToAdopt,
            relatedPlaces = relatedPlaces,
            onDismiss = { showAdoptTripDialog = false },
            onConfirm = { startDate, endDate, travelersCount ->
                scope.launch {
                    val created = Repository.createTrip(
                        title = "${destToAdopt.name} Discovery",
                        destination = destToAdopt.name,
                        country = destToAdopt.country,
                        startDate = startDate,
                        endDate = endDate,
                        travelers = travelersCount,
                    )
                    if (created != null) {
                        val numDays = created.days.size.coerceAtLeast(1)
                        relatedPlaces.forEachIndexed { index, p ->
                            val dayNumber = (index % numDays) + 1
                            val isMorning = (index / numDays) % 2 == 0
                            val startTime = if (isMorning) "09:30" else "14:30"
                            val endTime = if (isMorning) "12:30" else "17:30"
                            Repository.addActivityToTrip(created.id, dayNumber, p, startTime, endTime, p.description)
                        }
                        showAdoptTripDialog = false
                        if (onNavigateToTripDetail != null) {
                            onNavigateToTripDetail(created.id)
                        } else {
                            addedSuccessMessage = "Trip created! Added ${destToAdopt.name} to your trips."
                        }
                    }
                }
            }
        )
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
        PlaceDetailContent(
            place = currentPlace,
            addedSuccessMessage = addedSuccessMessage,
            onBack = onBack,
            onNavigateToMap = onNavigateToMap,
            onAddToItinerary = {
                selectedTargetPlace = currentPlace
                if (userTrips.isEmpty()) {
                    onNavigateToPlanTrip?.invoke(currentPlace.name)
                } else {
                    showAddToTripDialog = true
                }
            },
        )
    } else if (currentDest != null) {
        DestinationDetailContent(
            destination = currentDest,
            relatedPlaces = relatedPlaces,
            addedSuccessMessage = addedSuccessMessage,
            onBack = onBack,
            onPlaceClick = onPlaceClick,
            onNavigateToMap = onNavigateToMap,
            onPlanTrip = { showAdoptTripDialog = true },
            onAddPlaceToItinerary = { targetPl ->
                selectedTargetPlace = targetPl
                if (userTrips.isEmpty()) {
                    onNavigateToPlanTrip?.invoke(targetPl.name)
                } else {
                    showAddToTripDialog = true
                }
            },
        )
    } else {
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
    addedSuccessMessage: String?,
    onBack: () -> Unit,
    onNavigateToMap: ((lat: Double, lng: Double, placeId: String?) -> Unit)? = null,
    onAddToItinerary: () -> Unit,
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

        // Success snack banner
        addedSuccessMessage?.let { msg ->
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    intensity = GlassIntensity.Standard,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(msg, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
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
                        accent = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlassButton(
                        label = "Add to Trip",
                        icon = Icons.Filled.Add,
                        onClick = onAddToItinerary,
                        accent = true,
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
    addedSuccessMessage: String?,
    onBack: () -> Unit,
    onPlaceClick: ((String) -> Unit)? = null,
    onNavigateToMap: ((lat: Double, lng: Double, placeId: String?) -> Unit)? = null,
    onPlanTrip: ((String) -> Unit)? = null,
    onAddPlaceToItinerary: ((Place) -> Unit)? = null,
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

        // Success message banner
        addedSuccessMessage?.let { msg ->
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    intensity = GlassIntensity.Standard,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(msg, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
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

@Composable
private fun AddToTripModal(
    place: Place,
    trips: List<Trip>,
    onDismiss: () -> Unit,
    onCreateNewTrip: () -> Unit,
    onAddToTrip: (tripId: String, dayNumber: Int) -> Unit,
) {
    var selectedTrip by remember { mutableStateOf<Trip?>(trips.firstOrNull()) }
    var selectedDayNumber by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add to Itinerary")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Add \"${place.name}\" (${place.category.label}) to your planned route:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text("Select Trip:", style = MaterialTheme.typography.labelLarge)
                trips.forEach { t ->
                    val isSelected = selectedTrip?.id == t.id
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        intensity = if (isSelected) GlassIntensity.Prominent else GlassIntensity.Subtle,
                        onClick = {
                            selectedTrip = t
                            selectedDayNumber = 1
                        },
                        contentPadding = 10.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(t.title, style = MaterialTheme.typography.titleSmall)
                            Text("${t.days.size.coerceAtLeast(1)} days", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                selectedTrip?.let { st ->
                    Text("Select Day:", style = MaterialTheme.typography.labelLarge)
                    val daysCount = st.days.size.coerceAtLeast(3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        (1..daysCount).forEach { dayNum ->
                            GlassChip(
                                label = "Day $dayNum",
                                selected = selectedDayNumber == dayNum,
                                onClick = { selectedDayNumber = dayNum },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedTrip?.let {
                        onAddToTrip(it.id, selectedDayNumber)
                    }
                },
                enabled = selectedTrip != null,
            ) {
                Text("Add Activity")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCreateNewTrip) {
                    Text("New Trip")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdoptRecommendedTripModal(
    destination: Destination,
    relatedPlaces: List<Place>,
    onDismiss: () -> Unit,
    onConfirm: (startDate: String, endDate: String, travelers: Int) -> Unit,
) {
    val nowMillis = Clock.System.now().toEpochMilliseconds()
    var startDate by remember { mutableStateOf(formatDateFromMillis(nowMillis + 7L * 24 * 60 * 60 * 1000)) }
    var endDate by remember { mutableStateOf(formatDateFromMillis(nowMillis + 14L * 24 * 60 * 60 * 1000)) }
    var travelers by remember { mutableStateOf("2") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = nowMillis + 7L * 24 * 60 * 60 * 1000,
            selectableDates = FutureOrPresentSelectableDates,
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            startDate = formatDateFromMillis(it)
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val startLocalDate = Repository.parseDateStringToLocalDate(startDate)
        val nowLocalDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val minEpochDays = startLocalDate?.toEpochDays() ?: nowLocalDate.toEpochDays()
        val initialEndMillis = startLocalDate?.let {
            Instant.fromEpochMilliseconds(it.toEpochDays() * 24L * 60 * 60 * 1000).toEpochMilliseconds() + (7L * 24 * 60 * 60 * 1000)
        } ?: (nowMillis + 14L * 24 * 60 * 60 * 1000)

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEndMillis,
            selectableDates = MinDateSelectableDates(minEpochDays),
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            endDate = formatDateFromMillis(it)
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add ${destination.name} to Trips")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "Select your dates and we'll automatically generate your full day-by-day itinerary with top highlights.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text("Travel Dates *", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .glass(shape = RoundedCornerShape(16.dp), intensity = GlassIntensity.Standard)
                            .clickable { showStartDatePicker = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(startDate, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .glass(shape = RoundedCornerShape(16.dp), intensity = GlassIntensity.Standard)
                            .clickable { showEndDatePicker = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("End Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(endDate, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Text("Travelers", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("1", "2", "3", "4", "5+").forEach { num ->
                        GlassChip(
                            label = "$num ${if (num == "1") "Person" else "People"}",
                            selected = travelers == num || (num == "5+" && travelers.toIntOrNull() != null && travelers.toInt() >= 5),
                            onClick = {
                                travelers = if (num == "5+") "5" else num
                            }
                        )
                    }
                }

                if (relatedPlaces.isNotEmpty()) {
                    Text("Included Highlights:", style = MaterialTheme.typography.labelMedium)
                    Text(
                        relatedPlaces.take(4).joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(startDate, endDate, travelers.toIntOrNull() ?: 2)
                }
            ) {
                Text("Add to My Trips")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
