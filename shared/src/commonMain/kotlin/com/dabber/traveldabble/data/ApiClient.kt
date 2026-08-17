package com.dabber.traveldabble.data

import com.dabber.traveldabble.model.Destination
import com.dabber.traveldabble.model.InviteCode
import com.dabber.traveldabble.model.Trip
import com.dabber.traveldabble.model.TripMember
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
data class GenerateInviteRequest(val maxUses: Int? = null, val expiresInHours: Int? = null)

@Serializable
data class InviteResponse(val id: String, val code: String, val expiresAt: Long? = null)

@Serializable
data class InviteInfo(val id: String, val code: String, val createdAt: Long, val expiresAt: Long? = null, val maxUses: Int? = null, val useCount: Int = 0)

@Serializable
data class JoinTripRequest(val code: String)

@Serializable
data class JoinTripResponse(val tripId: String, val message: String)

@Serializable
data class UpdateMemberRoleRequest(val role: String)

@Serializable
data class ApiError(val error: String)

object ApiClient {
    private val defaultHttpClient by lazy { createHttpClient() }
    private var customHttpClient: io.ktor.client.HttpClient? = null

    val httpClient: io.ktor.client.HttpClient
        get() = customHttpClient ?: defaultHttpClient

    fun setMockHttpClient(client: io.ktor.client.HttpClient?) {
        customHttpClient = client
    }

    /**
     * Base URL for the server API.
     * Uses customServerUrl if configured in Settings, or falls back to platform DEFAULT_BASE_URL.
     */
    val baseUrl: String
        get() = SettingsState.customServerUrl?.trimEnd('/')?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL

    private var authToken: String? = null

    /** When true, all requests include X-Telemetry-Opt-Out: true */
    var telemetryOptOut: Boolean = false

    fun setToken(token: String?) {
        authToken = token
    }

    fun getToken(): String? = authToken

    /**
     * Tests connectivity to the server's /health endpoint.
     */
    suspend fun testConnection(targetUrl: String? = null): Boolean {
        val url = targetUrl?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() } ?: baseUrl
        return try {
            val response: HttpResponse = httpClient.get("$url/health")
            response.status.value in 200..299
        } catch (_: Exception) {
            false
        }
    }

    private fun HttpRequestBuilder.authHeader() {
        authToken?.let { header("Authorization", "Bearer $it") }
        if (telemetryOptOut) {
            header("X-Telemetry-Opt-Out", "true")
        }
    }

    // Auth endpoints
    suspend fun register(username: String, email: String, password: String, displayName: String): AuthResponse {
        val response: HttpResponse = httpClient.post("$baseUrl/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username, email, password, displayName))
        }
        return response.body()
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val response: HttpResponse = httpClient.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }
        return response.body()
    }

    suspend fun getMe(): AuthResponse {
        val response: HttpResponse = httpClient.get("$baseUrl/api/auth/me") {
            authHeader()
        }
        return response.body()
    }

    // Trip endpoints
    suspend fun getTrips(): List<Trip> {
        val response: HttpResponse = httpClient.get("$baseUrl/api/trips") {
            authHeader()
        }
        return response.body()
    }

    suspend fun getTrip(id: String): Trip {
        val response: HttpResponse = httpClient.get("$baseUrl/api/trips/$id") {
            authHeader()
        }
        return response.body()
    }

    suspend fun createTrip(request: CreateTripRequest): Trip {
        val response: HttpResponse = httpClient.post("$baseUrl/api/trips") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }

    suspend fun deleteTrip(id: String) {
        httpClient.delete("$baseUrl/api/trips/$id") {
            authHeader()
        }
    }

    // Trip Member endpoints
    suspend fun getTripMembers(tripId: String): List<TripMember> {
        val response: HttpResponse = httpClient.get("$baseUrl/api/trips/$tripId/members") {
            authHeader()
        }
        return response.body()
    }

    suspend fun generateInviteCode(tripId: String, maxUses: Int? = null, expiresInHours: Int? = null): InviteResponse {
        val response: HttpResponse = httpClient.post("$baseUrl/api/trips/$tripId/invite") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(GenerateInviteRequest(maxUses, expiresInHours))
        }
        return response.body()
    }

    suspend fun getInviteCodes(tripId: String): List<InviteInfo> {
        val response: HttpResponse = httpClient.get("$baseUrl/api/trips/$tripId/invite") {
            authHeader()
        }
        return response.body()
    }

    suspend fun deleteInviteCode(tripId: String, inviteId: String) {
        httpClient.delete("$baseUrl/api/trips/$tripId/invite/$inviteId") {
            authHeader()
        }
    }

    suspend fun joinTrip(code: String): JoinTripResponse {
        val response: HttpResponse = httpClient.post("$baseUrl/api/trips/join") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(JoinTripRequest(code))
        }
        return response.body()
    }

    suspend fun updateMemberRole(tripId: String, memberUserId: String, role: String) {
        httpClient.put("$baseUrl/api/trips/$tripId/members/$memberUserId") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(UpdateMemberRoleRequest(role))
        }
    }

    suspend fun removeMember(tripId: String, memberUserId: String) {
        httpClient.delete("$baseUrl/api/trips/$tripId/members/$memberUserId") {
            authHeader()
        }
    }

    // Destination endpoints
    suspend fun getDestinations(): List<Destination> {
        val response: HttpResponse = httpClient.get("$baseUrl/api/destinations") {
            authHeader()
        }
        return response.body()
    }

    suspend fun getDestination(id: String): Destination {
        val response: HttpResponse = httpClient.get("$baseUrl/api/destinations/$id") {
            authHeader()
        }
        return response.body()
    }

    suspend fun searchDestinations(query: String, country: String? = null): List<Destination> {
        val allDestinations = getDestinations()
        return allDestinations.filter { dest ->
            val matchesQuery = query.isBlank() ||
                dest.name.contains(query, ignoreCase = true) ||
                dest.country.contains(query, ignoreCase = true) ||
                dest.tagline.contains(query, ignoreCase = true)
            val matchesCountry = country.isNullOrBlank() ||
                dest.country.contains(country, ignoreCase = true)
            matchesQuery && matchesCountry
        }
    }
}
