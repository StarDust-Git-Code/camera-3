package dev.camera3.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.Surface
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.camera3.camera.CameraEngine
import dev.camera3.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

data class CameraUiState(
    val permission: Boolean = false,
    val discovering: Boolean = false,
    val cameras: List<CameraDescriptor> = emptyList(),
    val selectedId: String? = null,
    val runtime: CameraRuntime = CameraRuntime(),
    val preferences: PhotoPreferences = PhotoPreferences(),
    val lastPhoto: Uri? = null,
    val error: String? = null,
) {
    val eligible get() = CameraPolicy.isEligible(cameras)
    val selected get() = cameras.firstOrNull { it.id == selectedId }
    val configuration get() = selected?.let(CameraPolicy::configuration)
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val services = (application as Camera3Application).services
    private val engine = CameraEngine(application, services.logger, services.photoStore, services.scope)
    private val _ui = MutableStateFlow(CameraUiState())
    val ui = _ui.asStateFlow()
    val events = services.logger.events
    private var discoveryJob: Job? = null

    init {
        services.logger.log(LogLevel.INFO, "App", "Camera3 ${BuildConfig.VERSION_NAME}, Android API ${Build.VERSION.SDK_INT}")
        viewModelScope.launch { engine.runtime.collect { value -> _ui.update { it.copy(runtime = value) } } }
        viewModelScope.launch { engine.lastPhoto.collect { value -> _ui.update { it.copy(lastPhoto = value) } } }
        viewModelScope.launch {
            services.settings.preferences.collect { value ->
                _ui.update { it.copy(preferences = value) }
                engine.setPreferences(value)
            }
        }
        checkPermission()
    }

    fun checkPermission() {
        val granted = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        _ui.update { it.copy(permission = granted) }
        if (granted && _ui.value.cameras.isEmpty() && discoveryJob?.isActive != true) discover()
        if (!granted) engine.setForeground(false)
    }

    fun discover() {
        if (!_ui.value.permission || discoveryJob?.isActive == true) return
        discoveryJob = viewModelScope.launch {
            _ui.update { it.copy(discovering = true, error = null) }
            try {
                val cameras = services.discovery.discover()
                val selected = CameraPolicy.initialCamera(cameras)
                _ui.update { it.copy(cameras = cameras, selectedId = selected?.id, discovering = false) }
                services.logger.log(LogLevel.INFO, "Eligibility", "LEVEL_3 device eligible=${CameraPolicy.isEligible(cameras)}, baselineCamera=${selected?.id}")
            } catch (error: Exception) {
                services.logger.log(LogLevel.ERROR, "Discovery", "Camera discovery failed", error)
                _ui.update { it.copy(discovering = false, error = "Cannot discover cameras: ${error.message}") }
            }
        }
    }

    fun selectCamera(id: String) {
        val state = _ui.value
        if (!state.eligible || state.runtime.phase == CameraPhase.CAPTURING) return
        val camera = state.cameras.firstOrNull { it.id == id } ?: return
        if (CameraPolicy.configuration(camera) != null) _ui.update { it.copy(selectedId = id) }
    }
    fun switchCamera() {
        val state = _ui.value
        val choices = state.cameras.filter { CameraPolicy.configuration(it) != null }
        val opposite = choices.firstOrNull { it.facing != state.selected?.facing }
        val next = opposite ?: choices.getOrNull((choices.indexOf(state.selected) + 1) % choices.size.coerceAtLeast(1))
        next?.let { selectCamera(it.id) }
    }

    fun attachSurface(surface: Surface) {
        val state = _ui.value
        if (state.permission && state.eligible) {
            state.selected?.let { camera -> state.configuration?.let { engine.setSource(camera, it, surface) } }
        }
    }
    fun detachSurface(surface: Surface) = engine.detachSurface(surface)
    fun setForeground(value: Boolean) {
        if (value) checkPermission()
        engine.setForeground(value && _ui.value.permission)
        services.logger.log(LogLevel.DEBUG, "Lifecycle", "Camera foreground=$value")
    }
    fun capture(rotation: Int) = engine.capture(rotation * 90)
    fun zoom(value: Float) = engine.setZoom(value)
    fun torch() = engine.toggleTorch()
    fun retry() { if (_ui.value.cameras.isEmpty()) discover() else engine.retry() }
    fun setGrid(value: Boolean) = settingAction { services.settings.setGrid(value) }
    fun setQuality(value: Int) = settingAction { services.settings.setJpegQuality(value) }
    private fun settingAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() } catch (error: Exception) {
                services.logger.log(LogLevel.ERROR, "Settings", "Preference could not be saved", error)
                _ui.update { it.copy(error = "Setting was not saved: ${error.message}") }
            }
        }
    }
    fun dismissError() { _ui.update { it.copy(error = null) } }
    suspend fun clearLogs() = services.logger.clear()
    suspend fun exportDiagnostics(): File {
        val state = _ui.value
        val header = buildString {
            appendLine("Camera3 ${BuildConfig.VERSION_NAME} diagnostic report")
            appendLine("Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT}, ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("LEVEL_3 gate: ${state.eligible}; selected: ${state.selectedId}")
            appendLine("Runtime: ${state.runtime}")
            appendLine("Reported support is not proof of a working advanced capture pipeline.")
            state.cameras.forEach { c ->
                appendLine("\n${c.label} — ${c.level}, sensor orientation=${c.sensorOrientation}")
                appendLine("Physical IDs: ${c.physicalIds}; capabilities: ${c.capabilities}")
                appendLine("Extensions (platform IDs): ${c.extensionModes}")
                appendLine("JPEG sizes: ${c.jpegSizes}; preview sizes: ${c.previewSizes}")
                c.details.forEach { (key, value) -> appendLine("$key: $value") }
                appendLine("Request keys: ${c.requestKeys.sorted().joinToString()}")
                appendLine("Result keys: ${c.resultKeys.sorted().joinToString()}")
            }
        }
        return services.logger.export(header)
    }
    override fun onCleared() { engine.dispose(); super.onCleared() }
}
