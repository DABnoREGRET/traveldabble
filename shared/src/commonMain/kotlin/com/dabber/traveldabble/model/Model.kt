package com.dabber.traveldabble.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlaceCategory(val label: String) {
    SIGHT("Sight"),
    FOOD("Food"),
    STAY("Stay"),
    TRANSIT("Transit"),
    ACTIVITY("Activity");
}

@Serializable
data class Place(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val lat: Double,
    val lng: Double,
    val rating: Float,
    val description: String,
    val openHours: String = "9:00 - 18:00",
)

@Serializable
data class ActivityItem(
    val id: String,
    val place: Place,
    val startTime: String,
    val endTime: String,
    val note: String? = null,
)

@Serializable
data class DayPlan(
    val dayNumber: Int,
    val dateLabel: String,
    val activities: List<ActivityItem>,
)

@Serializable
data class Trip(
    val id: String,
    val title: String,
    val destination: String,
    val country: String,
    val startDate: String,
    val endDate: String,
    val daysUntil: Int?,
    val cover: List<Int>,
    val travelers: Int,
    val days: List<DayPlan>,
    val budget: Budget,
)

@Serializable
data class Expense(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val date: String,
)

@Serializable
data class Budget(
    val total: Double,
    val categories: List<Pair<String, Double>>,
    val expenses: List<Expense>,
) {
    val spent: Double get() = expenses.sumOf { it.amount }
}

@Serializable
data class Destination(
    val id: String,
    val name: String,
    val country: String,
    val tagline: String,
    val rating: Float,
    val tags: List<String>,
    val cover: List<Int>,
)

@Serializable
data class TripMember(
    val userId: String,
    val displayName: String,
    val email: String,
    val role: String, // "owner", "editor", "viewer"
    val joinedAt: Long,
)

@Serializable
data class InviteCode(
    val id: String,
    val code: String,
    val tripId: String,
    val createdBy: String,
    val createdAt: Long,
    val expiresAt: Long? = null,
    val maxUses: Int? = null,
    val useCount: Int = 0,
)

@Serializable
data class ChatMessage(
    val id: String,
    val fromAi: Boolean,
    val text: String,
    val attachment: ChatAttachment? = null,
)

@Serializable
data class ChatAttachment(
    val title: String,
    val rows: List<Pair<String, String>>,
)

@Serializable
data class LocalChatMessage(
    val id: String,
    val tripId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
)

@Serializable
data class Route(
    val geometry: String? = null,
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val legs: List<RouteLeg> = emptyList(),
    val weight_name: String? = null,
    val weight: Double = 0.0,
)

@Serializable
data class RouteLeg(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val summary: String = "",
    val steps: List<RouteStep> = emptyList(),
)

@Serializable
data class RouteStep(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val geometry: String? = null,
    val name: String = "",
    val mode: String = "",
    val maneuver: RouteManeuver? = null,
)

@Serializable
data class RouteManeuver(
    val type: String = "",
    val modifier: String? = null,
    val location: List<Double> = emptyList(),
    val instruction: String = "",
)

@Serializable
data class Item(
    val id: String,
    val name: String,
    val createdAt: Long,
)

@Serializable
data class User(
    val id: String,
    val username: String,
    val email: String,
    val displayName: String,
    val createdAt: Long,
)

@Serializable
data class InAppNotification(
    val id: Long,
    val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val data: String? = null,
    val read: Boolean = false,
    val createdAt: Long,
)
