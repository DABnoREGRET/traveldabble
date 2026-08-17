package com.dabber.traveldabble.data

import com.dabber.traveldabble.model.Budget
import com.dabber.traveldabble.model.Destination
import com.dabber.traveldabble.model.Trip
import com.dabber.traveldabble.model.TripMember
import com.dabber.traveldabble.ui.mock.MockData
import com.dabber.traveldabble.ui.mock.toDomain

object Repository {
    // User-created trips in local storage
    private val localUserTrips = mutableListOf<Trip>()
    private val deletedDemoTripIds = mutableSetOf<String>()

    suspend fun getTrips(): List<Trip> {
        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            val remote = try {
                ApiClient.getTrips()
            } catch (_: Exception) {
                emptyList()
            }
            if (remote.isNotEmpty()) {
                val remoteIds = remote.map { it.id }.toSet()
                val onlyLocal = localUserTrips.filterNot { it.id in remoteIds }
                return remote + onlyLocal
            }
        }

        // Guest / Local Mode: check if Demo Mode is enabled
        return if (SettingsState.demoMode) {
            val demoTrips = MockData.trips
                .map { it.toDomain() }
                .filterNot { it.id in deletedDemoTripIds }
            val demoIds = demoTrips.map { it.id }.toSet()
            val userTrips = localUserTrips.filterNot { it.id in demoIds }
            userTrips + demoTrips
        } else {
            localUserTrips.toList()
        }
    }

    suspend fun getTrip(id: String): Trip? {
        val userTrip = localUserTrips.firstOrNull { it.id == id }
        if (userTrip != null) return userTrip

        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            val remote = try {
                ApiClient.getTrip(id)
            } catch (_: Exception) {
                null
            }
            if (remote != null) return remote
        }

        if (SettingsState.demoMode && id !in deletedDemoTripIds) {
            return MockData.trips.firstOrNull { it.id == id }?.toDomain()
        }

        return null
    }

    suspend fun createTrip(
        title: String, destination: String, country: String,
        startDate: String, endDate: String, travelers: Int
    ): Trip? {
        val coverOptions = listOf(
            listOf(0xFF059669.toInt(), 0xFF0D9488.toInt()), // Ocean
            listOf(0xFFF97316.toInt(), 0xFFDC2626.toInt()), // Sunset
            listOf(0xFFEAB308.toInt(), 0xFF059669.toInt()), // Alpine
            listOf(0xFF7C3AED.toInt(), 0xFFDC2626.toInt()), // Royal
            listOf(0xFF059669.toInt(), 0xFF047857.toInt()), // Forest
        )
        val cover = coverOptions[localUserTrips.size % coverOptions.size]

        var remoteCreated: Trip? = null
        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            remoteCreated = try {
                ApiClient.createTrip(CreateTripRequest(title, destination, country, startDate, endDate, travelers))
            } catch (_: Exception) {
                null
            }
        }

        val newTrip = remoteCreated ?: Trip(
            id = "trip_${System.currentTimeMillis()}",
            title = title,
            destination = destination,
            country = country,
            startDate = startDate,
            endDate = endDate,
            daysUntil = 14,
            cover = cover,
            travelers = travelers,
            days = emptyList(),
            budget = Budget(
                total = 1500.0,
                categories = listOf("Lodging" to 600.0, "Food" to 400.0, "Transport" to 300.0, "Activities" to 200.0),
                expenses = emptyList(),
            ),
        )

        localUserTrips.add(0, newTrip)
        return newTrip
    }

    suspend fun deleteTrip(id: String): Boolean {
        localUserTrips.removeAll { it.id == id }
        deletedDemoTripIds.add(id)

        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            try {
                ApiClient.deleteTrip(id)
            } catch (_: Exception) {}
        }
        return true
    }

    suspend fun getDestinations(): List<Destination> {
        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            val remote = try {
                ApiClient.getDestinations()
            } catch (_: Exception) {
                emptyList()
            }
            if (remote.isNotEmpty()) return remote
        }

        return if (SettingsState.demoMode) {
            MockData.destinations.map { it.toDomain() }
        } else {
            emptyList()
        }
    }

    suspend fun getDestination(id: String): Destination? {
        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            val remote = try {
                ApiClient.getDestination(id)
            } catch (_: Exception) {
                null
            }
            if (remote != null) return remote
        }

        if (SettingsState.demoMode) {
            return MockData.destinations.firstOrNull { it.id == id }?.toDomain()
        }

        return null
    }

    // Group Trip methods
    suspend fun getTripMembers(tripId: String): List<TripMember> = try {
        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            ApiClient.getTripMembers(tripId)
        } else {
            listOf(
                TripMember(
                    userId = "local_user",
                    displayName = AuthState.currentUser?.displayName ?: "Me (Organizer)",
                    email = AuthState.currentUser?.email ?: "me@traveldabble.local",
                    role = "owner",
                    joinedAt = System.currentTimeMillis(),
                )
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun generateInviteCode(tripId: String, maxUses: Int? = null, expiresInHours: Int? = null): String? = try {
        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            val result = ApiClient.generateInviteCode(tripId, maxUses, expiresInHours)
            result.code
        } else {
            "VN" + (1000..9999).random()
        }
    } catch (_: Exception) {
        "VN" + (1000..9999).random()
    }

    suspend fun joinTrip(code: String): String? = try {
        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            val result = ApiClient.joinTrip(code)
            result.tripId
        } else {
            localUserTrips.firstOrNull()?.id
        }
    } catch (_: Exception) {
        null
    }

    suspend fun removeMember(tripId: String, memberUserId: String): Boolean = try {
        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            ApiClient.removeMember(tripId, memberUserId)
        }
        true
    } catch (_: Exception) {
        false
    }
}
