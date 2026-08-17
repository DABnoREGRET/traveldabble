package com.dabber.traveldabble.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dabber.traveldabble.model.PlaceCategory

val PlaceCategory.icon: ImageVector
    get() = when (this) {
        PlaceCategory.SIGHT -> Icons.Filled.Attractions
        PlaceCategory.FOOD -> Icons.Filled.Restaurant
        PlaceCategory.STAY -> Icons.Filled.Hotel
        PlaceCategory.TRANSIT -> Icons.Filled.DirectionsTransit
        PlaceCategory.ACTIVITY -> Icons.Filled.LocalActivity
    }

val PlaceCategory.tint: Color
    get() = when (this) {
        PlaceCategory.SIGHT -> Color(0xFF2DD4BF)
        PlaceCategory.FOOD -> Color(0xFFFBBF24)
        PlaceCategory.STAY -> Color(0xFF8B5CF6)
        PlaceCategory.TRANSIT -> Color(0xFF38BDF8)
        PlaceCategory.ACTIVITY -> Color(0xFFF472B6)
    }
