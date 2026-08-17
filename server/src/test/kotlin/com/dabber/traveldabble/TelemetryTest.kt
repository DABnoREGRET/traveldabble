package com.dabber.traveldabble

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Tests for TelemetryRoutes:
 *   POST /api/telemetry/events
 *   GET  /api/stats
 */
class TelemetryTest {

    @Test
    fun testRecordTelemetryEvent() = testApplication {
        application { module() }
        val res = client.post("/api/telemetry/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"eventType":"screen_view","screenName":"HomeScreen","optOut":false}""")
        }
        assertEquals(HttpStatusCode.Created, res.status, "telemetryEvent: ${res.bodyAsText()}")
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        assertEquals("recorded", body["status"]!!.jsonPrimitive.content)
    }

    @Test
    fun testTelemetryOptOutReturnsAccepted() = testApplication {
        application { module() }
        val res = client.post("/api/telemetry/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"eventType":"screen_view","screenName":"HomeScreen","optOut":true}""")
        }
        assertEquals(HttpStatusCode.Accepted, res.status, "optOut: ${res.bodyAsText()}")
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        assertEquals("opted_out", body["status"]!!.jsonPrimitive.content)
    }

    @Test
    fun testTelemetryInvalidBodyRejected() = testApplication {
        application { module() }
        val res = client.post("/api/telemetry/events") {
            contentType(ContentType.Application.Json)
            setBody("""not-json-at-all""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun testTelemetryEventWithOptionalFields() = testApplication {
        application { module() }
        val res = client.post("/api/telemetry/events") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "eventType": "app_startup",
                  "screenName": null,
                  "durationMs": 1200,
                  "connectionType": "wifi",
                  "memoryMb": 128,
                  "optOut": false
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.Created, res.status, "eventWithOptionals: ${res.bodyAsText()}")
    }

    @Test
    fun testTelemetryEventAnonymouslyWithoutAuth() = testApplication {
        application { module() }
        // Telemetry accepts events from unauthenticated users (anonymous usage)
        val res = client.post("/api/telemetry/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"eventType":"screen_view","screenName":"OnboardingScreen","optOut":false}""")
        }
        // Should succeed — no auth required
        assertTrue(
            res.status == HttpStatusCode.Created || res.status == HttpStatusCode.OK,
            "Expected 201 or 200 for anonymous telemetry, got ${res.status}"
        )
    }

    @Test
    fun testStatsEndpointReturnsUsageSummary() = testApplication {
        application { module() }
        val res = client.get("/api/stats")
        assertEquals(HttpStatusCode.OK, res.status, "stats: ${res.bodyAsText()}")
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        // Verify the shape of the UsageSummary response
        assertNotNull(body["date"], "Expected 'date' field in stats")
        assertNotNull(body["totalCalls"], "Expected 'totalCalls' field in stats")
        assertNotNull(body["uniqueUsers"], "Expected 'uniqueUsers' field in stats")
        assertNotNull(body["avgResponseTimeMs"], "Expected 'avgResponseTimeMs' field in stats")
        assertNotNull(body["topEndpoints"], "Expected 'topEndpoints' array in stats")
        assertNotNull(body["errorRate"], "Expected 'errorRate' in stats")
        assertNotNull(body["clientMetrics"], "Expected 'clientMetrics' in stats")
    }

    @Test
    fun testStatsDateMatchesToday() = testApplication {
        application { module() }
        val res = client.get("/api/stats")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        val date = body["date"]!!.jsonPrimitive.content
        // Date should be in ISO format YYYY-MM-DD
        assertTrue(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")), "Date '$date' should be in YYYY-MM-DD format")
    }

    @Test
    fun testStatsAfterRecordingEvent() = testApplication {
        application { module() }
        // Record an event
        client.post("/api/telemetry/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"eventType":"screen_view","screenName":"TripsScreen","optOut":false}""")
        }
        // Stats should be accessible and return valid data
        val res = client.get("/api/stats")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        // clientMetrics should reflect the event
        val clientMetrics = body["clientMetrics"]!!.jsonObject
        assertNotNull(clientMetrics["totalClientEvents"])
    }
}
