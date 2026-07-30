package com.zestyy.struct.ui.screens

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.zestyy.struct.data.repository.RouteRepository
import com.zestyy.struct.ui.components.*
import com.zestyy.struct.ui.theme.*
import com.zestyy.struct.util.Formatters
import com.zestyy.struct.util.GeoMath
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

private const val OFF_ROUTE_THRESHOLD_M = 35.0

@Composable
fun FollowRouteScreen(routeId: Long, onExit: () -> Unit) {
    LocationPermissionGate {
        FollowRouteContent(routeId, onExit)
    }
}

@Composable
private fun FollowRouteContent(routeId: Long, onExit: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { RouteRepository(context) }

    var routeName by remember { mutableStateOf("") }
    var routePoints by remember { mutableStateOf<List<GeoMath.Point>>(emptyList()) }
    var totalDistance by remember { mutableStateOf(0.0) }
    var mapViewRef by remember { mutableStateOf<org.osmdroid.views.MapView?>(null) }
    var userMarker by remember { mutableStateOf<Marker?>(null) }

    var currentPosition by remember { mutableStateOf<GeoMath.Point?>(null) }
    var distanceToRoute by remember { mutableStateOf(0.0) }
    var distanceCompleted by remember { mutableStateOf(0.0) }
    var bearingToNext by remember { mutableStateOf<Double?>(null) }
    var avgPaceSecPerKm by remember { mutableStateOf<Double?>(null) }
    var startedAtMs by remember { mutableStateOf(0L) }

    LaunchedEffect(routeId) {
        val route = repository.getRoute(routeId) ?: return@LaunchedEffect
        val pts = repository.getPoints(routeId)
        routeName = route.name
        routePoints = pts.map { GeoMath.Point(it.lat, it.lng, it.altitudeMeters, it.timestampMillis) }
        totalDistance = route.distanceMeters
        startedAtMs = System.currentTimeMillis()
    }

    LaunchedEffect(routePoints, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect
        if (routePoints.isNotEmpty()) {
            mv.drawRoutePolyline(routePoints, TextSecondary.toArgb(), widthPx = 8f)
            mv.zoomToBounds(routePoints)
            mv.invalidate()
        }
    }

    // plain LocationManager live position listener, scoped to this screen's lifecycle
    DisposableEffect(Unit) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val p = GeoMath.Point(location.latitude, location.longitude, if (location.hasAltitude()) location.altitude else null, location.time)
                currentPosition = p

                if (routePoints.isNotEmpty()) {
                    val (dist, segIdx) = GeoMath.distanceToPolyline(p, routePoints)
                    distanceToRoute = dist
                    distanceCompleted = GeoMath.distanceCompletedAlongRoute(routePoints, segIdx, 0.0)

                    val nextIdx = (segIdx + 1).coerceAtMost(routePoints.size - 1)
                    if (nextIdx < routePoints.size) {
                        bearingToNext = GeoMath.bearingDegrees(p, routePoints[nextIdx])
                    }

                    val elapsedSec = (System.currentTimeMillis() - startedAtMs) / 1000.0
                    if (distanceCompleted > 10 && elapsedSec > 5) {
                        avgPaceSecPerKm = elapsedSec / (distanceCompleted / 1000.0)
                    }
                }

                val mv = mapViewRef
                if (mv != null) {
                    val marker = userMarker ?: Marker(mv).also {
                        it.icon = com.zestyy.struct.ui.components.directionSpriteDrawable(context)
                        it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        it.title = "You"
                        mv.overlays.add(it)
                        userMarker = it
                    }
                    marker.position = GeoPoint(p.lat, p.lng)
                    bearingToNext?.let { marker.rotation = it.toFloat() }
                    mv.controller.animateTo(marker.position)
                    mv.invalidate()
                }
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            val provider = when {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            provider?.let { lm.requestLocationUpdates(it, 2000L, 3f, listener) }
        }
        onDispose { lm.removeUpdates(listener) }
    }

    val isOffRoute = distanceToRoute > OFF_ROUTE_THRESHOLD_M
    val remaining = (totalDistance - distanceCompleted).coerceAtLeast(0.0)
    val etaMin = avgPaceSecPerKm?.let { (remaining / 1000.0) * it / 60.0 }

    Box(Modifier.fillMaxSize()) {
        OsmMapView(modifier = Modifier.fillMaxSize()) { mv -> mapViewRef = mv }

        GlassBar(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
            tint = LocalGlassPalette.current.surfaceOpaque
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(routeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    NavStat("REMAINING", Formatters.meters(remaining))
                    NavStat("COMPLETED", Formatters.meters(distanceCompleted))
                    NavStat("ETA", etaMin?.let { "${it.toInt()} min" } ?: "--")
                    bearingToNext?.let { b ->
                        Icon(
                            Icons.Default.NearMe,
                            contentDescription = "Bearing to next waypoint",
                            tint = ByteOrange,
                            modifier = Modifier.rotate(b.toFloat())
                        )
                    }
                }
            }
        }

        if (isOffRoute) {
            GlassCard(
                modifier = Modifier.align(Alignment.Center),
                tint = DangerRed.copy(alpha = 0.3f)
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Off route", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("${distanceToRoute.toInt()} m from path", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        IconButton(onClick = onExit, modifier = Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 8.dp)) {
            Icon(Icons.Default.Close, "Exit follow mode", tint = TextPrimary)
        }
    }
}

@Composable
private fun NavStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
