package com.dabber.traveldabble.routes

import com.dabber.traveldabble.auth.JwtService
import com.dabber.traveldabble.auth.PasswordService
import com.dabber.traveldabble.db.Users
import com.dabber.traveldabble.model.ApiError
import com.dabber.traveldabble.model.AuthResponse
import com.dabber.traveldabble.model.LoginRequest
import com.dabber.traveldabble.model.RegisterRequest
import com.dabber.traveldabble.util.Validation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/** Extracts the authenticated user id from the JWT principal, or null when absent/invalid. */
internal fun ApplicationCall.userId(): UUID? =
    principal<JWTPrincipal>()?.payload?.subject?.let { subject ->
        runCatching { UUID.fromString(subject) }.getOrNull()
    }

fun Route.AuthRoutes() {
    route("/api/auth") {
        post("/register") {
            val request = try {
                call.receive<RegisterRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                return@post
            }
            val errors = Validation.validateRegister(request)
            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ApiError(errors.joinToString("; ")))
                return@post
            }
            try {
                val exists = transaction {
                    Users.selectAll().where {
                        (Users.username eq request.username) or (Users.email eq request.email)
                    }.any()
                }
                if (exists) {
                    call.respond(HttpStatusCode.Conflict, ApiError("Username or email already registered"))
                    return@post
                }

                val response = transaction {
                    val id = UUID.randomUUID()
                    val hash = PasswordService.hash(request.password)
                    Users.insert {
                        it[Users.id] = id
                        it[Users.username] = request.username.trim()
                        it[Users.email] = request.email.trim()
                        it[Users.passwordHash] = hash
                        it[Users.displayName] = request.displayName.trim()
                        it[Users.createdAt] = System.currentTimeMillis()
                    }
                    AuthResponse(
                        token = JwtService.createToken(id.toString()),
                        userId = id.toString(),
                        displayName = request.displayName.trim(),
                        email = request.email.trim(),
                    )
                }
                call.respond(HttpStatusCode.Created, response)
            } catch (e: Exception) {
                call.application.log.error("Registration failed", e)
                call.respond(HttpStatusCode.InternalServerError, ApiError("Registration failed"))
            }
        }

        post("/login") {
            val request = try {
                call.receive<LoginRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                return@post
            }
            val errors = Validation.validateLogin(request)
            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ApiError(errors.joinToString("; ")))
                return@post
            }
            val user = transaction {
                Users.selectAll().where { Users.email eq request.email.trim() }.singleOrNull()
            }
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Invalid email or password"))
                return@post
            }
            val verified = PasswordService.verify(request.password, user[Users.passwordHash])
            if (!verified) {
                call.respond(HttpStatusCode.Unauthorized, ApiError("Invalid email or password"))
                return@post
            }
            call.respond(
                AuthResponse(
                    token = JwtService.createToken(user[Users.id].value.toString()),
                    userId = user[Users.id].value.toString(),
                    displayName = user[Users.displayName],
                    email = user[Users.email],
                )
            )
        }

        authenticate("auth-jwt") {
            get("/me") {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@get
                }
                val user = try {
                    transaction {
                        Users.selectAll().where { Users.id eq userId }.singleOrNull()
                    }
                } catch (e: Exception) {
                    call.application.log.error("Failed to load profile", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to load profile"))
                    return@get
                }
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, ApiError("User not found"))
                    return@get
                }
                call.respond(
                    AuthResponse(
                        token = "",
                        userId = userId.toString(),
                        displayName = user[Users.displayName],
                        email = user[Users.email],
                    )
                )
            }
        }
    }
}
