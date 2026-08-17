package com.dabber.traveldabble.db

import com.dabber.traveldabble.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*

object Mappers {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val defaultCover: List<Int> = listOf(0xFF8B5CF6.toInt(), 0xFFEC4899.toInt())

    fun ResultRow.toPlace(): Place = Place(
        id = this[Places.id].value.toString(),
        name = this[Places.name],
        category = PlaceCategory.entries.firstOrNull { it.name == this[Places.category] } ?: PlaceCategory.SIGHT,
        lat = this[Places.lat],
        lng = this[Places.lng],
        rating = this[Places.rating].toFloat(),
        description = this[Places.description],
        openHours = this[Places.openHours],
    )

    fun ResultRow.toExpense(): Expense = Expense(
        id = this[Expenses.id].value.toString(),
        title = this[Expenses.title],
        category = this[Expenses.category],
        amount = this[Expenses.amount],
        date = this[Expenses.date],
    )

    fun ResultRow.toBudget(): Budget {
        val budgetId = this[Budgets.id].value
        val expenses = Expenses.selectAll()
            .where { Expenses.budgetId eq budgetId }
            .map { it.toExpense() }
        return Budget(
            total = this[Budgets.total],
            categories = json.decodeFromString(this[Budgets.categories]),
            expenses = expenses,
        )
    }

    fun ResultRow.toActivity(place: Place): ActivityItem = ActivityItem(
        id = this[Activities.id].value.toString(),
        place = place,
        startTime = this[Activities.startTime],
        endTime = this[Activities.endTime],
        note = this[Activities.note],
    )

    fun ResultRow.toDayPlan(): DayPlan = DayPlan(
        dayNumber = this[DayPlans.dayNumber],
        dateLabel = this[DayPlans.dateLabel],
        activities = emptyList(),
    )

    fun ResultRow.toActivityRow(placeId: java.util.UUID): ActivityItem {
        val place = Places.selectAll()
            .where { Places.id eq placeId }
            .singleOrNull()
            ?.toPlace()
            ?: Place(id = placeId.toString(), name = "Unknown", category = PlaceCategory.SIGHT, lat = 0.0, lng = 0.0, rating = 0f, description = "", openHours = "")
        return ActivityItem(
            id = this[Activities.id].value.toString(),
            place = place,
            startTime = this[Activities.startTime],
            endTime = this[Activities.endTime],
            note = this[Activities.note],
        )
    }

    fun ResultRow.toTrip(): Trip {
        val tripId = this[Trips.id].value
        val days = DayPlans.selectAll()
            .where { DayPlans.tripId eq tripId }
            .orderBy(DayPlans.dayNumber)
            .map { dayRow ->
                val dayPlanId = dayRow[DayPlans.id].value
                val activities = Activities.selectAll()
                    .where { Activities.dayPlanId eq dayPlanId }
                    .orderBy(Activities.startTime)
                    .mapNotNull { activityRow ->
                        val place = Places.selectAll()
                            .where { Places.id eq activityRow[Activities.placeId].value }
                            .singleOrNull()
                            ?.toPlace()
                            ?: return@mapNotNull null
                        activityRow.toActivity(place)
                    }
                DayPlan(
                    dayNumber = dayRow[DayPlans.dayNumber],
                    dateLabel = dayRow[DayPlans.dateLabel],
                    activities = activities,
                )
            }
        val budget = Budgets.selectAll()
            .where { Budgets.tripId eq tripId }
            .singleOrNull()
            ?.toBudget()
            ?: Budget(total = 0.0, categories = emptyList(), expenses = emptyList())
        return Trip(
            id = tripId.toString(),
            title = this[Trips.title],
            destination = this[Trips.destination],
            country = this[Trips.country],
            startDate = this[Trips.startDate],
            endDate = this[Trips.endDate],
            daysUntil = this[Trips.daysUntil],
            cover = json.decodeFromString(this[Trips.coverColors]),
            travelers = this[Trips.travelers],
            days = days,
            budget = budget,
        )
    }

    fun ResultRow.toDestination(): Destination = Destination(
        id = this[Destinations.id].value.toString(),
        name = this[Destinations.name],
        country = this[Destinations.country],
        tagline = this[Destinations.tagline],
        rating = this[Destinations.rating].toFloat(),
        tags = json.decodeFromString(this[Destinations.tags]),
        cover = json.decodeFromString(this[Destinations.coverColors]),
    )

    fun ResultRow.toTripMember(): TripMember = TripMember(
        userId = this[TripMembers.userId].value.toString(),
        displayName = this.getOrNull(Users.displayName) ?: "Traveler",
        email = this.getOrNull(Users.email) ?: "",
        role = this[TripMembers.role],
        joinedAt = this[TripMembers.joinedAt],
    )

    fun ResultRow.toInviteCode(): InviteCode = InviteCode(
        id = this[InviteCodes.id].value.toString(),
        code = this[InviteCodes.code],
        tripId = this[InviteCodes.tripId].value.toString(),
        createdBy = this[InviteCodes.createdBy].value.toString(),
        createdAt = this[InviteCodes.createdAt],
        expiresAt = this[InviteCodes.expiresAt],
        maxUses = this[InviteCodes.maxUses],
        useCount = this[InviteCodes.useCount],
    )

    fun ResultRow.toNotification(): InAppNotification = InAppNotification(
        id = this[Notifications.id],
        userId = this[Notifications.userId],
        type = this[Notifications.type],
        title = this[Notifications.title],
        body = this[Notifications.body],
        data = this[Notifications.data],
        read = this[Notifications.read],
        createdAt = this[Notifications.createdAt],
    )
}
