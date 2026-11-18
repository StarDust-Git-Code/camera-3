package dev.camera3.app

import dev.camera3.model.CameraDescriptor
import dev.camera3.model.CameraMode
import dev.camera3.model.Dimensions
import dev.camera3.model.HardwareLevel
import dev.camera3.model.LensFacing
import dev.camera3.model.PhotoPreferences
import dev.camera3.model.SensorPixelMode
import org.junit.Assert.*
import org.junit.Test

class CameraUiStateTest {
    // Synthetic policy fixtures, not evidence of hardware or session support.
    private fun camera() = CameraDescriptor(
        id = "fixture-rear",
        facing = LensFacing.BACK,
        level = HardwareLevel.LEVEL_3,
        sensorOrientation = 90,
        jpegSizes = listOf(Dimensions(4000, 3000)),
        previewSizes = listOf(Dimensions(1440, 1080), Dimensions(1920, 1080)),
        rawSizes = listOf(Dimensions(4000, 3000)),
        videoSizes = listOf(Dimensions(1920, 1080)),
        capabilities = setOf("RAW"),
    )

    private fun state(descriptor: CameraDescriptor = camera(), mode: CameraMode = CameraMode.PHOTO) = CameraUiState(
        cameras = listOf(descriptor),
        selectedId = descriptor.id,
        mode = mode,
    )

    @Test fun photoAvailabilityIsIndependentOfActiveMode() {
        for (rawEnabled in listOf(false, true)) {
            val photo = state().copy(preferences = PhotoPreferences(raw = rawEnabled))
            val video = photo.copy(mode = CameraMode.VIDEO)
            assertTrue(photo.photoUsable)
            assertTrue(video.photoUsable)
            assertNotNull(photo.configuration)
            assertNull(video.configuration)
        }
    }

    @Test fun videoAvailabilityIsIndependentOfActiveMode() {
        for (micGranted in listOf(false, true)) {
            val photo = state().copy(micGranted = micGranted)
            val video = photo.copy(mode = CameraMode.VIDEO)
            assertTrue(photo.videoUsable)
            assertTrue(video.videoUsable)
            assertNull(photo.videoConfiguration)
            assertNotNull(video.videoConfiguration)
        }
    }

    @Test fun missingOrIncompatibleStreamsDoNotClaimAvailability() {
        val fixture = camera()
        val unavailableInBothModes = listOf(
            CameraUiState(),
            state(fixture).copy(selectedId = "missing-fixture"),
            state(fixture.copy(previewSizes = emptyList())),
        )
        for (mode in CameraMode.entries) {
            for (candidate in unavailableInBothModes) {
                val unavailable = candidate.copy(mode = mode)
                assertFalse(unavailable.photoUsable)
                assertFalse(unavailable.videoUsable)
                assertNull(unavailable.configuration)
                assertNull(unavailable.videoConfiguration)
            }
            for (descriptor in listOf(
                fixture.copy(jpegSizes = emptyList()),
                fixture.copy(previewSizes = listOf(Dimensions(1920, 1080))),
                fixture.copy(rawSizes = listOf(Dimensions(1920, 1080))),
            )) {
                val unavailable = state(descriptor, mode).copy(preferences = PhotoPreferences(raw = true))
                assertFalse(unavailable.photoUsable)
                assertNull(unavailable.configuration)
                assertTrue(unavailable.videoUsable)
            }
            for (descriptor in listOf(
                fixture.copy(videoSizes = emptyList()),
                fixture.copy(videoSizes = listOf(Dimensions(640, 480))),
                fixture.copy(videoSizes = listOf(Dimensions(3840, 2160))),
                fixture.copy(previewSizes = listOf(Dimensions(1440, 1080))),
            )) {
                val unavailable = state(descriptor, mode)
                assertFalse(unavailable.videoUsable)
                assertNull(unavailable.videoConfiguration)
                assertTrue(unavailable.photoUsable)
            }
        }
    }

    @Test fun rawChangeRequiresDifferentPhotoConfigurationWithSamePreviewSize() {
        val jpegState = state()
        val rawState = jpegState.copy(preferences = jpegState.preferences.copy(raw = true))
        val jpeg = jpegState.configuration!!
        val raw = rawState.configuration!!
        assertEquals(jpeg.preview, raw.preview)
        assertEquals(jpeg.jpeg, raw.jpeg)
        assertNull(jpeg.raw)
        assertEquals(Dimensions(4000, 3000), raw.raw)
        // A preview-size-only key would miss this change to the capture streams.
        assertNotEquals(jpeg, raw)
    }

    @Test fun micChangeRequiresDifferentVideoConfigurationWithSamePreviewSize() {
        val silentState = state(mode = CameraMode.VIDEO)
        val micState = silentState.copy(micGranted = true)
        val silent = silentState.videoConfiguration!!
        val withMic = micState.videoConfiguration!!
        assertEquals(silent.preview, withMic.preview)
        assertEquals(silent.video, withMic.video)
        assertFalse(silent.mic)
        assertTrue(withMic.mic)
        // A preview-size-only key would miss the recorder's audio configuration.
        assertNotEquals(silent, withMic)
    }

    @Test fun highResolutionChangeRequiresDifferentPhotoConfigurationWithSamePreviewSize() {
        val full = Dimensions(8160, 6120)
        val descriptor = camera().copy(
            capabilities = setOf("RAW", "ULTRA_HIGH_RESOLUTION_SENSOR"),
            requestKeys = setOf("android.sensor.pixelMode"),
            maximumResolutionJpegSizes = listOf(full),
            maximumResolutionRawSizes = listOf(full),
        )
        val standardState = state(descriptor)
        val maximumState = standardState.copy(
            preferences = PhotoPreferences(highResolution = true),
        )
        val standard = standardState.configuration!!
        val maximum = maximumState.configuration!!
        assertEquals(standard.preview, maximum.preview)
        assertEquals(SensorPixelMode.DEFAULT, standard.sensorPixelMode)
        assertEquals(SensorPixelMode.MAXIMUM_RESOLUTION, maximum.sensorPixelMode)
        assertEquals(full, maximum.jpeg)
        assertNotEquals(standard, maximum)
    }
}
