package com.zestyy.struct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zestyy.struct.ui.theme.*

@Composable
fun ThemePickerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val current by ThemeManager.mode.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance") },
        text = {
            Column {
                ThemeOptionRow("Dark", "Default — black & orange", ByteBlack, ByteOrange, current == AppThemeMode.DARK) {
                    ThemeManager.setMode(context, AppThemeMode.DARK)
                }
                Spacer(Modifier.height(12.dp))
                ThemeOptionRow("Light", "White & orange", LightBackground, ByteOrange, current == AppThemeMode.LIGHT) {
                    ThemeManager.setMode(context, AppThemeMode.LIGHT)
                }
                Spacer(Modifier.height(12.dp))
                ThemeOptionRow("Green", "Dark & green", GreenBackground, GreenAccent, current == AppThemeMode.GREEN) {
                    ThemeManager.setMode(context, AppThemeMode.GREEN)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun ThemeOptionRow(name: String, subtitle: String, swatchBg: androidx.compose.ui.graphics.Color, swatchAccent: androidx.compose.ui.graphics.Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(swatchBg),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(swatchAccent))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        if (selected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = ByteOrange)
    }
}
