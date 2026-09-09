package br.com.rb8digital.rbcinecam.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class LargeFormatPolicyTest {
    @Test fun `large format exposes approved framing ratios`() {
        assertEquals(listOf("LF 1.90", "LF 1.43"), LargeFormatPolicy.modes)
    }

    @Test fun `large format prefers 24 fps when available`() {
        assertEquals(24, LargeFormatPolicy.preferredFps(listOf(24, 30, 60)))
        assertEquals(30, LargeFormatPolicy.preferredFps(listOf(30, 60)))
    }

    @Test fun `large format chooses highest supported quality`() {
        assertEquals("4K", LargeFormatPolicy.preferredQuality(listOf("HD", "FHD", "4K")))
        assertEquals("FHD", LargeFormatPolicy.preferredQuality(listOf("HD", "FHD")))
    }
}
