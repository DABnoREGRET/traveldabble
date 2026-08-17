package com.dabber.traveldabble.routes

import com.dabber.traveldabble.mcp.AiToolDefinitions
import com.dabber.traveldabble.mcp.McpTools
import com.dabber.traveldabble.model.ApiError
import com.dabber.traveldabble.util.rateLimited
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.get as clientGet
import io.ktor.client.request.header
import io.ktor.client.request.post as clientPost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json as clientJson
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class AiChatRequest(
    val messages: List<AiMessage>,
    val model: String? = null,
    val clientTools: String? = null,
)

@Serializable
data class AiMessage(
    val role: String,
    val content: String,
)

private val SERVER_OPENROUTER_KEY: String?
    get() = System.getenv("OPENROUTER_API_KEY")

private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
private const val DEFAULT_MODEL = "google/gemma-4-26b-a4b-it:free"
private const val MAX_TOOL_ROUNDS = 6

private val httpClient by lazy {
    HttpClient(CIO) {
        install(ClientContentNegotiation) {
            clientJson(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
}

private val SERVER_EXECUTABLE_TOOLS = setOf(
    "weather_forecast",
    "seasonal_recommendations",
    "travel_advisory",
    "local_events",
    "itinerary_templates",
    "compare_destinations",
    "search_destinations",
    "get_destination",
    "list_all_destinations",
)

fun Route.AiRoutes() {
    route("/api/ai") {
        rateLimited(limit = 20, windowMillis = 60_000) {
            post("/chat") {
                val request = try {
                    call.receive<AiChatRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                    return@post
                }

                val byokKey = call.request.headers["X-Api-Key"]?.takeIf { it.isNotBlank() }
                val apiKey = byokKey ?: SERVER_OPENROUTER_KEY

                if (apiKey.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiError("No AI API key configured. Set OPENROUTER_API_KEY on the server or provide your own key in settings."),
                    )
                    return@post
                }

                val model = request.model ?: DEFAULT_MODEL
                val byok = byokKey != null

                val clientToolDefs = request.clientTools?.let {
                    try {
                        Json.parseToJsonElement(it).jsonArray
                    } catch (_: Exception) { null }
                }

                val allTools = buildToolList(clientToolDefs)

                try {
                    val messages = kotlinx.serialization.json.JsonArray(
                        request.messages.map { msg ->
                            buildJsonObject {
                                put("role", JsonPrimitive(msg.role))
                                put("content", JsonPrimitive(msg.content))
                            }
                        }
                    )

                    val result = runToolLoop(apiKey, model, messages, allTools, byok)
                    call.respond(result)
                } catch (e: Exception) {
                    call.application.log.error("AI chat proxy failed", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError("AI service temporarily unavailable: ${e.message}"),
                    )
                }
            }
        }

        get("/models") {
            try {
                val openRouterResp = httpClient.clientGet("https://openrouter.ai/api/v1/models") {
                    header("HTTP-Referer", "https://traveldabble.app")
                    header("X-Title", "TravelDabble")
                }
                if (openRouterResp.status.value in 200..299) {
                    val responseJson = Json.parseToJsonElement(openRouterResp.bodyAsText()).jsonObject
                    val data = responseJson["data"]?.jsonArray
                    if (!data.isNullOrEmpty()) {
                        val models = data.mapNotNull { item ->
                            val obj = item.jsonObject
                            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: id
                            val description = obj["description"]?.jsonPrimitive?.contentOrNull ?: ""
                            val pricing = obj["pricing"]?.jsonObject
                            val isFree = id.endsWith(":free") || (
                                pricing?.get("prompt")?.jsonPrimitive?.contentOrNull == "0" &&
                                pricing?.get("completion")?.jsonPrimitive?.contentOrNull == "0"
                            )
                            buildJsonObject {
                                put("id", JsonPrimitive(id))
                                put("name", JsonPrimitive(name))
                                put("description", JsonPrimitive(description.take(150)))
                                put("is_free", JsonPrimitive(isFree))
                            }
                        }
                        call.respond(buildJsonObject {
                            put("default_model", JsonPrimitive(DEFAULT_MODEL))
                            put("available_models", kotlinx.serialization.json.JsonArray(models.map { it["id"]!! }))
                            put("models", kotlinx.serialization.json.JsonArray(models))
                            put("server_key_configured", JsonPrimitive(!SERVER_OPENROUTER_KEY.isNullOrBlank()))
                        })
                        return@get
                    }
                }
            } catch (_: Exception) {
                // Fallback to default models if OpenRouter API is unreachable or rate-limited
            }

            val fallbackModels = listOf(
                buildJsonObject { put("id", JsonPrimitive("google/gemma-4-26b-a4b-it:free")); put("name", JsonPrimitive("Gemma 4 26B (Free)")); put("description", JsonPrimitive("Google lightweight open-weights instruction model")); put("is_free", JsonPrimitive(true)) },
                buildJsonObject { put("id", JsonPrimitive("meta-llama/llama-3.3-70b-instruct:free")); put("name", JsonPrimitive("Llama 3.3 70B (Free)")); put("description", JsonPrimitive("Meta high-capability 70B open-weights LLM")); put("is_free", JsonPrimitive(true)) },
                buildJsonObject { put("id", JsonPrimitive("deepseek/deepseek-r1:free")); put("name", JsonPrimitive("DeepSeek R1 (Free)")); put("description", JsonPrimitive("Advanced reasoning open model")); put("is_free", JsonPrimitive(true)) },
                buildJsonObject { put("id", JsonPrimitive("openai/gpt-4o-mini")); put("name", JsonPrimitive("GPT-4o Mini")); put("description", JsonPrimitive("Fast and intelligent reasoning by OpenAI")); put("is_free", JsonPrimitive(false)) },
                buildJsonObject { put("id", JsonPrimitive("openai/gpt-4o")); put("name", JsonPrimitive("GPT-4o")); put("description", JsonPrimitive("OpenAI flagship multimodal intelligence")); put("is_free", JsonPrimitive(false)) },
                buildJsonObject { put("id", JsonPrimitive("anthropic/claude-3.5-sonnet")); put("name", JsonPrimitive("Claude 3.5 Sonnet")); put("description", JsonPrimitive("Anthropic state-of-the-art reasoning model")); put("is_free", JsonPrimitive(false)) },
                buildJsonObject { put("id", JsonPrimitive("google/gemini-2.0-flash-001")); put("name", JsonPrimitive("Gemini 2.0 Flash")); put("description", JsonPrimitive("Ultra-fast next-generation Google AI")); put("is_free", JsonPrimitive(false)) },
                buildJsonObject { put("id", JsonPrimitive("meta-llama/llama-3.1-8b-instruct")); put("name", JsonPrimitive("Llama 3.1 8B")); put("description", JsonPrimitive("Fast and compact Meta model")); put("is_free", JsonPrimitive(false)) },
            )

            call.respond(buildJsonObject {
                put("default_model", JsonPrimitive(DEFAULT_MODEL))
                put("available_models", kotlinx.serialization.json.JsonArray(fallbackModels.map { it["id"]!! }))
                put("models", kotlinx.serialization.json.JsonArray(fallbackModels))
                put("server_key_configured", JsonPrimitive(!SERVER_OPENROUTER_KEY.isNullOrBlank()))
            })
        }

        get("/health") {
            val hasServerKey = !SERVER_OPENROUTER_KEY.isNullOrBlank()
            call.respond(buildJsonObject {
                put("status", JsonPrimitive("ok"))
                put("server_key_configured", JsonPrimitive(hasServerKey))
                put("message", JsonPrimitive(
                    if (hasServerKey) "Server AI key is configured"
                    else "No server key — users must provide their own API key"
                ))
            })
        }
    }
}

private suspend fun runToolLoop(
    apiKey: String,
    model: String,
    initialMessages: kotlinx.serialization.json.JsonArray,
    tools: kotlinx.serialization.json.JsonArray?,
    byok: Boolean = false,
): JsonObject {
    var messages = initialMessages.toMutableList()
    var round = 0

    while (round < MAX_TOOL_ROUNDS) {
        round++

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("messages", kotlinx.serialization.json.JsonArray(messages))
            put("max_tokens", JsonPrimitive(2048))
            put("temperature", JsonPrimitive(0.7))
            tools?.let { put("tools", it) }
        }

        val openRouterResponse = httpClient.clientPost(OPENROUTER_URL) {
            header("Authorization", "Bearer $apiKey")
            header("HTTP-Referer", "https://traveldabble.app")
            header("X-Title", "TravelDabble")
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        val responseText = openRouterResponse.bodyAsText()

        if (openRouterResponse.status.value !in 200..299) {
            return buildJsonObject {
                put("content", JsonPrimitive("AI service error: ${openRouterResponse.status}"))
                put("model", JsonPrimitive(model))
                put("byok", JsonPrimitive(byok))
            }
        }

        val responseJson = Json.parseToJsonElement(responseText).jsonObject
        val choices = responseJson["choices"]?.jsonArray
        val choice = choices?.firstOrNull()?.jsonObject ?: break
        val message = choice["message"]?.jsonObject ?: break

        val content = message["content"]?.jsonPrimitive?.contentOrNull
        val toolCalls = message["tool_calls"]?.jsonArray

        if (toolCalls.isNullOrEmpty()) {
            return buildJsonObject {
                put("content", JsonPrimitive(content ?: ""))
                put("model", JsonPrimitive(model))
                put("byok", JsonPrimitive(byok))
            }
        }

        val serverResults = mutableListOf<JsonObject>()
        val clientToolCalls = mutableListOf<JsonObject>()
        var hasServerTool = false

        for (toolCall in toolCalls) {
            val tc = toolCall.jsonObject
            val function = tc["function"]?.jsonObject ?: continue
            val toolName = function["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val toolArgs = function["arguments"]?.jsonPrimitive?.contentOrNull?.let {
                try { Json.parseToJsonElement(it).jsonObject } catch (_: Exception) { null }
            }

            if (toolName in SERVER_EXECUTABLE_TOOLS) {
                val result = McpTools.callTool(toolName, toolArgs, null)
                serverResults.add(buildJsonObject {
                    put("tool_call_id", tc["id"] ?: JsonPrimitive(""))
                    put("role", JsonPrimitive("tool"))
                    put("content", JsonPrimitive(result.toString()))
                })
                hasServerTool = true
            } else {
                clientToolCalls.add(buildJsonObject {
                    put("id", tc["id"] ?: JsonPrimitive(""))
                    put("name", JsonPrimitive(toolName))
                    put("arguments", function["arguments"] ?: JsonPrimitive("{}"))
                })
            }
        }

        if (hasServerTool) {
            messages.add(message)
            for (result in serverResults) {
                messages.add(result)
            }
            continue
        }

        return buildJsonObject {
            put("content", JsonPrimitive(content ?: ""))
            put("model", JsonPrimitive(model))
            put("byok", JsonPrimitive(true))
            put("clientToolCalls", kotlinx.serialization.json.JsonArray(clientToolCalls))
        }
    }

    return buildJsonObject {
        put("content", JsonPrimitive("I processed your request but ran into too many steps. Please try again."))
        put("model", JsonPrimitive(model))
        put("byok", JsonPrimitive(true))
    }
}

private fun buildToolList(clientToolDefs: kotlinx.serialization.json.JsonArray?): kotlinx.serialization.json.JsonArray {
    val serverTools = AiToolDefinitions.buildAllTools()
    if (clientToolDefs.isNullOrEmpty()) return serverTools
    return kotlinx.serialization.json.JsonArray(serverTools + clientToolDefs)
}
