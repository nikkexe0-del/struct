package com.zestyy.struct.routing

import com.zestyy.struct.util.GeoMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class OsrmProfile(val path: String) { FOOT("foot"), BIKE("bike"), DRIVING("driving") }

data class OsrmResult(val points: List<GeoMath.Point>, val distanceMeters: Double, val durationSec: Double)

/**
 * Thin client for a public or self-hosted OSRM server. Only used for the Route Builder's
 * "road-snapped" mode — everything else in the app works without it.
 *
 * Public demo servers (router.project-osrm.org) are rate-limited and only route "driving".
 * For real foot/bike routing, self-host OSRM (see README) and point [baseUrl] at it.
 */
class OsrmClient(
    private val baseUrl: String = "https://router.project-osrm.org"
) {
    suspend fun route(waypoints: List<GeoMath.Point>, profile: OsrmProfile): Result<OsrmResult> =
        withContext(Dispatchers.IO) {
            if (waypoints.size < 2) return@withContext Result.failure(IllegalArgumentException("need >= 2 waypoints"))
            try {
                val coords = waypoints.joinToString(";") { "${it.lng},${it.lat}" }
                val url = URL("$baseUrl/route/v1/${profile.path}/$coords?overview=full&geometries=polyline")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 15_000
                }
                val code = conn.responseCode
                if (code != 200) {
                    return@withContext Result.failure(RuntimeException("OSRM HTTP $code"))
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                if (json.optString("code") != "Ok") {
                    return@withContext Result.failure(RuntimeException("OSRM error: ${json.optString("code")}"))
                }
                val route = json.getJSONArray("routes").getJSONObject(0)
                val geometry = route.getString("geometry")
                val decoded = GeoMath.decodePolyline(geometry)
                Result.success(
                    OsrmResult(
                        points = decoded,
                        distanceMeters = route.getDouble("distance"),
                        durationSec = route.getDouble("duration")
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
