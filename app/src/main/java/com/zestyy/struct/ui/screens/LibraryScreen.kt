package com.zestyy.struct.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zestyy.struct.data.db.entities.SavedRouteEntity
import com.zestyy.struct.data.repository.RouteRepository
import com.zestyy.struct.gpx.GpxImporter
import com.zestyy.struct.ui.components.GlassCard
import com.zestyy.struct.ui.components.RouteSnapshotCanvas
import com.zestyy.struct.ui.theme.ByteOrange
import com.zestyy.struct.ui.theme.TextSecondary
import com.zestyy.struct.util.ActivityStats
import com.zestyy.struct.util.Formatters
import com.zestyy.struct.util.GeoMath
import com.zestyy.struct.util.OfflineDownloader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LibraryScreen(onOpenRoute: (Long) -> Unit, onFollowRoute: (Long) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { RouteRepository(context) }
    val routes by repository.observeRoutes().collectAsState(initial = emptyList())
    var routeToDelete by remember { mutableStateOf<SavedRouteEntity?>(null) }
    var routeToRename by remember { mutableStateOf<SavedRouteEntity?>(null) }

    val gpxPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val parsed = GpxImporter.import(context, uri)
                if (parsed.points.isNotEmpty()) {
                    repository.importGpx(parsed.name ?: "Imported route", parsed.points)
                }
            }
        }
    }

    val weekly = remember(routes) { ActivityStats.weeklySummary(routes) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { gpxPicker.launch("*/*") }) { Icon(Icons.Default.FileUpload, "Import GPX") }
        }

        if (routes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No activities yet — record one or build a route.", color = TextSecondary)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { WeeklySummaryCard(weekly) }
                items(routes, key = { it.id }) { route ->
                    var points by remember(route.id) { mutableStateOf<List<GeoMath.Point>>(emptyList()) }
                    LaunchedEffect(route.id) {
                        points = repository.getPoints(route.id).map {
                            GeoMath.Point(it.lat, it.lng, it.altitudeMeters, it.timestampMillis)
                        }
                    }
                    RouteFeedCard(
                        route = route,
                        points = points,
                        onOpen = { onOpenRoute(route.id) },
                        onTravelAgain = { onFollowRoute(route.id) },
                        onRename = { routeToRename = route },
                        onDelete = { routeToDelete = route },
                        onDownloadOffline = {
                            scope.launch {
                                val bounds = GeoMath.Bounds(route.boundsNorth, route.boundsSouth, route.boundsEast, route.boundsWest)
                                OfflineDownloader.download(
                                    context = context,
                                    bounds = bounds,
                                    onProgress = { _, _ -> },
                                    onComplete = { scope.launch { repository.markOfflineDownloaded(route.id) } },
                                    onError = { }
                                )
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    routeToDelete?.let { route ->
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            title = { Text("Delete \"${route.name}\"?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deleteRoute(route.id) }
                    routeToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { routeToDelete = null }) { Text("Cancel") } }
        )
    }

    routeToRename?.let { route ->
        var name by remember(route.id) { mutableStateOf(route.name) }
        AlertDialog(
            onDismissRequest = { routeToRename = null },
            title = { Text("Rename route") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.renameRoute(route, name) }
                    routeToRename = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { routeToRename = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun WeeklySummaryCard(weekly: com.zestyy.struct.util.WeeklySummary) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("This week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WeeklyStat("ACTIVITIES", weekly.activityCount.toString())
                WeeklyStat("DISTANCE", Formatters.km(weekly.totalDistanceMeters))
                WeeklyStat("TIME", Formatters.duration(weekly.totalDurationMillis))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STREAK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = ByteOrange, modifier = Modifier.size(18.dp))
                        Text(" ${weekly.currentStreakDays}d", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RouteFeedCard(
    route: SavedRouteEntity,
    points: List<GeoMath.Point>,
    onOpen: () -> Unit,
    onTravelAgain: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDownloadOffline: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    var menuOpen by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column {
            if (points.size >= 2) {
                RouteSnapshotCanvas(
                    points = points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(route.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${Formatters.km(route.distanceMeters)} · ${Formatters.elevation(route.elevationGainMeters)} elev · ${dateFmt.format(Date(route.createdAtMillis))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (route.offlineDownloadedAtMillis != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DownloadDone, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Text(" Available offline", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
                IconButton(onClick = onTravelAgain) { Icon(Icons.Default.Navigation, "Travel again", tint = ByteOrange) }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Travel again") }, onClick = { menuOpen = false; onTravelAgain() })
                        DropdownMenuItem(text = { Text("Download for offline") }, onClick = { menuOpen = false; onDownloadOffline() })
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; onRename() })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
                    }
                }
            }
        }
    }
}
