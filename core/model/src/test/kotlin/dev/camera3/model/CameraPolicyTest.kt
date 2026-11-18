package dev.camera3.model

import org.junit.Assert.*
import org.junit.Test

class CameraPolicyTest {
    private fun camera(id: String = "rear", level: HardwareLevel = HardwareLevel.LEVEL_3,
                       facing: LensFacing = LensFacing.BACK) = CameraDescriptor(
        id, facing, level, 90,
        listOf(Dimensions(4000, 3000), Dimensions(8000, 6000)),
        listOf(Dimensions(1440, 1080), Dimensions(1920, 1080), Dimensions(640, 480)),
    )

    @Test fun onlyLevel3UnlocksDevice() {
        assertFalse(CameraPolicy.isEligible(listOf(camera(level = HardwareLevel.FULL))))
        assertTrue(CameraPolicy.isEligible(listOf(camera())))
        assertFalse(CameraPolicy.isEligible(emptyList()))
    }
    @Test fun preferQualifyingRearWithoutAssumingIdZero() {
        assertEquals("rear", CameraPolicy.initialCamera(listOf(
            camera("0", HardwareLevel.FULL), camera("selfie", facing = LensFacing.FRONT), camera(),
        ))?.id)
    }
    @Test fun selectMatchingAspectAndBoundedBaseline() {
        val config = CameraPolicy.configuration(camera())!!
        assertEquals(Dimensions(4000, 3000), config.jpeg)
        assertEquals(Dimensions(1440, 1080), config.preview)
    }
    @Test fun incompatibleStreamAspectIsNotInvented() {
        assertNull(CameraPolicy.configuration(camera().copy(previewSizes = listOf(Dimensions(1920, 1080)))))
    }
    @Test fun missingOutputsCannotBeSelected() {
        assertNull(CameraPolicy.initialCamera(listOf(camera().copy(jpegSizes = emptyList()))))
    }
    @Test fun rotationsCoverRearAndFront() {
        assertEquals(listOf(90, 180, 270, 0), listOf(0, 90, 180, 270).map {
            CameraPolicy.relativeRotation(90, it, false)
        })
        assertEquals(listOf(270, 180, 90, 0), listOf(0, 90, 180, 270).map {
            CameraPolicy.relativeRotation(270, it, true)
        })
        assertEquals(0, CameraPolicy.jpegOrientation(90, 90, false))
        assertEquals(0, CameraPolicy.jpegOrientation(270, 90, true))
    }
    @Test fun zoomIsFiniteAndClamped() {
        assertEquals(2f, CameraPolicy.validatedZoom(9f, camera().copy(maxZoom = 2f)), 0f)
        assertThrows(IllegalArgumentException::class.java) { CameraPolicy.validatedZoom(Float.NaN, camera()) }
    }
    @Test fun preferenceRangeIsValidated() {
        assertThrows(IllegalArgumentException::class.java) { PhotoPreferences(jpegQuality = 0) }
    }
    @Test fun torchRequiresHardwareAndWritableRequestKey() {
        assertFalse(camera().copy(flashAvailable = true).supportsTorch)
        assertFalse(camera().copy(requestKeys = setOf("android.flash.mode")).supportsTorch)
        assertTrue(camera().copy(flashAvailable = true, requestKeys = setOf("android.flash.mode")).supportsTorch)
    }
    @Test fun supportsRawNeedsCapabilityAndRealSizes() {
        assertFalse(camera().copy(rawSizes = listOf(Dimensions(4000, 3000))).supportsRaw)
        assertFalse(camera().copy(capabilities = setOf("RAW")).supportsRaw)
        assertTrue(camera().copy(capabilities = setOf("RAW"), rawSizes = listOf(Dimensions(4000, 3000))).supportsRaw)
    }
    @Test fun rawPairsOnlyWhenAdvertisedAndAspectMatching() {
        val raw = Dimensions(4080, 3060)
        val advertised = camera().copy(capabilities = setOf("RAW"), rawSizes = listOf(raw))
        assertEquals(raw, CameraPolicy.configuration(advertised, true)?.raw)
        assertNull(CameraPolicy.configuration(advertised, false)?.raw)
        // Sizes reported without the capability must not enable a RAW stream.
        assertNull(CameraPolicy.configuration(camera().copy(rawSizes = listOf(raw)), true)?.raw)
        // Capability reported but no aspect-matching size: no configuration is invented.
        assertNull(CameraPolicy.configuration(advertised.copy(rawSizes = listOf(Dimensions(1920, 1080))), true))
    }
    @Test fun videoRequiresReal1080p60CameraAndEncoderSupport() {
        val fullHd = Dimensions(1920, 1080)
        val camera = camera().copy(
            videoSizes = listOf(Dimensions(3840, 2160), fullHd, Dimensions(1280, 720)),
            previewSizes = listOf(Dimensions(1440, 1080), Dimensions(1280, 720)),
            requestKeys = setOf("android.control.aeTargetFpsRange"),
            aeFrameRateRanges = listOf(FrameRateRange(30, 30), FrameRateRange(60, 60)),
            videoMinFrameDurationNs = mapOf(fullHd to 16_666_666L),
            supportsAvc1080p60 = true,
        )
        val config = CameraPolicy.videoConfiguration(camera, mic = true)!!
        assertEquals(Dimensions(1920, 1080), config.video)
        assertEquals(Dimensions(1280, 720), config.preview)
        assertEquals(60, config.fps)
        assertEquals(VideoSessionMode.REGULAR, config.sessionMode)
        assertTrue(config.mic)
        assertFalse(CameraPolicy.videoConfiguration(camera, mic = false)!!.mic)
        assertNull(CameraPolicy.videoConfiguration(camera.copy(videoSizes = listOf(Dimensions(1280, 720))), false))
        assertNull(CameraPolicy.videoConfiguration(camera.copy(previewSizes = listOf(Dimensions(1440, 1080))), false))
        assertNull(CameraPolicy.videoConfiguration(camera.copy(requestKeys = emptySet()), false))
        assertNull(CameraPolicy.videoConfiguration(camera.copy(aeFrameRateRanges = listOf(FrameRateRange(30, 30))), false))
        assertNull(CameraPolicy.videoConfiguration(camera.copy(videoMinFrameDurationNs = mapOf(fullHd to 16_666_667L)), false))
        assertNull(CameraPolicy.videoConfiguration(camera.copy(videoMinFrameDurationNs = mapOf(fullHd to -1L)), false))
        assertNull(CameraPolicy.videoConfiguration(camera.copy(supportsAvc1080p60 = false), false))
    }

    @Test fun constrainedHighSpeed1080p60IsUsedOnlyWhenExplicitlyReported() {
        val fullHd = Dimensions(1920, 1080)
        val base = camera().copy(
            videoSizes = listOf(fullHd),
            previewSizes = listOf(fullHd),
            requestKeys = setOf("android.control.aeTargetFpsRange"),
            capabilities = setOf("HIGH_SPEED_VIDEO"),
            highSpeedVideoFrameRateRanges = mapOf(fullHd to listOf(FrameRateRange(60, 60))),
            supportsAvc1080p60 = true,
        )
        assertEquals(
            VideoSessionMode.CONSTRAINED_HIGH_SPEED,
            CameraPolicy.videoConfiguration(base, false)?.sessionMode,
        )
        assertNull(CameraPolicy.videoConfiguration(
            base.copy(highSpeedVideoFrameRateRanges = mapOf(fullHd to listOf(FrameRateRange(30, 120)))), false,
        ))
        assertNull(CameraPolicy.videoConfiguration(
            base.copy(previewSizes = listOf(Dimensions(1280, 720))), false,
        ))
    }

    @Test fun maximumResolutionNeedsRealPixelModeOutputs() {
        val full = Dimensions(8160, 6120)
        val maximumRaw = Dimensions(8160, 6120)
        val capable = camera().copy(
            capabilities = setOf("RAW", "ULTRA_HIGH_RESOLUTION_SENSOR"),
            requestKeys = setOf("android.sensor.pixelMode"),
            maximumResolutionJpegSizes = listOf(full),
            maximumResolutionRawSizes = listOf(maximumRaw),
        )
        val jpeg = CameraPolicy.configuration(capable, rawEnabled = false, highResolution = true)!!
        assertEquals(full, jpeg.jpeg)
        assertNull(jpeg.raw)
        assertEquals(SensorPixelMode.MAXIMUM_RESOLUTION, jpeg.sensorPixelMode)
        assertEquals(maximumRaw, CameraPolicy.configuration(capable, true, true)?.raw)
        // Android also permits SENSOR_PIXEL_MODE without the ultra-high-resolution
        // capability; the real key and maximum-resolution map remain authoritative.
        assertNotNull(CameraPolicy.configuration(
            capable.copy(capabilities = setOf("RAW")), false, true,
        ))
        assertNull(CameraPolicy.configuration(capable.copy(requestKeys = emptySet()), false, true))
        assertNull(CameraPolicy.configuration(capable.copy(maximumResolutionRawSizes = emptyList()), true, true))
        assertNull(CameraPolicy.configuration(
            capable.copy(maximumResolutionRawSizes = listOf(Dimensions(8160, 4590))), true, true,
        ))
    }

    @Test fun videoZoomInterpolationIsBoundedAndPerceptual() {
        assertEquals(1f, CameraPolicy.interpolatedZoom(1f, 8f, 0f), 0.0001f)
        assertEquals(8f, CameraPolicy.interpolatedZoom(1f, 8f, 1f), 0.0001f)
        assertEquals(kotlin.math.sqrt(8f), CameraPolicy.interpolatedZoom(1f, 8f, .5f), 0.0001f)
        assertEquals(1f, CameraPolicy.interpolatedZoom(1f, 8f, -1f), 0.0001f)
        assertEquals(8f, CameraPolicy.interpolatedZoom(1f, 8f, 2f), 0.0001f)
    }
}
