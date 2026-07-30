package com.zestyy.struct.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.zestyy.struct.util.GeoMath
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Thin Compose wrapper around osmdroid's MapView. [onReady] hands back the raw MapView so
 * callers (Tracking/Builder/Follow screens) can add/update overlays imperatively — osmdroid
 * doesn't have a declarative Compose API, so this is the standard interop pattern for it.
 */
@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    onReady: (MapView) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)          // pinch-to-zoom
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER) // no on-screen +/- buttons
            isTilesScaledToDpi = true            // crisper, Strava-like tile rendering on high-density screens
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            setScrollableAreaLimitLatitude(MapView.getTileSystem().maxLatitude, MapView.getTileSystem().minLatitude, 0)
            minZoomLevel = 3.0
            maxZoomLevel = 20.0
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null) // smoother pan/zoom rendering
            controller.setZoom(16.0)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            onReady(mapView)
            mapView
        }
    )

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }
}

fun List<GeoMath.Point>.toGeoPoints(): List<GeoPoint> = map { GeoPoint(it.lat, it.lng, it.altitude ?: 0.0) }

fun MapView.drawRoutePolyline(points: List<GeoMath.Point>, colorArgb: Int, widthPx: Float = 10f): Polyline {
    val poly = Polyline(this).apply {
        setPoints(points.toGeoPoints())
        outlinePaint.color = colorArgb
        outlinePaint.strokeWidth = widthPx
        outlinePaint.isAntiAlias = true
    }
    overlays.add(poly)
    return poly
}

fun MapView.addMarker(point: GeoMath.Point, title: String): Marker {
    val marker = Marker(this).apply {
        position = GeoPoint(point.lat, point.lng)
        this.title = title
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    }
    overlays.add(marker)
    return marker
}

fun MapView.zoomToBounds(points: List<GeoMath.Point>, paddingPx: Int = 80) {
    if (points.isEmpty()) return
    val b = GeoMath.bounds(points)
    post {
        zoomToBoundingBox(
            org.osmdroid.util.BoundingBox(b.north, b.east, b.south, b.west),
            true,
            paddingPx
        )
    }
}
