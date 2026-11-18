package dev.camera3.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.Surface
import dev.camera3.diagnostics.AppLogger
import dev.camera3.media.PhotoStore
import dev.camera3.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor
import kotlin.math.abs
import kotlin.math.ln

/** What a session is configured to produce; switching it reconfigures the camera. */
sealed interface StreamPlan {
    val mode: CameraMode
    data class Photo(val photo: PhotoConfiguration) : StreamPlan {
        override val mode get() = CameraMode.PHOTO
    }
    data class Video(val video: VideoConfiguration) : StreamPlan {
        override val mode get() = CameraMode.VIDEO
    }
}

/** All device/session mutations and callbacks are confined to a single handler. */
class CameraEngine(
    context: Context,
    private val logger: AppLogger,
    private val store: PhotoStore,
    private val ioScope: CoroutineScope,
) {
    private val manager = context.applicationContext.getSystemService(CameraManager::class.java)
    private val appContext = context.applicationContext
    private val thread = HandlerThread("Camera3-camera").apply { start() }
    private val handler = Handler(thread.looper)
    private val executor = Executor { handler.post(it) }
    private val _runtime = MutableStateFlow(CameraRuntime())
    val runtime = _runtime.asStateFlow()
    private val _lastPhoto = MutableStateFlow<Uri?>(null)
    val lastPhoto = _lastPhoto.asStateFlow()

    private data class Source(
        val camera: CameraDescriptor,
        val plan: StreamPlan,
        val surface: Surface,
        val displayDegrees: Int, // Frozen at attach time for the recorder's orientation hint.
    )
    // pendingJpeg/pendingRaw track save jobs from arrival to settle; a capture finishes only
    // when every expected output has settled, so a failed JPEG still waits for its DNG.
    private class Run(val epoch: Long, val source: Source, val reader: ImageReader?, val rawReader: ImageReader?) {
        var device: CameraDevice? = null
        var session: CameraCaptureSession? = null
        var closing = false
        var deviceClosed = false
        var readersClosed = false
        var pendingJpeg = false
        var pendingRaw = false
        var savedJpeg = false
        var savedDng = false
        var jpegError: String? = null
        var rawError: String? = null
        var rawImage: Image? = null
        var rawResult: TotalCaptureResult? = null
        var exif = 1
        var recorder: MediaRecorder? = null
        var recorderSurface: Surface? = null
        var videoUri: Uri? = null
        var videoFd: ParcelFileDescriptor? = null
        var recording = false
        var stopping = false
        var captureId: Long? = null
        var timeout: Runnable? = null
        var zoomStep: Runnable? = null
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

    // Phases in which user controls (zoom/torch) may re-submit the repeating request.
    private val CONTROL_PHASES = listOf(CameraPhase.PREVIEWING, CameraPhase.RECORDING)

    fun setSource(camera: CameraDescriptor, plan: StreamPlan, surface: Surface, displayDegrees: Int = 0) = post {
        logger.log(LogLevel.DEBUG, "Camera", "P-016 engine.setSource cameraId=${camera.id} plan=${plan.mode} surfaceValid=${surface.isValid} surfaceHash=${System.identityHashCode(surface)} displayDegrees=$displayDegrees")
        val source = Source(camera, plan, surface, displayDegrees)
        if (source != desired) blocked = false
        desired = source
        reconcile()
    }
    fun detachSurface(surface: Surface) = post {
        logger.log(LogLevel.DEBUG, "Camera", "P-016 engine.detachSurface matchesDesired=${desired?.surface === surface} surfaceValid=${surface.isValid} surfaceHash=${System.identityHashCode(surface)} desiredId=${desired?.camera?.id}")
        if (desired?.surface === surface) desired = null
        reconcile()
    }
    fun setForeground(value: Boolean) = post {
        logger.log(LogLevel.DEBUG, "Camera", "P-016 engine.setForeground value=$value previous=$foreground")
        if (value && !foreground) blocked = false
        foreground = value
        reconcile()
    }
    fun setPreferences(value: PhotoPreferences) = post { preferences = value }
    fun retry() = post { blocked = false; reconcile() }
    fun dispose() = post {
        logger.log(LogLevel.DEBUG, "Camera", "P-016 engine.dispose desiredId=${desired?.camera?.id} foreground=$foreground disposed=$disposed blocked=$blocked epoch=$epoch activeEpoch=${active?.epoch} phase=${_runtime.value.phase}")
        disposed = true; desired = null; foreground = false; reconcile(); quitIfSafe()
    }

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
        val pausedReason = when {
            disposed -> "disposed"
            desired == null -> "no-source"
            !foreground -> "not-foreground"
            blocked -> "blocked"
            target == null -> "invalid-surface"
            else -> "none"
        }
        logger.log(LogLevel.DEBUG, "Camera", "P-016 engine.reconcile desiredId=${desired?.camera?.id} plan=${desired?.plan?.mode} hasSource=${desired != null} foreground=$foreground disposed=$disposed surfaceValid=${desired?.surface?.isValid} surfaceHash=${desired?.surface?.let { System.identityHashCode(it) }} blocked=$blocked pausedReason=$pausedReason targetId=${target?.camera?.id} epoch=$epoch activeEpoch=${current?.epoch} activeId=${current?.source?.camera?.id} phase=${_runtime.value.phase} closing=${current?.closing}")
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
        val reader = if (source.plan is StreamPlan.Photo) {
            ImageReader.newInstance(source.plan.photo.jpeg.width, source.plan.photo.jpeg.height, ImageFormat.JPEG, 2)
        } else null
        val rawReader = (source.plan as? StreamPlan.Photo)?.photo?.raw?.let {
            ImageReader.newInstance(it.width, it.height, ImageFormat.RAW_SENSOR, 2)
        }
        val run = Run(++epoch, source, reader, rawReader)
        active = run
        val opened = when (val plan = source.plan) {
            is StreamPlan.Photo -> "Photo · JPEG=${plan.photo.jpeg}, preview=${plan.photo.preview}" +
                (plan.photo.raw?.let { ", RAW=$it" } ?: "") +
                (if (plan.photo.sensorPixelMode == SensorPixelMode.MAXIMUM_RESOLUTION) ", maximum-resolution pixel mode" else "")
            is StreamPlan.Video -> "Video · ${plan.video.video}, preview=${plan.video.preview}" +
                (if (plan.video.mic) ", mic" else ", silent")
        }
        _runtime.value = CameraRuntime(CameraPhase.OPENING, source.plan.mode, source.camera.id, null,
            "Opening ${source.camera.label}")
        reader?.setOnImageAvailableListener({ receiveImage(run) }, handler)
        rawReader?.setOnImageAvailableListener({ receiveRaw(run) }, handler)
        logger.log(LogLevel.INFO, "Camera", "Opening ID ${source.camera.id}, epoch=${run.epoch}, $opened")
        try {
            if (source.plan is StreamPlan.Video) prepareRecorder(run)
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
            finalizeRecording(run, publish = false)
            fail(null, "Cannot open camera ${source.camera.id}", error)
        }
    }

    // Handler thread. The recorder surface must exist before the session is configured, so
    // the recorder is prepared here with its MediaStore file descriptor already assigned.
    private fun prepareRecorder(run: Run) {
        val video = (run.source.plan as StreamPlan.Video).video
        val output = store.openVideoOutput()
        run.videoUri = output.uri
        run.videoFd = output.descriptor
        val recorder = MediaRecorder(appContext)
        run.recorder = recorder
        try {
            if (video.mic) recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setOutputFile(output.descriptor.fileDescriptor)
            // ~5 Mbps per megapixel keeps 1080p near 10 Mbps without a settings surface yet.
            recorder.setVideoEncodingBitRate((video.video.area / 1_000_000 * 5L).toInt().coerceIn(4_000_000, 24_000_000))
            recorder.setVideoFrameRate(30)
            recorder.setVideoSize(video.video.width, video.video.height)
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (video.mic) {
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioSamplingRate(44_100)
                recorder.setAudioEncodingBitRate(96_000)
            }
            recorder.setOrientationHint(CameraPolicy.jpegOrientation(
                run.source.camera.sensorOrientation, run.source.displayDegrees,
                run.source.camera.facing == LensFacing.FRONT))
            recorder.prepare()
            run.recorderSurface = recorder.surface
        } catch (error: Exception) {
            run.recorder = null
            recorder.release()
            val uri = run.videoUri; val fd = run.videoFd
            run.videoUri = null; run.videoFd = null
            if (uri != null && fd != null) ioScope.launch { runCatching { store.discardVideo(uri, fd) } }
            throw error
        }
    }

    private fun configure(run: Run) {
        try {
            val streams = when (run.source.plan) {
                is StreamPlan.Photo -> if (run.rawReader != null) "JPEG + RAW + preview" else "JPEG + preview"
                is StreamPlan.Video -> "preview + MediaRecorder"
            }
            _runtime.value = _runtime.value.copy(phase = CameraPhase.CONFIGURING, message = "Configuring $streams")
            val outputs = mutableListOf(
                OutputConfiguration(run.source.surface).apply { setMirrorMode(OutputConfiguration.MIRROR_MODE_AUTO) },
            )
            val maximumResolution = (run.source.plan as? StreamPlan.Photo)?.photo?.sensorPixelMode ==
                SensorPixelMode.MAXIMUM_RESOLUTION
            if (run.reader != null) outputs += OutputConfiguration(run.reader.surface).apply {
                if (maximumResolution) addSensorPixelModeUsed(CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
            }
            if (run.rawReader != null) outputs += OutputConfiguration(run.rawReader.surface).apply {
                if (maximumResolution) addSensorPixelModeUsed(CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
            }
            run.recorderSurface?.let { outputs += OutputConfiguration(it) }
            val config = SessionConfiguration(SessionConfiguration.SESSION_REGULAR, outputs, executor,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (active !== run || run.closing || disposed) { session.close(); return }
                        run.session = session
                        try {
                            val template = if (run.source.plan is StreamPlan.Video) {
                                CameraDevice.TEMPLATE_RECORD
                            } else CameraDevice.TEMPLATE_PREVIEW
                            session.setRepeatingRequest(request(run, template).apply {
                                addTarget(run.source.surface)
                            }.build(), telemetry(run), handler)
                            _runtime.value = _runtime.value.copy(phase = CameraPhase.PREVIEWING,
                                message = when {
                                    run.source.plan is StreamPlan.Video -> "Video · ready to record"
                                    run.rawReader != null -> "Photo · JPEG + RAW"
                                    else -> "Photo · JPEG"
                                })
                            logger.log(LogLevel.INFO, "Session", "Preview configured: epoch=${run.epoch}")
                        } catch (error: Exception) { fail(run, "Cannot start camera preview", error) }
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        session.close()
                        if (!run.closing && active === run) fail(run, "$streams configuration rejected by camera")
                    }
                })
            // Some HALs don't implement this query; successful configuration is still required.
            // With RAW this probe is the honest gate for the 3-stream combination.
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
        if (template == CameraDevice.TEMPLATE_STILL_CAPTURE &&
            (run.source.plan as? StreamPlan.Photo)?.photo?.sensorPixelMode == SensorPixelMode.MAXIMUM_RESOLUTION) {
            set(CaptureRequest.SENSOR_PIXEL_MODE, CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
        }
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
        if (run.closing || _runtime.value.phase !in CONTROL_PHASES || !run.source.camera.supportsZoomRatio) return@post
        try {
            val target = CameraPolicy.validatedZoom(value, run.source.camera)
            if (run.source.plan is StreamPlan.Video) rampVideoZoom(run, target) else applyZoom(run, target)
        } catch (error: Exception) {
            fail(run, "Zoom request rejected", error)
        }
    }

    private fun rampVideoZoom(run: Run, target: Float) {
        run.zoomStep?.let { handler.removeCallbacks(it) }
        val start = _runtime.value.zoom.coerceAtLeast(run.source.camera.minZoom)
        if (abs(target - start) < 0.001f) return
        val durationMs = (150.0 + abs(ln(target.toDouble() / start.toDouble())) * 180.0)
            .toLong().coerceIn(150L, 500L)
        val startedAt = SystemClock.elapsedRealtime()
        lateinit var step: Runnable
        step = Runnable {
            if (active !== run || run.closing || _runtime.value.phase !in CONTROL_PHASES) {
                run.zoomStep = null
                return@Runnable
            }
            val progress = ((SystemClock.elapsedRealtime() - startedAt).toFloat() / durationMs).coerceIn(0f, 1f)
            val zoom = CameraPolicy.interpolatedZoom(start, target, progress)
            try {
                applyZoom(run, zoom, logControl = progress >= 1f)
                if (progress < 1f) handler.postDelayed(step, 33L) else run.zoomStep = null
            } catch (error: Exception) {
                run.zoomStep = null
                fail(run, "Smooth zoom request rejected", error)
            }
        }
        run.zoomStep = step
        handler.post(step)
    }

    private fun applyZoom(run: Run, value: Float, logControl: Boolean = true) {
        val previous = _runtime.value
        try {
            _runtime.value = previous.copy(zoom = CameraPolicy.validatedZoom(value, run.source.camera))
            updateRepeating(run, logControl)
        } catch (error: Exception) {
            _runtime.value = previous
            throw error
        }
    }
    fun toggleTorch() = post {
        val run = active ?: return@post
        if (run.closing || _runtime.value.phase !in CONTROL_PHASES || !run.source.camera.supportsTorch) return@post
        val previous = _runtime.value
        try {
            _runtime.value = previous.copy(torch = !previous.torch)
            updateRepeating(run)
        } catch (error: Exception) { _runtime.value = previous; fail(run, "Torch request rejected", error) }
    }
    private fun updateRepeating(run: Run, logControl: Boolean = true) {
        val template = if (run.source.plan is StreamPlan.Video) CameraDevice.TEMPLATE_RECORD else CameraDevice.TEMPLATE_PREVIEW
        val builder = request(run, template).apply { addTarget(run.source.surface) }
        // While recording, the recorder surface must keep receiving frames.
        if (run.recording) builder.addTarget(requireNotNull(run.recorderSurface))
        run.session!!.setRepeatingRequest(builder.build(), telemetry(run), handler)
        if (logControl) logger.log(LogLevel.INFO, "Controls", "ID ${run.source.camera.id}: zoom=${_runtime.value.zoom}, torch=${_runtime.value.torch}")
    }

    fun capture(displayDegrees: Int) = post {
        val run = active ?: return@post
        if (_runtime.value.phase != CameraPhase.PREVIEWING || run.closing || run.captureId != null) return@post
        if (run.source.plan !is StreamPlan.Photo) return@post
        val id = ++captureSequence
        run.captureId = id
        run.savedJpeg = false
        run.savedDng = false
        run.jpegError = null
        run.rawError = null
        run.rawResult = null
        _runtime.value = _runtime.value.copy(phase = CameraPhase.CAPTURING, message = "Capturing and saving…")
        try {
            val orientation = CameraPolicy.jpegOrientation(run.source.camera.sensorOrientation, displayDegrees,
                run.source.camera.facing == LensFacing.FRONT)
            // JPEG_ORIENTATION is degrees; the DNG orientation tag takes EXIF 1–8 values.
            run.exif = when (orientation) { 90 -> 6; 180 -> 3; 270 -> 8; else -> 1 }
            val builder = request(run, CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(requireNotNull(run.reader).surface)
                if (run.rawReader != null) addTarget(run.rawReader.surface)
                set(CaptureRequest.JPEG_ORIENTATION, orientation)
                set(CaptureRequest.JPEG_QUALITY, preferences.jpegQuality.toByte())
            }
            rearmTimeout(run, id)
            run.session!!.capture(builder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    logger.log(LogLevel.INFO, "Capture", "JPEG $id result: epoch=${run.epoch}, orientation=$orientation, ISO=${result[CaptureResult.SENSOR_SENSITIVITY]}, exposureNs=${result[CaptureResult.SENSOR_EXPOSURE_TIME]}")
                    if (run.rawReader != null) {
                        run.rawResult = result
                        tryProcessRaw(run)
                    }
                }
                override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
                    if (active === run && !run.closing) fail(run, "Photo $id failed: reason=${failure.reason}")
                }
            }, handler)
        } catch (error: Exception) { fail(run, "Cannot capture photo $id", error) }
    }

    // The watchdog fires only while no save job is in flight; each settled output re-arms it
    // for the remaining output, so a slow DNG never gets a healthy JPEG killed early.
    private fun rearmTimeout(run: Run, id: Long) {
        run.timeout?.let { handler.removeCallbacks(it) }
        run.timeout = Runnable {
            if (active === run && !run.closing && run.captureId == id && !run.pendingJpeg && !run.pendingRaw) {
                fail(run, "Timed out waiting for capture $id")
            }
        }.also { handler.postDelayed(it, 12_000) }
    }

    fun startRecording() = post {
        val run = active ?: return@post
        if (run.source.plan !is StreamPlan.Video || _runtime.value.phase != CameraPhase.PREVIEWING ||
            run.closing || run.recording || run.stopping) return@post
        val recorder = run.recorder
        if (recorder == null) { fail(run, "Recorder is not prepared"); return@post }
        try {
            run.recording = true
            _runtime.value = _runtime.value.copy(phase = CameraPhase.RECORDING, message = "Recording · tap to stop")
            // The recorder surface must be an active capture target for frames to reach it.
            val builder = request(run, CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(run.source.surface)
                addTarget(requireNotNull(run.recorderSurface))
            }
            run.session!!.setRepeatingRequest(builder.build(), telemetry(run), handler)
            recorder.start()
            logger.log(LogLevel.INFO, "Record", "Recording started: epoch=${run.epoch}, mic=${run.source.plan.video.mic}")
        } catch (error: Exception) {
            run.recording = false
            fail(run, "Cannot start recording", error)
        }
    }

    fun stopRecording() = post {
        val run = active ?: return@post
        if (run.source.plan !is StreamPlan.Video || !run.recording || run.stopping) return@post
        run.stopping = true
        _runtime.value = _runtime.value.copy(phase = CameraPhase.CAPTURING, message = "Finalizing recording…")
        finalizeRecording(run, publish = true, reportToUser = true)
    }

    // Handler thread. Stops and releases the recorder exactly once, then either publishes
    // the pending MediaStore entry or discards it. Used by both user stop and camera close.
    private fun finalizeRecording(run: Run, publish: Boolean, reportToUser: Boolean = false) {
        val recorder = run.recorder ?: return
        run.recorder = null
        run.recording = false
        val uri = run.videoUri
        val fd = run.videoFd
        run.videoUri = null
        run.videoFd = null
        run.recorderSurface = null
        val stopped = try { recorder.stop(); true } catch (error: Exception) {
            if (reportToUser || publish) {
                logger.log(LogLevel.WARNING, "Record", "Recording produced no usable video", error)
            }
            false
        }
        recorder.release()
        // Give the preview back its repeating request when the session is still alive.
        if (!run.closing && active === run && run.session != null && _runtime.value.phase == CameraPhase.CAPTURING) {
            try { updateRepeating(run) } catch (error: Exception) { fail(run, "Cannot restart camera preview", error) }
        }
        if (uri == null || fd == null) { run.stopping = false; quitIfSafe(); return }
        if (publish && stopped) {
            ioScope.launch {
                val outcome = runCatching { store.publishVideo(uri, fd) }
                handler.post {
                    run.stopping = false
                    outcome.onSuccess {
                        _lastPhoto.value = uri
                        logger.log(LogLevel.INFO, "Storage", "Recording saved to DCIM/Camera3")
                    }.onFailure { error ->
                        logger.log(LogLevel.ERROR, "Storage", "Recording could not be published", error)
                        ioScope.launch { runCatching { store.discardVideo(uri, fd) } }
                    }
                    if (active === run && !run.closing) _runtime.value = _runtime.value.copy(
                        phase = CameraPhase.PREVIEWING,
                        message = if (outcome.isSuccess) "Recording saved to DCIM/Camera3"
                        else "Recording was not saved: ${outcome.exceptionOrNull()?.message}")
                    quitIfSafe()
                }
            }
        } else {
            ioScope.launch { runCatching { store.discardVideo(uri, fd) } }
            if (reportToUser && active === run && !run.closing) _runtime.value = _runtime.value.copy(
                phase = CameraPhase.PREVIEWING, message = "Recording too short to save")
            run.stopping = false
            quitIfSafe()
        }
    }

    private fun receiveImage(run: Run) {
        val reader = run.reader ?: return
        val image = try { reader.acquireNextImage() } catch (error: Exception) {
            if (!run.closing) fail(run, "Cannot acquire JPEG", error)
            return
        } ?: return
        val id = run.captureId
        if (id == null || run.closing || run.pendingJpeg) { image.close(); return }
        run.pendingJpeg = true
        imageJobs++
        ioScope.launch(Dispatchers.IO) {
            val outcome = runCatching {
                val bytes = try {
                    val buffer = image.planes[0].buffer
                    ByteArray(buffer.remaining()).also { buffer.get(it) }
                } finally { image.close() }
                store.saveJpeg(bytes)
            }
            handler.post {
                run.pendingJpeg = false
                run.savedJpeg = outcome.isSuccess
                outcome.onSuccess { uri ->
                    _lastPhoto.value = uri
                    logger.log(LogLevel.INFO, "Storage", "JPEG $id saved to DCIM/Camera3")
                }.onFailure { error ->
                    run.jpegError = error.message ?: "JPEG save failed"
                    logger.log(LogLevel.ERROR, "Storage", "JPEG $id could not be saved", error)
                }
                finishCapture(run, id)
            }
        }
    }

    private fun receiveRaw(run: Run) {
        val reader = run.rawReader ?: return
        val image = try { reader.acquireNextImage() } catch (error: Exception) {
            if (!run.closing) fail(run, "Cannot acquire RAW frame", error)
            return
        } ?: return
        if (run.closing || run.captureId == null || run.pendingRaw || run.rawImage != null) { image.close(); return }
        run.rawImage = image
        tryProcessRaw(run)
    }

    // Handler thread. DngCreator needs the capture result and the RAW image; they can arrive in
    // either order, so the frame is held until both are present.
    private fun tryProcessRaw(run: Run) {
        if (run.closing) { run.rawImage?.let { image -> run.rawImage = null; runCatching { image.close() } }; return }
        val result = run.rawResult ?: return
        val image = run.rawImage ?: return
        val id = run.captureId
        if (id == null) { run.rawImage = null; runCatching { image.close() }; return }
        run.rawImage = null
        run.pendingRaw = true
        imageJobs++
        ioScope.launch(Dispatchers.IO) {
            val outcome = runCatching {
                // DngCreator takes its metadata at construction: characteristics + this capture's result.
                DngCreator(manager.getCameraCharacteristics(run.source.camera.id), result).use { creator ->
                    creator.setOrientation(run.exif)
                    store.saveDng { output -> creator.writeImage(output, image) }
                }
            }
            runCatching { image.close() }
            handler.post {
                run.pendingRaw = false
                run.savedDng = outcome.isSuccess
                outcome.onSuccess { uri -> _lastPhoto.value = uri }
                outcome.onSuccess { logger.log(LogLevel.INFO, "Storage", "DNG $id saved to DCIM/Camera3") }
                outcome.onFailure { error ->
                    run.rawError = error.message ?: "DNG write failed"
                    logger.log(LogLevel.ERROR, "Storage", "DNG $id could not be written", error)
                }
                finishCapture(run, id)
            }
        }
    }

    // Handler thread. Called once per settled output; the capture closes when every expected
    // output has settled, and each partial settle re-arms the watchdog for the remainder.
    private fun finishCapture(run: Run, id: Long) {
        imageJobs--
        if (run.captureId == id) {
            if (run.pendingJpeg || run.pendingRaw) rearmTimeout(run, id)
            else {
                run.captureId = null
                run.timeout?.let { handler.removeCallbacks(it) }
                run.timeout = null
                // A RAW frame that never got its result must not stay held forever.
                run.rawImage?.let { image -> run.rawImage = null; runCatching { image.close() } }
                val message = if (run.rawReader != null) when {
                    run.savedJpeg && run.savedDng -> "JPEG + DNG saved to DCIM/Camera3"
                    run.savedJpeg -> "JPEG saved, DNG failed: ${run.rawError ?: "RAW output never completed"}"
                    run.savedDng -> "DNG saved, JPEG failed: ${run.jpegError ?: "JPEG output never completed"}"
                    else -> "Capture not saved: ${run.jpegError ?: run.rawError ?: "no output completed"}"
                } else if (run.savedJpeg) "Photo saved to DCIM/Camera3"
                else "Photo was not saved: ${run.jpegError ?: "JPEG output never completed"}"
                if (active === run && !run.closing) _runtime.value = _runtime.value.copy(
                    phase = CameraPhase.PREVIEWING, message = message)
            }
        }
        releaseReaderIfSafe(run)
        quitIfSafe()
    }

    private fun close(run: Run) {
        run.closing = true
        run.timeout?.let { handler.removeCallbacks(it) }
        run.zoomStep?.let { handler.removeCallbacks(it) }
        run.zoomStep = null
        if (!blocked) _runtime.value = _runtime.value.copy(phase = CameraPhase.CLOSING, message = "Closing camera")
        // A recording still in flight is stopped and published best-effort; an unstarted
        // pending entry is discarded.
        finalizeRecording(run, publish = run.recording)
        // reconcile() may call close() repeatedly while a run is shutting down; session teardown
        // must be idempotent so a closed session is never probed twice (duplicate ISE logs).
        run.session?.let { session ->
            run.session = null
            try { session.stopRepeating() } catch (error: Exception) { logger.log(LogLevel.DEBUG, "Close", "Repeating request already stopped", error) }
            session.close()
        }
        run.device?.close() // If still opening, onOpened will close it before configuring.
    }
    private fun releaseReaderIfSafe(run: Run) {
        if (!run.deviceClosed || run.readersClosed) return
        if (run.pendingJpeg || run.pendingRaw) return // a save job still needs its reader
        run.readersClosed = true
        run.rawImage?.let { image -> run.rawImage = null; runCatching { image.close() } }
        run.rawReader?.close()
        run.reader?.close()
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
