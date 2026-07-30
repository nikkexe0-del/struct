package com.zestyy.struct.util

import kotlin.math.*

object GeoMath {

    data class Point(
        val lat: Double,
        val lng: Double,
        val altitude: Double? = null,
        val timestampMillis: Long? = null,
        val speedMetersPerSec: Float? = null
    )

    data class Bounds(val north: Double, val south: Double, val east: Double, val west: Double)

    private const val EARTH_RADIUS_M = 6371000.0

    fun distanceMeters(a: Point, b: Point): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }

    fun totalDistanceMeters(points: List<Point>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) total += distanceMeters(points[i - 1], points[i])
        return total
    }

    /** Sum of positive/negative altitude deltas, lightly smoothed to reduce GPS altitude noise. */
    fun elevationGainLoss(points: List<Point>, smoothingWindow: Int = 3): Pair<Double, Double> {
        val altitudes = points.mapNotNull { it.altitude }
        if (altitudes.size < 2) return 0.0 to 0.0
        // simple moving average smoothing
        val smoothed = altitudes.indices.map { i ->
            val lo = max(0, i - smoothingWindow / 2)
            val hi = min(altitudes.size - 1, i + smoothingWindow / 2)
            altitudes.subList(lo, hi + 1).average()
        }
        var gain = 0.0
        var loss = 0.0
        for (i in 1 until smoothed.size) {
            val d = smoothed[i] - smoothed[i - 1]
            if (d > 0) gain += d else loss += -d
        }
        return gain to loss
    }

    fun bounds(points: List<Point>): Bounds {
        var n = -90.0; var s = 90.0; var e = -180.0; var w = 180.0
        for (p in points) {
            if (p.lat > n) n = p.lat
            if (p.lat < s) s = p.lat
            if (p.lng > e) e = p.lng
            if (p.lng < w) w = p.lng
        }
        return Bounds(n, s, e, w)
    }

    fun bearingDegrees(a: Point, b: Point): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /**
     * Shortest distance (meters) from [p] to the polyline [route], plus the index of the
     * nearest segment. Used for off-route detection and "distance completed along route".
     */
    fun distanceToPolyline(p: Point, route: List<Point>): Pair<Double, Int> {
        if (route.isEmpty()) return Double.MAX_VALUE to -1
        if (route.size == 1) return distanceMeters(p, route[0]) to 0

        var best = Double.MAX_VALUE
        var bestIdx = 0
        for (i in 0 until route.size - 1) {
            val d = distanceToSegmentMeters(p, route[i], route[i + 1])
            if (d < best) {
                best = d
                bestIdx = i
            }
        }
        return best to bestIdx
    }

    /** Approximate point-to-segment distance using an equirectangular projection (fine at city scale). */
    private fun distanceToSegmentMeters(p: Point, segA: Point, segB: Point): Double {
        val refLat = Math.toRadians(segA.lat)
        fun toXY(pt: Point): Pair<Double, Double> {
            val x = Math.toRadians(pt.lng - segA.lng) * cos(refLat) * EARTH_RADIUS_M
            val y = Math.toRadians(pt.lat - segA.lat) * EARTH_RADIUS_M
            return x to y
        }
        val (ax, ay) = 0.0 to 0.0
        val (bx, by) = toXY(segB)
        val (px, py) = toXY(p)

        val abx = bx - ax; val aby = by - ay
        val apx = px - ax; val apy = py - ay
        val lenSq = abx * abx + aby * aby
        val t = if (lenSq > 0) ((apx * abx + apy * aby) / lenSq).coerceIn(0.0, 1.0) else 0.0
        val cx = ax + t * abx
        val cy = ay + t * aby
        val dx = px - cx; val dy = py - cy
        return sqrt(dx * dx + dy * dy)
    }

    /** Cumulative distance (meters) walked along the route up to and including [segmentIdx], plus
     *  the extra distance from that segment's start to the projected point — used for progress %. */
    fun distanceCompletedAlongRoute(route: List<Point>, segmentIdx: Int, projectedExtra: Double): Double {
        if (segmentIdx < 0) return 0.0
        var d = 0.0
        for (i in 0 until segmentIdx) d += distanceMeters(route[i], route[i + 1])
        return d + projectedExtra
    }

    /** Decodes an OSRM/Google-style encoded polyline (precision 5) into a list of points. */
    fun decodePolyline(encoded: String, precision: Int = 5): List<Point> {
        val factor = 10.0.pow(precision)
        val points = mutableListOf<Point>()
        var index = 0
        var lat = 0L
        var lng = 0L
        while (index < encoded.length) {
            var result = 0L
            var shift = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f).toLong() shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if ((result and 1L) != 0L) (result shr 1).inv() else result shr 1
            lat += dLat

            result = 0
            shift = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f).toLong() shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if ((result and 1L) != 0L) (result shr 1).inv() else result shr 1
            lng += dLng

            points.add(Point(lat / factor, lng / factor))
        }
        return points
    }
}
