package dev.camera3.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.Surface
import dev.camera3.diagnostics.AppLogger
import dev.camera3.media.PhotoStore
import dev.camera3.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor

/** All device/session mutations and callbacks are confined to a single handler. */
class CameraEngine(
    context: Context,
    private val logger: AppLogger,
    private val store: PhotoStore,
    private val ioScope: CoroutineScope,
) {
    private val manager = context.applicationContext.getSystemService(CameraManager::class.java)
    private val thread = HandlerThread("Camera3-camera").apply { start() }
    private val handler = Handler(thread.looper)
    private val executor = Executor { handler.post(it) }
    private val _runtime = MutableStateFlow(CameraRuntime())
    val runtime = _runtime.asStateFlow()
    private val _lastPhoto = MutableStateFlow<Uri?>(null)
    val lastPhoto = _lastPhoto.asStateFlow()

    private data class Source(val camera: CameraDescriptor, val config: PhotoConfiguration, val surface: Surface)
    private class Run(val epoch: Long, val source: Source, val reader: ImageReader) {
        var device: CameraDevice? = null
        var session: CameraCaptureSession? = null
        var closing = false
        var deviceClosed = false
        var imageJobs = 0
        var captureId: Long? = null
        var timeout: Runnable? = null
        var lastTelemetry = 0L
    }
    private var desired: Source? = null
    private var active: Run? = null
    private var foreground = false
    private var disposed = false
    private var blocked = false
    private var epoch = 0L
    private var captureSequence = 0L
    private var imageJobs = 0
    private var preferences = PhotoPreferences()

    fun setSource(camera: CameraDescriptor, config: PhotoConfiguration, surface: Surface) = post {
        val source = Source(camera, config, surface)
        if (source != desired) blocked = false
        desired = source
        reconcile()
    }
    fun detachSurface(surface: Surface) = post {
        if (desired?.surface === surface) desired = null
        reconcile()
    }
    fun setForeground(value: Boolean) = post {
        if (value && !foreground) blocked = false
        foreground = value
        reconcile()
    }
    fun setPreferences(value: PhotoPreferences) = post { preferences = value }
    fun retry() = post { blocked = false; reconcile() }
    fun dispose() = post { disposed = true; desired = null; foreground = false; reconcile(); quitIfSafe() }

    private fun post(action: () -> Unit) {
        handler.post {
            if (!disposed) {
                try { action() } catch (error: Exception) { fail(active, "Camera operation failed", error) }
            }
        }
    }

    private fun reconcile() {
        val target = desired?.takeIf { foreground && !disposed && it.surface.isValid && !blocked }
        val current = active
        if (current != null) {
            if (target != current.source || current.closing) close(current)
            return // Never reopen until the old CameraDevice's onClosed callback.
        }
        if (target == null) {
            if (!blocked) _runtime.value = _runtime.value.copy(phase = CameraPhase.IDLE, message = "Camera paused")
            quitIfSafe()
            return
        }
        open(target)
    }

    @SuppressLint("MissingPermission") // Permission is gated by the app; revocation is caught as a real error.
    private fun open(source: Source) {
        val reader = ImageReader.newInstance(source.config.jpeg.width, source.config.jpeg.height, ImageFormat.JPEG, 2)
        val run = Run(++epoch, source, reader)
        active = run
        _runtime.value = CameraRuntime(CameraPhase.OPENING, source.camera.id, source.config, "Opening ${source.camera.label}")
        reader.setOnImageAvailableListener({ receiveImage(run) }, handler)
        logger.log(LogLevel.INFO, "Camera", "Opening ID ${source.camera.id}, epoch=${run.epoch}, JPEG=${source.config.jpeg}, preview=${source.config.preview}")
        try {
            manager.openCamera(source.camera.id, executor, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    run.device = camera
                    if (active !== run || run.closing || desired != source || !foreground || disposed || blocked) {
                        close(run)
                    } else configure(run)
                }
                override fun onDisconnected(camera: CameraDevice) {
                    run.device = camera
                    if (!run.closing) fail(run, "Camera disconnected or was taken by another app") else camera.close()
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    run.device = camera
                    if (!run.closing) fail(run, "Camera device error $error") else camera.close()
                }
                override fun onClosed(camera: CameraDevice) {
                    run.deviceClosed = true
                    releaseReaderIfSafe(run)
                    if (active === run) active = null
                    logger.log(LogLevel.DEBUG, "Camera", "Closed epoch=${run.epoch}")
                    reconcile()
                }
            })
        } catch (error: Exception) {
            run.deviceClosed = true
            active = null
            releaseReaderIfSafe(run)
            fail(null, "Cannot open camera ${source.camera.id}", error)
        }
    }

    private fun configure(run: Run) {
        try {
            _runtime.value = _runtime.value.copy(phase = CameraPhase.CONFIGURING, message = "Configuring JPEG + preview")
            val outputs = listOf(
                OutputConfiguration(run.source.surface).apply { setMirrorMode(OutputConfiguration.MIRROR_MODE_AUTO) },
                OutputConfiguration(run.reader.surface),
            )
            val config = SessionConfiguration(SessionConfiguration.SESSION_REGULAR, outputs, executor,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (active !== run || run.closing || disposed) { session.close(); return }
                        run.session = session
                        try {
                            session.setRepeatingRequest(request(run, CameraDevice.TEMPLATE_PREVIEW).apply {
                                addTarget(run.source.surface)
                            }.build(), telemetry(run), handler)
                            _runtime.value = _runtime.value.copy(phase = CameraPhase.PREVIEWING, message = "Photo · JPEG")
                            logger.log(LogLevel.INFO, "Session", "Preview configured: epoch=${run.epoch}")
                        } catch (error: Exception) { fail(run, "Cannot start camera preview", error) }
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        session.close()
                        if (!run.closing && active === run) fail(run, "JPEG + preview configuration rejected by camera")
                    }
                })
            // Some HALs don't implement this query; successful configuration is still required.
            val supported = try { run.device!!.isSessionConfigurationSupported(config) }
                catch (error: UnsupportedOperationException) {
                    logger.log(LogLevel.WARNING, "Session", "Session support query unavailable; trying advertised streams", error)
                    true
                }
            check(supported) { "Camera reports this stream combination unsupported" }
            run.device!!.createCaptureSession(config)
        } catch (error: Exception) { fail(run, "Cannot configure camera session", error) }
    }

    private fun request(run: Run, template: Int): CaptureRequest.Builder {
        val builder = run.device!!.createCaptureRequest(template)
        val c = run.source.camera
        val controls = _runtime.value
        fun <T> set(key: CaptureRequest.Key<T>, value: T) {
            if (key.name in c.requestKeys) builder.set(key, value)
        }
        set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        val af = when {
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE in c.afModes -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            CaptureRequest.CONTROL_AF_MODE_AUTO in c.afModes -> CaptureRequest.CONTROL_AF_MODE_AUTO
            else -> CaptureRequest.CONTROL_AF_MODE_OFF
        }
        set(CaptureRequest.CONTROL_AF_MODE, af)
        if (c.flashAvailable) set(CaptureRequest.FLASH_MODE,
            if (controls.torch) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF)
        if (c.supportsZoomRatio) set(CaptureRequest.CONTROL_ZOOM_RATIO, controls.zoom)
        // Prevent an automatic compatibility crop on devices where NONE is explicitly exposed.
        val modes = manager.getCameraCharacteristics(c.id)[CameraCharacteristics.SCALER_AVAILABLE_ROTATE_AND_CROP_MODES]
        if (modes?.contains(CaptureRequest.SCALER_ROTATE_AND_CROP_NONE) == true) {
            set(CaptureRequest.SCALER_ROTATE_AND_CROP, CaptureRequest.SCALER_ROTATE_AND_CROP_NONE)
        }
        return builder
    }

    private fun telemetry(run: Run) = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            val now = SystemClock.elapsedRealtime()
            if (active !== run || run.closing || now - run.lastTelemetry < 500) return
            run.lastTelemetry = now
            _runtime.value = _runtime.value.copy(
                effectiveIso = result[CaptureResult.SENSOR_SENSITIVITY],
                effectiveExposureNs = result[CaptureResult.SENSOR_EXPOSURE_TIME],
            )
        }
        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
            if (active === run && !run.closing) fail(run, "Preview request failed: reason=${failure.reason}")
        }
    }

    fun setZoom(value: Float) = post {
        val run = active ?: return@post
        if (run.closing || _runtime.value.phase != CameraPhase.PREVIEWING || !run.source.camera.supportsZoomRatio) return@post
        val previous = _runtime.value
        try {
            _runtime.value = previous.copy(zoom = CameraPolicy.validatedZoom(value, run.source.camera))
            updateRepeating(run)
        } catch (error: Exception) { _runtime.value = previous; fail(run, "Zoom request rejected", error) }
    }
    fun toggleTorch() = post {
        val run = active ?: return@post
        if (run.closing || _runtime.value.phase != CameraPhase.PREVIEWING || !run.source.camera.supportsTorch) return@post
        val previous = _runtime.value
        try {
            _runtime.value = previous.copy(torch = !previous.torch)
            updateRepeating(run)
        } catch (error: Exception) { _runtime.value = previous; fail(run, "Torch request rejected", error) }
    }
    private fun updateRepeating(run: Run) {
        run.session!!.setRepeatingRequest(request(run, CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(run.source.surface)
        }.build(), telemetry(run), handler)
        logger.log(LogLevel.INFO, "Controls", "ID ${run.source.camera.id}: zoom=${_runtime.value.zoom}, torch=${_runtime.value.torch}")
    }

    fun capture(displayDegrees: Int) = post {
        val run = active ?: return@post
        if (_runtime.value.phase != CameraPhase.PREVIEWING || run.closing || run.captureId != null) return@post
        val id = ++captureSequence
        run.captureId = id
        _runtime.value = _runtime.value.copy(phase = CameraPhase.CAPTURING, message = "Capturing and saving…")
        try {
            val orientation = CameraPolicy.jpegOrientation(run.source.camera.sensorOrientation, displayDegrees,
                run.source.camera.facing == LensFacing.FRONT)
            val builder = request(run, CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(run.reader.surface)
                set(CaptureRequest.JPEG_ORIENTATION, orientation)
                set(CaptureRequest.JPEG_QUALITY, preferences.jpegQuality.toByte())
                setTag(id)
            }
            run.timeout = Runnable {
                if (active === run && run.captureId == id && run.imageJobs == 0) {
                    fail(run, "Timed out waiting for JPEG capture $id")
                }
            }.also { handler.postDelayed(it, 12_000) }
            run.session!!.capture(builder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    logger.log(LogLevel.INFO, "Capture", "JPEG $id result: epoch=${run.epoch}, orientation=$orientation, ISO=${result[CaptureResult.SENSOR_SENSITIVITY]}, exposureNs=${result[CaptureResult.SENSOR_EXPOSURE_TIME]}")
                }
                override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
                    if (active === run && !run.closing) fail(run, "Photo $id failed: reason=${failure.reason}")
                }
            }, handler)
        } catch (error: Exception) { fail(run, "Cannot capture photo $id", error) }
    }

    private fun receiveImage(run: Run) {
        val image = try { run.reader.acquireNextImage() } catch (error: Exception) {
            if (!run.closing) fail(run, "Cannot acquire JPEG", error)
            return
        } ?: return
        val id = run.captureId
        if (id == null || run.closing || run.imageJobs > 0) { image.close(); return }
        run.timeout?.let { handler.removeCallbacks(it) }
        run.imageJobs++; imageJobs++
        ioScope.launch(Dispatchers.IO) {
            val outcome = runCatching {
                val bytes = try {
                    val buffer = image.planes[0].buffer
                    ByteArray(buffer.remaining()).also { buffer.get(it) }
                } finally { image.close() }
                store.saveJpeg(bytes)
            }
            handler.post {
                run.imageJobs--; imageJobs--
                run.captureId = null
                outcome.onSuccess { uri ->
                    _lastPhoto.value = uri
                    logger.log(LogLevel.INFO, "Storage", "JPEG $id saved to DCIM/Camera3")
                    if (active === run && !run.closing) _runtime.value = _runtime.value.copy(
                        phase = CameraPhase.PREVIEWING, message = "Photo saved to DCIM/Camera3")
                }.onFailure { error ->
                    logger.log(LogLevel.ERROR, "Storage", "JPEG $id could not be saved", error)
                    if (active === run && !run.closing) _runtime.value = _runtime.value.copy(
                        phase = CameraPhase.PREVIEWING, message = "Photo was not saved: ${error.message}")
                }
                releaseReaderIfSafe(run)
                quitIfSafe()
            }
        }
    }

    private fun close(run: Run) {
        run.closing = true
        run.timeout?.let { handler.removeCallbacks(it) }
        if (!blocked) _runtime.value = _runtime.value.copy(phase = CameraPhase.CLOSING, message = "Closing camera")
        try { run.session?.stopRepeating() } catch (error: Exception) { logger.log(LogLevel.DEBUG, "Close", "Repeating request already stopped", error) }
        run.session?.close()
        run.device?.close() // If still opening, onOpened will close it before configuring.
    }
    private fun releaseReaderIfSafe(run: Run) {
        if (run.deviceClosed && run.imageJobs == 0) run.reader.close()
    }
    private fun fail(run: Run?, message: String, error: Throwable? = null) {
        if (run != null && active !== run) return
        blocked = true
        _runtime.value = _runtime.value.copy(phase = CameraPhase.ERROR, message = message)
        logger.log(LogLevel.ERROR, "Camera", message, error)
        if (run != null) close(run)
    }
    private fun quitIfSafe() {
        if (disposed && active == null && imageJobs == 0) thread.quitSafely()
    }
}
