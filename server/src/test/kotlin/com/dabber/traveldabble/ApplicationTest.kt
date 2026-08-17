package com.dabber.traveldabble

import com.dabber.traveldabble.config.DatabaseFactory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("TravelDabble API v1.0", response.bodyAsText())
    }

    @Test
    fun testDestinationsSeeded() = testApplication {
        application {
            module()
        }
        val response = client.get("/api/destinations")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Hanoi"), "Expected seeded destination Hanoi, got: $body")
        assertTrue(body.contains("Vietnam"), "Expected Vietnam country in destinations, got: $body")
    }

    @Test
    fun testAuthAndTripCrud() = testApplication {
        application {
            module()
        }

        // Register a user
        val register = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"alice","email":"alice@example.com","password":"secret123","displayName":"Alice"}""")
        }
        assertEquals(HttpStatusCode.Created, register.status, "register body: ${register.bodyAsText()}")
        val token = Json.parseToJsonElement(register.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content
        assertTrue(token.isNotBlank())

        // Duplicate registration -> 409
        val dup = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"alice","email":"alice@example.com","password":"secret123","displayName":"Alice"}""")
        }
        assertEquals(HttpStatusCode.Conflict, dup.status)

        // Trips require auth -> 401
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/trips").status)

        // Create a trip
        val create = client.post("/api/trips") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"title":"Vietnam Trip","destination":"Hanoi","country":"Vietnam","startDate":"2026-09-01","endDate":"2026-09-10","travelers":2}""")
        }
        assertEquals(HttpStatusCode.Created, create.status, "create body: ${create.bodyAsText()}")
        val tripId = Json.parseToJsonElement(create.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        // List trips
        val list = client.get("/api/trips") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, list.status)
        assertTrue(list.bodyAsText().contains("Vietnam Trip"), "list body: ${list.bodyAsText()}")

        // Get single trip
        val single = client.get("/api/trips/$tripId") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, single.status)
        assertTrue(single.bodyAsText().contains("Hanoi"), "single body: ${single.bodyAsText()}")

        // Update trip
        val update = client.put("/api/trips/$tripId") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"title":"Vietnam Trip v2","destination":"Hanoi","country":"Vietnam","startDate":"2026-09-01","endDate":"2026-09-10","travelers":3}""")
        }
        assertEquals(HttpStatusCode.OK, update.status, "update body: ${update.bodyAsText()}")
        assertTrue(update.bodyAsText().contains("Vietnam Trip v2"), "update body: ${update.bodyAsText()}")

        // Delete trip -> cascades budget/dayplans
        val delete = client.delete("/api/trips/$tripId") { bearerAuth(token) }
        assertEquals(HttpStatusCode.NoContent, delete.status)
        assertEquals(HttpStatusCode.NotFound, client.get("/api/trips/$tripId") { bearerAuth(token) }.status)

        // Login
        val login = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"alice@example.com","password":"secret123"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status, "login body: ${login.bodyAsText()}")

        // Wrong password -> 401
        val badLogin = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"alice@example.com","password":"wrong"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, badLogin.status)

        // Me (authenticated)
        val me = client.get("/api/auth/me") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, me.status)
        assertTrue(me.bodyAsText().contains("alice@example.com"), "me body: ${me.bodyAsText()}")
    }
}
