package br.com.rb8digital.rbcinecam.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingProfilePolicyTest {
    @Test
    fun `only exposes profiles supported by hardware`() {
        val profiles = RecordingProfilePolicy.availableProfiles(
            CameraCapabilities(supportsHdr10Bit = true, supportsRaw = false)
        )
        assertEquals(listOf(RecordingProfile.SDR, RecordingProfile.HLG10, RecordingProfile.RB_LOG), profiles)
    }

    @Test
    fun `raw profile appears only when raw is supported`() {
        val profiles = RecordingProfilePolicy.availableProfiles(CameraCapabilities(supportsRaw = true))
        assertEquals(listOf(RecordingProfile.SDR, RecordingProfile.RAW), profiles)
    }
}
