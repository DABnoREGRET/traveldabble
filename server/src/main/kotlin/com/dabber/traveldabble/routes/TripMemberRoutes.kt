package com.dabber.traveldabble.routes

import com.dabber.traveldabble.db.InviteCodes
import com.dabber.traveldabble.db.TripMembers
import com.dabber.traveldabble.db.Trips
import com.dabber.traveldabble.db.Users
import com.dabber.traveldabble.model.ApiError
import com.dabber.traveldabble.model.GenerateInviteCodeRequest
import com.dabber.traveldabble.model.JoinTripRequest
import com.dabber.traveldabble.model.TripMember
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
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
data class UpdateMemberRoleRequest(
    val role: String,
)

@Serializable
data class InviteResponse(
    val id: String,
    val code: String,
    val expiresAt: Long? = null,
)

@Serializable
data class InviteInfo(
    val id: String,
    val code: String,
    val createdAt: Long,
    val expiresAt: Long? = null,
    val maxUses: Int? = null,
    val useCount: Int = 0,
)

@Serializable
data class JoinTripResponse(
    val tripId: String,
    val message: String,
)

@Serializable
data class MessageResponse(
    val message: String,
)

fun Route.TripMemberRoutes() {
    authenticate("auth-jwt") {
        route("/api/trips/{tripId}/invite") {
            post {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@post
                }
                val tripId = call.parameters["tripId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                if (tripId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                    return@post
                }
                val request = try {
                    call.receive<GenerateInviteCodeRequest>()
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                    return@post
                }
                try {
                    val membership = transaction {
                        TripMembers.selectAll()
                            .where { (TripMembers.tripId eq tripId) and (TripMembers.userId eq userId) }
                            .singleOrNull()
                    }
                    if (membership == null || membership[TripMembers.role] == "viewer") {
                        call.respond(HttpStatusCode.Forbidden, ApiError("Not authorized to generate invites"))
                        return@post
                    }

                    val inviteId = UUID.randomUUID()
                    val code = UUID.randomUUID().toString().take(8).uppercase()
                    val now = System.currentTimeMillis()
                    val expiresAt = request.expiresInHours?.let { now + it * 3600_000L }

                    transaction {
                        InviteCodes.insert {
                            it[InviteCodes.id] = inviteId
                            it[InviteCodes.tripId] = tripId
                            it[InviteCodes.code] = code
                            it[InviteCodes.createdBy] = userId
                            it[InviteCodes.createdAt] = now
                            it[InviteCodes.expiresAt] = expiresAt
                            it[InviteCodes.maxUses] = request.maxUses
                            it[InviteCodes.useCount] = 0
                        }
                    }
                    call.respond(InviteResponse(inviteId.toString(), code, expiresAt))
                } catch (e: Exception) {
                    call.application.log.error("Failed to generate invite", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to generate invite"))
                }
            }

            get {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@get
                }
                val tripId = call.parameters["tripId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                if (tripId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                    return@get
                }
                try {
                    val invites = transaction {
                        InviteCodes.selectAll()
                            .where { InviteCodes.tripId eq tripId }
                            .orderBy(InviteCodes.createdAt, SortOrder.DESC)
                            .map { row ->
                                InviteInfo(
                                    id = row[InviteCodes.id].value.toString(),
                                    code = row[InviteCodes.code],
                                    createdAt = row[InviteCodes.createdAt],
                                    expiresAt = row[InviteCodes.expiresAt],
                                    maxUses = row[InviteCodes.maxUses],
                                    useCount = row[InviteCodes.useCount],
                                )
                            }
                    }
                    call.respond(invites)
                } catch (e: Exception) {
                    call.application.log.error("Failed to list invites", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to load invites"))
                }
            }

            delete("/{inviteId}") {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@delete
                }
                val inviteId = call.parameters["inviteId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                if (inviteId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid invite id"))
                    return@delete
                }
                try {
                    transaction {
                        InviteCodes.deleteWhere { InviteCodes.id eq inviteId }
                    }
                    call.respond(HttpStatusCode.NoContent)
                } catch (e: Exception) {
                    call.application.log.error("Failed to delete invite", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to delete invite"))
                }
            }
        }

        post("/api/trips/join") {
            val userId = call.userId()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                return@post
            }
            val request = try {
                call.receive<JoinTripRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                return@post
            }
            try {
                val result = transaction {
                    val invite = InviteCodes.selectAll()
                        .where { InviteCodes.code eq request.code }
                        .singleOrNull()

                    if (invite == null) {
                        return@transaction null to "Invalid invite code"
                    }

                    val now = System.currentTimeMillis()
                    if (invite[InviteCodes.expiresAt] != null && invite[InviteCodes.expiresAt]!! < now) {
                        return@transaction null to "Invite code has expired"
                    }
                    if (invite[InviteCodes.maxUses] != null && invite[InviteCodes.useCount] >= invite[InviteCodes.maxUses]!!) {
                        return@transaction null to "Invite code has reached maximum uses"
                    }

                    val tripId = invite[InviteCodes.tripId].value

                    val existingMember = TripMembers.selectAll()
                        .where { (TripMembers.tripId eq tripId) and (TripMembers.userId eq userId) }
                        .singleOrNull()
                    if (existingMember != null) {
                        return@transaction null to "You are already a member of this trip"
                    }

                    TripMembers.insert {
                        it[TripMembers.id] = UUID.randomUUID()
                        it[TripMembers.tripId] = tripId
                        it[TripMembers.userId] = userId
                        it[TripMembers.role] = "member"
                        it[TripMembers.joinedAt] = now
                    }

                    InviteCodes.update({ InviteCodes.id eq invite[InviteCodes.id].value }) {
                        it[InviteCodes.useCount] = invite[InviteCodes.useCount] + 1
                    }

                    tripId to null
                }

                val (tripId, error) = result
                if (error != null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError(error))
                } else {
                    call.respond(JoinTripResponse(tripId.toString(), "Successfully joined trip"))
                }
            } catch (e: Exception) {
                call.application.log.error("Failed to join trip", e)
                call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to join trip"))
            }
        }

        get("/api/trips/{tripId}/members") {
            val userId = call.userId()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                return@get
            }
            val tripId = call.parameters["tripId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (tripId == null) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                return@get
            }
            try {
                val members = transaction {
                    TripMembers.selectAll()
                        .where { TripMembers.tripId eq tripId }
                        .map { row ->
                            val user = Users.selectAll()
                                .where { Users.id eq row[TripMembers.userId].value }
                                .singleOrNull()
                            TripMember(
                                userId = row[TripMembers.userId].value.toString(),
                                displayName = user?.get(Users.displayName) ?: "Unknown",
                                email = user?.get(Users.email) ?: "",
                                role = row[TripMembers.role],
                                joinedAt = row[TripMembers.joinedAt],
                            )
                        }
                }
                call.respond(members)
            } catch (e: Exception) {
                call.application.log.error("Failed to load members", e)
                call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to load members"))
            }
        }

        put("/api/trips/{tripId}/members/{memberUserId}") {
            val userId = call.userId()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                return@put
            }
            val tripId = call.parameters["tripId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            val memberUserId = call.parameters["memberUserId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (tripId == null || memberUserId == null) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip or member id"))
                return@put
            }
            val request = try {
                call.receive<UpdateMemberRoleRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                return@put
            }
            try {
                val updated = transaction {
                    val currentMember = TripMembers.selectAll()
                        .where { (TripMembers.tripId eq tripId) and (TripMembers.userId eq userId) }
                        .singleOrNull()
                    if (currentMember == null || currentMember[TripMembers.role] != "owner") {
                        return@transaction false
                    }

                    TripMembers.update({
                        (TripMembers.tripId eq tripId) and (TripMembers.userId eq memberUserId)
                    }) {
                        it[TripMembers.role] = request.role
                    }
                    true
                }
                if (updated) {
                    call.respond(MessageResponse("Role updated"))
                } else {
                    call.respond(HttpStatusCode.Forbidden, ApiError("Not authorized or member not found"))
                }
            } catch (e: Exception) {
                call.application.log.error("Failed to update member role", e)
                call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to update member role"))
            }
        }

        delete("/api/trips/{tripId}/members/{memberUserId}") {
            val userId = call.userId()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                return@delete
            }
            val tripId = call.parameters["tripId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            val memberUserId = call.parameters["memberUserId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (tripId == null || memberUserId == null) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip or member id"))
                return@delete
            }
            try {
                val removed = transaction {
                    val currentMember = TripMembers.selectAll()
                        .where { (TripMembers.tripId eq tripId) and (TripMembers.userId eq userId) }
                        .singleOrNull()

                    if (currentMember == null) {
                        return@transaction false
                    }

                    val isOwner = currentMember[TripMembers.role] == "owner"
                    val isSelf = userId == memberUserId

                    if (!isOwner && !isSelf) {
                        return@transaction false
                    }

                    TripMembers.deleteWhere {
                        (TripMembers.tripId eq tripId) and (TripMembers.userId eq memberUserId)
                    }
                    true
                }
                if (removed) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.Forbidden, ApiError("Not authorized or member not found"))
                }
            } catch (e: Exception) {
                call.application.log.error("Failed to remove member", e)
                call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to remove member"))
            }
        }
    }
}
