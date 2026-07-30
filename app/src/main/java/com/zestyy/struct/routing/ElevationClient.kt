package com.zestyy.struct.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Free elevation lookup for points that don't have GPS altitude yet — used only while building
 * a route (before you've actually walked it and gotten real GPS altitude readings).
 * Uses the public Open-Topo-Data API (no key). Batches up to 100 points per request per their limits.
 * If the request fails (offline / rate-limited), the builder just skips the elevation preview —
 * nothing else in the app depends on this.
 */
class ElevationClient(
    private val baseUrl: String = "https://api.opentopodata.org/v1/srtm90m"
) {
    suspend fun lookup(points: List<Pair<Double, Double>>): Result<List<Double?>> =
        withContext(Dispatchers.IO) {
            if (points.isEmpty()) return@withContext Result.success(emptyList())
            try {
                val results = mutableListOf<Double?>()
                for (chunk in points.chunked(100)) {
                    val locations = chunk.joinToString("|") { "${it.first},${it.second}" }
                    val url = URL("$baseUrl?locations=$locations")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 8_000
                        readTimeout = 12_000
                    }
                    if (conn.responseCode != 200) {
                        results.addAll(chunk.map { null })
                        continue
                    }
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val arr = json.getJSONArray("results")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        results.add(if (obj.isNull("elevation")) null else obj.getDouble("elevation"))
                    }
                }
                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
