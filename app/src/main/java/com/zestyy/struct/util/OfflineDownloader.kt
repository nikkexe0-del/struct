package com.zestyy.struct.util

import android.content.Context
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView

/**
 * Explicit, user-triggered offline map download for a single route's bounding box. This is the
 * *only* place tiles get pre-fetched for offline use — Follow Mode itself never auto-caches, so
 * offline navigation only works for routes the user has deliberately downloaded first (same idea
 * as downloading an area in Apple/Google Maps before going offline).
 */
object OfflineDownloader {
    fun download(
        context: Context,
        bounds: GeoMath.Bounds,
        onProgress: (downloaded: Int, total: Int) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // A MapView needs an Activity/View context to construct in some osmdroid versions, but
        // CacheManager only touches its tile provider — a detached instance is fine here since
        // it's never attached to a window or rendered.
        val throwaway = MapView(context).apply { setTileSource(TileSourceFactory.MAPNIK) }
        TileCacher.cacheRouteArea(
            mapView = throwaway,
            bounds = bounds,
            onProgress = onProgress,
            onComplete = {
                throwaway.onDetach()
                onComplete()
            },
            onError = {
                throwaway.onDetach()
                onError(it)
            }
        )
    }
}
