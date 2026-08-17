package com.dabber.traveldabble.mcp

import com.dabber.traveldabble.db.Destinations
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object McpTools {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    val definitions = listOf(
        ToolDefinition(
            name = "weather_forecast",
            description = "Get a 5-day / 3-hour weather forecast for a destination using OpenWeatherMap. Returns temperatures, weather conditions, humidity, and forecast timeline.",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("destination") {
                        put("type", "string")
                        put("description", "Destination or city name (e.g. 'Hanoi', 'Da Nang', 'Ha Long Bay')")
                    }
                }
                putJsonArray("required") { add("destination") }
            }
        ),
        ToolDefinition(
            name = "seasonal_recommendations",
            description = "Get seasonal travel recommendations and best times to visit a destination.",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("destination") {
                        put("type", "string")
                        put("description", "Destination name (e.g. 'Hanoi', 'Hoi An', 'Ha Giang')")
                    }
                    putJsonObject("travel_month") {
                        put("type", "string")
                        put("description", "Optional travel month (e.g. 'October', 'April')")
                    }
                }
                putJsonArray("required") { add("destination") }
            }
        ),
        ToolDefinition(
            name = "travel_advisory",
            description = "Get travel advisories, safety advice, visa rules, emergency contacts, local scams, and etiquette.",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("destination") {
                        put("type", "string")
                        put("description", "Destination or country name (e.g. 'Vietnam', 'Ha Giang')")
                    }
                }
                putJsonArray("required") { add("destination") }
            }
        ),
        ToolDefinition(
            name = "local_events",
            description = "Discover local festivals, cultural celebrations, seasonal markets, and traditions.",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("destination") {
                        put("type", "string")
                        put("description", "Optional destination name (e.g. 'Hoi An', 'Da Nang')")
                    }
                    putJsonObject("month") {
                        put("type", "string")
                        put("description", "Optional month or season (e.g. 'February', 'Tet', 'Autumn')")
                    }
                }
            }
        ),
        ToolDefinition(
            name = "itinerary_templates",
            description = "Retrieve curated multi-day itinerary templates (e.g. 4-day Hanoi & Ha Long, 5-day Central Coast).",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("destination") {
                        put("type", "string")
                        put("description", "Destination or region (e.g. 'North Vietnam', 'Ha Giang')")
                    }
                    putJsonObject("days") {
                        put("type", "integer")
                        put("description", "Optional duration in days")
                    }
                }
            }
        ),
        ToolDefinition(
            name = "compare_destinations",
            description = "Compare two destinations side by side.",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("destination_a") {
                        put("type", "string")
                        put("description", "First destination name")
                    }
                    putJsonObject("destination_b") {
                        put("type", "string")
                        put("description", "Second destination name")
                    }
                }
                putJsonArray("required") { add("destination_a"); add("destination_b") }
            }
        ),
        ToolDefinition(
            name = "search_destinations",
            description = "Search travel destinations by name, country, or tags.",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("query") {
                        put("type", "string")
                        put("description", "Search query")
                    }
                }
            }
        ),
        ToolDefinition(
            name = "list_all_destinations",
            description = "List all available travel destinations.",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {}
            }
        ),
    )

    suspend fun callTool(name: String, args: JsonObject?, userId: String?): JsonElement {
        return when (name) {
            "weather_forecast" -> {
                val destination = args?.get("destination")?.jsonPrimitive?.contentOrNull ?: "Hanoi"
                WeatherTool.getWeatherForecast(destination)
            }
            "seasonal_recommendations" -> {
                val destination = args?.get("destination")?.jsonPrimitive?.contentOrNull ?: "Vietnam"
                val month = args?.get("travel_month")?.jsonPrimitive?.contentOrNull
                SeasonalTool.getRecommendations(destination, month)
            }
            "travel_advisory" -> {
                val destination = args?.get("destination")?.jsonPrimitive?.contentOrNull ?: "Vietnam"
                AdvisoryTool.getAdvisory(destination)
            }
            "local_events" -> {
                val destination = args?.get("destination")?.jsonPrimitive?.contentOrNull
                val month = args?.get("month")?.jsonPrimitive?.contentOrNull
                EventsTool.getLocalEvents(destination, month)
            }
            "itinerary_templates" -> {
                val destination = args?.get("destination")?.jsonPrimitive?.contentOrNull
                val days = args?.get("days")?.jsonPrimitive?.intOrNull
                TemplatesTool.getTemplates(destination, days)
            }
            "compare_destinations" -> {
                val destA = args?.get("destination_a")?.jsonPrimitive?.contentOrNull ?: "Hanoi"
                val destB = args?.get("destination_b")?.jsonPrimitive?.contentOrNull ?: "Hoi An"
                CompareTool.compare(destA, destB)
            }
            "search_destinations" -> searchDestinations(args)
            "list_all_destinations" -> listAllDestinations()
            else -> buildJsonObject { put("error", "Unknown tool: $name") }
        }
    }

    private fun searchDestinations(args: JsonObject?): JsonElement {
        val query = args?.get("query")?.jsonPrimitive?.contentOrNull
        val results = transaction {
            Destinations.selectAll().toList().filter { row ->
                val name = row[Destinations.name]
                val rowCountry = row[Destinations.country]
                val rowTagline = row[Destinations.tagline]

                query.isNullOrBlank() ||
                    name.contains(query, ignoreCase = true) ||
                    rowCountry.contains(query, ignoreCase = true) ||
                    rowTagline.contains(query, ignoreCase = true)
            }.map { row ->
                buildJsonObject {
                    put("id", row[Destinations.id].toString())
                    put("name", row[Destinations.name])
                    put("country", row[Destinations.country])
                    put("tagline", row[Destinations.tagline])
                    put("rating", row[Destinations.rating])
                    put("tags", json.parseToJsonElement(row[Destinations.tags]))
                }
            }
        }

        return buildJsonObject {
            put("destinations", JsonArray(results))
            put("count", results.size)
        }
    }

    private fun listAllDestinations(): JsonElement {
        val results = transaction {
            Destinations.selectAll().map { row ->
                buildJsonObject {
                    put("id", row[Destinations.id].toString())
                    put("name", row[Destinations.name])
                    put("country", row[Destinations.country])
                    put("tagline", row[Destinations.tagline])
                    put("rating", row[Destinations.rating])
                }
            }
        }
        return buildJsonObject {
            put("destinations", JsonArray(results))
            put("count", results.size)
        }
    }
}
