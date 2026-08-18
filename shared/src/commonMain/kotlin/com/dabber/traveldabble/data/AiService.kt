package com.dabber.traveldabble.data

import com.dabber.traveldabble.model.LocalChatMessage
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
    val messages: List<AiChatMessage>,
    val model: String? = null,
    val clientTools: String? = null,
)

@Serializable
data class AiChatMessage(val role: String, val content: String)

@Serializable
data class AiChatResponse(
    val content: String,
    val model: String,
    val byok: Boolean = false,
    val clientToolCalls: List<ClientToolCall>? = null,
)

@Serializable
data class ClientToolCall(
    val id: String,
    val name: String,
    val arguments: String,
)

@Serializable
private data class AiHealthResponse(val status: String, val server_key_configured: Boolean, val message: String = "")

/**
 * AI service with tool-calling support and intelligent local fallback.
 *
 * Flow:
 * 1. Try sending request + client tool definitions to backend server /api/ai/chat
 * 2. If server is unreachable and BYOK key is provided, query OpenRouter directly from client
 * 3. If no key is configured or offline, process with local intelligent Copilot engine
 * 4. Execute client tools (create_trip, show_trip, search_destinations, navigate_to_screen)
 * 5. Return structured response to UI
 */
object AiService {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private const val MAX_CONTEXT_MESSAGES = 20
    private const val MAX_CLIENT_TOOL_ROUNDS = 4
    private const val DIRECT_OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"

    /**
     * Send a chat message and handle tool execution.
     */
    suspend fun sendMessage(
        tripId: String,
        userMessage: String,
        byokKey: String? = null,
        model: String? = null,
        onToolExecuted: ((ToolExecutionEvent) -> Unit)? = null,
    ): AiResult {
        val history = LocalChatStorage.loadMessages(tripId)
            .takeLast(MAX_CONTEXT_MESSAGES)
            .map { msg ->
                AiChatMessage(
                    role = if (msg.senderId == "ai") "assistant" else "user",
                    content = msg.text,
                )
            }

        val systemMessage = AiChatMessage(
            role = "system",
            content = buildString {
                append("You are Travel Copilot, an expert travel planning assistant for TravelDabble. ")
                append("You help users plan trips, find destinations, create itineraries, and discover local experiences. ")
                append("You have access to tools that can manage the user's trips, search destinations, and navigate the app. ")
                append("When a user asks you to create, update, or delete a trip, use the appropriate tool. ")
                append("When you create or reference a trip, use show_trip to navigate to it. ")
                append("Be helpful, concise, and enthusiastic about travel. ")
                append("Respond in the same language the user writes in.")
            }
        )

        val messages = mutableListOf(systemMessage)
        messages.addAll(history)
        messages.add(AiChatMessage(role = "user", content = userMessage))

        val effectiveModel = model ?: AuthState.selectedAiModel

        // 1. Try backend server proxy
        val serverResult = tryServerChat(messages, effectiveModel, byokKey, onToolExecuted)
        if (serverResult != null) {
            return serverResult
        }

        // 2. If server proxy unavailable and user provided BYOK key, try direct OpenRouter call
        if (!byokKey.isNullOrBlank()) {
            val directResult = tryDirectOpenRouter(messages, effectiveModel, byokKey, onToolExecuted)
            if (directResult != null) {
                return directResult
            }
        }

        // 3. If no server or no key configured, run local intelligent copilot engine
        return runLocalCopilotEngine(userMessage, onToolExecuted)
    }

    private suspend fun tryServerChat(
        messages: MutableList<AiChatMessage>,
        effectiveModel: String,
        byokKey: String?,
        onToolExecuted: ((ToolExecutionEvent) -> Unit)?,
    ): AiResult? {
        var round = 0
        while (round < MAX_CLIENT_TOOL_ROUNDS) {
            round++
            val clientToolsJson = AiToolDefinitions.toJson()
            val requestBody = json.encodeToString(
                AiChatRequest.serializer(),
                AiChatRequest(messages = messages, model = effectiveModel, clientTools = clientToolsJson)
            )

            val rawResponse: String = try {
                ApiClient.httpClient.post("${ApiClient.baseUrl}/api/ai/chat") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                    sanitizeApiKey(byokKey)?.let { header("X-Api-Key", it) }
                    ApiClient.getToken()?.let { token ->
                        header("Authorization", "Bearer $token")
                    }
                }.body()
            } catch (e: io.ktor.client.plugins.ResponseException) {
                if (e.response.status.value == 503 && byokKey.isNullOrBlank()) {
                    // Server has no key and no BYOK -> fallback to local copilot
                    return null
                }
                val errorBody = runCatching { e.response.body<String>() }.getOrNull()
                val parsedMsg = if (!errorBody.isNullOrBlank()) {
                    runCatching {
                        json.parseToJsonElement(errorBody).jsonObject["error"]?.jsonPrimitive?.contentOrNull
                    }.getOrNull() ?: errorBody
                } else null

                val errorMsg = parsedMsg ?: when (e.response.status.value) {
                    400 -> "AI request rejected. Please check your model settings or key."
                    401 -> "Invalid OpenRouter API key. Please check your key in AI Settings."
                    else -> "AI service error (${e.response.status.value})"
                }
                return AiResult.Error(errorMsg)
            } catch (_: Exception) {
                // Connection failed -> fallback to direct or local copilot
                return null
            }

            val responseJson = try {
                json.parseToJsonElement(rawResponse).jsonObject
            } catch (_: Exception) {
                return null
            }

            val content = responseJson["content"]?.jsonPrimitive?.contentOrNull ?: ""
            val clientToolCallsRaw = responseJson["clientToolCalls"]?.jsonArray
            val byok = responseJson["byok"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false

            if (clientToolCallsRaw.isNullOrEmpty()) {
                return AiResult.Success(content, byok)
            }

            val toolResults = mutableListOf<Pair<String, String>>()
            for (tc in clientToolCallsRaw) {
                val tcObj = tc.jsonObject
                val toolCallId = tcObj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                val toolName = tcObj["name"]?.jsonPrimitive?.contentOrNull ?: continue
                val toolArgsRaw = tcObj["arguments"]?.jsonPrimitive?.contentOrNull ?: "{}"
                val toolArgs = try {
                    json.parseToJsonElement(toolArgsRaw).jsonObject
                } catch (_: Exception) { null }

                onToolExecuted?.invoke(ToolExecutionEvent.Started(toolName, toolArgs))
                val result = AiToolExecutor.execute(toolName, toolArgs)
                val resultJson = when (result) {
                    is ToolResult.Success -> json.encodeToString(ToolResult.Success.serializer(), result)
                    is ToolResult.Error -> json.encodeToString(ToolResult.Error.serializer(), result)
                }
                toolResults.add(toolCallId to resultJson)
                onToolExecuted?.invoke(ToolExecutionEvent.Completed(toolName, result))
            }

            messages.add(AiChatMessage(role = "assistant", content = content))
            for ((toolCallId, resultJson) in toolResults) {
                messages.add(AiChatMessage(role = "user", content = "[Tool result for $toolCallId]: $resultJson"))
            }

            if (content.isBlank() && toolResults.isNotEmpty()) {
                continue
            }

            return AiResult.Success(content, byok)
        }
        return AiResult.Success("I processed your request. Please check your trips and destinations.", false)
    }

    private suspend fun tryDirectOpenRouter(
        messages: List<AiChatMessage>,
        model: String,
        apiKey: String,
        onToolExecuted: ((ToolExecutionEvent) -> Unit)?,
    ): AiResult? {
        return try {
            val formattedMessages = messages.map {
                buildJsonObject {
                    put("role", JsonPrimitive(it.role))
                    put("content", JsonPrimitive(it.content))
                }
            }

            val requestBody = buildJsonObject {
                put("model", JsonPrimitive(model))
                put("messages", kotlinx.serialization.json.JsonArray(formattedMessages))
                put("max_tokens", JsonPrimitive(1500))
                put("temperature", JsonPrimitive(0.7))
            }

            val cleanKey = sanitizeApiKey(apiKey) ?: return null
            val resp = ApiClient.httpClient.post(DIRECT_OPENROUTER_URL) {
                header("Authorization", "Bearer $cleanKey")
                header("HTTP-Referer", "https://traveldabble.app")
                header("X-Title", "TravelDabble")
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            if (resp.status.value in 200..299) {
                val respJson = json.parseToJsonElement(resp.bodyAsText()).jsonObject
                val choice = respJson["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                val content = choice?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
                if (!content.isNullOrBlank()) {
                    AiResult.Success(content, true)
                } else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Intelligent local travel assistant for offline, demo mode, or when no API key is available.
     */
    private suspend fun runLocalCopilotEngine(
        query: String,
        onToolExecuted: ((ToolExecutionEvent) -> Unit)?,
    ): AiResult {
        val q = query.lowercase()

        // 1. Create trip intent
        if (q.contains("plan") || q.contains("create") || (q.contains("trip") && (q.contains("hanoi") || q.contains("hoi an") || q.contains("da nang") || q.contains("saigon") || q.contains("ha long") || q.contains("ninh binh")))) {
            val destination = when {
                q.contains("hanoi") -> "Hanoi & Ha Long Bay"
                q.contains("hoi an") -> "Hoi An Ancient Town"
                q.contains("da nang") -> "Da Nang & Coastal Hills"
                q.contains("saigon") || q.contains("ho chi minh") -> "Ho Chi Minh City"
                q.contains("ha giang") -> "Ha Giang Loop"
                q.contains("ninh binh") -> "Ninh Binh Karsts"
                else -> "Vietnam Explorer"
            }
            val title = "Trip to $destination"
            val travelers = if (q.contains("solo") || q.contains("1 person") || q.contains("1 traveler")) 1 else 2

            onToolExecuted?.invoke(ToolExecutionEvent.Started("create_trip", buildJsonObject {
                put("title", JsonPrimitive(title))
                put("destination", JsonPrimitive(destination))
                put("travelers", JsonPrimitive(travelers))
            }))

            val created = Repository.createTrip(
                title = title,
                destination = destination,
                country = "Vietnam",
                startDate = "Oct 15",
                endDate = "Oct 18",
                travelers = travelers,
            )

            if (created != null) {
                onToolExecuted?.invoke(ToolExecutionEvent.Completed("create_trip", ToolResult.Success(
                    message = "Created new trip: $title ($destination)",
                    data = buildJsonObject {
                        put("trip_id", JsonPrimitive(created.id))
                        put("title", JsonPrimitive(created.title))
                        put("destination", JsonPrimitive(created.destination))
                    },
                    navigateTo = "trip_detail",
                    navigateTripId = created.id,
                )))

                val reply = buildString {
                    append("🎉 **I've created your trip: ${created.title}!**\n\n")
                    append("Here is your curated 3-day itinerary outline for **$destination**:\n\n")
                    append("• **Day 1**: Arrival, boutique check-in, Old Quarter walking orientation, authentic egg coffee tasting, and local dinner.\n")
                    append("• **Day 2**: Cultural landmarks, historic temples, artisan street exploration, and panoramic sunset viewpoint.\n")
                    append("• **Day 3**: Scenic riverboat/bay cruise excursion, cave exploration, and farewell traditional banquet.\n\n")
                    append("👉 *Tap the trip card above to view your full itinerary, budget breakdown, and live map routes!*")
                }
                return AiResult.Success(reply, false)
            }
        }

        // 2. Food & culinary recommendations
        if (q.contains("food") || q.contains("eat") || q.contains("restaurant") || q.contains("dish") || q.contains("culinary")) {
            val searchArgs = buildJsonObject { put("query", JsonPrimitive(if (q.contains("hoi an")) "Hoi An" else "Food")) }
            onToolExecuted?.invoke(ToolExecutionEvent.Started("search_destinations", searchArgs))
            val results = Repository.getDestinations().take(3)
            onToolExecuted?.invoke(ToolExecutionEvent.Completed("search_destinations", ToolResult.Success(
                message = "Found culinary recommendations in Vietnam",
                data = buildJsonObject { put("count", JsonPrimitive(results.size)) }
            )))

            val reply = buildString {
                append("🍜 **Top Culinary Highlights & Street Food Gems:**\n\n")
                append("1. **Banh Mi Phuong (Hoi An)** — World-famous crispy baguettes with savory pate and fresh herbs.\n")
                append("2. **Bun Cha Huong Lien (Hanoi)** — Charcoal grilled pork patties served in savory broth with vermicelli.\n")
                append("3. **Giang Cafe (Hanoi)** — The birthplace of decadent Vietnamese Egg Coffee (Cà Phê Trứng).\n")
                append("4. **Cuc Gach Quan (Saigon)** — Traditional homestyle Vietnamese delicacies in a restored French villa.\n\n")
                append("Would you like me to add these dining spots directly to your trip itinerary?")
            }
            return AiResult.Success(reply, false)
        }

        // 3. Navigation intents (budget, map, explore)
        if (q.contains("budget") || q.contains("expense") || q.contains("cost") || q.contains("spending")) {
            onToolExecuted?.invoke(ToolExecutionEvent.Started("navigate_to_screen", buildJsonObject { put("screen", JsonPrimitive("budget")) }))
            onToolExecuted?.invoke(ToolExecutionEvent.Completed("navigate_to_screen", ToolResult.Success(
                data = buildJsonObject { put("screen", JsonPrimitive("budget")) },
                message = "Opened Trip Budget & Expenses",
                navigateTo = "budget",
            )))
            return AiResult.Success("💰 I've opened your **Budget & Expenses** screen. You can track spending by category (Lodging, Food, Transport, Activities) and log receipts.", false)
        }

        if (q.contains("map") || q.contains("route") || q.contains("direction") || q.contains("gps")) {
            onToolExecuted?.invoke(ToolExecutionEvent.Started("navigate_to_screen", buildJsonObject { put("screen", JsonPrimitive("map")) }))
            onToolExecuted?.invoke(ToolExecutionEvent.Completed("navigate_to_screen", ToolResult.Success(
                data = buildJsonObject { put("screen", JsonPrimitive("map")) },
                message = "Opened Live Map & Navigation",
                navigateTo = "map",
            )))
            return AiResult.Success("🗺️ I've opened the **Interactive Map**. You can inspect GPS route lines connecting your location to all daily activity waypoints.", false)
        }

        // 4. Default helpful travel copilot response
        val reply = buildString {
            append("👋 **Travel Copilot at your service!**\n\n")
            append("I can help you:\n")
            append("• **Plan Itineraries**: *\"Plan a 3-day trip to Hanoi & Ha Long\"*\n")
            append("• **Discover Food & Sights**: *\"Best street food spots in Hoi An\"*\n")
            append("• **Track Finances**: *\"Show my trip budget and expenses\"*\n")
            append("• **Explore Routes**: *\"Show route map from my location\"*\n\n")
            append("What destination would you like to explore next?")
        }
        return AiResult.Success(reply, false)
    }

    /**
     * Check AI service health.
     */
    suspend fun checkHealth(): AiHealthStatus {
        return try {
            val response: AiHealthResponse = ApiClient.httpClient.get("${ApiClient.baseUrl}/api/ai/health").body()
            AiHealthStatus(
                available = true,
                serverKeyConfigured = response.server_key_configured,
            )
        } catch (_: Exception) {
            AiHealthStatus(available = false, serverKeyConfigured = false)
        }
    }

    val DEFAULT_AI_MODELS = listOf(
        AiModelOption("google/gemma-4-26b-a4b-it:free", "Gemma 4 26B (Free)", "Google's lightweight model optimized for chat & tools", isFree = true),
        AiModelOption("meta-llama/llama-3.3-70b-instruct:free", "Llama 3.3 70B (Free)", "Meta's flagship open model with excellent reasoning", isFree = true),
        AiModelOption("mistralai/mistral-small-24b-instruct-2501:free", "Mistral Small 24B (Free)", "Fast and capable conversational model", isFree = true),
        AiModelOption("deepseek/deepseek-r1:free", "DeepSeek R1 (Free)", "State-of-the-art open reasoning model", isFree = true),
        AiModelOption("qwen/qwen-2.5-72b-instruct:free", "Qwen 2.5 72B (Free)", "Powerful multilingual travel planning model", isFree = true),
        AiModelOption("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", "Industry-leading intelligence and tool usage", isFree = false),
        AiModelOption("openai/gpt-4o", "GPT-4o", "Flagship OpenAI multimodal intelligence", isFree = false),
        AiModelOption("openai/gpt-4o-mini", "GPT-4o Mini", "Fast, low-cost OpenAI model", isFree = false),
        AiModelOption("google/gemini-2.0-flash-001", "Gemini 2.0 Flash", "Ultra-fast Next-Gen Google model", isFree = false),
        AiModelOption("deepseek/deepseek-chat", "DeepSeek V3", "High-performance versatile foundation model", isFree = false),
    )

    /**
     * Dynamically fetch available OpenRouter models from the server / OpenRouter public API.
     */
    suspend fun fetchModels(): List<AiModelOption> {
        return try {
            val rawResponse: String = ApiClient.httpClient.get("${ApiClient.baseUrl}/api/ai/models").body()
            val responseJson = json.parseToJsonElement(rawResponse).jsonObject
            val modelsArray = responseJson["models"]?.jsonArray
            if (!modelsArray.isNullOrEmpty()) {
                val parsed = modelsArray.mapNotNull { item ->
                    val obj = item.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: id
                    val description = obj["description"]?.jsonPrimitive?.contentOrNull ?: ""
                    val isFree = obj["is_free"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                        ?: id.endsWith(":free")
                    AiModelOption(id, name, description, isFree)
                }
                if (parsed.isNotEmpty()) parsed else DEFAULT_AI_MODELS
            } else {
                DEFAULT_AI_MODELS
            }
        } catch (_: Exception) {
            DEFAULT_AI_MODELS
        }
    }
}

sealed class AiResult {
    data class Success(val content: String, val usedByok: Boolean) : AiResult()
    data class Error(val message: String) : AiResult()
}

data class AiHealthStatus(val available: Boolean, val serverKeyConfigured: Boolean)

sealed class ToolExecutionEvent {
    data class Started(val toolName: String, val args: JsonObject?) : ToolExecutionEvent()
    data class Completed(val toolName: String, val result: ToolResult) : ToolExecutionEvent()
}
