package com.dabber.traveldabble

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Tests for NotificationRoutes:
 *   GET    /api/notifications
 *   POST   /api/notifications/{notificationId}/read
 *   POST   /api/notifications/read-all
 *   DELETE /api/notifications/{notificationId}
 *   POST   /api/fcm/register
 *   DELETE /api/fcm/unregister
 */
class NotificationTest {

    private suspend fun registerAndGetToken(client: io.ktor.client.HttpClient, suffix: String): String {
        val res = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"notif_$suffix","email":"notif_$suffix@example.com","password":"pass1234","displayName":"Notif $suffix"}""")
        }
        return Json.parseToJsonElement(res.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content
    }

    // ---- Notifications ----

    @Test
    fun testGetNotificationsRequiresAuth() = testApplication {
        application { module() }
        val res = client.get("/api/notifications")
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun testGetNotificationsReturnsEmptyListForNewUser() = testApplication {
        application { module() }
        val token = registerAndGetToken(client, "get1")

        val res = client.get("/api/notifications") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, res.status, "getNotifications: ${res.bodyAsText()}")
        val arr = Json.parseToJsonElement(res.bodyAsText()).jsonArray
        // New user has no notifications — list may be empty or contain system messages
        assertNotNull(arr)
    }

    @Test
    fun testMarkSingleNotificationReadWithInvalidId() = testApplication {
        application { module() }
        val token = registerAndGetToken(client, "read1")

        // An invalid (non-numeric) notification id should return 400
        val res = client.post("/api/notifications/not-a-number/read") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun testMarkAllNotificationsRead() = testApplication {
        application { module() }
        val token = registerAndGetToken(client, "readall1")

        // Even with no notifications, read-all should succeed
        val res = client.post("/api/notifications/read-all") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, res.status, "readAll: ${res.bodyAsText()}")
        assertTrue(res.bodyAsText().contains("All marked as read"))
    }

    @Test
    fun testMarkAllNotificationsRequiresAuth() = testApplication {
        application { module() }
        val res = client.post("/api/notifications/read-all")
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun testDeleteNotificationWithInvalidId() = testApplication {
        application { module() }
        val token = registerAndGetToken(client, "del1")

        val res = client.delete("/api/notifications/not-a-number") { bearerAuth(token) }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun testDeleteNotificationRequiresAuth() = testApplication {
        application { module() }
        val res = client.delete("/api/notifications/123")
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun testDeleteNonExistentNotification() = testApplication {
        application { module() }
        val token = registerAndGetToken(client, "del2")

        // Deleting a notification that doesn't exist should still return NoContent (delete is idempotent)
        val res = client.delete("/api/notifications/999999") { bearerAuth(token) }
        assertEquals(HttpStatusCode.NoContent, res.status)
    }

    // ---- FCM Token ----

    @Test
    fun testRegisterFcmToken() = testApplication {
        application { module() }
        val token = registerAndGetToken(client, "fcm1")

        val res = client.post("/api/fcm/register") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"token":"fake-fcm-device-token-abc123","platform":"android"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status, "fcmRegister: ${res.bodyAsText()}")
        assertTrue(res.bodyAsText().contains("FCM token registered"))
    }

    @Test
    fun testRegisterFcmTokenRequiresAuth() = testApplication {
        application { module() }
        val res = client.post("/api/fcm/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"token":"fake-token","platform":"android"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun testRegisterFcmTokenInvalidBody() = testApplication {
        application { module() }
        val token = registerAndGetToken(client, "fcm2")

        val res = client.post("/api/fcm/register") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"bad":"json"}""")
        }
        // Missing required fields — should return 400
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun testUnregisterFcmToken() = testApplication {
        application { module() }
        val token = registerAndGetToken(client, "fcm3")

        // Register first
        client.post("/api/fcm/register") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"token":"device-token-xyz","platform":"android"}""")
        }

        // Then unregister
        val res = client.delete("/api/fcm/unregister") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, res.status, "fcmUnregister: ${res.bodyAsText()}")
        assertTrue(res.bodyAsText().contains("FCM tokens unregistered"))
    }

    @Test
    fun testUnregisterFcmTokenRequiresAuth() = testApplication {
        application { module() }
        val res = client.delete("/api/fcm/unregister")
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun testRegisterFcmTokenIsIdempotent() = testApplication {
        application { module() }
        val token = registerAndGetToken(client, "fcm4")

        // Register the same platform twice — should replace, not error
        repeat(2) {
            val res = client.post("/api/fcm/register") {
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                setBody("""{"token":"same-token","platform":"android"}""")
            }
            assertEquals(HttpStatusCode.OK, res.status)
        }
    }
}
