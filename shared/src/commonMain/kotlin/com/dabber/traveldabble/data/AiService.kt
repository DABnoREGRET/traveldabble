package com.dabber.traveldabble.data

import com.dabber.traveldabble.model.LocalChatMessage
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
 * AI service with tool-calling support.
 *
 * Flow:
 * 1. Send message + client tool definitions to server
 * 2. Server executes destination tools automatically
 * 3. Server returns clientToolCalls for tools we need to execute locally
 * 4. We execute client tools, collect results, send follow-up
 * 5. Repeat until AI gives final text response
 */
object AiService {
    private val json = Json { ignoreUnknownKeys = true }
    private const val MAX_CONTEXT_MESSAGES = 20
    private const val MAX_CLIENT_TOOL_ROUNDS = 4

    /**
     * Send a chat message and handle tool execution.
     * Returns a flow of events for the UI to render.
     */
    suspend fun sendMessage(
        tripId: String,
        userMessage: String,
        byokKey: String? = null,
        model: String? = null,
        onToolExecuted: ((ToolExecutionEvent) -> Unit)? = null,
    ): AiResult {
        // Build message history from local storage
        val history = LocalChatStorage.loadMessages(tripId)
            .takeLast(MAX_CONTEXT_MESSAGES)
            .map { msg ->
                AiChatMessage(
                    role = if (msg.senderId == "ai") "assistant" else "user",
                    content = msg.text,
                )
            }

        // System prompt with tool usage instructions
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

        // Tool-calling loop (client-side tools)
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
                    byokKey?.let { header("X-Api-Key", it) }
                    ApiClient.getToken()?.let { token ->
                        header("Authorization", "Bearer $token")
                    }
                }.body()
            } catch (e: Exception) {
                return AiResult.Error(e.message ?: "AI service unavailable")
            }

            val responseJson = json.parseToJsonElement(rawResponse).jsonObject
            val content = responseJson["content"]?.jsonPrimitive?.contentOrNull ?: ""
            val clientToolCallsRaw = responseJson["clientToolCalls"]?.jsonArray
            val byok = responseJson["byok"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false

            // No client tools to execute → return final response
            if (clientToolCallsRaw.isNullOrEmpty()) {
                return AiResult.Success(content, byok)
            }

            // Execute client-side tools
            val toolResults = mutableListOf<Pair<String, String>>() // tool_call_id, result_json

            for (tc in clientToolCallsRaw) {
                val tcObj = tc.jsonObject
                val toolCallId = tcObj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                val toolName = tcObj["name"]?.jsonPrimitive?.contentOrNull ?: continue
                val toolArgsRaw = tcObj["arguments"]?.jsonPrimitive?.contentOrNull ?: "{}"
                val toolArgs = try {
                    json.parseToJsonElement(toolArgsRaw).jsonObject
                } catch (_: Exception) { null }

                // Notify UI of tool execution
                onToolExecuted?.invoke(ToolExecutionEvent.Started(toolName, toolArgs))

                val result = AiToolExecutor.execute(toolName, toolArgs)

                val resultJson = when (result) {
                    is ToolResult.Success -> json.encodeToString(
                        ToolResult.Success.serializer(),
                        result
                    )
                    is ToolResult.Error -> json.encodeToString(
                        ToolResult.Error.serializer(),
                        result
                    )
                }

                toolResults.add(toolCallId to resultJson)
                onToolExecuted?.invoke(ToolExecutionEvent.Completed(toolName, result))
            }

            // Add assistant message with tool calls to history
            messages.add(AiChatMessage(role = "assistant", content = content))
            // Add tool results as follow-up messages
            for ((toolCallId, resultJson) in toolResults) {
                messages.add(AiChatMessage(
                    role = "user",
                    content = "[Tool result for $toolCallId]: $resultJson"
                ))
            }

            // If no content from AI but tools were executed, continue loop
            // so AI can generate a natural language summary
            if (content.isBlank() && toolResults.isNotEmpty()) {
                continue
            }

            // Return with content (tools were already executed, UI was notified)
            return AiResult.Success(content, byok)
        }

        return AiResult.Success("I processed your request. Please check the results.", false)
    }

    /**
     * Check AI service health.
     */
    suspend fun checkHealth(): AiHealthStatus {
        return try {
            val response: AiHealthResponse = ApiClient.httpClient.post("${ApiClient.baseUrl}/api/ai/health") {
                // No auth needed for health check
            }.body()
            AiHealthStatus(
                available = true,
                serverKeyConfigured = response.server_key_configured,
            )
        } catch (e: Exception) {
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
