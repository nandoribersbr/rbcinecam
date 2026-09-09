package br.com.rb8digital.rbcinecam.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioMeterMathTest {
    @Test fun `silence maps to floor`() {
        assertEquals(-60f, AudioMeterMath.dbfs(shortArrayOf(0, 0, 0)), 0.01f)
    }

    @Test fun `full scale sample approaches zero dbfs`() {
        val db = AudioMeterMath.dbfs(shortArrayOf(32767, -32768))
        assertTrue(db > -0.2f)
    }

    @Test fun `clipping threshold detects near full scale peaks`() {
        assertTrue(AudioMeterMath.isClipping(-0.5f))
    }

    @Test fun `CameraX full amplitude maps to zero dbfs`() {
        assertEquals(0f, AudioMeterMath.amplitudeDbfs(1.0), 0.01f)
        assertEquals(-60f, AudioMeterMath.amplitudeDbfs(0.0), 0.01f)
    }
}
