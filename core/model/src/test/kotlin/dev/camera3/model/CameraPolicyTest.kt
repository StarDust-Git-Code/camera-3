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
}
