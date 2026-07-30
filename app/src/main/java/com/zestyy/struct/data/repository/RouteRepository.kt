package com.zestyy.struct.data.repository

import android.content.Context
import com.zestyy.struct.data.db.AppDatabase
import com.zestyy.struct.data.db.entities.RouteMode
import com.zestyy.struct.data.db.entities.RoutePointEntity
import com.zestyy.struct.data.db.entities.RouteType
import com.zestyy.struct.data.db.entities.SavedRouteEntity
import com.zestyy.struct.util.GeoMath
import kotlinx.coroutines.flow.Flow

class RouteRepository(context: Context) {
    private val dao = AppDatabase.get(context).routeDao()

    fun observeRoutes(): Flow<List<SavedRouteEntity>> = dao.observeAllRoutes()

    suspend fun getRoute(id: Long) = dao.getRoute(id)

    suspend fun getPoints(routeId: Long) = dao.getPointsForRoute(routeId)

    suspend fun getLapMarkers(routeId: Long) = dao.getLapMarkers(routeId)

    suspend fun deleteRoute(id: Long) {
        dao.deletePointsForRoute(id)
        dao.deleteRoute(id)
    }

    suspend fun renameRoute(route: SavedRouteEntity, newName: String) {
        dao.updateRoute(route.copy(name = newName))
    }

    suspend fun markOfflineDownloaded(routeId: Long) {
        dao.markOfflineDownloaded(routeId, System.currentTimeMillis())
    }

    /**
     * Persists a finished recording or a hand-built route: computes distance/elevation/bounds
     * from the point list, inserts the route row, then bulk-inserts the points.
     */
    suspend fun saveRoute(
        name: String,
        type: RouteType,
        mode: RouteMode,
        points: List<GeoMath.Point>,
        durationMillis: Long = 0,
        lapIndices: Set<Int> = emptySet()
    ): Long {
        require(points.isNotEmpty()) { "Cannot save an empty route" }
        val distance = GeoMath.totalDistanceMeters(points)
        val (gain, loss) = GeoMath.elevationGainLoss(points)
        val bounds = GeoMath.bounds(points)
        val avgPace = if (type == RouteType.RECORDED && distance > 0 && durationMillis > 0) {
            (durationMillis / 1000.0) / (distance / 1000.0)
        } else null

        val routeId = dao.insertRoute(
            SavedRouteEntity(
                name = name,
                type = type,
                mode = mode,
                createdAtMillis = System.currentTimeMillis(),
                distanceMeters = distance,
                elevationGainMeters = gain,
                elevationLossMeters = loss,
                durationMillis = durationMillis,
                avgPaceSecPerKm = avgPace,
                boundsNorth = bounds.north,
                boundsSouth = bounds.south,
                boundsEast = bounds.east,
                boundsWest = bounds.west
            )
        )

        val entities = points.mapIndexed { idx, p ->
            RoutePointEntity(
                routeId = routeId,
                seq = idx,
                lat = p.lat,
                lng = p.lng,
                altitudeMeters = p.altitude,
                timestampMillis = p.timestampMillis ?: System.currentTimeMillis(),
                speedMetersPerSec = p.speedMetersPerSec,
                isLapMarker = idx in lapIndices
            )
        }
        dao.insertPoints(entities)
        return routeId
    }

    /** Import a parsed GPX track straight into the library. */
    suspend fun importGpx(name: String, points: List<GeoMath.Point>, mode: RouteMode = RouteMode.OTHER): Long {
        val durationMs = if (points.size >= 2) {
            val first = points.first().timestampMillis
            val last = points.last().timestampMillis
            if (first != null && last != null) last - first else 0L
        } else 0L
        return saveRoute(name, RouteType.RECORDED, mode, points, durationMs)
    }
}
