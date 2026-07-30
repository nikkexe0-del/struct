package com.zestyy.struct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zestyy.struct.data.db.entities.RouteMode
import com.zestyy.struct.data.db.entities.RouteType
import com.zestyy.struct.data.repository.RouteRepository
import com.zestyy.struct.location.LocationTrackingService
import com.zestyy.struct.location.TrackingManager
import com.zestyy.struct.location.TrackingSnapshot
import com.zestyy.struct.location.TrackingState
import com.zestyy.struct.ui.components.*
import com.zestyy.struct.ui.theme.*
import com.zestyy.struct.util.Formatters
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun TrackingScreen(onFinished: (Long) -> Unit, onCancel: () -> Unit) {
    LocationPermissionGate {
        TrackingScreenContent(onFinished, onCancel)
    }
}

@Composable
private fun TrackingScreenContent(onFinished: (Long) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { RouteRepository(context) }
    val snapshot by TrackingManager.state.collectAsState()
    var mapViewRef by remember { mutableStateOf<org.osmdroid.views.MapView?>(null) }
    var livePolyline by remember { mutableStateOf<Polyline?>(null) }
    var youMarker by remember { mutableStateOf<Marker?>(null) }
    var hasZoomedToStart by remember { mutableStateOf(false) }

    // IMPORTANT: stopping the service resets TrackingManager's shared state to empty
    // immediately (other screens rely on that reset). Capture the final snapshot the moment
    // Stop is tapped so the save dialog isn't reading an already-emptied state — that gap was
    // throwing on an empty point list and taking the whole app down with it.
    var pendingSaveSnapshot by remember { mutableStateOf<TrackingSnapshot?>(null) }

    // redraw the live route + a rotating "you are here" sprite as new points arrive
    LaunchedEffect(snapshot.points.size) {
        val mv = mapViewRef ?: return@LaunchedEffect
        livePolyline?.let { mv.overlays.remove(it) }
        if (snapshot.points.isNotEmpty()) {
            livePolyline = mv.drawRoutePolyline(snapshot.points, ByteOrange.toArgb())

            val last = snapshot.points.last()
            val lastGeo = GeoPoint(last.lat, last.lng)

            val marker = youMarker ?: Marker(mv).also {
                it.icon = directionSpriteDrawable(context)
                it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                it.title = "You"
                mv.overlays.add(it)
                youMarker = it
            }
            marker.position = lastGeo
            marker.rotation = snapshot.bearingDegrees.toFloat()

            if (!hasZoomedToStart) {
                // zoom in close on the very first fix so the user immediately sees themselves,
                // instead of the map sitting at whatever the default/previous zoom was
                mv.controller.setZoom(19.0)
                mv.controller.setCenter(lastGeo)
                hasZoomedToStart = true
            } else {
                mv.controller.animateTo(lastGeo)
            }
            mv.invalidate()
        }
    }

    fun stopAndPromptSave() {
        pendingSaveSnapshot = snapshot // capture BEFORE stopping resets shared state
        LocationTrackingService.stop(context)
    }

    Box(Modifier.fillMaxSize()) {
        OsmMapView(modifier = Modifier.fillMaxSize()) { mv -> mapViewRef = mv }

        // Top stat glass bar — solid enough to stay legible over bright map tiles regardless of
        // what's underneath (was reading as fully transparent before)
        GlassBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            tint = LocalGlassPalette.current.surfaceOpaque
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn("DISTANCE", Formatters.km(snapshot.distanceMeters))
                StatColumn("TIME", Formatters.duration(snapshot.elapsedMillis))
                StatColumn("SPEED", Formatters.speedKmh(snapshot.currentSpeedMps))
                StatColumn("ELEV+", Formatters.elevation(snapshot.elevationGain))
            }
        }

        if (snapshot.isAutoPaused) {
            GlassCard(
                modifier = Modifier.align(Alignment.Center),
                tint = WarnAmber.copy(alpha = 0.85f)
            ) {
                Text(
                    "Auto-paused — waiting for motion",
                    modifier = Modifier.padding(16.dp),
                    color = TextPrimary
                )
            }
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (snapshot.state) {
                TrackingState.IDLE -> {
                    GlassButton(onClick = {
                        LocationTrackingService.start(context)
                    }, tint = LocalGlassPalette.current.surfaceOpaque) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, null, tint = TextPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Start", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                TrackingState.RECORDING -> {
                    GlassButton(onClick = { LocationTrackingService.lap(context) }, tint = LocalGlassPalette.current.surfaceOpaque) {
                        Icon(Icons.Default.Flag, "Lap", tint = TextPrimary)
                    }
                    GlassButton(onClick = { LocationTrackingService.pause(context) }, tint = WarnAmber.copy(alpha = 0.85f)) {
                        Icon(Icons.Default.Pause, "Pause", tint = TextPrimary)
                    }
                    GlassButton(onClick = { stopAndPromptSave() }, tint = DangerRed.copy(alpha = 0.85f)) {
                        Icon(Icons.Default.Stop, "Stop", tint = TextPrimary)
                    }
                }
                TrackingState.PAUSED -> {
                    GlassButton(onClick = { LocationTrackingService.resume(context) }, tint = SuccessGreen.copy(alpha = 0.85f)) {
                        Icon(Icons.Default.PlayArrow, "Resume", tint = TextPrimary)
                    }
                    GlassButton(onClick = { stopAndPromptSave() }, tint = DangerRed.copy(alpha = 0.85f)) {
                        Icon(Icons.Default.Stop, "Stop", tint = TextPrimary)
                    }
                }
            }
        }

        IconButton(onClick = onCancel, modifier = Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 8.dp)) {
            Icon(Icons.Default.Close, "Cancel", tint = TextPrimary)
        }
    }

    pendingSaveSnapshot?.let { finalSnapshot ->
        SaveActivityDialog(
            onSave = { name, mode ->
                if (finalSnapshot.points.isEmpty()) {
                    // nothing was ever recorded (e.g. stopped within the first GPS fix) —
                    // don't attempt to save, just leave quietly instead of crashing
                    pendingSaveSnapshot = null
                    onCancel()
                    return@SaveActivityDialog
                }
                scope.launch {
                    val id = repository.saveRoute(
                        name = name,
                        type = RouteType.RECORDED,
                        mode = mode,
                        points = finalSnapshot.points,
                        durationMillis = finalSnapshot.elapsedMillis,
                        lapIndices = finalSnapshot.lapMarkerIndices
                    )
                    pendingSaveSnapshot = null
                    onFinished(id)
                }
            },
            onDiscard = { pendingSaveSnapshot = null; onCancel() }
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SaveActivityDialog(onSave: (String, RouteMode) -> Unit, onDiscard: () -> Unit) {
    var name by remember { mutableStateOf("Untitled activity") }
    var mode by remember { mutableStateOf(RouteMode.RUN) }

    AlertDialog(
        onDismissRequest = onDiscard,
        title = { Text("Save activity") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(RouteMode.RUN, RouteMode.WALK, RouteMode.BIKE, RouteMode.HIKE).forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.name) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, mode) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDiscard) { Text("Discard", color = DangerRed) } }
    )
}
