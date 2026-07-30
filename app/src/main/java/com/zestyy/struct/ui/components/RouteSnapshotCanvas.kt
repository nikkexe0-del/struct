package com.zestyy.struct.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.zestyy.struct.ui.theme.LocalGlassPalette
import com.zestyy.struct.util.GeoMath

/**
 * A fast, offline-friendly "route snapshot" — normalizes the route's lat/lng into the card's
 * own coordinate space and draws it as a simple line, the way Strava's feed thumbnails read as
 * a route silhouette without needing to fetch/render actual map tiles for every history card.
 */
@Composable
fun RouteSnapshotCanvas(points: List<GeoMath.Point>, modifier: Modifier = Modifier, strokeColor: Color = LocalGlassPalette.current.accent) {
    Canvas(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (points.size < 2) return@Canvas
        val bounds = GeoMath.bounds(points)
        val latSpan = (bounds.north - bounds.south).takeIf { it > 0.00001 } ?: 0.001
        val lngSpan = (bounds.east - bounds.west).takeIf { it > 0.00001 } ?: 0.001

        val paddingPx = size.minDimension * 0.12f
        val drawableW = size.width - paddingPx * 2
        val drawableH = size.height - paddingPx * 2

        // preserve aspect ratio so the route doesn't look stretched
        val scale = minOf(drawableW / lngSpan.toFloat(), drawableH / latSpan.toFloat())
        val offsetX = paddingPx + (drawableW - lngSpan.toFloat() * scale) / 2f
        val offsetY = paddingPx + (drawableH - latSpan.toFloat() * scale) / 2f

        fun toOffset(p: GeoMath.Point): Offset {
            val x = offsetX + ((p.lng - bounds.west).toFloat() * scale)
            val y = offsetY + ((bounds.north - p.lat).toFloat() * scale) // lat grows north = up
            return Offset(x, y)
        }

        val path = androidx.compose.ui.graphics.Path().apply {
            val first = toOffset(points.first())
            moveTo(first.x, first.y)
            for (i in 1 until points.size) {
                val o = toOffset(points[i])
                lineTo(o.x, o.y)
            }
        }
        drawPath(path, color = strokeColor, style = Stroke(width = 5f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))

        // start/end dots
        drawCircle(color = Color(0xFF32D74B), radius = 6f, center = toOffset(points.first()))
        drawCircle(color = strokeColor, radius = 6f, center = toOffset(points.last()))
    }
}
