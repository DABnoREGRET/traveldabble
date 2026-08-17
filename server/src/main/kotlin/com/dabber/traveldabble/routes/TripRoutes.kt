package com.dabber.traveldabble.routes

import com.dabber.traveldabble.db.Budgets
import com.dabber.traveldabble.db.Mappers
import com.dabber.traveldabble.db.Mappers.toTrip
import com.dabber.traveldabble.db.TripMembers
import com.dabber.traveldabble.db.Trips
import com.dabber.traveldabble.model.ApiError
import com.dabber.traveldabble.model.CreateTripRequest
import com.dabber.traveldabble.util.Validation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.TripRoutes() {
    authenticate("auth-jwt") {
        route("/api/trips") {
            get {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@get
                }
                try {
                    val trips = transaction {
                        val ownedTrips = Trips.selectAll()
                            .where { Trips.userId eq userId }
                            .map { it[Trips.id].value }
                            .toSet()

                        val memberTripIds = TripMembers.selectAll()
                            .where { TripMembers.userId eq userId }
                            .map { it[TripMembers.tripId].value }
                            .toSet()

                        val allTripIds = ownedTrips + memberTripIds

                        Trips.selectAll()
                            .where { Trips.id inList allTripIds }
                            .map { it.toTrip() }
                    }
                    call.respond(trips)
                } catch (e: Exception) {
                    call.application.log.error("Failed to list trips", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to load trips"))
                }
            }

            post {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@post
                }
                val request = try {
                    call.receive<CreateTripRequest>()
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                    return@post
                }
                val errors = Validation.validateCreateTrip(request)
                if (errors.isNotEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, ApiError(errors.joinToString("; ")))
                    return@post
                }
                try {
                    val trip = transaction {
                        val tripId = UUID.randomUUID()
                        Trips.insert {
                            it[Trips.id] = tripId
                            it[Trips.userId] = userId
                            it[Trips.title] = request.title.trim()
                            it[Trips.destination] = request.destination.trim()
                            it[Trips.country] = request.country.trim()
                            it[Trips.startDate] = request.startDate.trim()
                            it[Trips.endDate] = request.endDate.trim()
                            it[Trips.daysUntil] = Validation.daysUntil(request.startDate.trim())
                            it[Trips.coverColors] = Mappers.json.encodeToString(Mappers.defaultCover)
                            it[Trips.travelers] = request.travelers
                            it[Trips.createdAt] = System.currentTimeMillis()
                        }
                        Budgets.insert {
                            it[Budgets.id] = UUID.randomUUID()
                            it[Budgets.tripId] = tripId
                            it[Budgets.total] = 0.0
                            it[Budgets.categories] = "[]"
                        }
                        TripMembers.insert {
                            it[TripMembers.id] = UUID.randomUUID()
                            it[TripMembers.tripId] = tripId
                            it[TripMembers.userId] = userId
                            it[TripMembers.role] = "owner"
                            it[TripMembers.joinedAt] = System.currentTimeMillis()
                        }
                        Trips.selectAll().where { Trips.id eq tripId }.single().toTrip()
                    }
                    call.respond(HttpStatusCode.Created, trip)
                } catch (e: Exception) {
                    call.application.log.error("Failed to create trip", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to create trip"))
                }
            }

            get("/{id}") {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@get
                }
                val tripId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                if (tripId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                    return@get
                }
                try {
                    val trip = transaction {
                        val isOwner = Trips.selectAll()
                            .where { (Trips.id eq tripId) and (Trips.userId eq userId) }
                            .singleOrNull() != null
                        val isMember = TripMembers.selectAll()
                            .where { (TripMembers.tripId eq tripId) and (TripMembers.userId eq userId) }
                            .singleOrNull() != null

                        if (isOwner || isMember) {
                            Trips.selectAll()
                                .where { Trips.id eq tripId }
                                .singleOrNull()
                                ?.toTrip()
                        } else {
                            null
                        }
                    }
                    if (trip == null) {
                        call.respond(HttpStatusCode.NotFound, ApiError("Trip not found"))
                    } else {
                        call.respond(trip)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Failed to load trip", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to load trip"))
                }
            }

            put("/{id}") {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@put
                }
                val tripId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                if (tripId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                    return@put
                }
                val request = try {
                    call.receive<CreateTripRequest>()
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                    return@put
                }
                val errors = Validation.validateCreateTrip(request)
                if (errors.isNotEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, ApiError(errors.joinToString("; ")))
                    return@put
                }
                try {
                    val updated = transaction {
                        val owned = Trips.selectAll()
                            .where { (Trips.id eq tripId) and (Trips.userId eq userId) }
                            .singleOrNull()
                        if (owned == null) {
                            null
                        } else {
                            Trips.update({ Trips.id eq tripId }) {
                                it[Trips.title] = request.title.trim()
                                it[Trips.destination] = request.destination.trim()
                                it[Trips.country] = request.country.trim()
                                it[Trips.startDate] = request.startDate.trim()
                                it[Trips.endDate] = request.endDate.trim()
                                it[Trips.daysUntil] = Validation.daysUntil(request.startDate.trim())
                                it[Trips.travelers] = request.travelers
                            }
                            Trips.selectAll().where { Trips.id eq tripId }.single().toTrip()
                        }
                    }
                    if (updated == null) {
                        call.respond(HttpStatusCode.NotFound, ApiError("Trip not found"))
                    } else {
                        call.respond(updated)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Failed to update trip", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to update trip"))
                }
            }

            delete("/{id}") {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@delete
                }
                val tripId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                if (tripId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                    return@delete
                }
                try {
                    val deleted = transaction {
                        val owned = Trips.selectAll()
                            .where { (Trips.id eq tripId) and (Trips.userId eq userId) }
                            .singleOrNull()
                        if (owned == null) {
                            false
                        } else {
                            Trips.deleteWhere { Trips.id eq tripId }
                            true
                        }
                    }
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, ApiError("Trip not found"))
                    }
                } catch (e: Exception) {
                    call.application.log.error("Failed to delete trip", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to delete trip"))
                }
            }
        }
    }
}
