package com.dabber.traveldabble.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.dabber.traveldabble.ui.components.bounceClick
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.MapStyleSetting
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.data.SettingsState
import com.dabber.traveldabble.model.DayPlan
import com.dabber.traveldabble.model.Place
import com.dabber.traveldabble.model.PlaceCategory
import com.dabber.traveldabble.ui.components.CategoryBadge
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.map.MapStyle
import com.dabber.traveldabble.ui.map.TravelMap
import com.dabber.traveldabble.ui.mock.Trip
import com.dabber.traveldabble.ui.mock.icon
import com.dabber.traveldabble.ui.mock.tint
import com.dabber.traveldabble.ui.mock.toUi
import com.dabber.traveldabble.ui.theme.AuroraGold
import com.dabber.traveldabble.ui.theme.AuroraTeal
import com.dabber.traveldabble.ui.theme.SpaceDeep
import com.dabber.traveldabble.ui.theme.SpaceNight
import kotlinx.coroutines.launch

/**
 * High-contrast surface modifier for floating map cards to eliminate "bright on bright" issues.
 */
@Composable
private fun Modifier.mapCardSurface(shape: Shape = RoundedCornerShape(20.dp)): Modifier {
    val isDark = MaterialTheme.colorScheme.background == SpaceNight || MaterialTheme.colorScheme.surface == SpaceDeep
    val bgColor = if (isDark) Color(0xF40F172A) else Color(0xFAFFFFFF)
    val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x240F172A)

    return this
        .shadow(8.dp, shape)
        .clip(shape)
        .background(bgColor)
        .border(1.dp, borderColor, shape)
}

/**
 * Google Maps styled interactive Map screen with high-contrast surfaces,
 * real roadway curve navigation, and crash-proof style switcher.
 */
@Composable
fun MapScreen(
    tripId: String? = null,
    initialLat: Double? = null,
    initialLng: Double? = null,
    focusPlaceId: String? = null,
    onPlaceClick: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    var trip by remember { mutableStateOf<Trip?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var allPlaces by remember { mutableStateOf<List<Place>>(emptyList()) }
    var selectedTripId by remember { mutableStateOf(tripId) }
    var showTripSelector by remember { mutableStateOf(false) }
    var showLayerMenu by remember { mutableStateOf(false) }

    var focusLocation by remember(initialLat, initialLng) {
        mutableStateOf(
            if (initialLat != null && initialLng != null) initialLat to initialLng else null
        )
    }

    // Day-by-day route filter: null = all days in trip, 1 = Day 1, 2 = Day 2...
    var selectedDayNumber by remember { mutableStateOf<Int?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<PlaceCategory?>(null) }
    var isDrawerExpanded by remember { mutableStateOf(true) }

    // Map style from persistent SettingsState
    var style by remember {
        mutableStateOf(
            when (SettingsState.defaultMapStyle) {
                MapStyleSetting.Liberty -> MapStyle.Liberty
                MapStyleSetting.Positron -> MapStyle.Positron
                MapStyleSetting.Bright -> MapStyle.Bright
                MapStyleSetting.Dark -> MapStyle.Dark
                MapStyleSetting.Fiord -> MapStyle.Fiord
            }
        )
    }
    var tilt3d by remember { mutableStateOf(true) }
    var showPolylines by remember { mutableStateOf(true) }
    var showUserLocation by remember { mutableStateOf(true) }
    var autoCenter by remember { mutableStateOf(initialLat == null && focusPlaceId == null) }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var addPlaceDayNumber by remember { mutableStateOf(1) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tripId) {
        selectedTripId = tripId
        val loadedTrips = Repository.getTrips().map { it.toUi() }
        trips = loadedTrips
        val tripPlaces = loadedTrips.flatMap { t ->
            t.days.flatMap { it.activities }.map { it.place }
        }
        val defaultPlaces = Repository.getPlaces()
        allPlaces = (tripPlaces + defaultPlaces).distinctBy { it.id }

        if (tripId != null) {
            val t = Repository.getTrip(tripId)?.toUi()
            trip = t
            if (focusLocation == null && t != null) {
                val firstPlace = t.days.flatMap { it.activities }.map { it.place }.firstOrNull()
                if (firstPlace != null) {
                    focusLocation = firstPlace.lat to firstPlace.lng
                    autoCenter = false
                }
            }
        }
    }

    LaunchedEffect(focusPlaceId, allPlaces) {
        if (focusPlaceId != null) {
            val found = allPlaces.firstOrNull { it.id == focusPlaceId }
                ?: Repository.getPlace(focusPlaceId)
            if (found != null) {
                selectedPlace = found
                focusLocation = found.lat to found.lng
                autoCenter = false
            }
        }
    }

    LaunchedEffect(selectedTripId) {
        selectedDayNumber = null
        if (selectedTripId == null) {
            trip = null
        } else {
            val t = Repository.getTrip(selectedTripId!!)?.toUi()
            trip = t
            val firstPlace = t?.days?.flatMap { it.activities }?.map { it.place }?.firstOrNull()
            if (firstPlace != null) {
                focusLocation = firstPlace.lat to firstPlace.lng
                autoCenter = false
            }
        }
    }

    val loadedTrip = trip

    if (showAddPlaceDialog && selectedPlace != null) {
        val placeToAdd = selectedPlace!!
        var targetTripId by remember { mutableStateOf(loadedTrip?.id ?: trips.firstOrNull()?.id ?: "") }
        val targetTrip = trips.firstOrNull { it.id == targetTripId } ?: loadedTrip

        AlertDialog(
            onDismissRequest = { showAddPlaceDialog = false },
            title = { Text("Add ${placeToAdd.name} to trip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Add this point of interest to your trip itinerary.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (trips.size > 1) {
                        Text("Select Trip:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            trips.forEach { t ->
                                GlassChip(
                                    label = t.title,
                                    selected = targetTripId == t.id,
                                    onClick = { targetTripId = t.id },
                                )
                            }
                        }
                    }
                    if (targetTrip != null && targetTrip.days.isNotEmpty()) {
                        Text("Select Day:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            targetTrip.days.forEach { day ->
                                GlassChip(
                                    label = "Day ${day.dayNumber}",
                                    selected = addPlaceDayNumber == day.dayNumber,
                                    onClick = { addPlaceDayNumber = day.dayNumber },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalTripId = targetTrip?.id ?: targetTripId
                        if (finalTripId.isNotBlank()) {
                            scope.launch {
                                Repository.addActivityToTrip(
                                    tripId = finalTripId,
                                    dayNumber = addPlaceDayNumber,
                                    place = placeToAdd,
                                    startTime = "09:00",
                                    endTime = "11:00",
                                    note = "Added from trip map",
                                )
                                val updatedTrips = Repository.getTrips().map { it.toUi() }
                                trips = updatedTrips
                                trip = updatedTrips.firstOrNull { it.id == (loadedTrip?.id ?: finalTripId) }
                                showAddPlaceDialog = false
                                selectedPlace = null
                            }
                        }
                    }
                ) {
                    Text("Add to itinerary")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlaceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Filter places based on selected trip, selected day, and category filter
    val currentDayPlan: DayPlan? = remember(loadedTrip, selectedDayNumber) {
        if (loadedTrip != null && selectedDayNumber != null) {
            loadedTrip.days.firstOrNull { it.dayNumber == selectedDayNumber }
        } else null
    }

    val placesForMap = remember(loadedTrip, selectedDayNumber, selectedCategoryFilter, allPlaces, selectedPlace) {
        val basePlaces = when {
            loadedTrip != null -> {
                val itineraryPlaces = if (currentDayPlan != null) {
                    currentDayPlan.activities.map { it.place }
                } else {
                    loadedTrip.days.flatMap { it.activities }.map { it.place }
                }
                (itineraryPlaces + allPlaces).distinctBy { it.id }
            }
            else -> allPlaces
        }

        val list = if (selectedCategoryFilter != null) {
            basePlaces.filter { it.category == selectedCategoryFilter }
        } else {
            basePlaces
        }

        // Ensure focused place is in the marker set so its pin renders
        if (selectedPlace != null && list.none { it.id == selectedPlace?.id }) {
            list + selectedPlace!!
        } else {
            list
        }
    }

    val routePlaces = remember(loadedTrip, selectedDayNumber) {
        when {
            currentDayPlan != null -> currentDayPlan.activities.map { it.place }
            loadedTrip != null -> loadedTrip.days.flatMap { it.activities }.map { it.place }
            else -> emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Google Maps Vector Surface with turn-by-turn road curves
        TravelMap(
            places = placesForMap,
            style = style,
            modifier = Modifier.fillMaxSize(),
            tilt3d = tilt3d,
            showPolylines = showPolylines && (loadedTrip != null || currentDayPlan != null),
            routePlaces = routePlaces,
            showUserLocation = showUserLocation,
            autoCenterOnLocation = autoCenter,
            focusLocation = focusLocation,
            onMarkerClick = {
                selectedPlace = it
                focusLocation = it.lat to it.lng
                autoCenter = false
            },
        )

        // 2. Top Google Maps Floating Header
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Search / Trip Bar Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .mapCardSurface(RoundedCornerShape(18.dp))
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (onBack != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                .bounceClick(pressedScale = 0.90f) { onBack() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to trip",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (loadedTrip != null) Icons.Filled.Route else Icons.Filled.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTripSelector = true },
                    ) {
                        Text(
                            text = loadedTrip?.title ?: "All Explorer Places",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (loadedTrip != null) "${loadedTrip.destination}, ${loadedTrip.country} • ${loadedTrip.days.size} days"
                                   else "${placesForMap.size} locations across Vietnam",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }

                    // Trip Dropdown Button
                    Box {
                        Icon(
                            Icons.Filled.ExpandMore,
                            contentDescription = "Select Trip",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(24.dp)
                                .bounceClick(pressedScale = 0.90f) { showTripSelector = true },
                        )
                        DropdownMenu(
                            expanded = showTripSelector,
                            onDismissRequest = { showTripSelector = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Explorer Places (${allPlaces.size})") },
                                onClick = {
                                    selectedTripId = null
                                    showTripSelector = false
                                },
                            )
                            trips.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("${t.title} (${t.days.size} days)") },
                                    onClick = {
                                        selectedTripId = t.id
                                        showTripSelector = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Quick Category Filters (Pill Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GlassChip(
                    label = "All (${placesForMap.size})",
                    selected = selectedCategoryFilter == null,
                    tint = if (selectedCategoryFilter == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { selectedCategoryFilter = null },
                )
                PlaceCategory.entries.forEach { cat ->
                    GlassChip(
                        label = cat.label,
                        icon = cat.icon,
                        selected = selectedCategoryFilter == cat,
                        tint = if (selectedCategoryFilter == cat) cat.tint else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                        },
                    )
                }
            }
        }

        // 3. Right-Hand Side Floating Action Control Stack
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 118.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            // Layer / Map Style Button
            MapFloatingButton(
                icon = Icons.Filled.Layers,
                contentDescription = "Map Style",
                onClick = { showLayerMenu = !showLayerMenu },
            )
            DropdownMenu(
                expanded = showLayerMenu,
                onDismissRequest = { showLayerMenu = false },
            ) {
                MapStyle.entries.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.label) },
                        trailingIcon = {
                            if (s == style) Icon(Icons.Filled.Star, null, tint = AuroraGold, modifier = Modifier.size(16.dp))
                        },
                        onClick = {
                            style = s
                            SettingsState.updateDefaultMapStyle(
                                when (s) {
                                    MapStyle.Liberty, MapStyle.ThreeD -> MapStyleSetting.Liberty
                                    MapStyle.Positron -> MapStyleSetting.Positron
                                    MapStyle.Bright -> MapStyleSetting.Bright
                                    MapStyle.Dark -> MapStyleSetting.Dark
                                    MapStyle.Fiord -> MapStyleSetting.Fiord
                                }
                            )
                            showLayerMenu = false
                        },
                    )
                }
            }

            // 3D / 2D Perspective Tilt Button
            MapFloatingButton(
                icon = Icons.Filled.ViewInAr,
                contentDescription = if (tilt3d) "3D Mode" else "2D Mode",
                selected = tilt3d,
                onClick = { tilt3d = !tilt3d },
            )

            // GPS My Location / Center Button
            MapFloatingButton(
                icon = Icons.Filled.MyLocation,
                contentDescription = "My Location",
                selected = autoCenter,
                onClick = {
                    autoCenter = true
                    com.dabber.traveldabble.requestLocationPermissionFromContext()
                },
            )
        }

        // 4. Bottom Day-by-Day Itinerary Drawer or Place Summary
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 90.dp),
        ) {
            // Selected Place Popup Card
            AnimatedVisibility(
                visible = selectedPlace != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                selectedPlace?.let { place ->
                    PlaceDetailPopup(
                        place = place,
                        onDismiss = { selectedPlace = null },
                        onOpen = { onPlaceClick(place.id) },
                        onAddToTrip = { showAddPlaceDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }

            // Trip Day-by-Day Drawer
            if (loadedTrip != null && selectedPlace == null) {
                TripDayDrawer(
                    trip = loadedTrip,
                    selectedDay = selectedDayNumber,
                    isExpanded = isDrawerExpanded,
                    onToggleExpand = { isDrawerExpanded = !isDrawerExpanded },
                    onSelectDay = { selectedDayNumber = it },
                    onPlaceClick = {
                        selectedPlace = it
                        autoCenter = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MapFloatingButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .bounceClick(pressedScale = 0.90f, onClick = onClick)
            .mapCardSurface(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun TripDayDrawer(
    trip: Trip,
    selectedDay: Int?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectDay: (Int?) -> Unit,
    onPlaceClick: (Place) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .mapCardSurface(RoundedCornerShape(22.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Drawer Header with Day Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Day-by-Day Route",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onToggleExpand),
                ) {
                    Text(
                        text = if (isExpanded) "Hide stops" else "Show stops",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Day Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GlassChip(
                    label = "All Days (${trip.days.size})",
                    selected = selectedDay == null,
                    tint = if (selectedDay == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onSelectDay(null) },
                )
                trip.days.forEach { day ->
                    GlassChip(
                        label = "Day ${day.dayNumber}",
                        selected = selectedDay == day.dayNumber,
                        tint = if (selectedDay == day.dayNumber) AuroraTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { onSelectDay(day.dayNumber) },
                    )
                }
            }

            // Horizontal Carousel of Itinerary Stops for selected day
            AnimatedVisibility(visible = isExpanded) {
                val activeActivities = if (selectedDay != null) {
                    trip.days.firstOrNull { it.dayNumber == selectedDay }?.activities.orEmpty()
                } else {
                    trip.days.flatMap { it.activities }
                }

                if (activeActivities.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        itemsIndexed(activeActivities) { index, activity ->
                            ItineraryStopCard(
                                stepNumber = index + 1,
                                time = activity.startTime,
                                place = activity.place,
                                note = activity.note ?: activity.place.description,
                                onClick = { onPlaceClick(activity.place) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItineraryStopCard(
    stepNumber: Int,
    time: String,
    place: Place,
    note: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(210.dp)
            .bounceClick(pressedScale = 0.95f, onClick = onClick)
            .mapCardSurface(RoundedCornerShape(16.dp))
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(place.category.tint),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }

            Text(
                text = place.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = note.ifBlank { place.description },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlaceDetailPopup(
    place: Place,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onAddToTrip: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(pressedScale = 0.98f, onClick = onOpen)
            .mapCardSurface(RoundedCornerShape(20.dp))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryBadge(icon = place.category.icon, tint = place.category.tint)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = AuroraGold, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${place.rating}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        place.category.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = place.category.tint,
                    )
                }
                Text(
                    text = place.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                onAddToTrip?.let {
                    TextButton(onClick = it) {
                        Text("Add to itinerary")
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .bounceClick(pressedScale = 0.85f, onClick = onDismiss),
                )
                Spacer(Modifier.height(14.dp))
                Icon(
                    Icons.Filled.Navigation,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
