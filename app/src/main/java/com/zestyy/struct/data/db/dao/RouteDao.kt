package com.zestyy.struct.data.db.dao

import androidx.room.*
import com.zestyy.struct.data.db.entities.RoutePointEntity
import com.zestyy.struct.data.db.entities.SavedRouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Query("UPDATE saved_routes SET offlineDownloadedAtMillis = :timestamp WHERE id = :routeId")
    suspend fun markOfflineDownloaded(routeId: Long, timestamp: Long)

    @Insert
    suspend fun insertRoute(route: SavedRouteEntity): Long

    @Update
    suspend fun updateRoute(route: SavedRouteEntity)

    @Query("DELETE FROM saved_routes WHERE id = :routeId")
    suspend fun deleteRoute(routeId: Long)

    @Query("DELETE FROM route_points WHERE routeId = :routeId")
    suspend fun deletePointsForRoute(routeId: Long)

    @Query("SELECT * FROM saved_routes ORDER BY createdAtMillis DESC")
    fun observeAllRoutes(): Flow<List<SavedRouteEntity>>

    @Query("SELECT * FROM saved_routes WHERE id = :routeId")
    suspend fun getRoute(routeId: Long): SavedRouteEntity?

    @Insert
    suspend fun insertPoints(points: List<RoutePointEntity>)

    @Query("SELECT * FROM route_points WHERE routeId = :routeId ORDER BY seq ASC")
    suspend fun getPointsForRoute(routeId: Long): List<RoutePointEntity>

    @Query("SELECT * FROM route_points WHERE routeId = :routeId AND isLapMarker = 1 ORDER BY seq ASC")
    suspend fun getLapMarkers(routeId: Long): List<RoutePointEntity>

    @Query("SELECT COUNT(*) FROM route_points WHERE routeId = :routeId")
    suspend fun pointCount(routeId: Long): Int
}
