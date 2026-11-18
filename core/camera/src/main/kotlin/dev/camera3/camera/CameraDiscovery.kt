package dev.camera3.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics as CC
import android.hardware.camera2.CameraManager
import android.view.SurfaceHolder
import dev.camera3.diagnostics.AppLogger
import dev.camera3.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CameraDiscovery(context: Context, private val logger: AppLogger) {
    private val manager = context.applicationContext.getSystemService(CameraManager::class.java)

    suspend fun discover(): List<CameraDescriptor> = withContext(Dispatchers.IO) {
        manager.cameraIdList.mapNotNull { id ->
            try {
                val c = manager.getCameraCharacteristics(id)
                val map = c[CC.SCALER_STREAM_CONFIGURATION_MAP]
                val caps = c[CC.REQUEST_AVAILABLE_CAPABILITIES]?.toSet().orEmpty()
                val zoom = c[CC.CONTROL_ZOOM_RATIO_RANGE]
                val keys = c.availableCaptureRequestKeys.map { it.name }.toSet()
                val descriptor = CameraDescriptor(
                    id = id,
                    facing = when (c[CC.LENS_FACING]) {
                        CC.LENS_FACING_BACK -> LensFacing.BACK
                        CC.LENS_FACING_FRONT -> LensFacing.FRONT
                        CC.LENS_FACING_EXTERNAL -> LensFacing.EXTERNAL
                        else -> LensFacing.UNKNOWN
                    },
                    level = when (c[CC.INFO_SUPPORTED_HARDWARE_LEVEL]) {
                        CC.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> HardwareLevel.LEVEL_3
                        CC.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> HardwareLevel.FULL
                        CC.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> HardwareLevel.LIMITED
                        CC.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> HardwareLevel.LEGACY
                        CC.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> HardwareLevel.EXTERNAL
                        else -> HardwareLevel.UNKNOWN
                    },
                    sensorOrientation = c[CC.SENSOR_ORIENTATION] ?: 0,
                    jpegSizes = map?.getOutputSizes(ImageFormat.JPEG)?.map { Dimensions(it.width, it.height) }.orEmpty(),
                    previewSizes = map?.getOutputSizes(SurfaceHolder::class.java)?.map { Dimensions(it.width, it.height) }.orEmpty(),
                    physicalIds = c.physicalCameraIds,
                    capabilities = caps.map { capabilityName(it) }.toSet(),
                    requestKeys = keys,
                    resultKeys = c.availableCaptureResultKeys.map { it.name }.toSet(),
                    afModes = c[CC.CONTROL_AF_AVAILABLE_MODES]?.toSet().orEmpty(),
                    flashAvailable = c[CC.FLASH_INFO_AVAILABLE] == true,
                    minZoom = zoom?.lower ?: 1f,
                    maxZoom = zoom?.upper ?: 1f,
                    supportsZoomRatio = zoom != null && "android.control.zoomRatio" in keys,
                    extensionModes = try { manager.getCameraExtensionCharacteristics(id).supportedExtensions.toSet() }
                        catch (error: Exception) { logger.log(LogLevel.WARNING, "Discovery", "Extension query failed for ID $id", error); emptySet() },
                    details = linkedMapOf(
                        "Sensor pixels" to (c[CC.SENSOR_INFO_PIXEL_ARRAY_SIZE]?.toString() ?: "Not reported"),
                        "Sensor size (mm)" to (c[CC.SENSOR_INFO_PHYSICAL_SIZE]?.toString() ?: "Not reported"),
                        "Focal lengths (mm)" to (c[CC.LENS_INFO_AVAILABLE_FOCAL_LENGTHS]?.joinToString() ?: "Not reported"),
                        "ISO range" to (c[CC.SENSOR_INFO_SENSITIVITY_RANGE]?.toString() ?: "Not reported"),
                        "Exposure time (ns)" to (c[CC.SENSOR_INFO_EXPOSURE_TIME_RANGE]?.toString() ?: "Not reported"),
                        "Minimum focus distance" to (c[CC.LENS_INFO_MINIMUM_FOCUS_DISTANCE]?.toString() ?: "Not reported"),
                        "AE FPS ranges" to (c[CC.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]?.joinToString() ?: "Not reported"),
                        "OIS modes" to (c[CC.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION]?.joinToString() ?: "Not reported"),
                        "Video stabilization modes" to (c[CC.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES]?.joinToString() ?: "Not reported"),
                        "Dynamic range profile IDs" to (c[CC.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES]?.supportedProfiles?.joinToString() ?: "Not reported"),
                        "High-speed sizes" to (map?.highSpeedVideoSizes?.joinToString() ?: "Not reported"),
                    ),
                )
                logger.log(LogLevel.INFO, "Discovery", "ID $id: ${descriptor.facing}, ${descriptor.level}, JPEG sizes=${descriptor.jpegSizes.size}, capabilities=${descriptor.capabilities}")
                descriptor
            } catch (error: Exception) {
                logger.log(LogLevel.ERROR, "Discovery", "Cannot inspect camera ID $id", error)
                null
            }
        }
    }

    private fun capabilityName(value: Int): String = when (value) {
        CC.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "BACKWARD_COMPATIBLE"
        CC.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "MANUAL_SENSOR"
        CC.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> "MANUAL_POST_PROCESSING"
        CC.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW"
        CC.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "BURST_CAPTURE"
        CC.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> "PRIVATE_REPROCESSING"
        CC.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV_REPROCESSING"
        CC.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "DEPTH_OUTPUT"
        CC.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> "HIGH_SPEED_VIDEO"
        CC.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> "LOGICAL_MULTI_CAMERA"
        CC.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT -> "TEN_BIT_DYNAMIC_RANGE"
        else -> "CAPABILITY_$value"
    }
}
