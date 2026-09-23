package br.com.rb8digital.rbcinecam.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessionalHudContractTest {
    @Test fun topBarContainsOnlyImplementedTechnicalControls() {
        assertEquals(
            listOf("QUALITY", "FPS", "ASPECT", "SCOPES", "GRID"),
            ProfessionalHudContract.topTechnicalControls
        )
    }

    @Test fun unimplementedCinemaToolsAreNotExposed() {
        val visible = ProfessionalHudContract.allVisibleControls
        assertFalse("LUT" in visible)
        assertFalse("ZEBRA" in visible)
        assertFalse("FALSE_COLOR" in visible)
        assertFalse("PEAKING" in visible)
        assertFalse("LOG" in visible)
        assertFalse("RAW" in visible)
    }

    @Test fun existingCaptureAndManualControlsRemainAvailable() {
        val visible = ProfessionalHudContract.allVisibleControls
        listOf("VIDEO", "PHOTO", "GALLERY", "SWITCH_CAMERA", "REC", "ISO", "SHUTTER", "WB", "FOCUS", "EV")
            .forEach { assertTrue("missing $it", it in visible) }
    }

    @Test fun visualMetricsKeepPreviewDominant() {
        assertTrue(ProfessionalHudContract.topBarHeightDp in 44..48)
        assertTrue(ProfessionalHudContract.sideRailWidthDp <= 52)
        assertTrue(ProfessionalHudContract.recordButtonDp <= 60)
        assertTrue(ProfessionalHudContract.audioExpandedWidthDp <= 44)
    }
}
