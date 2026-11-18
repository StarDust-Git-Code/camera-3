package dev.camera3.model

enum class HardwareLevel { LEVEL_3, FULL, LIMITED, LEGACY, EXTERNAL, UNKNOWN }
enum class LensFacing { BACK, FRONT, EXTERNAL, UNKNOWN }
enum class CameraPhase { IDLE, OPENING, CONFIGURING, PREVIEWING, CAPTURING, RECORDING, CLOSING, ERROR }
enum class LogLevel { DEBUG, INFO, WARNING, ERROR }
enum class CameraMode { PHOTO, VIDEO }

data class Dimensions(val width: Int, val height: Int) {
    init { require(width > 0 && height > 0) }
    val area: Long get() = width.toLong() * height
    val aspect: Double get() = width.toDouble() / height
    override fun toString() = "${width}×${height}"
}

data class CameraDescriptor(
    val id: String,
    val facing: LensFacing,
    val level: HardwareLevel,
    val sensorOrientation: Int,
    val jpegSizes: List<Dimensions>,
    val previewSizes: List<Dimensions>,
    val rawSizes: List<Dimensions> = emptyList(),
    val videoSizes: List<Dimensions> = emptyList(),
    val physicalIds: Set<String> = emptySet(),
    val capabilities: Set<String> = emptySet(),
    val requestKeys: Set<String> = emptySet(),
    val resultKeys: Set<String> = emptySet(),
    val afModes: Set<Int> = emptySet(),
    val flashAvailable: Boolean = false,
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val supportsZoomRatio: Boolean = false,
    val extensionModes: Set<Int> = emptySet(),
    val details: Map<String, String> = emptyMap(),
) {
    val label: String get() = "${facing.name.lowercase().replaceFirstChar { it.uppercase() }} · ID $id"
    val supportsTorch: Boolean get() = flashAvailable && "android.flash.mode" in requestKeys
    // A RAW stream is only offered when the lens both advertises the capability and reports real output sizes.
    val supportsRaw: Boolean get() = "RAW" in capabilities && rawSizes.isNotEmpty()
}

data class PhotoConfiguration(val jpeg: Dimensions, val preview: Dimensions, val raw: Dimensions? = null)
data class VideoConfiguration(val video: Dimensions, val preview: Dimensions, val mic: Boolean)
data class PhotoPreferences(val grid: Boolean = false, val jpegQuality: Int = 95, val raw: Boolean = false) {
    init { require(jpegQuality in 50..100) }
}
data class CameraRuntime(
    val phase: CameraPhase = CameraPhase.IDLE,
    val mode: CameraMode = CameraMode.PHOTO,
    val cameraId: String? = null,
    val configuration: PhotoConfiguration? = null,
    val message: String = "Camera is idle",
    val zoom: Float = 1f,
    val torch: Boolean = false,
    val effectiveIso: Int? = null,
    val effectiveExposureNs: Long? = null,
)
data class LogEvent(
    val sequence: Long,
    val timeMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
)
