package com.dabber.traveldabble.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dabber.traveldabble.model.Place

enum class MapStyle(val label: String, val styleUrl: String) {
    Liberty("Liberty", "https://tiles.openfreemap.org/styles/liberty"),
    Positron("Positron", "https://tiles.openfreemap.org/styles/positron"),
    Bright("Bright", "https://tiles.openfreemap.org/styles/bright"),
    Dark("Dark", "https://tiles.openfreemap.org/styles/dark"),
    Fiord("Fiord", "https://tiles.openfreemap.org/styles/fiord"),
    ThreeD("3D", "https://tiles.openfreemap.org/styles/liberty"),
}

@Composable
expect fun TravelMap(
    places: List<Place>,
    style: MapStyle,
    modifier: Modifier = Modifier,
    tilt3d: Boolean = true,
    showPolylines: Boolean = true,
    showUserLocation: Boolean = false,
    autoCenterOnLocation: Boolean = false,
    onMarkerClick: (Place) -> Unit = {},
)
