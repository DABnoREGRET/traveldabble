package com.dabber.traveldabble.mcp

import kotlinx.serialization.json.*

/**
 * Tool definitions for OpenRouter function calling — server-side MCP tools.
 */
object AiToolDefinitions {

    fun buildAllTools(): JsonArray = JsonArray(listOf(
        buildTool(
            name = "weather_forecast",
            description = "Get a 5-day / 3-hour weather forecast for a destination using OpenWeatherMap. Returns temperatures, weather conditions, humidity, and forecast timeline.",
            properties = buildJsonObject {
                putJsonObject("destination") {
                    put("type", "string")
                    put("description", "Destination or city name (e.g. 'Hanoi', 'Da Nang', 'Ha Long Bay')")
                }
            },
            required = listOf("destination"),
        ),
        buildTool(
            name = "seasonal_recommendations",
            description = "Get seasonal travel recommendations and best times to visit a destination (weather patterns, dry vs rainy seasons, packing tips).",
            properties = buildJsonObject {
                putJsonObject("destination") {
                    put("type", "string")
                    put("description", "Destination name (e.g. 'Hanoi', 'Hoi An', 'Ha Giang')")
                }
                putJsonObject("travel_month") {
                    put("type", "string")
                    put("description", "Optional planned travel month (e.g. 'October', 'April')")
                }
            },
            required = listOf("destination"),
        ),
        buildTool(
            name = "travel_advisory",
            description = "Get travel advisories, safety advice, visa rules, emergency contacts, local scams to avoid, and cultural etiquette for a destination.",
            properties = buildJsonObject {
                putJsonObject("destination") {
                    put("type", "string")
                    put("description", "Destination or country name (e.g. 'Vietnam', 'Ha Giang')")
                }
            },
            required = listOf("destination"),
        ),
        buildTool(
            name = "local_events",
            description = "Discover local festivals, cultural celebrations, seasonal markets, and traditions for a destination or month.",
            properties = buildJsonObject {
                putJsonObject("destination") {
                    put("type", "string")
                    put("description", "Optional destination name (e.g. 'Hoi An', 'Da Nang')")
                }
                putJsonObject("month") {
                    put("type", "string")
                    put("description", "Optional month or season (e.g. 'February', 'Tet', 'Autumn')")
                }
            },
        ),
        buildTool(
            name = "itinerary_templates",
            description = "Retrieve curated multi-day itinerary templates (e.g. 4-day Hanoi & Ha Long, 5-day Central Coast, 4-day Ha Giang loop).",
            properties = buildJsonObject {
                putJsonObject("destination") {
                    put("type", "string")
                    put("description", "Destination or region (e.g. 'North Vietnam', 'Ha Giang', 'Hoi An')")
                }
                putJsonObject("days") {
                    put("type", "integer")
                    put("description", "Optional duration in days (e.g. 4, 5)")
                }
            },
        ),
        buildTool(
            name = "compare_destinations",
            description = "Compare two destinations side by side to help travelers choose based on vibe, budget, landscape, and activities.",
            properties = buildJsonObject {
                putJsonObject("destination_a") {
                    put("type", "string")
                    put("description", "First destination name")
                }
                putJsonObject("destination_b") {
                    put("type", "string")
                    put("description", "Second destination name")
                }
            },
            required = listOf("destination_a", "destination_b"),
        ),
        buildTool(
            name = "search_destinations",
            description = "Search curated travel destinations by name, country, or tags in the local catalog.",
            properties = buildJsonObject {
                putJsonObject("query") {
                    put("type", "string")
                    put("description", "Search query (e.g. 'beach', 'hiking', 'Hanoi')")
                }
            },
        ),
        buildTool(
            name = "list_all_destinations",
            description = "List all available curated destinations in the catalog.",
            properties = buildJsonObject {},
        ),
    ))

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
