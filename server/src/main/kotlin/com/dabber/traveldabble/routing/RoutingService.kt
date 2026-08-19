package com.dabber.traveldabble.routing

import com.dabber.traveldabble.model.Route
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class RoutingService {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val httpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
            defaultRequest {
                url(OSRM_BASE_URL)
            }
        }
    }

    private val routeCache = ConcurrentHashMap<String, Route>()

    private fun cacheKey(waypoints: List<Pair<Double, Double>>, profile: String): String {
        val wp = waypoints.joinToString(";") { "${it.first},${it.second}" }
        return "$profile:$wp"
    }

    suspend fun getRoute(
        waypoints: List<Pair<Double, Double>>,
        profile: String = "driving",
        overview: Boolean = true,
        steps: Boolean = true,
    ): Route? {
        if (waypoints.size < 2) return null

        val key = cacheKey(waypoints, profile)
        routeCache[key]?.let { return it }

        val coords = waypoints.joinToString(";") { "${it.second},${it.first}" }
        val url = "/route/v1/$profile/$coords?overview=${if (overview) "full" else "simplified"}&steps=$steps&alternatives=false"

        return try {
            val response: OsrmResponse = httpClient.get(url).body()
            if (response.code != "Ok" || response.routes.isEmpty()) return null
            val route = response.routes[0]
            routeCache[key] = route
            route
        } catch (_: Exception) {
            null
        }
    }

    fun clearCache() {
        routeCache.clear()
    }

    companion object {
        var OSRM_BASE_URL: String = System.getenv("OSRM_BASE_URL") ?: "https://router.project-osrm.org"
    }
}

@Serializable
private data class OsrmResponse(
    val code: String = "",
    val routes: List<Route> = emptyList(),
    val waypoints: List<OsrmWaypoint> = emptyList(),
)

@Serializable
private data class OsrmWaypoint(
    val location: List<Double> = emptyList(),
    val name: String = "",
)
