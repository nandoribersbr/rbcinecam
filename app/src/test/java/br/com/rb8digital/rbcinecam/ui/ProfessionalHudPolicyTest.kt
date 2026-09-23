package br.com.rb8digital.rbcinecam.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessionalHudPolicyTest {
    @Test fun technicalControlsBelongToTopBar() {
        assertEquals(
            listOf("QUALITY", "FPS", "ASPECT", "SCOPES", "GRID"),
            ProfessionalHudPolicy.topTechnicalControls
        )
    }

    @Test fun previewHasNoFloatingTechnicalDuplicates() {
        assertFalse(ProfessionalHudPolicy.previewFloatingControls.contains("SCOPES"))
        assertFalse(ProfessionalHudPolicy.previewFloatingControls.contains("GRID"))
    }

    @Test fun onlyImplementedManualControlsAreVisible() {
        assertEquals(
            listOf("ISO", "SHUTTER", "WB", "FOCUS", "EV"),
            ProfessionalHudPolicy.manualControls
        )
        assertFalse(ProfessionalHudPolicy.manualControls.contains("LUT"))
        assertFalse(ProfessionalHudPolicy.manualControls.contains("PEAKING"))
    }

    @Test fun touchTargetsRemainUsable() {
        assertTrue(ProfessionalHudPolicy.minimumTouchTargetDp >= 44)
        assertTrue(ProfessionalHudPolicy.topBarHeightDp in 44..48)
    }
}
