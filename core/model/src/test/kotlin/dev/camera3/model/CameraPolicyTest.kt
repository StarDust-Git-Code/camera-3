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
    @Test fun videoNeedsBoundedWideSizeAndMatchingPreview() {
        val camera = camera().copy(videoSizes = listOf(
            Dimensions(3840, 2160), Dimensions(1920, 1080), Dimensions(1280, 720),
            Dimensions(640, 480),
        ), previewSizes = listOf(Dimensions(1440, 1080), Dimensions(1280, 720), Dimensions(640, 480)))
        val config = CameraPolicy.videoConfiguration(camera, mic = true)!!
        assertEquals(Dimensions(1920, 1080), config.video)
        assertEquals(Dimensions(1280, 720), config.preview)
        assertTrue(config.mic)
        assertTrue(CameraPolicy.videoConfiguration(camera, mic = false)!!.preview.width == 1280)
        // No 16:9 preview for any 16:9 video size: the mode stays unavailable.
        assertNull(CameraPolicy.videoConfiguration(
            camera.copy(previewSizes = listOf(Dimensions(1440, 1080))), mic = false))
        // Nothing under the bound except 4:3 sizes: unavailable, not invented.
        assertNull(CameraPolicy.videoConfiguration(
            camera.copy(videoSizes = listOf(Dimensions(640, 480))), mic = false))
    }
}
