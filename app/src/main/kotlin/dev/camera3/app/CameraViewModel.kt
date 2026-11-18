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
import dev.camera3.camera.StreamPlan
import dev.camera3.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

data class CameraUiState(
    val permission: Boolean = false,
    val micGranted: Boolean = false,
    val discovering: Boolean = false,
    val cameras: List<CameraDescriptor> = emptyList(),
    val selectedId: String? = null,
    val mode: CameraMode = CameraMode.PHOTO,
    val runtime: CameraRuntime = CameraRuntime(),
    val preferences: PhotoPreferences = PhotoPreferences(),
    val lastPhoto: Uri? = null,
    val error: String? = null,
) {
    val eligible get() = CameraPolicy.isEligible(cameras)
    val selected get() = cameras.firstOrNull { it.id == selectedId }
    val rawEnabled get() = preferences.raw
    val configuration get() = selected?.let { camera ->
        if (mode == CameraMode.PHOTO) CameraPolicy.configuration(camera, rawEnabled) else null
    }
    val videoConfiguration get() = selected?.let { camera ->
        if (mode == CameraMode.VIDEO) CameraPolicy.videoConfiguration(camera, micGranted) else null
    }
    val photoUsable get() = selected?.let { CameraPolicy.configuration(it, rawEnabled) } != null
    val videoUsable get() = selected?.let { CameraPolicy.videoConfiguration(it, micGranted) } != null
    val busy get() = runtime.phase == CameraPhase.CAPTURING || runtime.phase == CameraPhase.RECORDING
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
        val mic = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        _ui.update { it.copy(permission = granted, micGranted = mic) }
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
                services.logger.log(LogLevel.DEBUG, "Discovery", "P-016 viewModel.discover count=${cameras.size} selectedId=${selected?.id}")
                services.logger.log(LogLevel.INFO, "Eligibility", "LEVEL_3 device eligible=${CameraPolicy.isEligible(cameras)}, baselineCamera=${selected?.id}")
            } catch (error: Exception) {
                services.logger.log(LogLevel.ERROR, "Discovery", "Camera discovery failed", error)
                _ui.update { it.copy(discovering = false, error = "Cannot discover cameras: ${error.message}") }
            }
        }
    }

    fun selectCamera(id: String) {
        val state = _ui.value
        if (!state.eligible || state.busy) return
        val camera = state.cameras.firstOrNull { it.id == id } ?: return
        val usable = when (state.mode) {
            CameraMode.PHOTO -> CameraPolicy.configuration(camera, state.preferences.raw)
            CameraMode.VIDEO -> CameraPolicy.videoConfiguration(camera, state.micGranted)
        } != null
        if (usable) _ui.update { it.copy(selectedId = id) }
    }
    fun switchCamera() {
        val state = _ui.value
        val choices = state.cameras.filter { camera ->
            when (state.mode) {
                CameraMode.PHOTO -> CameraPolicy.configuration(camera, state.preferences.raw) != null
                CameraMode.VIDEO -> CameraPolicy.videoConfiguration(camera, state.micGranted) != null
            }
        }
        val opposite = choices.firstOrNull { it.facing != state.selected?.facing }
        val next = opposite ?: choices.getOrNull((choices.indexOf(state.selected) + 1) % choices.size.coerceAtLeast(1))
        next?.let { selectCamera(it.id) }
    }
    fun setMode(mode: CameraMode) {
        val state = _ui.value
        if (!state.eligible || state.busy || state.mode == mode) return
        // The mode change rekeys the preview, which detaches/reattaches the surface and
        // reconfigures the engine with the matching plan.
        _ui.update { it.copy(mode = mode) }
    }

    fun attachSurface(surface: Surface, displayRotation: Int) {
        val state = _ui.value
        val degrees = displayRotation * 90
        services.logger.log(LogLevel.DEBUG, "Preview", "P-016 viewModel.attachSurface selectedId=${state.selectedId} mode=${state.mode} surfaceValid=${surface.isValid} surfaceHash=${System.identityHashCode(surface)} permission=${state.permission} eligible=${state.eligible} displayRotation=$displayRotation degrees=$degrees")
        if (!state.permission || !state.eligible) {
            services.logger.log(LogLevel.DEBUG, "Preview", "P-016 viewModel.attachSurface skipped reason=${if (!state.permission) "no-permission" else "ineligible"} selectedId=${state.selectedId} mode=${state.mode}")
            return
        }
        if (state.selected == null) {
            services.logger.log(LogLevel.DEBUG, "Preview", "P-016 viewModel.attachSurface skipped reason=no-camera selectedId=${state.selectedId} mode=${state.mode}")
        }
        state.selected?.let { camera ->
            val plan = when (state.mode) {
                CameraMode.PHOTO -> state.configuration?.let { StreamPlan.Photo(it) }
                CameraMode.VIDEO -> state.videoConfiguration?.let { StreamPlan.Video(it) }
            } ?: run {
                services.logger.log(LogLevel.DEBUG, "Preview", "P-016 viewModel.attachSurface skipped reason=${if (state.mode == CameraMode.PHOTO) "no-photo-configuration" else "no-video-configuration"} selectedId=${state.selectedId} mode=${state.mode}")
                return
            }
            engine.setSource(camera, plan, surface, degrees)
        }
    }
    fun detachSurface(surface: Surface) = engine.detachSurface(surface)
    fun setForeground(value: Boolean) {
        if (value) checkPermission()
        engine.setForeground(value && _ui.value.permission)
        services.logger.log(LogLevel.DEBUG, "Lifecycle", "Camera foreground=$value")
    }
    fun capture(rotation: Int) = engine.capture(rotation * 90)
    fun startRecording() = engine.startRecording()
    fun stopRecording() = engine.stopRecording()
    fun zoom(value: Float) = engine.setZoom(value)
    fun torch() = engine.toggleTorch()
    fun retry() { if (_ui.value.cameras.isEmpty()) discover() else engine.retry() }
    fun setGrid(value: Boolean) = settingAction { services.settings.setGrid(value) }
    fun setQuality(value: Int) = settingAction { services.settings.setJpegQuality(value) }
    fun setRaw(value: Boolean) = settingAction { services.settings.setRaw(value) }
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
