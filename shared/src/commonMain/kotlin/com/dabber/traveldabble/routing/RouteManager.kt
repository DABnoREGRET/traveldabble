package com.dabber.traveldabble.routing

import com.dabber.traveldabble.data.ApiClient
import com.dabber.traveldabble.model.Route
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * High-performance polyline decoder implementing the Google Encoded Polyline Algorithm (precision 1e-5).
 */
object PolylineDecoder {
    fun decode(encoded: String): List<Pair<Double, Double>> {
        val poly = ArrayList<Pair<Double, Double>>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat

            shift = 0
            result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng

            val pLat = lat / 1E5
            val pLng = lng / 1E5
            poly.add(pLat to pLng)
        }
        return poly
    }
}

/**
 * RouteManager coordinates road-network routing requests, caching, and fallback logic.
 */
object RouteManager {
    private val memoryCache = mutableMapOf<String, List<Pair<Double, Double>>>()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun makeKey(waypoints: List<Pair<Double, Double>>, profile: String): String {
        return "$profile:" + waypoints.joinToString(";") { "${(it.first * 10000).toInt()},${(it.second * 10000).toInt()}" }
    }

    /**
     * Resolves turn-by-turn road coordinates for a sequence of [waypoints] (lat, lng).
     * Falls back to straight-line waypoints if routing services are unreachable.
     */
    suspend fun getRoadwayCoordinates(
        waypoints: List<Pair<Double, Double>>,
        profile: String = "driving",
    ): List<Pair<Double, Double>> {
        if (waypoints.size < 2) return waypoints

        val key = makeKey(waypoints, profile)
        memoryCache[key]?.let { return it }

        // 1. Try TravelDabble Backend API
        try {
            val serverRoute = ApiClient.getRoute(waypoints, profile)
            val geom = serverRoute?.geometry
            if (!geom.isNullOrBlank()) {
                val decoded = PolylineDecoder.decode(geom)
                if (decoded.isNotEmpty()) {
                    memoryCache[key] = decoded
                    return decoded
                }
            }
        } catch (_: Throwable) {}

        // 2. Direct fallback to OpenStreetMap / OSRM routing engine
        try {
            val coords = waypoints.joinToString(";") { "${it.second},${it.first}" }
            val url = "https://router.project-osrm.org/route/v1/$profile/$coords?overview=full&geometries=polyline"
            val response: HttpResponse = ApiClient.httpClient.get(url)
            if (response.status.value in 200..299) {
                val osrmResp = json.decodeFromString<DirectOsrmResponse>(response.body())
                val geom = osrmResp.routes.firstOrNull()?.geometry
                if (!geom.isNullOrBlank()) {
                    val decoded = PolylineDecoder.decode(geom)
                    if (decoded.isNotEmpty()) {
                        memoryCache[key] = decoded
                        return decoded
                    }
                }
            }
        } catch (_: Throwable) {}

        // 3. Fallback to direct waypoints
        return waypoints
    }
}

@Serializable
private data class DirectOsrmResponse(
    val code: String = "",
    val routes: List<DirectOsrmRoute> = emptyList(),
)

@Serializable
private data class DirectOsrmRoute(
    val geometry: String? = null,
    val distance: Double = 0.0,
    val duration: Double = 0.0,
)
