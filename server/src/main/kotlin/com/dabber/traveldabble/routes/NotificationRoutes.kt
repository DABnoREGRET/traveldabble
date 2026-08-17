package com.dabber.traveldabble.routes

import com.dabber.traveldabble.db.Notifications
import com.dabber.traveldabble.db.UserFcmTokens
import com.dabber.traveldabble.model.ApiError
import com.dabber.traveldabble.model.FcmTokenRequest
import com.dabber.traveldabble.model.InAppNotification
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.NotificationRoutes() {
    authenticate("auth-jwt") {
        route("/api/notifications") {
            get {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@get
                }
                try {
                    val notifications = transaction {
                        Notifications.selectAll()
                            .where { Notifications.userId eq userId.toString() }
                            .orderBy(Notifications.createdAt, SortOrder.DESC)
                            .limit(50)
                            .map { row ->
                                InAppNotification(
                                    id = row[Notifications.id],
                                    userId = row[Notifications.userId],
                                    type = row[Notifications.type],
                                    title = row[Notifications.title],
                                    body = row[Notifications.body],
                                    data = row[Notifications.data],
                                    read = row[Notifications.read],
                                    createdAt = row[Notifications.createdAt],
                                )
                            }
                    }
                    call.respond(notifications)
                } catch (e: Exception) {
                    call.application.log.error("Failed to load notifications", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to load notifications"))
                }
            }

            post("/{notificationId}/read") {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@post
                }
                val notificationId = call.parameters["notificationId"]?.toLongOrNull()
                if (notificationId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid notification id"))
                    return@post
                }
                try {
                    transaction {
                        Notifications.update({
                            (Notifications.id eq notificationId) and
                            (Notifications.userId eq userId.toString())
                        }) {
                            it[Notifications.read] = true
                        }
                    }
                    call.respond(mapOf("message" to "Marked as read"))
                } catch (e: Exception) {
                    call.application.log.error("Failed to mark notification", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to update notification"))
                }
            }

            post("/read-all") {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@post
                }
                try {
                    transaction {
                        Notifications.update({
                            Notifications.userId eq userId.toString()
                        }) {
                            it[Notifications.read] = true
                        }
                    }
                    call.respond(mapOf("message" to "All marked as read"))
                } catch (e: Exception) {
                    call.application.log.error("Failed to mark all notifications", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to update notifications"))
                }
            }

            delete("/{notificationId}") {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@delete
                }
                val notificationId = call.parameters["notificationId"]?.toLongOrNull()
                if (notificationId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid notification id"))
                    return@delete
                }
                try {
                    transaction {
                        Notifications.deleteWhere {
                            (Notifications.id eq notificationId) and
                            (Notifications.userId eq userId.toString())
                        }
                    }
                    call.respond(HttpStatusCode.NoContent)
                } catch (e: Exception) {
                    call.application.log.error("Failed to delete notification", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to delete notification"))
                }
            }
        }

        route("/api/fcm") {
            post("/register") {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@post
                }
                val request = try {
                    call.receive<FcmTokenRequest>()
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                    return@post
                }
                try {
                    transaction {
                        UserFcmTokens.deleteWhere {
                            (UserFcmTokens.userId eq userId.toString()) and
                            (UserFcmTokens.platform eq request.platform)
                        }
                        UserFcmTokens.insert {
                            it[UserFcmTokens.userId] = userId.toString()
                            it[UserFcmTokens.token] = request.token
                            it[UserFcmTokens.platform] = request.platform
                            it[UserFcmTokens.createdAt] = System.currentTimeMillis()
                        }
                    }
                    call.respond(mapOf("message" to "FCM token registered"))
                } catch (e: Exception) {
                    call.application.log.error("Failed to register FCM token", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to register token"))
                }
            }

            delete("/unregister") {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                    return@delete
                }
                try {
                    transaction {
                        UserFcmTokens.deleteWhere {
                            UserFcmTokens.userId eq userId.toString()
                        }
                    }
                    call.respond(mapOf("message" to "FCM tokens unregistered"))
                } catch (e: Exception) {
                    call.application.log.error("Failed to unregister FCM tokens", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to unregister tokens"))
                }
            }
        }
    }
}
