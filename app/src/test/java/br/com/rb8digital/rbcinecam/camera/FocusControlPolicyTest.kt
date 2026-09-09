package br.com.rb8digital.rbcinecam.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusControlPolicyTest {
    @Test
    fun `manual focus is available only with positive minimum focus distance`() {
        assertTrue(FocusControlPolicy.manualFocusSupported(3.5f))
        assertFalse(FocusControlPolicy.manualFocusSupported(0f))
        assertFalse(FocusControlPolicy.manualFocusSupported(null))
    }

    @Test
    fun `manual focus fraction maps infinity to zero diopters and macro to minimum distance`() {
        assertEquals(0f, FocusControlPolicy.distanceForFraction(4f, 0f), 0.001f)
        assertEquals(2f, FocusControlPolicy.distanceForFraction(4f, 0.5f), 0.001f)
        assertEquals(4f, FocusControlPolicy.distanceForFraction(4f, 1f), 0.001f)
    }
}