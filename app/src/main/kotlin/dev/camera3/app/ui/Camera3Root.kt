package dev.camera3.app.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.camera3.app.CameraUiState
import dev.camera3.app.CameraViewModel
import dev.camera3.model.CameraPhase
import dev.camera3.model.CameraPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class Page { CAMERA, SETTINGS, DIAGNOSTICS, LOGS }

@Composable
fun Camera3Root(model: CameraViewModel) {
    val ui by model.ui.collectAsStateWithLifecycle()
    val events by model.events.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(Page.CAMERA) }
    var notice by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        model.checkPermission()
        model.setForeground(page == Page.CAMERA && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle, page) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> model.setForeground(page == Page.CAMERA)
                Lifecycle.Event.ON_PAUSE -> model.setForeground(false)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        model.setForeground(page == Page.CAMERA && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { lifecycle.removeObserver(observer); model.setForeground(false) }
    }
    BackHandler(page != Page.CAMERA) { page = if (page == Page.SETTINGS) Page.CAMERA else Page.SETTINGS }

    fun openSettings() {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }
    fun export() {
        if (exporting) return
        exporting = true
        scope.launch {
            try {
                val file = model.exportDiagnostics()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share diagnostics"))
            } catch (error: Exception) { notice = "Could not share diagnostics: ${error.message}" }
            finally { exporting = false }
        }
    }

    Surface(Modifier.fillMaxSize(), color = Ink) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            when (page) {
                Page.CAMERA -> CameraScreen(ui, model,
                    settings = { page = Page.SETTINGS },
                    askPermission = { permission.launch(Manifest.permission.CAMERA) },
                    appSettings = { openSettings() },
                    diagnostics = { page = Page.DIAGNOSTICS },
                    openPhoto = { uri ->
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "image/jpeg")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            })
                        } catch (_: ActivityNotFoundException) { notice = "No installed app can open this photo" }
                    })
                Page.SETTINGS -> SettingsScreen(ui, model, { page = Page.CAMERA },
                    { page = Page.DIAGNOSTICS }, { page = Page.LOGS }, { openSettings() })
                Page.DIAGNOSTICS -> DiagnosticsScreen(ui, { page = Page.SETTINGS }, { model.discover() }, { export() }, exporting)
                Page.LOGS -> LogsScreen(events, { page = Page.SETTINGS }, { export() }, exporting,
                    clear = { scope.launch {
                        try { model.clearLogs() } catch (error: Exception) { notice = "Could not clear logs: ${error.message}" }
                    } })
            }
        }
    }
    val error = notice ?: ui.error
    if (error != null) AlertDialog(
        onDismissRequest = { notice = null; model.dismissError() },
        title = { Text("Camera3") }, text = { Text(error) },
        confirmButton = { TextButton(onClick = { notice = null; model.dismissError() }) { Text("OK") } },
    )
}

@Composable
private fun ColumnScope.CameraScreen(
    ui: CameraUiState,
    model: CameraViewModel,
    settings: () -> Unit,
    askPermission: () -> Unit,
    appSettings: () -> Unit,
    diagnostics: () -> Unit,
    openPhoto: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val ready = ui.runtime.phase == CameraPhase.PREVIEWING && ui.runtime.cameraId == ui.selectedId
    val changingAllowed = ui.runtime.phase != CameraPhase.CAPTURING
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = model::torch, enabled = ready && ui.selected?.supportsTorch == true) {
            Icon(if (ui.runtime.torch) Icons.Default.FlashOn else Icons.Default.FlashOff, "Toggle continuous torch")
        }
        Text("JPEG", style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.border(1.dp, Muted, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        Text(ui.configuration?.jpeg?.let { "${String.format(Locale.US, "%.1f", it.area / 1_000_000.0)} MP" } ?: "PHOTO",
            style = MaterialTheme.typography.labelMedium)
        IconButton(onClick = settings) { Icon(Icons.Default.Settings, "Open settings") }
    }
    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        val camera = ui.selected
        val config = ui.configuration
        when {
            !ui.permission -> CameraMessage("Camera access", "Allow camera access to inspect real hardware and start the viewfinder.",
                "Allow camera", askPermission, "App permissions", appSettings)
            ui.discovering -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(); Spacer(Modifier.height(20.dp)); Text("Inspecting cameras…")
            }
            !ui.eligible -> CameraMessage("LEVEL_3 required", "No accessible camera reports LEVEL_3. Capture is disabled on this device.",
                "Retry discovery", model::retry, "View diagnostics", diagnostics)
            camera == null || config == null -> CameraMessage("No baseline configuration", "LEVEL_3 is reported, but no qualifying camera exposes matching JPEG and preview sizes.",
                "Retry discovery", model::discover, "View diagnostics", diagnostics)
            else -> {
                CameraPreview(camera, config, ui.preferences.grid, Modifier.fillMaxSize(), model::attachSurface, model::detachSurface)
                if (ui.runtime.phase == CameraPhase.ERROR) {
                    CameraMessage("Camera could not start", ui.runtime.message, "Retry", model::retry, "Diagnostics", diagnostics)
                } else if (ui.runtime.phase in listOf(CameraPhase.OPENING, CameraPhase.CONFIGURING, CameraPhase.CLOSING)) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
                if (camera.supportsZoomRatio && camera.maxZoom > camera.minZoom) {
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
                        .background(Ink.copy(alpha = .65f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp)) {
                        Text("${String.format(Locale.US, "%.1f", ui.runtime.zoom)}× · zoom ratio", style = MaterialTheme.typography.labelMedium)
                        Slider(value = ui.runtime.zoom.coerceIn(camera.minZoom, camera.maxZoom), onValueChange = model::zoom,
                            enabled = ready, valueRange = camera.minZoom..camera.maxZoom)
                    }
                }
            }
        }
    }
    if (ui.eligible && ui.cameras.size > 1) LazyRow(
        Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ui.cameras.filter { CameraPolicy.configuration(it) != null }, key = { it.id }) { camera ->
            FilterChip(selected = camera.id == ui.selectedId, onClick = { model.selectCamera(camera.id) },
                label = { Text(camera.label) }, enabled = changingAllowed)
        }
    }
    LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)) {
        items(listOf("Night", "Portrait", "Photo", "Video", "Pro Video")) { mode ->
            if (mode == "Photo") Surface(shape = CircleShape, color = Foreground, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Photo", color = Ink, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
            } else TextButton(onClick = {}, enabled = false) { Text(mode, style = MaterialTheme.typography.labelMedium) }
        }
    }
    Text("This development build supports Photo only", color = Muted, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.align(Alignment.CenterHorizontally))
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        GalleryButton(ui.lastPhoto, openPhoto)
        Surface(onClick = { model.capture(context.display?.rotation ?: 0) }, enabled = ready,
            shape = CircleShape, color = Ink, modifier = Modifier.size(76.dp).border(2.dp, if (ready) Foreground else Muted, CircleShape)) {
            Box(Modifier.padding(6.dp).background(if (ready) Foreground else Panel, CircleShape), contentAlignment = Alignment.Center) {
                if (ui.runtime.phase == CameraPhase.CAPTURING) CircularProgressIndicator(Modifier.size(24.dp), color = Foreground)
                else Icon(Icons.Default.CameraAlt, "Capture JPEG photo", tint = if (ready) Ink else Muted, modifier = Modifier.size(22.dp))
            }
        }
        IconButton(onClick = model::switchCamera, enabled = ui.eligible && ui.cameras.size > 1 && changingAllowed,
            modifier = Modifier.size(48.dp).border(1.dp, Muted.copy(alpha = .35f), CircleShape)) {
            Icon(Icons.Default.Cameraswitch, "Switch camera")
        }
    }
    Text(ui.runtime.message, style = MaterialTheme.typography.labelSmall, color = Muted,
        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp).align(Alignment.CenterHorizontally))
}

@Composable
private fun GalleryButton(uri: Uri?, openPhoto: (Uri) -> Unit) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, uri) {
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.loadThumbnail(uri, Size(128, 128), null).asImageBitmap() }.getOrNull()
        }
    }
    Surface(onClick = { uri?.let(openPhoto) }, enabled = uri != null, shape = RoundedCornerShape(10.dp),
        modifier = Modifier.size(48.dp), color = Panel) {
        Box(contentAlignment = Alignment.Center) {
            image?.let { Image(it, "Open last saved photo", Modifier.fillMaxSize()) }
                ?: Icon(Icons.Default.PhotoLibrary, "Open last saved photo")
        }
    }
}

@Composable
private fun CameraMessage(title: String, message: String, action: String, onAction: () -> Unit,
                          secondary: String, onSecondary: () -> Unit) {
    Surface(color = Ink.copy(alpha = .9f), shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(24.dp)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CameraAlt, null, tint = Muted, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(16.dp)); Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp)); Text(message, color = Muted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp)); Button(onClick = onAction) { Text(action) }
            TextButton(onClick = onSecondary) { Text(secondary) }
        }
    }
}

@Composable
fun PageHeader(title: String, back: () -> Unit, action: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action()
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Panel, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}
