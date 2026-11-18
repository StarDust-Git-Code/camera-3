package dev.camera3.camera

import android.content.Context
import android.graphics.ImageFormat
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaRecorder
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
        val avc1080p60 = supportsAvc1080p60()
        manager.cameraIdList.mapNotNull { id ->
            try {
                val c = manager.getCameraCharacteristics(id)
                val map = c[CC.SCALER_STREAM_CONFIGURATION_MAP]
                val maximumResolutionMap = c[CC.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION]
                val caps = c[CC.REQUEST_AVAILABLE_CAPABILITIES]?.toSet().orEmpty()
                val zoom = c[CC.CONTROL_ZOOM_RATIO_RANGE]
                val keys = c.availableCaptureRequestKeys.map { it.name }.toSet()
                val rawSizes = try { map?.getOutputSizes(ImageFormat.RAW_SENSOR)?.map { Dimensions(it.width, it.height) }.orEmpty() }
                    catch (error: Exception) {
                        logger.log(LogLevel.WARNING, "Discovery", "RAW size query failed for ID $id", error)
                        emptyList()
                    }
                val platformVideoSizes = try { map?.getOutputSizes(MediaRecorder::class.java).orEmpty().toList() }
                    catch (error: Exception) {
                        logger.log(LogLevel.WARNING, "Discovery", "MediaRecorder size query failed for ID $id", error)
                        emptyList()
                    }
                val videoSizes = platformVideoSizes.map { Dimensions(it.width, it.height) }
                val videoMinFrameDurations = platformVideoSizes.associate { size ->
                    Dimensions(size.width, size.height) to try {
                        map?.getOutputMinFrameDuration(MediaRecorder::class.java, size) ?: Long.MAX_VALUE
                    } catch (error: Exception) {
                        logger.log(LogLevel.WARNING, "Discovery", "MediaRecorder frame-duration query failed for ID $id at ${size.width}×${size.height}", error)
                        Long.MAX_VALUE
                    }
                }
                val highSpeedSizes = try { map?.highSpeedVideoSizes?.toSet().orEmpty() }
                    catch (error: Exception) {
                        logger.log(LogLevel.WARNING, "Discovery", "High-speed size query failed for ID $id", error)
                        emptySet()
                    }
                val highSpeedRanges = platformVideoSizes.filter { it in highSpeedSizes }.associate { size ->
                    Dimensions(size.width, size.height) to try {
                        map?.getHighSpeedVideoFpsRangesFor(size)
                            ?.mapNotNull { range ->
                                range.takeIf { it.lower > 0 && it.upper >= it.lower }
                                    ?.let { FrameRateRange(it.lower, it.upper) }
                            }.orEmpty()
                    } catch (error: Exception) {
                        logger.log(LogLevel.WARNING, "Discovery", "High-speed FPS query failed for ID $id at ${size.width}×${size.height}", error)
                        emptyList()
                    }
                }
                val maximumResolutionJpegSizes = try {
                    maximumResolutionMap?.getOutputSizes(ImageFormat.JPEG)
                        ?.map { Dimensions(it.width, it.height) }.orEmpty()
                } catch (error: Exception) {
                    logger.log(LogLevel.WARNING, "Discovery", "Maximum-resolution JPEG size query failed for ID $id", error)
                    emptyList()
                }
                val maximumResolutionRawSizes = try {
                    maximumResolutionMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
                        ?.map { Dimensions(it.width, it.height) }.orEmpty()
                } catch (error: Exception) {
                    logger.log(LogLevel.WARNING, "Discovery", "Maximum-resolution RAW size query failed for ID $id", error)
                    emptyList()
                }
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
                    rawSizes = rawSizes,
                    videoSizes = videoSizes,
                    aeFrameRateRanges = c[CC.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]
                        ?.mapNotNull { range ->
                            range.takeIf { it.lower > 0 && it.upper >= it.lower }
                                ?.let { FrameRateRange(it.lower, it.upper) }
                        }.orEmpty(),
                    videoMinFrameDurationNs = videoMinFrameDurations,
                    highSpeedVideoFrameRateRanges = highSpeedRanges,
                    supportsAvc1080p60 = avc1080p60,
                    maximumResolutionJpegSizes = maximumResolutionJpegSizes,
                    maximumResolutionRawSizes = maximumResolutionRawSizes,
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
                        "Maximum-resolution sensor pixels" to
                            (c[CC.SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION]?.toString() ?: "Not reported"),
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
                        "RAW output sizes" to (rawSizes.takeIf { it.isNotEmpty() }?.joinToString() ?: "Not reported"),
                        "Maximum-resolution JPEG sizes" to
                            (maximumResolutionJpegSizes.takeIf { it.isNotEmpty() }?.joinToString() ?: "Not reported"),
                        "Maximum-resolution RAW sizes" to
                            (maximumResolutionRawSizes.takeIf { it.isNotEmpty() }?.joinToString() ?: "Not reported"),
                        "MediaRecorder sizes" to (videoSizes.takeIf { it.isNotEmpty() }?.joinToString() ?: "Not reported"),
                        "AVC encoder 1920×1080 @ 60 fps" to avc1080p60.toString(),
                        "MediaRecorder minimum frame durations (ns)" to
                            (videoMinFrameDurations.takeIf { it.isNotEmpty() }?.entries?.joinToString() ?: "Not reported"),
                        "High-speed FPS ranges by size" to
                            (highSpeedRanges.takeIf { it.isNotEmpty() }?.entries?.joinToString() ?: "Not reported"),
                    ),
                )
                logger.log(LogLevel.INFO, "Discovery", "ID $id: ${descriptor.facing}, ${descriptor.level}, JPEG sizes=${descriptor.jpegSizes.size}, RAW sizes=${descriptor.rawSizes.size}, maximum JPEG sizes=${descriptor.maximumResolutionJpegSizes.size}, maximum RAW sizes=${descriptor.maximumResolutionRawSizes.size}, video sizes=${descriptor.videoSizes.size}, capabilities=${descriptor.capabilities}")
                descriptor
            } catch (error: Exception) {
                logger.log(LogLevel.ERROR, "Discovery", "Cannot inspect camera ID $id", error)
                null
            }
        }
    }

    private fun supportsAvc1080p60(): Boolean = try {
        MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.any { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) } &&
                runCatching {
                    info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
                        .videoCapabilities?.areSizeAndRateSupported(1920, 1080, 60.0) == true
                }.getOrDefault(false)
        }
    } catch (error: Exception) {
        logger.log(LogLevel.WARNING, "Discovery", "AVC 1080p60 encoder query failed", error)
        false
    }

    private fun capabilityName(value: Int): String = when (value) {
        CC.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "BACKWARD_COMPATIBLE"
        CC.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "MANUAL_SENSOR"
        CC.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> "MANUAL_POST_PROCESSING"
        CC.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW"
        CC.REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS -> "READ_SENSOR_SETTINGS"
        CC.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "BURST_CAPTURE"
        CC.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> "PRIVATE_REPROCESSING"
        CC.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV_REPROCESSING"
        CC.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "DEPTH_OUTPUT"
        CC.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> "HIGH_SPEED_VIDEO"
        CC.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> "LOGICAL_MULTI_CAMERA"
        CC.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME -> "MONOCHROME"
        CC.REQUEST_AVAILABLE_CAPABILITIES_SECURE_IMAGE_DATA -> "SECURE_IMAGE_DATA"
        CC.REQUEST_AVAILABLE_CAPABILITIES_SYSTEM_CAMERA -> "SYSTEM_CAMERA"
        CC.REQUEST_AVAILABLE_CAPABILITIES_OFFLINE_PROCESSING -> "OFFLINE_PROCESSING"
        CC.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR -> "ULTRA_HIGH_RESOLUTION_SENSOR"
        CC.REQUEST_AVAILABLE_CAPABILITIES_REMOSAIC_REPROCESSING -> "REMOSAIC_REPROCESSING"
        CC.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT -> "TEN_BIT_DYNAMIC_RANGE"
        CC.REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE -> "STREAM_USE_CASE"
        CC.REQUEST_AVAILABLE_CAPABILITIES_COLOR_SPACE_PROFILES -> "COLOR_SPACE_PROFILES"
        else -> "CAPABILITY_$value"
    }
}
