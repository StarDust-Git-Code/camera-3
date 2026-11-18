package dev.camera3.model

import kotlin.math.abs

object CameraPolicy {
    fun isEligible(cameras: List<CameraDescriptor>) = cameras.any { it.level == HardwareLevel.LEVEL_3 }

    fun initialCamera(cameras: List<CameraDescriptor>): CameraDescriptor? = cameras
        .filter { it.level == HardwareLevel.LEVEL_3 && configuration(it) != null }
        .sortedWith(compareBy<CameraDescriptor> { it.facing != LensFacing.BACK }.thenBy { it.id })
        .firstOrNull()

    // Start with a modest real JPEG stream, not the sensor's maximum-resolution mode.
    // Advertised combinations still require a successful Camera2 session.
    // A RAW stream is only paired when the lens advertises RAW and one of its reported
    // sizes matches the JPEG aspect; an incompatible pairing is never invented.
    fun configuration(camera: CameraDescriptor, rawEnabled: Boolean = false): PhotoConfiguration? {
        val jpegs = camera.jpegSizes.sortedWith(
            compareBy<Dimensions> { abs(it.aspect - 4.0 / 3) > 0.03 }
                .thenBy { it.area > 12_500_000L }
                .thenByDescending { it.area },
        )
        val rawWanted = rawEnabled && camera.supportsRaw
        for (jpeg in jpegs) {
            val raw = if (rawWanted) camera.rawSizes
                .filter { abs(it.aspect - jpeg.aspect) <= 0.03 }
                .maxByOrNull { it.area }
            else null
            if (rawWanted && raw == null) continue
            val candidates = camera.previewSizes.filter { abs(it.aspect - jpeg.aspect) < 0.015 }
            val preview = candidates.filter { it.width <= 1920 && it.height <= 1080 }
                .maxByOrNull { it.area } ?: candidates.minByOrNull { it.area }
            if (preview != null) return PhotoConfiguration(jpeg, preview, raw)
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
}
