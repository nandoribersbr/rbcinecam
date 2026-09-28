package br.com.rb8digital.rbcinecam.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigurationPolicyTest {
    private val rear = CameraCapabilities(
        cameraId = "rear-main",
        supportedModes = setOf(
            VideoMode(Resolution.FHD, 24),
            VideoMode(Resolution.FHD, 30),
            VideoMode(Resolution.FHD, 60),
            VideoMode(Resolution.UHD4K, 24),
            VideoMode(Resolution.UHD4K, 30)
        )
    )
    private val policy = ConfigurationPolicy()

    @Test
    fun unsupported4k60KeepsLastValidMode() {
        val requested = CameraSettings("rear-main", VideoMode(Resolution.UHD4K, 60))
        val previous = CameraSettings("rear-main", VideoMode(Resolution.UHD4K, 30))
        val result = policy.resolve(requested, previous, rear)
        assertEquals(previous, result.applied)
        assertTrue(result.didFallback)
    }

    @Test
    fun supportedFhd60IsApplied() {
        val previous = CameraSettings("rear-main", VideoMode(Resolution.UHD4K, 30))
        val requested = CameraSettings("rear-main", VideoMode(Resolution.FHD, 60))
        val result = policy.resolve(requested, previous, rear)
        assertEquals(requested, result.applied)
        assertFalse(result.didFallback)
    }

    @Test
    fun switchingSensorRevalidatesMode() {
        val ultraWide = CameraCapabilities(
            cameraId = "rear-ultra",
            supportedModes = setOf(VideoMode(Resolution.FHD, 30))
        )
        val result = policy.resolve(
            requested = CameraSettings("rear-ultra", VideoMode(Resolution.UHD4K, 30)),
            previous = CameraSettings("rear-main", VideoMode(Resolution.UHD4K, 30)),
            capabilities = ultraWide
        )
        assertEquals("rear-ultra", result.applied.cameraId)
        assertEquals(VideoMode(Resolution.FHD, 30), result.applied.mode)
        assertTrue(result.didFallback)
    }
}
