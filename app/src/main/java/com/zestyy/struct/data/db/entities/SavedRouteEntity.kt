package com.zestyy.struct.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RouteType { RECORDED, BUILT }
enum class RouteMode { WALK, RUN, BIKE, HIKE, OTHER }

@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: RouteType,
    val mode: RouteMode = RouteMode.OTHER,
    val createdAtMillis: Long,
    val distanceMeters: Double,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
    val durationMillis: Long = 0, // only meaningful for RECORDED
    val avgPaceSecPerKm: Double? = null,
    /** bounding box, cached so the library screen can draw a thumbnail without loading all points */
    val boundsNorth: Double,
    val boundsSouth: Double,
    val boundsEast: Double,
    val boundsWest: Double,
    val notes: String = "",
    /** null = not downloaded for offline use; set when the user explicitly downloads tiles
     *  for this route via the History screen — offline Follow mode only relies on this,
     *  never on automatic background caching. */
    val offlineDownloadedAtMillis: Long? = null
)
