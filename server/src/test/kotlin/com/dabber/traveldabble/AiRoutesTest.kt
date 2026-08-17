package com.dabber.traveldabble

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Tests for AiRoutes:
 *   GET  /api/ai/health
 *   GET  /api/ai/models
 *   POST /api/ai/chat  (no-key 503 path; actual AI calls require external key)
 */
class AiRoutesTest {

    @Test
    fun testAiHealthEndpoint() = testApplication {
        application { module() }
        val res = client.get("/api/ai/health")
        assertEquals(HttpStatusCode.OK, res.status, "aiHealth: ${res.bodyAsText()}")
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        assertEquals("ok", body["status"]!!.jsonPrimitive.content)
        assertNotNull(body["server_key_configured"])
        assertNotNull(body["message"])
    }

    @Test
    fun testAiModelsEndpoint() = testApplication {
        application { module() }
        val res = client.get("/api/ai/models")
        assertEquals(HttpStatusCode.OK, res.status, "aiModels: ${res.bodyAsText()}")
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        assertEquals("google/gemma-4-26b-a4b-it:free", body["default_model"]!!.jsonPrimitive.content)
        val models = body["available_models"]!!.jsonArray
        assertTrue(models.isNotEmpty(), "Should return at least one available model")
        assertTrue(models.any { it.jsonPrimitive.content.contains("gemma") }, "Expected Gemma model in the list")
        assertNotNull(body["server_key_configured"])
    }

    @Test
    fun testAiChatWithNoKeyReturnsServiceUnavailable() = testApplication {
        application { module() }
        // In the test environment, OPENROUTER_API_KEY is not set and no BYOK header is provided
        // The server should return 503 Service Unavailable
        val res = client.post("/api/ai/chat") {
            contentType(ContentType.Application.Json)
            setBody("""{"messages":[{"role":"user","content":"Hello, plan me a trip to Vietnam"}]}""")
        }
        // If server key is configured in CI it returns 200, otherwise 503
        assertTrue(
            res.status == HttpStatusCode.ServiceUnavailable || res.status == HttpStatusCode.OK,
            "Expected 503 (no key) or 200 (key configured), got ${res.status}"
        )
    }

    @Test
    fun testAiChatInvalidBodyRejected() = testApplication {
        application { module() }
        val res = client.post("/api/ai/chat") {
            contentType(ContentType.Application.Json)
            setBody("""{"invalid":"json"}""")
        }
        // Missing 'messages' field — either 400 or 503 (if key check happens first)
        assertTrue(
            res.status == HttpStatusCode.BadRequest || res.status == HttpStatusCode.ServiceUnavailable,
            "Expected 400 or 503, got ${res.status}"
        )
    }
}
