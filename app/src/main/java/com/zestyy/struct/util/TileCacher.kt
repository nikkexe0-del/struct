package com.zestyy.struct.util

import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.cachemanager.CacheManager

/**
 * Downloads OSM tiles covering a route's bounding box (+ a small margin) across a zoom range,
 * so Follow Mode works with no data connection once caching completes. Call this when a route
 * is selected for follow mode, while still online.
 */
object TileCacher {

    fun cacheRouteArea(
        mapView: MapView,
        bounds: GeoMath.Bounds,
        minZoom: Int = 13,
        maxZoom: Int = 18,
        onProgress: (downloaded: Int, total: Int) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val marginDeg = 0.01 // ~1km margin
            val bb = BoundingBox(
                bounds.north + marginDeg,
                bounds.east + marginDeg,
                bounds.south - marginDeg,
                bounds.west - marginDeg
            )
            val cacheManager = CacheManager(mapView)
            val total = cacheManager.possibleTilesInArea(bb, minZoom, maxZoom)

            cacheManager.downloadAreaAsync(
                mapView.context, bb, minZoom, maxZoom,
                object : CacheManager.CacheManagerCallback {
                    override fun onTaskComplete() = onComplete()
                    override fun onTaskFailed(errors: Int) = onError(RuntimeException("$errors tiles failed to download"))
                    override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
                        onProgress(progress, total)
                    }
                    override fun downloadStarted() {}
                    override fun setPossibleTilesInArea(total: Int) {}
                }
            )
        } catch (e: Exception) {
            onError(e)
        }
    }
}
