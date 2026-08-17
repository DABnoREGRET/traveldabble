package com.dabber.traveldabble.telemetry

import com.auth0.jwt.JWT
import com.dabber.traveldabble.db.Telemetry
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

val TelemetryPlugin: ApplicationPlugin<Unit> = createApplicationPlugin(
    name = "TelemetryPlugin"
) {
    onCall { call ->
        val startTime = System.currentTimeMillis()
        val request = call.request
        val optedOut = request.header("X-Telemetry-Opt-Out") == "true" ||
            request.queryParameters["telemetry"] == "false"

        val path = request.path()

        call.attributes.put(io.ktor.util.AttributeKey("StartTime"), startTime)
        call.attributes.put(io.ktor.util.AttributeKey("OptedOut"), optedOut)
    }

    onCallRespond { call, _ ->
        val startTime = call.attributes.getOrNull(io.ktor.util.AttributeKey<Long>("StartTime")) ?: return@onCallRespond
        val optedOut = call.attributes.getOrNull(io.ktor.util.AttributeKey<Boolean>("OptedOut")) ?: false
        val path = call.request.path()

        if (optedOut || path.startsWith("/health") || path.startsWith("/api/stats")) return@onCallRespond

        val endTime = System.currentTimeMillis()
        val responseTime = endTime - startTime

        try {
            transaction {
                Telemetry.insert {
                    it[timestamp] = System.currentTimeMillis()
                    it[eventType] = "api_call"
                    it[userId] = decodeUserId(call.request.headers["Authorization"])?.take(100)
                    it[endpoint] = path
                    it[method] = call.request.httpMethod.value
                    it[statusCode] = call.response.status()?.value ?: 200
                    it[responseTimeMs] = responseTime
                    it[userAgent] = call.request.userAgent()?.take(500)
                    it[ipAddress] = call.request.origin.remoteAddress.take(50)
                    it[metadata] = buildJsonObject {
                        put("query", call.request.queryString().take(500))
                    }.toString().take(1000)
                    it[optOut] = false
                }
            }
        } catch (_: Exception) {
            // Telemetry must never crash the response pipeline
        }
    }
}

private fun decodeUserId(authHeader: String?): String? {
    val token = authHeader?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        JWT.decode(token).subject?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
