package com.dabber.traveldabble.mcp

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val method: String,
    val params: JsonObject? = null,
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null,
)

@Serializable
data class JsonRpcError(val code: Int, val message: String, val data: JsonElement? = null)

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
)

fun Route.mcpRoutes() {
    get("/mcp") {
        call.respond(buildJsonObject {
            put("name", "TravelDabble")
            put("version", "1.0.0")
            put("description", "Travel planning and curated destination information for AI models")
            put("protocolVersion", "2024-11-05")
            put("capabilities", buildJsonObject {
                put("tools", true)
                put("resources", false)
                put("prompts", false)
            })
        })
    }

    post("/mcp") {
        val request = call.receive<JsonRpcRequest>()
        val response = try {
            when (request.method) {
                "initialize" -> handleInitialize(request)
                "tools/list" -> handleToolsList(request)
                "tools/call" -> handleToolsCall(request)
                else -> JsonRpcResponse(
                    id = request.id,
                    error = JsonRpcError(-32601, "Method not found: ${request.method}")
                )
            }
        } catch (e: Exception) {
            JsonRpcResponse(
                id = request.id,
                error = JsonRpcError(-32603, "Internal error: ${e.message}")
            )
        }
        call.respond(response)
    }
}

private fun handleInitialize(request: JsonRpcRequest): JsonRpcResponse {
    val result = buildJsonObject {
        put("protocolVersion", "2024-11-05")
        put("serverInfo", buildJsonObject {
            put("name", "TravelDabble")
            put("version", "1.0.0")
        })
        put("capabilities", buildJsonObject {
            put("tools", buildJsonObject {
                put("listChanged", false)
            })
        })
    }
    return JsonRpcResponse(id = request.id, result = result)
}

private fun handleToolsList(request: JsonRpcRequest): JsonRpcResponse {
    val result = buildJsonObject {
        putJsonArray("tools") {
            McpTools.definitions.forEach { tool ->
                addJsonObject {
                    put("name", tool.name)
                    put("description", tool.description)
                    put("inputSchema", tool.inputSchema)
                }
            }
        }
    }
    return JsonRpcResponse(id = request.id, result = result)
}

private suspend fun handleToolsCall(request: JsonRpcRequest): JsonRpcResponse {
    val params = request.params ?: return JsonRpcResponse(
        id = request.id,
        error = JsonRpcError(-32602, "Missing params")
    )
    val toolName = params["name"]?.jsonPrimitive?.content ?: return JsonRpcResponse(
        id = request.id,
        error = JsonRpcError(-32602, "Missing tool name")
    )
    val args = params["arguments"]?.jsonObject

    val result = McpTools.callTool(toolName, args, null)
    return JsonRpcResponse(id = request.id, result = result)
}
