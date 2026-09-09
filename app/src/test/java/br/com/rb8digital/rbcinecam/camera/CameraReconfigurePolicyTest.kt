package br.com.rb8digital.rbcinecam.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraReconfigurePolicyTest {
    @Test
    fun `quality change starts from a safe 30 fps baseline`() {
        assertEquals(30, CameraReconfigurePolicy.safeFpsAfterQualityChange(currentFps = 60, supported = listOf(24, 30, 60)))
        assertEquals(24, CameraReconfigurePolicy.safeFpsAfterQualityChange(currentFps = 60, supported = listOf(24)))
    }

    @Test
    fun `failed requested quality falls back to FHD when available`() {
        assertEquals("FHD", CameraReconfigurePolicy.fallbackQuality("4K", listOf("HD", "FHD", "4K")))
        assertEquals("HD", CameraReconfigurePolicy.fallbackQuality("4K", listOf("HD", "4K")))
    }
}