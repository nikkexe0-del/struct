package com.zestyy.struct.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single GPS fix belonging to a route (either a live-recorded activity or a built route).
 * Stored in-order via [seq]; timestamp is epoch millis.
 */
@Entity(tableName = "route_points")
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val seq: Int,
    val lat: Double,
    val lng: Double,
    val altitudeMeters: Double?,
    val timestampMillis: Long,
    val speedMetersPerSec: Float? = null,
    /** true if this point is a manually-dropped lap/split marker */
    val isLapMarker: Boolean = false
)
