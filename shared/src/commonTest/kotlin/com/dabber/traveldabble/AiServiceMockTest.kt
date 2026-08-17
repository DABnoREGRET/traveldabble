package com.dabber.traveldabble

import com.dabber.traveldabble.data.AiResult
import com.dabber.traveldabble.data.AiService
import com.dabber.traveldabble.data.ApiClient
import com.dabber.traveldabble.data.ToolExecutionEvent
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AiServiceMockTest {

    @AfterTest
    fun tearDown() {
        ApiClient.setMockHttpClient(null)
        ApiClient.setToken(null)
    }

    @Test
    fun testCheckHealthAvailableWithServerKey() = runTest {
        val mockClient = createMockHttpClient {
            respond(
                content = """{"status":"ok","server_key_configured":true,"message":"Server AI key is configured"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val status = AiService.checkHealth()

        assertTrue(status.available)
        assertTrue(status.serverKeyConfigured)
    }

    @Test
    fun testCheckHealthAvailableWithoutServerKey() = runTest {
        val mockClient = createMockHttpClient {
            respond(
                content = """{"status":"ok","server_key_configured":false,"message":"No server key"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val status = AiService.checkHealth()

        assertTrue(status.available)
        assertFalse(status.serverKeyConfigured)
    }

    @Test
    fun testCheckHealthServerFailure() = runTest {
        val mockClient = createMockHttpClient {
            respond(
                content = "Service Unavailable",
                status = HttpStatusCode.ServiceUnavailable
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val status = AiService.checkHealth()

        // Non-200 or exception marks service unavailable
        assertFalse(status.available)
    }

    @Test
    fun testSendMessageDirectResponse() = runTest {
        val mockClient = createMockHttpClient {
            respond(
                content = """{"content":"Here is a 3-day itinerary for Hoi An!","model":"openai/gpt-4o-mini","byok":false}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val result = AiService.sendMessage(
            tripId = "test-trip",
            userMessage = "Plan me a trip to Hoi An"
        )

        assertTrue(result is AiResult.Success)
        assertEquals("Here is a 3-day itinerary for Hoi An!", result.content)
        assertFalse(result.usedByok)
    }

    @Test
    fun testSendMessageWithByokHeader() = runTest {
        var capturedApiKey: String? = null

        val mockClient = createMockHttpClient { request ->
            capturedApiKey = request.headers["X-Api-Key"]
            respond(
                content = """{"content":"Hello with BYOK!","model":"anthropic/claude-3.5-sonnet","byok":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val result = AiService.sendMessage(
            tripId = "test-trip",
            userMessage = "Hello",
            byokKey = "sk-openrouter-my-custom-key"
        )

        assertEquals("sk-openrouter-my-custom-key", capturedApiKey)
        assertTrue(result is AiResult.Success)
        assertTrue(result.usedByok)
    }

    @Test
    fun testSendMessageWithCustomModelSelection() = runTest {
        var capturedRequestBody: String? = null

        val mockClient = createMockHttpClient { request ->
            capturedRequestBody = (request.body as io.ktor.http.content.TextContent).text
            respond(
                content = """{"content":"Response from Gemma 4!","model":"google/gemma-4-26b-a4b-it:free","byok":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val result = AiService.sendMessage(
            tripId = "test-trip",
            userMessage = "Hello Gemma",
            byokKey = "sk-openrouter-key",
            model = "google/gemma-4-26b-a4b-it:free"
        )

        assertTrue(result is AiResult.Success)
        assertTrue(capturedRequestBody!!.contains("google/gemma-4-26b-a4b-it:free"))
    }

    @Test
    fun testSendMessageWithToolExecutionLoop() = runTest {
        var aiChatRound = 0
        val toolEvents = mutableListOf<ToolExecutionEvent>()

        val mockClient = createMockHttpClient { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/api/destinations") -> {
                    respond(
                        content = """
                        [
                          {
                            "id": "dest-1",
                            "name": "Hanoi",
                            "country": "Vietnam",
                            "tagline": "Old Quarter",
                            "rating": 4.8,
                            "tags": ["Culture"],
                            "cover": [-7685642, -1292135]
                          }
                        ]
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                path.endsWith("/api/ai/chat") -> {
                    aiChatRound++
                    if (aiChatRound == 1) {
                        // Round 1: AI requests client tool (search_destinations)
                        respond(
                            content = """
                            {
                              "content": "",
                              "model": "openai/gpt-4o-mini",
                              "byok": false,
                              "clientToolCalls": [
                                {
                                  "id": "call_1",
                                  "name": "search_destinations",
                                  "arguments": "{\"query\":\"Vietnam\"}"
                                }
                              ]
                            }
                            """.trimIndent(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    } else {
                        // Round 2: AI returns final text response after tool results
                        respond(
                            content = """
                            {
                              "content": "Found great destinations in Vietnam for you!",
                              "model": "openai/gpt-4o-mini",
                              "byok": false
                            }
                            """.trimIndent(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    }
                }
                else -> {
                    respond(content = "{}", status = HttpStatusCode.OK)
                }
            }
        }

        ApiClient.setMockHttpClient(mockClient)
        val result = AiService.sendMessage(
            tripId = "test-trip",
            userMessage = "Find places in Vietnam",
            onToolExecuted = { toolEvents.add(it) }
        )

        assertTrue(result is AiResult.Success)
        assertEquals("Found great destinations in Vietnam for you!", result.content)
        assertEquals(2, aiChatRound)
        assertTrue(toolEvents.isNotEmpty(), "Expected tool execution events to be emitted")
    }

    @Test
    fun testFetchModelsDynamic() = runTest {
        val mockClient = createMockHttpClient {
            respond(
                content = """
                {
                  "default_model": "google/gemma-4-26b-a4b-it:free",
                  "models": [
                    {
                      "id": "google/gemma-4-26b-a4b-it:free",
                      "name": "Google: Gemma 4 26B (free)",
                      "description": "Gemma 4 instruction model",
                      "is_free": true
                    },
                    {
                      "id": "anthropic/claude-3.5-sonnet",
                      "name": "Anthropic: Claude 3.5 Sonnet",
                      "description": "SOTA reasoning",
                      "is_free": false
                    }
                  ]
                }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val models = AiService.fetchModels()

        assertEquals(2, models.size)
        assertEquals("google/gemma-4-26b-a4b-it:free", models[0].id)
        assertTrue(models[0].isFree)
        assertEquals("anthropic/claude-3.5-sonnet", models[1].id)
        assertFalse(models[1].isFree)
    }
}
