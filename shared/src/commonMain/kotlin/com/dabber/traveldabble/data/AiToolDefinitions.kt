package com.dabber.traveldabble.data

import kotlinx.serialization.json.*

/**
 * Client-side tool definitions for AI function calling.
 * These tools are executed locally on the device (trip CRUD, navigation, app control).
 * Server-side tools (destination search) are handled by the server.
 */
object AiToolDefinitions {

    /**
     * Serialize all client tool definitions as a JSON string
     * to send to the server's /api/ai/chat endpoint.
     */
    fun toJson(): String {
        val tools = JsonArray(listOf(
            // --- Trip management ---
            getMyTrips,
            getTripDetail,
            createTrip,
            updateTrip,
            deleteTrip,

            // --- Itinerary management ---
            addPlaceToItinerary,
            removePlaceFromItinerary,

            // --- Navigation ---
            navigateTo,
            showTrip,

            // --- App control ---
            getMyProfile,
            searchDestinations,
        ))
        return Json.encodeToString(JsonArray.serializer(), tools)
    }

    // --- Trip management ---

    private val getMyTrips = buildTool(
        name = "get_my_trips",
        description = "Get all trips for the current user. Use when the user asks about their trips, wants to see their trip list, or needs to reference existing trips.",
        properties = buildJsonObject {},
    )

    private val getTripDetail = buildTool(
        name = "get_trip_detail",
        description = "Get detailed information about a specific trip including full itinerary and budget.",
        properties = buildJsonObject {
            putJsonObject("trip_id") {
                put("type", "string")
                put("description", "The trip ID")
            }
        },
        required = listOf("trip_id"),
    )

    private val createTrip = buildTool(
        name = "create_trip",
        description = "Create a new travel trip. Use when the user wants to plan a new trip.",
        properties = buildJsonObject {
            putJsonObject("title") {
                put("type", "string")
                put("description", "Trip title (e.g., 'Tokyo Adventure', 'Vietnam 2-week tour')")
            }
            putJsonObject("destination") {
                put("type", "string")
                put("description", "Primary destination name (e.g., 'Tokyo', 'Ha Long Bay')")
            }
            putJsonObject("country") {
                put("type", "string")
                put("description", "Country name (e.g., 'Japan', 'Vietnam')")
            }
            putJsonObject("start_date") {
                put("type", "string")
                put("description", "Start date in YYYY-MM-DD format")
            }
            putJsonObject("end_date") {
                put("type", "string")
                put("description", "End date in YYYY-MM-DD format")
            }
            putJsonObject("travelers") {
                put("type", "integer")
                put("description", "Number of travelers (default: 1)")
            }
        },
        required = listOf("title", "destination", "country", "start_date", "end_date"),
    )

    private val updateTrip = buildTool(
        name = "update_trip",
        description = "Update an existing trip's details. Only include fields you want to change.",
        properties = buildJsonObject {
            putJsonObject("trip_id") {
                put("type", "string")
                put("description", "The trip ID to update")
            }
            putJsonObject("title") {
                put("type", "string")
                put("description", "New trip title (optional)")
            }
            putJsonObject("destination") {
                put("type", "string")
                put("description", "New destination name (optional)")
            }
            putJsonObject("country") {
                put("type", "string")
                put("description", "New country name (optional)")
            }
            putJsonObject("start_date") {
                put("type", "string")
                put("description", "New start date in YYYY-MM-DD format (optional)")
            }
            putJsonObject("end_date") {
                put("type", "string")
                put("description", "New end date in YYYY-MM-DD format (optional)")
            }
            putJsonObject("travelers") {
                put("type", "integer")
                put("description", "New number of travelers (optional)")
            }
        },
        required = listOf("trip_id"),
    )

    private val deleteTrip = buildTool(
        name = "delete_trip",
        description = "Delete a trip permanently. Use when the user wants to remove a trip.",
        properties = buildJsonObject {
            putJsonObject("trip_id") {
                put("type", "string")
                put("description", "The trip ID to delete")
            }
        },
        required = listOf("trip_id"),
    )

    // --- Itinerary management ---

    private val addPlaceToItinerary = buildTool(
        name = "add_place_to_itinerary",
        description = "Add a place or activity to a trip's day plan.",
        properties = buildJsonObject {
            putJsonObject("trip_id") {
                put("type", "string")
                put("description", "The trip ID")
            }
            putJsonObject("day_number") {
                put("type", "integer")
                put("description", "Day number (1-indexed)")
            }
            putJsonObject("place_name") {
                put("type", "string")
                put("description", "Name of the place to add")
            }
            putJsonObject("category") {
                put("type", "string")
                put("description", "Place category: Sight, Food, Stay, Transit, or Activity")
            }
            putJsonObject("start_time") {
                put("type", "string")
                put("description", "Start time (e.g., '09:00')")
            }
            putJsonObject("end_time") {
                put("type", "string")
                put("description", "End time (e.g., '11:00')")
            }
            putJsonObject("note") {
                put("type", "string")
                put("description", "Optional note about this activity")
            }
        },
        required = listOf("trip_id", "day_number", "place_name", "category"),
    )

    private val removePlaceFromItinerary = buildTool(
        name = "remove_place_from_itinerary",
        description = "Remove a place from a trip's day plan.",
        properties = buildJsonObject {
            putJsonObject("trip_id") {
                put("type", "string")
                put("description", "The trip ID")
            }
            putJsonObject("day_number") {
                put("type", "integer")
                put("description", "Day number (1-indexed)")
            }
            putJsonObject("place_name") {
                put("type", "string")
                put("description", "Name of the place to remove (fuzzy match)")
            }
        },
        required = listOf("trip_id", "day_number", "place_name"),
    )

    // --- Navigation ---

    private val navigateTo = buildTool(
        name = "navigate_to",
        description = "Navigate to a specific screen in the app. Use when the user wants to go somewhere.",
        properties = buildJsonObject {
            putJsonObject("screen") {
                put("type", "string")
                put("description", "Screen to navigate to: home, trips, map, explore, profile, ai_chat, trip_detail, create_trip")
            }
            putJsonObject("trip_id") {
                put("type", "string")
                put("description", "Trip ID (required for trip_detail screen)")
            }
        },
        required = listOf("screen"),
    )

    private val showTrip = buildTool(
        name = "show_trip",
        description = "Show a specific trip's details and navigate to it. Use after creating or referencing a trip.",
        properties = buildJsonObject {
            putJsonObject("trip_id") {
                put("type", "string")
                put("description", "The trip ID to show")
            }
        },
        required = listOf("trip_id"),
    )

    // --- App control ---

    private val getMyProfile = buildTool(
        name = "get_my_profile",
        description = "Get the current user's profile information.",
        properties = buildJsonObject {},
    )

    private val searchDestinations = buildTool(
        name = "search_destinations",
        description = "Search for travel destinations. This will search the server's curated destination database.",
        properties = buildJsonObject {
            putJsonObject("query") {
                put("type", "string")
                put("description", "Search query")
            }
            putJsonObject("country") {
                put("type", "string")
                put("description", "Filter by country")
            }
        },
    )

    // --- Helper ---

    private fun buildTool(
        name: String,
        description: String,
        properties: JsonObject,
        required: List<String> = emptyList(),
    ): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("function"))
        putJsonObject("function") {
            put("name", JsonPrimitive(name))
            put("description", JsonPrimitive(description))
            put("parameters", buildJsonObject {
                put("type", JsonPrimitive("object"))
                put("properties", properties)
                if (required.isNotEmpty()) {
                    put("required", JsonArray(required.map { JsonPrimitive(it) }))
                }
            })
        }
    }
}
