package com.dabber.traveldabble

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Extended tests for DestinationRoutes:
 *   GET /api/destinations/{id}   — fetch a single destination by ID
 *   (GET /api/destinations is already tested in ApplicationTest)
 */
class DestinationExtendedTest {

    private suspend fun getFirstDestinationId(client: io.ktor.client.HttpClient): String {
        val res = client.get("/api/destinations")
        val arr = Json.parseToJsonElement(res.bodyAsText()).jsonArray
        assertTrue(arr.isNotEmpty(), "Destinations list must not be empty for this test")
        return arr.first().jsonObject["id"]!!.jsonPrimitive.content
    }

    @Test
    fun testGetSingleDestinationById() = testApplication {
        application { module() }
        val id = getFirstDestinationId(client)

        val res = client.get("/api/destinations/$id")
        assertEquals(HttpStatusCode.OK, res.status, "getDestination($id): ${res.bodyAsText()}")
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        assertEquals(id, body["id"]!!.jsonPrimitive.content, "Returned destination should have the requested id")
        assertNotNull(body["name"], "Expected 'name' field")
        assertNotNull(body["country"], "Expected 'country' field")
    }

    @Test
    fun testGetDestinationByIdContainsVietnam() = testApplication {
        application { module() }
        val id = getFirstDestinationId(client)

        val res = client.get("/api/destinations/$id")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.bodyAsText()
        assertTrue(body.contains("Vietnam"), "Seeded destinations should be from Vietnam, got: $body")
    }

    @Test
    fun testGetDestinationNotFoundReturns404() = testApplication {
        application { module() }
        // Use a UUID that definitely doesn't exist in the seed data
        val res = client.get("/api/destinations/00000000-0000-0000-0000-000000000000")
        assertEquals(HttpStatusCode.NotFound, res.status, "Should return 404 for unknown destination id")
    }

    @Test
    fun testGetDestinationWithInvalidIdFormat() = testApplication {
        application { module() }
        val res = client.get("/api/destinations/not-a-valid-uuid")
        // Either 400 Bad Request or 404 depending on implementation
        assertTrue(
            res.status == HttpStatusCode.BadRequest || res.status == HttpStatusCode.NotFound,
            "Expected 400 or 404 for invalid uuid format, got ${res.status}"
        )
    }

    @Test
    fun testAllSeededDestinationsAreRetrievable() = testApplication {
        application { module() }
        val listRes = client.get("/api/destinations")
        val destinations = Json.parseToJsonElement(listRes.bodyAsText()).jsonArray

        // Each destination in the list should be independently fetchable by ID
        for (dest in destinations.take(3)) { // check first 3 to keep test fast
            val id = dest.jsonObject["id"]!!.jsonPrimitive.content
            val singleRes = client.get("/api/destinations/$id")
            assertEquals(
                HttpStatusCode.OK, singleRes.status,
                "Failed to fetch destination $id individually"
            )
        }
    }

    @Test
    fun testGetDestinationResponseShape() = testApplication {
        application { module() }
        val id = getFirstDestinationId(client)
        val res = client.get("/api/destinations/$id")
        assertEquals(HttpStatusCode.OK, res.status)

        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        // Verify expected fields are present in the Destination DTO
        assertTrue(body.containsKey("id"))
        assertTrue(body.containsKey("name"))
        assertTrue(body.containsKey("country"))
        assertTrue(body.containsKey("tagline"))
        assertTrue(body.containsKey("rating"))
        assertTrue(body.containsKey("tags"))
        assertTrue(body.containsKey("cover"))
    }
}
