package com.dabber.traveldabble.ui.mock

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dabber.traveldabble.model.PlaceCategory
import com.dabber.traveldabble.model.Trip as DomainTrip
import com.dabber.traveldabble.model.Destination as DomainDestination

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

data class Trip(
    val id: String,
    val title: String,
    val destination: String,
    val country: String,
    val startDate: String,
    val endDate: String,
    val daysUntil: Int?,
    val coverColors: List<Color>,
    val travelers: Int,
    val days: List<com.dabber.traveldabble.model.DayPlan>,
    val budget: com.dabber.traveldabble.model.Budget,
)

data class Destination(
    val id: String,
    val name: String,
    val country: String,
    val tagline: String,
    val rating: Float,
    val tags: List<String>,
    val coverColors: List<Color>,
)

fun DomainTrip.toUi() = Trip(
    id = id,
    title = title,
    destination = destination,
    country = country,
    startDate = startDate,
    endDate = endDate,
    daysUntil = daysUntil,
    coverColors = cover.map { Color(it) },
    travelers = travelers,
    days = days,
    budget = budget,
)

fun Trip.toDomain() = DomainTrip(
    id = id,
    title = title,
    destination = destination,
    country = country,
    startDate = startDate,
    endDate = endDate,
    daysUntil = daysUntil,
    cover = coverColors.map {
        val a = (it.alpha * 255.0f + 0.5f).toInt()
        val r = (it.red * 255.0f + 0.5f).toInt()
        val g = (it.green * 255.0f + 0.5f).toInt()
        val b = (it.blue * 255.0f + 0.5f).toInt()
        (a shl 24) or (r shl 16) or (g shl 8) or b
    },
    travelers = travelers,
    days = days,
    budget = budget,
)

fun DomainDestination.toUi() = Destination(
    id = id,
    name = name,
    country = country,
    tagline = tagline,
    rating = rating,
    tags = tags,
    coverColors = cover.map { Color(it) },
)

fun Destination.toDomain() = DomainDestination(
    id = id,
    name = name,
    country = country,
    tagline = tagline,
    rating = rating,
    tags = tags,
    cover = coverColors.map {
        val a = (it.alpha * 255.0f + 0.5f).toInt()
        val r = (it.red * 255.0f + 0.5f).toInt()
        val g = (it.green * 255.0f + 0.5f).toInt()
        val b = (it.blue * 255.0f + 0.5f).toInt()
        (a shl 24) or (r shl 16) or (g shl 8) or b
    },
)

