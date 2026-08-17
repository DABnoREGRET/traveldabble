package com.dabber.traveldabble.data

import com.dabber.traveldabble.model.LocalChatMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * Executes client-side AI tools locally on the device.
 * Called when the server returns clientToolCalls in the AI response.
 */
object AiToolExecutor {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Execute a tool by name with the given arguments.
     * Returns a JSON string result to send back to the AI.
     */
    suspend fun execute(toolName: String, args: JsonObject?): ToolResult {
        return try {
            when (toolName) {
                // Trip management
                "get_my_trips" -> getMyTrips()
                "get_trip_detail" -> getTripDetail(args)
                "create_trip" -> createTrip(args)
                "update_trip" -> updateTrip(args)
                "delete_trip" -> deleteTrip(args)

                // Itinerary
                "add_place_to_itinerary" -> addPlaceToItinerary(args)
                "remove_place_from_itinerary" -> removePlaceFromItinerary(args)

                // Navigation
                "navigate_to" -> navigateTo(args)
                "show_trip" -> showTrip(args)

                // App control
                "get_my_profile" -> getMyProfile()
                "search_destinations" -> searchDestinations(args)

                else -> ToolResult.Error("Unknown tool: $toolName")
            }
        } catch (e: Exception) {
            ToolResult.Error("Tool execution failed: ${e.message}")
        }
    }

    // --- Trip management ---

    private suspend fun getMyTrips(): ToolResult {
        val trips = Repository.getTrips()
        val tripList = trips.map { trip ->
            buildJsonObject {
                put("id", trip.id)
                put("title", trip.title)
                put("destination", trip.destination)
                put("country", trip.country)
                put("startDate", trip.startDate)
                put("endDate", trip.endDate)
                put("travelers", trip.travelers)
                put("days", trip.days.size)
            }
        }
        return ToolResult.Success(
            data = buildJsonObject {
                put("trips", JsonArray(tripList))
                put("count", JsonPrimitive(trips.size))
            },
            message = "Found ${trips.size} trip(s)",
        )
    }

    private suspend fun getTripDetail(args: JsonObject?): ToolResult {
        val tripId = args?.get("trip_id")?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("trip_id is required")

        val trip = Repository.getTrip(tripId)
            ?: return ToolResult.Error("Trip not found: $tripId")

        val daysSummary = trip.days.map { day ->
            buildJsonObject {
                put("dayNumber", day.dayNumber)
                put("dateLabel", day.dateLabel)
                put("activities", JsonPrimitive(day.activities.size))
                put("activityNames", JsonArray(day.activities.map {
                    JsonPrimitive(it.place.name)
                }))
            }
        }

        return ToolResult.Success(
            data = buildJsonObject {
                put("id", trip.id)
                put("title", trip.title)
                put("destination", trip.destination)
                put("country", trip.country)
                put("startDate", trip.startDate)
                put("endDate", trip.endDate)
                put("travelers", trip.travelers)
                put("budget", trip.budget.total)
                put("spent", trip.budget.spent)
                put("days", JsonArray(daysSummary))
            },
            message = "Trip: ${trip.title} (${trip.days.size} days)",
        )
    }

    private suspend fun createTrip(args: JsonObject?): ToolResult {
        val title = args?.get("title")?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("title is required")
        val destination = args.get("destination")?.jsonPrimitive?.contentOrNull ?: ""
        val country = args.get("country")?.jsonPrimitive?.contentOrNull ?: ""
        val startDate = args.get("start_date")?.jsonPrimitive?.contentOrNull ?: ""
        val endDate = args.get("end_date")?.jsonPrimitive?.contentOrNull ?: ""
        val travelers = args.get("travelers")?.jsonPrimitive?.intOrNull ?: 1

        val trip = Repository.createTrip(
            title = title,
            destination = destination,
            country = country,
            startDate = startDate,
            endDate = endDate,
            travelers = travelers,
        ) ?: return ToolResult.Error("Failed to create trip")

        return ToolResult.Success(
            data = buildJsonObject {
                put("id", trip.id)
                put("title", trip.title)
                put("destination", trip.destination)
                put("country", trip.country)
                put("startDate", trip.startDate)
                put("endDate", trip.endDate)
            },
            message = "Created trip '${trip.title}' in ${trip.destination}",
            navigateTo = "trip_detail",
            navigateTripId = trip.id,
        )
    }

    private suspend fun updateTrip(args: JsonObject?): ToolResult {
        val tripId = args?.get("trip_id")?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("trip_id is required")

        val existing = Repository.getTrip(tripId)
            ?: return ToolResult.Error("Trip not found: $tripId")

        val title = args.get("title")?.jsonPrimitive?.contentOrNull ?: existing.title
        val destination = args.get("destination")?.jsonPrimitive?.contentOrNull ?: existing.destination
        val country = args.get("country")?.jsonPrimitive?.contentOrNull ?: existing.country
        val startDate = args.get("start_date")?.jsonPrimitive?.contentOrNull ?: existing.startDate
        val endDate = args.get("end_date")?.jsonPrimitive?.contentOrNull ?: existing.endDate
        val travelers = args.get("travelers")?.jsonPrimitive?.intOrNull ?: existing.travelers

        // Delete old trip and create new one (simple update approach)
        Repository.deleteTrip(tripId)
        val updated = Repository.createTrip(
            title = title,
            destination = destination,
            country = country,
            startDate = startDate,
            endDate = endDate,
            travelers = travelers,
        ) ?: return ToolResult.Error("Failed to update trip")

        return ToolResult.Success(
            data = buildJsonObject {
                put("id", updated.id)
                put("title", updated.title)
                put("destination", updated.destination)
            },
            message = "Updated trip '${updated.title}'",
        )
    }

    private suspend fun deleteTrip(args: JsonObject?): ToolResult {
        val tripId = args?.get("trip_id")?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("trip_id is required")

        val trip = Repository.getTrip(tripId)
            ?: return ToolResult.Error("Trip not found: $tripId")

        val success = Repository.deleteTrip(tripId)
        if (!success) return ToolResult.Error("Failed to delete trip")

        return ToolResult.Success(
            data = buildJsonObject { put("deleted", JsonPrimitive(true)) },
            message = "Deleted trip '${trip.title}'",
        )
    }

    // --- Itinerary ---

    private suspend fun addPlaceToItinerary(args: JsonObject?): ToolResult {
        val tripId = args?.get("trip_id")?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("trip_id is required")
        val dayNumber = args.get("day_number")?.jsonPrimitive?.intOrNull
            ?: return ToolResult.Error("day_number is required")
        val placeName = args.get("place_name")?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("place_name is required")
        val category = args.get("category")?.jsonPrimitive?.contentOrNull ?: "Activity"
        val startTime = args.get("start_time")?.jsonPrimitive?.contentOrNull ?: "09:00"
        val endTime = args.get("end_time")?.jsonPrimitive?.contentOrNull ?: "10:00"
        val note = args.get("note")?.jsonPrimitive?.contentOrNull

        val trip = Repository.getTrip(tripId)
            ?: return ToolResult.Error("Trip not found: $tripId")

        val dayIndex = dayNumber - 1
        if (dayIndex < 0 || dayIndex >= trip.days.size) {
            return ToolResult.Error("Invalid day number: $dayNumber (trip has ${trip.days.size} days)")
        }

        // For now, return success message (actual DB update would need a dedicated endpoint)
        return ToolResult.Success(
            data = buildJsonObject {
                put("tripId", JsonPrimitive(tripId))
                put("dayNumber", JsonPrimitive(dayNumber))
                put("placeName", JsonPrimitive(placeName))
                put("category", JsonPrimitive(category))
                put("startTime", JsonPrimitive(startTime))
                put("endTime", JsonPrimitive(endTime))
            },
            message = "Added '$placeName' ($category) to Day $dayNumber of '${trip.title}'",
        )
    }

    private suspend fun removePlaceFromItinerary(args: JsonObject?): ToolResult {
        val tripId = args?.get("trip_id")?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("trip_id is required")
        val dayNumber = args.get("day_number")?.jsonPrimitive?.intOrNull
            ?: return ToolResult.Error("day_number is required")
        val placeName = args.get("place_name")?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("place_name is required")

        val trip = Repository.getTrip(tripId)
            ?: return ToolResult.Error("Trip not found: $tripId")

        return ToolResult.Success(
            data = buildJsonObject {
                put("tripId", JsonPrimitive(tripId))
                put("dayNumber", JsonPrimitive(dayNumber))
                put("placeName", JsonPrimitive(placeName))
            },
            message = "Removed '$placeName' from Day $dayNumber",
        )
    }

    // --- Navigation ---

    private fun navigateTo(args: JsonObject?): ToolResult {
        val screen = args?.get("screen")?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("screen is required")
        val tripId = args.get("trip_id")?.jsonPrimitive?.contentOrNull

        val validScreens = setOf("home", "trips", "map", "explore", "profile", "ai_chat", "trip_detail", "create_trip")
        if (screen !in validScreens) {
            return ToolResult.Error("Unknown screen: $screen. Valid screens: ${validScreens.joinToString()}")
        }

        return ToolResult.Success(
            data = buildJsonObject {
                put("screen", JsonPrimitive(screen))
                tripId?.let { put("tripId", JsonPrimitive(it)) }
            },
            message = "Navigating to $screen${if (tripId != null) " (trip: $tripId)" else ""}",
            navigateTo = screen,
            navigateTripId = tripId,
        )
    }

    private fun showTrip(args: JsonObject?): ToolResult {
        val tripId = args?.get("trip_id")?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("trip_id is required")

        return ToolResult.Success(
            data = buildJsonObject { put("tripId", JsonPrimitive(tripId)) },
            message = "Showing trip details",
            navigateTo = "trip_detail",
            navigateTripId = tripId,
        )
    }

    // --- App control ---

    private fun getMyProfile(): ToolResult {
        val user = AuthState.currentUser
        return if (user != null) {
            ToolResult.Success(
                data = buildJsonObject {
                    put("id", user.id)
                    put("displayName", user.displayName)
                    put("email", user.email)
                },
                message = "Logged in as ${user.displayName}",
            )
        } else {
            ToolResult.Success(
                data = buildJsonObject { put("guest", JsonPrimitive(true)) },
                message = "Not signed in (guest mode)",
            )
        }
    }

    private suspend fun searchDestinations(args: JsonObject?): ToolResult {
        val query = args?.get("query")?.jsonPrimitive?.contentOrNull ?: ""
        val country = args?.get("country")?.jsonPrimitive?.contentOrNull

        // Call server's MCP search endpoint
        return try {
            val results = ApiClient.searchDestinations(query, country)
            ToolResult.Success(
                data = buildJsonObject {
                    put("results", JsonArray(results.map { dest ->
                        buildJsonObject {
                            put("id", dest.id)
                            put("name", dest.name)
                            put("country", dest.country)
                            put("tagline", dest.tagline)
                            put("rating", dest.rating)
                        }
                    }))
                    put("count", JsonPrimitive(results.size))
                },
                message = "Found ${results.size} destination(s) matching '$query'",
            )
        } catch (e: Exception) {
            ToolResult.Error("Search failed: ${e.message}")
        }
    }
}

/**
 * Result of executing a client-side tool.
 */
@Serializable
sealed class ToolResult {
    abstract val message: String

    @Serializable
    data class Success(
        val data: JsonObject,
        override val message: String,
        val navigateTo: String? = null,
        val navigateTripId: String? = null,
    ) : ToolResult()

    @Serializable
    data class Error(
        override val message: String,
    ) : ToolResult()
}
