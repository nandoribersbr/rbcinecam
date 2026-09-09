package br.com.rb8digital.rbcinecam.monitoring

import org.junit.Assert.assertEquals
import org.junit.Test

class ScopeMathTest {
    @Test fun `histogram bins clamp full range`() {
        assertEquals(0, ScopeMath.histogramBin(0))
        assertEquals(32, ScopeMath.histogramBin(128))
        assertEquals(63, ScopeMath.histogramBin(255))
    }

    @Test fun `waveform maps image edges and luma correctly`() {
        assertEquals(0, ScopeMath.waveformX(0, 1920))
        assertEquals(63, ScopeMath.waveformX(1919, 1920))
        assertEquals(63, ScopeMath.waveformY(0))
        assertEquals(0, ScopeMath.waveformY(255))
    }
}
