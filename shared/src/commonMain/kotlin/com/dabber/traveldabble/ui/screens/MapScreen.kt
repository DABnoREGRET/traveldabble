package com.dabber.traveldabble.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.data.SettingsState
import com.dabber.traveldabble.data.ThemeMode
import com.dabber.traveldabble.model.DayPlan
import com.dabber.traveldabble.model.Place
import com.dabber.traveldabble.model.PlaceCategory
import com.dabber.traveldabble.ui.components.CategoryBadge
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.glass.glass
import com.dabber.traveldabble.ui.map.MapStyle
import com.dabber.traveldabble.ui.map.TravelMap
import com.dabber.traveldabble.ui.mock.Trip
import com.dabber.traveldabble.ui.mock.icon
import com.dabber.traveldabble.ui.mock.tint
import com.dabber.traveldabble.ui.mock.toUi
import com.dabber.traveldabble.ui.theme.AuroraBlue
import com.dabber.traveldabble.ui.theme.AuroraGold
import com.dabber.traveldabble.ui.theme.AuroraTeal

/**
 * Google Maps styled interactive Map screen with:
 * - Floating top search/trip header with category filter pills
 * - Right-hand side floating control stack (3D/2D, Layer switcher, GPS My Location)
 * - Day-by-Day Itinerary drawer with route overview and step-by-step navigation
 * - Google Maps styled teardrop pin markers
 */
@Composable
fun MapScreen(
    tripId: String? = null,
    onPlaceClick: (String) -> Unit = {},
) {
    var trip by remember { mutableStateOf<Trip?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var allPlaces by remember { mutableStateOf<List<Place>>(emptyList()) }
    var selectedTripId by remember { mutableStateOf(tripId) }
    var showTripSelector by remember { mutableStateOf(false) }
    var showLayerMenu by remember { mutableStateOf(false) }

    // Day-by-day route filter: null = all days in trip, 1 = Day 1, 2 = Day 2...
    var selectedDayNumber by remember { mutableStateOf<Int?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<PlaceCategory?>(null) }
    var isDrawerExpanded by remember { mutableStateOf(true) }

    // Map style automatically follows app theme or user preference
    val defaultStyle = remember {
        when (SettingsState.themeMode) {
            ThemeMode.Dark -> MapStyle.Dark
            else -> MapStyle.Liberty
        }
    }
    var style by remember { mutableStateOf(defaultStyle) }
    var tilt3d by remember { mutableStateOf(true) }
    var showPolylines by remember { mutableStateOf(true) }
    var showUserLocation by remember { mutableStateOf(true) }
    var autoCenter by remember { mutableStateOf(true) }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }

    LaunchedEffect(tripId) {
        selectedTripId = tripId
        val loadedTrips = Repository.getTrips().map { it.toUi() }
        trips = loadedTrips
        allPlaces = loadedTrips.flatMap { t ->
            t.days.flatMap { it.activities }.map { it.place }
        }.distinctBy { it.id }

        if (tripId != null) {
            trip = Repository.getTrip(tripId)?.toUi()
        }
    }

    LaunchedEffect(selectedTripId) {
        selectedDayNumber = null
        trip = if (selectedTripId == null) {
            null
        } else {
            Repository.getTrip(selectedTripId!!)?.toUi()
        }
    }

    val loadedTrip = trip

    // Filter places based on selected trip, selected day, and category filter
    val currentDayPlan: DayPlan? = remember(loadedTrip, selectedDayNumber) {
        if (loadedTrip != null && selectedDayNumber != null) {
            loadedTrip.days.firstOrNull { it.dayNumber == selectedDayNumber }
        } else null
    }

    val placesForMap = remember(loadedTrip, selectedDayNumber, selectedCategoryFilter, allPlaces) {
        val basePlaces = when {
            currentDayPlan != null -> currentDayPlan.activities.map { it.place }
            loadedTrip != null -> loadedTrip.days.flatMap { it.activities }.map { it.place }.distinctBy { it.id }
            else -> allPlaces
        }

        if (selectedCategoryFilter != null) {
            basePlaces.filter { it.category == selectedCategoryFilter }
        } else {
            basePlaces
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Google Maps Vector Surface
        TravelMap(
            places = placesForMap,
            style = style,
            modifier = Modifier.fillMaxSize(),
            tilt3d = tilt3d,
            showPolylines = showPolylines && (loadedTrip != null || currentDayPlan != null),
            showUserLocation = showUserLocation,
            autoCenterOnLocation = autoCenter,
            onMarkerClick = {
                selectedPlace = it
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
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 10.dp,
                intensity = GlassIntensity.Prominent,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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

            // Quick Category Filters (Google Maps Pill Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GlassChip(
                    label = "All (${if (loadedTrip != null) loadedTrip.days.flatMap { it.activities }.size else allPlaces.size})",
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

        // 3. Right-Hand Side Floating Action Control Stack (Google Maps Style)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 110.dp, end = 16.dp),
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
                onClick = { autoCenter = true },
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
            .glass(CircleShape, GlassIntensity.Prominent),
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
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        intensity = GlassIntensity.Prominent,
        contentPadding = 12.dp,
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
            .glass(RoundedCornerShape(16.dp), GlassIntensity.Standard)
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
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpen,
        intensity = GlassIntensity.Prominent,
        contentPadding = 12.dp,
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
