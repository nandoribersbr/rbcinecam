package br.com.rb8digital.rbcinecam.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureConfigurationTest {
    @Test fun `requested configuration does not replace applied state until success`() {
        val applied = CaptureConfiguration("FHD", 30)
        val requested = applied.request("4K", 60)
        assertEquals("FHD", applied.quality)
        assertEquals(30, applied.fps)
        assertEquals("4K", requested.quality)
        assertEquals(60, requested.fps)
    }

    @Test fun `nearest safe fps prefers 30 then closest`() {
        assertEquals(30, CaptureConfiguration.safeFps(listOf(24, 30, 60)))
        assertEquals(25, CaptureConfiguration.safeFps(listOf(25, 50)))
    }
}
