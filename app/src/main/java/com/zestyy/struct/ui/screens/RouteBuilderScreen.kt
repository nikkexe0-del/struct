package com.zestyy.struct.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zestyy.struct.data.db.entities.RouteMode
import com.zestyy.struct.data.db.entities.RouteType
import com.zestyy.struct.data.repository.RouteRepository
import com.zestyy.struct.routing.OsrmClient
import com.zestyy.struct.routing.OsrmProfile
import com.zestyy.struct.ui.components.*
import com.zestyy.struct.ui.theme.*
import com.zestyy.struct.util.Formatters
import com.zestyy.struct.util.GeoMath
import kotlinx.coroutines.launch
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private enum class BuilderMode { FREEHAND, ROAD_SNAPPED }

@Composable
fun RouteBuilderScreen(onSaved: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { RouteRepository(context) }
    val osrm = remember { OsrmClient() }

    var waypoints by remember { mutableStateOf<List<GeoMath.Point>>(emptyList()) }
    var routedPoints by remember { mutableStateOf<List<GeoMath.Point>>(emptyList()) } // snapped/expanded path for display+distance
    var mode by remember { mutableStateOf(BuilderMode.FREEHAND) }
    var mapViewRef by remember { mutableStateOf<org.osmdroid.views.MapView?>(null) }
    var isRouting by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val displayPoints = if (mode == BuilderMode.ROAD_SNAPPED && routedPoints.isNotEmpty()) routedPoints else waypoints
    val distance = GeoMath.totalDistanceMeters(displayPoints)

    fun refreshMapOverlays() {
        val mv = mapViewRef ?: return
        mv.overlays.clear()
        if (displayPoints.size >= 2) {
            mv.overlays.add(Polyline(mv).apply {
                setPoints(displayPoints.map { GeoPoint(it.lat, it.lng) })
                outlinePaint.color = ByteOrange.toArgb()
                outlinePaint.strokeWidth = 10f
            })
        }
        waypoints.forEachIndexed { i, wp ->
            mv.overlays.add(Marker(mv).apply {
                position = GeoPoint(wp.lat, wp.lng)
                title = "Waypoint ${i + 1}"
                setOnMarkerClickListener { marker, _ ->
                    // long-press-to-delete is handled via a dedicated gesture below; single tap just shows info
                    marker.showInfoWindow()
                    true
                }
            })
        }
        mv.invalidate()
    }

    fun requestRoadSnap() {
        if (waypoints.size < 2) { routedPoints = emptyList(); return }
        isRouting = true
        scope.launch {
            val result = osrm.route(waypoints, OsrmProfile.FOOT)
            isRouting = false
            result.onSuccess { routedPoints = it.points }
                .onFailure { errorMsg = "Road-snap routing failed (offline or rate-limited) — showing freehand instead" }
            refreshMapOverlays()
        }
    }

    LaunchedEffect(waypoints, mode) {
        if (mode == BuilderMode.ROAD_SNAPPED) requestRoadSnap() else refreshMapOverlays()
    }

    Box(Modifier.fillMaxSize()) {
        OsmMapView(modifier = Modifier.fillMaxSize()) { mv ->
            mapViewRef = mv
        }

        // tap-to-add-waypoint layer
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(mapViewRef) {
                    detectTapGestures { offset ->
                        val mv = mapViewRef ?: return@detectTapGestures
                        val geo = mv.projection.fromPixels(offset.x.toInt(), offset.y.toInt()) as GeoPoint
                        waypoints = waypoints + GeoMath.Point(geo.latitude, geo.longitude)
                    }
                }
        )

        GlassBar(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
            tint = LocalGlassPalette.current.surfaceOpaque
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Route builder", style = MaterialTheme.typography.titleMedium)
                    Text(Formatters.km(distance), style = MaterialTheme.typography.titleMedium, color = ByteOrange)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == BuilderMode.FREEHAND,
                        onClick = { mode = BuilderMode.FREEHAND },
                        label = { Text("Freehand") }
                    )
                    FilterChip(
                        selected = mode == BuilderMode.ROAD_SNAPPED,
                        onClick = { mode = BuilderMode.ROAD_SNAPPED },
                        label = { Text(if (isRouting) "Routing…" else "Road-snapped") }
                    )
                }
                errorMsg?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = WarnAmber, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GlassButton(onClick = {
                waypoints = waypoints.dropLast(1)
            }) { Icon(Icons.Default.Undo, "Undo last point", tint = TextPrimary) }

            GlassButton(onClick = {
                waypoints = waypoints.reversed()
            }) { Icon(Icons.Default.SwapVert, "Reverse", tint = TextPrimary) }

            GlassButton(onClick = {
                if (waypoints.size >= 2) showSaveDialog = true
            }, tint = ByteOrange.copy(alpha = 0.5f)) {
                Icon(Icons.Default.Check, "Save route", tint = TextPrimary)
            }
        }

        IconButton(onClick = onCancel, modifier = Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 8.dp)) {
            Icon(Icons.Default.Close, "Cancel", tint = TextPrimary)
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("New route") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save route") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.saveRoute(name, RouteType.BUILT, RouteMode.OTHER, displayPoints)
                        showSaveDialog = false
                        onSaved()
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } }
        )
    }
}
