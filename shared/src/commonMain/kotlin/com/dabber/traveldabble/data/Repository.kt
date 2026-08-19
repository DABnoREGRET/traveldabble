package com.dabber.traveldabble.data

import com.dabber.traveldabble.model.ActivityItem
import com.dabber.traveldabble.model.Budget
import com.dabber.traveldabble.model.DayPlan
import com.dabber.traveldabble.model.Destination
import com.dabber.traveldabble.model.Expense
import com.dabber.traveldabble.model.Place
import com.dabber.traveldabble.model.Trip
import com.dabber.traveldabble.model.TripMember
import com.dabber.traveldabble.ui.mock.MockData
import com.dabber.traveldabble.ui.mock.toDomain
import com.russhwolf.settings.Settings
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object Repository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val kvSettings by lazy {
        try {
            Settings()
        } catch (_: Throwable) {
            null
        }
    }

    private const val LOCAL_TRIPS_KEY = "traveldabble_local_user_trips"
    private const val DELETED_DEMO_TRIPS_KEY = "traveldabble_deleted_demo_trips"

    // In-memory cache synced with persistent Settings
    private val localUserTrips = mutableListOf<Trip>()
    private val deletedDemoTripIds = mutableSetOf<String>()

    private val MONTH_MAP = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
        "january" to 1, "february" to 2, "march" to 3, "april" to 4, "may" to 5, "june" to 6,
        "july" to 7, "august" to 8, "september" to 9, "october" to 10, "november" to 11, "december" to 12
    )

    fun parseDateStringToLocalDate(str: String): LocalDate? {
        val clean = str.trim().replace(",", "").replace("-", " ").replace("/", " ")
        if (clean.isBlank()) return null

        val isoParts = str.trim().split("-")
        if (isoParts.size == 3) {
            val y = isoParts[0].toIntOrNull()
            val m = isoParts[1].toIntOrNull()
            val d = isoParts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                return runCatching { LocalDate(y, m, d) }.getOrNull()
            }
        }

        val tokens = clean.split(" ").filter { it.isNotBlank() }
        if (tokens.size >= 2) {
            val m1 = MONTH_MAP[tokens[0].lowercase()]
            val m2 = MONTH_MAP[tokens[1].lowercase()]

            if (m1 != null) {
                val day = tokens[1].toIntOrNull() ?: 1
                val year = tokens.getOrNull(2)?.toIntOrNull() ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
                return runCatching { LocalDate(year, m1, day) }.getOrNull()
            } else if (m2 != null) {
                val day = tokens[0].toIntOrNull() ?: 1
                val year = tokens.getOrNull(2)?.toIntOrNull() ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
                return runCatching { LocalDate(year, m2, day) }.getOrNull()
            }
        }
        return null
    }

    init {
        loadPersistedData()
    }

    private fun loadPersistedData() {
        try {
            val storedTrips = kvSettings?.getStringOrNull(LOCAL_TRIPS_KEY)
            if (!storedTrips.isNullOrBlank()) {
                val decoded = json.decodeFromString<List<Trip>>(storedTrips)
                localUserTrips.clear()
                localUserTrips.addAll(decoded)
            }
            val storedDeleted = kvSettings?.getStringOrNull(DELETED_DEMO_TRIPS_KEY)
            if (!storedDeleted.isNullOrBlank()) {
                val decoded = json.decodeFromString<List<String>>(storedDeleted)
                deletedDemoTripIds.clear()
                deletedDemoTripIds.addAll(decoded)
            }
        } catch (_: Throwable) {}
    }

    private fun savePersistedData() {
        try {
            kvSettings?.putString(LOCAL_TRIPS_KEY, json.encodeToString(localUserTrips))
            kvSettings?.putString(DELETED_DEMO_TRIPS_KEY, json.encodeToString(deletedDemoTripIds.toList()))
        } catch (_: Throwable) {}
    }

    fun generateDayPlanLabels(startDate: String, endDate: String): List<String> {
        val start = parseDateStringToLocalDate(startDate)
        val end = parseDateStringToLocalDate(endDate)

        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        if (start != null && end != null && end >= start) {
            val startEpoch = start.toEpochDays()
            val endEpoch = end.toEpochDays()
            val totalDays = (endEpoch - startEpoch + 1).coerceIn(1, 30)

            return (0 until totalDays).map { offset ->
                val d = LocalDate.fromEpochDays(startEpoch + offset)
                val monthStr = monthNames.getOrElse(d.monthNumber - 1) { "Jan" }
                "$monthStr ${d.dayOfMonth}, ${d.year}"
            }
        } else if (start != null) {
            val startEpoch = start.toEpochDays()
            return (0 until 3).map { offset ->
                val d = LocalDate.fromEpochDays(startEpoch + offset)
                val monthStr = monthNames.getOrElse(d.monthNumber - 1) { "Jan" }
                "$monthStr ${d.dayOfMonth}, ${d.year}"
            }
        }

        if (startDate.isNotBlank() && endDate.isNotBlank() && startDate != endDate) {
            return listOf(startDate, "Day 2", endDate)
        }
        return listOf(startDate.ifBlank { "Day 1" }, "Day 2", "Day 3")
    }

    suspend fun addDayToTrip(tripId: String): DayPlan? {
        val trip = getTrip(tripId) ?: return null
        val nextDayNum = (trip.days.maxOfOrNull { it.dayNumber } ?: 0) + 1

        val startLocalDate = parseDateStringToLocalDate(trip.startDate)
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val dateLabel = if (startLocalDate != null) {
            val d = LocalDate.fromEpochDays(startLocalDate.toEpochDays() + (nextDayNum - 1))
            val monthStr = monthNames.getOrElse(d.monthNumber - 1) { "Jan" }
            "$monthStr ${d.dayOfMonth}, ${d.year}"
        } else {
            "Day $nextDayNum"
        }

        val remoteDay = if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            runCatching { ApiClient.addDayPlan(tripId, nextDayNum, dateLabel) }.getOrNull()
        } else null

        val newDay = remoteDay ?: DayPlan(
            id = "day_${System.currentTimeMillis()}",
            dayNumber = nextDayNum,
            dateLabel = dateLabel,
            activities = emptyList(),
        )

        saveTrip(trip.copy(days = trip.days + newDay))
        return newDay
    }

    private suspend fun ensureRemoteTripDays(trip: Trip): Trip {
        if (!AuthState.isLoggedIn || AuthState.isGuestMode || trip.days.isNotEmpty()) return trip
        val labels = generateDayPlanLabels(trip.startDate, trip.endDate)
        val days = labels.mapIndexedNotNull { index, label ->
            runCatching { ApiClient.addDayPlan(trip.id, index + 1, label) }.getOrNull()
        }
        return if (days.isNotEmpty()) trip.copy(days = days) else trip
    }

    suspend fun getTrips(): List<Trip> {
        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            val remote = try {
                ApiClient.getTrips()
            } catch (_: Exception) {
                emptyList()
            }
            if (remote.isNotEmpty()) {
                val remoteWithDays = remote.map { ensureRemoteTripDays(it) }
                val remoteIds = remoteWithDays.map { it.id }.toSet()
                val onlyLocal = localUserTrips.filterNot { it.id in remoteIds }
                return remoteWithDays + onlyLocal
            }
        }

        // Guest / Local Mode: check if Demo Mode is enabled
        return if (SettingsState.demoMode) {
            val userTripIds = localUserTrips.map { it.id }.toSet()
            val demoTrips = MockData.trips
                .map { it.toDomain() }
                .filterNot { it.id in deletedDemoTripIds || it.id in userTripIds }
            localUserTrips + demoTrips
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
            if (remote != null) return ensureRemoteTripDays(remote)
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
            remoteCreated = remoteCreated?.let { ensureRemoteTripDays(it) }
        }

        // Generate initial day structure
        val dayLabels = generateDayPlanLabels(startDate, endDate)
        val initialDays = dayLabels.mapIndexed { index, label ->
            DayPlan(dayNumber = index + 1, dateLabel = label, activities = emptyList())
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
            days = initialDays,
            budget = Budget(
                total = 1500.0,
                categories = listOf("Lodging" to 600.0, "Food" to 400.0, "Transport" to 300.0, "Activities" to 200.0),
                expenses = emptyList(),
            ),
        )

        localUserTrips.removeAll { it.id == newTrip.id }
        localUserTrips.add(0, newTrip)
        savePersistedData()
        return newTrip
    }

    suspend fun saveTrip(trip: Trip): Boolean {
        val index = localUserTrips.indexOfFirst { it.id == trip.id }
        if (index >= 0) {
            localUserTrips[index] = trip
        } else {
            localUserTrips.add(0, trip)
        }
        savePersistedData()
        return true
    }

    suspend fun updateTrip(
        id: String,
        title: String? = null,
        destination: String? = null,
        country: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        travelers: Int? = null,
    ): Trip? {
        val existing = getTrip(id) ?: return null
        val updated = existing.copy(
            title = title ?: existing.title,
            destination = destination ?: existing.destination,
            country = country ?: existing.country,
            startDate = startDate ?: existing.startDate,
            endDate = endDate ?: existing.endDate,
            travelers = travelers ?: existing.travelers,
        )
        saveTrip(updated)
        return updated
    }

    suspend fun addActivityToTrip(
        tripId: String,
        dayNumber: Int,
        place: Place,
        startTime: String = "09:00",
        endTime: String = "11:00",
        note: String? = null,
    ): Boolean {
        val trip = getTrip(tripId) ?: return false
        val day = trip.days.firstOrNull { it.dayNumber == dayNumber }
        val remoteActivity = if (AuthState.isLoggedIn && !AuthState.isGuestMode && day?.id != null) {
            runCatching {
                ApiClient.addActivity(tripId, day.id, place, startTime, endTime, note)
            }.getOrNull()
        } else {
            null
        }
        val newActivity = remoteActivity ?: ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            place = place,
            startTime = startTime,
            endTime = endTime,
            note = note,
        )

        val updatedDays = if (trip.days.none { it.dayNumber == dayNumber }) {
            trip.days + DayPlan(dayNumber = dayNumber, dateLabel = "Day $dayNumber", activities = listOf(newActivity))
        } else {
            trip.days.map { day ->
                if (day.dayNumber == dayNumber) {
                    day.copy(activities = day.activities + newActivity)
                } else {
                    day
                }
            }
        }

        saveTrip(trip.copy(days = updatedDays))
        return true
    }

    suspend fun removeActivityFromTrip(tripId: String, dayNumber: Int, activityId: String): Boolean {
        val trip = getTrip(tripId) ?: return false
        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            runCatching { ApiClient.removeActivity(tripId, activityId) }
        }
        val updatedDays = trip.days.map { day ->
            if (day.dayNumber == dayNumber) {
                day.copy(activities = day.activities.filterNot { it.id == activityId })
            } else {
                day
            }
        }
        saveTrip(trip.copy(days = updatedDays))
        return true
    }

    suspend fun addExpenseToTrip(
        tripId: String,
        title: String,
        category: String,
        amount: Double,
        date: String = "Today",
    ): Boolean {
        val trip = getTrip(tripId) ?: return false
        val remoteExpense = if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            runCatching { ApiClient.addExpense(tripId, title, category, amount, date) }.getOrNull()
        } else {
            null
        }
        val newExpense = remoteExpense ?: Expense(
            id = "exp_${System.currentTimeMillis()}",
            title = title,
            category = category,
            amount = amount,
            date = date,
        )
        val updatedExpenses = listOf(newExpense) + trip.budget.expenses

        val currentCategories = trip.budget.categories.toMap().toMutableMap()
        val currentCatAmt = currentCategories[category] ?: 0.0
        currentCategories[category] = currentCatAmt + amount

        val updatedBudget = trip.budget.copy(
            categories = currentCategories.toList(),
            expenses = updatedExpenses,
        )
        saveTrip(trip.copy(budget = updatedBudget))
        return true
    }

    suspend fun removeExpenseFromTrip(tripId: String, expenseId: String): Boolean {
        val trip = getTrip(tripId) ?: return false
        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            runCatching { ApiClient.removeExpense(tripId, expenseId) }
        }
        val removedExp = trip.budget.expenses.firstOrNull { it.id == expenseId }
        val updatedExpenses = trip.budget.expenses.filterNot { it.id == expenseId }

        val currentCategories = trip.budget.categories.toMap().toMutableMap()
        if (removedExp != null) {
            val currentCatAmt = currentCategories[removedExp.category] ?: 0.0
            val newCatAmt = (currentCatAmt - removedExp.amount).coerceAtLeast(0.0)
            if (newCatAmt > 0) {
                currentCategories[removedExp.category] = newCatAmt
            } else {
                currentCategories.remove(removedExp.category)
            }
        }

        val updatedBudget = trip.budget.copy(
            categories = currentCategories.toList(),
            expenses = updatedExpenses,
        )
        saveTrip(trip.copy(budget = updatedBudget))
        return true
    }

    suspend fun updateTripBudget(tripId: String, totalBudget: Double): Boolean {
        val trip = getTrip(tripId) ?: return false
        val ratio = if (trip.budget.total > 0) totalBudget / trip.budget.total else 1.0
        val updatedCategories = trip.budget.categories.map { (cat, amt) -> cat to (amt * ratio) }
        val updatedBudget = trip.budget.copy(
            total = totalBudget,
            categories = updatedCategories.ifEmpty {
                listOf("Lodging" to totalBudget * 0.4, "Food" to totalBudget * 0.25, "Transport" to totalBudget * 0.2, "Activities" to totalBudget * 0.15)
            }
        )
        saveTrip(trip.copy(budget = updatedBudget))
        return true
    }

    suspend fun clearAllData() {
        localUserTrips.clear()
        deletedDemoTripIds.clear()
        try {
            kvSettings?.remove(LOCAL_TRIPS_KEY)
            kvSettings?.remove(DELETED_DEMO_TRIPS_KEY)
        } catch (_: Throwable) {}
    }

    suspend fun deleteTrip(id: String): Boolean {
        localUserTrips.removeAll { it.id == id }
        deletedDemoTripIds.add(id)
        savePersistedData()

        if (AuthState.isLoggedIn && !AuthState.isGuestMode) {
            try {
                ApiClient.deleteTrip(id)
            } catch (_: Exception) {}
        }
        return true
    }

    suspend fun getDestinations(): List<Destination> {
        val remote = try {
            ApiClient.getDestinations()
        } catch (_: Exception) {
            emptyList()
        }
        if (remote.isNotEmpty()) return remote

        return MockData.destinations.map { it.toDomain() }
    }

    suspend fun getDestination(id: String): Destination? {
        val remote = try {
            ApiClient.getDestination(id)
        } catch (_: Exception) {
            null
        }
        if (remote != null) return remote

        return MockData.destinations.firstOrNull { it.id == id }?.toDomain()
    }

    suspend fun getPlaces(): List<Place> {
        val allTripsPlaces = getTrips().flatMap { it.days }.flatMap { it.activities }.map { it.place }
        val allMockPlaces = MockData.hanoiPlaces + MockData.centralPlaces + MockData.haGiangPlaces + MockData.saigonPlaces + MockData.ninhBinhPlaces
        return (allTripsPlaces + allMockPlaces).distinctBy { it.id }
    }

    suspend fun getPlace(id: String): Place? {
        val allTripsPlaces = getTrips().flatMap { it.days }.flatMap { it.activities }.map { it.place }
        allTripsPlaces.firstOrNull { it.id == id }?.let { return it }

        val allMockPlaces = MockData.hanoiPlaces + MockData.centralPlaces + MockData.haGiangPlaces + MockData.saigonPlaces + MockData.ninhBinhPlaces
        return allMockPlaces.firstOrNull { it.id == id }
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
