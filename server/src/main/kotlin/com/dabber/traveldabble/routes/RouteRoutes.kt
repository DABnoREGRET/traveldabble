package com.dabber.traveldabble.routes

import com.dabber.traveldabble.routing.RoutingService
import com.dabber.traveldabble.util.rateLimited
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class RouteRequest(
    val waypoints: List<List<Double>>,
    val profile: String? = null,
)

fun Route.RouteRoutes() {
    val routingService = RoutingService()

    route("/api/route") {
        rateLimited(limit = 60, windowMillis = 60_000) {
            post {
                val request = try {
                    call.receive<RouteRequest>()
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
                    return@post
                }

                if (request.waypoints.size < 2) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "At least 2 waypoints are required"))
                    return@post
                }

                val waypoints = request.waypoints.mapNotNull {
                    if (it.size >= 2) it[0] to it[1] else null
                }

                if (waypoints.size < 2) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid coordinates in waypoints"))
                    return@post
                }

                val invalid = waypoints.firstOrNull { (lat, lng) ->
                    lat.isNaN() || lng.isNaN() || lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0
                }
                if (invalid != null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Coordinates out of range (lat -90..90, lng -180..180)"))
                    return@post
                }

                val profile = request.profile ?: "driving"
                val route = routingService.getRoute(waypoints, profile = profile)
                if (route != null) {
                    call.respond(route)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Routing service unavailable or route not found"))
                }
            }
        }

        get("/test") {
            call.respond(mapOf("status" to "ok", "osrm" to RoutingService.OSRM_BASE_URL))
        }
    }
}
