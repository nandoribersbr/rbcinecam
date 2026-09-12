package br.com.rb8digital.rbcinecam.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureSettingsPolicyTest {
    @Test
    fun `cycles only through supported frame rates`() {
        assertEquals(60, CaptureSettingsPolicy.nextFps(30, listOf(24, 30, 60)))
        assertEquals(30, CaptureSettingsPolicy.nextFps(24, listOf(24, 30, 60)))
        assertEquals(24, CaptureSettingsPolicy.nextFps(60, listOf(24, 30, 60)))
    }

    @Test
    fun `cycles only through supported sizes`() {
        assertEquals("FHD", CaptureSettingsPolicy.nextQuality("HD", listOf("HD", "FHD", "4K")))
        assertEquals("4K", CaptureSettingsPolicy.nextQuality("FHD", listOf("HD", "FHD", "4K")))
        assertEquals("HD", CaptureSettingsPolicy.nextQuality("4K", listOf("HD", "FHD", "4K")))
    }

    @Test
    fun `aspect ratios cycle through cinema and Large Format choices`() {
        assertEquals("17:9", CaptureSettingsPolicy.nextAspect("16:9"))
        assertEquals("LF 1.90", CaptureSettingsPolicy.nextAspect("1:1"))
        assertEquals("LF 1.43", CaptureSettingsPolicy.nextAspect("LF 1.90"))
        assertEquals("16:9", CaptureSettingsPolicy.nextAspect("LF 1.43"))
    }
}
