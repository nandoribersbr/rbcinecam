package br.com.rb8digital.rbcinecam.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CinemaControlPolicyTest {
    @Test
    fun `180 degree shutter follows selected frame rate`() {
        assertEquals(48, CinemaControlPolicy.shutterDenominator(24, 180))
        assertEquals(50, CinemaControlPolicy.shutterDenominator(25, 180))
        assertEquals(60, CinemaControlPolicy.shutterDenominator(30, 180))
    }

    @Test
    fun `frame rate choices expose only hardware supported values`() {
        assertEquals(listOf(24, 30, 60), CinemaControlPolicy.availableFrameRates(listOf(15, 24, 30, 60, 120), maxFps = 60))
    }

    @Test
    fun `lens labels are stable cinema labels`() {
        assertEquals("0.6×", CinemaControlPolicy.lensLabel(0.6f))
        assertEquals("1×", CinemaControlPolicy.lensLabel(1f))
        assertEquals("3×", CinemaControlPolicy.lensLabel(3f))
    }
}