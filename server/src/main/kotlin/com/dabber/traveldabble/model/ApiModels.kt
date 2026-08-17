package com.dabber.traveldabble.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val username: String, val email: String, val password: String, val displayName: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val userId: String, val displayName: String, val email: String)

@Serializable
data class CreateTripRequest(val title: String, val destination: String, val country: String, val startDate: String, val endDate: String, val travelers: Int)

@Serializable
data class CreatePlaceRequest(val name: String, val category: String, val lat: Double, val lng: Double, val rating: Float, val description: String, val openHours: String)

@Serializable
data class AddDayPlanRequest(val dayNumber: Int, val dateLabel: String)

@Serializable
data class PlaceInput(
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val rating: Float,
    val description: String,
    val openHours: String,
)

@Serializable
data class AddActivityRequest(
    val place: PlaceInput,
    val startTime: String,
    val endTime: String,
    val note: String? = null,
)

@Serializable
data class UpdateActivityRequest(
    val startTime: String? = null,
    val endTime: String? = null,
    val note: String? = null,
)

@Serializable
data class UpdateBudgetRequest(
    val total: Double,
    val categories: List<Pair<String, Double>> = emptyList(),
)

@Serializable
data class AddExpenseRequest(
    val title: String,
    val category: String,
    val amount: Double,
    val date: String,
)

@Serializable
data class GenerateInviteCodeRequest(
    val maxUses: Int? = null,
    val expiresInHours: Int? = null,
)

@Serializable
data class JoinTripRequest(
    val code: String,
)

@Serializable
data class FcmTokenRequest(
    val token: String,
    val platform: String = "android",
)

@Serializable
data class ApiError(val error: String)

@Serializable
data class TelemetryEventRequest(
    val eventType: String,
    val screenName: String? = null,
    val durationMs: Long? = null,
    val connectionType: String? = null,
    val memoryMb: Int? = null,
    val exceptionHash: String? = null,
    val metadata: String? = null,
    val optOut: Boolean = false,
)

@Serializable
data class EndpointUsage(
    val endpoint: String,
    val count: Long,
    val avgResponseTimeMs: Double,
)

@Serializable
data class AiTelemetrySummary(
    val totalAiRequests: Long,
    val avgLatencyMs: Double,
    val totalToolCalls: Long,
)

@Serializable
data class ClientTelemetrySummary(
    val totalClientEvents: Long,
    val avgColdStartMs: Double,
    val reportedCrashes: Long,
)

@Serializable
data class UsageSummary(
    val date: String,
    val totalCalls: Long,
    val uniqueUsers: Long,
    val avgResponseTimeMs: Double,
    val topEndpoints: List<EndpointUsage>,
    val p50ResponseTimeMs: Double = 0.0,
    val p95ResponseTimeMs: Double = 0.0,
    val p99ResponseTimeMs: Double = 0.0,
    val errorRate: Double = 0.0,
    val slowEndpoints: List<EndpointUsage> = emptyList(),
    val aiMetrics: AiTelemetrySummary? = null,
    val clientMetrics: ClientTelemetrySummary? = null,
    val optOutNote: String = "All metrics exclude opt-out requests",
)
