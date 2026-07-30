package com.zestyy.struct.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.zestyy.struct.data.db.entities.SavedRouteEntity
import com.zestyy.struct.data.repository.RouteRepository
import com.zestyy.struct.gpx.GpxExporter
import com.zestyy.struct.ui.components.*
import com.zestyy.struct.ui.theme.*
import com.zestyy.struct.util.Formatters
import com.zestyy.struct.util.GeoMath

@Composable
fun SummaryScreen(routeId: Long, onBack: () -> Unit, onFollow: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { RouteRepository(context) }
    var route by remember { mutableStateOf<SavedRouteEntity?>(null) }
    var points by remember { mutableStateOf<List<GeoMath.Point>>(emptyList()) }
    var lapSeqs by remember { mutableStateOf<List<Int>>(emptyList()) }
    var mapViewRef by remember { mutableStateOf<org.osmdroid.views.MapView?>(null) }

    LaunchedEffect(routeId) {
        val r = repository.getRoute(routeId) ?: return@LaunchedEffect
        val pts = repository.getPoints(routeId)
        route = r
        points = pts.map { GeoMath.Point(it.lat, it.lng, it.altitudeMeters, it.timestampMillis, it.speedMetersPerSec) }
        lapSeqs = repository.getLapMarkers(routeId).map { it.seq }
    }

    LaunchedEffect(points, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect
        if (points.isNotEmpty()) {
            mv.drawRoutePolyline(points, ByteOrange.toArgb())
            mv.zoomToBounds(points)
            mv.invalidate()
        }
    }

    val r = route
    val scope = rememberCoroutineScope()

    fun shareGpx() {
        val currentRoute = r ?: return
        val uri = GpxExporter.export(context, currentRoute, points)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share GPX"))
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text(r?.name ?: "Loading…", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row {
                IconButton(onClick = { shareGpx() }) { Icon(Icons.Default.Share, "Export GPX") }
                IconButton(onClick = onFollow) { Icon(Icons.Default.Navigation, "Follow this route") }
            }
        }

        OsmMapView(modifier = Modifier.fillMaxWidth().height(260.dp)) { mv -> mapViewRef = mv }

        if (r != null) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBlock("Distance", Formatters.km(r.distanceMeters))
                StatBlock("Duration", Formatters.duration(r.durationMillis))
                StatBlock("Avg pace", Formatters.pace(r.avgPaceSecPerKm))
                StatBlock("Elev+", Formatters.elevation(r.elevationGainMeters))
            }

            if (points.any { it.altitude != null }) {
                Text("Elevation profile", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                val entries = remember(points) {
                    points.filter { it.altitude != null }
                        .mapIndexed { i, p -> com.patrykandpatrick.vico.core.entry.entryOf(i.toFloat(), p.altitude!!.toFloat()) }
                }
                if (entries.isNotEmpty()) {
                    Chart(
                        chart = lineChart(),
                        model = entryModelOf(entries),
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier.fillMaxWidth().height(160.dp).padding(16.dp)
                    )
                }
            }

            if (lapSeqs.isNotEmpty()) {
                Text("Splits", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    items(lapSeqs.size) { idx ->
                        val seqStart = if (idx == 0) 0 else lapSeqs[idx - 1]
                        val seqEnd = lapSeqs[idx]
                        val segment = points.subList(seqStart.coerceIn(0, points.size), (seqEnd + 1).coerceIn(0, points.size))
                        val dist = GeoMath.totalDistanceMeters(segment)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lap ${idx + 1}", color = TextSecondary)
                            Text(Formatters.meters(dist), color = TextPrimary)
                        }
                        Divider(color = GlassBorder)
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                Button(onClick = onFollow, colors = ButtonDefaults.buttonColors(containerColor = ByteOrange)) {
                    Icon(Icons.Default.Navigation, null); Spacer(Modifier.width(8.dp)); Text("Follow route")
                }
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
