package com.dabber.traveldabble.routes

import com.dabber.traveldabble.db.Telemetry
import com.dabber.traveldabble.model.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.ZoneId

fun Route.TelemetryRoutes() {
    route("/api/telemetry") {
        post("/events") {
            val event = try {
                call.receive<TelemetryEventRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Invalid telemetry payload"))
                return@post
            }

            if (event.optOut) {
                call.respond(HttpStatusCode.Accepted, mapOf("status" to "opted_out"))
                return@post
            }

            val userId = call.userId()?.toString()

            try {
                transaction {
                    Telemetry.insert {
                        it[timestamp] = System.currentTimeMillis()
                        it[eventType] = event.eventType
                        it[Telemetry.userId] = userId
                        it[screenName] = event.screenName
                        it[durationMs] = event.durationMs
                        it[connectionType] = event.connectionType
                        it[memoryMb] = event.memoryMb
                        it[exceptionHash] = event.exceptionHash
                        it[metadata] = event.metadata
                        it[optOut] = false
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "recorded"))
            } catch (_: Exception) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "ignored"))
            }
        }
    }

    get("/api/stats") {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val stats = transaction {
            val nonOptOutQuery = (Telemetry.timestamp greaterEq startOfDay) and (Telemetry.optOut eq false)

            val totalCalls = Telemetry.selectAll()
                .where { nonOptOutQuery and (Telemetry.eventType eq "api_call") }
                .count()

            val uniqueUsers = Telemetry.select(Telemetry.userId)
                .where { nonOptOutQuery and (Telemetry.userId.isNotNull()) and (Telemetry.userId neq "") }
                .distinct()
                .count()

            val latencies = Telemetry.select(Telemetry.responseTimeMs)
                .where { nonOptOutQuery and (Telemetry.eventType eq "api_call") and (Telemetry.responseTimeMs.isNotNull()) }
                .mapNotNull { it[Telemetry.responseTimeMs] }
                .sorted()

            val avgResponse = if (latencies.isEmpty()) 0.0 else latencies.average()
            val p50 = percentile(latencies, 50.0)
            val p95 = percentile(latencies, 95.0)
            val p99 = percentile(latencies, 99.0)

            val totalErrors = Telemetry.selectAll()
                .where { nonOptOutQuery and (Telemetry.eventType eq "api_call") and (Telemetry.statusCode greaterEq 400) }
                .count()
            val errorRate = if (totalCalls == 0L) 0.0 else (totalErrors.toDouble() / totalCalls) * 100.0

            val topEndpoints = Telemetry.select(Telemetry.endpoint, Telemetry.endpoint.count(), Telemetry.responseTimeMs.avg())
                .where { nonOptOutQuery and (Telemetry.eventType eq "api_call") and (Telemetry.endpoint.isNotNull()) }
                .groupBy(Telemetry.endpoint)
                .orderBy(Telemetry.endpoint.count(), SortOrder.DESC)
                .limit(5)
                .map {
                    EndpointUsage(
                        endpoint = it[Telemetry.endpoint] ?: "",
                        count = it[Telemetry.endpoint.count()].toLong(),
                        avgResponseTimeMs = it[Telemetry.responseTimeMs.avg()]?.toDouble() ?: 0.0,
                    )
                }

            val totalClientEvents = Telemetry.selectAll()
                .where { nonOptOutQuery and (Telemetry.eventType neq "api_call") }
                .count()

            val clientMetrics = ClientTelemetrySummary(
                totalClientEvents = totalClientEvents,
                avgColdStartMs = Telemetry.select(Telemetry.durationMs)
                    .where { nonOptOutQuery and (Telemetry.eventType eq "app_startup") }
                    .mapNotNull { it[Telemetry.durationMs] }
                    .let { if (it.isEmpty()) 0.0 else it.average() },
                reportedCrashes = Telemetry.selectAll()
                    .where { nonOptOutQuery and (Telemetry.eventType eq "crash") }
                    .count(),
            )

            UsageSummary(
                date = today.toString(),
                totalCalls = totalCalls,
                uniqueUsers = uniqueUsers.toLong(),
                avgResponseTimeMs = avgResponse,
                topEndpoints = topEndpoints,
                p50ResponseTimeMs = p50,
                p95ResponseTimeMs = p95,
                p99ResponseTimeMs = p99,
                errorRate = errorRate,
                slowEndpoints = topEndpoints.sortedByDescending { it.avgResponseTimeMs },
                clientMetrics = clientMetrics,
            )
        }

        call.respond(stats)
    }
}

private fun percentile(sortedList: List<Long>, percentile: Double): Double {
    if (sortedList.isEmpty()) return 0.0
    val index = Math.ceil(percentile / 100.0 * sortedList.size).toInt() - 1
    return sortedList[index.coerceIn(0, sortedList.size - 1)].toDouble()
}
