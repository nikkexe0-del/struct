package com.zestyy.struct.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditRoad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zestyy.struct.ui.components.GlassCard
import com.zestyy.struct.ui.components.ThemePickerDialog
import com.zestyy.struct.ui.theme.ByteOrange
import com.zestyy.struct.ui.theme.TextSecondary
import com.zestyy.struct.ui.theme.TextTertiary

@Composable
fun HomeScreen(
    onStartTracking: () -> Unit,
    onBuildRoute: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    var showThemePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(
                        "maarga",
                        style = MaterialTheme.typography.displayLarge,
                        color = ByteOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "GPS tracking · route builder · offline nav",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = { showThemePicker = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Appearance settings")
                }
            }
            Spacer(Modifier.height(40.dp))

            HomeActionCard(Icons.Default.PlayArrow, "Start recording", "Track a run, ride, or hike right now", onStartTracking)
            Spacer(Modifier.height(16.dp))
            HomeActionCard(Icons.Default.EditRoad, "Build a route", "Tap the map to plan a route in advance", onBuildRoute)
            Spacer(Modifier.height(16.dp))
            HomeActionCard(Icons.Default.History, "History", "Weekly stats, streaks, and your saved routes", onOpenLibrary)
        }

        AppFooter()
    }

    if (showThemePicker) {
        ThemePickerDialog(onDismiss = { showThemePicker = false })
    }
}

@Composable
private fun HomeActionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(ByteOrange.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ByteOrange)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun AppFooter() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "maarga · built by Nikshep",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
        Text(
            "Maps © OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}
