package br.com.rb8digital.rbcinecam.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProOverlayPolicyTest {
    @Test fun preview_is_full_bleed_and_controls_are_translucent() {
        assertTrue(ProOverlayPolicy.fullBleedPreview)
        assertTrue(ProOverlayPolicy.controlAlpha in 0.35f..0.70f)
        assertEquals(0.dpSafeMargin, ProOverlayPolicy.previewOuterMarginDp)
    }

    private val Int.dpSafeMargin: Int get() = this
}
