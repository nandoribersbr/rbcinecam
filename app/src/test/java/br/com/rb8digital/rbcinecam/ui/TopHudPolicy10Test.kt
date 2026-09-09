package br.com.rb8digital.rbcinecam.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopHudPolicy10Test {
    @Test
    fun `wide screens keep full technical labels`() {
        val spec = TopHudPolicy10.forWidthDp(900f, recording = false)
        assertEquals(TopHudDensity10.FULL, spec.density)
        assertTrue(spec.showCodec)
        assertTrue(spec.showBattery)
        assertTrue(spec.showFullBrand)
    }

    @Test
    fun `medium screens compact secondary labels`() {
        val spec = TopHudPolicy10.forWidthDp(680f, recording = false)
        assertEquals(TopHudDensity10.COMPACT, spec.density)
        assertTrue(spec.showBattery)
        assertTrue(!spec.showFullBrand)
    }

    @Test
    fun `narrow screens preserve recording status over secondary metadata`() {
        val spec = TopHudPolicy10.forWidthDp(520f, recording = true)
        assertEquals(TopHudDensity10.CRITICAL, spec.density)
        assertTrue(spec.showRecordingStatus)
        assertTrue(!spec.showCodec)
        assertTrue(!spec.showBattery)
        assertTrue(!spec.showFullBrand)
    }
}