package dev.camera3.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.camera3.app.BuildConfig
import dev.camera3.app.CameraUiState
import dev.camera3.app.CameraViewModel

@Composable
fun SettingsScreen(ui: CameraUiState, model: CameraViewModel, back: () -> Unit,
                   diagnostics: () -> Unit, logs: () -> Unit, permissions: () -> Unit) {
    var quality by remember(ui.preferences.jpegQuality) { mutableFloatStateOf(ui.preferences.jpegQuality.toFloat()) }
    PageHeader("Settings", back)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("CAPTURE", color = Muted, style = MaterialTheme.typography.labelMedium) }
        item {
            Surface(color = Panel, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("JPEG quality · ${quality.toInt()}%", style = MaterialTheme.typography.bodyLarge)
                    Text("Applied to the next real JPEG capture", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Slider(value = quality, onValueChange = { quality = it }, valueRange = 50f..100f, steps = 9,
                        onValueChangeFinished = { model.setQuality(quality.toInt()) })
                    Text("Storage: DCIM/Camera3 · unique timestamped names", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
        item { Text("INTERFACE", color = Muted, style = MaterialTheme.typography.labelMedium) }
        item {
            Surface(color = Panel, shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GridOn, null); Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) { Text("Rule-of-thirds grid"); Text("Viewfinder overlay", color = Muted, style = MaterialTheme.typography.bodySmall) }
                    Switch(checked = ui.preferences.grid, onCheckedChange = model::setGrid)
                }
            }
        }
        item { Text("SYSTEM & DIAGNOSTICS", color = Muted, style = MaterialTheme.typography.labelMedium) }
        item { SettingsRow(Icons.Default.Memory, "Camera diagnostics", "Actual camera levels, capabilities, streams and keys", diagnostics) }
        item { SettingsRow(Icons.Default.ReceiptLong, "Logs", "Debug, info, warning, error · export and share", logs) }
        item { SettingsRow(Icons.Default.Lock, "Permissions", if (ui.permission) "Camera access granted · open system settings" else "Camera access required · open system settings", permissions) }
        item {
            Text("Camera3 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Android 15+ · Kotlin · Compose · Camera2\nDevelopment build: Photo (JPEG + RAW DNG on lenses that advertise it), automatic focus/exposure, supported torch/zoom. Extensions and video recording are not implemented yet.",
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
