package com.dabber.traveldabble.routes

import com.dabber.traveldabble.db.Destinations
import com.dabber.traveldabble.db.Mappers.toDestination
import com.dabber.traveldabble.model.ApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.DestinationRoutes() {
    route("/api/destinations") {
        get {
            try {
                val destinations = transaction {
                    Destinations.selectAll().map { it.toDestination() }
                }
                call.respond(destinations)
            } catch (e: Exception) {
                call.application.log.error("Failed to list destinations", e)
                call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to load destinations"))
            }
        }

        get("/{id}") {
            val destinationId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (destinationId == null) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Invalid destination id"))
                return@get
            }
            try {
                val destination = transaction {
                    Destinations.selectAll()
                        .where { Destinations.id eq destinationId }
                        .singleOrNull()
                        ?.toDestination()
                }
                if (destination == null) {
                    call.respond(HttpStatusCode.NotFound, ApiError("Destination not found"))
                } else {
                    call.respond(destination)
                }
            } catch (e: Exception) {
                call.application.log.error("Failed to load destination", e)
                call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to load destination"))
            }
        }
    }
}
