package dev.camera3.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.camera3.app.CameraUiState
import dev.camera3.model.HardwareLevel

@Composable
fun DiagnosticsScreen(ui: CameraUiState, back: () -> Unit, refresh: () -> Unit, export: () -> Unit, exporting: Boolean) {
    PageHeader("Camera diagnostics", back) {
        IconButton(onClick = refresh, enabled = ui.permission && !ui.discovering) { Icon(Icons.Default.Refresh, "Refresh camera discovery") }
        IconButton(onClick = export, enabled = !exporting) { Icon(Icons.Default.Share, "Share diagnostic report") }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(if (ui.eligible) "LEVEL_3 reported · device eligible" else "LEVEL_3 requirement not met",
                color = if (ui.eligible) Accent else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Reported flags are not proof of working output. Only the baseline Photo pipeline is implemented; physical-device verification is pending.",
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Surface(shape = RoundedCornerShape(12.dp), color = Panel) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("ACTIVE SESSION", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Fact("State", ui.runtime.phase.name)
                    Fact("Camera ID", ui.runtime.cameraId ?: "None")
                    Fact("JPEG / Preview", ui.runtime.configuration?.let { "${it.jpeg} / ${it.preview}" } ?: "Not configured")
                    Fact("Effective ISO", ui.runtime.effectiveIso?.toString() ?: "No result")
                    Fact("Exposure (ns)", ui.runtime.effectiveExposureNs?.toString() ?: "No result")
                    Text(ui.runtime.message, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (ui.cameras.isEmpty()) item { Text(if (ui.permission) "No accessible cameras were discovered" else "Grant camera permission before discovery") }
        items(ui.cameras, key = { it.id }) { c ->
            var expanded by rememberSaveable(c.id) { mutableStateOf(false) }
            Surface(shape = RoundedCornerShape(12.dp), color = Panel) {
                Column(Modifier.padding(16.dp)) {
                    Text(c.label, style = MaterialTheme.typography.titleMedium)
                    Text(c.level.name, color = if (c.level == HardwareLevel.LEVEL_3) Accent else Muted)
                    Fact("Physical IDs", c.physicalIds.joinToString().ifEmpty { "None reported" })
                    Fact("RAW advertised", if ("RAW" in c.capabilities) "Yes · pipeline not implemented" else "No")
                    Fact("Manual sensor advertised", if ("MANUAL_SENSOR" in c.capabilities) "Yes · controls not implemented" else "No")
                    Fact("Extensions (API IDs)", c.extensionModes.joinToString().ifEmpty { "None reported" })
                    Fact("Zoom ratio range", if (c.supportsZoomRatio) "${c.minZoom}…${c.maxZoom}" else "Not exposed")
                    TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide full capabilities" else "View full capabilities") }
                    if (expanded) {
                        c.details.forEach { (key, value) -> Fact(key, value) }
                        Fact("Capabilities", c.capabilities.sorted().joinToString())
                        Fact("JPEG sizes", c.jpegSizes.joinToString())
                        Fact("Preview sizes", c.previewSizes.joinToString())
                        Fact("Request keys", c.requestKeys.sorted().joinToString("\n"))
                        Fact("Result keys", c.resultKeys.sorted().joinToString("\n"))
                    }
                }
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .4f))
}
