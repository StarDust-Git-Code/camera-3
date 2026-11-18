package dev.camera3.model

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

object CameraPolicy {
    fun isEligible(cameras: List<CameraDescriptor>) = cameras.any { it.level == HardwareLevel.LEVEL_3 }

    fun initialCamera(
        cameras: List<CameraDescriptor>,
        rawEnabled: Boolean = false,
        highResolution: Boolean = false,
    ): CameraDescriptor? = cameras
        .filter { it.level == HardwareLevel.LEVEL_3 && configuration(it, rawEnabled, highResolution) != null }
        .sortedWith(compareBy<CameraDescriptor> { it.facing != LensFacing.BACK }.thenBy { it.id })
        .firstOrNull()

    // Start with a modest real JPEG stream, not the sensor's maximum-resolution mode.
    // Advertised combinations still require a successful Camera2 session.
    // A RAW stream is only paired when the lens advertises RAW and one of its reported
    // sizes matches the JPEG aspect; an incompatible pairing is never invented.
    fun configuration(
        camera: CameraDescriptor,
        rawEnabled: Boolean = false,
        highResolution: Boolean = false,
    ): PhotoConfiguration? {
        if (highResolution && !camera.supportsHighResolutionPhoto) return null
        val pixelMode = if (highResolution) SensorPixelMode.MAXIMUM_RESOLUTION else SensorPixelMode.DEFAULT
        val jpegs = (if (highResolution) camera.maximumResolutionJpegSizes else camera.jpegSizes).sortedWith(
            if (highResolution) compareBy<Dimensions> { abs(it.aspect - 4.0 / 3) > 0.03 }
                .thenByDescending { it.area }
            else compareBy<Dimensions> { abs(it.aspect - 4.0 / 3) > 0.03 }
                .thenBy { it.area > 12_500_000L }
                .thenByDescending { it.area },
        )
        val rawWanted = rawEnabled && if (highResolution) camera.supportsHighResolutionRaw else camera.supportsRaw
        if (rawEnabled && !rawWanted) return null
        val rawSizes = if (highResolution) camera.maximumResolutionRawSizes else camera.rawSizes
        for (jpeg in jpegs) {
            val raw = if (rawWanted) rawSizes
                .filter { abs(it.aspect - jpeg.aspect) <= 0.03 }
                .maxByOrNull { it.area }
            else null
            if (rawWanted && raw == null) continue
            val candidates = camera.previewSizes.filter { abs(it.aspect - jpeg.aspect) < 0.015 }
            val preview = candidates.filter { it.width <= 1920 && it.height <= 1080 }
                .maxByOrNull { it.area } ?: candidates.minByOrNull { it.area }
            if (preview != null) return PhotoConfiguration(jpeg, preview, raw, pixelMode)
        }
        return null
    }

    fun relativeRotation(sensorOrientation: Int, displayDegrees: Int, front: Boolean): Int =
        (sensorOrientation - displayDegrees * (if (front) 1 else -1) + 360) % 360

    // Bounded 16:9 video baseline: the largest MediaRecorder size at or under 1080p whose
    // aspect matches an available preview stream; without a matching preview the mode is
    // unavailable rather than shown with mismatched framing.
    fun videoConfiguration(camera: CameraDescriptor, mic: Boolean): VideoConfiguration? {
        val videos = camera.videoSizes
            .filter { abs(it.aspect - 16.0 / 9) <= 0.03 && it.width <= 1920 && it.height <= 1080 }
            .sortedByDescending { it.area }
        for (video in videos) {
            val preview = camera.previewSizes
                .filter { abs(it.aspect - video.aspect) < 0.015 }
                .filter { it.width <= 1920 && it.height <= 1080 }
                .maxByOrNull { it.area } ?: continue
            return VideoConfiguration(video, preview, mic)
        }
        return null
    }

    // Display#getRotation is counterclockwise. JPEG_ORIENTATION uses clockwise device orientation.
    fun jpegOrientation(sensorOrientation: Int, displayDegrees: Int, front: Boolean): Int =
        (sensorOrientation + (if (front) displayDegrees else -displayDegrees) + 360) % 360

    fun validatedZoom(value: Float, camera: CameraDescriptor): Float {
        require(value.isFinite())
        return value.coerceIn(camera.minZoom, camera.maxZoom)
    }

    /** Perceptually even video zoom: smoothstep timing in logarithmic ratio space. */
    fun interpolatedZoom(start: Float, target: Float, progress: Float): Float {
        require(start.isFinite() && target.isFinite() && start > 0f && target > 0f)
        val t = progress.coerceIn(0f, 1f)
        val eased = t * t * (3f - 2f * t)
        return exp(ln(start.toDouble()) + (ln(target.toDouble()) - ln(start.toDouble())) * eased).toFloat()
    }
}
