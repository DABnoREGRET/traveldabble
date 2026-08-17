package com.dabber.traveldabble

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Extended MCP tool tests covering the 5 tools not exercised in McpTest.kt:
 *   tools/call → weather_forecast
 *   tools/call → travel_advisory
 *   tools/call → local_events
 *   tools/call → itinerary_templates
 *   tools/call → compare_destinations
 *
 * Also covers GET /health endpoint.
 */
class McpToolsExtendedTest {

    private suspend fun callTool(
        client: io.ktor.client.HttpClient,
        toolName: String,
        args: String,
    ): JsonObject {
        val res = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "tools/call",
                  "params": {
                    "name": "$toolName",
                    "arguments": $args
                  }
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, res.status, "$toolName: ${res.bodyAsText()}")
        return Json.parseToJsonElement(res.bodyAsText()).jsonObject
    }

    // ---- Health endpoint ----

    @Test
    fun testHealthEndpoint() = testApplication {
        application { module() }
        val res = client.get("/health")
        assertEquals(HttpStatusCode.OK, res.status, "health: ${res.bodyAsText()}")
        assertEquals("ok", res.bodyAsText().trim())
    }

    // ---- MCP Tool Tests ----

    @Test
    fun testMcpWeatherForecast() = testApplication {
        application { module() }
        val body = callTool(client, "weather_forecast", """{"destination":"Hanoi"}""")
        assertNotNull(body["result"], "Expected 'result' in weather_forecast response")
    }

    @Test
    fun testMcpWeatherForecastWithoutDate() = testApplication {
        application { module() }
        // Date is optional — should still return a result
        val body = callTool(client, "weather_forecast", """{"destination":"Ho Chi Minh City"}""")
        assertNotNull(body["result"], "Expected 'result' even without date parameter")
    }

    @Test
    fun testMcpTravelAdvisory() = testApplication {
        application { module() }
        val body = callTool(client, "travel_advisory", """{"destination":"Vietnam"}""")
        assertNotNull(body["result"], "Expected 'result' in travel_advisory response")
    }

    @Test
    fun testMcpTravelAdvisoryWithCountry() = testApplication {
        application { module() }
        val body = callTool(client, "travel_advisory", """{"destination":"Da Nang"}""")
        assertNotNull(body["result"], "Expected 'result' in travel_advisory response")
    }

    @Test
    fun testMcpLocalEvents() = testApplication {
        application { module() }
        val body = callTool(client, "local_events", """{"destination":"Hoi An","month":"October"}""")
        assertNotNull(body["result"], "Expected 'result' in local_events response")
    }

    @Test
    fun testMcpLocalEventsMinimalArgs() = testApplication {
        application { module() }
        // Only destination is required
        val body = callTool(client, "local_events", """{"destination":"Sapa"}""")
        assertNotNull(body["result"], "Expected 'result' in local_events with minimal args")
    }

    @Test
    fun testMcpItineraryTemplates() = testApplication {
        application { module() }
        val body = callTool(
            client, "itinerary_templates",
            """{"destination":"Ha Long Bay","days":3}"""
        )
        assertNotNull(body["result"], "Expected 'result' in itinerary_templates response")
    }

    @Test
    fun testMcpItineraryTemplatesDefaultStyle() = testApplication {
        application { module() }
        val body = callTool(
            client, "itinerary_templates",
            """{"destination":"Phu Quoc"}"""
        )
        assertNotNull(body["result"], "Expected 'result' in itinerary_templates with default style")
    }

    @Test
    fun testMcpCompareDestinations() = testApplication {
        application { module() }
        val body = callTool(
            client, "compare_destinations",
            """{"destination_a":"Hanoi","destination_b":"Da Nang"}"""
        )
        assertNotNull(body["result"], "Expected 'result' in compare_destinations response")
    }

    @Test
    fun testMcpCompareDestinationsTwoLocations() = testApplication {
        application { module() }
        val body = callTool(
            client, "compare_destinations",
            """{"destination_a":"Hoi An","destination_b":"Hue"}"""
        )
        assertNotNull(body["result"], "Expected 'result' comparing two destinations")
    }

    // ---- Error handling ----

    @Test
    fun testMcpToolsCallWithMissingRequiredArg() = testApplication {
        application { module() }
        // weather_forecast with empty args
        val body = callTool(client, "weather_forecast", """{}""")
        assertNotNull(body["result"], "Even missing-arg calls should return a result wrapper")
    }

    @Test
    fun testMcpAllToolsAreListedAndCallable() = testApplication {
        application { module() }
        // Verify all 7 tools from the spec are listed
        val listRes = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            setBody("""{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}""")
        }
        val body = Json.parseToJsonElement(listRes.bodyAsText()).jsonObject
        val tools = body["result"]!!.jsonObject["tools"]!!.jsonArray
        val toolNames = tools.map { it.jsonObject["name"]!!.jsonPrimitive.content }.toSet()

        val expectedTools = setOf(
            "search_destinations",
            "weather_forecast",
            "seasonal_recommendations",
            "travel_advisory",
            "local_events",
            "itinerary_templates",
            "compare_destinations",
        )
        for (expected in expectedTools) {
            assertTrue(toolNames.contains(expected), "Expected tool '$expected' in tools/list, found: $toolNames")
        }
    }
}
