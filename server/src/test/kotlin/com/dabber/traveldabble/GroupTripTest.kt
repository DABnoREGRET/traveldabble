package com.dabber.traveldabble

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

class GroupTripTest {

    private suspend fun registerUser(
        client: io.ktor.client.HttpClient,
        username: String = "alice",
        email: String = "alice@example.com",
        password: String = "secret123",
        displayName: String = "Alice",
    ): String {
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","email":"$email","password":"$password","displayName":"$displayName"}""")
        }
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content
    }

    private suspend fun createTrip(client: io.ktor.client.HttpClient, token: String): String {
        val response = client.post("/api/trips") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"title":"Vietnam Trip","destination":"Hanoi","country":"Vietnam","startDate":"2026-09-01","endDate":"2026-09-10","travelers":2}""")
        }
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content
    }

    @Test
    fun testGenerateInviteCode() = testApplication {
        application {
            module()
        }
        val token = registerUser(client, "gen_inv", "gen_inv@example.com")
        val tripId = createTrip(client, token)

        val response = client.post("/api/trips/$tripId/invite") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"maxUses":5,"expiresInHours":24}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(body["code"]!!.jsonPrimitive.content.isNotBlank())
        assertNotNull(body["expiresAt"])
    }

    @Test
    fun testJoinTrip() = testApplication {
        application {
            module()
        }
        val token1 = registerUser(client, "alice_join", "alice_join@example.com")
        val tripId = createTrip(client, token1)

        val inviteResponse = client.post("/api/trips/$tripId/invite") {
            contentType(ContentType.Application.Json)
            bearerAuth(token1)
            setBody("""{}""")
        }
        val code = Json.parseToJsonElement(inviteResponse.bodyAsText()).jsonObject["code"]!!.jsonPrimitive.content

        val token2 = registerUser(client, "bob_join", "bob_join@example.com")
        val joinResponse = client.post("/api/trips/join") {
            contentType(ContentType.Application.Json)
            bearerAuth(token2)
            setBody("""{"code":"$code"}""")
        }
        assertEquals(HttpStatusCode.OK, joinResponse.status)
        assertTrue(joinResponse.bodyAsText().contains("Successfully joined trip"))
    }
}
